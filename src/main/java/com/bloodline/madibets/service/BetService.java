package com.bloodline.madibets.service;

import com.bloodline.madibets.config.DatabaseConnection;
import com.bloodline.madibets.dao.AccountDAO;
import com.bloodline.madibets.dao.BetDAO;
import com.bloodline.madibets.dao.EventDAO;
import com.bloodline.madibets.dao.UserDAO;
import com.bloodline.madibets.model.Bet;
import com.bloodline.madibets.model.Event;
import com.bloodline.madibets.model.User;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Owner: Kieran (B-series). Business rules for the bet lifecycle; SQL stays in BetDAO.
 * Follows the AuthService pattern. Invalid input is reported with
 * IllegalArgumentException so callers can show the message to the user.
 */
public class BetService {

    /** odds is DECIMAL(6,2): anything above this overflows the column. */
    private static final BigDecimal MAX_ODDS = new BigDecimal("9999.99");

    private final BetDAO betDAO = new BetDAO();
    private final EventDAO eventDAO = new EventDAO();
    private final AccountDAO accountDAO = new AccountDAO();
    private final UserDAO userDAO = new UserDAO();   // read-only use: B300 admin check

    /**
     * B700: the bets a student can currently wager on.
     * No business rules yet — the 'ACTIVE' filter is the whole use case. Stake
     * validation (B100) and payout logic (B400/B600) land here later.
     */
    public List<Bet> viewActiveBets() throws SQLException {
        return betDAO.findActiveBets();
    }

    /** B300: the review queue for the admin "Accounting System" screen. */
    public List<Bet> viewProposedBets() throws SQLException {
        return betDAO.findProposedBets();
    }

    /**
     * B200: a student proposes a bet. It stays PROPOSED (invisible to B700)
     * until an admin reviews it and assigns odds (B300). The event link is
     * optional, but when given it must point at an event that can still happen.
     * Returns the new betID.
     */
    public int proposeBet(int userID, Integer eventID, String description) throws SQLException {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("A proposal needs a description.");
        }
        if (eventID != null) {
            Event e = eventDAO.findById(eventID);
            if (e == null) {
                throw new IllegalArgumentException("Event " + eventID + " does not exist.");
            }
            if ("ENDED".equals(e.getStatus()) || "CANCELLED".equals(e.getStatus())) {
                throw new IllegalArgumentException(
                        "Cannot propose a bet on a " + e.getStatus().toLowerCase() + " event.");
            }
        }
        Bet b = new Bet();
        b.setUserID(userID);
        b.setEventID(eventID);
        b.setDescription(description.trim());
        b.setOutcome("PENDING");
        b.setStatus("PROPOSED");
        b.setProposedDate(LocalDateTime.now());
        return betDAO.propose(b);
    }

    /**
     * B300: an admin approves a proposal — assigns the odds and opens the bet
     * for wagering. Odds are decimal ("for one"): 1.00 would pay back exactly
     * the stake, so they must be strictly greater.
     */
    public void approveProposal(int betID, BigDecimal odds, int adminUserID) throws SQLException {
        requireAdmin(adminUserID, "review bet proposals");
        if (odds == null || odds.compareTo(BigDecimal.ONE) <= 0 || odds.compareTo(MAX_ODDS) > 0) {
            throw new IllegalArgumentException("Odds must be between 1.01 and " + MAX_ODDS + ".");
        }
        if (!betDAO.activate(betID, odds)) {
            throw notReviewable(betID);
        }
    }

    /**
     * B300: an admin rejects a proposal. The FSSB keeps rejected bets out of
     * every view but the schema has no REJECTED state, so rejection reuses
     * DELETED — the terminal status B500 also uses.
     */
    public void rejectProposal(int betID, int adminUserID) throws SQLException {
        requireAdmin(adminUserID, "review bet proposals");
        if (!betDAO.reject(betID)) {
            throw notReviewable(betID);
        }
    }

    /**
     * B100: a student wagers on an ACTIVE bet. Update-in-place model (team
     * decision on design gap #1): the wager is stored on the market row itself,
     * so each bet takes exactly one wager, implicitly on the YES side of the
     * description. The stake leaves the student's Account immediately;
     * amountToBeWon = stake × odds (decimal odds — payout includes the stake
     * back). Debit, wager claim, and audit row commit as ONE DB transaction.
     */
    public void placeBet(int betID, int userID, BigDecimal stake) throws SQLException {
        if (stake == null || stake.signum() <= 0 || stake.scale() > 2) {
            throw new IllegalArgumentException("Stake must be a positive amount with at most 2 decimals.");
        }
        Bet b = betDAO.findById(betID);
        if (b == null) {
            throw new IllegalArgumentException("Bet " + betID + " does not exist.");
        }
        if (!"ACTIVE".equals(b.getStatus())) {
            throw new IllegalArgumentException("Bet " + betID + " is " + b.getStatus() + " — not open for wagering.");
        }
        if (b.getPlacedDate() != null) {
            throw new IllegalArgumentException("Bet " + betID + " already has a wager on it.");
        }
        BigDecimal payout = stake.multiply(b.getOdds()).setScale(2, RoundingMode.HALF_UP);

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                if (!accountDAO.adjustBalance(con, userID, stake.negate())) {
                    throw new IllegalArgumentException("Insufficient MadiBucks for a stake of " + stake + ".");
                }
                if (!betDAO.placeWager(con, betID, userID, payout, LocalDateTime.now())) {
                    // Someone else's wager or a status change won the race after our read.
                    throw new IllegalArgumentException("Bet " + betID + " was taken or closed in the meantime.");
                }
                accountDAO.logTransaction(con, userID, stake.negate(),
                        "B100: user " + userID + " staked " + stake + " on bet " + betID);
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    /**
     * B400 + B600: an admin records the real-world outcome, and any winnings or
     * refund land in the holder's Account in the same DB transaction.
     * YES        -> the wager (implicitly on the YES side) won: credit amountToBeWon.
     * NO         -> wager lost: the stake left the account at placement; nothing moves.
     * CANCELLED  -> stake refunded. The schema stores no stake column, so the
     *               refund is derived as amountToBeWon / odds (see stakeOf).
     * A never-wagered bet can still be graded — the market simply closes.
     */
    public void gradeBet(int betID, String outcome, int adminUserID) throws SQLException {
        requireAdmin(adminUserID, "grade bets");
        if (!"YES".equals(outcome) && !"NO".equals(outcome) && !"CANCELLED".equals(outcome)) {
            throw new IllegalArgumentException("Outcome must be YES, NO or CANCELLED.");
        }
        Bet b = betDAO.findById(betID);
        if (b == null) {
            throw new IllegalArgumentException("Bet " + betID + " does not exist.");
        }
        if (!"ACTIVE".equals(b.getStatus())) {
            throw new IllegalArgumentException(
                    "Bet " + betID + " is " + b.getStatus() + " — only an ACTIVE bet can be graded.");
        }

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                if (!betDAO.grade(con, betID, outcome, adminUserID, LocalDateTime.now())) {
                    throw new IllegalArgumentException("Bet " + betID + " was graded or closed in the meantime.");
                }
                if (b.getPlacedDate() != null) {   // someone holds a wager on this bet
                    if ("YES".equals(outcome)) {
                        payOut(con, b.getUserID(), b.getAmountToBeWon(),
                                "B600: bet " + betID + " won, paid user " + b.getUserID());
                    } else if ("CANCELLED".equals(outcome)) {
                        payOut(con, b.getUserID(), stakeOf(b),
                                "B600: bet " + betID + " cancelled, stake refunded to user " + b.getUserID());
                    }
                }
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    /**
     * B500: an admin retires a bet. Soft delete — status DELETED keeps the row
     * for the audit trail (simulated accounting must stay reconstructable), and
     * B700 already filters it out. A live wager is refunded in the same
     * transaction so an admin deletion never costs the student their stake.
     * GRADED bets are settled history and cannot be deleted.
     */
    public void deleteBet(int betID, int adminUserID) throws SQLException {
        requireAdmin(adminUserID, "delete bets");
        Bet b = betDAO.findById(betID);
        if (b == null) {
            throw new IllegalArgumentException("Bet " + betID + " does not exist.");
        }
        if (!"PROPOSED".equals(b.getStatus()) && !"ACTIVE".equals(b.getStatus())) {
            throw new IllegalArgumentException(
                    "Bet " + betID + " is " + b.getStatus() + " — only a PROPOSED or ACTIVE bet can be deleted.");
        }

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                if (!betDAO.delete(con, betID)) {
                    throw new IllegalArgumentException("Bet " + betID + " was closed in the meantime.");
                }
                if ("ACTIVE".equals(b.getStatus()) && b.getPlacedDate() != null) {
                    payOut(con, b.getUserID(), stakeOf(b),
                            "B500: bet " + betID + " deleted, stake refunded to user " + b.getUserID());
                }
                con.commit();
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    /** Credit within the caller's transaction + audit row. A positive credit only
     *  fails when the Account row is missing — that is data corruption, not user error. */
    private void payOut(Connection con, int userID, BigDecimal amount, String description) throws SQLException {
        if (!accountDAO.adjustBalance(con, userID, amount)) {
            throw new IllegalStateException("User " + userID + " has no Account row to receive " + amount + ".");
        }
        accountDAO.logTransaction(con, userID, amount, description);
    }

    /** The stake is not stored (design gap #1) — derive it from payout / odds.
     *  Rounding can drift the refund by a cent; flagged, accepted by the team. */
    private BigDecimal stakeOf(Bet b) {
        return b.getAmountToBeWon().divide(b.getOdds(), 2, RoundingMode.HALF_UP);
    }

    private void requireAdmin(int adminUserID, String action) throws SQLException {
        User u = userDAO.findById(adminUserID);
        if (u == null || !"ADMIN".equals(u.getUserType())) {
            throw new IllegalArgumentException("Only an ADMIN may " + action + ".");
        }
    }

    /** Shared diagnostics for the two B300 outcomes when the guarded UPDATE matched nothing. */
    private IllegalArgumentException notReviewable(int betID) throws SQLException {
        Bet b = betDAO.findById(betID);
        if (b == null) {
            return new IllegalArgumentException("Bet " + betID + " does not exist.");
        }
        return new IllegalArgumentException(
                "Bet " + betID + " is " + b.getStatus() + ", not PROPOSED — nothing to review.");
    }
}

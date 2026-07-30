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
        User reviewer = userDAO.findById(adminUserID);
        if (reviewer == null || !"ADMIN".equals(reviewer.getUserType())) {
            throw new IllegalArgumentException("Only an ADMIN may review bet proposals.");
        }
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
        User reviewer = userDAO.findById(adminUserID);
        if (reviewer == null || !"ADMIN".equals(reviewer.getUserType())) {
            throw new IllegalArgumentException("Only an ADMIN may review bet proposals.");
        }
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
                accountDAO.logTransaction(con, stake.negate(),
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

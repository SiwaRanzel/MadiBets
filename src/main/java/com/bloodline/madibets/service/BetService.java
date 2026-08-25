package com.bloodline.madibets.service;

import com.bloodline.madibets.config.DatabaseConnection;
import com.bloodline.madibets.dao.AccountDAO;
import com.bloodline.madibets.dao.BetDAO;
import com.bloodline.madibets.dao.EventDAO;
import com.bloodline.madibets.dao.NotificationDAO;
import com.bloodline.madibets.dao.UserDAO;
import com.bloodline.madibets.model.Bet;
import com.bloodline.madibets.model.BetOutcome;
import com.bloodline.madibets.model.Event;
import com.bloodline.madibets.model.User;
import com.bloodline.madibets.model.Wager;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Owner: Kieran (B-series). Business rules for the bet lifecycle; SQL stays in BetDAO.
 * Follows the AuthService pattern. Invalid input is reported with
 * IllegalArgumentException so callers can show the message to the user.
 * Multi-outcome markets: the proposer names 2-4 outcomes, the admin prices
 * each and sets the wagering deadline at approval, students back exactly one
 * outcome per bet, and grading pays every wager on the winning outcome.
 */
public class BetService {

    /** odds is DECIMAL(6,2): anything above this overflows the column. */
    private static final BigDecimal MAX_ODDS = new BigDecimal("9999.99");
    private static final int MIN_OUTCOMES = 2;
    private static final int MAX_OUTCOMES = 4;

    private final BetDAO betDAO = new BetDAO();
    private final EventDAO eventDAO = new EventDAO();
    private final AccountDAO accountDAO = new AccountDAO();
    private final UserDAO userDAO = new UserDAO();   // read-only use: B300 admin check
    private final NotificationDAO notificationDAO = new NotificationDAO();

    /**
     * B700: the bets a student can currently wager on, outcomes and deadline
     * included. Past-deadline bets stay listed (they read as closed) until the
     * admin grades them.
     */
    public List<Bet> viewActiveBets() throws SQLException {
        return betDAO.findActiveBets();
    }

    /** B300: the review queue for the admin "Accounting System" screen. */
    public List<Bet> viewProposedBets() throws SQLException {
        return betDAO.findProposedBets();
    }

    /**
     * B200: a student proposes a bet with the 2-4 outcomes it can end in
     * (e.g. "Madibaz win" / "Wits win" / "Draw"). It stays PROPOSED (invisible
     * to B700) until an admin prices each outcome and sets the deadline (B300).
     * The event link is optional, but when given it must point at an event
     * that can still happen. Returns the new betID.
     */
    public int proposeBet(int userID, Integer eventID, String description,
                          List<String> outcomeLabels) throws SQLException {
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("A proposal needs a description.");
        }
        List<String> labels = cleanOutcomeLabels(outcomeLabels);
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
        return betDAO.propose(b, labels);
    }

    /** 2-4 trimmed, non-blank, case-insensitively distinct labels of sane length. */
    private List<String> cleanOutcomeLabels(List<String> raw) {
        if (raw == null) {
            throw new IllegalArgumentException("A proposal needs its possible outcomes.");
        }
        List<String> labels = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (String s : raw) {
            if (s == null || s.isBlank()) continue;
            String label = s.trim();
            if (label.length() > 100) {
                throw new IllegalArgumentException("Outcome labels must be 100 characters or fewer.");
            }
            if (!seen.add(label.toLowerCase())) {
                throw new IllegalArgumentException("Outcome '" + label + "' is listed twice.");
            }
            labels.add(label);
        }
        if (labels.size() < MIN_OUTCOMES || labels.size() > MAX_OUTCOMES) {
            throw new IllegalArgumentException(
                    "A bet needs between " + MIN_OUTCOMES + " and " + MAX_OUTCOMES + " outcomes.");
        }
        return labels;
    }

    /**
     * B300: an admin approves a proposal — prices every outcome and sets the
     * wagering deadline, which must be in the future. Odds are decimal ("for
     * one"): 1.00 would pay back exactly the stake, so they must be strictly
     * greater. Every outcome of the bet must be priced — no half-priced
     * markets (that was the old Yes-only model's flaw).
     */
    public void approveProposal(int betID, Map<Integer, BigDecimal> oddsByOutcomeID,
                                LocalDateTime deadline, int adminUserID) throws SQLException {
        requireAdmin(adminUserID, "review bet proposals");
        if (deadline == null || !deadline.isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("The wagering deadline must be in the future.");
        }
        for (BigDecimal odds : oddsByOutcomeID.values()) {
            if (odds == null || odds.compareTo(BigDecimal.ONE) <= 0 || odds.compareTo(MAX_ODDS) > 0) {
                throw new IllegalArgumentException("Each outcome's odds must be between 1.01 and " + MAX_ODDS + ".");
            }
        }
        Bet b = betDAO.findById(betID);
        if (b == null) {
            throw new IllegalArgumentException("Bet " + betID + " does not exist.");
        }
        Set<Integer> expected = new HashSet<>();
        for (BetOutcome o : b.getOutcomes()) {
            expected.add(o.getOutcomeID());
        }
        if (!expected.equals(oddsByOutcomeID.keySet())) {
            throw new IllegalArgumentException("Odds are required for every outcome of bet " + betID + ".");
        }
        if (!betDAO.activate(betID, deadline, oddsByOutcomeID)) {
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
     * B100: a student backs ONE outcome of an ACTIVE bet before its deadline.
     * One Wager row per student per bet (UNIQUE constraint); any number of
     * students can ride the same market. The stake leaves the student's
     * Account immediately; amountToBeWon = stake × the chosen outcome's odds
     * (decimal odds — payout includes the stake back). Debit, wager row, and
     * audit row commit as ONE DB transaction.
     */
    public void placeBet(int betID, int outcomeID, int userID, BigDecimal stake) throws SQLException {
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
        if (b.getDeadline() != null && !b.getDeadline().isAfter(LocalDateTime.now())) {
            throw new IllegalArgumentException("Bet " + betID + " closed for wagering on " + b.getDeadline() + ".");
        }
        BetOutcome chosen = b.getOutcomes().stream()
                .filter(o -> o.getOutcomeID() == outcomeID)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Outcome " + outcomeID + " does not belong to bet " + betID + "."));
        if (chosen.getOdds() == null) {
            throw new IllegalArgumentException("Outcome '" + chosen.getLabel() + "' has no odds — not open for wagering.");
        }
        BigDecimal payout = stake.multiply(chosen.getOdds()).setScale(2, RoundingMode.HALF_UP);

        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                if (!accountDAO.adjustBalance(con, userID, stake.negate())) {
                    throw new IllegalArgumentException("Insufficient MadiBucks for a stake of " + stake + ".");
                }
                if (!betDAO.placeWager(con, betID, outcomeID, userID, stake, payout, LocalDateTime.now())) {
                    // The SQL guard found nothing: closed, deadline passed, or bad outcome.
                    throw new IllegalArgumentException("Bet " + betID + " closed in the meantime.");
                }
                accountDAO.logTransaction(con, userID, stake.negate(),
                        "B100: user " + userID + " staked " + stake + " on '" + chosen.getLabel()
                        + "' (bet " + betID + ")");
                con.commit();
            } catch (SQLIntegrityConstraintViolationException e) {
                con.rollback();
                // UNIQUE (betID, userID): same student, second wager
                throw new IllegalArgumentException("You already have a wager on bet " + betID + ".");
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    /**
     * B400 + B600: an admin records which outcome won (or cancels the market)
     * and every wager settles in the same DB transaction.
     * winner given -> wagers on that outcome are paid their amountToBeWon;
     *                all other wagers lost (stakes left at placement).
     * cancelled    -> every stake refunded.
     * A never-wagered bet can still be graded — the market simply closes.
     * Grading is allowed before the deadline too — the admin may know the
     * result early (a cancelled fixture, a released mark).
     */
    public void gradeBet(int betID, Integer winningOutcomeID, boolean cancelled,
                         int adminUserID) throws SQLException {
        requireAdmin(adminUserID, "grade bets");
        if (cancelled == (winningOutcomeID != null)) {
            throw new IllegalArgumentException("Grade with either a winning outcome or cancelled — exactly one.");
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
                if (!betDAO.grade(con, betID, winningOutcomeID, adminUserID, LocalDateTime.now())) {
                    // Guard failed: raced by another grade/delete, or winner not on this bet.
                    throw new IllegalArgumentException(
                            "Could not grade bet " + betID + " — check the winning outcome and try again.");
                }
                // outcomeID -> label, for the settlement notifications below
                Map<Integer, String> labels = new java.util.HashMap<>();
                for (BetOutcome o : b.getOutcomes()) {
                    labels.put(o.getOutcomeID(), o.getLabel());
                }
                String winnerLabel = cancelled ? null : labels.get(winningOutcomeID);
                for (Wager w : betDAO.findWagersByBet(con, betID)) {
                    // Settle the money, then queue the login notification (B400 step 3:
                    // "Users are informed of the outcome") — same transaction, so the
                    // message can never describe a payout that did not happen.
                    if (cancelled) {
                        payOut(con, w.getUserID(), w.getStake(),
                                "B600: bet " + betID + " cancelled, stake refunded to user " + w.getUserID());
                        notificationDAO.insert(con, w.getUserID(),
                                "Bet cancelled: '" + b.getDescription() + "' — your stake of "
                                + w.getStake() + " MB was refunded.");
                    } else if (w.getOutcomeID() == winningOutcomeID) {
                        payOut(con, w.getUserID(), w.getAmountToBeWon(),
                                "B600: bet " + betID + " won, paid user " + w.getUserID());
                        notificationDAO.insert(con, w.getUserID(),
                                "You won! '" + winnerLabel + "' came in on '" + b.getDescription()
                                + "' — " + w.getAmountToBeWon() + " MB paid to your account.");
                    } else {
                        // losing wagers: stake already left the account at placement
                        notificationDAO.insert(con, w.getUserID(),
                                "Bet settled: '" + b.getDescription() + "' ended '" + winnerLabel
                                + "'. Your pick '" + labels.getOrDefault(w.getOutcomeID(), "?")
                                + "' did not come in.");
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
     * B700 already filters it out. Every live wager is refunded in the same
     * transaction so an admin deletion never costs a student their stake.
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
                if ("ACTIVE".equals(b.getStatus())) {
                    for (Wager w : betDAO.findWagersByBet(con, betID)) {
                        payOut(con, w.getUserID(), w.getStake(),
                                "B500: bet " + betID + " deleted, stake refunded to user " + w.getUserID());
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

    /** Credit within the caller's transaction + audit row. A positive credit only
     *  fails when the Account row is missing — that is data corruption, not user error. */
    private void payOut(Connection con, int userID, BigDecimal amount, String description) throws SQLException {
        if (!accountDAO.adjustBalance(con, userID, amount)) {
            throw new IllegalStateException("User " + userID + " has no Account row to receive " + amount + ".");
        }
        accountDAO.logTransaction(con, userID, amount, description);
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

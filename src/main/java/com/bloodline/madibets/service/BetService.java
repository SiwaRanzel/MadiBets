package com.bloodline.madibets.service;

import com.bloodline.madibets.dao.BetDAO;
import com.bloodline.madibets.dao.EventDAO;
import com.bloodline.madibets.dao.UserDAO;
import com.bloodline.madibets.model.Bet;
import com.bloodline.madibets.model.Event;
import com.bloodline.madibets.model.User;
import java.math.BigDecimal;
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
            // The guarded UPDATE matched nothing — find out why for a useful message.
            Bet b = betDAO.findById(betID);
            if (b == null) {
                throw new IllegalArgumentException("Bet " + betID + " does not exist.");
            }
            throw new IllegalArgumentException(
                    "Bet " + betID + " is " + b.getStatus() + ", not PROPOSED — nothing to review.");
        }
    }
}

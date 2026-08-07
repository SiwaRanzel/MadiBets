package com.bloodline.madibets.service;

import com.bloodline.madibets.dao.BetDAO;
import com.bloodline.madibets.model.Bet;
import java.sql.SQLException;
import java.util.List;

/**
 * Owner: Kieran (B-series). Business rules for the bet lifecycle; SQL stays in BetDAO.
 * Follows the AuthService pattern.
 */
public class BetService {

    private final BetDAO betDAO = new BetDAO();

    /**
     * B700: the bets a student can currently wager on.
     * No business rules yet — the 'ACTIVE' filter is the whole use case. Stake
     * validation (B100) and payout logic (B400/B600) land here later.
     */
    public List<Bet> viewActiveBets() throws SQLException {
        return betDAO.findActiveBets();
    }
}

package com.bloodline.madibets.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import com.bloodline.madibets.dao.LeaderboardDAO;

/**
 * Owner: Jason (C-series).
 * Business logic for bet history, rankings, and scheduled updates.
 */
public class LeaderboardService {

    private final LeaderboardDAO leaderboardDAO = new LeaderboardDAO();

    // ------------------------------------------------------------------
    // C400 — Get bet history for a user
    // ------------------------------------------------------------------
    public List<Map<String, Object>> getBetHistory(int userID) throws SQLException {
        return leaderboardDAO.findBetsByUser(userID);
    }

    // ------------------------------------------------------------------
    // C500 — Get leaderboard rankings (top N users)
    // sortBy: "balance", "wins", or "totalBets"
    // ------------------------------------------------------------------
    public List<Map<String, Object>> getRankings(int limit, String sortBy) throws SQLException {
        return getRankings(limit, sortBy, "all", 0, 0);
    }

    /**
     * Scoped rankings. scope: "all", "friends" (needs userID), or "group" (needs groupID).
     */
    public List<Map<String, Object>> getRankings(int limit, String sortBy,
                                                  String scope, int userID, int groupID) throws SQLException {
        if (limit <= 0) limit = 50;
        if (sortBy == null || sortBy.isBlank()) sortBy = "balance";
        if (scope == null || scope.isBlank()) scope = "all";
        return leaderboardDAO.getRankings(limit, sortBy, scope, userID, groupID);
    }

    // ------------------------------------------------------------------
    // C500 — Get a specific user's rank and stats
    // ------------------------------------------------------------------
    public Map<String, Object> getUserStats(int userID) throws SQLException {
        Map<String, Object> stats = leaderboardDAO.getUserBetStats(userID);
        int rank = leaderboardDAO.getUserRank(userID);
        stats.put("rank", rank);
        stats.put("totalPlayers", leaderboardDAO.getTotalPlayers());
        stats.put("weeklyGrowth", leaderboardDAO.getWeeklyBalanceGrowth(userID));
        return stats;
    }

    // ------------------------------------------------------------------
    // C600 — Grant monthly allowance to all users
    // Returns the number of accounts updated.
    // ------------------------------------------------------------------
    public int grantMonthlyAllowance() throws SQLException {
        return leaderboardDAO.grantMonthlyAllowance();
    }
}

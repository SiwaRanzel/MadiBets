package com.bloodline.madibets.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bloodline.madibets.config.DatabaseConnection;

/**
 * Owner: Jason (C-series).
 * Handles bet history retrieval (C400), leaderboard rankings (C500),
 * and leaderboard recalculation (C600).
 */
public class LeaderboardDAO {

    // ------------------------------------------------------------------
    // C400 — View Bet History for a user
    // Returns all bets placed by a given user, newest first.
    // ------------------------------------------------------------------
    public List<Map<String, Object>> findBetsByUser(int userID) throws SQLException {
        // Multi-outcome markets: a user's history is their Wager rows joined to
        // the outcome they backed. The outcome column keeps its old UI values
        // (YES = won, NO = lost) derived from whether their pick was the winner.
        String sql = "SELECT b.betID, b.description, o.odds, w.amountToBeWon, "
                   + "CASE WHEN b.outcome = 'PENDING' THEN 'PENDING' "
                   + "     WHEN b.outcome = 'CANCELLED' THEN 'CANCELLED' "
                   + "     WHEN w.outcomeID = b.winningOutcomeID THEN 'YES' ELSE 'NO' END AS outcome, "
                   + "o.label AS pick, "
                   + "w.placedDate, b.gradedDate, e.eventDescription "
                   + "FROM Wager w "
                   + "JOIN Bet b ON w.betID = b.betID "
                   + "JOIN BetOutcome o ON w.outcomeID = o.outcomeID "
                   + "LEFT JOIN Event e ON b.eventID = e.eventID "
                   + "WHERE w.userID = ? "
                   + "ORDER BY w.placedDate DESC";
        List<Map<String, Object>> bets = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> bet = new HashMap<>();
                    bet.put("betID", rs.getInt("betID"));
                    bet.put("description", rs.getString("description"));
                    bet.put("odds", rs.getDouble("odds"));
                    bet.put("amountToBeWon", rs.getDouble("amountToBeWon"));
                    bet.put("outcome", rs.getString("outcome"));
                    bet.put("pick", rs.getString("pick"));
                    bet.put("placedDate", rs.getString("placedDate"));
                    bet.put("gradedDate", rs.getString("gradedDate"));
                    bet.put("eventDescription", rs.getString("eventDescription"));
                    bets.add(bet);
                }
            }
        }
        return bets;
    }

    // ------------------------------------------------------------------
    // C500 — View Leaderboard / Rankings
    // Ranks users by the given criterion: "balance", "wins", or "totalBets".
    // Returns top N users with rank, name, balance, and bet stats.
    // ------------------------------------------------------------------
    public List<Map<String, Object>> getRankings(int limit, String sortBy) throws SQLException {
        // Multi-outcome markets: a win = the user's wager backed the winning outcome.
        String winsSub  = "(SELECT COUNT(*) FROM Wager w JOIN Bet b ON w.betID = b.betID "
                        + "WHERE w.userID = u.userID AND b.outcome = 'DECIDED' "
                        + "AND w.outcomeID = b.winningOutcomeID)";
        String totalSub = "(SELECT COUNT(*) FROM Wager w WHERE w.userID = u.userID)";
        String orderClause;
        switch (sortBy) {
            case "wins":
                orderClause = winsSub + " DESC";
                break;
            case "totalBets":
                orderClause = totalSub + " DESC";
                break;
            default: // "balance"
                orderClause = "a.balance DESC";
                break;
        }

        String sql = "SELECT u.userID, u.name, u.surname, u.userType, a.balance, "
                   + totalSub + " AS totalBets, "
                   + winsSub + " AS betsWon "
                   + "FROM User u "
                   + "JOIN Account a ON u.userID = a.userID "
                   + "WHERE u.userType = 'STUDENT' "
                   + "ORDER BY " + orderClause + " "
                   + "LIMIT ?";
        List<Map<String, Object>> rankings = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                int rank = 1;
                while (rs.next()) {
                    Map<String, Object> entry = new HashMap<>();
                    entry.put("rank", rank++);
                    entry.put("userID", rs.getInt("userID"));
                    entry.put("name", rs.getString("name") + " " + rs.getString("surname"));
                    entry.put("userType", rs.getString("userType"));
                    entry.put("balance", rs.getDouble("balance"));
                    entry.put("totalBets", rs.getInt("totalBets"));
                    entry.put("betsWon", rs.getInt("betsWon"));
                    rankings.add(entry);
                }
            }
        }
        return rankings;
    }

    // ------------------------------------------------------------------
    // C500 — Get a specific user's rank position
    // ------------------------------------------------------------------
    public int getUserRank(int userID) throws SQLException {
        String sql = "SELECT COUNT(*) + 1 AS userRank "
                   + "FROM Account a JOIN User u ON a.userID = u.userID "
                   + "WHERE u.userType = 'STUDENT' AND a.balance > (SELECT balance FROM Account WHERE userID = ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("userRank") : 0;
            }
        }
    }

    public int getTotalPlayers() throws SQLException {
        String sql = "SELECT COUNT(*) FROM User WHERE userType = 'STUDENT'";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public double getWeeklyBalanceGrowth(int userID) throws SQLException {
        // Find total payout from transactions in the last 7 days for this user's account
        String sql = "SELECT SUM(t.payoutAmount) "
                   + "FROM `Transaction` t "
                   + "JOIN Account a ON t.accountID = a.accountID "
                   + "WHERE a.userID = ? AND t.`date` >= DATE_SUB(NOW(), INTERVAL 7 DAY)";
        
        double recentGains = 0;
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) recentGains = rs.getDouble(1);
            }
        }
        
        // Find current balance
        String balSql = "SELECT balance FROM Account WHERE userID = ?";
        double currentBalance = 0;
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(balSql)) {
            ps.setInt(1, userID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) currentBalance = rs.getDouble(1);
            }
        }
        
        double oldBalance = currentBalance - recentGains;
        if (oldBalance <= 0) {
            return recentGains > 0 ? 100.0 : 0.0;
        }
        return (recentGains / oldBalance) * 100.0;
    }

    // ------------------------------------------------------------------
    // C500 — Get user's bet stats (wins, losses, pending)
    // ------------------------------------------------------------------
    public Map<String, Object> getUserBetStats(int userID) throws SQLException {
        // Multi-outcome markets: win = backed the winning outcome; loss = the
        // market was decided and the pick was not the winner.
        String sql = "SELECT "
                   + "COUNT(*) AS totalBets, "
                   + "SUM(CASE WHEN b.outcome = 'DECIDED' AND w.outcomeID = b.winningOutcomeID THEN 1 ELSE 0 END) AS wins, "
                   + "SUM(CASE WHEN b.outcome = 'DECIDED' AND w.outcomeID <> b.winningOutcomeID THEN 1 ELSE 0 END) AS losses, "
                   + "SUM(CASE WHEN b.outcome = 'PENDING' THEN 1 ELSE 0 END) AS pending "
                   + "FROM Wager w JOIN Bet b ON w.betID = b.betID WHERE w.userID = ?";
        Map<String, Object> stats = new HashMap<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    stats.put("totalBets", rs.getInt("totalBets"));
                    stats.put("wins", rs.getInt("wins"));
                    stats.put("losses", rs.getInt("losses"));
                    stats.put("pending", rs.getInt("pending"));
                } else {
                    stats.put("totalBets", 0);
                    stats.put("wins", 0);
                    stats.put("losses", 0);
                    stats.put("pending", 0);
                }
            }
        }
        return stats;
    }

    // ------------------------------------------------------------------
    // C600 — Scheduled Leaderboard Update
    // This recalculates rankings. In the current schema, rankings are
    // derived live from Account balance, so this method can be used
    // for any periodic maintenance (e.g., monthly MadiBucks allowance).
    // Awards 100 MadiBucks to all users (monthly reset/allowance).
    // ------------------------------------------------------------------
    public int grantMonthlyAllowance() throws SQLException {
        String sql = "UPDATE Account SET balance = balance + 100.00";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            return ps.executeUpdate(); // returns number of accounts updated
        }
    }
}

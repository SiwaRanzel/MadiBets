package com.bloodline.madibets.dao;

import com.bloodline.madibets.config.DatabaseConnection;
import com.bloodline.madibets.model.Account;
import java.math.BigDecimal;
import java.sql.*;

/** Owner: Kieran (B-series). Shared with Jason's leaderboard reads — agree the API early. */
public class AccountDAO {

    /** Balance read used by B100 and C400/C500. Returns null if the user has no account. */
    public Account findByUserId(int userID) throws SQLException {
        String sql = "SELECT accountID, balance, userID FROM Account WHERE userID = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userID);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Account a = new Account();
                a.setAccountID(rs.getInt("accountID"));
                a.setBalance(rs.getBigDecimal("balance"));
                a.setUserID(rs.getInt("userID"));
                return a;
            }
        }
    }

    /**
     * B100/B600 balance change (UPDATE). Takes the caller's Connection because a
     * balance change never stands alone — it commits together with the wager
     * update (B100) or the payout + Transaction row (B600). The "balance + delta
     * >= 0" guard stops overdrafts inside the database itself; returns false when
     * the account is missing or the funds are insufficient.
     */
    public boolean adjustBalance(Connection con, int userID, BigDecimal delta) throws SQLException {
        String sql = "UPDATE Account SET balance = balance + ? WHERE userID = ? AND balance + ? >= 0";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, delta);
            ps.setInt(2, userID);
            ps.setBigDecimal(3, delta);
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * Audit row for every balance change (CREATE; the simulated accounting trail).
     * Same transaction as the adjustBalance it records. Table name is backticked —
     * TRANSACTION is a MySQL keyword. NOTE: the Transaction table has no user or
     * account FK (schema DESIGN NOTES #2), so the link lives in the description
     * text until the team agrees to add the column.
     */
    public int logTransaction(Connection con, BigDecimal payoutAmount, String description) throws SQLException {
        String sql = "INSERT INTO `Transaction` (payoutAmount, description) VALUES (?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setBigDecimal(1, payoutAmount);
            ps.setString(2, description);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }
}

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
     * readBalance(int userID) -> used by C400/C500
     * Reads the current balance for the given user.
     */
    public BigDecimal readBalance(int userID) throws SQLException {
        String sql = "SELECT balance FROM Account WHERE userID = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBigDecimal("balance");
                }
            }
        }
        return null;
    }

    /**
     * B600 Update MadiBuck Balance (UPDATE)
     * Adjusts the account balance and inserts a transaction record in one transaction.
     */
    public boolean adjustBalance(int userID, BigDecimal delta, String description) throws SQLException {
        String getAccountSql = "SELECT accountID, balance FROM Account WHERE userID = ? FOR UPDATE";
        String updateBalanceSql = "UPDATE Account SET balance = balance + ? WHERE accountID = ?";
        String insertTxnSql = "INSERT INTO `Transaction` (payoutAmount, description, accountID) VALUES (?, ?, ?)";

        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            int accountID = -1;

            try (PreparedStatement ps = con.prepareStatement(getAccountSql)) {
                ps.setInt(1, userID);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        accountID = rs.getInt("accountID");
                    } else {
                        // Account not found for user
                        con.rollback();
                        return false;
                    }
                }
            }

            try (PreparedStatement ps = con.prepareStatement(updateBalanceSql)) {
                ps.setBigDecimal(1, delta);
                ps.setInt(2, accountID);
                int updated = ps.executeUpdate();
                if (updated == 0) {
                    con.rollback();
                    return false;
                }
            }

            try (PreparedStatement ps = con.prepareStatement(insertTxnSql)) {
                ps.setBigDecimal(1, delta);
                ps.setString(2, description);
                ps.setInt(3, accountID);
                ps.executeUpdate();
            }

            con.commit();
            return true;
        } catch (SQLException e) {
            if (con != null) {
                try {
                    con.rollback();
                } catch (SQLException ex) {
                    // ignore
                }
            }
            throw e;
        } finally {
            if (con != null) {
                try {
                    con.setAutoCommit(true);
                    con.close();
                } catch (SQLException ex) {
                    // ignore
                }
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
     * TRANSACTION is a MySQL keyword. Transaction.accountID is NOT NULL in the
     * merged schema, so the row is linked to the user's account via a subquery
     * inside the same transaction.
     */
    public int logTransaction(Connection con, int userID, BigDecimal payoutAmount, String description) throws SQLException {
        String sql = "INSERT INTO `Transaction` (payoutAmount, description, accountID) "
                   + "SELECT ?, ?, accountID FROM Account WHERE userID = ?";
        try (PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setBigDecimal(1, payoutAmount);
            ps.setString(2, description);
            ps.setInt(3, userID);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }
}

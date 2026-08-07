package com.bloodline.madibets.dao;

import com.bloodline.madibets.config.DatabaseConnection;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/** Owner: Kieran (B-series). Shared with Jason's leaderboard reads — agree the API early. */
public class AccountDAO {
    
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
}

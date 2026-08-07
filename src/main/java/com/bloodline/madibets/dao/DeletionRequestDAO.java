package com.bloodline.madibets.dao;

import com.bloodline.madibets.config.DatabaseConnection;
import com.bloodline.madibets.model.AccountDeletionRequest;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

public class DeletionRequestDAO {

    public AccountDeletionRequest create(int userID) throws SQLException {
        String sql = "INSERT INTO AccountDeletionRequest (userID, status) VALUES (?, 'NEW')";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, userID);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    AccountDeletionRequest req = new AccountDeletionRequest();
                    req.setRequestID(rs.getInt(1));
                    req.setUserID(userID);
                    req.setStatus("NEW");
                    // requestDate would require selecting back, omitting for brevity
                    return req;
                }
            }
        }
        return null;
    }

    public List<AccountDeletionRequest> getAll() throws SQLException {
        String sql = "SELECT r.requestID, r.userID, r.requestDate, r.status, u.name, u.surname, u.email " +
                     "FROM AccountDeletionRequest r " +
                     "JOIN User u ON r.userID = u.userID " +
                     "ORDER BY r.requestDate DESC";
        List<AccountDeletionRequest> list = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                AccountDeletionRequest req = new AccountDeletionRequest();
                req.setRequestID(rs.getInt("requestID"));
                req.setUserID(rs.getInt("userID"));
                req.setRequestDate(rs.getTimestamp("requestDate"));
                req.setStatus(rs.getString("status"));
                req.setUserName(rs.getString("name") + " " + rs.getString("surname"));
                req.setUserEmail(rs.getString("email"));
                list.add(req);
            }
        }
        return list;
    }

    public boolean updateStatus(int requestID, String status) throws SQLException {
        String sql = "UPDATE AccountDeletionRequest SET status = ? WHERE requestID = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, requestID);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean hasPendingDeleteRequest(int userID) throws SQLException {
        String sql = "SELECT 1 FROM AccountDeletionRequest WHERE userID = ? AND status = 'NEW' LIMIT 1";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }
}

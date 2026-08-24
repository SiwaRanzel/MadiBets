package com.bloodline.madibets.dao;

import com.bloodline.madibets.config.DatabaseConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Owner: Kieran (B-series). Login-time messages — currently written by bet
 * settlement (B400/B600) so every wagerer learns how their bet ended.
 */
public class NotificationDAO {

    /**
     * Queue a message for one user. Takes the caller's Connection: settlement
     * notifications must commit (or roll back) together with the payouts they
     * describe — a notification about money that never moved would be a lie.
     */
    public void insert(Connection con, int userID, String message) throws SQLException {
        String sql = "INSERT INTO Notification (userID, message) VALUES (?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userID);
            // Column is VARCHAR(255) — trim rather than fail on a long bet description.
            ps.setString(2, message.length() > 255 ? message.substring(0, 252) + "..." : message);
            ps.executeUpdate();
        }
    }

    /** Everything the user has not seen yet, oldest first (reads as a timeline). */
    public List<Map<String, Object>> findUnseen(int userID) throws SQLException {
        String sql = "SELECT notificationID, message, createdDate FROM Notification "
                   + "WHERE userID = ? AND seen = 0 ORDER BY notificationID ASC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userID);
            try (ResultSet rs = ps.executeQuery()) {
                List<Map<String, Object>> out = new ArrayList<>();
                while (rs.next()) {
                    Map<String, Object> n = new HashMap<>();
                    n.put("notificationID", rs.getInt("notificationID"));
                    n.put("message", rs.getString("message"));
                    n.put("createdDate", rs.getString("createdDate"));
                    out.add(n);
                }
                return out;
            }
        }
    }

    /** Flag every unseen message as seen — called once the user has been shown them. */
    public int markAllSeen(int userID) throws SQLException {
        String sql = "UPDATE Notification SET seen = 1 WHERE userID = ? AND seen = 0";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userID);
            return ps.executeUpdate();
        }
    }
}

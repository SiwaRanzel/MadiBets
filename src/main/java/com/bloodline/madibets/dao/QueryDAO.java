package com.bloodline.madibets.dao;

import com.bloodline.madibets.config.DatabaseConnection;
import com.bloodline.madibets.model.Query;

import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Owner: Pieter (D-series). */
public class QueryDAO {
    // D600 Log Query (CREATE)      -> create(...)
    // D700 Review Query (READ/UPDATE) -> findAll() / updateStatus(int queryID, String status)

    public int create(Query query) throws SQLException {
        String sql = "INSERT INTO `Query` (title, description, userID, resolvedStatus, queryDate) VALUES (?, ?, ?, ?, NOW())";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, query.getTitle());
            ps.setString(2, query.getDescription());
            ps.setInt(3, query.getUserID());
            ps.setString(4, "OPEN");
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    public List<Map<String, Object>> findAllWithUser() throws SQLException {
        List<Map<String, Object>> results = new ArrayList<>();
        String sql = "SELECT q.*, u.email FROM `Query` q JOIN `User` u ON q.userID = u.userID ORDER BY q.queryDate DESC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Map<String, Object> map = new HashMap<>();
                map.put("queryID", rs.getInt("queryID"));
                map.put("title", rs.getString("title"));
                map.put("description", rs.getString("description"));
                map.put("queryDate", rs.getTimestamp("queryDate"));
                map.put("userID", rs.getInt("userID"));
                map.put("resolvedStatus", rs.getString("resolvedStatus"));
                map.put("email", rs.getString("email"));
                results.add(map);
            }
        }
        return results;
    }

    public boolean updateStatus(int queryID, String status) throws SQLException {
        String sql = "UPDATE `Query` SET resolvedStatus = ? WHERE queryID = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, queryID);
            return ps.executeUpdate() > 0;
        }
    }
}

package com.bloodline.madibets.dao;

import com.bloodline.madibets.config.DatabaseConnection;
import com.bloodline.madibets.model.Query;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/** Owner: Pieter (D-series). */
public class QueryDAO {
    // D600 Log Query (CREATE)      -> create(...)
    // D700 Review Query (READ/UPDATE) -> findAll() / resolve(int queryID)

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
}

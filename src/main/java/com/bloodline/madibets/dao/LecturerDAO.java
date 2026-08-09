package com.bloodline.madibets.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.bloodline.madibets.config.DatabaseConnection;
import com.bloodline.madibets.model.LecturerStats;

public class LecturerDAO {

    public LecturerStats getDashboardStats(int lecturerId) {
        LecturerStats stats = new LecturerStats();

        String sqlGroups = "SELECT COUNT(*) AS c FROM `Group` WHERE createdBy = ?";
        String sqlTotalStudents = "SELECT COUNT(*) AS c FROM User WHERE userType = 'STUDENT'";
        String sqlNewStudents = "SELECT COUNT(*) AS c FROM User WHERE userType = 'STUDENT' AND createdDate >= DATE_SUB(CURDATE(), INTERVAL 7 DAY)";
        
        // Active today: students who completed a task today
        // A more comprehensive query could also include bets placed.
        String sqlActiveToday = "SELECT COUNT(DISTINCT userID) AS c FROM TaskCompletion WHERE DATE(completionDate) = CURDATE()";

        try (Connection con = DatabaseConnection.getConnection()) {
            // 1. Total Groups
            try (PreparedStatement ps = con.prepareStatement(sqlGroups)) {
                ps.setInt(1, lecturerId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        stats.setTotalGroups(rs.getInt("c"));
                    }
                }
            }

            // 2. Total Students
            try (PreparedStatement ps = con.prepareStatement(sqlTotalStudents)) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        stats.setTotalStudents(rs.getInt("c"));
                    }
                }
            }

            // 3. New Students This Week
            try (PreparedStatement ps = con.prepareStatement(sqlNewStudents)) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        stats.setNewStudentsThisWeek(rs.getInt("c"));
                    }
                }
            }

            // 4. Active Today
            try (PreparedStatement ps = con.prepareStatement(sqlActiveToday)) {
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        stats.setActiveToday(rs.getInt("c"));
                    }
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return stats;
    }
}

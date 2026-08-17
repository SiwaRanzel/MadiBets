package com.bloodline.madibets.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import com.bloodline.madibets.config.DatabaseConnection;
import com.bloodline.madibets.model.DashboardStats;

public class AdminDAO {

    public DashboardStats getDashboardStats() throws SQLException {
        DashboardStats stats = new DashboardStats();
        
        // Use single connection
        try (Connection con = DatabaseConnection.getConnection()) {
            
            // 1. Bets Proposed Today
            String q1 = "SELECT COUNT(*) FROM Bet WHERE DATE(proposedDate) = CURDATE()";
            try (PreparedStatement ps = con.prepareStatement(q1); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) stats.setBetsProposedToday(rs.getInt(1));
            }
            
            // 2. Bets Placed Today
            String q2 = "SELECT COUNT(*) FROM Wager WHERE DATE(placedDate) = CURDATE()";
            try (PreparedStatement ps = con.prepareStatement(q2); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) stats.setBetsPlacedToday(rs.getInt(1));
            }

            // 3. Bets Pending Manual Review (PROPOSED status)
            String q3 = "SELECT COUNT(*) FROM Bet WHERE status = 'PROPOSED'";
            try (PreparedStatement ps = con.prepareStatement(q3); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) stats.setBetsPendingReview(rs.getInt(1));
            }

            // 4. Upcoming Events
            String q4 = "SELECT COUNT(*) FROM Event WHERE status = 'UPCOMING'";
            try (PreparedStatement ps = con.prepareStatement(q4); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) stats.setUpcomingEvents(rs.getInt(1));
            }

            // 5. Total Registered Users (Excluding Admins)
            String q5 = "SELECT COUNT(*) FROM User WHERE userType != 'ADMIN'";
            try (PreparedStatement ps = con.prepareStatement(q5); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) stats.setTotalUsers(rs.getInt(1));
            }

            // 6. Users Joined Today (Excluding Admins)
            String q6 = "SELECT COUNT(*) FROM User WHERE DATE(createdDate) = CURDATE() AND userType != 'ADMIN'";
            try (PreparedStatement ps = con.prepareStatement(q6); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) stats.setUsersJoinedToday(rs.getInt(1));
            }

            // 7. Open Support Queries
            String q7 = "SELECT COUNT(*) FROM Query WHERE resolvedStatus = 'OPEN'";
            try (PreparedStatement ps = con.prepareStatement(q7); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) stats.setOpenSupportQueries(rs.getInt(1));
            }

            // 8. Users Rewarded Bonus MadiBucks Today
            String q8 = "SELECT COUNT(DISTINCT userID) FROM TaskCompletion WHERE completionStatus = 'COMPLETED' AND DATE(completionDate) = CURDATE()";
            try (PreparedStatement ps = con.prepareStatement(q8); ResultSet rs = ps.executeQuery()) {
                if (rs.next()) stats.setUsersRewardedToday(rs.getInt(1));
            }
        }
        
        return stats;
    }
}

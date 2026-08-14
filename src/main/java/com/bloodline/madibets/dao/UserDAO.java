package com.bloodline.madibets.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.ArrayList;
import com.bloodline.madibets.config.DatabaseConnection;
import com.bloodline.madibets.model.User;

/**
 * Owner: Siwapiwe (A-series). The template every other DAO copies:
 * one PreparedStatement per CRUD operation. register() and findByEmail()
 * are worked examples; the rest are stubbed with their use-case mapping.
 */
public class UserDAO {

    /** A100 Register (CREATE). Returns the generated userID, or -1. */
    public int register(User u) throws SQLException {
        String insertUserSql = "INSERT INTO User (name, surname, email, password, userType) VALUES (?, ?, ?, ?, ?)";
        String insertStudentSql = "INSERT INTO Student (userID, studentNo) VALUES (?, ?)";
        String insertLecturerSql = "INSERT INTO Lecturer (userID, staffNo) VALUES (?, ?)";
        String insertAdminSql = "INSERT INTO Admin (userID) VALUES (?)";
        String insertAccountSql = "INSERT INTO Account (userID, balance) VALUES (?, 100.00)";

        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            int userID = -1;
            try (PreparedStatement ps = con.prepareStatement(insertUserSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, u.getName());
                ps.setString(2, u.getSurname());
                ps.setString(3, u.getEmail());
                ps.setString(4, u.getPassword());   // pass a bcrypt HASH (see AuthService)
                ps.setString(5, u.getUserType());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        userID = keys.getInt(1);
                    }
                }
            }

            if (userID == -1) {
                con.rollback();
                return -1;
            }

            String type = u.getUserType();
            if ("STUDENT".equalsIgnoreCase(type)) {
                try (PreparedStatement ps = con.prepareStatement(insertStudentSql)) {
                    ps.setInt(1, userID);
                    ps.setString(2, u.getStudentNo() != null ? u.getStudentNo() : "S" + userID);
                    ps.executeUpdate();
                }
            } else if ("LECTURER".equalsIgnoreCase(type)) {
                try (PreparedStatement ps = con.prepareStatement(insertLecturerSql)) {
                    ps.setInt(1, userID);
                    ps.setString(2, u.getStaffNo() != null ? u.getStaffNo() : "L" + userID);
                    ps.executeUpdate();
                }
            } else if ("ADMIN".equalsIgnoreCase(type)) {
                try (PreparedStatement ps = con.prepareStatement(insertAdminSql)) {
                    ps.setInt(1, userID);
                    ps.executeUpdate();
                }
            }

            try (PreparedStatement ps = con.prepareStatement(insertAccountSql)) {
                ps.setInt(1, userID);
                ps.executeUpdate();
            }

            con.commit();
            return userID;
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

    /** A200 Login (READ). Returns the matching user, or null. */
    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT u.userID, u.name, u.surname, u.email, u.password, u.userType, u.avatarPath, u.createdDate, "
                   + "s.studentNo, l.staffNo "
                   + "FROM User u "
                   + "LEFT JOIN Student s ON u.userID = s.userID "
                   + "LEFT JOIN Lecturer l ON u.userID = l.userID "
                   + "WHERE u.email = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** A300 View Profile (READ). */
    public User findById(int userID) throws SQLException {
        String sql = "SELECT u.userID, u.name, u.surname, u.email, u.password, u.userType, u.avatarPath, u.createdDate, "
                   + "s.studentNo, l.staffNo "
                   + "FROM User u "
                   + "LEFT JOIN Student s ON u.userID = s.userID "
                   + "LEFT JOIN Lecturer l ON u.userID = l.userID "
                   + "WHERE u.userID = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** View All Users (READ). */
    public List<User> getAllUsers() throws SQLException {
        String sql = "SELECT u.userID, u.name, u.surname, u.email, u.password, u.userType, u.avatarPath, u.createdDate, "
                   + "s.studentNo, l.staffNo "
                   + "FROM User u "
                   + "LEFT JOIN Student s ON u.userID = s.userID "
                   + "LEFT JOIN Lecturer l ON u.userID = l.userID";
        List<User> users = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                users.add(map(rs));
            }
        }
        return users;
    }

    /** A400 Update Profile (UPDATE). */
    public boolean updateProfile(User u) throws SQLException {
        String sql = "UPDATE User SET name=?, surname=?, email=? WHERE userID=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, u.getName());
            ps.setString(2, u.getSurname());
            ps.setString(3, u.getEmail());
            ps.setInt(4, u.getUserID());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean updateAvatar(int userID, String avatarPath) throws SQLException {
        String sql = "UPDATE User SET avatarPath=? WHERE userID=?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, avatarPath);
            ps.setInt(2, userID);
            return ps.executeUpdate() > 0;
        }
    }

    /** Fetch only the current avatar path for a user (null if none set). */
    public String getAvatarPath(int userID) throws SQLException {
        String sql = "SELECT avatarPath FROM User WHERE userID = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getString("avatarPath") : null;
            }
        }
    }
    /** A600 Delete User (DELETE). */
    public boolean delete(int userID) throws SQLException {
        String updateGroups = "UPDATE `Group` SET createdBy=1 WHERE createdBy=?";
        String updateTasks = "UPDATE Task SET createdBy=1 WHERE createdBy=?";
        String updateGradedBets = "UPDATE Bet SET gradedBy=1 WHERE gradedBy=?";
        String deleteBets = "DELETE FROM Bet WHERE userID=?";
        String deleteTransactions = "DELETE FROM Transaction WHERE accountID IN (SELECT accountID FROM Account WHERE userID=?)";
        String sql = "DELETE FROM User WHERE userID=?";
        
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps1 = con.prepareStatement(updateGroups)) {
                    ps1.setInt(1, userID);
                    ps1.executeUpdate();
                }
                try (PreparedStatement ps2 = con.prepareStatement(updateTasks)) {
                    ps2.setInt(1, userID);
                    ps2.executeUpdate();
                }
                try (PreparedStatement ps2b = con.prepareStatement(updateGradedBets)) {
                    ps2b.setInt(1, userID);
                    ps2b.executeUpdate();
                }
                try (PreparedStatement ps3 = con.prepareStatement(deleteBets)) {
                    ps3.setInt(1, userID);
                    ps3.executeUpdate();
                }
                try (PreparedStatement ps4 = con.prepareStatement(deleteTransactions)) {
                    ps4.setInt(1, userID);
                    ps4.executeUpdate();
                }
                try (PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, userID);
                    int affected = ps.executeUpdate();
                    con.commit();
                    return affected > 0;
                }
            } catch (SQLException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    public double getAccountBalance(int userID) throws SQLException {
        String sql = "SELECT balance FROM Account WHERE userID = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getDouble("balance") : 0.0;
            }
        }
    }

    /** Count all registered users. */
    public int countAllUsers() throws SQLException {
        String sql = "SELECT COUNT(*) FROM User WHERE userType != 'ADMIN'";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** Count users whose userType is STUDENT. */
    public int countStudents() throws SQLException {
        String sql = "SELECT COUNT(*) FROM User WHERE userType = 'STUDENT'";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    /** Calculate weekly growth percentage for users (or just students). */
    public double getWeeklyGrowth(boolean onlyStudents) throws SQLException {
        String filter = onlyStudents ? " AND userType = 'STUDENT'" : "";
        String recentSql = "SELECT COUNT(*) FROM User WHERE createdDate >= DATE_SUB(NOW(), INTERVAL 7 DAY)" + filter;
        String pastSql = "SELECT COUNT(*) FROM User WHERE createdDate < DATE_SUB(NOW(), INTERVAL 7 DAY)" + filter;
        
        int recent = 0;
        int past = 0;
        
        try (Connection con = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(recentSql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) recent = rs.getInt(1);
            }
            try (PreparedStatement ps = con.prepareStatement(pastSql);
                 ResultSet rs = ps.executeQuery()) {
                if (rs.next()) past = rs.getInt(1);
            }
        }
        
        if (past == 0) {
            return recent > 0 ? 100.0 : 0.0;
        }
        return ((double) recent / past) * 100.0;
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserID(rs.getInt("userID"));
        u.setName(rs.getString("name"));
        u.setSurname(rs.getString("surname"));
        u.setEmail(rs.getString("email"));
        u.setPassword(rs.getString("password"));
        u.setUserType(rs.getString("userType"));
        u.setStudentNo(rs.getString("studentNo"));
        u.setStaffNo(rs.getString("staffNo"));
        u.setAvatarPath(rs.getString("avatarPath"));
        u.setCreatedDate(rs.getString("createdDate"));
        return u;
    }
}

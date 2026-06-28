package com.bloodline.madibets.dao;

import com.bloodline.madibets.config.DatabaseConnection;
import com.bloodline.madibets.model.User;
import java.sql.*;

/**
 * Owner: Siwapiwe (A-series). The template every other DAO copies:
 * one PreparedStatement per CRUD operation. register() and findByEmail()
 * are worked examples; the rest are stubbed with their use-case mapping.
 */
public class UserDAO {

    /** A100 Register (CREATE). Returns the generated userID, or -1. */
    public int register(User u) throws SQLException {
        String sql = "INSERT INTO User (name, surname, email, password, userType) "
                   + "VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getName());
            ps.setString(2, u.getSurname());
            ps.setString(3, u.getEmail());
            ps.setString(4, u.getPassword());   // pass a bcrypt HASH (see AuthService)
            ps.setString(5, u.getUserType());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    /** A200 Login (READ). Returns the matching user, or null. */
    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT userID, name, surname, email, password, userType "
                   + "FROM User WHERE email = ?";
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
        String sql = "SELECT userID, name, surname, email, password, userType "
                   + "FROM User WHERE userID = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /** A400 Update Profile (UPDATE). TODO: implement. */
    public boolean updateProfile(User u) throws SQLException {
        // String sql = "UPDATE User SET name=?, surname=?, email=? WHERE userID=?";
        throw new UnsupportedOperationException("TODO A400");
    }

    /** A600 Delete User (DELETE). TODO: implement. */
    public boolean delete(int userID) throws SQLException {
        // String sql = "DELETE FROM User WHERE userID=?";
        throw new UnsupportedOperationException("TODO A600");
    }

    private User map(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserID(rs.getInt("userID"));
        u.setName(rs.getString("name"));
        u.setSurname(rs.getString("surname"));
        u.setEmail(rs.getString("email"));
        u.setPassword(rs.getString("password"));
        u.setUserType(rs.getString("userType"));
        return u;
    }
}

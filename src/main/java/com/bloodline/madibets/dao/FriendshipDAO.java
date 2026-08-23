package com.bloodline.madibets.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.bloodline.madibets.config.DatabaseConnection;
import com.bloodline.madibets.model.Friendship;

/**
 * Owner: Jason (C-series).
 * Handles all database operations for friendships.
 */
public class FriendshipDAO {

    // ------------------------------------------------------------------
    // C100 — View Friends (READ)
    // Returns all ACCEPTED friendships for a given user (both directions).
    // Joins with User table to get friend names for display.
    // ------------------------------------------------------------------
    public List<Friendship> findFriends(int userID) throws SQLException {
        String sql = "SELECT f.friendshipID, f.requesterID, f.addresseID, f.status, "
                   + "u1.name AS requesterName, u1.surname AS requesterSurname, "
                   + "u2.name AS addresseName, u2.surname AS addresseSurname, "
                   + "s1.studentNo AS requesterStudentNo, s2.studentNo AS addresseStudentNo "
                   + "FROM Friendship f "
                   + "JOIN User u1 ON f.requesterID = u1.userID "
                   + "JOIN User u2 ON f.addresseID = u2.userID "
                   + "LEFT JOIN Student s1 ON f.requesterID = s1.userID "
                   + "LEFT JOIN Student s2 ON f.addresseID = s2.userID "
                   + "WHERE (f.requesterID = ? OR f.addresseID = ?) "
                   + "AND f.status = 'ACCEPTED'";
        List<Friendship> friends = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userID);
            ps.setInt(2, userID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Friendship f = map(rs);
                    friends.add(f);
                }
            }
        }
        return friends;
    }

    // ------------------------------------------------------------------
    // C100 — View Pending Requests (READ)
    // Returns friend requests sent TO this user that are still PENDING.
    // ------------------------------------------------------------------
    public List<Friendship> findPendingRequests(int userID) throws SQLException {
        String sql = "SELECT f.friendshipID, f.requesterID, f.addresseID, f.status, "
                   + "u1.name AS requesterName, u1.surname AS requesterSurname, "
                   + "u2.name AS addresseName, u2.surname AS addresseSurname "
                   + "FROM Friendship f "
                   + "JOIN User u1 ON f.requesterID = u1.userID "
                   + "JOIN User u2 ON f.addresseID = u2.userID "
                   + "WHERE f.addresseID = ? AND f.status = 'PENDING'";
        List<Friendship> pending = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    pending.add(map(rs));
                }
            }
        }
        return pending;
    }

    // ------------------------------------------------------------------
    // C200 — Add Friend (CREATE)
    // Sends a friend request from one user to another. Status = PENDING.
    // Returns the generated friendshipID, or -1 on failure.
    // ------------------------------------------------------------------
    public int sendRequest(int requesterID, int addresseID) throws SQLException {
        String sql = "INSERT INTO Friendship (requesterID, addresseID, status) VALUES (?, ?, 'PENDING')";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, requesterID);
            ps.setInt(2, addresseID);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    // ------------------------------------------------------------------
    // C200 — Accept Friend Request (UPDATE)
    // Changes a PENDING request to ACCEPTED.
    // ------------------------------------------------------------------
    public boolean acceptRequest(int friendshipID) throws SQLException {
        String sql = "UPDATE Friendship SET status = 'ACCEPTED' WHERE friendshipID = ? AND status = 'PENDING'";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, friendshipID);
            return ps.executeUpdate() > 0;
        }
    }

    // ------------------------------------------------------------------
    // C200 — Reject Friend Request (UPDATE)
    // Changes a PENDING request to REJECTED.
    // ------------------------------------------------------------------
    public boolean rejectRequest(int friendshipID) throws SQLException {
        String sql = "UPDATE Friendship SET status = 'REJECTED' WHERE friendshipID = ? AND status = 'PENDING'";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, friendshipID);
            return ps.executeUpdate() > 0;
        }
    }

    // ------------------------------------------------------------------
    // C300 — Remove Friend (DELETE)
    // Deletes an accepted friendship entirely.
    // ------------------------------------------------------------------
    public boolean remove(int friendshipID) throws SQLException {
        String sql = "DELETE FROM Friendship WHERE friendshipID = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, friendshipID);
            return ps.executeUpdate() > 0;
        }
    }

    // ------------------------------------------------------------------
    // Helper: check if a friendship already exists between two users
    // (in either direction), regardless of status.
    // ------------------------------------------------------------------
    public Friendship findBetweenUsers(int userA, int userB) throws SQLException {
        String sql = "SELECT f.friendshipID, f.requesterID, f.addresseID, f.status, "
                   + "u1.name AS requesterName, u1.surname AS requesterSurname, "
                   + "u2.name AS addresseName, u2.surname AS addresseSurname "
                   + "FROM Friendship f "
                   + "JOIN User u1 ON f.requesterID = u1.userID "
                   + "JOIN User u2 ON f.addresseID = u2.userID "
                   + "WHERE (f.requesterID = ? AND f.addresseID = ?) "
                   + "OR (f.requesterID = ? AND f.addresseID = ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userA);
            ps.setInt(2, userB);
            ps.setInt(3, userB);
            ps.setInt(4, userA);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    // ------------------------------------------------------------------
    // Row mapper
    // ------------------------------------------------------------------
    private Friendship map(ResultSet rs) throws SQLException {
        Friendship f = new Friendship();
        f.setFriendshipID(rs.getInt("friendshipID"));
        f.setRequesterID(rs.getInt("requesterID"));
        f.setAddresseID(rs.getInt("addresseID"));
        f.setStatus(rs.getString("status"));
        f.setRequesterName(rs.getString("requesterName") + " " + rs.getString("requesterSurname"));
        f.setAddresseName(rs.getString("addresseName") + " " + rs.getString("addresseSurname"));
        try { f.setRequesterStudentNo(rs.getString("requesterStudentNo")); } catch (SQLException e) { /* column not in this query */ }
        try { f.setAddresseStudentNo(rs.getString("addresseStudentNo")); } catch (SQLException e) { /* column not in this query */ }
        return f;
    }
}

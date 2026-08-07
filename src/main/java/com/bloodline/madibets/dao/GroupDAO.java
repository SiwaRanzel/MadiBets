package com.bloodline.madibets.dao;

import com.bloodline.madibets.config.DatabaseConnection;
import com.bloodline.madibets.model.Group;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

/** Owner: Pieter (D-series). */
public class GroupDAO {

    /** D100 Create Group (CREATE). Returns generated groupID or -1 on failure. */
    public int create(Group g) throws SQLException {
        String insertGroupSql = "INSERT INTO `Group` (groupName, description, createdBy) VALUES (?, ?, ?)";
        String insertMemberSql = "INSERT INTO GroupMember (groupID, userID, role) VALUES (?, ?, 'OWNER')";

        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            int groupID = -1;
            try (PreparedStatement ps = con.prepareStatement(insertGroupSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, g.getGroupName());
                ps.setString(2, g.getDescription());
                ps.setInt(3, g.getCreatedBy());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        groupID = keys.getInt(1);
                    }
                }
            }

            if (groupID == -1) {
                con.rollback();
                return -1;
            }

            try (PreparedStatement ps = con.prepareStatement(insertMemberSql)) {
                ps.setInt(1, groupID);
                ps.setInt(2, g.getCreatedBy());
                ps.executeUpdate();
            }

            con.commit();
            return groupID;
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

    /** D200 View Group (READ). */
    public Group findById(int groupID) throws SQLException {
        String sql = "SELECT groupID, groupName, description, createdBy, createdDate FROM `Group` WHERE groupID = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, groupID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Group g = new Group();
                    g.setGroupID(rs.getInt("groupID"));
                    g.setGroupName(rs.getString("groupName"));
                    g.setDescription(rs.getString("description"));
                    g.setCreatedBy(rs.getInt("createdBy"));
                    g.setCreatedDate(rs.getTimestamp("createdDate"));
                    return g;
                } else {
                    return null;
                }
            }
        }
    }

    /** Search by name (LIKE) or numeric id. */
    public List<Group> searchByNameOrId(String q) throws SQLException {
        List<Group> results = new ArrayList<>();
        if (q == null || q.isBlank()) {
            // return all groups (limit 100)
            String sql = "SELECT groupID, groupName, description, createdBy, createdDate FROM `Group` ORDER BY groupName LIMIT 100";
            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(sql);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Group g = map(rs);
                    results.add(g);
                }
            }
            return results;
        }

        // try numeric id search
        try {
            int id = Integer.parseInt(q);
            Group g = findById(id);
            if (g != null) results.add(g);
            return results;
        } catch (NumberFormatException ignore) {
            // not numeric, fall through to name search
        }

        String like = "%" + q + "%";
        String sql = "SELECT groupID, groupName, description, createdBy, createdDate FROM `Group` WHERE groupName LIKE ? ORDER BY groupName LIMIT 100";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, like);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(map(rs));
                }
            }
        }
        return results;
    }

    /** Find groups a user belongs to. */
    public List<Group> findByUser(int userID) throws SQLException {
        List<Group> results = new ArrayList<>();
        String sql = "SELECT g.groupID, g.groupName, g.description, g.createdBy, g.createdDate "
                   + "FROM `Group` g JOIN GroupMember m ON g.groupID = m.groupID WHERE m.userID = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(map(rs));
                }
            }
        }
        return results;
    }

    /** Add a member to a group. Returns true if inserted, false if already member. */
    public boolean addMember(int groupID, int userID) throws SQLException {
        String sql = "INSERT INTO GroupMember (groupID, userID) VALUES (?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, groupID);
            ps.setInt(2, userID);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            // Unique constraint violation -> already member
            String state = e.getSQLState();
            // MySQL uses SQLState 23000 for integrity constraint violation
            if (state != null && state.startsWith("23")) {
                return false;
            }
            throw e;
        }
    }

    public boolean isMember(int groupID, int userID) throws SQLException {
        String sql = "SELECT COUNT(*) AS c FROM GroupMember WHERE groupID = ? AND userID = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, groupID);
            ps.setInt(2, userID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("c") > 0;
            }
        }
    }

    private Group map(ResultSet rs) throws SQLException {
        Group g = new Group();
        g.setGroupID(rs.getInt("groupID"));
        g.setGroupName(rs.getString("groupName"));
        g.setDescription(rs.getString("description"));
        g.setCreatedBy(rs.getInt("createdBy"));
        g.setCreatedDate(rs.getTimestamp("createdDate"));
        return g;
    }
}

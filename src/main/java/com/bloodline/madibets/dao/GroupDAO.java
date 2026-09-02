package com.bloodline.madibets.dao;

import com.bloodline.madibets.config.DatabaseConnection;
import com.bloodline.madibets.model.Group;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GroupDAO {

    /** D100 Create Group (CREATE). Returns generated groupID or -1 on failure. */
    public int create(Group g) throws SQLException {
        String insertGroupSql = "INSERT INTO `Group` (groupName, description, password, maxMembers, createdBy) VALUES (?, ?, ?, ?, ?)";
        String insertMemberSql = "INSERT INTO GroupMember (groupID, userID, role) VALUES (?, ?, 'OWNER')";

        // Hash an optional join password (bcrypt); NULL/blank means an open group.
        String plain = g.getPassword();
        String passwordHash = (plain != null && !plain.isBlank())
                ? BCrypt.hashpw(plain, BCrypt.gensalt())
                : null;

        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            int groupID = -1;
            try (PreparedStatement ps = con.prepareStatement(insertGroupSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, g.getGroupName());
                ps.setString(2, g.getDescription());
                if (passwordHash != null) {
                    ps.setString(3, passwordHash);
                } else {
                    ps.setNull(3, java.sql.Types.VARCHAR);
                }
                // maxMembers: NULL when unset or non-positive (treated as unlimited)
                if (g.getMaxMembers() != null && g.getMaxMembers() > 0) {
                    ps.setInt(4, g.getMaxMembers());
                } else {
                    ps.setNull(4, java.sql.Types.INTEGER);
                }
                ps.setInt(5, g.getCreatedBy());
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
        String sql = "SELECT groupID, groupName, description, maxMembers, createdBy, createdDate, (password IS NOT NULL) AS hasPassword FROM `Group` WHERE groupID = ?";
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
                    g.setHasPassword(rs.getInt("hasPassword") == 1);
                    int mm = rs.getInt("maxMembers");
                    g.setMaxMembers(rs.wasNull() ? null : mm);
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
            // return all groups (limit 100) — exclude default groups
            String sql = "SELECT groupID, groupName, description, maxMembers, createdBy, createdDate, (password IS NOT NULL) AS hasPassword FROM `Group` WHERE groupName NOT IN ('Overall', 'My Friends') ORDER BY groupName LIMIT 100";
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
        String sql = "SELECT groupID, groupName, description, maxMembers, createdBy, createdDate, (password IS NOT NULL) AS hasPassword FROM `Group` WHERE groupName LIKE ? AND groupName NOT IN ('Overall', 'My Friends') ORDER BY groupName LIMIT 100";
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
        String sql = "SELECT g.groupID, g.groupName, g.description, g.maxMembers, g.createdBy, g.createdDate, (g.password IS NOT NULL) AS hasPassword "
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

    /** Find groups created by a specific user. */
    public List<Group> findByCreator(int createdBy) throws SQLException {
        List<Group> results = new ArrayList<>();
        String sql = "SELECT groupID, groupName, description, maxMembers, createdBy, createdDate, (password IS NOT NULL) AS hasPassword FROM `Group` WHERE createdBy = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, createdBy);
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

    /** True if the group has a non-null join password set. */
    public boolean hasPassword(int groupID) throws SQLException {
        String sql = "SELECT password FROM `Group` WHERE groupID = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, groupID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getString("password") != null;
            }
        }
    }

    /**
     * Verify a plaintext password against the group's stored bcrypt hash.
     * Returns true when the group has no password (open group) or the password
     * matches; false when a password is required and the supplied one is wrong.
     */
    public boolean verifyPassword(int groupID, String plain) throws SQLException {
        String sql = "SELECT password FROM `Group` WHERE groupID = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, groupID);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return false;            // group doesn't exist
                String hash = rs.getString("password");
                if (hash == null) return true;           // open group
                return plain != null && BCrypt.checkpw(plain, hash);
            }
        }
    }

    /** Count members in a group. */
    public int getMemberCount(int groupID) throws SQLException {
        String sql = "SELECT COUNT(*) AS c FROM GroupMember WHERE groupID = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, groupID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("c") : 0;
            }
        }
    }

    /** Count joining members only (role != 'OWNER') — used for the capacity cap. */
    public int getNonOwnerMemberCount(int groupID) throws SQLException {
        String sql = "SELECT COUNT(*) AS c FROM GroupMember WHERE groupID = ? AND role != 'OWNER'";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, groupID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("c") : 0;
            }
        }
    }

    /** The group's member cap (excludes owner), or null if unlimited. */
    public Integer getMaxMembers(int groupID) throws SQLException {
        String sql = "SELECT maxMembers FROM `Group` WHERE groupID = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, groupID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int mm = rs.getInt("maxMembers");
                    return rs.wasNull() ? null : mm;
                }
                return null;
            }
        }
    }

    /**
     * List members of a group with names, roles, and MadiBucks balance,
     * ranked by balance descending — this is the per-group leaderboard.
     * Members without an Account row (shouldn't normally happen) sort last
     * with a balance of 0.
     */
    public List<Map<String, Object>> findMembers(int groupID) throws SQLException {
        List<Map<String, Object>> members = new ArrayList<>();
        String sql = "SELECT u.userID, u.name, u.surname, u.email, u.userType, gm.role, "
                   + "COALESCE(a.balance, 0) AS balance "
                   + "FROM GroupMember gm "
                   + "JOIN User u ON gm.userID = u.userID "
                   + "LEFT JOIN Account a ON a.userID = u.userID "
                   + "WHERE gm.groupID = ? "
                   + "ORDER BY balance DESC, u.name";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, groupID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new java.util.HashMap<>();
                    m.put("userID", rs.getInt("userID"));
                    m.put("name", rs.getString("name"));
                    m.put("surname", rs.getString("surname"));
                    m.put("email", rs.getString("email"));
                    m.put("userType", rs.getString("userType"));
                    m.put("role", rs.getString("role"));
                    m.put("balance", rs.getBigDecimal("balance"));
                    members.add(m);
                }
            }
        }
        return members;
    }

    /** D300 Leave Group (DELETE). Removes a member from a group. Cannot remove OWNER. */
    public boolean removeMember(int groupID, int userID) throws SQLException {
        String sql = "DELETE FROM GroupMember WHERE groupID = ? AND userID = ? AND role != 'OWNER'";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, groupID);
            ps.setInt(2, userID);
            return ps.executeUpdate() > 0;
        }
    }

    private Group map(ResultSet rs) throws SQLException {
        Group g = new Group();
        g.setGroupID(rs.getInt("groupID"));
        g.setGroupName(rs.getString("groupName"));
        g.setDescription(rs.getString("description"));
        g.setCreatedBy(rs.getInt("createdBy"));
        g.setCreatedDate(rs.getTimestamp("createdDate"));
        // hasPassword is a computed 0/1 column; the hash itself is never selected.
        try {
            g.setHasPassword(rs.getInt("hasPassword") == 1);
        } catch (SQLException ignore) {
            // query didn't select hasPassword — leave default false
        }
        try {
            int mm = rs.getInt("maxMembers");
            g.setMaxMembers(rs.wasNull() ? null : mm);
        } catch (SQLException ignore) {
            // query didn't select maxMembers — leave default null
        }
        return g;
    }
}
package com.bloodline.madibets.dao;

import com.bloodline.madibets.config.DatabaseConnection;
import com.bloodline.madibets.model.Task;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.math.BigDecimal;

/** Owner: Pieter (D-series). */
public class TaskDAO {
    
    // D400 Create Tasks (CREATE)   -> create(...)
    public Task create(Task task) throws SQLException {
        String sql = "INSERT INTO Task (title, description, groupID, userID, amount, createdBy) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setString(1, task.getTitle());
            ps.setString(2, task.getDescription());
            ps.setInt(3, task.getGroupID());
            
            if (task.getUserID() != null) {
                ps.setInt(4, task.getUserID());
            } else {
                ps.setNull(4, java.sql.Types.INTEGER);
            }
            
            ps.setBigDecimal(5, task.getAmount());
            ps.setInt(6, task.getCreatedBy());
            
            ps.executeUpdate();
            
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    task.setTaskID(rs.getInt(1));
                }
            }
            return task;
        }
    }

    // D500 View Tasks (READ)       -> findByGroup(int groupID)
    public List<Task> findByGroup(int groupID) throws SQLException {
        String sql = "SELECT taskID, title, description, groupID, userID, amount, createdBy FROM Task WHERE groupID = ?";
        List<Task> tasks = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, groupID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Task task = new Task();
                    task.setTaskID(rs.getInt("taskID"));
                    task.setTitle(rs.getString("title"));
                    task.setDescription(rs.getString("description"));
                    task.setGroupID(rs.getInt("groupID"));
                    
                    int userID = rs.getInt("userID");
                    if (!rs.wasNull()) {
                        task.setUserID(userID);
                    }
                    
                    task.setAmount(rs.getBigDecimal("amount"));
                    task.setCreatedBy(rs.getInt("createdBy"));
                    tasks.add(task);
                }
            }
        }
        return tasks;
    }

    // D501 View Tasks by Creator (READ) -> findByCreator(int createdBy)
    public List<Task> findByCreator(int createdBy) throws SQLException {
        String sql = "SELECT taskID, title, description, groupID, userID, amount, createdBy FROM Task WHERE createdBy = ?";
        List<Task> tasks = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, createdBy);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Task task = new Task();
                    task.setTaskID(rs.getInt("taskID"));
                    task.setTitle(rs.getString("title"));
                    task.setDescription(rs.getString("description"));
                    task.setGroupID(rs.getInt("groupID"));
                    
                    int userID = rs.getInt("userID");
                    if (!rs.wasNull()) {
                        task.setUserID(userID);
                    }
                    
                    task.setAmount(rs.getBigDecimal("amount"));
                    task.setCreatedBy(rs.getInt("createdBy"));
                    tasks.add(task);
                }
            }
        }
        return tasks;
    }

    // Submit a task completion (student marks task as done)
    public boolean submitCompletion(int taskID, int userID) throws SQLException {
        String sql = "INSERT INTO TaskCompletion (taskID, userID, completionStatus, completionDate) VALUES (?, ?, 'PENDING', NOW())";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, taskID);
            ps.setInt(2, userID);
            ps.executeUpdate();
            return true;
        } catch (SQLException e) {
            // Unique constraint or duplicate
            if (e.getSQLState() != null && e.getSQLState().startsWith("23")) {
                return false;
            }
            throw e;
        }
    }

    // Get all completions for a task (for lecturer review)
    public List<java.util.Map<String, Object>> getCompletions(int taskID) throws SQLException {
        String sql = "SELECT tc.completionID, tc.taskID, tc.userID, tc.completionStatus, tc.completionDate, "
                   + "u.name, u.surname, s.studentNo "
                   + "FROM TaskCompletion tc "
                   + "JOIN User u ON tc.userID = u.userID "
                   + "LEFT JOIN Student s ON tc.userID = s.userID "
                   + "WHERE tc.taskID = ? ORDER BY tc.completionDate DESC";
        List<java.util.Map<String, Object>> results = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, taskID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    java.util.Map<String, Object> row = new java.util.HashMap<>();
                    row.put("completionID", rs.getInt("completionID"));
                    row.put("taskID", rs.getInt("taskID"));
                    row.put("userID", rs.getInt("userID"));
                    row.put("completionStatus", rs.getString("completionStatus"));
                    row.put("completionDate", rs.getString("completionDate"));
                    row.put("name", rs.getString("name") + " " + rs.getString("surname"));
                    row.put("studentNo", rs.getString("studentNo"));
                    results.add(row);
                }
            }
        }
        return results;
    }

    // Update completion status (lecturer approves/rejects)
    public boolean updateCompletionStatus(int completionID, String status) throws SQLException {
        String sql = "UPDATE TaskCompletion SET completionStatus = ? WHERE completionID = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setInt(2, completionID);
            return ps.executeUpdate() > 0;
        }
    }
}

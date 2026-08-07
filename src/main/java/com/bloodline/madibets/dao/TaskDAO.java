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
}

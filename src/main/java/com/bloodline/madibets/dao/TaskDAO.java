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
import java.util.Map;
import java.util.HashMap;
import java.math.BigDecimal;

/** Owner: Pieter (D-series). */
public class TaskDAO {
    
    // D400 Create Tasks (CREATE)   -> create(...)
    public Task create(Task task) throws SQLException {
        String sql = "INSERT INTO Task (title, description, groupID, userID, amount, createdBy, correctAnswer) VALUES (?, ?, ?, ?, ?, ?, ?)";
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
            
            if (task.getCorrectAnswer() != null) {
                ps.setBoolean(7, task.getCorrectAnswer());
            } else {
                ps.setNull(7, java.sql.Types.BOOLEAN);
            }
            
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
        String sql = "SELECT taskID, title, description, groupID, userID, amount, createdBy, correctAnswer FROM Task WHERE groupID = ?";
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
                    
                    boolean correctAnswer = rs.getBoolean("correctAnswer");
                    if (!rs.wasNull()) {
                        task.setCorrectAnswer(correctAnswer);
                    }
                    
                    tasks.add(task);
                }
            }
        }
        return tasks;
    }

    // D501 View Tasks by Creator (READ) -> findByCreator(int createdBy)
    public List<Task> findByCreator(int createdBy) throws SQLException {
        String sql = "SELECT taskID, title, description, groupID, userID, amount, createdBy, correctAnswer FROM Task WHERE createdBy = ?";
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
                    
                    boolean correctAnswer = rs.getBoolean("correctAnswer");
                    if (!rs.wasNull()) {
                        task.setCorrectAnswer(correctAnswer);
                    }
                    
                    tasks.add(task);
                }
            }
        }
        return tasks;
    }

    /**
     * Find tasks for a group, each annotated with the given user's answer status.
     * Returns a list of maps: {taskID, title, description, amount, answered, isCorrect}.
     * The correctAnswer is NOT exposed to students — only whether they answered
     * and whether their answer was right.
     */
    public List<Map<String, Object>> findByGroupWithStatus(int groupID, int userID) throws SQLException {
        String sql = "SELECT t.taskID, t.title, t.description, t.amount, "
                   + "tc.completionStatus, tc.completionDate "
                   + "FROM Task t "
                   + "LEFT JOIN TaskCompletion tc ON tc.taskID = t.taskID AND tc.userID = ? "
                   + "WHERE t.groupID = ? "
                   + "ORDER BY t.taskID";
        List<Map<String, Object>> tasks = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userID);
            ps.setInt(2, groupID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("taskID", rs.getInt("taskID"));
                    m.put("title", rs.getString("title"));
                    m.put("description", rs.getString("description"));
                    m.put("amount", rs.getBigDecimal("amount"));
                    
                    String status = rs.getString("completionStatus");
                    boolean answered = status != null;
                    m.put("answered", answered);
                    m.put("isCorrect", answered && "COMPLETED".equals(status));
                    tasks.add(m);
                }
            }
        }
        return tasks;
    }

    /**
     * Record a student's True/False answer for a task.
     * Returns 1 if answered correctly (and MadiBucks credited),
     * 0 if answered incorrectly, -1 if already answered or task doesn't exist.
     * The answer is correct if it matches the task's correctAnswer.
     */
    public int answerTask(int taskID, int userID, boolean answer) throws SQLException {
        // Check if already answered
        String checkSql = "SELECT completionID FROM TaskCompletion WHERE taskID = ? AND userID = ?";
        try (Connection con = DatabaseConnection.getConnection()) {
            try (PreparedStatement ps = con.prepareStatement(checkSql)) {
                ps.setInt(1, taskID);
                ps.setInt(2, userID);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        return -1; // already answered
                    }
                }
            }

            // Determine if the answer is correct and get the task amount
            String correctSql = "SELECT correctAnswer, amount FROM Task WHERE taskID = ?";
            boolean isCorrect = false;
            BigDecimal reward = BigDecimal.ZERO;
            try (PreparedStatement ps = con.prepareStatement(correctSql)) {
                ps.setInt(1, taskID);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        boolean correctAnswer = rs.getBoolean("correctAnswer");
                        if (!rs.wasNull()) {
                            isCorrect = (answer == correctAnswer);
                        }
                        reward = rs.getBigDecimal("amount");
                    } else {
                        return -1; // task doesn't exist
                    }
                }
            }

            // Insert the completion record
            String insertSql = "INSERT INTO TaskCompletion (taskID, userID, completionStatus, completionDate) "
                             + "VALUES (?, ?, ?, NOW())";
            try (PreparedStatement ps = con.prepareStatement(insertSql)) {
                ps.setInt(1, taskID);
                ps.setInt(2, userID);
                ps.setString(3, isCorrect ? "COMPLETED" : "REJECTED");
                ps.executeUpdate();
            }

            // Credit MadiBucks to the student's account if the answer was correct
            if (isCorrect && reward != null && reward.signum() > 0) {
                AccountDAO accountDAO = new AccountDAO();
                accountDAO.adjustBalance(userID, reward,
                        "Task reward: correct answer on task " + taskID);
            }

            return isCorrect ? 1 : 0;
        }
    }

}

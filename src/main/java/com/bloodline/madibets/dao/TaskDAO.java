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

/** Owner: Pieter (D-series). D400 Create Tasks (CREATE), D500 View Tasks (READ), answer submission. */
public class TaskDAO {

    /** D400 Create a task for a group. Returns the generated taskID or -1 on failure. */
    public int create(Task t) throws SQLException {
        String sql = "INSERT INTO Task (groupID, question, correctAnswer, amount, createdBy) VALUES (?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, t.getGroupID());
            ps.setString(2, t.getQuestion());
            ps.setBoolean(3, t.isCorrectAnswer());
            ps.setDouble(4, t.getAmount());
            ps.setInt(5, t.getCreatedBy());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    return keys.getInt(1);
                }
            }
            return -1;
        }
    }

    /** D500 View tasks belonging to a group, with the given user's answered state (null = not answered). */
    public List<Task> findByGroup(int groupID, int userID) throws SQLException {
        String sql = "SELECT t.taskID, t.groupID, t.question, t.correctAnswer, t.amount, t.createdBy, t.createdDate, "
                   + "tc.answer AS userAnswer "
                   + "FROM Task t "
                   + "LEFT JOIN TaskCompletion tc ON tc.taskID = t.taskID AND tc.userID = ? "
                   + "WHERE t.groupID = ? "
                   + "ORDER BY t.createdDate DESC, t.taskID DESC";
        List<Task> results = new ArrayList<>();
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userID);
            ps.setInt(2, groupID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Task t = new Task();
                    t.setTaskID(rs.getInt("taskID"));
                    t.setGroupID(rs.getInt("groupID"));
                    t.setQuestion(rs.getString("question"));
                    t.setCorrectAnswer(rs.getBoolean("correctAnswer"));
                    t.setAmount(rs.getDouble("amount"));
                    t.setCreatedBy(rs.getInt("createdBy"));
                    t.setCreatedDate(rs.getTimestamp("createdDate"));

                    boolean hasAnswer = rs.getObject("userAnswer") != null;
                    if (hasAnswer) {
                        boolean ans = rs.getBoolean("userAnswer");
                        t.setAnswered(ans);
                        t.setIsCorrect(ans == t.isCorrectAnswer());
                    } else {
                        t.setAnswered(null);
                        t.setIsCorrect(null);
                    }
                    results.add(t);
                }
            }
        }
        return results;
    }

    /**
     * Submit a student's True/False answer for a task.
     * Validates the user is a member of the task's group, records the answer,
     * and if correct credits the user's Account balance with the task's amount.
     * Returns true on success; false if already answered; throws if not a member.
     */
    public boolean submitAnswer(int taskID, int userID, boolean answer) throws SQLException {
        // Load task + group membership in one query
        String taskSql = "SELECT t.taskID, t.amount, t.correctAnswer, t.groupID, "
                       + "(SELECT COUNT(*) FROM GroupMember gm WHERE gm.groupID = t.groupID AND gm.userID = ?) AS memberCount, "
                       + "(SELECT COUNT(*) FROM TaskCompletion tc WHERE tc.taskID = t.taskID AND tc.userID = ?) AS answeredCount "
                       + "FROM Task t WHERE t.taskID = ?";
        double amount = 0.0;
        boolean correctAnswer = false;
        int groupID = -1;
        boolean isMember = false;
        boolean alreadyAnswered = false;

        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            try (PreparedStatement ps = con.prepareStatement(taskSql)) {
                ps.setInt(1, userID);
                ps.setInt(2, userID);
                ps.setInt(3, taskID);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        groupID = rs.getInt("groupID");
                        amount = rs.getDouble("amount");
                        correctAnswer = rs.getBoolean("correctAnswer");
                        isMember = rs.getInt("memberCount") > 0;
                        alreadyAnswered = rs.getInt("answeredCount") > 0;
                    } else {
                        throw new SQLException("Task not found");
                    }
                }
            }

            if (!isMember) {
                throw new SQLException("You must be a member of this group to answer its tasks");
            }
            if (alreadyAnswered) {
                con.rollback();
                con.setAutoCommit(true);
                con.close();
                return false;
            }

            // Record the answer
            String insertSql = "INSERT INTO TaskCompletion (taskID, userID, answer) VALUES (?, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(insertSql)) {
                ps.setInt(1, taskID);
                ps.setInt(2, userID);
                ps.setBoolean(3, answer);
                ps.executeUpdate();
            }

            // Award MadiBucks if the answer is correct
            boolean isCorrect = answer == correctAnswer;
            if (isCorrect && amount > 0) {
                String creditSql = "UPDATE Account SET balance = balance + ? WHERE userID = ?";
                try (PreparedStatement ps = con.prepareStatement(creditSql)) {
                    ps.setDouble(1, amount);
                    ps.setInt(2, userID);
                    ps.executeUpdate();
                }
            }

            con.commit();

            // Expose the computed correctness to callers via a transient field if desired
            if (groupID > 0) {
                // nothing extra needed; caller can read Task details separately
            }
            return true;
        } catch (SQLException e) {
            if (con != null) {
                try { con.rollback(); } catch (SQLException ex) { /* ignore */ }
            }
            throw e;
        } finally {
            if (con != null) {
                try { con.setAutoCommit(true); con.close(); } catch (SQLException ex) { /* ignore */ }
            }
        }
    }

    /** Check whether a task belongs to the given group (used for validation). */
    public boolean taskBelongsToGroup(int taskID, int groupID) throws SQLException {
        String sql = "SELECT COUNT(*) AS c FROM Task WHERE taskID = ? AND groupID = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, taskID);
            ps.setInt(2, groupID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt("c") > 0;
            }
        }
    }
}
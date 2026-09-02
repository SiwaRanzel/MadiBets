package com.bloodline.madibets.dao;

import com.bloodline.madibets.config.DatabaseConnection;
import com.bloodline.madibets.model.Task;
import com.bloodline.madibets.model.TaskQuestion;
import com.bloodline.madibets.model.TaskOption;
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
     * Find quiz tasks for a group, each annotated with the given user's
     * submission status. Returns maps:
     * {taskID, title, description, questionCount, submitted, score,
     *  awardedMadibucks}. Correct answers are never exposed here.
     */
    public List<Map<String, Object>> findByGroupWithStatus(int groupID, int userID) throws SQLException {
        String sql = "SELECT t.taskID, t.title, t.description, t.amount, "
                   + "(SELECT COUNT(*) FROM TaskQuestion q WHERE q.taskID = t.taskID) AS questionCount, "
                   + "ts.score, ts.awardedMadibucks, ts.submissionID "
                   + "FROM Task t "
                   + "LEFT JOIN TaskSubmission ts ON ts.taskID = t.taskID AND ts.userID = ? "
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
                    m.put("questionCount", rs.getInt("questionCount"));

                    rs.getInt("submissionID");
                    boolean submitted = !rs.wasNull();
                    m.put("submitted", submitted);
                    if (submitted) {
                        m.put("score", rs.getInt("score"));
                        m.put("awardedMadibucks", rs.getInt("awardedMadibucks"));
                    }
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

    // ================================================================
    // QUIZ SUPPORT (multi-question T/F + MCQ, 10 MadiBucks per correct)
    // ================================================================

    /**
     * Create a quiz Task with its questions and options in one transaction.
     * The Task's amount is stored as 0 (reward is derived as 10 * correct at
     * submit time). Returns the generated taskID.
     */
    public int createQuiz(String title, int groupID, int createdBy, List<TaskQuestion> questions) throws SQLException {
        String insertTaskSql = "INSERT INTO Task (title, description, groupID, amount, createdBy) VALUES (?, ?, ?, 0, ?)";
        String insertQSql = "INSERT INTO TaskQuestion (taskID, prompt, type, position) VALUES (?, ?, ?, ?)";
        String insertOSql = "INSERT INTO TaskOption (questionID, optionText, isCorrect, position) VALUES (?, ?, ?, ?)";

        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            int taskID;
            try (PreparedStatement ps = con.prepareStatement(insertTaskSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, title);
                ps.setString(2, title);
                ps.setInt(3, groupID);
                ps.setInt(4, createdBy);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) { con.rollback(); return -1; }
                    taskID = keys.getInt(1);
                }
            }

            int qPos = 0;
            for (TaskQuestion q : questions) {
                int questionID;
                try (PreparedStatement ps = con.prepareStatement(insertQSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, taskID);
                    ps.setString(2, q.getPrompt());
                    ps.setString(3, q.getType());
                    ps.setInt(4, qPos++);
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        keys.next();
                        questionID = keys.getInt(1);
                    }
                }
                int oPos = 0;
                for (TaskOption o : q.getOptions()) {
                    try (PreparedStatement ps = con.prepareStatement(insertOSql)) {
                        ps.setInt(1, questionID);
                        ps.setString(2, o.getOptionText());
                        ps.setBoolean(3, o.isCorrect());
                        ps.setInt(4, oPos++);
                        ps.executeUpdate();
                    }
                }
            }

            con.commit();
            return taskID;
        } catch (SQLException e) {
            if (con != null) try { con.rollback(); } catch (SQLException ignore) {}
            throw e;
        } finally {
            if (con != null) try { con.setAutoCommit(true); con.close(); } catch (SQLException ignore) {}
        }
    }

    /** Delete a task (quiz). Questions, options, submissions and answers cascade via FKs. */
    public boolean deleteTask(int taskID) throws SQLException {
        String sql = "DELETE FROM Task WHERE taskID = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, taskID);
            return ps.executeUpdate() > 0;
        }
    }

    /** Number of questions in a task (0 if it's a legacy single-answer task). */
    public int getQuestionCount(int taskID) throws SQLException {
        String sql = "SELECT COUNT(*) AS c FROM TaskQuestion WHERE taskID = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, taskID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? rs.getInt("c") : 0;
            }
        }
    }

    /** True if the user has already submitted this quiz. */
    public boolean hasSubmitted(int taskID, int userID) throws SQLException {
        String sql = "SELECT 1 FROM TaskSubmission WHERE taskID = ? AND userID = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, taskID);
            ps.setInt(2, userID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Load a quiz for a student to TAKE. Options are returned WITHOUT the
     * isCorrect flag so nothing is revealed before submit. If the student has
     * already submitted, includes their stored per-question result and the
     * total score/award.
     * Returns a map: { taskID, title, submitted, score, awardedMadibucks,
     *                  questions: [ { questionID, prompt, type,
     *                                 options: [ { optionID, optionText } ],
     *                                 (if submitted) chosenOptionID, isCorrect,
     *                                 correctOptionID } ] }
     */
    public Map<String, Object> getQuiz(int taskID, int userID) throws SQLException {
        Map<String, Object> out = new HashMap<>();
        boolean submitted = hasSubmitted(taskID, userID);
        out.put("taskID", taskID);
        out.put("submitted", submitted);

        // Task title
        String titleSql = "SELECT title FROM Task WHERE taskID = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(titleSql)) {
            ps.setInt(1, taskID);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) out.put("title", rs.getString("title"));
            }
        }

        // If submitted, load the submission summary + per-question answers.
        Map<Integer, Map<String, Object>> answerByQuestion = new HashMap<>();
        if (submitted) {
            String subSql = "SELECT submissionID, score, awardedMadibucks FROM TaskSubmission WHERE taskID = ? AND userID = ?";
            int submissionID = -1;
            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(subSql)) {
                ps.setInt(1, taskID);
                ps.setInt(2, userID);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        submissionID = rs.getInt("submissionID");
                        out.put("score", rs.getInt("score"));
                        out.put("awardedMadibucks", rs.getInt("awardedMadibucks"));
                    }
                }
            }
            if (submissionID != -1) {
                String ansSql = "SELECT questionID, chosenOptionID, isCorrect FROM TaskAnswer WHERE submissionID = ?";
                try (Connection con = DatabaseConnection.getConnection();
                     PreparedStatement ps = con.prepareStatement(ansSql)) {
                    ps.setInt(1, submissionID);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            Map<String, Object> a = new HashMap<>();
                            int chosen = rs.getInt("chosenOptionID");
                            a.put("chosenOptionID", rs.wasNull() ? null : chosen);
                            a.put("isCorrect", rs.getBoolean("isCorrect"));
                            answerByQuestion.put(rs.getInt("questionID"), a);
                        }
                    }
                }
            }
        }

        // Questions + options
        List<Map<String, Object>> questions = new ArrayList<>();
        String qSql = "SELECT questionID, prompt, type FROM TaskQuestion WHERE taskID = ? ORDER BY position, questionID";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(qSql)) {
            ps.setInt(1, taskID);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> q = new HashMap<>();
                    int questionID = rs.getInt("questionID");
                    q.put("questionID", questionID);
                    q.put("prompt", rs.getString("prompt"));
                    q.put("type", rs.getString("type"));
                    questions.add(q);
                }
            }
        }

        String oSql = "SELECT optionID, optionText, isCorrect FROM TaskOption WHERE questionID = ? ORDER BY position, optionID";
        for (Map<String, Object> q : questions) {
            int questionID = (int) q.get("questionID");
            List<Map<String, Object>> options = new ArrayList<>();
            Integer correctOptionID = null;
            try (Connection con = DatabaseConnection.getConnection();
                 PreparedStatement ps = con.prepareStatement(oSql)) {
                ps.setInt(1, questionID);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> o = new HashMap<>();
                        int optionID = rs.getInt("optionID");
                        o.put("optionID", optionID);
                        o.put("optionText", rs.getString("optionText"));
                        // Only expose correctness AFTER the student has submitted.
                        boolean correct = rs.getBoolean("isCorrect");
                        if (submitted) {
                            o.put("isCorrect", correct);
                            if (correct) correctOptionID = optionID;
                        }
                        options.add(o);
                    }
                }
            }
            q.put("options", options);
            if (submitted) {
                q.put("correctOptionID", correctOptionID);
                Map<String, Object> a = answerByQuestion.get(questionID);
                if (a != null) {
                    q.put("chosenOptionID", a.get("chosenOptionID"));
                    q.put("isCorrect", a.get("isCorrect"));
                }
            }
        }

        out.put("questions", questions);
        return out;
    }

    /**
     * Grade and record a whole quiz submission in one transaction. Awards a
     * fixed 10 MadiBucks per correct answer. Returns a result map, or null if
     * the student has already submitted (single attempt).
     */
    public Map<String, Object> submitQuiz(int taskID, int userID, Map<Integer, Integer> answers) throws SQLException {
        Connection con = null;
        try {
            con = DatabaseConnection.getConnection();
            con.setAutoCommit(false);

            // Guard: one attempt per student.
            String checkSql = "SELECT 1 FROM TaskSubmission WHERE taskID = ? AND userID = ?";
            try (PreparedStatement ps = con.prepareStatement(checkSql)) {
                ps.setInt(1, taskID);
                ps.setInt(2, userID);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) { con.rollback(); return null; }
                }
            }

            // Load this task's questions and the correct option per question.
            String qSql = "SELECT questionID FROM TaskQuestion WHERE taskID = ?";
            List<Integer> questionIDs = new ArrayList<>();
            try (PreparedStatement ps = con.prepareStatement(qSql)) {
                ps.setInt(1, taskID);
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) questionIDs.add(rs.getInt("questionID"));
                }
            }

            String correctSql = "SELECT optionID FROM TaskOption WHERE questionID = ? AND isCorrect = 1 LIMIT 1";
            // Grade each question.
            int score = 0;
            List<int[]> graded = new ArrayList<>(); // {questionID, chosenOptionID(-1 if null), isCorrect(0/1)}
            for (int questionID : questionIDs) {
                int correctOptionID = -1;
                try (PreparedStatement ps = con.prepareStatement(correctSql)) {
                    ps.setInt(1, questionID);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) correctOptionID = rs.getInt("optionID");
                    }
                }
                Integer chosen = answers.get(questionID);
                boolean isCorrect = chosen != null && chosen == correctOptionID;
                if (isCorrect) score++;
                graded.add(new int[]{ questionID, chosen == null ? -1 : chosen, isCorrect ? 1 : 0 });
            }

            int awarded = score * 10;

            // Insert the submission.
            int submissionID;
            String insSub = "INSERT INTO TaskSubmission (taskID, userID, score, awardedMadibucks) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(insSub, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, taskID);
                ps.setInt(2, userID);
                ps.setInt(3, score);
                ps.setInt(4, awarded);
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    keys.next();
                    submissionID = keys.getInt(1);
                }
            }

            // Insert per-question answers.
            String insAns = "INSERT INTO TaskAnswer (submissionID, questionID, chosenOptionID, isCorrect) VALUES (?, ?, ?, ?)";
            try (PreparedStatement ps = con.prepareStatement(insAns)) {
                for (int[] g : graded) {
                    ps.setInt(1, submissionID);
                    ps.setInt(2, g[0]);
                    if (g[1] == -1) ps.setNull(3, java.sql.Types.INTEGER); else ps.setInt(3, g[1]);
                    ps.setInt(4, g[2]);
                    ps.addBatch();
                }
                ps.executeBatch();
            }

            // Also record a TaskCompletion row for compatibility with existing views.
            String insComp = "INSERT INTO TaskCompletion (taskID, userID, completionStatus, completionDate) VALUES (?, ?, ?, NOW())";
            try (PreparedStatement ps = con.prepareStatement(insComp)) {
                ps.setInt(1, taskID);
                ps.setInt(2, userID);
                ps.setString(3, score > 0 ? "COMPLETED" : "REJECTED");
                ps.executeUpdate();
            }

            // Credit MadiBucks (10 per correct) in the SAME transaction.
            if (awarded > 0) {
                String getAcc = "SELECT accountID FROM Account WHERE userID = ? FOR UPDATE";
                int accountID = -1;
                try (PreparedStatement ps = con.prepareStatement(getAcc)) {
                    ps.setInt(1, userID);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) accountID = rs.getInt("accountID");
                    }
                }
                if (accountID != -1) {
                    BigDecimal delta = new BigDecimal(awarded);
                    try (PreparedStatement ps = con.prepareStatement("UPDATE Account SET balance = balance + ? WHERE accountID = ?")) {
                        ps.setBigDecimal(1, delta);
                        ps.setInt(2, accountID);
                        ps.executeUpdate();
                    }
                    try (PreparedStatement ps = con.prepareStatement("INSERT INTO `Transaction` (payoutAmount, description, accountID) VALUES (?, ?, ?)")) {
                        ps.setBigDecimal(1, delta);
                        ps.setString(2, "Quiz reward: " + score + " correct on task " + taskID);
                        ps.setInt(3, accountID);
                        ps.executeUpdate();
                    }
                }
            }

            con.commit();

            Map<String, Object> result = new HashMap<>();
            result.put("success", true);
            result.put("score", score);
            result.put("total", questionIDs.size());
            result.put("awardedMadibucks", awarded);
            return result;
        } catch (SQLException e) {
            if (con != null) try { con.rollback(); } catch (SQLException ignore) {}
            throw e;
        } finally {
            if (con != null) try { con.setAutoCommit(true); con.close(); } catch (SQLException ignore) {}
        }
    }

}

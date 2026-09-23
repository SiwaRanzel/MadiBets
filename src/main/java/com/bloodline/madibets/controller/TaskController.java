package com.bloodline.madibets.controller;

import com.bloodline.madibets.dao.GroupDAO;
import com.bloodline.madibets.dao.TaskDAO;
import com.bloodline.madibets.dao.UserDAO;
import com.bloodline.madibets.model.Task;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*")
public class TaskController {

    private final TaskDAO taskDAO = new TaskDAO();
    private final GroupDAO groupDAO = new GroupDAO();
    private final UserDAO userDAO = new UserDAO();

    @GetMapping
    public ResponseEntity<?> getTasks(@RequestParam(required = false) Integer createdBy,
                                      @RequestParam(required = false) Integer groupID) {
        try {
            if (groupID != null) {
                List<Task> tasks = taskDAO.findByGroup(groupID);
                return ResponseEntity.ok(tasks);
            }
            if (createdBy != null) {
                List<Task> tasks = taskDAO.findByCreator(createdBy);
                return ResponseEntity.ok(tasks);
            }
            return ResponseEntity.badRequest().body(Map.of("error", "Provide createdBy or groupID parameter."));
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Database error."));
        }
    }



    /**
     * GET /api/tasks/{id}/quiz?userId=N — fetch a quiz for a student to take.
     * Options come back WITHOUT correctness until the student has submitted;
     * once submitted, the stored per-question results and score are included.
     */
    @GetMapping("/{id}/quiz")
    public ResponseEntity<?> getQuiz(@PathVariable int id, @RequestParam(required = false) Integer userId) {
        try {
            int uid = userId == null ? 0 : userId;
            // Privileged viewers (the creating lecturer, or any admin) always see
            // the correct answers plus the completion summary.
            boolean privileged = false;
            if (uid > 0) {
                String role = userDAO.getUserType(uid);
                boolean isAdmin = "ADMIN".equalsIgnoreCase(role);
                boolean isCreator = "LECTURER".equalsIgnoreCase(role) && taskDAO.getCreatedBy(id) == uid;
                privileged = isAdmin || isCreator;
            }
            Map<String, Object> quiz = taskDAO.getQuiz(id, uid, privileged);
            return ResponseEntity.ok(quiz);
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Database error."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/tasks/{id}/submit — submit a whole quiz (student only).
     * Body: { userID, answers: [ { questionID, chosenOptionID } ] }.
     * Grades all questions, awards 10 MadiBucks per correct answer, and locks
     * the quiz to a single attempt.
     */
    @PostMapping("/{id}/submit")
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> submitQuiz(@PathVariable int id, @RequestBody Map<String, Object> body) {
        try {
            Object userIDRaw = body.get("userID");
            if (!(userIDRaw instanceof Number)) {
                return ResponseEntity.badRequest().body(Map.of("error", "userID must be numeric"));
            }
            int userID = ((Number) userIDRaw).intValue();

            // Only students can complete tasks (enforced on the backend).
            String role = userDAO.getUserType(userID);
            if (!"STUDENT".equalsIgnoreCase(role)) {
                return ResponseEntity.status(403).body(Map.of("error", "Only students can complete tasks."));
            }

            // Must be a member of the quiz's group to take it.
            int groupID = taskDAO.getGroupIdForTask(id);
            if (groupID == -1) {
                return ResponseEntity.status(404).body(Map.of("error", "Quiz not found."));
            }
            if (!groupDAO.isMember(groupID, userID)) {
                return ResponseEntity.status(403).body(Map.of("error", "You must be a member of this group to take its quizzes."));
            }

            Object answersRaw = body.get("answers");
            if (!(answersRaw instanceof List<?> aList)) {
                return ResponseEntity.badRequest().body(Map.of("error", "answers must be a list"));
            }
            Map<Integer, Integer> answers = new HashMap<>();
            for (Object aObj : aList) {
                if (!(aObj instanceof Map<?, ?> am)) continue;
                Object qRaw = am.get("questionID");
                if (!(qRaw instanceof Number)) continue;
                int questionID = ((Number) qRaw).intValue();
                Object cRaw = am.get("chosenOptionID");
                Integer chosen = (cRaw instanceof Number n) ? n.intValue() : null;
                answers.put(questionID, chosen);
            }

            Map<String, Object> result = taskDAO.submitQuiz(id, userID, answers);
            if (result == null) {
                return ResponseEntity.status(409).body(Map.of("error", "You have already completed this quiz."));
            }
            return ResponseEntity.ok(result);
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Database error."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * DELETE /api/tasks/{id}?userID=N — delete a task/quiz (lecturer only).
     * Enforced on the backend via userType, not just the UI.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTask(@PathVariable int id, @RequestParam int userID) {
        try {
            String role = userDAO.getUserType(userID);
            if (!"LECTURER".equalsIgnoreCase(role)) {
                return ResponseEntity.status(403).body(Map.of("error", "Only lecturers can delete tasks."));
            }
            boolean deleted = taskDAO.deleteTask(id);
            if (deleted) {
                return ResponseEntity.ok(Map.of("success", true));
            }
            return ResponseEntity.status(404).body(Map.of("error", "Task not found."));
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Database error."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * POST /api/tasks/{id}/reveal?userID=N — reveal quiz results to students.
     * Only the LECTURER who created the quiz may reveal it.
     */
    @PostMapping("/{id}/reveal")
    public ResponseEntity<?> revealQuiz(@PathVariable int id, @RequestParam int userID) {
        try {
            String role = userDAO.getUserType(userID);
            if (!"LECTURER".equalsIgnoreCase(role)) {
                return ResponseEntity.status(403).body(Map.of("error", "Only lecturers can reveal quiz results."));
            }
            if (taskDAO.getCreatedBy(id) != userID) {
                return ResponseEntity.status(403).body(Map.of("error", "Only the lecturer who created this quiz can reveal its results."));
            }
            boolean updated = taskDAO.setRevealed(id, true);
            if (updated) {
                return ResponseEntity.ok(Map.of("success", true));
            }
            return ResponseEntity.status(404).body(Map.of("error", "Quiz not found."));
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Database error."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}

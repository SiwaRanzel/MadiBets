package com.bloodline.madibets.controller;

import com.bloodline.madibets.dao.TaskDAO;
import com.bloodline.madibets.dao.UserDAO;
import com.bloodline.madibets.model.Task;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class TaskController {

    private final TaskDAO taskDAO = new TaskDAO();
    private final UserDAO userDAO = new UserDAO();

    /** Lecturer-only: create a True/False task for a group. */
    @PostMapping("/groups/{groupId}/tasks")
    public ResponseEntity<?> createTask(@PathVariable int groupId, @RequestBody Map<String, Object> body) {
        try {
            if (body == null || !body.containsKey("createdBy")) {
                return ResponseEntity.badRequest().body(Map.of("error", "createdBy (lecturer userID) is required"));
            }
            int createdBy = ((Number) body.get("createdBy")).intValue();

            // Only lecturers may create tasks
            String userType = userDAO.getUserType(createdBy);
            if (!"LECTURER".equalsIgnoreCase(userType)) {
                return ResponseEntity.status(403).body(Map.of("error", "Only lecturers can create tasks"));
            }

            String question = body.get("question") != null ? body.get("question").toString() : null;
            if (question == null || question.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Question is required"));
            }

            boolean correctAnswer = body.get("correctAnswer") != null && Boolean.parseBoolean(body.get("correctAnswer").toString());
            double amount = body.get("amount") != null ? ((Number) body.get("amount")).doubleValue() : 0.0;

            Task t = new Task();
            t.setGroupID(groupId);
            t.setQuestion(question);
            t.setCorrectAnswer(correctAnswer);
            t.setAmount(amount);
            t.setCreatedBy(createdBy);

            int taskID = taskDAO.create(t);
            if (taskID > 0) {
                return ResponseEntity.status(201).body(Map.of("taskID", taskID));
            } else {
                return ResponseEntity.internalServerError().body(Map.of("error", "Create failed"));
            }
        } catch (ClassCastException cce) {
            return ResponseEntity.badRequest().body(Map.of("error", "createdBy must be numeric"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /** List tasks for a group, with the requesting user's answered state. */
    @GetMapping("/groups/{groupId}/tasks")
    public ResponseEntity<?> listTasks(@PathVariable int groupId, @RequestParam(required = false) Integer userId) {
        try {
            if (userId == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "userId is required"));
            }
            List<Task> tasks = taskDAO.findByGroup(groupId, userId);
            return ResponseEntity.ok(tasks);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /** Submit a student's True/False answer for a task. */
    @PostMapping("/tasks/{taskId}/answer")
    public ResponseEntity<?> submitAnswer(@PathVariable int taskId, @RequestBody Map<String, Object> body) {
        try {
            if (body == null || !body.containsKey("userID") || !body.containsKey("answer")) {
                return ResponseEntity.badRequest().body(Map.of("error", "userID and answer are required"));
            }
            int userID = ((Number) body.get("userID")).intValue();
            boolean answer = Boolean.parseBoolean(body.get("answer").toString());

            boolean success = taskDAO.submitAnswer(taskId, userID, answer);
            if (success) {
                return ResponseEntity.ok(Map.of("success", true));
            } else {
                return ResponseEntity.status(409).body(Map.of("error", "You have already answered this task"));
            }
        } catch (ClassCastException cce) {
            return ResponseEntity.badRequest().body(Map.of("error", "userID must be numeric"));
        } catch (Exception e) {
            String msg = e.getMessage();
            if (msg != null && msg.contains("member")) {
                return ResponseEntity.status(403).body(Map.of("error", msg));
            }
            return ResponseEntity.internalServerError().body(Map.of("error", msg != null ? msg : "Server error"));
        }
    }
}
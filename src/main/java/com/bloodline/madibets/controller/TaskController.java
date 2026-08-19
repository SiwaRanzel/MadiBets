package com.bloodline.madibets.controller;

import com.bloodline.madibets.dao.TaskDAO;
import com.bloodline.madibets.model.Task;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/tasks")
@CrossOrigin(origins = "*")
public class TaskController {

    private final TaskDAO taskDAO = new TaskDAO();

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

    @PostMapping
    public ResponseEntity<?> createTask(@RequestBody Task task) {
        try {
            if (task.getTitle() == null || task.getTitle().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Task title is required."));
            }
            if (task.getGroupID() <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "groupID is required."));
            }
            if (task.getAmount() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "MadiBucks reward amount is required."));
            }
            if (task.getCreatedBy() <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "createdBy is required."));
            }

            Task created = taskDAO.create(task);
            return ResponseEntity.status(201).body(Map.of("taskID", created.getTaskID(), "message", "Task created!"));
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Database error: " + e.getMessage()));
        }
    }

    @PostMapping("/{taskID}/complete")
    public ResponseEntity<?> completeTask(@PathVariable int taskID, @RequestBody Map<String, Integer> body) {
        try {
            Integer userID = body.get("userID");
            if (userID == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "userID is required."));
            }
            boolean success = taskDAO.submitCompletion(taskID, userID);
            if (success) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Task completion submitted!"));
            } else {
                return ResponseEntity.status(409).body(Map.of("error", "You have already submitted this task."));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Database error: " + e.getMessage()));
        }
    }

    @GetMapping("/{taskID}/completions")
    public ResponseEntity<?> getCompletions(@PathVariable int taskID) {
        try {
            List<Map<String, Object>> completions = taskDAO.getCompletions(taskID);
            return ResponseEntity.ok(completions);
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Database error."));
        }
    }

    @PutMapping("/completions/{completionID}/status")
    public ResponseEntity<?> updateCompletionStatus(@PathVariable int completionID, @RequestBody Map<String, String> body) {
        try {
            String status = body.get("status");
            if (status == null || status.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "status is required (COMPLETED or REJECTED)."));
            }
            boolean success = taskDAO.updateCompletionStatus(completionID, status);
            if (success) {
                return ResponseEntity.ok(Map.of("success", true));
            }
            return ResponseEntity.notFound().build();
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Database error."));
        }
    }

    @GetMapping("/{taskID}/status")
    public ResponseEntity<?> getMyCompletionStatus(@PathVariable int taskID, @RequestParam int userID) {
        try {
            String status = taskDAO.getCompletionStatus(taskID, userID);
            return ResponseEntity.ok(Map.of("status", status != null ? status : "NOT_SUBMITTED"));
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Database error."));
        }
    }
}

package com.bloodline.madibets.controller;

import com.bloodline.madibets.dao.GroupDAO;
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
    private final GroupDAO groupDAO = new GroupDAO();

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



    /** POST /api/tasks/{id}/answer — submit a True/False answer for a task. */
    @PostMapping("/{id}/answer")
    public ResponseEntity<?> answerTask(@PathVariable int id, @RequestBody Map<String, Object> body) {
        try {
            Object userIDRaw = body.get("userID");
            if (!(userIDRaw instanceof Number)) {
                return ResponseEntity.badRequest().body(Map.of("error", "userID must be numeric"));
            }
            Object answerRaw = body.get("answer");
            if (!(answerRaw instanceof Boolean)) {
                return ResponseEntity.badRequest().body(Map.of("error", "answer must be true or false"));
            }

            int userID = ((Number) userIDRaw).intValue();
            boolean answer = (Boolean) answerRaw;

            int result = taskDAO.answerTask(id, userID, answer);
            if (result == 1) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "correct", true,
                        "message", "Correct answer! MadiBucks awarded."
                ));
            } else if (result == 0) {
                return ResponseEntity.ok(Map.of(
                        "success", true,
                        "correct", false,
                        "message", "Incorrect answer. No MadiBucks awarded."
                ));
            } else {
                return ResponseEntity.status(409).body(Map.of("error", "You have already answered this task."));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Database error."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}

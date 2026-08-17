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
    public ResponseEntity<?> getTasks(@RequestParam(required = false) Integer createdBy) {
        try {
            if (createdBy != null) {
                List<Task> tasks = taskDAO.findByCreator(createdBy);
                return ResponseEntity.ok(tasks);
            }
            return ResponseEntity.badRequest().body(Map.of("error", "Missing required parameter: createdBy"));
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Database error."));
        }
    }
}

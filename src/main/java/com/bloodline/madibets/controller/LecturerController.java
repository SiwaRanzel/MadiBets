package com.bloodline.madibets.controller;

import com.bloodline.madibets.dao.LecturerDAO;
import com.bloodline.madibets.model.LecturerStats;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/lecturers")
@CrossOrigin(origins = "*")
public class LecturerController {

    private final LecturerDAO lecturerDAO = new LecturerDAO();

    @GetMapping("/{lecturerId}/dashboard-stats")
    public ResponseEntity<?> getDashboardStats(@PathVariable int lecturerId) {
        try {
            LecturerStats stats = lecturerDAO.getDashboardStats(lecturerId);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(java.util.Map.of("error", e.getMessage()));
        }
    }
}

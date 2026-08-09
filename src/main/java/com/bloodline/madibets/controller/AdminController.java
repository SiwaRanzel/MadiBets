package com.bloodline.madibets.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.bloodline.madibets.dao.AdminDAO;
import com.bloodline.madibets.model.DashboardStats;

import java.sql.SQLException;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminDAO adminDAO = new AdminDAO();

    @GetMapping("/dashboard-stats")
    public ResponseEntity<?> getDashboardStats() {
        try {
            DashboardStats stats = adminDAO.getDashboardStats();
            return ResponseEntity.ok(stats);
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body("{\"error\": \"Failed to retrieve dashboard stats\"}");
        }
    }
}

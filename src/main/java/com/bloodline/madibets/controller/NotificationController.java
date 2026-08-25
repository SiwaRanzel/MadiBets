package com.bloodline.madibets.controller;

import com.bloodline.madibets.dao.NotificationDAO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLException;
import java.util.Map;

/**
 * Owner: Kieran (B-series). Login-time notifications (bet settlement messages).
 *
 * Routes (all under /api/notifications):
 *   GET  /{userID}        the user's unseen messages, oldest first
 *   POST /{userID}/seen   flag them seen (called after the UI has shown them)
 */
@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    private final NotificationDAO notificationDAO = new NotificationDAO();

    @GetMapping("/{userID}")
    public ResponseEntity<?> unseen(@PathVariable("userID") int userID) {
        try {
            return ResponseEntity.ok(Map.of("notifications", notificationDAO.findUnseen(userID)));
        } catch (SQLException e) {
            e.printStackTrace();   // console only — no SQL detail leaves the server (POPI)
            return ResponseEntity.internalServerError().body(Map.of("error", "Database error. Please try again."));
        }
    }

    @PostMapping("/{userID}/seen")
    public ResponseEntity<?> markSeen(@PathVariable("userID") int userID) {
        try {
            int n = notificationDAO.markAllSeen(userID);
            return ResponseEntity.ok(Map.of("marked", n));
        } catch (SQLException e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().body(Map.of("error", "Database error. Please try again."));
        }
    }
}

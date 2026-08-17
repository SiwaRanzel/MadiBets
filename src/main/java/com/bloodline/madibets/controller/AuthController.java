package com.bloodline.madibets.controller;

import com.bloodline.madibets.dao.UserDAO;
import com.bloodline.madibets.model.User;
import com.bloodline.madibets.service.AuthService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService authService = new AuthService();
    private final UserDAO userDAO = new UserDAO();

    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        // Basic validation
        if (user.getName() == null || user.getName().isBlank() ||
            user.getSurname() == null || user.getSurname().isBlank() ||
            user.getEmail() == null || user.getEmail().isBlank() ||
            user.getPassword() == null || user.getPassword().isBlank() ||
            user.getUserType() == null || user.getUserType().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "All required fields must be provided."));
        }

        // Only @mandela.ac.za emails are permitted
        if (!user.getEmail().toLowerCase().endsWith("@mandela.ac.za")) {
            return ResponseEntity.badRequest().body(
                Map.of("error", "Only @mandela.ac.za email addresses are allowed."));
        }

        // Enforce studentNo / staffNo based on role
        if ("STUDENT".equalsIgnoreCase(user.getUserType())) {
            String sno = user.getStudentNo();
            if (sno == null || sno.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Student number is required."));
            }
            if (!sno.matches("\\d{9}")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Student number must be exactly 9 digits."));
            }
        }
        if ("LECTURER".equalsIgnoreCase(user.getUserType()) &&
            (user.getStaffNo() == null || user.getStaffNo().isBlank())) {
            return ResponseEntity.badRequest().body(Map.of("error", "Staff number is required."));
        }

        try {
            int newId = authService.register(user, user.getPassword());
            if (newId != -1) {
                Map<String, Object> response = new HashMap<>();
                response.put("userID", newId);
                response.put("message", "Registration successful!");
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Registration failed. Please try again."));
            }
        } catch (SQLIntegrityConstraintViolationException e) {
            // Duplicate email or studentNo/staffNo
            String msg = e.getMessage();
            if (msg != null && msg.contains("email")) {
                return ResponseEntity.badRequest().body(Map.of("error", "An account with this email already exists."));
            } else if (msg != null && msg.contains("studentNo")) {
                return ResponseEntity.badRequest().body(Map.of("error", "This student number is already registered."));
            } else if (msg != null && msg.contains("staffNo")) {
                return ResponseEntity.badRequest().body(Map.of("error", "This staff number is already registered."));
            }
            return ResponseEntity.badRequest().body(Map.of("error", "Duplicate entry. Please check your details."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Server error: " + e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        try {
            String email = credentials.get("email");
            String password = credentials.get("password");

            if (email == null || email.isBlank() || password == null || password.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Email and password are required."));
            }

            User u = authService.login(email, password);
            if (u != null) {
                com.bloodline.madibets.dao.DeletionRequestDAO drDao = new com.bloodline.madibets.dao.DeletionRequestDAO();
                if (drDao.hasPendingDeleteRequest(u.getUserID())) {
                    return ResponseEntity.status(403).body(Map.of("error", "Your account deletion is pending. Please contact support if this was a mistake."));
                }
                
                u.setPassword(null); // hide password
                double balance = userDAO.getAccountBalance(u.getUserID());
                Map<String, Object> response = new HashMap<>();
                response.put("user", u);
                response.put("balance", balance);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.status(401).body(Map.of("error", "Invalid email or password."));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Server error: " + e.getMessage()));
        }
    }
}

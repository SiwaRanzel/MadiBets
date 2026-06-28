package com.bloodline.madibets.controller;

import com.bloodline.madibets.dao.UserDAO;
import com.bloodline.madibets.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserDAO userDAO = new UserDAO();

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserProfile(@PathVariable int id) {
        try {
            User u = userDAO.findById(id);
            if (u != null) {
                u.setPassword(null); // hide password
                double balance = userDAO.getAccountBalance(id);
                Map<String, Object> response = new HashMap<>();
                response.put("user", u);
                response.put("balance", balance);
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable int id, @RequestBody User user) {
        try {
            user.setUserID(id);
            boolean updated = userDAO.updateProfile(user);
            if (updated) {
                return ResponseEntity.ok(Map.of("success", true));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Update failed."));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable int id) {
        try {
            boolean deleted = userDAO.delete(id);
            if (deleted) {
                return ResponseEntity.ok(Map.of("success", true));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Delete failed."));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}

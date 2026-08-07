package com.bloodline.madibets.controller;

import com.bloodline.madibets.dao.UserDAO;
import com.bloodline.madibets.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.mindrot.jbcrypt.BCrypt;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserDAO userDAO = new UserDAO();

    @GetMapping("/all")
    public ResponseEntity<?> getAllUsers() {
        try {
            List<User> users = userDAO.getAllUsers();
            for (User u : users) {
                u.setPassword(null); // hide passwords
            }
            return ResponseEntity.ok(users);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/lookup")
    public ResponseEntity<?> lookupByEmail(@RequestParam String email) {
        try {
            User u = userDAO.findByEmail(email);
            if (u != null) {
                u.setPassword(null);
                return ResponseEntity.ok(u);
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "No user found with that email."));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/count")
    public ResponseEntity<?> getUserCounts() {
        try {
            int totalUsers = userDAO.countAllUsers();
            int totalStudents = userDAO.countStudents();
            Map<String, Object> counts = new HashMap<>();
            counts.put("totalUsers", totalUsers);
            counts.put("totalStudents", totalStudents);
            return ResponseEntity.ok(counts);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id:\\d+}")
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

    @PutMapping("/{id:\\d+}")
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

    @DeleteMapping("/{id:\\d+}")
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

    @PostMapping("/{id:\\d+}/avatar")
    public ResponseEntity<?> uploadAvatar(@PathVariable int id, @RequestParam("avatar") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Empty file"));
            }
            Path uploadDir = Paths.get("uploads", "avatars").toAbsolutePath();
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            // Delete ALL old avatar files for this user by scanning the directory
            String prefix = id + "_avatar";
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(uploadDir, prefix + ".*")) {
                for (Path oldFile : stream) {
                    Files.deleteIfExists(oldFile);
                }
            } catch (IOException ignored) {
                // Directory might not exist yet on first upload, that's fine
            }

            // Save new avatar as {userId}_avatar.{ext}
            String originalName = file.getOriginalFilename();
            String ext = "";
            if (originalName != null && originalName.contains(".")) {
                ext = originalName.substring(originalName.lastIndexOf('.'));
            }
            String filename = id + "_avatar" + ext;
            Path filePath = uploadDir.resolve(filename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            String avatarUrl = "/uploads/avatars/" + filename;
            boolean updated = userDAO.updateAvatar(id, avatarUrl);
            
            if (updated) {
                return ResponseEntity.ok(Map.of("success", true, "avatarUrl", avatarUrl));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "User not found"));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id:\\d+}/password")
    public ResponseEntity<?> updatePassword(@PathVariable int id, @RequestBody Map<String, String> payload) {
        try {
            String newPassword = payload.get("password");
            if (newPassword == null || newPassword.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Password is required"));
            }
            String hashed = BCrypt.hashpw(newPassword, BCrypt.gensalt());
            // We need a DAO method to update password, let's implement it inline here for simplicity,
            // but properly it should be in UserDAO.
            String sql = "UPDATE User SET password=? WHERE userID=?";
            try (java.sql.Connection con = com.bloodline.madibets.config.DatabaseConnection.getConnection();
                 java.sql.PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, hashed);
                ps.setInt(2, id);
                if (ps.executeUpdate() > 0) {
                    return ResponseEntity.ok(Map.of("success", true));
                }
            }
            return ResponseEntity.badRequest().body(Map.of("error", "Update failed."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}

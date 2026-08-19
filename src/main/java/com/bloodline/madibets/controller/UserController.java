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
    public ResponseEntity<?> lookupUser(@RequestParam(required = false) String email,
                                        @RequestParam(required = false) String studentNo) {
        try {
            User u = null;
            if (studentNo != null && !studentNo.isBlank()) {
                u = userDAO.findByStudentNo(studentNo);
            } else if (email != null && !email.isBlank()) {
                u = userDAO.findByEmail(email);
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "Provide email or studentNo."));
            }

            if (u != null) {
                u.setPassword(null);
                return ResponseEntity.ok(u);
            } else {
                return ResponseEntity.status(404).body(Map.of("error", "No user found."));
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
            double userGrowth = userDAO.getWeeklyGrowth(false);
            double studentGrowth = userDAO.getWeeklyGrowth(true);
            
            return ResponseEntity.ok(Map.of(
                "totalUsers", totalUsers,
                "totalStudents", totalStudents,
                "userWeeklyGrowth", userGrowth,
                "studentWeeklyGrowth", studentGrowth
            ));
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

    @PostMapping("/{id:\\d+}/delete-request")
    public ResponseEntity<?> submitDeleteRequest(@PathVariable int id) {
        try {
            com.bloodline.madibets.dao.DeletionRequestDAO dao = new com.bloodline.madibets.dao.DeletionRequestDAO();
            com.bloodline.madibets.model.AccountDeletionRequest req = dao.create(id);
            if (req != null) {
                return ResponseEntity.ok(Map.of("success", true, "requestID", req.getRequestID()));
            }
            return ResponseEntity.badRequest().body(Map.of("error", "Failed to create delete request"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/delete-requests")
    public ResponseEntity<?> getAllDeleteRequests() {
        try {
            com.bloodline.madibets.dao.DeletionRequestDAO dao = new com.bloodline.madibets.dao.DeletionRequestDAO();
            return ResponseEntity.ok(dao.getAll());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
    @PutMapping("/delete-requests/{reqId:\\d+}/reinstate")
    public ResponseEntity<?> reinstateAccount(@PathVariable int reqId) {
        try {
            com.bloodline.madibets.dao.DeletionRequestDAO dao = new com.bloodline.madibets.dao.DeletionRequestDAO();
            boolean ok = dao.updateStatus(reqId, "REJOIN");
            if (ok) return ResponseEntity.ok(Map.of("success", true));
            return ResponseEntity.badRequest().body(Map.of("error", "Update failed."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}

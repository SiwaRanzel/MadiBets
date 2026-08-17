package com.bloodline.madibets.controller;

import com.bloodline.madibets.dao.GroupDAO;
import com.bloodline.madibets.model.Group;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.bloodline.madibets.config.DatabaseConnection;

@RestController
@RequestMapping("/api/groups")
@CrossOrigin(origins = "*")
public class GroupController {

    private final GroupDAO groupDAO = new GroupDAO();

    @PostMapping("")
    public ResponseEntity<?> createGroup(@RequestBody Group g) {
        try {
            if (g.getGroupName() == null || g.getGroupName().isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Group name is required"));
            }
            int id = groupDAO.create(g);
            if (id > 0) {
                return ResponseEntity.status(201).body(Map.of("groupID", id));
            } else {
                return ResponseEntity.internalServerError().body(Map.of("error", "Create failed"));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("")
    public ResponseEntity<?> listGroups(@RequestParam(required = false) String q,
                                        @RequestParam(required = false) Integer userId,
                                        @RequestParam(required = false) Integer createdBy) {
        try {
            List<Object> out = new ArrayList<>();
            
            if (createdBy != null) {
                out.addAll(groupDAO.findByCreator(createdBy));
                return ResponseEntity.ok(out);
            }
            
            if (userId != null) {
                Map<String,Object> overall = Map.of(
                        "groupID", 0,
                        "groupName", "Overall",
                        "description", "All registered users (excluding admin)",
                        "virtual", true
                );
                Map<String,Object> myfriends = Map.of(
                        "groupID", -1,
                        "groupName", "My Friends",
                        "description", "Your friends",
                        "virtual", true
                );
                out.add(overall);
                out.add(myfriends);
                out.addAll(groupDAO.findByUser(userId));
            }

            if (q == null || q.isBlank()) {
                return ResponseEntity.ok(out);
            }

            List<Group> found = groupDAO.searchByNameOrId(q);
            out.addAll(found);
            return ResponseEntity.ok(out);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getGroup(@PathVariable int id, @RequestParam(required = false) Integer userId) {
        try {
            if (id == 0) {
                // Overall virtual group: provide summary
                int count = 0;
                try (Connection con = DatabaseConnection.getConnection();
                     PreparedStatement ps = con.prepareStatement("SELECT COUNT(*) AS c FROM User WHERE userType <> 'ADMIN'")) {
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) count = rs.getInt("c");
                    }
                }
                Map<String,Object> resp = new HashMap<>();
                resp.put("groupID", 0);
                resp.put("groupName", "Overall");
                resp.put("description", "All registered users (excluding admin)");
                resp.put("memberCount", count);
                return ResponseEntity.ok(resp);
            } else if (id == -1) {
                // My Friends virtual group: compute friend count for userId
                if (userId == null) {
                    return ResponseEntity.badRequest().body(Map.of("error", "userId required for My Friends"));
                }
                int count = 0;
                String sql = "SELECT COUNT(*) AS c FROM Friendship WHERE (requesterID = ? OR addresseID = ?) AND status = 'ACCEPTED'";
                try (Connection con = DatabaseConnection.getConnection();
                     PreparedStatement ps = con.prepareStatement(sql)) {
                    ps.setInt(1, userId);
                    ps.setInt(2, userId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) count = rs.getInt("c");
                    }
                }
                Map<String,Object> resp = new HashMap<>();
                resp.put("groupID", -1);
                resp.put("groupName", "My Friends");
                resp.put("description", "Your friends");
                resp.put("memberCount", count);
                return ResponseEntity.ok(resp);
            }

            Group g = groupDAO.findById(id);
            if (g == null) return ResponseEntity.notFound().build();

            // Build a richer response: group + members + memberCount + isMember
            Map<String, Object> resp = new HashMap<>();
            resp.put("groupID", g.getGroupID());
            resp.put("groupName", g.getGroupName());
            resp.put("description", g.getDescription());
            resp.put("createdBy", g.getCreatedBy());
            resp.put("createdDate", g.getCreatedDate());
            resp.put("memberCount", groupDAO.getMemberCount(id));
            resp.put("members", groupDAO.findMembers(id));
            if (userId != null) {
                resp.put("isMember", groupDAO.isMember(id, userId));
            } else {
                resp.put("isMember", false);
            }
            return ResponseEntity.ok(resp);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<?> joinGroup(@PathVariable int id, @RequestBody Map<String, Object> body) {
        try {
            if (id <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Cannot join virtual group"));
            }
            if (body == null || !body.containsKey("userID")) {
                return ResponseEntity.badRequest().body(Map.of("error", "userID required in body"));
            }
            int userID = (int) ((Number) body.get("userID")).intValue();
            boolean added = groupDAO.addMember(id, userID);
            if (added) {
                return ResponseEntity.ok(Map.of("success", true));
            } else {
                return ResponseEntity.status(409).body(Map.of("error", "Already a member"));
            }
        } catch (ClassCastException cce) {
            return ResponseEntity.badRequest().body(Map.of("error", "userID must be numeric"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}

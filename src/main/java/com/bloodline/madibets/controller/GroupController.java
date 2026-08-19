package com.bloodline.madibets.controller;

import com.bloodline.madibets.dao.GroupDAO;
import com.bloodline.madibets.dao.TaskDAO;
import com.bloodline.madibets.model.Group;
import com.bloodline.madibets.model.Task;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/groups")
@CrossOrigin(origins = "*")
public class GroupController {

    private final GroupDAO groupDAO = new GroupDAO();
    private final TaskDAO taskDAO = new TaskDAO();

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
                // Only real groups the user belongs to — no virtual "Overall"/"My Friends" groups.
                out.addAll(groupDAO.findByUser(userId));
            }

            if (q == null || q.isBlank()) {
                // No search term: return all groups (limit 100) so the admin
                // dashboard and group list always show something.
                if (userId == null) {
                    out.addAll(groupDAO.searchByNameOrId(null));
                }
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
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid group id"));
            }
            if (body == null || !body.containsKey("userID")) {
                return ResponseEntity.badRequest().body(Map.of("error", "userID required in body"));
            }
            Object raw = body.get("userID");
            if (!(raw instanceof Number)) {
                return ResponseEntity.badRequest().body(Map.of("error", "userID must be numeric"));
            }
            int userID = ((Number) raw).intValue();
            boolean added = groupDAO.addMember(id, userID);
            if (added) {
                return ResponseEntity.ok(Map.of("success", true));
            } else {
                return ResponseEntity.status(409).body(Map.of("error", "Already a member"));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    // ------------------------------------------------------------------
    // Tasks scoped to a group (D400/D500)
    // ------------------------------------------------------------------

    /** GET /api/groups/{id}/tasks?userId=N — list tasks with the user's answer status. */
    @GetMapping("/{id}/tasks")
    public ResponseEntity<?> getGroupTasks(@PathVariable int id,
                                           @RequestParam(required = false) Integer userId) {
        try {
            if (id <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid group id"));
            }
            if (userId == null) {
                // No user context: return plain tasks without answer status
                return ResponseEntity.ok(taskDAO.findByGroup(id));
            }
            return ResponseEntity.ok(taskDAO.findByGroupWithStatus(id, userId));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    /** POST /api/groups/{id}/tasks — create a task in a group (lecturer only). */
    @PostMapping("/{id}/tasks")
    public ResponseEntity<?> createGroupTask(@PathVariable int id, @RequestBody Map<String, Object> body) {
        try {
            if (id <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid group id"));
            }
            String question = body.get("question") instanceof String s ? s.trim() : null;
            if (question == null || question.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Question is required"));
            }
            Object correctRaw = body.get("correctAnswer");
            if (!(correctRaw instanceof Boolean)) {
                return ResponseEntity.badRequest().body(Map.of("error", "correctAnswer must be true or false"));
            }
            Object amountRaw = body.get("amount");
            if (!(amountRaw instanceof Number)) {
                return ResponseEntity.badRequest().body(Map.of("error", "amount must be numeric"));
            }
            Object createdByRaw = body.get("createdBy");
            if (!(createdByRaw instanceof Number)) {
                return ResponseEntity.badRequest().body(Map.of("error", "createdBy must be numeric"));
            }
            int createdBy = ((Number) createdByRaw).intValue();

            // Lecturer must be a member of the group to create tasks for it
            if (!groupDAO.isMember(id, createdBy)) {
                return ResponseEntity.status(403).body(Map.of("error", "You must be a member of this group to create tasks."));
            }

            Task task = new Task();
            task.setTitle(question);
            task.setDescription(question);
            task.setGroupID(id);
            task.setAmount(new java.math.BigDecimal(String.valueOf(amountRaw)));
            task.setCreatedBy(createdBy);
            task.setCorrectAnswer((Boolean) correctRaw);

            Task saved = taskDAO.create(task);
            return ResponseEntity.status(201).body(Map.of(
                    "taskID", saved.getTaskID(),
                    "message", "Task created successfully!"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}
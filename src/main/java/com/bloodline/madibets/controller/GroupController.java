package com.bloodline.madibets.controller;

import com.bloodline.madibets.dao.GroupDAO;
import com.bloodline.madibets.dao.TaskDAO;
import com.bloodline.madibets.dao.UserDAO;
import com.bloodline.madibets.model.Group;
import com.bloodline.madibets.model.Task;
import com.bloodline.madibets.model.TaskQuestion;
import com.bloodline.madibets.model.TaskOption;
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
    private final UserDAO userDAO = new UserDAO();

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
            resp.put("hasPassword", groupDAO.hasPassword(id));
            resp.put("maxMembers", g.getMaxMembers());
            resp.put("nonOwnerMemberCount", groupDAO.getNonOwnerMemberCount(id));
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

            // Admins cannot join groups (they have view-only oversight).
            if ("ADMIN".equalsIgnoreCase(userDAO.getUserType(userID))) {
                return ResponseEntity.status(403).body(Map.of("error", "Admins cannot join groups."));
            }

            // If the group is password-protected, require and verify the password.
            if (groupDAO.hasPassword(id)) {
                Object pwRaw = body.get("password");
                String password = pwRaw instanceof String s ? s : null;
                if (password == null || password.isBlank()) {
                    return ResponseEntity.status(401).body(Map.of("error", "This group requires a password to join.", "passwordRequired", true));
                }
                if (!groupDAO.verifyPassword(id, password)) {
                    return ResponseEntity.status(403).body(Map.of("error", "Incorrect group password.", "passwordRequired", true));
                }
            }

            // Enforce the member cap (counts joining members only — excludes the owner).
            Integer maxMembers = groupDAO.getMaxMembers(id);
            if (maxMembers != null && !groupDAO.isMember(id, userID)
                    && groupDAO.getNonOwnerMemberCount(id) >= maxMembers) {
                return ResponseEntity.status(409).body(Map.of("error", "This group is full."));
            }

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

    @PostMapping("/{id}/leave")
    public ResponseEntity<?> leaveGroup(@PathVariable int id, @RequestBody Map<String, Object> body) {
        try {
            if (id <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Cannot leave virtual group"));
            }
            if (body == null || !body.containsKey("userID")) {
                return ResponseEntity.badRequest().body(Map.of("error", "userID required in body"));
            }
            int userID = (int) ((Number) body.get("userID")).intValue();

            Group g = groupDAO.findById(id);
            if (g != null && g.getCreatedBy() == userID) {
                return ResponseEntity.badRequest().body(Map.of("error", "The group owner cannot leave the group. You must delete the group instead."));
            }

            boolean removed = groupDAO.removeMember(id, userID);
            if (removed) {
                return ResponseEntity.ok(Map.of("success", true, "message", "You have left the group."));
            } else {
                return ResponseEntity.badRequest().body(Map.of("error", "You are not a member of this group."));
            }
        } catch (ClassCastException cce) {
            return ResponseEntity.badRequest().body(Map.of("error", "userID must be numeric"));
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

    /**
     * POST /api/groups/{id}/tasks — create a QUIZ task in a group (lecturer only).
     * Body: { title, createdBy, questions: [ { prompt, type: 'TRUE_FALSE'|'MULTIPLE_CHOICE',
     *         options: [ { optionText|text, isCorrect } ] } ] }.
     * Rules: 1-10 questions; each question needs >=2 options and exactly one correct.
     */
    @PostMapping("/{id}/tasks")
    @SuppressWarnings("unchecked")
    public ResponseEntity<?> createGroupTask(@PathVariable int id, @RequestBody Map<String, Object> body) {
        try {
            if (id <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid group id"));
            }
            Object createdByRaw = body.get("createdBy");
            if (!(createdByRaw instanceof Number)) {
                return ResponseEntity.badRequest().body(Map.of("error", "createdBy must be numeric"));
            }
            int createdBy = ((Number) createdByRaw).intValue();

            // Only lecturers can create tasks (enforced on the backend, not just the UI).
            String role = userDAO.getUserType(createdBy);
            if (!"LECTURER".equalsIgnoreCase(role)) {
                return ResponseEntity.status(403).body(Map.of("error", "Only lecturers can create tasks."));
            }
            // Lecturer must be a member of the group to create tasks for it.
            if (!groupDAO.isMember(id, createdBy)) {
                return ResponseEntity.status(403).body(Map.of("error", "You must be a member of this group to create tasks."));
            }

            String title = body.get("title") instanceof String s ? s.trim() : null;
            if (title == null || title.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Quiz title is required"));
            }

            Object questionsRaw = body.get("questions");
            if (!(questionsRaw instanceof List<?> qList) || qList.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "At least one question is required"));
            }
            if (qList.size() > 10) {
                return ResponseEntity.badRequest().body(Map.of("error", "A quiz can have at most 10 questions"));
            }

            List<TaskQuestion> questions = new ArrayList<>();
            for (Object qObj : qList) {
                if (!(qObj instanceof Map<?, ?> qm)) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Malformed question"));
                }
                Object promptRaw = qm.get("prompt");
                String prompt = promptRaw instanceof String s ? s.trim() : null;
                if (prompt == null || prompt.isBlank()) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Each question needs a prompt"));
                }
                String type = qm.get("type") instanceof String s ? s.trim().toUpperCase() : "";
                if (!"TRUE_FALSE".equals(type) && !"MULTIPLE_CHOICE".equals(type)) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Question type must be TRUE_FALSE or MULTIPLE_CHOICE"));
                }

                Object optsRaw = qm.get("options");
                if (!(optsRaw instanceof List<?> optList) || optList.size() < 2) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Each question needs at least two options"));
                }
                if ("MULTIPLE_CHOICE".equals(type) && optList.size() > 4) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Multiple choice questions allow at most 4 options"));
                }

                TaskQuestion tq = new TaskQuestion();
                tq.setPrompt(prompt);
                tq.setType(type);
                int correctCount = 0;
                List<TaskOption> options = new ArrayList<>();
                for (Object oObj : optList) {
                    if (!(oObj instanceof Map<?, ?> om)) {
                        return ResponseEntity.badRequest().body(Map.of("error", "Malformed option"));
                    }
                    Object textRaw = om.containsKey("optionText") ? om.get("optionText") : om.get("text");
                    String text = textRaw instanceof String s ? s.trim() : null;
                    if (text == null || text.isBlank()) {
                        return ResponseEntity.badRequest().body(Map.of("error", "Each option needs text"));
                    }
                    boolean isCorrect = Boolean.TRUE.equals(om.get("isCorrect"));
                    if (isCorrect) correctCount++;
                    TaskOption to = new TaskOption();
                    to.setOptionText(text);
                    to.setCorrect(isCorrect);
                    options.add(to);
                }
                if (correctCount != 1) {
                    return ResponseEntity.badRequest().body(Map.of("error", "Each question must have exactly one correct answer"));
                }
                tq.setOptions(options);
                questions.add(tq);
            }

            int taskID = taskDAO.createQuiz(title, id, createdBy, questions);
            if (taskID <= 0) {
                return ResponseEntity.internalServerError().body(Map.of("error", "Failed to create quiz"));
            }
            return ResponseEntity.status(201).body(Map.of(
                    "taskID", taskID,
                    "message", "Quiz created successfully!"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}

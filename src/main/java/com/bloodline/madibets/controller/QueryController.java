package com.bloodline.madibets.controller;

import com.bloodline.madibets.dao.QueryDAO;
import com.bloodline.madibets.model.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/queries")
@CrossOrigin(origins = "*")
public class QueryController {

    private final QueryDAO queryDAO = new QueryDAO();

    @GetMapping
    public ResponseEntity<?> getAllQueries() {
        try {
            List<Map<String, Object>> queries = queryDAO.findAllWithUser();
            return ResponseEntity.ok(queries);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateQueryStatus(@PathVariable int id, @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if (status == null || status.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Status is required."));
        }
        try {
            boolean updated = queryDAO.updateStatus(id, status);
            if (updated) {
                return ResponseEntity.ok(Map.of("success", true));
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteQuery(@PathVariable int id) {
        try {
            if (id <= 0) {
                return ResponseEntity.badRequest().body(Map.of("error", "Invalid query id."));
            }
            boolean deleted = queryDAO.delete(id);
            if (deleted) {
                return ResponseEntity.ok(Map.of("success", true));
            }
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping
    public ResponseEntity<?> submitQuery(@RequestBody Query query) {
        if (query == null || query.getTitle() == null || query.getTitle().isBlank() ||
            query.getDescription() == null || query.getDescription().isBlank() ||
            query.getUserID() <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Title, description and userID are required."));
        }

        try {
            int newQueryId = queryDAO.create(query);
            if (newQueryId > 0) {
                return ResponseEntity.ok(Map.of("success", true, "queryID", newQueryId));
            }
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to save query."));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", e.getMessage()));
        }
    }
}

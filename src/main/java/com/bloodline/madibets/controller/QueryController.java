package com.bloodline.madibets.controller;

import com.bloodline.madibets.dao.QueryDAO;
import com.bloodline.madibets.model.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/queries")
@CrossOrigin(origins = "*")
public class QueryController {

    private final QueryDAO queryDAO = new QueryDAO();

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

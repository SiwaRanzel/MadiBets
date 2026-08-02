package com.bloodline.madibets.controller;

import com.bloodline.madibets.service.LeaderboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Owner: Jason (C-series).
 * REST endpoints for bet history (C400), rankings (C500), and scheduled updates (C600).
 */
@RestController
@RequestMapping("/api/leaderboard")
@CrossOrigin(origins = "*")
public class LeaderboardController {

    private final LeaderboardService leaderboardService = new LeaderboardService();

    // ------------------------------------------------------------------
    // C400 — GET /api/leaderboard/history/{userID}
    // Returns all bets placed by a user (newest first).
    // ------------------------------------------------------------------
    @GetMapping("/history/{userID}")
    public ResponseEntity<?> getBetHistory(@PathVariable int userID) {
        try {
            List<Map<String, Object>> history = leaderboardService.getBetHistory(userID);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to load bet history: " + e.getMessage()));
        }
    }

    // ------------------------------------------------------------------
    // C500 — GET /api/leaderboard/rankings?limit=50&sortBy=balance
    // Returns top N users ranked by chosen criterion.
    // sortBy options: balance (default), wins, totalBets
    // ------------------------------------------------------------------
    @GetMapping("/rankings")
    public ResponseEntity<?> getRankings(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "balance") String sortBy) {
        try {
            List<Map<String, Object>> rankings = leaderboardService.getRankings(limit, sortBy);
            return ResponseEntity.ok(rankings);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to load rankings: " + e.getMessage()));
        }
    }

    // ------------------------------------------------------------------
    // C500 — GET /api/leaderboard/stats/{userID}
    // Returns a user's rank position and bet statistics.
    // ------------------------------------------------------------------
    @GetMapping("/stats/{userID}")
    public ResponseEntity<?> getUserStats(@PathVariable int userID) {
        try {
            Map<String, Object> stats = leaderboardService.getUserStats(userID);
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to load user stats: " + e.getMessage()));
        }
    }

    // ------------------------------------------------------------------
    // C600 — POST /api/leaderboard/allowance
    // Grants monthly 100 MadiBucks allowance to all users.
    // This would typically be triggered by a scheduled job or admin action.
    // ------------------------------------------------------------------
    @PostMapping("/allowance")
    public ResponseEntity<?> grantMonthlyAllowance() {
        try {
            int updated = leaderboardService.grantMonthlyAllowance();
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Monthly allowance granted to " + updated + " accounts.",
                    "accountsUpdated", updated
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to grant allowance: " + e.getMessage()));
        }
    }
}

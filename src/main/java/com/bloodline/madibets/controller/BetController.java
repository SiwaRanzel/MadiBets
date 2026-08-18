package com.bloodline.madibets.controller;

import com.bloodline.madibets.model.Bet;
import com.bloodline.madibets.model.BetOutcome;
import com.bloodline.madibets.service.AccountService;
import com.bloodline.madibets.service.BetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Owner: Kieran (B-series). REST endpoints for the bet lifecycle, following
 * AuthController's conventions: Map payloads in, {"error": message} on failure.
 * Business rules live in BetService — this layer only translates HTTP.
 *
 * Routes (all under /api/bets):
 *   GET    /active           B700  every ACTIVE bet (+ outcomes, deadline, wager aggregates)
 *   GET    /proposed         B300  the admin review queue (+ unpriced outcomes)
 *   POST   /propose          B200  {userID, eventID?, description, outcomes:[label]} -> 201 {betID}
 *   POST   /{id}/approve     B300  {odds:{outcomeID:odds}, deadline, adminUserID}
 *   POST   /{id}/reject      B300  {adminUserID}
 *   POST   /{id}/wager       B100  {userID, outcomeID, stake}        -> {betID, newBalance}
 *   POST   /{id}/grade       B400+B600  {winningOutcomeID | cancelled:true, adminUserID}
 *   DELETE /{id}?adminUserID=N  B500
 */
@RestController
@RequestMapping("/api/bets")
@CrossOrigin(origins = "*")
public class BetController {

    private final BetService betService = new BetService();
    private final AccountService accountService = new AccountService();

    @GetMapping("/active")
    public ResponseEntity<?> active() {
        try {
            return ResponseEntity.ok(Map.of("bets", toMaps(betService.viewActiveBets())));
        } catch (SQLException e) {
            return dbError(e);
        }
    }

    @GetMapping("/proposed")
    public ResponseEntity<?> proposed() {
        try {
            return ResponseEntity.ok(Map.of("bets", toMaps(betService.viewProposedBets())));
        } catch (SQLException e) {
            return dbError(e);
        }
    }

    @PostMapping("/propose")
    public ResponseEntity<?> propose(@RequestBody Map<String, Object> body) {
        try {
            int betID = betService.proposeBet(
                    requireInt(body, "userID"),
                    optInt(body, "eventID"),
                    requireString(body, "description"),
                    requireStringList(body, "outcomes"));
            return ResponseEntity.status(201).body(Map.of("betID", betID));
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        } catch (SQLException e) {
            return dbError(e);
        }
    }

    @PostMapping("/{id}/approve")
    public ResponseEntity<?> approve(@PathVariable("id") int id, @RequestBody Map<String, Object> body) {
        try {
            betService.approveProposal(id,
                    requireOddsMap(body, "odds"),
                    requireDateTime(body, "deadline"),
                    requireInt(body, "adminUserID"));
            return ResponseEntity.ok(Map.of("betID", id, "status", "ACTIVE"));
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        } catch (SQLException e) {
            return dbError(e);
        }
    }

    @PostMapping("/{id}/reject")
    public ResponseEntity<?> reject(@PathVariable("id") int id, @RequestBody Map<String, Object> body) {
        try {
            betService.rejectProposal(id, requireInt(body, "adminUserID"));
            return ResponseEntity.ok(Map.of("betID", id, "status", "DELETED"));
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        } catch (SQLException e) {
            return dbError(e);
        }
    }

    @PostMapping("/{id}/wager")
    public ResponseEntity<?> wager(@PathVariable("id") int id, @RequestBody Map<String, Object> body) {
        try {
            int userID = requireInt(body, "userID");
            betService.placeBet(id, requireInt(body, "outcomeID"), userID, requireDecimal(body, "stake"));
            // Fresh balance in the response saves the UI a round-trip to /api/users.
            BigDecimal newBalance = accountService.getBalance(userID);
            Map<String, Object> resp = new LinkedHashMap<>();
            resp.put("betID", id);
            resp.put("newBalance", newBalance);
            return ResponseEntity.ok(resp);
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        } catch (SQLException e) {
            return dbError(e);
        }
    }

    @PostMapping("/{id}/grade")
    public ResponseEntity<?> grade(@PathVariable("id") int id, @RequestBody Map<String, Object> body) {
        try {
            Integer winningOutcomeID = optInt(body, "winningOutcomeID");
            boolean cancelled = Boolean.TRUE.equals(body.get("cancelled"));
            betService.gradeBet(id, winningOutcomeID, cancelled, requireInt(body, "adminUserID"));
            return ResponseEntity.ok(Map.of("betID", id, "status", "GRADED",
                    "outcome", cancelled ? "CANCELLED" : "DECIDED"));
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        } catch (SQLException e) {
            return dbError(e);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable("id") int id, @RequestParam("adminUserID") int adminUserID) {
        try {
            betService.deleteBet(id, adminUserID);
            return ResponseEntity.ok(Map.of("betID", id, "status", "DELETED"));
        } catch (IllegalArgumentException e) {
            return badRequest(e);
        } catch (SQLException e) {
            return dbError(e);
        }
    }

    /* ── helpers ─────────────────────────────────────────────── */

    private static ResponseEntity<?> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }

    private static ResponseEntity<?> dbError(SQLException e) {
        e.printStackTrace();   // console only — no SQL detail leaves the server (POPI)
        return ResponseEntity.internalServerError().body(Map.of("error", "Database error. Please try again."));
    }

    /** Jackson deserializes JSON numbers as Integer/Long/Double — normalise here. */
    private static int requireInt(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v instanceof Number n && n.doubleValue() == Math.rint(n.doubleValue())) {
            return n.intValue();
        }
        throw new IllegalArgumentException("Missing or invalid '" + key + "'.");
    }

    private static Integer optInt(Map<String, Object> body, String key) {
        return body.get(key) == null ? null : requireInt(body, key);
    }

    private static BigDecimal requireDecimal(Map<String, Object> body, String key) {
        Object v = body.get(key);
        if (v instanceof Number n) {
            return new BigDecimal(String.valueOf(n));
        }
        throw new IllegalArgumentException("Missing or invalid '" + key + "'.");
    }

    private static String requireString(Map<String, Object> body, String key) {
        if (body.get(key) instanceof String s) return s;
        throw new IllegalArgumentException("Missing or invalid '" + key + "'.");
    }

    /** JSON array of strings (the proposal's outcome labels). */
    private static List<String> requireStringList(Map<String, Object> body, String key) {
        if (body.get(key) instanceof List<?> raw) {
            List<String> out = new ArrayList<>(raw.size());
            for (Object o : raw) {
                if (!(o instanceof String s)) {
                    throw new IllegalArgumentException("Missing or invalid '" + key + "'.");
                }
                out.add(s);
            }
            return out;
        }
        throw new IllegalArgumentException("Missing or invalid '" + key + "'.");
    }

    /** JSON object {outcomeID: odds} — Jackson gives String keys and Number values. */
    private static Map<Integer, BigDecimal> requireOddsMap(Map<String, Object> body, String key) {
        if (body.get(key) instanceof Map<?, ?> raw && !raw.isEmpty()) {
            Map<Integer, BigDecimal> out = new LinkedHashMap<>();
            for (Map.Entry<?, ?> e : raw.entrySet()) {
                try {
                    int outcomeID = Integer.parseInt(String.valueOf(e.getKey()));
                    if (!(e.getValue() instanceof Number n)) {
                        throw new NumberFormatException();
                    }
                    out.put(outcomeID, new BigDecimal(String.valueOf(n)));
                } catch (NumberFormatException ex) {
                    throw new IllegalArgumentException("Missing or invalid '" + key + "'.");
                }
            }
            return out;
        }
        throw new IllegalArgumentException("Missing or invalid '" + key + "'.");
    }

    /** ISO-8601 local datetime, e.g. "2026-08-20T18:00" (what <input type=datetime-local> sends). */
    private static LocalDateTime requireDateTime(Map<String, Object> body, String key) {
        if (body.get(key) instanceof String s && !s.isBlank()) {
            try {
                return LocalDateTime.parse(s);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid '" + key + "' — use e.g. 2026-08-20T18:00.");
            }
        }
        throw new IllegalArgumentException("Missing or invalid '" + key + "'.");
    }

    private static List<Map<String, Object>> toMaps(List<Bet> bets) {
        List<Map<String, Object>> out = new ArrayList<>(bets.size());
        LocalDateTime now = LocalDateTime.now();
        for (Bet b : bets) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("betID", b.getBetID());
            m.put("userID", b.getUserID());
            m.put("eventID", b.getEventID());
            m.put("description", b.getDescription());
            m.put("outcome", b.getOutcome());
            m.put("status", b.getStatus());
            m.put("deadline", b.getDeadline());
            m.put("winningOutcomeID", b.getWinningOutcomeID());
            m.put("proposedDate", b.getProposedDate());
            m.put("gradedDate", b.getGradedDate());
            m.put("gradedBy", b.getGradedBy());
            m.put("wagerCount", b.getWagerCount());
            m.put("totalStaked", b.getTotalStaked());
            List<Map<String, Object>> outcomes = new ArrayList<>();
            if (b.getOutcomes() != null) {
                for (BetOutcome o : b.getOutcomes()) {
                    Map<String, Object> om = new LinkedHashMap<>();
                    om.put("outcomeID", o.getOutcomeID());
                    om.put("label", o.getLabel());
                    om.put("odds", o.getOdds());
                    outcomes.add(om);
                }
            }
            m.put("outcomes", outcomes);
            // open = still taking wagers: ACTIVE and the deadline has not passed
            m.put("open", "ACTIVE".equals(b.getStatus())
                    && (b.getDeadline() == null || b.getDeadline().isAfter(now)));
            out.add(m);
        }
        return out;
    }
}

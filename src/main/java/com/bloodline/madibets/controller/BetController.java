package com.bloodline.madibets.controller;

import com.bloodline.madibets.model.Bet;
import com.bloodline.madibets.service.AccountService;
import com.bloodline.madibets.service.BetService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.sql.SQLException;
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
 *   GET    /active           B700  every ACTIVE bet (+ wagerCount/totalStaked)
 *   GET    /proposed         B300  the admin review queue
 *   POST   /propose          B200  {userID, eventID?, description}   -> 201 {betID}
 *   POST   /{id}/approve     B300  {odds, adminUserID}
 *   POST   /{id}/reject      B300  {adminUserID}
 *   POST   /{id}/wager       B100  {userID, stake}                   -> {betID, newBalance}
 *   POST   /{id}/grade       B400+B600  {outcome, adminUserID}
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
                    requireString(body, "description"));
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
            betService.approveProposal(id, requireDecimal(body, "odds"), requireInt(body, "adminUserID"));
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
            betService.placeBet(id, userID, requireDecimal(body, "stake"));
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
            String outcome = requireString(body, "outcome");
            betService.gradeBet(id, outcome, requireInt(body, "adminUserID"));
            return ResponseEntity.ok(Map.of("betID", id, "status", "GRADED", "outcome", outcome));
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

    private static List<Map<String, Object>> toMaps(List<Bet> bets) {
        List<Map<String, Object>> out = new ArrayList<>(bets.size());
        for (Bet b : bets) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("betID", b.getBetID());
            m.put("userID", b.getUserID());
            m.put("eventID", b.getEventID());
            m.put("description", b.getDescription());
            m.put("odds", b.getOdds());
            m.put("outcome", b.getOutcome());
            m.put("status", b.getStatus());
            m.put("proposedDate", b.getProposedDate());
            m.put("gradedDate", b.getGradedDate());
            m.put("gradedBy", b.getGradedBy());
            m.put("wagerCount", b.getWagerCount());
            m.put("totalStaked", b.getTotalStaked());
            // market/wager split: an ACTIVE bet stays open to every student
            m.put("open", "ACTIVE".equals(b.getStatus()));
            out.add(m);
        }
        return out;
    }
}

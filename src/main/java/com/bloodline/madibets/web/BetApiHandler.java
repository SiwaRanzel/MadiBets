package com.bloodline.madibets.web;

import com.bloodline.madibets.model.Bet;
import com.bloodline.madibets.service.AccountService;
import com.bloodline.madibets.service.BetService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Owner: Kieran (B-series). REST endpoints for the bet lifecycle, written
 * against the JDK's built-in HTTP server (jdk.httpserver) so no dependency is
 * added to the shared pom.xml.
 *
 * NOT YET MOUNTED: the server bootstrap (port 8081, static file serving, the
 * A-series /auth and /users contexts) is a shared/team decision. Once agreed,
 * wiring this up is one line wherever the server is created:
 *
 *     server.createContext("/api/bets", new BetApiHandler());
 *
 * Routes (all under /api/bets):
 *   GET    /active           B700  every ACTIVE bet ("open": true until wagered)
 *   GET    /proposed         B300  the admin review queue
 *   POST   /propose          B200  {userID, eventID?, description}      -> 201 {betID}
 *   POST   /{id}/approve     B300  {odds, adminUserID}                  -> {betID, status}
 *   POST   /{id}/reject      B300  {adminUserID}                        -> {betID, status}
 *   POST   /{id}/wager       B100  {userID, stake}                      -> {betID, newBalance}
 *   POST   /{id}/grade       B400+B600  {outcome, adminUserID}          -> {betID, status, outcome}
 *   DELETE /{id}?adminUserID=N  B500                                    -> {betID, status}
 *
 * Errors follow the convention app.js expects: non-2xx + {"error": "message"}.
 */
public class BetApiHandler implements HttpHandler {

    private static final Pattern ID_ROUTE =
            Pattern.compile("^/(\\d+)(?:/(approve|reject|wager|grade))?$");

    private final BetService betService = new BetService();
    private final AccountService accountService = new AccountService();

    @Override
    public void handle(HttpExchange ex) throws IOException {
        try {
            if ("OPTIONS".equals(ex.getRequestMethod())) {   // CORS preflight
                Http.cors(ex);
                ex.sendResponseHeaders(204, -1);
                ex.close();
                return;
            }
            route(ex);
        } catch (IllegalArgumentException e) {
            Http.send(ex, 400, error(e.getMessage()));
        } catch (SQLException e) {
            e.printStackTrace();   // console only — no SQL detail leaves the server (POPI)
            Http.send(ex, 500, error("Database error. Please try again."));
        } catch (Exception e) {
            e.printStackTrace();
            Http.send(ex, 500, error("Server error. Please try again."));
        }
    }

    private void route(HttpExchange ex) throws IOException, SQLException {
        String method = ex.getRequestMethod();
        String rest = ex.getRequestURI().getPath().replaceFirst("^/api/bets", "");

        switch (rest) {
            case "/active" -> {
                requireMethod(ex, method, "GET");
                Http.send(ex, 200, Map.of("bets", toMaps(betService.viewActiveBets())));
                return;
            }
            case "/proposed" -> {
                requireMethod(ex, method, "GET");
                Http.send(ex, 200, Map.of("bets", toMaps(betService.viewProposedBets())));
                return;
            }
            case "/propose" -> {
                requireMethod(ex, method, "POST");
                Map<String, Object> body = Http.readJsonBody(ex);
                int betID = betService.proposeBet(
                        JsonUtil.requireInt(body, "userID"),
                        JsonUtil.optInt(body, "eventID"),
                        JsonUtil.requireString(body, "description"));
                Http.send(ex, 201, Map.of("betID", betID));
                return;
            }
            default -> { /* falls through to the /{id} routes below */ }
        }

        Matcher m = ID_ROUTE.matcher(rest);
        if (!m.matches()) {
            Http.send(ex, 404, error("No such endpoint."));
            return;
        }
        int betID = Integer.parseInt(m.group(1));
        String action = m.group(2);

        if (action == null) {
            requireMethod(ex, method, "DELETE");
            String admin = Http.queryParams(ex).get("adminUserID");
            if (admin == null || !admin.matches("\\d+")) {
                throw new IllegalArgumentException("Missing 'adminUserID' query parameter.");
            }
            betService.deleteBet(betID, Integer.parseInt(admin));
            Http.send(ex, 200, Map.of("betID", betID, "status", "DELETED"));
            return;
        }

        requireMethod(ex, method, "POST");
        Map<String, Object> body = Http.readJsonBody(ex);
        switch (action) {
            case "approve" -> {
                betService.approveProposal(betID,
                        JsonUtil.requireDecimal(body, "odds"),
                        JsonUtil.requireInt(body, "adminUserID"));
                Http.send(ex, 200, Map.of("betID", betID, "status", "ACTIVE"));
            }
            case "reject" -> {
                betService.rejectProposal(betID, JsonUtil.requireInt(body, "adminUserID"));
                Http.send(ex, 200, Map.of("betID", betID, "status", "DELETED"));
            }
            case "wager" -> {
                int userID = JsonUtil.requireInt(body, "userID");
                betService.placeBet(betID, userID, JsonUtil.requireDecimal(body, "stake"));
                // Return the fresh balance so the UI can update the wallet header
                // without an extra round-trip to the A-series /users endpoint.
                BigDecimal newBalance = accountService.getBalance(userID);
                Http.send(ex, 200, Map.of("betID", betID, "newBalance", newBalance));
            }
            case "grade" -> {
                String outcome = JsonUtil.requireString(body, "outcome");
                betService.gradeBet(betID, outcome, JsonUtil.requireInt(body, "adminUserID"));
                Http.send(ex, 200, Map.of("betID", betID, "status", "GRADED", "outcome", outcome));
            }
            default -> Http.send(ex, 404, error("No such endpoint."));
        }
    }

    private static void requireMethod(HttpExchange ex, String actual, String expected) {
        if (!expected.equals(actual)) {
            throw new IllegalArgumentException(expected + " required for this endpoint.");
        }
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
            m.put("amountToBeWon", b.getAmountToBeWon());
            m.put("outcome", b.getOutcome());
            m.put("status", b.getStatus());
            m.put("proposedDate", b.getProposedDate());
            m.put("placedDate", b.getPlacedDate());
            m.put("gradedDate", b.getGradedDate());
            m.put("gradedBy", b.getGradedBy());
            // update-in-place model: an ACTIVE bet takes exactly one wager
            m.put("open", "ACTIVE".equals(b.getStatus()) && b.getPlacedDate() == null);
            out.add(m);
        }
        return out;
    }

    private static Map<String, Object> error(String message) {
        return Map.of("error", message == null ? "Bad request." : message);
    }
}

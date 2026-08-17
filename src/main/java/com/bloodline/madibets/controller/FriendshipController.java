package com.bloodline.madibets.controller;

import com.bloodline.madibets.model.Friendship;
import com.bloodline.madibets.service.FriendshipService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Owner: Jason (C-series).
 * REST endpoints for friend management (C100, C200, C300).
 */
@RestController
@RequestMapping("/api/friends")
@CrossOrigin(origins = "*")
public class FriendshipController {

    private final FriendshipService friendshipService = new FriendshipService();

    // ------------------------------------------------------------------
    // C100 — GET /api/friends/{userID}
    // Returns all accepted friends for a user.
    // ------------------------------------------------------------------
    @GetMapping("/{userID}")
    public ResponseEntity<?> getFriends(@PathVariable int userID) {
        try {
            List<Friendship> friends = friendshipService.getFriends(userID);
            return ResponseEntity.ok(friends);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to load friends: " + e.getMessage()));
        }
    }

    // ------------------------------------------------------------------
    // C100 — GET /api/friends/{userID}/pending
    // Returns pending friend requests sent TO this user.
    // ------------------------------------------------------------------
    @GetMapping("/{userID}/pending")
    public ResponseEntity<?> getPendingRequests(@PathVariable int userID) {
        try {
            List<Friendship> pending = friendshipService.getPendingRequests(userID);
            return ResponseEntity.ok(pending);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to load pending requests: " + e.getMessage()));
        }
    }

    // ------------------------------------------------------------------
    // C200 — POST /api/friends/request
    // Body: { "requesterID": 1, "addresseID": 2 }
    // Sends a friend request.
    // ------------------------------------------------------------------
    @PostMapping("/request")
    public ResponseEntity<?> sendRequest(@RequestBody Map<String, Integer> body) {
        try {
            Integer requesterID = body.get("requesterID");
            Integer addresseID = body.get("addresseID");

            if (requesterID == null || addresseID == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "requesterID and addresseID are required."));
            }

            int friendshipID = friendshipService.sendFriendRequest(requesterID, addresseID);
            return ResponseEntity.ok(Map.of(
                    "friendshipID", friendshipID,
                    "message", "Friend request sent!"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to send request: " + e.getMessage()));
        }
    }

    // ------------------------------------------------------------------
    // C200 — PUT /api/friends/{friendshipID}/accept
    // Accepts a pending friend request.
    // ------------------------------------------------------------------
    @PutMapping("/{friendshipID}/accept")
    public ResponseEntity<?> acceptRequest(@PathVariable int friendshipID) {
        try {
            boolean success = friendshipService.acceptRequest(friendshipID);
            if (success) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Friend request accepted!"));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Request not found or already handled."));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to accept request: " + e.getMessage()));
        }
    }

    // ------------------------------------------------------------------
    // C200 — PUT /api/friends/{friendshipID}/reject
    // Rejects a pending friend request.
    // ------------------------------------------------------------------
    @PutMapping("/{friendshipID}/reject")
    public ResponseEntity<?> rejectRequest(@PathVariable int friendshipID) {
        try {
            boolean success = friendshipService.rejectRequest(friendshipID);
            if (success) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Friend request rejected."));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Request not found or already handled."));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to reject request: " + e.getMessage()));
        }
    }

    // ------------------------------------------------------------------
    // C300 — DELETE /api/friends/{friendshipID}
    // Removes an existing friendship.
    // ------------------------------------------------------------------
    @DeleteMapping("/{friendshipID}")
    public ResponseEntity<?> removeFriend(@PathVariable int friendshipID) {
        try {
            boolean success = friendshipService.removeFriend(friendshipID);
            if (success) {
                return ResponseEntity.ok(Map.of("success", true, "message", "Friend removed."));
            } else {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Friendship not found."));
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Failed to remove friend: " + e.getMessage()));
        }
    }
}

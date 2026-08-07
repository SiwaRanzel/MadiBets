package com.bloodline.madibets.service;

import java.sql.SQLException;
import java.util.List;

import com.bloodline.madibets.dao.FriendshipDAO;
import com.bloodline.madibets.model.Friendship;

/**
 * Owner: Jason (C-series).
 * Business logic for friend requests and friend list management.
 */
public class FriendshipService {

    private final FriendshipDAO friendshipDAO = new FriendshipDAO();

    // ------------------------------------------------------------------
    // C100 — View Friends
    // ------------------------------------------------------------------
    public List<Friendship> getFriends(int userID) throws SQLException {
        return friendshipDAO.findFriends(userID);
    }

    // ------------------------------------------------------------------
    // C100 — View Pending Requests (incoming)
    // ------------------------------------------------------------------
    public List<Friendship> getPendingRequests(int userID) throws SQLException {
        return friendshipDAO.findPendingRequests(userID);
    }

    // ------------------------------------------------------------------
    // C200 — Send Friend Request
    // Validates: can't friend yourself, can't duplicate an existing request.
    // Returns the new friendshipID, or throws IllegalArgumentException.
    // ------------------------------------------------------------------
    public int sendFriendRequest(int requesterID, int addresseID) throws SQLException {
        if (requesterID == addresseID) {
            throw new IllegalArgumentException("You cannot send a friend request to yourself.");
        }

        Friendship existing = friendshipDAO.findBetweenUsers(requesterID, addresseID);
        if (existing != null) {
            String status = existing.getStatus();
            if ("ACCEPTED".equals(status)) {
                throw new IllegalArgumentException("You are already friends with this user.");
            } else if ("PENDING".equals(status)) {
                throw new IllegalArgumentException("A friend request already exists between you and this user.");
            }
            // If REJECTED, allow re-sending (delete old record first)
            friendshipDAO.remove(existing.getFriendshipID());
        }

        return friendshipDAO.sendRequest(requesterID, addresseID);
    }

    // ------------------------------------------------------------------
    // C200 — Accept Friend Request
    // ------------------------------------------------------------------
    public boolean acceptRequest(int friendshipID) throws SQLException {
        return friendshipDAO.acceptRequest(friendshipID);
    }

    // ------------------------------------------------------------------
    // C200 — Reject Friend Request
    // ------------------------------------------------------------------
    public boolean rejectRequest(int friendshipID) throws SQLException {
        return friendshipDAO.rejectRequest(friendshipID);
    }

    // ------------------------------------------------------------------
    // C300 — Remove Friend
    // ------------------------------------------------------------------
    public boolean removeFriend(int friendshipID) throws SQLException {
        return friendshipDAO.remove(friendshipID);
    }
}

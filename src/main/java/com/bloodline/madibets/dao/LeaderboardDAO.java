package com.bloodline.madibets.dao;

/** Owner: Jason (C-series). Depends on Kieran's Account/Bet schema. */
public class LeaderboardDAO {
    // C400 View Bet History (READ) -> findBetsByUser(int userID)
    // C500 View Leaderboard (READ) -> rankUsers(String criterion)
    // C600 Update Leaderboard      -> scheduled job (see "Time" actor)
}

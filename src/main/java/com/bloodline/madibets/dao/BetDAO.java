package com.bloodline.madibets.dao;

import com.bloodline.madibets.config.DatabaseConnection;
import com.bloodline.madibets.model.Bet;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Owner: Kieran (B-series). Copy the PreparedStatement pattern from UserDAO. */
public class BetDAO {

    /** B700 View Bets (READ). Every bet currently open for wagering. */
    public List<Bet> findActiveBets() throws SQLException {
        String sql = "SELECT betID, userID, eventID, description, odds, amountToBeWon, "
                   + "outcome, status, proposedDate, placedDate, gradedDate, gradedBy "
                   + "FROM Bet WHERE status = ? ORDER BY proposedDate DESC, betID DESC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "ACTIVE");
            try (ResultSet rs = ps.executeQuery()) {
                List<Bet> bets = new ArrayList<>();
                while (rs.next()) {
                    bets.add(map(rs));
                }
                return bets;
            }
        }
    }

    // B100 Place Bet (CREATE)      -> placeWager(...)
    // B200 Propose Bet (CREATE)    -> propose(...)
    // B400 Grade Bet (UPDATE)      -> grade(...)
    // B500 Delete Bet (DELETE)     -> delete(int betID)
    // TODO: implement, starting with the READ.

    private Bet map(ResultSet rs) throws SQLException {
        Bet b = new Bet();
        b.setBetID(rs.getInt("betID"));
        b.setUserID(rs.getInt("userID"));
        // getObject, not getInt: getInt turns a SQL NULL into 0, which would
        // read as "event 0" instead of "no event". Same for gradedBy.
        b.setEventID(rs.getObject("eventID", Integer.class));
        b.setDescription(rs.getString("description"));
        b.setOdds(rs.getBigDecimal("odds"));
        b.setAmountToBeWon(rs.getBigDecimal("amountToBeWon"));
        b.setOutcome(rs.getString("outcome"));
        b.setStatus(rs.getString("status"));
        b.setProposedDate(rs.getObject("proposedDate", LocalDateTime.class));
        b.setPlacedDate(rs.getObject("placedDate", LocalDateTime.class));
        b.setGradedDate(rs.getObject("gradedDate", LocalDateTime.class));
        b.setGradedBy(rs.getObject("gradedBy", Integer.class));
        return b;
    }
}

package com.bloodline.madibets.dao;

import com.bloodline.madibets.config.DatabaseConnection;
import com.bloodline.madibets.model.Bet;
import java.math.BigDecimal;
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

    /** B200 Propose Bet (CREATE). Inserts a PROPOSED bet; returns the generated betID, or -1. */
    public int propose(Bet b) throws SQLException {
        String sql = "INSERT INTO Bet (userID, eventID, description, outcome, status, proposedDate) "
                   + "VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setInt(1, b.getUserID());
            ps.setObject(2, b.getEventID());     // setObject, not setInt: eventID may be null
            ps.setString(3, b.getDescription());
            ps.setString(4, b.getOutcome());
            ps.setString(5, b.getStatus());
            ps.setObject(6, b.getProposedDate());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                return keys.next() ? keys.getInt(1) : -1;
            }
        }
    }

    /** Single-row read used by the B-series services. Returns null if absent. */
    public Bet findById(int betID) throws SQLException {
        String sql = "SELECT betID, userID, eventID, description, odds, amountToBeWon, "
                   + "outcome, status, proposedDate, placedDate, gradedDate, gradedBy "
                   + "FROM Bet WHERE betID = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, betID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? map(rs) : null;
            }
        }
    }

    /**
     * B300 Review Proposal (UPDATE). Assigns odds and opens the bet for wagering.
     * The status guard lives in the WHERE clause so a bet can never be activated
     * twice (or resurrected from GRADED/DELETED); returns false if no row matched.
     */
    public boolean activate(int betID, BigDecimal odds) throws SQLException {
        String sql = "UPDATE Bet SET odds = ?, status = ? WHERE betID = ? AND status = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setBigDecimal(1, odds);
            ps.setString(2, "ACTIVE");
            ps.setInt(3, betID);
            ps.setString(4, "PROPOSED");
            return ps.executeUpdate() == 1;
        }
    }

    // B100 Place Bet (CREATE)      -> placeWager(...)
    // B400 Grade Bet (UPDATE)      -> grade(...)
    // B500 Delete Bet (DELETE)     -> delete(int betID)

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

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

    /** B300 list (READ): proposals waiting for admin review, oldest first. */
    public List<Bet> findProposedBets() throws SQLException {
        String sql = "SELECT betID, userID, eventID, description, odds, amountToBeWon, "
                   + "outcome, status, proposedDate, placedDate, gradedDate, gradedBy "
                   + "FROM Bet WHERE status = ? ORDER BY proposedDate ASC, betID ASC";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "PROPOSED");
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

    /** B300 Review Proposal (UPDATE, reject path). Same WHERE guard as activate(). */
    public boolean reject(int betID) throws SQLException {
        String sql = "UPDATE Bet SET status = ? WHERE betID = ? AND status = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "DELETED");
            ps.setInt(2, betID);
            ps.setString(3, "PROPOSED");
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * B100 Place Bet (UPDATE). Claims the ACTIVE market row for the wagering
     * student — the wager lives on the same row as the market (team decision on
     * design gap #1: update-in-place, one wager per bet). Takes the caller's
     * Connection because the claim must commit together with the Account debit.
     * The "placedDate IS NULL" guard makes the one-wager rule atomic: two
     * simultaneous placements can never both match the row.
     */
    public boolean placeWager(Connection con, int betID, int userID,
                              BigDecimal amountToBeWon, LocalDateTime placedDate) throws SQLException {
        String sql = "UPDATE Bet SET userID = ?, amountToBeWon = ?, placedDate = ? "
                   + "WHERE betID = ? AND status = ? AND placedDate IS NULL";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userID);
            ps.setBigDecimal(2, amountToBeWon);
            ps.setObject(3, placedDate);
            ps.setInt(4, betID);
            ps.setString(5, "ACTIVE");
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * B400 Grade Bet (UPDATE). Records the real-world result and closes the bet.
     * Takes the caller's Connection: grading commits together with the B600
     * payout/refund. Guard: only an ACTIVE bet can be graded.
     */
    public boolean grade(Connection con, int betID, String outcome,
                         int gradedBy, LocalDateTime gradedDate) throws SQLException {
        String sql = "UPDATE Bet SET outcome = ?, status = ?, gradedDate = ?, gradedBy = ? "
                   + "WHERE betID = ? AND status = ?";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, outcome);
            ps.setString(2, "GRADED");
            ps.setObject(3, gradedDate);
            ps.setInt(4, gradedBy);
            ps.setInt(5, betID);
            ps.setString(6, "ACTIVE");
            return ps.executeUpdate() == 1;
        }
    }

    /**
     * B500 Delete Bet — soft delete via status, keeping the row for the audit
     * trail. Takes the caller's Connection: deleting a wagered ACTIVE bet
     * refunds the stake in the same transaction. GRADED bets are settled
     * history and never match the guard.
     */
    public boolean delete(Connection con, int betID) throws SQLException {
        String sql = "UPDATE Bet SET status = ? WHERE betID = ? AND status IN (?, ?)";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, "DELETED");
            ps.setInt(2, betID);
            ps.setString(3, "PROPOSED");
            ps.setString(4, "ACTIVE");
            return ps.executeUpdate() == 1;
        }
    }

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

package com.bloodline.madibets.dao;

import com.bloodline.madibets.config.DatabaseConnection;
import com.bloodline.madibets.model.Bet;
import com.bloodline.madibets.model.Wager;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/** Owner: Kieran (B-series). Copy the PreparedStatement pattern from UserDAO. */
public class BetDAO {

    /** Shared SELECT for the list reads: market columns + wager aggregates. */
    private static final String LIST_SQL =
            "SELECT b.betID, b.userID, b.eventID, b.description, b.odds, "
          + "b.outcome, b.status, b.proposedDate, b.gradedDate, b.gradedBy, "
          + "COUNT(w.wagerID) AS wagerCount, COALESCE(SUM(w.stake), 0) AS totalStaked "
          + "FROM Bet b LEFT JOIN Wager w ON w.betID = b.betID "
          + "WHERE b.status = ? GROUP BY b.betID ";

    /** B700 View Bets (READ). Every bet currently open for wagering. */
    public List<Bet> findActiveBets() throws SQLException {
        String sql = LIST_SQL + "ORDER BY b.proposedDate DESC, b.betID DESC";
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
        String sql = LIST_SQL + "ORDER BY b.proposedDate ASC, b.betID ASC";
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
        String sql = "SELECT betID, userID, eventID, description, odds, "
                   + "outcome, status, proposedDate, gradedDate, gradedBy "
                   + "FROM Bet WHERE betID = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, betID);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? mapMarket(rs) : null;
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
     * B100 Place Bet (CREATE). One Wager row per student per bet — the UNIQUE
     * (betID, userID) constraint turns a double wager by the same student into
     * an SQLIntegrityConstraintViolationException the service reports nicely.
     * Takes the caller's Connection: the insert commits with the Account debit.
     * The subquery guard keeps the insert atomic against a concurrent
     * grade/delete: it only finds a betID that is still ACTIVE.
     */
    public boolean placeWager(Connection con, int betID, int userID, BigDecimal stake,
                              BigDecimal amountToBeWon, LocalDateTime placedDate) throws SQLException {
        String sql = "INSERT INTO Wager (betID, userID, stake, amountToBeWon, placedDate) "
                   + "SELECT b.betID, ?, ?, ?, ? FROM Bet b WHERE b.betID = ? AND b.status = 'ACTIVE'";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userID);
            ps.setBigDecimal(2, stake);
            ps.setBigDecimal(3, amountToBeWon);
            ps.setObject(4, placedDate);
            ps.setInt(5, betID);
            return ps.executeUpdate() == 1;
        }
    }

    /** All wagers riding on one bet — B400/B600 settlement walks this list. */
    public List<Wager> findWagersByBet(Connection con, int betID) throws SQLException {
        String sql = "SELECT wagerID, betID, userID, stake, amountToBeWon, placedDate "
                   + "FROM Wager WHERE betID = ? ORDER BY wagerID";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, betID);
            try (ResultSet rs = ps.executeQuery()) {
                List<Wager> wagers = new ArrayList<>();
                while (rs.next()) {
                    Wager w = new Wager();
                    w.setWagerID(rs.getInt("wagerID"));
                    w.setBetID(rs.getInt("betID"));
                    w.setUserID(rs.getInt("userID"));
                    w.setStake(rs.getBigDecimal("stake"));
                    w.setAmountToBeWon(rs.getBigDecimal("amountToBeWon"));
                    w.setPlacedDate(rs.getObject("placedDate", LocalDateTime.class));
                    wagers.add(w);
                }
                return wagers;
            }
        }
    }

    /**
     * B400 Grade Bet (UPDATE). Records the real-world result and closes the bet.
     * Takes the caller's Connection: grading commits together with the B600
     * payouts. Guard: only an ACTIVE bet can be graded.
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
     * refunds every stake in the same transaction. GRADED bets are settled
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

    /** Row mapper for the list queries (market columns + aggregates). */
    private Bet map(ResultSet rs) throws SQLException {
        Bet b = mapMarket(rs);
        b.setWagerCount(rs.getInt("wagerCount"));
        b.setTotalStaked(rs.getBigDecimal("totalStaked"));
        return b;
    }

    /** Row mapper for the market columns only. */
    private Bet mapMarket(ResultSet rs) throws SQLException {
        Bet b = new Bet();
        b.setBetID(rs.getInt("betID"));
        b.setUserID(rs.getInt("userID"));
        // getObject, not getInt: getInt turns a SQL NULL into 0, which would
        // read as "event 0" instead of "no event". Same for gradedBy.
        b.setEventID(rs.getObject("eventID", Integer.class));
        b.setDescription(rs.getString("description"));
        b.setOdds(rs.getBigDecimal("odds"));
        b.setOutcome(rs.getString("outcome"));
        b.setStatus(rs.getString("status"));
        b.setProposedDate(rs.getObject("proposedDate", LocalDateTime.class));
        b.setGradedDate(rs.getObject("gradedDate", LocalDateTime.class));
        b.setGradedBy(rs.getObject("gradedBy", Integer.class));
        return b;
    }
}

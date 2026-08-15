package com.bloodline.madibets.dao;

import com.bloodline.madibets.config.DatabaseConnection;
import com.bloodline.madibets.model.Bet;
import com.bloodline.madibets.model.BetOutcome;
import com.bloodline.madibets.model.Wager;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Owner: Kieran (B-series). Copy the PreparedStatement pattern from UserDAO. */
public class BetDAO {

    /** Shared SELECT for the list reads: market columns + wager aggregates. */
    private static final String LIST_SQL =
            "SELECT b.betID, b.userID, b.eventID, b.description, "
          + "b.outcome, b.status, b.deadline, b.winningOutcomeID, "
          + "b.proposedDate, b.gradedDate, b.gradedBy, "
          + "COUNT(w.wagerID) AS wagerCount, COALESCE(SUM(w.stake), 0) AS totalStaked "
          + "FROM Bet b LEFT JOIN Wager w ON w.betID = b.betID "
          + "WHERE b.status = ? GROUP BY b.betID ";

    /** B700 View Bets (READ). Every bet currently open for wagering, outcomes attached. */
    public List<Bet> findActiveBets() throws SQLException {
        return findByStatus("ACTIVE", "ORDER BY b.proposedDate DESC, b.betID DESC");
    }

    /** B300 list (READ): proposals waiting for admin review, oldest first, outcomes attached. */
    public List<Bet> findProposedBets() throws SQLException {
        return findByStatus("PROPOSED", "ORDER BY b.proposedDate ASC, b.betID ASC");
    }

    private List<Bet> findByStatus(String status, String orderBy) throws SQLException {
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(LIST_SQL + orderBy)) {
            ps.setString(1, status);
            try (ResultSet rs = ps.executeQuery()) {
                List<Bet> bets = new ArrayList<>();
                while (rs.next()) {
                    bets.add(map(rs));
                }
                attachOutcomes(con, bets);
                return bets;
            }
        }
    }

    /** One round-trip for all outcome rows of the listed bets (avoids N+1). */
    private void attachOutcomes(Connection con, List<Bet> bets) throws SQLException {
        if (bets.isEmpty()) return;
        Map<Integer, Bet> byId = new HashMap<>();
        StringBuilder in = new StringBuilder();
        for (Bet b : bets) {
            byId.put(b.getBetID(), b);
            b.setOutcomes(new ArrayList<>());
            in.append(in.length() == 0 ? "?" : ",?");
        }
        String sql = "SELECT outcomeID, betID, label, odds, position FROM BetOutcome "
                   + "WHERE betID IN (" + in + ") ORDER BY betID, position, outcomeID";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            int i = 1;
            for (Bet b : bets) {
                ps.setInt(i++, b.getBetID());
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    byId.get(rs.getInt("betID")).getOutcomes().add(mapOutcome(rs));
                }
            }
        }
    }

    /**
     * B200 Propose Bet (CREATE). Inserts a PROPOSED bet plus its 2-4 outcome
     * labels in one transaction; returns the generated betID, or -1.
     * Odds stay NULL — the admin prices the outcomes at approval (B300).
     */
    public int propose(Bet b, List<String> outcomeLabels) throws SQLException {
        String betSql = "INSERT INTO Bet (userID, eventID, description, outcome, status, proposedDate) "
                      + "VALUES (?, ?, ?, ?, ?, ?)";
        String outcomeSql = "INSERT INTO BetOutcome (betID, label, position) VALUES (?, ?, ?)";
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                int betID;
                try (PreparedStatement ps = con.prepareStatement(betSql, Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, b.getUserID());
                    ps.setObject(2, b.getEventID());     // setObject, not setInt: eventID may be null
                    ps.setString(3, b.getDescription());
                    ps.setString(4, b.getOutcome());
                    ps.setString(5, b.getStatus());
                    ps.setObject(6, b.getProposedDate());
                    ps.executeUpdate();
                    try (ResultSet keys = ps.getGeneratedKeys()) {
                        betID = keys.next() ? keys.getInt(1) : -1;
                    }
                }
                if (betID != -1) {
                    try (PreparedStatement ps = con.prepareStatement(outcomeSql)) {
                        for (int i = 0; i < outcomeLabels.size(); i++) {
                            ps.setInt(1, betID);
                            ps.setString(2, outcomeLabels.get(i));
                            ps.setInt(3, i);
                            ps.addBatch();
                        }
                        ps.executeBatch();
                    }
                }
                con.commit();
                return betID;
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    /** Single-row read used by the B-series services (outcomes attached). Returns null if absent. */
    public Bet findById(int betID) throws SQLException {
        String sql = "SELECT betID, userID, eventID, description, outcome, status, "
                   + "deadline, winningOutcomeID, proposedDate, gradedDate, gradedBy "
                   + "FROM Bet WHERE betID = ?";
        try (Connection con = DatabaseConnection.getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, betID);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Bet b = mapMarket(rs);
                attachOutcomes(con, List.of(b));
                return b;
            }
        }
    }

    /**
     * B300 Review Proposal (UPDATE). Prices every outcome and opens the bet for
     * wagering, all in one transaction. The status guard on the Bet UPDATE
     * keeps a bet from being activated twice; the betID guard on the outcome
     * UPDATE stops odds being written onto another bet's outcome.
     * Returns false (and rolls back) if the bet was not PROPOSED or any
     * outcomeID did not belong to it.
     */
    public boolean activate(int betID, LocalDateTime deadline,
                            Map<Integer, BigDecimal> oddsByOutcomeID) throws SQLException {
        String oddsSql = "UPDATE BetOutcome SET odds = ? WHERE outcomeID = ? AND betID = ?";
        String betSql  = "UPDATE Bet SET status = 'ACTIVE', deadline = ? "
                       + "WHERE betID = ? AND status = 'PROPOSED'";
        try (Connection con = DatabaseConnection.getConnection()) {
            con.setAutoCommit(false);
            try {
                try (PreparedStatement ps = con.prepareStatement(oddsSql)) {
                    for (Map.Entry<Integer, BigDecimal> e : oddsByOutcomeID.entrySet()) {
                        ps.setBigDecimal(1, e.getValue());
                        ps.setInt(2, e.getKey());
                        ps.setInt(3, betID);
                        if (ps.executeUpdate() != 1) {
                            con.rollback();
                            return false;   // outcomeID not on this bet
                        }
                    }
                }
                try (PreparedStatement ps = con.prepareStatement(betSql)) {
                    ps.setObject(1, deadline);
                    ps.setInt(2, betID);
                    if (ps.executeUpdate() != 1) {
                        con.rollback();
                        return false;       // not PROPOSED any more
                    }
                }
                con.commit();
                return true;
            } catch (SQLException | RuntimeException e) {
                con.rollback();
                throw e;
            } finally {
                con.setAutoCommit(true);
            }
        }
    }

    /** B300 Review Proposal (UPDATE, reject path). Guarded like activate(). */
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
     * The SELECT guard keeps the insert atomic against races: it only finds a
     * row while the bet is ACTIVE, the outcome belongs to the bet and is
     * priced, and the deadline (if any) has not passed.
     */
    public boolean placeWager(Connection con, int betID, int outcomeID, int userID,
                              BigDecimal stake, BigDecimal amountToBeWon,
                              LocalDateTime placedDate) throws SQLException {
        String sql = "INSERT INTO Wager (betID, outcomeID, userID, stake, amountToBeWon, placedDate) "
                   + "SELECT b.betID, o.outcomeID, ?, ?, ?, ? "
                   + "FROM Bet b JOIN BetOutcome o ON o.betID = b.betID "
                   + "WHERE b.betID = ? AND o.outcomeID = ? AND b.status = 'ACTIVE' "
                   + "AND o.odds IS NOT NULL AND (b.deadline IS NULL OR b.deadline > NOW())";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, userID);
            ps.setBigDecimal(2, stake);
            ps.setBigDecimal(3, amountToBeWon);
            ps.setObject(4, placedDate);
            ps.setInt(5, betID);
            ps.setInt(6, outcomeID);
            return ps.executeUpdate() == 1;
        }
    }

    /** All wagers riding on one bet — B400/B600 settlement walks this list. */
    public List<Wager> findWagersByBet(Connection con, int betID) throws SQLException {
        String sql = "SELECT wagerID, betID, outcomeID, userID, stake, amountToBeWon, placedDate "
                   + "FROM Wager WHERE betID = ? ORDER BY wagerID";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setInt(1, betID);
            try (ResultSet rs = ps.executeQuery()) {
                List<Wager> wagers = new ArrayList<>();
                while (rs.next()) {
                    Wager w = new Wager();
                    w.setWagerID(rs.getInt("wagerID"));
                    w.setBetID(rs.getInt("betID"));
                    w.setOutcomeID(rs.getInt("outcomeID"));
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
     * B400 Grade Bet (UPDATE). Records which outcome won (winningOutcomeID,
     * outcome DECIDED) or that the market was cancelled (winningOutcomeID null,
     * outcome CANCELLED) and closes the bet. Takes the caller's Connection:
     * grading commits together with the B600 payouts. Guards: only an ACTIVE
     * bet, and a non-null winner must be one of this bet's outcomes.
     */
    public boolean grade(Connection con, int betID, Integer winningOutcomeID,
                         int gradedBy, LocalDateTime gradedDate) throws SQLException {
        String sql = "UPDATE Bet SET outcome = ?, status = 'GRADED', winningOutcomeID = ?, "
                   + "gradedDate = ?, gradedBy = ? "
                   + "WHERE betID = ? AND status = 'ACTIVE' "
                   + "AND (? IS NULL OR EXISTS (SELECT 1 FROM BetOutcome o "
                   + "WHERE o.outcomeID = ? AND o.betID = Bet.betID))";
        try (PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, winningOutcomeID == null ? "CANCELLED" : "DECIDED");
            ps.setObject(2, winningOutcomeID);
            ps.setObject(3, gradedDate);
            ps.setInt(4, gradedBy);
            ps.setInt(5, betID);
            ps.setObject(6, winningOutcomeID);
            ps.setObject(7, winningOutcomeID);
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
        // read as "event 0" instead of "no event". Same for the other nullables.
        b.setEventID(rs.getObject("eventID", Integer.class));
        b.setDescription(rs.getString("description"));
        b.setOutcome(rs.getString("outcome"));
        b.setStatus(rs.getString("status"));
        b.setDeadline(rs.getObject("deadline", LocalDateTime.class));
        b.setWinningOutcomeID(rs.getObject("winningOutcomeID", Integer.class));
        b.setProposedDate(rs.getObject("proposedDate", LocalDateTime.class));
        b.setGradedDate(rs.getObject("gradedDate", LocalDateTime.class));
        b.setGradedBy(rs.getObject("gradedBy", Integer.class));
        return b;
    }

    private BetOutcome mapOutcome(ResultSet rs) throws SQLException {
        BetOutcome o = new BetOutcome();
        o.setOutcomeID(rs.getInt("outcomeID"));
        o.setBetID(rs.getInt("betID"));
        o.setLabel(rs.getString("label"));
        o.setOdds(rs.getBigDecimal("odds"));
        o.setPosition(rs.getInt("position"));
        return o;
    }
}

package com.bloodline.madibets.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Mirrors one row of the Bet table — the MARKET only. Owner: Kieran (B-series).
 * Student stakes live in Wager; the 2-4 possible outcomes (each with its own
 * odds) live in BetOutcome. outcome says HOW the market ended (PENDING /
 * DECIDED / CANCELLED); WHICH outcome won is winningOutcomeID.
 */
public class Bet {
    private int betID;
    private int userID;              // proposer
    private Integer eventID;         // nullable FK
    private String description;
    private String outcome;          // PENDING | DECIDED | CANCELLED
    private String status;           // PROPOSED | ACTIVE | GRADED | DELETED
    private LocalDateTime deadline;  // admin-set at approval; wagering closes when it passes
    private Integer winningOutcomeID;
    private LocalDateTime proposedDate;
    private LocalDateTime gradedDate;
    private Integer gradedBy;        // nullable FK — admin userID

    // Filled by the DAO for list/detail views (not columns on Bet itself)
    private List<BetOutcome> outcomes;
    private int wagerCount;
    private BigDecimal totalStaked;

    public Bet() {}

    public int getBetID()                        { return betID; }
    public void setBetID(int v)                  { this.betID = v; }
    public int getUserID()                       { return userID; }
    public void setUserID(int v)                 { this.userID = v; }
    public Integer getEventID()                  { return eventID; }
    public void setEventID(Integer v)            { this.eventID = v; }
    public String getDescription()               { return description; }
    public void setDescription(String v)         { this.description = v; }
    public String getOutcome()                   { return outcome; }
    public void setOutcome(String v)             { this.outcome = v; }
    public String getStatus()                    { return status; }
    public void setStatus(String v)              { this.status = v; }
    public LocalDateTime getDeadline()           { return deadline; }
    public void setDeadline(LocalDateTime v)     { this.deadline = v; }
    public Integer getWinningOutcomeID()         { return winningOutcomeID; }
    public void setWinningOutcomeID(Integer v)   { this.winningOutcomeID = v; }
    public LocalDateTime getProposedDate()       { return proposedDate; }
    public void setProposedDate(LocalDateTime v) { this.proposedDate = v; }
    public LocalDateTime getGradedDate()         { return gradedDate; }
    public void setGradedDate(LocalDateTime v)   { this.gradedDate = v; }
    public Integer getGradedBy()                 { return gradedBy; }
    public void setGradedBy(Integer v)           { this.gradedBy = v; }
    public List<BetOutcome> getOutcomes()        { return outcomes; }
    public void setOutcomes(List<BetOutcome> v)  { this.outcomes = v; }
    public int getWagerCount()                   { return wagerCount; }
    public void setWagerCount(int v)             { this.wagerCount = v; }
    public BigDecimal getTotalStaked()           { return totalStaked; }
    public void setTotalStaked(BigDecimal v)     { this.totalStaked = v; }
}

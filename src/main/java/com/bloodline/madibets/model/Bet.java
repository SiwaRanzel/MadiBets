package com.bloodline.madibets.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Mirrors one row of the Bet table — the MARKET only. Owner: Kieran (B-series).
 * Student stakes live in Wager (design note #1 resolved: many wagers per bet).
 */
public class Bet {
    private int betID;
    private int userID;              // proposer
    private Integer eventID;         // nullable FK
    private String description;
    private BigDecimal odds;
    private String outcome;          // PENDING | YES | NO | CANCELLED
    private String status;           // PROPOSED | ACTIVE | GRADED | DELETED
    private LocalDateTime proposedDate;
    private LocalDateTime gradedDate;
    private Integer gradedBy;        // nullable FK — admin userID

    // Aggregates filled by the list queries (not columns on Bet itself)
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
    public BigDecimal getOdds()                  { return odds; }
    public void setOdds(BigDecimal v)            { this.odds = v; }
    public String getOutcome()                   { return outcome; }
    public void setOutcome(String v)             { this.outcome = v; }
    public String getStatus()                    { return status; }
    public void setStatus(String v)              { this.status = v; }
    public LocalDateTime getProposedDate()       { return proposedDate; }
    public void setProposedDate(LocalDateTime v) { this.proposedDate = v; }
    public LocalDateTime getGradedDate()         { return gradedDate; }
    public void setGradedDate(LocalDateTime v)   { this.gradedDate = v; }
    public Integer getGradedBy()                 { return gradedBy; }
    public void setGradedBy(Integer v)           { this.gradedBy = v; }
    public int getWagerCount()                   { return wagerCount; }
    public void setWagerCount(int v)             { this.wagerCount = v; }
    public BigDecimal getTotalStaked()           { return totalStaked; }
    public void setTotalStaked(BigDecimal v)     { this.totalStaked = v; }
}

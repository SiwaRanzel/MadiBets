package com.bloodline.madibets.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Mirrors one row of the Bet table. Owner: Kieran (B-series). */
public class Bet {
    private int betID;
    private int userID;              // proposer/placer (see schema DESIGN NOTES #1)
    private Integer eventID;         // nullable FK
    private String description;
    private BigDecimal odds;
    private BigDecimal amountToBeWon;
    private String outcome;          // PENDING | YES | NO | CANCELLED
    private String status;           // PROPOSED | ACTIVE | GRADED | DELETED
    private LocalDateTime proposedDate;
    private LocalDateTime placedDate;
    private LocalDateTime gradedDate;
    private Integer gradedBy;        // nullable FK — admin userID

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
    public BigDecimal getAmountToBeWon()         { return amountToBeWon; }
    public void setAmountToBeWon(BigDecimal v)   { this.amountToBeWon = v; }
    public String getOutcome()                   { return outcome; }
    public void setOutcome(String v)             { this.outcome = v; }
    public String getStatus()                    { return status; }
    public void setStatus(String v)              { this.status = v; }
    public LocalDateTime getProposedDate()       { return proposedDate; }
    public void setProposedDate(LocalDateTime v) { this.proposedDate = v; }
    public LocalDateTime getPlacedDate()         { return placedDate; }
    public void setPlacedDate(LocalDateTime v)   { this.placedDate = v; }
    public LocalDateTime getGradedDate()         { return gradedDate; }
    public void setGradedDate(LocalDateTime v)   { this.gradedDate = v; }
    public Integer getGradedBy()                 { return gradedBy; }
    public void setGradedBy(Integer v)           { this.gradedBy = v; }
}

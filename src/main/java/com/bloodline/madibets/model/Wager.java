package com.bloodline.madibets.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Mirrors one row of the Wager table: one student's stake on one bet. Owner: Kieran (B-series). */
public class Wager {
    private int wagerID;
    private int betID;
    private int outcomeID;               // the outcome this student backed
    private int userID;
    private BigDecimal stake;
    private BigDecimal amountToBeWon;    // stake x chosen outcome's odds, frozen at placement
    private LocalDateTime placedDate;

    public Wager() {}

    public int getWagerID()                      { return wagerID; }
    public void setWagerID(int v)                { this.wagerID = v; }
    public int getBetID()                        { return betID; }
    public void setBetID(int v)                  { this.betID = v; }
    public int getOutcomeID()                    { return outcomeID; }
    public void setOutcomeID(int v)              { this.outcomeID = v; }
    public int getUserID()                       { return userID; }
    public void setUserID(int v)                 { this.userID = v; }
    public BigDecimal getStake()                 { return stake; }
    public void setStake(BigDecimal v)           { this.stake = v; }
    public BigDecimal getAmountToBeWon()         { return amountToBeWon; }
    public void setAmountToBeWon(BigDecimal v)   { this.amountToBeWon = v; }
    public LocalDateTime getPlacedDate()         { return placedDate; }
    public void setPlacedDate(LocalDateTime v)   { this.placedDate = v; }
}

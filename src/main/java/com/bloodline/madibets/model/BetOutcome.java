package com.bloodline.madibets.model;

import java.math.BigDecimal;

/**
 * One possible outcome of a bet (e.g. "Madibaz win" / "Draw"). Owner: Kieran
 * (B-series). The proposer names 2-4 of these; the admin prices them at
 * approval, so odds is null while the bet is PROPOSED.
 */
public class BetOutcome {
    private int outcomeID;
    private int betID;
    private String label;
    private BigDecimal odds;     // null until priced (B300)
    private int position;        // display order

    public BetOutcome() {}

    public int getOutcomeID()            { return outcomeID; }
    public void setOutcomeID(int v)      { this.outcomeID = v; }
    public int getBetID()                { return betID; }
    public void setBetID(int v)          { this.betID = v; }
    public String getLabel()             { return label; }
    public void setLabel(String v)       { this.label = v; }
    public BigDecimal getOdds()          { return odds; }
    public void setOdds(BigDecimal v)    { this.odds = v; }
    public int getPosition()             { return position; }
    public void setPosition(int v)       { this.position = v; }
}

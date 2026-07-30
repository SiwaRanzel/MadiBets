package com.bloodline.madibets.model;

import java.math.BigDecimal;

/** Mirrors one row of the Account table. Owner: Kieran (B-series). */
public class Account {
    private int accountID;
    private BigDecimal balance;
    private int userID;

    public Account() {}

    public int getAccountID()              { return accountID; }
    public void setAccountID(int v)        { this.accountID = v; }
    public BigDecimal getBalance()         { return balance; }
    public void setBalance(BigDecimal v)   { this.balance = v; }
    public int getUserID()                 { return userID; }
    public void setUserID(int v)           { this.userID = v; }
}

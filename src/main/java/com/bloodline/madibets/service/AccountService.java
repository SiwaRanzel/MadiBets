package com.bloodline.madibets.service;

import com.bloodline.madibets.dao.AccountDAO;
import com.bloodline.madibets.model.Account;
import java.math.BigDecimal;
import java.sql.SQLException;

/**
 * Owner: Kieran (B-series). Agree this API in week 1 — Jason's leaderboard and
 * Siwapiwe's register both call into it.
 */
public class AccountService {

    private final AccountDAO accountDAO = new AccountDAO();

    /** Used by the register confirmation, the header balance, and the leaderboard.
     *  Returns null if the user has no Account row yet. */
    public BigDecimal getBalance(int userID) throws SQLException {
        Account a = accountDAO.findByUserId(userID);
        return a == null ? null : a.getBalance();
    }

    // grantMonthlyAllowance()  -> 100 MadiBucks per user (business rule) — later B work
    // settleBet(int betID)     -> B400/B600: pay winner, log Transaction
}

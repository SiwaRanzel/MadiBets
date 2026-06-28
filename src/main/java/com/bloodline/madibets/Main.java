package com.bloodline.madibets;

import com.bloodline.madibets.config.DatabaseConnection;
import java.sql.Connection;

/**
 * Day-one smoke test. Run this first (VS Code: "Run" above main, or `mvn exec:java`).
 * If it prints "Success", every member's DAO work can begin.
 */
public class Main {
    public static void main(String[] args) {
        System.out.println("MadiBets — verifying database connection...");
        try (Connection con = DatabaseConnection.getConnection()) {
            System.out.println("Connected to : " + con.getMetaData().getURL());
            System.out.println("MySQL version: " + con.getMetaData().getDatabaseProductVersion());
            System.out.println("Success. You're ready to build.");
        } catch (Exception e) {
            System.err.println("Connection failed: " + e.getMessage());
            System.err.println("Checklist: MySQL running? db.properties created? schema loaded?");
        }
    }
}

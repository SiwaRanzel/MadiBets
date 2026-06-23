package com.bloodline.madibets.config;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Shared connection helper. Reads credentials from db.properties on the classpath
 * so passwords never get committed to Git. Build this together in week 1.
 */
public final class DatabaseConnection {

    private static final Properties PROPS = new Properties();

    static {
        try (InputStream in = DatabaseConnection.class.getClassLoader()
                .getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new IllegalStateException(
                    "db.properties not found. Copy db.properties.example to "
                  + "src/main/resources/db.properties and add your local MySQL password.");
            }
            PROPS.load(in);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    private DatabaseConnection() {}

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(
            PROPS.getProperty("db.url"),
            PROPS.getProperty("db.user"),
            PROPS.getProperty("db.password"));
    }
}

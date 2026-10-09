package com.library;

import com.library.exception.DatabaseException;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DBConnection {

    private static final String CONFIG_FILE = "db.properties";
    private static Properties props;

    static {
        loadProperties();
    }

    private static void loadProperties() {
        props = new Properties();
        try (InputStream input = DBConnection.class.getClassLoader()
                .getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                throw new DatabaseException(
                        "Configuration file " + CONFIG_FILE + " was not found on the classpath. " +
                                "Please ensure it is located in src/main/resources/.");
            }
            props.load(input);
        } catch (IOException e) {
            throw new DatabaseException("Failed to read " + CONFIG_FILE + ": " + e.getMessage(), e);
        }
    }

    public static Connection getConnection() throws SQLException {
        if (props == null) {
            loadProperties();
        }
        String url = props.getProperty("db.url");
        String user = props.getProperty("db.username");
        String password = props.getProperty("db.password");

        try {
            return DriverManager.getConnection(url, user, password);
        } catch (SQLException e) {
            throw DatabaseException.fromSQLException("Failed to connect to database", e);
        }
    }

    /**
     * Checks if the database is reachable and accepting connections.
     * Returns true if connection succeeds, false otherwise.
     */
    public static boolean checkConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (Exception e) {
            return false;
        }
    }
}
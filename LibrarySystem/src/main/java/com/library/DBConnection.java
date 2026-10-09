package com.library;

import com.library.exception.DatabaseException;

import java.io.File;
import java.io.FileInputStream;
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

        // 1. Check for external db.properties in working directory or app root
        File externalFile = new File(CONFIG_FILE);
        if (externalFile.exists() && externalFile.isFile()) {
            try (InputStream input = new FileInputStream(externalFile)) {
                props.load(input);
                return;
            } catch (IOException ignored) {
                // fallback to classpath
            }
        }

        // 2. Fallback to classpath resource (src/main/resources/db.properties)
        try (InputStream input = DBConnection.class.getClassLoader()
                .getResourceAsStream(CONFIG_FILE)) {
            if (input == null) {
                throw new DatabaseException(
                        "Configuration file " + CONFIG_FILE + " was not found. " +
                                "Please place a valid db.properties file in the application directory.");
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
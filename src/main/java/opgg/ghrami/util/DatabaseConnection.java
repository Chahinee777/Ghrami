package opgg.ghrami.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {
    private static DatabaseConnection instance;
    private String url;
    private String username;
    private String password;

    private DatabaseConnection() {
        loadConfiguration();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            // Test that we can connect
            try (Connection testConn = DriverManager.getConnection(url, username, password)) {
                if (testConn.isValid(2)) {
                    System.out.println("Database connection established successfully!");
                } else {
                    System.err.println("Warning: Database connection may not be valid!");
                }
            }
        } catch (ClassNotFoundException e) {
            System.err.println("MySQL JDBC Driver not found!");
            e.printStackTrace();
        } catch (SQLException e) {
            System.err.println("Failed to connect to database!");
            e.printStackTrace();
        }
    }

    private void loadConfiguration() {
        Properties properties = new Properties();
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("db.properties")) {
            if (input == null) {
                System.err.println("db.properties file not found! Using default values.");
                useDefaultValues();
                return;
            }
            properties.load(input);
            String baseUrl = properties.getProperty("db.url");
            
            // Add connection parameters if not already present
            if (baseUrl != null && !baseUrl.contains("autoReconnect")) {
                if (baseUrl.contains("?")) {
                    baseUrl += "&autoReconnect=true&maxReconnects=3&initialTimeout=2";
                } else {
                    baseUrl += "?autoReconnect=true&maxReconnects=3&initialTimeout=2";
                }
            }
            
            this.url = baseUrl;
            this.username = properties.getProperty("db.username");
            this.password = properties.getProperty("db.password");
        } catch (IOException e) {
            System.err.println("Error loading db.properties! Using default values.");
            useDefaultValues();
        }
    }

    private void useDefaultValues() {
        this.url = "jdbc:mysql://localhost:3306/ghrami_db?useSSL=false&serverTimezone=UTC&autoReconnect=true&maxReconnects=3&initialTimeout=2";
        this.username = "root";
        this.password = "";
    }

    public static DatabaseConnection getInstance() {
        if (instance == null) {
            synchronized (DatabaseConnection.class) {
                if (instance == null) {
                    instance = new DatabaseConnection();
                }
            }
        }
        return instance;
    }

    /**
     * Creates and returns a NEW database connection for each call.
     * This connection should be closed by the caller using try-with-resources.
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    public void closeConnection() {
        // No-op since we don't hold a single connection anymore
        System.out.println("Connection closing is now handled by try-with-resources in controllers.");
    }
}

package opgg.ghrami.util;

import java.sql.*;

public class DatabaseConnection {
    private static DatabaseConnection instance;
    private Connection conn;

    private final String URL = "jdbc:mysql://localhost:3306/ghrami_db?useSSL=false&serverTimezone=UTC";
    private final String USER = "root";
    private final String PASS = "";

    private DatabaseConnection() {
        try {
            conn = DriverManager.getConnection(URL, USER, PASS);
            System.out.println("connected");
        } catch (SQLException e) {
            System.err.println(" error: " + e.getMessage());}}
    public static DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();}
        return instance;}
    public Connection getConnection() {
        return conn;}
}

package opgg.ghrami.controller;

import opgg.ghrami.model.ProgressLog;
import opgg.ghrami.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class ProgressLogController {

    public ProgressLog create(ProgressLog log) {
        String sql = "INSERT INTO progress_log (hobby_id, hours_spent, notes, log_date) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setLong(1, log.getHobbyId());
            stmt.setDouble(2, log.getHoursSpent());
            stmt.setString(3, log.getNotes());
            stmt.setDate(4, Date.valueOf(log.getLogDate() != null ? log.getLogDate() : LocalDate.now()));

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        log.setLogId(keys.getLong(1));
                    }
                }
                return log;
            }
        } catch (SQLException e) {
            System.err.println("Error creating progress log: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public List<ProgressLog> findAllByHobbyId(Long hobbyId) {
        List<ProgressLog> logs = new ArrayList<>();
        String sql = "SELECT * FROM progress_log WHERE hobby_id = ? ORDER BY log_date DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setLong(1, hobbyId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                logs.add(mapResultSet(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching progress logs: " + e.getMessage());
            e.printStackTrace();
        }
        return logs;
    }

    public boolean delete(Long logId) {
        String sql = "DELETE FROM progress_log WHERE log_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, logId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting progress log: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    private ProgressLog mapResultSet(ResultSet rs) throws SQLException {
        ProgressLog log = new ProgressLog();
        log.setLogId(rs.getLong("log_id"));
        log.setHobbyId(rs.getLong("hobby_id"));
        log.setHoursSpent(rs.getDouble("hours_spent"));
        log.setNotes(rs.getString("notes"));
        Date d = rs.getDate("log_date");
        log.setLogDate(d != null ? d.toLocalDate() : LocalDate.now());
        return log;
    }
}

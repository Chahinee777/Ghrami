package opgg.ghrami.controller;

import opgg.ghrami.model.Progress;
import opgg.ghrami.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ProgressController {
    private Connection connection;

    public ProgressController() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    public Progress create(Progress progress) {
        String sql = "INSERT INTO progress (hobby_id, hours_spent, notes) VALUES (?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, progress.getHobbyId());
            stmt.setDouble(2, progress.getHoursSpent());
            stmt.setString(3, progress.getNotes());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        progress.setProgressId(generatedKeys.getLong(1));
                        System.out.println("✅ Progress created (ID: " + progress.getProgressId() + ")");
                        return progress;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Error creating progress: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public Optional<Progress> findById(Long progressId) {
        String sql = "SELECT * FROM progress WHERE progress_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, progressId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToProgress(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Error finding progress by ID: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public Optional<Progress> findByHobbyId(Long hobbyId) {
        String sql = "SELECT * FROM progress WHERE hobby_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, hobbyId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToProgress(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Error finding progress by hobby ID: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public List<Progress> findAll() {
        List<Progress> progressList = new ArrayList<>();
        String sql = "SELECT * FROM progress";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                progressList.add(mapResultSetToProgress(rs));
            }
        } catch (SQLException e) {
            System.err.println("❌ Error finding all progress: " + e.getMessage());
            e.printStackTrace();
        }
        return progressList;
    }

    public Progress update(Progress progress) {
        String sql = "UPDATE progress SET hours_spent = ?, notes = ? WHERE progress_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setDouble(1, progress.getHoursSpent());
            stmt.setString(2, progress.getNotes());
            stmt.setLong(3, progress.getProgressId());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("✅ Progress updated");
                return progress;
            }
        } catch (SQLException e) {
            System.err.println("❌ Error updating progress: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public Progress addHours(Long hobbyId, Double additionalHours, String notes) {
        Optional<Progress> existingProgress = findByHobbyId(hobbyId);

        if (existingProgress.isPresent()) {
            Progress progress = existingProgress.get();
            progress.setHoursSpent(progress.getHoursSpent() + additionalHours);
            progress.setNotes(notes);
            return update(progress);
        } else {
            Progress newProgress = new Progress(hobbyId, additionalHours, notes);
            return create(newProgress);
        }
    }

    public boolean delete(Long progressId) {
        String sql = "DELETE FROM progress WHERE progress_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, progressId);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("✅ Progress deleted: " + progressId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Error deleting progress: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    private Progress mapResultSetToProgress(ResultSet rs) throws SQLException {
        Progress progress = new Progress();
        progress.setProgressId(rs.getLong("progress_id"));
        progress.setHobbyId(rs.getLong("hobby_id"));
        progress.setHoursSpent(rs.getDouble("hours_spent"));
        progress.setNotes(rs.getString("notes"));
        return progress;
    }
}

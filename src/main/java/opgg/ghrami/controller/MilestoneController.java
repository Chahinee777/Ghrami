package opgg.ghrami.controller;

import opgg.ghrami.model.Milestone;
import opgg.ghrami.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MilestoneController {
    private Connection connection;

    public MilestoneController() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    public Milestone create(Milestone milestone) {
        String sql = "INSERT INTO milestones (hobby_id, title, target_date, is_achieved) VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, milestone.getHobbyId());
            stmt.setString(2, milestone.getTitle());
            stmt.setDate(3, milestone.getTargetDate() != null ? Date.valueOf(milestone.getTargetDate()) : null);
            stmt.setBoolean(4, milestone.getIsAchieved());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        milestone.setMilestoneId(generatedKeys.getLong(1));
                        System.out.println("Milestone created: " + milestone.getTitle() + " (ID: " + milestone.getMilestoneId() + ")");
                        return milestone;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating milestone: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public Optional<Milestone> findById(Long milestoneId) {
        String sql = "SELECT * FROM milestones WHERE milestone_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, milestoneId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToMilestone(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding milestone by ID: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public List<Milestone> findByHobbyId(Long hobbyId) {
        List<Milestone> milestones = new ArrayList<>();
        String sql = "SELECT * FROM milestones WHERE hobby_id = ? ORDER BY target_date ASC";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, hobbyId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                milestones.add(mapResultSetToMilestone(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding milestones by hobby ID: " + e.getMessage());
            e.printStackTrace();
        }
        return milestones;
    }

    public List<Milestone> findAll() {
        List<Milestone> milestones = new ArrayList<>();
        String sql = "SELECT * FROM milestones ORDER BY target_date ASC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                milestones.add(mapResultSetToMilestone(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding all milestones: " + e.getMessage());
            e.printStackTrace();
        }
        return milestones;
    }

    public int countByHobbyId(Long hobbyId) {
        String sql = "SELECT COUNT(*) FROM milestones WHERE hobby_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, hobbyId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting milestones: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    public int countAchievedByHobbyId(Long hobbyId) {
        String sql = "SELECT COUNT(*) FROM milestones WHERE hobby_id = ? AND is_achieved = TRUE";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, hobbyId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting achieved milestones: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    public Milestone update(Milestone milestone) {
        String sql = "UPDATE milestones SET title = ?, target_date = ?, is_achieved = ? WHERE milestone_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, milestone.getTitle());
            stmt.setDate(2, milestone.getTargetDate() != null ? Date.valueOf(milestone.getTargetDate()) : null);
            stmt.setBoolean(3, milestone.getIsAchieved());
            stmt.setLong(4, milestone.getMilestoneId());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("Milestone updated: " + milestone.getTitle());
                return milestone;
            }
        } catch (SQLException e) {
            System.err.println("Error updating milestone: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public boolean toggleAchieved(Long milestoneId) {
        Optional<Milestone> milestoneOpt = findById(milestoneId);
        if (milestoneOpt.isPresent()) {
            Milestone milestone = milestoneOpt.get();
            milestone.setIsAchieved(!milestone.getIsAchieved());
            return update(milestone) != null;
        }
        return false;
    }

    public boolean delete(Long milestoneId) {
        String sql = "DELETE FROM milestones WHERE milestone_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, milestoneId);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("Milestone deleted: " + milestoneId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error deleting milestone: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    private Milestone mapResultSetToMilestone(ResultSet rs) throws SQLException {
        Milestone milestone = new Milestone();
        milestone.setMilestoneId(rs.getLong("milestone_id"));
        milestone.setHobbyId(rs.getLong("hobby_id"));
        milestone.setTitle(rs.getString("title"));

        Date targetDate = rs.getDate("target_date");
        if (targetDate != null) {
            milestone.setTargetDate(targetDate.toLocalDate());
        }

        milestone.setIsAchieved(rs.getBoolean("is_achieved"));
        return milestone;
    }
}

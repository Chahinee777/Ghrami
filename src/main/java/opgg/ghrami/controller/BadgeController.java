package opgg.ghrami.controller;

import opgg.ghrami.model.Badge;
import opgg.ghrami.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class BadgeController {

    public Badge create(Badge badge) {
        String sql = "INSERT INTO badges (user_id, name, description, earned_date) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, badge.getUserId());
            stmt.setString(2, badge.getName());
            stmt.setString(3, badge.getDescription());
            stmt.setTimestamp(4, Timestamp.valueOf(badge.getEarnedDate()));

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        badge.setBadgeId(generatedKeys.getLong(1));
                        System.out.println("Badge created: " + badge.getName() + " (ID: " + badge.getBadgeId() + ")");
                        return badge;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating badge: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public Optional<Badge> findById(Long badgeId) {
        String sql = "SELECT * FROM badges WHERE badge_id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, badgeId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToBadge(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding badge by ID: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public List<Badge> findAll() {
        List<Badge> badges = new ArrayList<>();
        String sql = "SELECT * FROM badges ORDER BY earned_date DESC";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                badges.add(mapResultSetToBadge(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding all badges: " + e.getMessage());
            e.printStackTrace();
        }
        return badges;
    }

    public Badge update(Badge badge) {
        String sql = "UPDATE badges SET user_id = ?, name = ?, description = ?, earned_date = ? WHERE badge_id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, badge.getUserId());
            stmt.setString(2, badge.getName());
            stmt.setString(3, badge.getDescription());
            stmt.setTimestamp(4, Timestamp.valueOf(badge.getEarnedDate()));
            stmt.setLong(5, badge.getBadgeId());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("Badge updated: " + badge.getName());
                return badge;
            }
        } catch (SQLException e) {
            System.err.println("Error updating badge: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public boolean delete(Long badgeId) {
        String sql = "DELETE FROM badges WHERE badge_id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, badgeId);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("Badge deleted: " + badgeId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error deleting badge: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Find all badges for a specific user
     */
    public List<Badge> findByUserId(Long userId) {
        List<Badge> badges = new ArrayList<>();
        String sql = "SELECT * FROM badges WHERE user_id = ? ORDER BY earned_date DESC";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                badges.add(mapResultSetToBadge(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding badges by user ID: " + e.getMessage());
            e.printStackTrace();
        }
        return badges;
    }

    /**
     * Count badges for a specific user
     */
    public int countByUserId(Long userId) {
        String sql = "SELECT COUNT(*) FROM badges WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting badges: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Check if a user already has a badge with the same name
     */
    public boolean userHasBadge(Long userId, String badgeName) {
        String sql = "SELECT COUNT(*) FROM badges WHERE user_id = ? AND name = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setString(2, badgeName);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking badge existence: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    private Badge mapResultSetToBadge(ResultSet rs) throws SQLException {
        Badge badge = new Badge();
        badge.setBadgeId(rs.getLong("badge_id"));
        badge.setUserId(rs.getLong("user_id"));
        badge.setName(rs.getString("name"));
        badge.setDescription(rs.getString("description"));

        Timestamp earnedDate = rs.getTimestamp("earned_date");
        if (earnedDate != null) {
            badge.setEarnedDate(earnedDate.toLocalDateTime());
        }

        return badge;
    }
}

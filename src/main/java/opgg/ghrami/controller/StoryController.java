package opgg.ghrami.controller;

import opgg.ghrami.model.Story;
import opgg.ghrami.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class StoryController {

    private static StoryController instance;

    private StoryController() {}

    public static StoryController getInstance() {
        if (instance == null) instance = new StoryController();
        return instance;
    }

    /** Create a new story (expires in 24h automatically via DB default). */
    public Story create(Story story) {
        String sql = "INSERT INTO stories (user_id, caption, image_url, created_at, expires_at) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, story.getUserId());
            stmt.setString(2, story.getCaption());
            stmt.setString(3, story.getImageUrl());
            stmt.setTimestamp(4, Timestamp.valueOf(story.getCreatedAt()));
            stmt.setTimestamp(5, Timestamp.valueOf(story.getExpiresAt()));
            if (stmt.executeUpdate() > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) story.setStoryId(keys.getLong(1));
                }
            }
            return story;
        } catch (SQLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /** Get all active (non-expired) stories for a user and their accepted friends. */
    public List<Story> getActiveStoriesForFeed(Long userId) {
        List<Story> stories = new ArrayList<>();
        String sql = "SELECT s.*, u.full_name, u.profile_picture " +
                     "FROM stories s " +
                     "JOIN users u ON s.user_id = u.user_id " +
                     "WHERE s.expires_at > NOW() " +
                     "AND (s.user_id = ? " +
                     "  OR s.user_id IN (" +
                     "    SELECT user2_id FROM friendships WHERE user1_id = ? AND status = 'ACCEPTED' " +
                     "    UNION " +
                     "    SELECT user1_id FROM friendships WHERE user2_id = ? AND status = 'ACCEPTED'" +
                     "  )" +
                     ") ORDER BY s.created_at DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, userId);
            stmt.setLong(3, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) stories.add(map(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stories;
    }

    /** Get active stories by a specific user. */
    public List<Story> getActiveByUser(Long userId) {
        List<Story> stories = new ArrayList<>();
        String sql = "SELECT s.*, u.full_name, u.profile_picture " +
                     "FROM stories s JOIN users u ON s.user_id = u.user_id " +
                     "WHERE s.user_id = ? AND s.expires_at > NOW() ORDER BY s.created_at DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) stories.add(map(rs));
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return stories;
    }

    public boolean hasActiveStory(Long userId) {
        String sql = "SELECT 1 FROM stories WHERE user_id = ? AND expires_at > NOW() LIMIT 1";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            return stmt.executeQuery().next();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /** Delete all expired stories (call on startup to clean up). */
    public void purgeExpired() {
        String sql = "DELETE FROM stories WHERE expires_at <= NOW()";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private Story map(ResultSet rs) throws SQLException {
        Story s = new Story();
        s.setStoryId(rs.getLong("story_id"));
        s.setUserId(rs.getLong("user_id"));
        s.setCaption(rs.getString("caption"));
        s.setImageUrl(rs.getString("image_url"));
        Timestamp created = rs.getTimestamp("created_at");
        if (created != null) s.setCreatedAt(created.toLocalDateTime());
        Timestamp expires = rs.getTimestamp("expires_at");
        if (expires != null) s.setExpiresAt(expires.toLocalDateTime());
        s.setAuthorName(rs.getString("full_name"));
        s.setAuthorProfilePicture(rs.getString("profile_picture"));
        return s;
    }
}

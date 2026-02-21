package opgg.ghrami.controller;

import opgg.ghrami.model.Notification;
import opgg.ghrami.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class NotificationController {

    private static NotificationController instance;

    private NotificationController() {}

    public static NotificationController getInstance() {
        if (instance == null) instance = new NotificationController();
        return instance;
    }

    /** Create and persist a notification. */
    public Notification create(Long userId, String type, String content, Long relatedUserId) {
        String sql = "INSERT INTO notifications (user_id, type, content, related_user_id) VALUES (?, ?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, userId);
            stmt.setString(2, type);
            stmt.setString(3, content);
            if (relatedUserId != null) stmt.setLong(4, relatedUserId);
            else stmt.setNull(4, Types.BIGINT);

            if (stmt.executeUpdate() > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        Notification notif = new Notification(userId, type, content, relatedUserId);
                        notif.setNotificationId(rs.getLong(1));
                        return notif;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating notification: " + e.getMessage());
        }
        return null;
    }

    /** Get all notifications for a user, newest first. */
    public List<Notification> getForUser(Long userId) {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE user_id = ? ORDER BY created_at DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching notifications: " + e.getMessage());
        }
        return list;
    }

    /** Get only unread notifications for a user. */
    public List<Notification> getUnread(Long userId) {
        List<Notification> list = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE user_id = ? AND is_read = 0 ORDER BY created_at DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error fetching unread notifications: " + e.getMessage());
        }
        return list;
    }

    /** Count unread notifications for a user. */
    public int countUnread(Long userId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE user_id = ? AND is_read = 0";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting notifications: " + e.getMessage());
        }
        return 0;
    }

    /** Mark a single notification as read. */
    public void markRead(Long notificationId) {
        String sql = "UPDATE notifications SET is_read = 1 WHERE notification_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, notificationId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error marking notification read: " + e.getMessage());
        }
    }

    /** Mark all notifications for a user as read. */
    public void markAllRead(Long userId) {
        String sql = "UPDATE notifications SET is_read = 1 WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error marking all notifications read: " + e.getMessage());
        }
    }

    /** Delete all notifications for a user. */
    public void clearAll(Long userId) {
        String sql = "DELETE FROM notifications WHERE user_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error clearing notifications: " + e.getMessage());
        }
    }

    // ─── Convenience factory methods ────────────────────────────────────────────

    public void notifyFriendRequest(Long toUserId, String fromUsername, Long fromUserId) {
        create(toUserId, "FRIEND_REQUEST",
                fromUsername + " vous a envoyé une demande d'amitié", fromUserId);
    }

    public void notifyFriendAccepted(Long toUserId, String acceptorUsername, Long acceptorUserId) {
        create(toUserId, "FRIEND_ACCEPTED",
                acceptorUsername + " a accepté votre demande d'amitié ✅", acceptorUserId);
    }

    public void notifyNewMessage(Long toUserId, String senderUsername, Long senderUserId) {
        create(toUserId, "MESSAGE",
                senderUsername + " vous a envoyé un message 💬", senderUserId);
    }

    public void notifyMeeting(Long toUserId, String meetingTitle, Long organizerUserId) {
        create(toUserId, "MEETING",
                "Nouveau meetup : \"" + meetingTitle + "\" 📅", organizerUserId);
    }

    private Notification mapRow(ResultSet rs) throws SQLException {
        Notification n = new Notification();
        n.setNotificationId(rs.getLong("notification_id"));
        n.setUserId(rs.getLong("user_id"));
        n.setType(rs.getString("type"));
        n.setContent(rs.getString("content"));
        long relId = rs.getLong("related_user_id");
        if (!rs.wasNull()) n.setRelatedUserId(relId);
        Timestamp ts = rs.getTimestamp("created_at");
        if (ts != null) n.setCreatedAt(ts.toLocalDateTime());
        n.setRead(rs.getBoolean("is_read"));
        return n;
    }
}

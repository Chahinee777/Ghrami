package opgg.ghrami.controller;

import opgg.ghrami.model.Message;
import opgg.ghrami.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageController {

    private static MessageController instance;

    private MessageController() {}

    public static MessageController getInstance() {
        if (instance == null) instance = new MessageController();
        return instance;
    }

    /** Send a message from senderId to receiverId. Returns the saved message or null on failure. */
    public Message send(Long senderId, Long receiverId, String content) {
        String sql = "INSERT INTO messages (sender_id, receiver_id, content) VALUES (?, ?, ?)";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, senderId);
            stmt.setLong(2, receiverId);
            stmt.setString(3, content);
            if (stmt.executeUpdate() > 0) {
                try (ResultSet rs = stmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        Message msg = new Message(senderId, receiverId, content);
                        msg.setMessageId(rs.getLong(1));
                        return msg;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error sending message: " + e.getMessage());
        }
        return null;
    }

    /**
     * Get the conversation between two users, ordered by sent_at ascending.
     */
    public List<Message> getConversation(Long userId1, Long userId2) {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT * FROM messages WHERE " +
                "(sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?) " +
                "ORDER BY sent_at ASC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId1);
            stmt.setLong(2, userId2);
            stmt.setLong(3, userId2);
            stmt.setLong(4, userId1);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapRow(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading conversation: " + e.getMessage());
        }
        return messages;
    }

    /**
     * Returns the list of user IDs that the given user has had a conversation with,
     * ordered by the most recent message first.
     */
    public List<Long> getConversationPartners(Long userId) {
        List<Long> partners = new ArrayList<>();
        String sql = "SELECT DISTINCT CASE WHEN sender_id = ? THEN receiver_id ELSE sender_id END AS partner_id, " +
                "MAX(sent_at) AS last_msg " +
                "FROM messages WHERE sender_id = ? OR receiver_id = ? " +
                "GROUP BY partner_id ORDER BY last_msg DESC";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, userId);
            stmt.setLong(3, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    partners.add(rs.getLong("partner_id"));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error loading conversation partners: " + e.getMessage());
        }
        return partners;
    }

    /** Mark all messages sent to userId from senderId as read. */
    public void markAsRead(Long senderId, Long receiverId) {
        String sql = "UPDATE messages SET is_read = 1 WHERE sender_id = ? AND receiver_id = ?";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, senderId);
            stmt.setLong(2, receiverId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error marking messages as read: " + e.getMessage());
        }
    }

    /** Count unread messages for a user. */
    public int countUnread(Long userId) {
        String sql = "SELECT COUNT(*) FROM messages WHERE receiver_id = ? AND is_read = 0";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting unread messages: " + e.getMessage());
        }
        return 0;
    }

    /** Count unread messages from a specific sender. */
    public int countUnreadFrom(Long senderId, Long receiverId) {
        String sql = "SELECT COUNT(*) FROM messages WHERE sender_id = ? AND receiver_id = ? AND is_read = 0";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, senderId);
            stmt.setLong(2, receiverId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting unread messages from user: " + e.getMessage());
        }
        return 0;
    }

    /** Get the last message between two users (used in conversation list preview). */
    public Message getLastMessage(Long userId1, Long userId2) {
        String sql = "SELECT * FROM messages WHERE " +
                "(sender_id = ? AND receiver_id = ?) OR (sender_id = ? AND receiver_id = ?) " +
                "ORDER BY sent_at DESC LIMIT 1";
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId1);
            stmt.setLong(2, userId2);
            stmt.setLong(3, userId2);
            stmt.setLong(4, userId1);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error getting last message: " + e.getMessage());
        }
        return null;
    }

    private Message mapRow(ResultSet rs) throws SQLException {
        Message msg = new Message();
        msg.setMessageId(rs.getLong("message_id"));
        msg.setSenderId(rs.getLong("sender_id"));
        msg.setReceiverId(rs.getLong("receiver_id"));
        msg.setContent(rs.getString("content"));
        Timestamp ts = rs.getTimestamp("sent_at");
        if (ts != null) msg.setSentAt(ts.toLocalDateTime());
        msg.setRead(rs.getBoolean("is_read"));
        return msg;
    }
}

package opgg.ghrami.model;

import java.time.LocalDateTime;

public class Notification {
    private Long notificationId;
    private Long userId;
    private String type;       // FRIEND_REQUEST, FRIEND_ACCEPTED, MESSAGE, MEETING, BADGE, etc.
    private String content;
    private Long relatedUserId; // optional - the user who triggered it
    private LocalDateTime createdAt;
    private boolean isRead;

    public Notification() {
        this.createdAt = LocalDateTime.now();
        this.isRead = false;
    }

    public Notification(Long userId, String type, String content, Long relatedUserId) {
        this();
        this.userId = userId;
        this.type = type;
        this.content = content;
        this.relatedUserId = relatedUserId;
    }

    public Long getNotificationId() { return notificationId; }
    public void setNotificationId(Long notificationId) { this.notificationId = notificationId; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Long getRelatedUserId() { return relatedUserId; }
    public void setRelatedUserId(Long relatedUserId) { this.relatedUserId = relatedUserId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }

    /** Emoji icon based on type */
    public String getIcon() {
        switch (type == null ? "" : type) {
            case "FRIEND_REQUEST":  return "👥";
            case "FRIEND_ACCEPTED": return "✅";
            case "MESSAGE":         return "💬";
            case "MEETING":         return "📅";
            case "BADGE":           return "🏆";
            default:                return "🔔";
        }
    }
}

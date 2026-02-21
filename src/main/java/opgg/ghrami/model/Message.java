package opgg.ghrami.model;

import java.time.LocalDateTime;

public class Message {
    private Long messageId;
    private Long senderId;
    private Long receiverId;
    private String content;
    private LocalDateTime sentAt;
    private boolean isRead;

    public Message() {
        this.sentAt = LocalDateTime.now();
        this.isRead = false;
    }

    public Message(Long senderId, Long receiverId, String content) {
        this();
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.content = content;
    }

    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }

    public Long getSenderId() { return senderId; }
    public void setSenderId(Long senderId) { this.senderId = senderId; }

    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public LocalDateTime getSentAt() { return sentAt; }
    public void setSentAt(LocalDateTime sentAt) { this.sentAt = sentAt; }

    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
}

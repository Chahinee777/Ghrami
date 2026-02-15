package opgg.ghrami.model;

import java.time.LocalDateTime;

public class Comment {
    private Long commentId;
    private Long postId;
    private Long userId;
    private String content;
    private LocalDateTime createdAt;
    
    // Transient fields for UI display (not in database)
    private String authorName;
    private String authorProfilePicture;
    
    // Constructors
    public Comment() {
        this.createdAt = LocalDateTime.now();
    }
    
    public Comment(Long postId, Long userId, String content) {
        this();
        this.postId = postId;
        this.userId = userId;
        this.content = content;
    }
    
    // Getters and Setters
    public Long getCommentId() {
        return commentId;
    }
    
    public void setCommentId(Long commentId) {
        this.commentId = commentId;
    }
    
    public Long getPostId() {
        return postId;
    }
    
    public void setPostId(Long postId) {
        this.postId = postId;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getAuthorName() {
        return authorName;
    }
    
    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }
    
    public String getAuthorProfilePicture() {
        return authorProfilePicture;
    }
    
    public void setAuthorProfilePicture(String authorProfilePicture) {
        this.authorProfilePicture = authorProfilePicture;
    }
    
    @Override
    public String toString() {
        return "Comment{" +
                "commentId=" + commentId +
                ", postId=" + postId +
                ", userId=" + userId +
                ", content='" + content + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}

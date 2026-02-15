package opgg.ghrami.model;

import java.time.LocalDateTime;

public class Post {
    private Long postId;
    private Long userId;
    private String content;
    private String imageUrl;
    private LocalDateTime createdAt;
    
    // Transient fields for UI display (not in database)
    private String authorName;
    private String authorProfilePicture;
    private int commentsCount;
    
    // Constructors
    public Post() {
        this.createdAt = LocalDateTime.now();
    }
    
    public Post(Long userId, String content) {
        this();
        this.userId = userId;
        this.content = content;
    }
    
    public Post(Long userId, String content, String imageUrl) {
        this(userId, content);
        this.imageUrl = imageUrl;
    }
    
    // Getters and Setters
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
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
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
    
    public int getCommentsCount() {
        return commentsCount;
    }
    
    public void setCommentsCount(int commentsCount) {
        this.commentsCount = commentsCount;
    }
    
    @Override
    public String toString() {
        return "Post{" +
                "postId=" + postId +
                ", userId=" + userId +
                ", content='" + content + '\'' +
                ", imageUrl='" + imageUrl + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}

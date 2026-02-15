package ghrami.model;
import java.time.LocalDateTime;
public class post {

        private int idPost;
        private String content;
        private String type; // ex: "text", "image", "video"
        private LocalDateTime createdAt;
        private int likeCount;
        private int commentCount;
        private int userId; // l'id de l'utilisateur qui a créé le post

        // Constructeurs
        public post() {}

        public post(int idPost, String content, String type, LocalDateTime createdAt, int likeCount, int commentCount, int userId) {
            this.idPost = idPost;
            this.content = content;
            this.type = type;
            this.createdAt = createdAt;
            this.likeCount = likeCount;
            this.commentCount = commentCount;
            this.userId = userId;
        }

        // Getters et Setters
        public int getIdPost() { return idPost; }
        public void setIdPost(int idPost) { this.idPost = idPost; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public int getLikeCount() { return likeCount; }
        public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

        public int getCommentCount() { return commentCount; }
        public void setCommentCount(int commentCount) { this.commentCount = commentCount; }

        public int getUserId() { return userId; }
        public void setUserId(int userId) { this.userId = userId; }

        // toString pour debug
        @Override
        public String toString() {
            return "Post{" +
                    "idPost=" + idPost +
                    ", content='" + content + '\'' +
                    ", type='" + type + '\'' +
                    ", createdAt=" + createdAt +
                    ", likeCount=" + likeCount +
                    ", commentCount=" + commentCount +
                    ", userId=" + userId +
                    '}';
        }
    }



package ghrami.model;
import java.time.LocalDateTime;

public class comment {

        private int idComment;
        private String content;
        private LocalDateTime createdAt;
        private int likeCount;
        private int idPost; // référence au post commenté
        private int userId; // utilisateur qui a écrit le commentaire

        // Constructeurs
        public comment() {
            // constructeur vide nécessaire pour l'affichage depuis la DB
        }

        public comment( String content, LocalDateTime createdAt, int likeCount, int idPost, int userId) {

            this.content = content;
            this.createdAt = createdAt;
            this.likeCount = likeCount;
            this.idPost = idPost;
            this.userId = userId;
        }

        // Getters et Setters
        public int getIdComment() { return idComment; }
        public void setIdComment(int idComment) { this.idComment = idComment; }

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }

        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

        public int getLikeCount() { return likeCount; }
        public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

        public int getIdPost() { return idPost; }
        public void setIdPost(int idPost) { this.idPost = idPost; }

        public int getUserId() { return userId; }
        public void setUserId(int userId) { this.userId = userId; }

        // toString pour debug
        @Override
        public String toString() {
            return "Comment{" +
                    "idComment=" + idComment +
                    ", content='" + content + '\'' +
                    ", createdAt=" + createdAt +
                    ", likeCount=" + likeCount +
                    ", idPost=" + idPost +
                    ", userId=" + userId +
                    '}';
        }
    }



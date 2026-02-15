package opgg.ghrami.controller;

import opgg.ghrami.model.Comment;
import opgg.ghrami.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class CommentController {
    
    private static CommentController instance;
    
    private CommentController() {}
    
    public static CommentController getInstance() {
        if (instance == null) {
            instance = new CommentController();
        }
        return instance;
    }
    
    // Create a new comment
    public Comment create(Comment comment) {
        String sql = "INSERT INTO comments (post_id, user_id, content, created_at) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setLong(1, comment.getPostId());
            stmt.setLong(2, comment.getUserId());
            stmt.setString(3, comment.getContent());
            stmt.setTimestamp(4, Timestamp.valueOf(comment.getCreatedAt()));
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        comment.setCommentId(generatedKeys.getLong(1));
                    }
                }
            }
            
            return comment;
            
        } catch (SQLException e) {
            System.err.println("Error creating comment: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    // Find comment by ID
    public Optional<Comment> findById(Long commentId) {
        String sql = "SELECT c.*, u.full_name, u.profile_picture " +
                     "FROM comments c " +
                     "JOIN users u ON c.user_id = u.user_id " +
                     "WHERE c.comment_id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, commentId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToComment(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error finding comment: " + e.getMessage());
            e.printStackTrace();
        }
        
        return Optional.empty();
    }
    
    // Find all comments for a post
    public List<Comment> findByPostId(Long postId) {
        List<Comment> comments = new ArrayList<>();
        String sql = "SELECT c.*, u.full_name, u.profile_picture " +
                     "FROM comments c " +
                     "JOIN users u ON c.user_id = u.user_id " +
                     "WHERE c.post_id = ? " +
                     "ORDER BY c.created_at ASC";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, postId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                comments.add(mapResultSetToComment(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error finding comments by post: " + e.getMessage());
            e.printStackTrace();
        }
        
        return comments;
    }
    
    // Find all comments by a user
    public List<Comment> findByUserId(Long userId) {
        List<Comment> comments = new ArrayList<>();
        String sql = "SELECT c.*, u.full_name, u.profile_picture " +
                     "FROM comments c " +
                     "JOIN users u ON c.user_id = u.user_id " +
                     "WHERE c.user_id = ? " +
                     "ORDER BY c.created_at DESC";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                comments.add(mapResultSetToComment(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error finding comments by user: " + e.getMessage());
            e.printStackTrace();
        }
        
        return comments;
    }
    
    // Update comment
    public Comment update(Comment comment) {
        String sql = "UPDATE comments SET content = ? WHERE comment_id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, comment.getContent());
            stmt.setLong(2, comment.getCommentId());
            
            stmt.executeUpdate();
            return comment;
            
        } catch (SQLException e) {
            System.err.println("Error updating comment: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    // Delete comment
    public boolean delete(Long commentId) {
        String sql = "DELETE FROM comments WHERE comment_id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, commentId);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
            
        } catch (SQLException e) {
            System.err.println("Error deleting comment: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // Count comments for a post
    public int countByPostId(Long postId) {
        String sql = "SELECT COUNT(*) FROM comments WHERE post_id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, postId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            
        } catch (SQLException e) {
            System.err.println("Error counting comments: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0;
    }
    
    // Count total comments
    public int countComments() {
        String sql = "SELECT COUNT(*) FROM comments";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            
        } catch (SQLException e) {
            System.err.println("Error counting total comments: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0;
    }
    
    // Helper method to map ResultSet to Comment object
    private Comment mapResultSetToComment(ResultSet rs) throws SQLException {
        Comment comment = new Comment();
        comment.setCommentId(rs.getLong("comment_id"));
        comment.setPostId(rs.getLong("post_id"));
        comment.setUserId(rs.getLong("user_id"));
        comment.setContent(rs.getString("content"));
        
        Timestamp timestamp = rs.getTimestamp("created_at");
        if (timestamp != null) {
            comment.setCreatedAt(timestamp.toLocalDateTime());
        }
        
        // Set transient fields from JOIN
        comment.setAuthorName(rs.getString("full_name"));
        comment.setAuthorProfilePicture(rs.getString("profile_picture"));
        
        return comment;
    }
}

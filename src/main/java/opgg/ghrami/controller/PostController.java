package opgg.ghrami.controller;

import opgg.ghrami.model.Post;
import opgg.ghrami.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PostController {
    
    private static PostController instance;
    
    private PostController() {}
    
    public static PostController getInstance() {
        if (instance == null) {
            instance = new PostController();
        }
        return instance;
    }
    
    // Create a new post
    public Post create(Post post) {
        String sql = "INSERT INTO posts (user_id, content, image_url, created_at) VALUES (?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setLong(1, post.getUserId());
            stmt.setString(2, post.getContent());
            stmt.setString(3, post.getImageUrl());
            stmt.setTimestamp(4, Timestamp.valueOf(post.getCreatedAt()));
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        post.setPostId(generatedKeys.getLong(1));
                    }
                }
            }
            
            return post;
            
        } catch (SQLException e) {
            System.err.println("Error creating post: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    // Find post by ID with author details
    public Optional<Post> findById(Long postId) {
        String sql = "SELECT p.*, u.full_name, u.profile_picture, " +
                     "(SELECT COUNT(*) FROM comments WHERE post_id = p.post_id) as comments_count " +
                     "FROM posts p " +
                     "JOIN users u ON p.user_id = u.user_id " +
                     "WHERE p.post_id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, postId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToPost(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error finding post: " + e.getMessage());
            e.printStackTrace();
        }
        
        return Optional.empty();
    }
    
    // Find all posts (ordered by most recent)
    public List<Post> findAll() {
        List<Post> posts = new ArrayList<>();
        String sql = "SELECT p.*, u.full_name, u.profile_picture, " +
                     "(SELECT COUNT(*) FROM comments WHERE post_id = p.post_id) as comments_count " +
                     "FROM posts p " +
                     "JOIN users u ON p.user_id = u.user_id " +
                     "ORDER BY p.created_at DESC";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                posts.add(mapResultSetToPost(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error finding all posts: " + e.getMessage());
            e.printStackTrace();
        }
        
        return posts;
    }
    
    // Find posts by user ID
    public List<Post> findByUserId(Long userId) {
        List<Post> posts = new ArrayList<>();
        String sql = "SELECT p.*, u.full_name, u.profile_picture, " +
                     "(SELECT COUNT(*) FROM comments WHERE post_id = p.post_id) as comments_count " +
                     "FROM posts p " +
                     "JOIN users u ON p.user_id = u.user_id " +
                     "WHERE p.user_id = ? " +
                     "ORDER BY p.created_at DESC";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                posts.add(mapResultSetToPost(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error finding posts by user: " + e.getMessage());
            e.printStackTrace();
        }
        
        return posts;
    }
    
    // Get feed for user (their posts + friends' posts)
    public List<Post> getFeedForUser(Long userId) {
        List<Post> posts = new ArrayList<>();
        String sql = "SELECT p.*, u.full_name, u.profile_picture, " +
                     "(SELECT COUNT(*) FROM comments WHERE post_id = p.post_id) as comments_count " +
                     "FROM posts p " +
                     "JOIN users u ON p.user_id = u.user_id " +
                     "WHERE p.user_id = ? " +
                     "OR p.user_id IN (" +
                     "  SELECT user2_id FROM friendships WHERE user1_id = ? AND status = 'ACCEPTED' " +
                     "  UNION " +
                     "  SELECT user1_id FROM friendships WHERE user2_id = ? AND status = 'ACCEPTED'" +
                     ") " +
                     "ORDER BY p.created_at DESC " +
                     "LIMIT 50";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            stmt.setLong(2, userId);
            stmt.setLong(3, userId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                posts.add(mapResultSetToPost(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error getting feed: " + e.getMessage());
            e.printStackTrace();
        }
        
        return posts;
    }
    
    // Update post
    public Post update(Post post) {
        String sql = "UPDATE posts SET content = ?, image_url = ? WHERE post_id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, post.getContent());
            stmt.setString(2, post.getImageUrl());
            stmt.setLong(3, post.getPostId());
            
            stmt.executeUpdate();
            return post;
            
        } catch (SQLException e) {
            System.err.println("Error updating post: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    // Delete post
    public boolean delete(Long postId) {
        String sql = "DELETE FROM posts WHERE post_id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, postId);
            int affectedRows = stmt.executeUpdate();
            return affectedRows > 0;
            
        } catch (SQLException e) {
            System.err.println("Error deleting post: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    // Count total posts
    public int countPosts() {
        String sql = "SELECT COUNT(*) FROM posts";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            
        } catch (SQLException e) {
            System.err.println("Error counting posts: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0;
    }
    
    // Count posts by user
    public int countPostsByUser(Long userId) {
        String sql = "SELECT COUNT(*) FROM posts WHERE user_id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1);
            }
            
        } catch (SQLException e) {
            System.err.println("Error counting user posts: " + e.getMessage());
            e.printStackTrace();
        }
        
        return 0;
    }
    
    // Helper method to map ResultSet to Post object
    private Post mapResultSetToPost(ResultSet rs) throws SQLException {
        Post post = new Post();
        post.setPostId(rs.getLong("post_id"));
        post.setUserId(rs.getLong("user_id"));
        post.setContent(rs.getString("content"));
        post.setImageUrl(rs.getString("image_url"));
        
        Timestamp timestamp = rs.getTimestamp("created_at");
        if (timestamp != null) {
            post.setCreatedAt(timestamp.toLocalDateTime());
        }
        
        // Set transient fields from JOIN
        post.setAuthorName(rs.getString("full_name"));
        post.setAuthorProfilePicture(rs.getString("profile_picture"));
        post.setCommentsCount(rs.getInt("comments_count"));
        
        return post;
    }
}

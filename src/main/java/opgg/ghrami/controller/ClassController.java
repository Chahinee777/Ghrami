package opgg.ghrami.controller;

import opgg.ghrami.model.ClassEntity;
import opgg.ghrami.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for managing classes/courses
 */
public class ClassController {
    private static ClassController instance;
    
    private ClassController() {
    }
    
    public static ClassController getInstance() {
        if (instance == null) {
            instance = new ClassController();
        }
        return instance;
    }
    
    /**
     * Create a new class
     */
    public boolean create(ClassEntity classEntity) {
        String sql = "INSERT INTO classes (provider_id, title, description, category, price, duration, max_participants) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setLong(1, classEntity.getProviderId());
            stmt.setString(2, classEntity.getTitle());
            stmt.setString(3, classEntity.getDescription());
            stmt.setString(4, classEntity.getCategory());
            stmt.setDouble(5, classEntity.getPrice());
            stmt.setInt(6, classEntity.getDuration());
            stmt.setInt(7, classEntity.getMaxParticipants());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        classEntity.setClassId(generatedKeys.getLong(1));
                    }
                }
                System.out.println("Class created successfully: " + classEntity.getClassId());
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("Error creating class: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Get class by ID with provider details and enrollment count
     */
    public ClassEntity getById(Long classId) {
        String sql = "SELECT c.*, " +
                    "  cp.company_name as provider_company, " +
                    "  cp.rating as provider_rating, " +
                    "  cp.is_verified as provider_verified, " +
                    "  u.full_name as provider_name, " +
                    "  (SELECT COUNT(*) FROM bookings WHERE class_id = c.class_id AND status != 'cancelled') as current_enrollment " +
                    "FROM classes c " +
                    "JOIN class_providers cp ON c.provider_id = cp.provider_id " +
                    "JOIN users u ON cp.user_id = u.user_id " +
                    "WHERE c.class_id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, classId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToClass(rs);
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching class: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Get all classes with filters
     */
    public List<ClassEntity> getAll(String category, Double minPrice, Double maxPrice) {
        List<ClassEntity> classes = new ArrayList<>();
        StringBuilder sql = new StringBuilder(
            "SELECT c.*, " +
            "  cp.company_name as provider_company, " +
            "  cp.rating as provider_rating, " +
            "  cp.is_verified as provider_verified, " +
            "  u.full_name as provider_name, " +
            "  (SELECT COUNT(*) FROM bookings WHERE class_id = c.class_id AND status != 'cancelled') as current_enrollment " +
            "FROM classes c " +
            "JOIN class_providers cp ON c.provider_id = cp.provider_id " +
            "JOIN users u ON cp.user_id = u.user_id " +
            "WHERE cp.is_verified = TRUE"
        );
        
        // Add filters
        if (category != null && !category.isEmpty()) {
            sql.append(" AND c.category = '").append(category).append("'");
        }
        if (minPrice != null) {
            sql.append(" AND c.price >= ").append(minPrice);
        }
        if (maxPrice != null) {
            sql.append(" AND c.price <= ").append(maxPrice);
        }
        
        sql.append(" ORDER BY cp.rating DESC, c.class_id DESC");
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql.toString())) {
            
            while (rs.next()) {
                classes.add(mapResultSetToClass(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching classes: " + e.getMessage());
            e.printStackTrace();
        }
        return classes;
    }
    
    /**
     * Get all classes by provider
     */
    public List<ClassEntity> getByProviderId(Long providerId) {
        List<ClassEntity> classes = new ArrayList<>();
        String sql = "SELECT c.*, " +
                    "  cp.company_name as provider_company, " +
                    "  cp.rating as provider_rating, " +
                    "  cp.is_verified as provider_verified, " +
                    "  u.full_name as provider_name, " +
                    "  (SELECT COUNT(*) FROM bookings WHERE class_id = c.class_id AND status != 'cancelled') as current_enrollment " +
                    "FROM classes c " +
                    "JOIN class_providers cp ON c.provider_id = cp.provider_id " +
                    "JOIN users u ON cp.user_id = u.user_id " +
                    "WHERE c.provider_id = ? " +
                    "ORDER BY c.class_id DESC";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, providerId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                classes.add(mapResultSetToClass(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching provider classes: " + e.getMessage());
            e.printStackTrace();
        }
        return classes;
    }
    
    /**
     * Get all available categories
     */
    public List<String> getCategories() {
        List<String> categories = new ArrayList<>();
        String sql = "SELECT DISTINCT category FROM classes WHERE category IS NOT NULL ORDER BY category";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                categories.add(rs.getString("category"));
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching categories: " + e.getMessage());
            e.printStackTrace();
        }
        return categories;
    }
    
    /**
     * Search classes by keyword (title or description)
     */
    public List<ClassEntity> search(String keyword) {
        List<ClassEntity> classes = new ArrayList<>();
        String sql = "SELECT c.*, " +
                    "  cp.company_name as provider_company, " +
                    "  cp.rating as provider_rating, " +
                    "  cp.is_verified as provider_verified, " +
                    "  u.full_name as provider_name, " +
                    "  (SELECT COUNT(*) FROM bookings WHERE class_id = c.class_id AND status != 'cancelled') as current_enrollment " +
                    "FROM classes c " +
                    "JOIN class_providers cp ON c.provider_id = cp.provider_id " +
                    "JOIN users u ON cp.user_id = u.user_id " +
                    "WHERE cp.is_verified = TRUE " +
                    "  AND (c.title LIKE ? OR c.description LIKE ? OR c.category LIKE ?) " +
                    "ORDER BY cp.rating DESC";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            String searchPattern = "%" + keyword + "%";
            stmt.setString(1, searchPattern);
            stmt.setString(2, searchPattern);
            stmt.setString(3, searchPattern);
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                classes.add(mapResultSetToClass(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error searching classes: " + e.getMessage());
            e.printStackTrace();
        }
        return classes;
    }
    
    /**
     * Update class information
     */
    public boolean update(ClassEntity classEntity) {
        String sql = "UPDATE classes SET title = ?, description = ?, category = ?, " +
                    "price = ?, duration = ?, max_participants = ? WHERE class_id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, classEntity.getTitle());
            stmt.setString(2, classEntity.getDescription());
            stmt.setString(3, classEntity.getCategory());
            stmt.setDouble(4, classEntity.getPrice());
            stmt.setInt(5, classEntity.getDuration());
            stmt.setInt(6, classEntity.getMaxParticipants());
            stmt.setLong(7, classEntity.getClassId());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("Class updated successfully: " + classEntity.getClassId());
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("Error updating class: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Delete a class
     */
    public boolean delete(Long classId) {
        String sql = "DELETE FROM classes WHERE class_id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, classId);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("Class deleted successfully: " + classId);
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("Error deleting class: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Get popular classes (by enrollment)
     */
    public List<ClassEntity> getPopular(int limit) {
        List<ClassEntity> classes = new ArrayList<>();
        String sql = "SELECT c.*, " +
                    "  cp.company_name as provider_company, " +
                    "  cp.rating as provider_rating, " +
                    "  cp.is_verified as provider_verified, " +
                    "  u.full_name as provider_name, " +
                    "  (SELECT COUNT(*) FROM bookings WHERE class_id = c.class_id AND status != 'cancelled') as current_enrollment " +
                    "FROM classes c " +
                    "JOIN class_providers cp ON c.provider_id = cp.provider_id " +
                    "JOIN users u ON cp.user_id = u.user_id " +
                    "WHERE cp.is_verified = TRUE " +
                    "ORDER BY current_enrollment DESC " +
                    "LIMIT ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, limit);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                classes.add(mapResultSetToClass(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching popular classes: " + e.getMessage());
            e.printStackTrace();
        }
        return classes;
    }
    
    /**
     * Map ResultSet to ClassEntity object
     */
    private ClassEntity mapResultSetToClass(ResultSet rs) throws SQLException {
        ClassEntity classEntity = new ClassEntity();
        classEntity.setClassId(rs.getLong("class_id"));
        classEntity.setProviderId(rs.getLong("provider_id"));
        classEntity.setTitle(rs.getString("title"));
        classEntity.setDescription(rs.getString("description"));
        classEntity.setCategory(rs.getString("category"));
        classEntity.setPrice(rs.getDouble("price"));
        classEntity.setDuration(rs.getInt("duration"));
        classEntity.setMaxParticipants(rs.getInt("max_participants"));
        
        // Provider details
        classEntity.setProviderName(rs.getString("provider_name"));
        classEntity.setProviderCompany(rs.getString("provider_company"));
        classEntity.setProviderRating(rs.getDouble("provider_rating"));
        classEntity.setProviderVerified(rs.getBoolean("provider_verified"));
        
        // Enrollment
        classEntity.setCurrentEnrollment(rs.getInt("current_enrollment"));
        
        return classEntity;
    }
}

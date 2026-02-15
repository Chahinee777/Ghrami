package opgg.ghrami.controller;

import opgg.ghrami.model.ClassProvider;
import opgg.ghrami.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for managing class providers (instructors)
 */
public class ClassProviderController {
    private static ClassProviderController instance;
    
    private ClassProviderController() {
    }
    
    public static ClassProviderController getInstance() {
        if (instance == null) {
            instance = new ClassProviderController();
        }
        return instance;
    }
    
    /**
     * Create a new class provider
     */
    public boolean create(ClassProvider provider) {
        String sql = "INSERT INTO class_providers (user_id, company_name, expertise, rating, is_verified) " +
                    "VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setLong(1, provider.getUserId());
            stmt.setString(2, provider.getCompanyName());
            stmt.setString(3, provider.getExpertise());
            stmt.setDouble(4, provider.getRating());
            stmt.setBoolean(5, provider.isVerified());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        provider.setProviderId(generatedKeys.getLong(1));
                    }
                }
                System.out.println("Class provider created successfully: " + provider.getProviderId());
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("Error creating class provider: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Get provider by ID with full user details
     */
    public ClassProvider getById(Long providerId) {
        String sql = "SELECT cp.*, u.username, u.full_name, u.email, u.profile_picture, u.bio " +
                    "FROM class_providers cp " +
                    "JOIN users u ON cp.user_id = u.user_id " +
                    "WHERE cp.provider_id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, providerId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToProvider(rs);
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching class provider: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Get provider by user ID
     */
    public ClassProvider getByUserId(Long userId) {
        String sql = "SELECT cp.*, u.username, u.full_name, u.email, u.profile_picture, u.bio " +
                    "FROM class_providers cp " +
                    "JOIN users u ON cp.user_id = u.user_id " +
                    "WHERE cp.user_id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToProvider(rs);
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching class provider by user ID: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * Get all verified providers
     */
    public List<ClassProvider> getAllVerified() {
        List<ClassProvider> providers = new ArrayList<>();
        String sql = "SELECT cp.*, u.username, u.full_name, u.email, u.profile_picture, u.bio " +
                    "FROM class_providers cp " +
                    "JOIN users u ON cp.user_id = u.user_id " +
                    "WHERE cp.is_verified = TRUE " +
                    "ORDER BY cp.rating DESC";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                providers.add(mapResultSetToProvider(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching verified providers: " + e.getMessage());
            e.printStackTrace();
        }
        return providers;
    }
    
    /**
     * Get all providers (verified and pending)
     */
    public List<ClassProvider> getAll() {
        List<ClassProvider> providers = new ArrayList<>();
        String sql = "SELECT cp.*, u.username, u.full_name, u.email, u.profile_picture, u.bio " +
                    "FROM class_providers cp " +
                    "JOIN users u ON cp.user_id = u.user_id " +
                    "ORDER BY cp.provider_id DESC";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                providers.add(mapResultSetToProvider(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching all providers: " + e.getMessage());
            e.printStackTrace();
        }
        return providers;
    }
    
    /**
     * Get all pending verification providers (for admin)
     */
    public List<ClassProvider> getPendingVerification() {
        List<ClassProvider> providers = new ArrayList<>();
        String sql = "SELECT cp.*, u.username, u.full_name, u.email, u.profile_picture, u.bio " +
                    "FROM class_providers cp " +
                    "JOIN users u ON cp.user_id = u.user_id " +
                    "WHERE cp.is_verified = FALSE " +
                    "ORDER BY cp.provider_id DESC";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                providers.add(mapResultSetToProvider(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Error fetching pending providers: " + e.getMessage());
            e.printStackTrace();
        }
        return providers;
    }
    
    /**
     * Update provider information
     */
    public boolean update(ClassProvider provider) {
        String sql = "UPDATE class_providers SET company_name = ?, expertise = ?, " +
                    "rating = ?, is_verified = ? WHERE provider_id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, provider.getCompanyName());
            stmt.setString(2, provider.getExpertise());
            stmt.setDouble(3, provider.getRating());
            stmt.setBoolean(4, provider.isVerified());
            stmt.setLong(5, provider.getProviderId());
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("Class provider updated successfully: " + provider.getProviderId());
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("Error updating class provider: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Verify a provider (admin action)
     */
    public boolean verifyProvider(Long providerId, boolean verified) {
        String sql = "UPDATE class_providers SET is_verified = ? WHERE provider_id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setBoolean(1, verified);
            stmt.setLong(2, providerId);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("Provider verification status updated: " + providerId);
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("Error verifying provider: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Update provider rating
     */
    public boolean updateRating(Long providerId, double newRating) {
        String sql = "UPDATE class_providers SET rating = ? WHERE provider_id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setDouble(1, newRating);
            stmt.setLong(2, providerId);
            
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Error updating provider rating: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Delete a provider
     */
    public boolean delete(Long providerId) {
        String sql = "DELETE FROM class_providers WHERE provider_id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, providerId);
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("Class provider deleted successfully: " + providerId);
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("Error deleting class provider: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Check if user is already a provider
     */
    public boolean isUserProvider(Long userId) {
        String sql = "SELECT COUNT(*) FROM class_providers WHERE user_id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
            
        } catch (SQLException e) {
            System.err.println("Error checking provider status: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Check if user is a verified provider
     */
    public boolean isUserVerifiedProvider(Long userId) {
        String sql = "SELECT is_verified FROM class_providers WHERE user_id = ?";
        
        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return rs.getBoolean("is_verified");
            }
            
        } catch (SQLException e) {
            System.err.println("Error checking verified provider status: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    /**
     * Map ResultSet to ClassProvider object
     */
    private ClassProvider mapResultSetToProvider(ResultSet rs) throws SQLException {
        ClassProvider provider = new ClassProvider();
        provider.setProviderId(rs.getLong("provider_id"));
        provider.setUserId(rs.getLong("user_id"));
        provider.setCompanyName(rs.getString("company_name"));
        provider.setExpertise(rs.getString("expertise"));
        provider.setRating(rs.getDouble("rating"));
        provider.setVerified(rs.getBoolean("is_verified"));
        
        // User details
        provider.setUsername(rs.getString("username"));
        provider.setFullName(rs.getString("full_name"));
        provider.setEmail(rs.getString("email"));
        provider.setProfilePicture(rs.getString("profile_picture"));
        provider.setBio(rs.getString("bio"));
        
        return provider;
    }
}

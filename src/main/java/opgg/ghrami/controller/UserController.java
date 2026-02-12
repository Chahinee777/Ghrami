package opgg.ghrami.controller;

import opgg.ghrami.model.User;
import opgg.ghrami.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class UserController {
    private Connection connection;

    public UserController() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    public User create(User user) {
        // Special handling for admin user with ID=0
        if (user.getUserId() != null && user.getUserId() == 0) {
            return createAdminUser(user);
        }
        
        String sql = "INSERT INTO users (username, full_name, email, password, profile_picture, bio, location, is_online, created_at, last_login) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getFullName());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getPassword());
            stmt.setString(5, user.getProfilePicture());
            stmt.setString(6, user.getBio());
            stmt.setString(7, user.getLocation());
            stmt.setBoolean(8, user.isOnline());
            stmt.setTimestamp(9, Timestamp.valueOf(user.getCreatedAt()));
            stmt.setTimestamp(10, user.getLastLogin() != null ? Timestamp.valueOf(user.getLastLogin()) : null);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        user.setUserId(generatedKeys.getLong(1));
                        System.out.println("User created: " + user.getUsername() + " (ID: " + user.getUserId() + ")");
                        return user;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating user: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
    
    private User createAdminUser(User user) {
        try {
            // First, check if a user with this username or email already exists
            String checkSql = "SELECT user_id FROM users WHERE username = ? OR email = ?";
            try (PreparedStatement checkStmt = connection.prepareStatement(checkSql)) {
                checkStmt.setString(1, user.getUsername());
                checkStmt.setString(2, user.getEmail());
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next()) {
                        long existingId = rs.getLong("user_id");
                        System.err.println("Cannot create admin: User already exists with ID=" + existingId);
                        System.err.println("   Username or email 'chahine' / 'chahine@ghrami.tn' is already taken");
                        System.err.println("   Please delete this user or use a different admin username/email");
                        return null;
                    }
                }
            }
            
            // Enable NO_AUTO_VALUE_ON_ZERO mode to allow inserting 0 into AUTO_INCREMENT column
            connection.createStatement().execute("SET SESSION sql_mode = 'NO_AUTO_VALUE_ON_ZERO'");
            
            String sql = "INSERT INTO users (user_id, username, full_name, email, password, profile_picture, bio, location, is_online, created_at, last_login) " +
                    "VALUES (0, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

            PreparedStatement stmt = connection.prepareStatement(sql);
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getFullName());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getPassword());
            stmt.setString(5, user.getProfilePicture());
            stmt.setString(6, user.getBio());
            stmt.setString(7, user.getLocation());
            stmt.setBoolean(8, user.isOnline());
            stmt.setTimestamp(9, Timestamp.valueOf(user.getCreatedAt()));
            stmt.setTimestamp(10, user.getLastLogin() != null ? Timestamp.valueOf(user.getLastLogin()) : null);

            int affectedRows = stmt.executeUpdate();
            stmt.close();
            
            // Reset sql_mode
            connection.createStatement().execute("SET SESSION sql_mode = DEFAULT");

            if (affectedRows > 0) {
                System.out.println("Admin user SQL executed successfully (affected rows: " + affectedRows + ")");
                
                // Verify the user was actually inserted
                User verifyAdmin = findById(0);
                if (verifyAdmin != null) {
                    System.out.println("VERIFIED: Admin exists in database!");
                    System.out.println("   ID: " + verifyAdmin.getUserId());
                    System.out.println("   Username: " + verifyAdmin.getUsername());
                    System.out.println("   Email: " + verifyAdmin.getEmail());
                    return verifyAdmin;
                } else {
                    System.err.println("FAILED: Admin was inserted but not found at ID=0");
                    System.err.println("   MySQL may have auto-assigned a different ID");
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating admin user: " + e.getMessage());
            System.err.println("   SQL State: " + e.getSQLState());
            System.err.println("   Error Code: " + e.getErrorCode());
            e.printStackTrace();
        }
        return null;
    }

    public Optional<User> findById(Long userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding user by ID: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }
    
    public User findById(int userId) {
        Optional<User> userOpt = findById((long) userId);
        return userOpt.orElse(null);
    }

    public List<User> findAll() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY created_at DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding all users: " + e.getMessage());
            e.printStackTrace();
        }
        return users;
    }

    public User update(User user) {
        String sql = "UPDATE users SET username = ?, full_name = ?, email = ?, password = ?, profile_picture = ?, " +
                "bio = ?, location = ?, is_online = ?, last_login = ? WHERE user_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getFullName());
            stmt.setString(3, user.getEmail());
            stmt.setString(4, user.getPassword());
            stmt.setString(5, user.getProfilePicture());
            stmt.setString(6, user.getBio());
            stmt.setString(7, user.getLocation());
            stmt.setBoolean(8, user.isOnline());
            stmt.setTimestamp(9, user.getLastLogin() != null ? Timestamp.valueOf(user.getLastLogin()) : null);
            stmt.setLong(10, user.getUserId());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("User updated: " + user.getUsername());
                return user;
            }
        } catch (SQLException e) {
            System.err.println("Error updating user: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public boolean delete(Long userId) {
        String sql = "DELETE FROM users WHERE user_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("User deleted: " + userId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error deleting user: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToUser(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error finding user by email: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setUserId(rs.getLong("user_id"));
        user.setUsername(rs.getString("username"));
        user.setFullName(rs.getString("full_name"));
        user.setEmail(rs.getString("email"));
        user.setPassword(rs.getString("password"));
        user.setProfilePicture(rs.getString("profile_picture"));
        user.setBio(rs.getString("bio"));
        user.setLocation(rs.getString("location"));
        user.setOnline(rs.getBoolean("is_online"));

        Timestamp createdAt = rs.getTimestamp("created_at");
        if (createdAt != null) {
            user.setCreatedAt(createdAt.toLocalDateTime());
        }

        Timestamp lastLogin = rs.getTimestamp("last_login");
        if (lastLogin != null) {
            user.setLastLogin(lastLogin.toLocalDateTime());
        }

        return user;
    }
}

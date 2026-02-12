package opgg.ghrami.controller;

import opgg.ghrami.model.Friendship;
import opgg.ghrami.model.FriendshipStatus;
import opgg.ghrami.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class FriendshipController {
    private Connection connection;

    public FriendshipController() {
        this.connection = DatabaseConnection.getInstance().getConnection();
    }

    public Friendship create(Friendship friendship) {
        // Validate: users cannot be friends with themselves
        if (friendship.getUser1Id().equals(friendship.getUser2Id())) {
            System.err.println("Cannot create friendship: user cannot be friend with themselves");
            return null;
        }
        
        // Check if friendship already exists
        if (friendshipExists(friendship.getUser1Id(), friendship.getUser2Id())) {
            System.err.println("Friendship already exists between users " + friendship.getUser1Id() + " and " + friendship.getUser2Id());
            return null;
        }
        
        String sql = "INSERT INTO friendships (user1_id, user2_id, status, created_date) VALUES (?, ?, ?, ?)";

        try (PreparedStatement stmt = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, friendship.getUser1Id());
            stmt.setLong(2, friendship.getUser2Id());
            stmt.setString(3, friendship.getStatus().name());
            stmt.setTimestamp(4, Timestamp.valueOf(friendship.getCreatedDate()));

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        friendship.setFriendshipId(generatedKeys.getLong(1));
                        System.out.println("Friendship request created: " + friendship.getFriendshipId());
                        return friendship;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating friendship: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public Optional<Friendship> findById(Long friendshipId) {
        String sql = "SELECT * FROM friendships WHERE friendship_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, friendshipId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToFriendship(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding friendship by ID: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public List<Friendship> findAll() {
        List<Friendship> friendships = new ArrayList<>();
        String sql = "SELECT * FROM friendships ORDER BY created_date DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                friendships.add(mapResultSetToFriendship(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding all friendships: " + e.getMessage());
            e.printStackTrace();
        }
        return friendships;
    }

    public Friendship update(Friendship friendship) {
        String sql = "UPDATE friendships SET user1_id = ?, user2_id = ?, status = ?, accepted_date = ? WHERE friendship_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, friendship.getUser1Id());
            stmt.setLong(2, friendship.getUser2Id());
            stmt.setString(3, friendship.getStatus().name());
            stmt.setTimestamp(4, friendship.getAcceptedDate() != null ? Timestamp.valueOf(friendship.getAcceptedDate()) : null);
            stmt.setLong(5, friendship.getFriendshipId());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("Friendship updated: " + friendship.getFriendshipId());
                return friendship;
            }
        } catch (SQLException e) {
            System.err.println("Error updating friendship: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public boolean delete(Long friendshipId) {
        String sql = "DELETE FROM friendships WHERE friendship_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, friendshipId);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("Friendship deleted: " + friendshipId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error deleting friendship: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    private boolean friendshipExists(Long user1Id, Long user2Id) {
        String sql = "SELECT COUNT(*) FROM friendships WHERE " +
                     "(user1_id = ? AND user2_id = ?) OR (user1_id = ? AND user2_id = ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, user1Id);
            stmt.setLong(2, user2Id);
            stmt.setLong(3, user2Id);
            stmt.setLong(4, user1Id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    // Get all friendships for a specific user (both as user1 and user2)
    public List<Friendship> findByUserId(Long userId) {
        List<Friendship> friendships = new ArrayList<>();
        String sql = "SELECT * FROM friendships WHERE user1_id = ? OR user2_id = ? ORDER BY created_date DESC";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, userId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                friendships.add(mapResultSetToFriendship(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding friendships by user ID: " + e.getMessage());
            e.printStackTrace();
        }
        return friendships;
    }
    
    // Get pending friend requests for a specific user (where they are user2)
    public List<Friendship> getPendingRequestsForUser(Long userId) {
        List<Friendship> friendships = new ArrayList<>();
        String sql = "SELECT * FROM friendships WHERE user2_id = ? AND status = 'PENDING' ORDER BY created_date DESC";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                friendships.add(mapResultSetToFriendship(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding pending requests: " + e.getMessage());
            e.printStackTrace();
        }
        return friendships;
    }
    
    // Get accepted friendships for a user
    public List<Friendship> getAcceptedFriendships(Long userId) {
        List<Friendship> friendships = new ArrayList<>();
        String sql = "SELECT * FROM friendships WHERE (user1_id = ? OR user2_id = ?) AND status = 'ACCEPTED' ORDER BY accepted_date DESC";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            stmt.setLong(2, userId);
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                friendships.add(mapResultSetToFriendship(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding accepted friendships: " + e.getMessage());
            e.printStackTrace();
        }
        return friendships;
    }
    
    // Accept a friend request
    public boolean acceptFriendRequest(Long friendshipId) {
        String sql = "UPDATE friendships SET status = 'ACCEPTED', accepted_date = ? WHERE friendship_id = ? AND status = 'PENDING'";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setTimestamp(1, Timestamp.valueOf(java.time.LocalDateTime.now()));
            stmt.setLong(2, friendshipId);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("Friend request accepted: " + friendshipId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error accepting friend request: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    // Reject a friend request
    public boolean rejectFriendRequest(Long friendshipId) {
        String sql = "UPDATE friendships SET status = 'REJECTED' WHERE friendship_id = ? AND status = 'PENDING'";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, friendshipId);
            
            int affectedRows = stmt.executeUpdate();
            if (affectedRows > 0) {
                System.out.println("Friend request rejected: " + friendshipId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error rejecting friend request: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }
    
    // Check friendship status between two users
    public Optional<Friendship> getFriendshipBetweenUsers(Long user1Id, Long user2Id) {
        String sql = "SELECT * FROM friendships WHERE " +
                     "(user1_id = ? AND user2_id = ?) OR (user1_id = ? AND user2_id = ?)";
        
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setLong(1, user1Id);
            stmt.setLong(2, user2Id);
            stmt.setLong(3, user2Id);
            stmt.setLong(4, user1Id);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return Optional.of(mapResultSetToFriendship(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error checking friendship status: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }
    
    // Send friend request (user1 sends to user2)
    public Friendship sendFriendRequest(Long fromUserId, Long toUserId) {
        Friendship friendship = new Friendship(fromUserId, toUserId);
        friendship.setStatus(FriendshipStatus.PENDING);
        return create(friendship);
    }
    
    private Friendship mapResultSetToFriendship(ResultSet rs) throws SQLException {
        Friendship friendship = new Friendship();
        friendship.setFriendshipId(rs.getLong("friendship_id"));
        friendship.setUser1Id(rs.getLong("user1_id"));
        friendship.setUser2Id(rs.getLong("user2_id"));
        friendship.setStatus(FriendshipStatus.valueOf(rs.getString("status")));

        Timestamp createdDate = rs.getTimestamp("created_date");
        if (createdDate != null) {
            friendship.setCreatedDate(createdDate.toLocalDateTime());
        }

        Timestamp acceptedDate = rs.getTimestamp("accepted_date");
        if (acceptedDate != null) {
            friendship.setAcceptedDate(acceptedDate.toLocalDateTime());
        }

        return friendship;
    }
}

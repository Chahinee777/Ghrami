package opgg.ghrami.controller;

import opgg.ghrami.model.Hobby;
import opgg.ghrami.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HobbyController {

    public Hobby create(Hobby hobby) {
        String sql = "INSERT INTO hobbies (user_id, name, category, description) VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setLong(1, hobby.getUserId());
            stmt.setString(2, hobby.getName());
            stmt.setString(3, hobby.getCategory());
            stmt.setString(4, hobby.getDescription());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        hobby.setHobbyId(generatedKeys.getLong(1));
                        System.out.println("Hobby created: " + hobby.getName() + " (ID: " + hobby.getHobbyId() + ")");
                        return hobby;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Error creating hobby: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public Optional<Hobby> findById(Long hobbyId) {
        String sql = "SELECT * FROM hobbies WHERE hobby_id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, hobbyId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToHobby(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding hobby by ID: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public List<Hobby> findAll() {
        List<Hobby> hobbies = new ArrayList<>();
        String sql = "SELECT * FROM hobbies ORDER BY name ASC";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                hobbies.add(mapResultSetToHobby(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding all hobbies: " + e.getMessage());
            e.printStackTrace();
        }
        return hobbies;
    }

    public List<Hobby> findByUserId(Long userId) {
        List<Hobby> hobbies = new ArrayList<>();
        String sql = "SELECT * FROM hobbies WHERE user_id = ? ORDER BY name ASC";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                hobbies.add(mapResultSetToHobby(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding hobbies by user ID: " + e.getMessage());
            e.printStackTrace();
        }
        return hobbies;
    }

    public List<Hobby> findByCategory(String category) {
        List<Hobby> hobbies = new ArrayList<>();
        String sql = "SELECT * FROM hobbies WHERE category = ? ORDER BY name ASC";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, category);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                hobbies.add(mapResultSetToHobby(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding hobbies by category: " + e.getMessage());
            e.printStackTrace();
        }
        return hobbies;
    }

    public int countByUserId(Long userId) {
        String sql = "SELECT COUNT(*) FROM hobbies WHERE user_id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting hobbies: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    public Hobby update(Hobby hobby) {
        String sql = "UPDATE hobbies SET name = ?, category = ?, description = ? WHERE hobby_id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, hobby.getName());
            stmt.setString(2, hobby.getCategory());
            stmt.setString(3, hobby.getDescription());
            stmt.setLong(4, hobby.getHobbyId());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("Hobby updated: " + hobby.getName());
                return hobby;
            }
        } catch (SQLException e) {
            System.err.println("Error updating hobby: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public boolean delete(Long hobbyId) {
        String sql = "DELETE FROM hobbies WHERE hobby_id = ?";

        try (Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setLong(1, hobbyId);

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("Hobby deleted: " + hobbyId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error deleting hobby: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    private Hobby mapResultSetToHobby(ResultSet rs) throws SQLException {
        Hobby hobby = new Hobby();
        hobby.setHobbyId(rs.getLong("hobby_id"));
        hobby.setUserId(rs.getLong("user_id"));
        hobby.setName(rs.getString("name"));
        hobby.setCategory(rs.getString("category"));
        hobby.setDescription(rs.getString("description"));
        return hobby;
    }
}

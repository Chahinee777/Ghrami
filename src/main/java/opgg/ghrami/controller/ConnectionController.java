package opgg.ghrami.controller;

import opgg.ghrami.model.Connection;
import opgg.ghrami.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ConnectionController {
    private static ConnectionController instance;

    private ConnectionController() {}

    public static synchronized ConnectionController getInstance() {
        if (instance == null) {
            instance = new ConnectionController();
        }
        return instance;
    }

    public Connection create(Connection connection) {
        String sql = "INSERT INTO connections (connection_id, initiator_id, receiver_id, connection_type, receiver_skill, initiator_skill, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (java.sql.Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            String connectionId = UUID.randomUUID().toString();
            connection.setConnectionId(connectionId);

            stmt.setString(1, connectionId);
            stmt.setLong(2, connection.getInitiatorId());
            stmt.setLong(3, connection.getReceiverId());
            stmt.setString(4, connection.getConnectionType());
            stmt.setString(5, connection.getReceiverSkill());
            stmt.setString(6, connection.getInitiatorSkill());
            stmt.setString(7, connection.getStatus());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("Connection created: " + connectionId);
                return connection;
            }
        } catch (SQLException e) {
            System.err.println("Error creating connection: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public Optional<Connection> findById(String connectionId) {
        String sql = "SELECT * FROM connections WHERE connection_id = ?";

        try (java.sql.Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, connectionId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToConnection(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding connection by ID: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public List<Connection> findAll() {
        List<Connection> connections = new ArrayList<>();
        String sql = "SELECT * FROM connections ORDER BY connection_id";

        try (java.sql.Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                connections.add(mapResultSetToConnection(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding all connections: " + e.getMessage());
            e.printStackTrace();
        }
        return connections;
    }

    public List<Connection> findByInitiator(Long userId) {
        List<Connection> connections = new ArrayList<>();
        String sql = "SELECT * FROM connections WHERE initiator_id = ? ORDER BY connection_id";

        try (java.sql.Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                connections.add(mapResultSetToConnection(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding connections by initiator: " + e.getMessage());
            e.printStackTrace();
        }
        return connections;
    }

    public List<Connection> findByReceiver(Long userId) {
        List<Connection> connections = new ArrayList<>();
        String sql = "SELECT * FROM connections WHERE receiver_id = ? ORDER BY connection_id";

        try (java.sql.Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                connections.add(mapResultSetToConnection(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding connections by receiver: " + e.getMessage());
            e.printStackTrace();
        }
        return connections;
    }

    public List<Connection> findByUser(Long userId) {
        List<Connection> connections = new ArrayList<>();
        String sql = "SELECT * FROM connections WHERE initiator_id = ? OR receiver_id = ? ORDER BY connection_id";

        try (java.sql.Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            stmt.setLong(2, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                connections.add(mapResultSetToConnection(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding connections by user: " + e.getMessage());
            e.printStackTrace();
        }
        return connections;
    }

    public List<Connection> findByStatus(String status) {
        List<Connection> connections = new ArrayList<>();
        String sql = "SELECT * FROM connections WHERE status = ? ORDER BY connection_id";

        try (java.sql.Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                connections.add(mapResultSetToConnection(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding connections by status: " + e.getMessage());
            e.printStackTrace();
        }
        return connections;
    }

    public List<Connection> findPendingForUser(Long userId) {
        List<Connection> connections = new ArrayList<>();
        String sql = "SELECT * FROM connections WHERE receiver_id = ? AND status = 'pending' ORDER BY connection_id";

        try (java.sql.Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                connections.add(mapResultSetToConnection(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding pending connections: " + e.getMessage());
            e.printStackTrace();
        }
        return connections;
    }

    public Connection update(Connection connection) {
        String sql = "UPDATE connections SET initiator_id = ?, receiver_id = ?, connection_type = ?, " +
                     "receiver_skill = ?, initiator_skill = ?, status = ? WHERE connection_id = ?";

        try (java.sql.Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, connection.getInitiatorId());
            stmt.setLong(2, connection.getReceiverId());
            stmt.setString(3, connection.getConnectionType());
            stmt.setString(4, connection.getReceiverSkill());
            stmt.setString(5, connection.getInitiatorSkill());
            stmt.setString(6, connection.getStatus());
            stmt.setString(7, connection.getConnectionId());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("Connection updated: " + connection.getConnectionId());
                return connection;
            }
        } catch (SQLException e) {
            System.err.println("Error updating connection: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public boolean acceptConnection(String connectionId) {
        String sql = "UPDATE connections SET status = 'accepted' WHERE connection_id = ? AND status = 'pending'";

        try (java.sql.Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, connectionId);
            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("Connection accepted: " + connectionId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error accepting connection: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean rejectConnection(String connectionId) {
        String sql = "UPDATE connections SET status = 'rejected' WHERE connection_id = ? AND status = 'pending'";

        try (java.sql.Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, connectionId);
            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("Connection rejected: " + connectionId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error rejecting connection: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean delete(String connectionId) {
        String sql = "DELETE FROM connections WHERE connection_id = ?";

        try (java.sql.Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, connectionId);
            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("Connection deleted: " + connectionId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error deleting connection: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    private Connection mapResultSetToConnection(ResultSet rs) throws SQLException {
        Connection connection = new Connection();
        connection.setConnectionId(rs.getString("connection_id"));
        connection.setInitiatorId(rs.getLong("initiator_id"));
        connection.setReceiverId(rs.getLong("receiver_id"));
        connection.setConnectionType(rs.getString("connection_type"));
        connection.setReceiverSkill(rs.getString("receiver_skill"));
        connection.setInitiatorSkill(rs.getString("initiator_skill"));
        connection.setStatus(rs.getString("status"));
        return connection;
    }
}

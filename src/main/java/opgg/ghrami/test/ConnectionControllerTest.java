package opgg.ghrami.test;

import opgg.ghrami.controller.ConnectionController;
import opgg.ghrami.controller.UserController;
import opgg.ghrami.model.Connection;
import opgg.ghrami.model.User;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ConnectionControllerTest {
    
    private ConnectionController connectionController;
    private UserController userController;
    private User testUser1;
    private User testUser2;
    private User testUser3;
    private Connection testConnection;
    private String testConnectionId;

    @BeforeAll
    public void setUp() {
        connectionController = ConnectionController.getInstance();
        userController = new UserController();
        System.out.println("ConnectionController tests initialized");
        
        // Create test user 1
        testUser1 = new User();
        testUser1.setUsername("connect_user1_" + System.currentTimeMillis());
        testUser1.setFullName("Connection Test User 1");
        testUser1.setEmail("connuser1_" + System.currentTimeMillis() + "@test.com");
        testUser1.setPassword("hashedPass123");
        testUser1.setBio("Expert in Photography");
        testUser1.setLocation("Tunis");
        testUser1.setProfilePicture("");
        testUser1.setOnline(true);
        testUser1.setCreatedAt(LocalDateTime.now());
        
        testUser1 = userController.create(testUser1);
        assertNotNull(testUser1.getUserId(), "Test user 1 should be created");
        
        // Create test user 2
        testUser2 = new User();
        testUser2.setUsername("connect_user2_" + System.currentTimeMillis());
        testUser2.setFullName("Connection Test User 2");
        testUser2.setEmail("connuser2_" + System.currentTimeMillis() + "@test.com");
        testUser2.setPassword("hashedPass456");
        testUser2.setBio("Beginner in Photography");
        testUser2.setLocation("Tunis");
        testUser2.setProfilePicture("");
        testUser2.setOnline(true);
        testUser2.setCreatedAt(LocalDateTime.now());
        
        testUser2 = userController.create(testUser2);
        assertNotNull(testUser2.getUserId(), "Test user 2 should be created");
        
        // Create test user 3 for additional tests
        testUser3 = new User();
        testUser3.setUsername("connect_user3_" + System.currentTimeMillis());
        testUser3.setFullName("Connection Test User 3");
        testUser3.setEmail("connuser3_" + System.currentTimeMillis() + "@test.com");
        testUser3.setPassword("hashedPass789");
        testUser3.setBio("Intermediate in Photography");
        testUser3.setLocation("Sfax");
        testUser3.setProfilePicture("");
        testUser3.setOnline(false);
        testUser3.setCreatedAt(LocalDateTime.now());
        
        testUser3 = userController.create(testUser3);
        assertNotNull(testUser3.getUserId(), "Test user 3 should be created");
    }

    @Test
    @Order(1)
    public void testCreateConnection() {
        testConnection = new Connection();
        testConnection.setInitiatorId(testUser1.getUserId());
        testConnection.setReceiverId(testUser2.getUserId());
        testConnection.setConnectionType("skill");
        testConnection.setInitiatorSkill("Photography");
        testConnection.setReceiverSkill("Photo Editing");
        testConnection.setStatus("pending");

        Connection created = connectionController.create(testConnection);
        
        assertNotNull(created, "Connection should be created successfully");
        assertNotNull(created.getConnectionId(), "Connection ID should be generated");
        assertEquals("pending", created.getStatus());
        
        testConnectionId = created.getConnectionId();
        System.out.println("Created connection with ID: " + testConnectionId);
    }

    @Test
    @Order(2)
    public void testFindConnectionById() {
        assertNotNull(testConnectionId, "Test connection ID should be set");
        
        Optional<Connection> foundConnection = connectionController.findById(testConnectionId);
        
        assertTrue(foundConnection.isPresent(), "Connection should be found");
        assertEquals(testUser1.getUserId(), foundConnection.get().getInitiatorId());
        assertEquals(testUser2.getUserId(), foundConnection.get().getReceiverId());
        assertEquals("skill", foundConnection.get().getConnectionType());
        assertEquals("pending", foundConnection.get().getStatus());
    }

    @Test
    @Order(3)
    public void testFindByInitiator() {
        List<Connection> connections = connectionController.findByInitiator(testUser1.getUserId());
        
        assertNotNull(connections, "Connections list should not be null");
        assertFalse(connections.isEmpty(), "User 1 should have initiated connections");
        assertTrue(connections.stream()
            .anyMatch(c -> c.getConnectionId().equals(testConnectionId)));
    }

    @Test
    @Order(4)
    public void testFindByReceiver() {
        List<Connection> connections = connectionController.findByReceiver(testUser2.getUserId());
        
        assertNotNull(connections, "Connections list should not be null");
        assertFalse(connections.isEmpty(), "User 2 should have received connections");
        assertTrue(connections.stream()
            .anyMatch(c -> c.getConnectionId().equals(testConnectionId)));
    }

    @Test
    @Order(5)
    public void testFindByUser() {
        List<Connection> user1Connections = connectionController.findByUser(testUser1.getUserId());
        List<Connection> user2Connections = connectionController.findByUser(testUser2.getUserId());
        
        assertNotNull(user1Connections, "User 1 connections should not be null");
        assertNotNull(user2Connections, "User 2 connections should not be null");
        
        assertTrue(user1Connections.stream()
            .anyMatch(c -> c.getConnectionId().equals(testConnectionId)));
        assertTrue(user2Connections.stream()
            .anyMatch(c -> c.getConnectionId().equals(testConnectionId)));
    }

    @Test
    @Order(6)
    public void testFindPendingForUser() {
        List<Connection> pendingConnections = connectionController.findPendingForUser(testUser2.getUserId());
        
        assertNotNull(pendingConnections, "Pending connections list should not be null");
        assertFalse(pendingConnections.isEmpty(), "User 2 should have pending requests");
        assertTrue(pendingConnections.stream()
            .anyMatch(c -> c.getConnectionId().equals(testConnectionId) && 
                          "pending".equals(c.getStatus())));
    }

    @Test
    @Order(7)
    public void testAcceptConnection() {
        assertNotNull(testConnectionId, "Test connection ID should be set");
        
        boolean accepted = connectionController.acceptConnection(testConnectionId);
        
        assertTrue(accepted, "Connection should be accepted successfully");
        
        Optional<Connection> updatedConnection = connectionController.findById(testConnectionId);
        assertTrue(updatedConnection.isPresent(), "Connection should exist");
        assertEquals("accepted", updatedConnection.get().getStatus(), 
                    "Connection status should be 'accepted'");
    }

    @Test
    @Order(8)
    public void testFindByStatus() {
        List<Connection> acceptedConnections = connectionController.findByStatus("accepted");
        
        assertNotNull(acceptedConnections, "Accepted connections list should not be null");
        assertTrue(acceptedConnections.stream()
            .anyMatch(c -> c.getConnectionId().equals(testConnectionId)));
    }

    @Test
    @Order(9)
    public void testUpdateConnection() {
        Optional<Connection> connectionOpt = connectionController.findById(testConnectionId);
        assertTrue(connectionOpt.isPresent(), "Connection should exist");
        
        Connection connection = connectionOpt.get();
        connection.setReceiverSkill("Advanced Photo Editing");
        
        Connection updated = connectionController.update(connection);
        
        assertNotNull(updated, "Connection should be updated");
        assertEquals("Advanced Photo Editing", updated.getReceiverSkill());
    }

    @Test
    @Order(10)
    public void testCreateActivityConnection() {
        Connection activityConnection = new Connection();
        activityConnection.setInitiatorId(testUser2.getUserId());
        activityConnection.setReceiverId(testUser3.getUserId());
        activityConnection.setConnectionType("activity");
        activityConnection.setStatus("pending");

        Connection created = connectionController.create(activityConnection);
        
        assertNotNull(created, "Activity connection should be created");
        assertEquals("activity", created.getConnectionType());
        
        // Reject this connection for testing
        boolean rejected = connectionController.rejectConnection(created.getConnectionId());
        assertTrue(rejected, "Connection should be rejected successfully");
        
        Optional<Connection> rejectedConn = connectionController.findById(created.getConnectionId());
        assertTrue(rejectedConn.isPresent());
        assertEquals("rejected", rejectedConn.get().getStatus());
    }

    @Test
    @Order(11)
    public void testFindAllConnections() {
        List<Connection> allConnections = connectionController.findAll();
        
        assertNotNull(allConnections, "Connections list should not be null");
        assertFalse(allConnections.isEmpty(), "Should have at least one connection");
    }

    @Test
    @Order(12)
    public void testCreateGeneralConnection() {
        Connection generalConnection = new Connection();
        generalConnection.setInitiatorId(testUser1.getUserId());
        generalConnection.setReceiverId(testUser3.getUserId());
        generalConnection.setConnectionType("general");
        generalConnection.setStatus("pending");

        Connection created = connectionController.create(generalConnection);
        
        assertNotNull(created, "General connection should be created");
        assertEquals("general", created.getConnectionType());
        assertNull(created.getInitiatorSkill(), "General connection should not have initiator skill");
        assertNull(created.getReceiverSkill(), "General connection should not have receiver skill");
    }

    @Test
    @Order(13)
    public void testDeleteConnection() {
        // Create a connection to delete
        Connection tempConnection = new Connection();
        tempConnection.setInitiatorId(testUser1.getUserId());
        tempConnection.setReceiverId(testUser2.getUserId());
        tempConnection.setConnectionType("hobby");
        tempConnection.setStatus("pending");
        
        Connection created = connectionController.create(tempConnection);
        assertNotNull(created, "Temporary connection should be created");
        String tempId = created.getConnectionId();
        
        // Delete it
        boolean deleted = connectionController.delete(tempId);
        assertTrue(deleted, "Connection should be deleted successfully");
        
        // Verify deletion
        Optional<Connection> deletedConnection = connectionController.findById(tempId);
        assertFalse(deletedConnection.isPresent(), "Deleted connection should not be found");
    }

    @Test
    @Order(14)
    public void testValidationSkillConnection() {
        // Test that skill connections work with both skills specified
        Connection skillConn = new Connection();
        skillConn.setInitiatorId(testUser2.getUserId());
        skillConn.setReceiverId(testUser1.getUserId());
        skillConn.setConnectionType("skill");
        skillConn.setInitiatorSkill("Cooking");
        skillConn.setReceiverSkill("Baking");
        skillConn.setStatus("accepted");
        
        Connection created = connectionController.create(skillConn);
        
        assertNotNull(created, "Skill connection with both skills should be created");
        assertEquals("Cooking", created.getInitiatorSkill());
        assertEquals("Baking", created.getReceiverSkill());
    }

    @AfterAll
    public void tearDown() {
        // Clean up test data
        try {
            if (testConnectionId != null) {
                connectionController.delete(testConnectionId);
            }
            
            if (testUser1 != null && testUser1.getUserId() != null) {
                userController.delete(testUser1.getUserId());
            }
            if (testUser2 != null && testUser2.getUserId() != null) {
                userController.delete(testUser2.getUserId());
            }
            if (testUser3 != null && testUser3.getUserId() != null) {
                userController.delete(testUser3.getUserId());
            }
            
            System.out.println("ConnectionController test cleanup completed");
        } catch (Exception e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }
}

package opgg.ghrami.test;

import opgg.ghrami.controller.UserController;
import opgg.ghrami.model.User;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class UserControllerTest {
    
    private UserController userController;
    private User testUser;
    private Long testUserId;

    @BeforeAll
    public void setUp() {
        userController = new UserController();
        System.out.println("UserController tests initialized");
    }

    @Test
    @Order(1)
    public void testCreateUser() {
        testUser = new User();
        testUser.setUsername("testuser_" + System.currentTimeMillis());
        testUser.setFullName("Test User");
        testUser.setEmail("testuser" + System.currentTimeMillis() + "@test.com");
        testUser.setPassword("hashedPassword123");
        testUser.setBio("Test bio");
        testUser.setLocation("Test Location");
        testUser.setProfilePicture("");
        testUser.setOnline(false);
        testUser.setCreatedAt(LocalDateTime.now());

        User createdUser = userController.create(testUser);
        
        assertNotNull(createdUser, "Created user should not be null");
        assertNotNull(createdUser.getUserId(), "User ID should be generated");
        assertEquals(testUser.getUsername(), createdUser.getUsername());
        
        testUserId = createdUser.getUserId();
        testUser = createdUser;
    }

    @Test
    @Order(2)
    public void testFindUserById() {
        assertNotNull(testUserId, "Test user ID should be set");
        
        Optional<User> foundUser = userController.findById(testUserId);
        
        assertTrue(foundUser.isPresent(), "User should be found");
        assertEquals(testUser.getUsername(), foundUser.get().getUsername());
        assertEquals(testUser.getEmail(), foundUser.get().getEmail());
    }

    @Test
    @Order(3)
    public void testFindUserByEmail() {
        assertNotNull(testUser, "Test user should be set");
        
        Optional<User> foundUser = userController.findByEmail(testUser.getEmail());
        
        assertTrue(foundUser.isPresent(), "User should be found by email");
        assertEquals(testUser.getUserId(), foundUser.get().getUserId());
    }

    @Test
    @Order(4)
    public void testFindAllUsers() {
        List<User> users = userController.findAll();
        
        assertNotNull(users, "Users list should not be null");
        assertFalse(users.isEmpty(), "Users list should not be empty");
        assertTrue(users.stream().anyMatch(u -> u.getUserId().equals(testUserId)), 
                   "Test user should be in the list");
    }

    @Test
    @Order(5)
    public void testUpdateUser() {
        assertNotNull(testUser, "Test user should be set");
        
        testUser.setBio("Updated test bio");
        testUser.setOnline(true);
        
        User updatedUser = userController.update(testUser);
        
        assertNotNull(updatedUser, "Updated user should not be null");
        assertEquals("Updated test bio", updatedUser.getBio());
        assertTrue(updatedUser.isOnline());
    }

    @Test
    @Order(6)
    public void testDeleteUser() {
        assertNotNull(testUserId, "Test user ID should be set");
        
        boolean deleted = userController.delete(testUserId);
        
        assertTrue(deleted, "User should be deleted successfully");
        
        Optional<User> foundUser = userController.findById(testUserId);
        assertFalse(foundUser.isPresent(), "Deleted user should not be found");
    }

    @Test
    @Order(7)
    public void testCreateUserWithNullFields() {
        User invalidUser = new User();
        // Missing required fields
        
        User result = userController.create(invalidUser);
        
        // Should handle gracefully (may return null or throw exception)
        // Adjust based on your implementation
        assertNull(result, "Creating user with null fields should fail");
    }
}

package opgg.ghrami.test;

import opgg.ghrami.controller.HobbyController;
import opgg.ghrami.controller.UserController;
import opgg.ghrami.model.Hobby;
import opgg.ghrami.model.User;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class HobbyControllerTest {
    
    private HobbyController hobbyController;
    private UserController userController;
    private Hobby testHobby;
    private Long testHobbyId;
    private Long testUserId;

    @BeforeAll
    public void setUp() {
        hobbyController = new HobbyController();
        userController = new UserController();
        
        // Create a test user first
        User testUser = new User();
        testUser.setUsername("hobbytest_" + System.currentTimeMillis());
        testUser.setFullName("Hobby Test User");
        testUser.setEmail("hobbytest" + System.currentTimeMillis() + "@test.com");
        testUser.setPassword("password123");
        testUser.setBio("Test");
        testUser.setLocation("Test");
        testUser.setProfilePicture("");
        testUser.setOnline(false);
        testUser.setCreatedAt(LocalDateTime.now());
        
        User createdUser = userController.create(testUser);
        testUserId = createdUser.getUserId();
        
        System.out.println("HobbyController tests initialized");
    }

    @Test
    @Order(1)
    public void testCreateHobby() {
        testHobby = new Hobby();
        testHobby.setUserId(testUserId);
        testHobby.setName("Test Hobby");
        testHobby.setCategory("Sports");
        testHobby.setDescription("Test hobby description");

        Hobby createdHobby = hobbyController.create(testHobby);
        
        assertNotNull(createdHobby, "Created hobby should not be null");
        assertNotNull(createdHobby.getHobbyId(), "Hobby ID should be generated");
        assertEquals(testHobby.getName(), createdHobby.getName());
        assertEquals(testHobby.getCategory(), createdHobby.getCategory());
        
        testHobbyId = createdHobby.getHobbyId();
        testHobby = createdHobby;
    }

    @Test
    @Order(2)
    public void testFindHobbyById() {
        assertNotNull(testHobbyId, "Test hobby ID should be set");
        
        Optional<Hobby> foundHobby = hobbyController.findById(testHobbyId);
        
        assertTrue(foundHobby.isPresent(), "Hobby should be found");
        assertEquals(testHobby.getName(), foundHobby.get().getName());
        assertEquals(testHobby.getCategory(), foundHobby.get().getCategory());
    }

    @Test
    @Order(3)
    public void testFindHobbyByUserId() {
        assertNotNull(testUserId, "Test user ID should be set");
        
        List<Hobby> hobbies = hobbyController.findByUserId(testUserId);
        
        assertNotNull(hobbies, "Hobbies list should not be null");
        assertFalse(hobbies.isEmpty(), "User should have at least one hobby");
        assertTrue(hobbies.stream().anyMatch(h -> h.getHobbyId().equals(testHobbyId)));
    }

    @Test
    @Order(4)
    public void testFindHobbyByCategory() {
        List<Hobby> hobbies = hobbyController.findByCategory("Sports");
        
        assertNotNull(hobbies, "Hobbies list should not be null");
        assertTrue(hobbies.stream().anyMatch(h -> h.getHobbyId().equals(testHobbyId)));
    }

    @Test
    @Order(5)
    public void testCountHobbiesByUserId() {
        int count = hobbyController.countByUserId(testUserId);
        
        assertTrue(count > 0, "User should have at least one hobby");
    }

    @Test
    @Order(6)
    public void testFindAllHobbies() {
        List<Hobby> hobbies = hobbyController.findAll();
        
        assertNotNull(hobbies, "Hobbies list should not be null");
        assertFalse(hobbies.isEmpty(), "Hobbies list should not be empty");
    }

    @Test
    @Order(7)
    public void testUpdateHobby() {
        assertNotNull(testHobby, "Test hobby should be set");
        
        testHobby.setName("Updated Hobby Name");
        testHobby.setDescription("Updated description");
        
        Hobby updatedHobby = hobbyController.update(testHobby);
        
        assertNotNull(updatedHobby, "Updated hobby should not be null");
        assertEquals("Updated Hobby Name", updatedHobby.getName());
        assertEquals("Updated description", updatedHobby.getDescription());
    }

    @Test
    @Order(8)
    public void testDeleteHobby() {
        assertNotNull(testHobbyId, "Test hobby ID should be set");
        
        boolean deleted = hobbyController.delete(testHobbyId);
        
        assertTrue(deleted, "Hobby should be deleted successfully");
        
        Optional<Hobby> foundHobby = hobbyController.findById(testHobbyId);
        assertFalse(foundHobby.isPresent(), "Deleted hobby should not be found");
    }

    @AfterAll
    public void tearDown() {
        // Clean up test user
        if (testUserId != null) {
            userController.delete(testUserId);
        }
    }
}

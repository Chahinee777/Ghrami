package opgg.ghrami.test;

import opgg.ghrami.controller.HobbyController;
import opgg.ghrami.controller.ProgressController;
import opgg.ghrami.controller.UserController;
import opgg.ghrami.model.Hobby;
import opgg.ghrami.model.Progress;
import opgg.ghrami.model.User;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProgressControllerTest {
    
    private ProgressController progressController;
    private HobbyController hobbyController;
    private UserController userController;
    private Progress testProgress;
    private Long testProgressId;
    private Long testHobbyId;
    private Long testUserId;

    @BeforeAll
    public void setUp() {
        progressController = new ProgressController();
        hobbyController = new HobbyController();
        userController = new UserController();
        
        // Create test user
        User testUser = new User();
        testUser.setUsername("progresstest_" + System.currentTimeMillis());
        testUser.setFullName("Progress Test User");
        testUser.setEmail("progresstest" + System.currentTimeMillis() + "@test.com");
        testUser.setPassword("password123");
        testUser.setBio("Test");
        testUser.setLocation("Test");
        testUser.setProfilePicture("");
        testUser.setOnline(false);
        testUser.setCreatedAt(LocalDateTime.now());
        
        User createdUser = userController.create(testUser);
        testUserId = createdUser.getUserId();
        
        // Create test hobby
        Hobby testHobby = new Hobby();
        testHobby.setUserId(testUserId);
        testHobby.setName("Test Hobby for Progress");
        testHobby.setCategory("Learning");
        testHobby.setDescription("Test hobby");
        
        Hobby createdHobby = hobbyController.create(testHobby);
        testHobbyId = createdHobby.getHobbyId();
        
        System.out.println("ProgressController tests initialized");
    }

    @Test
    @Order(1)
    public void testCreateProgress() {
        testProgress = new Progress();
        testProgress.setHobbyId(testHobbyId);
        testProgress.setHoursSpent(5.5);
        testProgress.setNotes("Initial progress");

        Progress createdProgress = progressController.create(testProgress);
        
        assertNotNull(createdProgress, "Created progress should not be null");
        assertNotNull(createdProgress.getProgressId(), "Progress ID should be generated");
        assertEquals(5.5, createdProgress.getHoursSpent());
        
        testProgressId = createdProgress.getProgressId();
        testProgress = createdProgress;
    }

    @Test
    @Order(2)
    public void testFindProgressById() {
        assertNotNull(testProgressId, "Test progress ID should be set");
        
        Optional<Progress> foundProgress = progressController.findById(testProgressId);
        
        assertTrue(foundProgress.isPresent(), "Progress should be found");
        assertEquals(testProgress.getHoursSpent(), foundProgress.get().getHoursSpent());
    }

    @Test
    @Order(3)
    public void testFindProgressByHobbyId() {
        assertNotNull(testHobbyId, "Test hobby ID should be set");
        
        Optional<Progress> foundProgress = progressController.findByHobbyId(testHobbyId);
        
        assertTrue(foundProgress.isPresent(), "Progress should be found by hobby ID");
        assertEquals(testHobbyId, foundProgress.get().getHobbyId());
    }

    @Test
    @Order(4)
    public void testFindAllProgress() {
        List<Progress> progressList = progressController.findAll();
        
        assertNotNull(progressList, "Progress list should not be null");
        assertFalse(progressList.isEmpty(), "Progress list should not be empty");
    }

    @Test
    @Order(5)
    public void testUpdateProgress() {
        assertNotNull(testProgress, "Test progress should be set");
        
        testProgress.setHoursSpent(10.0);
        testProgress.setNotes("Updated progress notes");
        
        Progress updatedProgress = progressController.update(testProgress);
        
        assertNotNull(updatedProgress, "Updated progress should not be null");
        assertEquals(10.0, updatedProgress.getHoursSpent());
        assertEquals("Updated progress notes", updatedProgress.getNotes());
    }

    @Test
    @Order(6)
    public void testAddHours() {
        assertNotNull(testHobbyId, "Test hobby ID should be set");
        
        Progress result = progressController.addHours(testHobbyId, 3.5, "Added more hours");
        
        assertNotNull(result, "Add hours should return progress");
        assertEquals(13.5, result.getHoursSpent(), 0.01); // 10.0 + 3.5
    }

    @Test
    @Order(7)
    public void testDeleteProgress() {
        assertNotNull(testProgressId, "Test progress ID should be set");
        
        boolean deleted = progressController.delete(testProgressId);
        
        assertTrue(deleted, "Progress should be deleted successfully");
        
        Optional<Progress> foundProgress = progressController.findById(testProgressId);
        assertFalse(foundProgress.isPresent(), "Deleted progress should not be found");
    }

    @AfterAll
    public void tearDown() {
        // Clean up
        if (testHobbyId != null) {
            hobbyController.delete(testHobbyId);
        }
        if (testUserId != null) {
            userController.delete(testUserId);
        }
    }
}

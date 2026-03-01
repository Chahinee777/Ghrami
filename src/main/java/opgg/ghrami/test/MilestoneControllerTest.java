package opgg.ghrami.test;

import opgg.ghrami.controller.HobbyController;
import opgg.ghrami.controller.MilestoneController;
import opgg.ghrami.controller.UserController;
import opgg.ghrami.model.Hobby;
import opgg.ghrami.model.Milestone;
import opgg.ghrami.model.User;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MilestoneControllerTest {
    
    private MilestoneController milestoneController;
    private HobbyController hobbyController;
    private UserController userController;
    private Milestone testMilestone;
    private Long testMilestoneId;
    private Long testHobbyId;
    private Long testUserId;

    @BeforeAll
    public void setUp() {
        milestoneController = new MilestoneController();
        hobbyController = new HobbyController();
        userController = new UserController();
        
        // Create test user
        User testUser = new User();
        testUser.setUsername("milestonetest_" + System.currentTimeMillis());
        testUser.setFullName("Milestone Test User");
        testUser.setEmail("milestonetest" + System.currentTimeMillis() + "@test.com");
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
        testHobby.setName("Test Hobby for Milestones");
        testHobby.setCategory("Learning");
        testHobby.setDescription("Test hobby");
        
        Hobby createdHobby = hobbyController.create(testHobby);
        testHobbyId = createdHobby.getHobbyId();
        
        System.out.println("MilestoneController tests initialized");
    }

    @Test
    @Order(1)
    public void testCreateMilestone() {
        testMilestone = new Milestone();
        testMilestone.setHobbyId(testHobbyId);
        testMilestone.setTitle("Test Milestone");
        testMilestone.setTargetDate(LocalDate.now().plusDays(30));
        testMilestone.setIsAchieved(false);

        Milestone createdMilestone = milestoneController.create(testMilestone);
        
        assertNotNull(createdMilestone, "Created milestone should not be null");
        assertNotNull(createdMilestone.getMilestoneId(), "Milestone ID should be generated");
        assertEquals(testMilestone.getTitle(), createdMilestone.getTitle());
        assertFalse(createdMilestone.getIsAchieved());
        
        testMilestoneId = createdMilestone.getMilestoneId();
        testMilestone = createdMilestone;
    }

    @Test
    @Order(2)
    public void testFindMilestoneById() {
        assertNotNull(testMilestoneId, "Test milestone ID should be set");
        
        Optional<Milestone> foundMilestone = milestoneController.findById(testMilestoneId);
        
        assertTrue(foundMilestone.isPresent(), "Milestone should be found");
        assertEquals(testMilestone.getTitle(), foundMilestone.get().getTitle());
    }

    @Test
    @Order(3)
    public void testFindMilestonesByHobbyId() {
        assertNotNull(testHobbyId, "Test hobby ID should be set");
        
        List<Milestone> milestones = milestoneController.findByHobbyId(testHobbyId);
        
        assertNotNull(milestones, "Milestones list should not be null");
        assertFalse(milestones.isEmpty(), "Hobby should have at least one milestone");
        assertTrue(milestones.stream().anyMatch(m -> m.getMilestoneId().equals(testMilestoneId)));
    }

    @Test
    @Order(4)
    public void testCountMilestonesByHobbyId() {
        int count = milestoneController.countByHobbyId(testHobbyId);
        
        assertTrue(count > 0, "Hobby should have at least one milestone");
    }

    @Test
    @Order(5)
    public void testCountAchievedMilestones() {
        int achievedCount = milestoneController.countAchievedByHobbyId(testHobbyId);
        
        assertEquals(0, achievedCount, "No milestones should be achieved yet");
    }

    @Test
    @Order(6)
    public void testFindAllMilestones() {
        List<Milestone> milestones = milestoneController.findAll();
        
        assertNotNull(milestones, "Milestones list should not be null");
        assertFalse(milestones.isEmpty(), "Milestones list should not be empty");
    }

    @Test
    @Order(7)
    public void testToggleAchieved() {
        assertNotNull(testMilestoneId, "Test milestone ID should be set");
        
        boolean toggled = milestoneController.toggleAchieved(testMilestoneId);
        
        assertTrue(toggled, "Toggle should be successful");
        
        Optional<Milestone> updated = milestoneController.findById(testMilestoneId);
        assertTrue(updated.isPresent() && updated.get().getIsAchieved(), 
                   "Milestone should now be achieved");
    }

    @Test
    @Order(8)
    public void testUpdateMilestone() {
        assertNotNull(testMilestone, "Test milestone should be set");
        
        testMilestone.setTitle("Updated Milestone Title");
        testMilestone.setTargetDate(LocalDate.now().plusDays(60));
        
        Milestone updatedMilestone = milestoneController.update(testMilestone);
        
        assertNotNull(updatedMilestone, "Updated milestone should not be null");
        assertEquals("Updated Milestone Title", updatedMilestone.getTitle());
    }

    @Test
    @Order(9)
    public void testDeleteMilestone() {
        assertNotNull(testMilestoneId, "Test milestone ID should be set");
        
        boolean deleted = milestoneController.delete(testMilestoneId);
        
        assertTrue(deleted, "Milestone should be deleted successfully");
        
        Optional<Milestone> foundMilestone = milestoneController.findById(testMilestoneId);
        assertFalse(foundMilestone.isPresent(), "Deleted milestone should not be found");
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

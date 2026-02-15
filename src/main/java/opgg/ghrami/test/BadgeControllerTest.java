package opgg.ghrami.test;

import opgg.ghrami.controller.BadgeController;
import opgg.ghrami.controller.UserController;
import opgg.ghrami.model.Badge;
import opgg.ghrami.model.User;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class BadgeControllerTest {
    
    private BadgeController badgeController;
    private UserController userController;
    private Badge testBadge;
    private Long testBadgeId;
    private Long testUserId;

    @BeforeAll
    public void setUp() {
        badgeController = new BadgeController();
        userController = new UserController();
        
        // Create test user
        User testUser = new User();
        testUser.setUsername("badgetest_" + System.currentTimeMillis());
        testUser.setFullName("Badge Test User");
        testUser.setEmail("badgetest" + System.currentTimeMillis() + "@test.com");
        testUser.setPassword("password123");
        testUser.setBio("Test");
        testUser.setLocation("Test");
        testUser.setProfilePicture("");
        testUser.setOnline(false);
        testUser.setCreatedAt(LocalDateTime.now());
        
        User createdUser = userController.create(testUser);
        testUserId = createdUser.getUserId();
        
        System.out.println("BadgeController tests initialized");
    }

    @Test
    @Order(1)
    public void testCreateBadge() {
        testBadge = new Badge();
        testBadge.setUserId(testUserId);
        testBadge.setName("Test Achievement");
        testBadge.setDescription("Test badge description");
        testBadge.setEarnedDate(LocalDateTime.now());

        Badge createdBadge = badgeController.create(testBadge);
        
        assertNotNull(createdBadge, "Created badge should not be null");
        assertNotNull(createdBadge.getBadgeId(), "Badge ID should be generated");
        assertEquals(testBadge.getName(), createdBadge.getName());
        
        testBadgeId = createdBadge.getBadgeId();
        testBadge = createdBadge;
    }

    @Test
    @Order(2)
    public void testFindBadgeById() {
        assertNotNull(testBadgeId, "Test badge ID should be set");
        
        Optional<Badge> foundBadge = badgeController.findById(testBadgeId);
        
        assertTrue(foundBadge.isPresent(), "Badge should be found");
        assertEquals(testBadge.getName(), foundBadge.get().getName());
    }

    @Test
    @Order(3)
    public void testFindBadgesByUserId() {
        assertNotNull(testUserId, "Test user ID should be set");
        
        List<Badge> badges = badgeController.findByUserId(testUserId);
        
        assertNotNull(badges, "Badges list should not be null");
        assertFalse(badges.isEmpty(), "User should have at least one badge");
        assertTrue(badges.stream().anyMatch(b -> b.getBadgeId().equals(testBadgeId)));
    }

    @Test
    @Order(4)
    public void testCountBadgesByUserId() {
        int count = badgeController.countByUserId(testUserId);
        
        assertTrue(count > 0, "User should have at least one badge");
    }

    @Test
    @Order(5)
    public void testUserHasBadge() {
        boolean hasBadge = badgeController.userHasBadge(testUserId, "Test Achievement");
        
        assertTrue(hasBadge, "User should have the Test Achievement badge");
        
        boolean hasNonExistent = badgeController.userHasBadge(testUserId, "Non-existent Badge");
        assertFalse(hasNonExistent, "User should not have non-existent badge");
    }

    @Test
    @Order(6)
    public void testFindAllBadges() {
        List<Badge> badges = badgeController.findAll();
        
        assertNotNull(badges, "Badges list should not be null");
        assertFalse(badges.isEmpty(), "Badges list should not be empty");
    }

    @Test
    @Order(7)
    public void testUpdateBadge() {
        assertNotNull(testBadge, "Test badge should be set");
        
        testBadge.setDescription("Updated badge description");
        
        Badge updatedBadge = badgeController.update(testBadge);
        
        assertNotNull(updatedBadge, "Updated badge should not be null");
        assertEquals("Updated badge description", updatedBadge.getDescription());
    }

    @Test
    @Order(8)
    public void testDeleteBadge() {
        assertNotNull(testBadgeId, "Test badge ID should be set");
        
        boolean deleted = badgeController.delete(testBadgeId);
        
        assertTrue(deleted, "Badge should be deleted successfully");
        
        Optional<Badge> foundBadge = badgeController.findById(testBadgeId);
        assertFalse(foundBadge.isPresent(), "Deleted badge should not be found");
    }

    @AfterAll
    public void tearDown() {
        // Clean up test user
        if (testUserId != null) {
            userController.delete(testUserId);
        }
    }
}

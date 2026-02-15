package opgg.ghrami.test;

import opgg.ghrami.controller.FriendshipController;
import opgg.ghrami.controller.UserController;
import opgg.ghrami.model.Friendship;
import opgg.ghrami.model.FriendshipStatus;
import opgg.ghrami.model.User;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class FriendshipControllerTest {
    
    private FriendshipController friendshipController;
    private UserController userController;
    private Friendship testFriendship;
    private Long testFriendshipId;
    private Long testUser1Id;
    private Long testUser2Id;

    @BeforeAll
    public void setUp() {
        friendshipController = new FriendshipController();
        userController = new UserController();
        
        // Create first test user
        User testUser1 = new User();
        testUser1.setUsername("friendtest1_" + System.currentTimeMillis());
        testUser1.setFullName("Friend Test User 1");
        testUser1.setEmail("friendtest1_" + System.currentTimeMillis() + "@test.com");
        testUser1.setPassword("password123");
        testUser1.setBio("Test");
        testUser1.setLocation("Test");
        testUser1.setProfilePicture("");
        testUser1.setOnline(false);
        testUser1.setCreatedAt(LocalDateTime.now());
        
        User createdUser1 = userController.create(testUser1);
        testUser1Id = createdUser1.getUserId();
        
        // Create second test user
        User testUser2 = new User();
        testUser2.setUsername("friendtest2_" + System.currentTimeMillis());
        testUser2.setFullName("Friend Test User 2");
        testUser2.setEmail("friendtest2_" + System.currentTimeMillis() + "@test.com");
        testUser2.setPassword("password123");
        testUser2.setBio("Test");
        testUser2.setLocation("Test");
        testUser2.setProfilePicture("");
        testUser2.setOnline(false);
        testUser2.setCreatedAt(LocalDateTime.now());
        
        User createdUser2 = userController.create(testUser2);
        testUser2Id = createdUser2.getUserId();
        
        System.out.println("FriendshipController tests initialized");
    }

    @Test
    @Order(1)
    public void testSendFriendRequest() {
        testFriendship = friendshipController.sendFriendRequest(testUser1Id, testUser2Id);
        
        assertNotNull(testFriendship, "Friendship should be created");
        assertNotNull(testFriendship.getFriendshipId(), "Friendship ID should be generated");
        assertEquals(FriendshipStatus.PENDING, testFriendship.getStatus());
        
        testFriendshipId = testFriendship.getFriendshipId();
    }

    @Test
    @Order(2)
    public void testFindFriendshipById() {
        assertNotNull(testFriendshipId, "Test friendship ID should be set");
        
        Optional<Friendship> foundFriendship = friendshipController.findById(testFriendshipId);
        
        assertTrue(foundFriendship.isPresent(), "Friendship should be found");
        assertEquals(testUser1Id, foundFriendship.get().getUser1Id());
        assertEquals(testUser2Id, foundFriendship.get().getUser2Id());
    }

    @Test
    @Order(3)
    public void testGetPendingRequests() {
        List<Friendship> pendingRequests = friendshipController.getPendingRequestsForUser(testUser2Id);
        
        assertNotNull(pendingRequests, "Pending requests list should not be null");
        assertFalse(pendingRequests.isEmpty(), "User 2 should have pending requests");
        assertTrue(pendingRequests.stream().anyMatch(f -> f.getFriendshipId().equals(testFriendshipId)));
    }

    @Test
    @Order(4)
    public void testGetFriendshipBetweenUsers() {
        Optional<Friendship> friendship = friendshipController.getFriendshipBetweenUsers(testUser1Id, testUser2Id);
        
        assertTrue(friendship.isPresent(), "Friendship should exist between users");
        assertEquals(testFriendshipId, friendship.get().getFriendshipId());
    }

    @Test
    @Order(5)
    public void testFindByUserId() {
        List<Friendship> friendships = friendshipController.findByUserId(testUser1Id);
        
        assertNotNull(friendships, "Friendships list should not be null");
        assertFalse(friendships.isEmpty(), "User should have at least one friendship");
    }

    @Test
    @Order(6)
    public void testAcceptFriendRequest() {
        assertNotNull(testFriendshipId, "Test friendship ID should be set");
        
        boolean accepted = friendshipController.acceptFriendRequest(testFriendshipId);
        
        assertTrue(accepted, "Friend request should be accepted");
        
        Optional<Friendship> updated = friendshipController.findById(testFriendshipId);
        assertTrue(updated.isPresent(), "Friendship should still exist");
        assertEquals(FriendshipStatus.ACCEPTED, updated.get().getStatus());
    }

    @Test
    @Order(7)
    public void testGetAcceptedFriendships() {
        List<Friendship> acceptedFriendships = friendshipController.getAcceptedFriendships(testUser1Id);
        
        assertNotNull(acceptedFriendships, "Accepted friendships list should not be null");
        assertFalse(acceptedFriendships.isEmpty(), "User should have accepted friendships");
        assertTrue(acceptedFriendships.stream().anyMatch(f -> f.getFriendshipId().equals(testFriendshipId)));
    }

    @Test
    @Order(8)
    public void testFindAllFriendships() {
        List<Friendship> friendships = friendshipController.findAll();
        
        assertNotNull(friendships, "Friendships list should not be null");
        assertFalse(friendships.isEmpty(), "Friendships list should not be empty");
    }

    @Test
    @Order(9)
    public void testUpdateFriendship() {
        assertNotNull(testFriendship, "Test friendship should be set");
        
        testFriendship.setStatus(FriendshipStatus.ACCEPTED);
        testFriendship.setAcceptedDate(LocalDateTime.now());
        
        Friendship updatedFriendship = friendshipController.update(testFriendship);
        
        assertNotNull(updatedFriendship, "Updated friendship should not be null");
        assertEquals(FriendshipStatus.ACCEPTED, updatedFriendship.getStatus());
    }

    @Test
    @Order(10)
    public void testCannotBeFriendWithSelf() {
        Friendship selfFriendship = friendshipController.sendFriendRequest(testUser1Id, testUser1Id);
        
        assertNull(selfFriendship, "User cannot be friend with themselves");
    }

    @Test
    @Order(11)
    public void testDeleteFriendship() {
        assertNotNull(testFriendshipId, "Test friendship ID should be set");
        
        boolean deleted = friendshipController.delete(testFriendshipId);
        
        assertTrue(deleted, "Friendship should be deleted successfully");
        
        Optional<Friendship> foundFriendship = friendshipController.findById(testFriendshipId);
        assertFalse(foundFriendship.isPresent(), "Deleted friendship should not be found");
    }

    @AfterAll
    public void tearDown() {
        // Clean up test users
        if (testUser1Id != null) {
            userController.delete(testUser1Id);
        }
        if (testUser2Id != null) {
            userController.delete(testUser2Id);
        }
    }
}

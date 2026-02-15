package opgg.ghrami.test;

import opgg.ghrami.controller.ClassProviderController;
import opgg.ghrami.controller.UserController;
import opgg.ghrami.model.ClassProvider;
import opgg.ghrami.model.User;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ClassProviderControllerTest {
    
    private ClassProviderController providerController;
    private UserController userController;
    private ClassProvider testProvider;
    private User testUser;
    private Long testProviderId;
    private Long testUserId;

    @BeforeAll
    public void setUp() {
        providerController = ClassProviderController.getInstance();
        userController = new UserController();
        System.out.println("ClassProviderController tests initialized");
        
        // Create a test user first
        testUser = new User();
        testUser.setUsername("instructor_" + System.currentTimeMillis());
        testUser.setFullName("Test Instructor");
        testUser.setEmail("instructor" + System.currentTimeMillis() + "@test.com");
        testUser.setPassword("hashedPass123");
        testUser.setBio("Test instructor bio");
        testUser.setLocation("Test City");
        testUser.setProfilePicture("");
        testUser.setOnline(false);
        testUser.setCreatedAt(LocalDateTime.now());
        
        testUser = userController.create(testUser);
        testUserId = testUser.getUserId();
        assertNotNull(testUserId, "Test user should be created");
    }

    @Test
    @Order(1)
    public void testCreateProvider() {
        testProvider = new ClassProvider();
        testProvider.setUserId(testUserId);
        testProvider.setCompanyName("Test Academy");
        testProvider.setExpertise("Software Development, Web Design, Testing");
        testProvider.setRating(0.0);
        testProvider.setVerified(false);

        boolean created = providerController.create(testProvider);
        
        assertTrue(created, "Provider should be created successfully");
        assertNotNull(testProvider.getProviderId(), "Provider ID should be generated");
        
        testProviderId = testProvider.getProviderId();
        System.out.println("Created provider with ID: " + testProviderId);
    }

    @Test
    @Order(2)
    public void testGetProviderById() {
        assertNotNull(testProviderId, "Test provider ID should be set");
        
        ClassProvider foundProvider = providerController.getById(testProviderId);
        
        assertNotNull(foundProvider, "Provider should be found");
        assertEquals(testUserId, foundProvider.getUserId());
        assertEquals("Test Academy", foundProvider.getCompanyName());
        assertFalse(foundProvider.isVerified(), "Provider should not be verified initially");
    }

    @Test
    @Order(3)
    public void testGetProviderByUserId() {
        assertNotNull(testUserId, "Test user ID should be set");
        
        ClassProvider foundProvider = providerController.getByUserId(testUserId);
        
        assertNotNull(foundProvider, "Provider should be found by user ID");
        assertEquals(testProviderId, foundProvider.getProviderId());
        assertEquals(testUser.getUsername(), foundProvider.getUsername());
    }

    @Test
    @Order(4)
    public void testIsUserProvider() {
        assertTrue(providerController.isUserProvider(testUserId), 
                  "User should be identified as a provider");
        
        assertFalse(providerController.isUserProvider(999999L), 
                   "Non-existent user should not be a provider");
    }

    @Test
    @Order(5)
    public void testIsUserVerifiedProvider() {
        assertFalse(providerController.isUserVerifiedProvider(testUserId), 
                   "Provider should not be verified initially");
    }

    @Test
    @Order(6)
    public void testGetPendingVerification() {
        List<ClassProvider> pendingProviders = providerController.getPendingVerification();
        
        assertNotNull(pendingProviders, "Pending providers list should not be null");
        assertTrue(pendingProviders.stream().anyMatch(p -> p.getProviderId().equals(testProviderId)),
                  "Test provider should be in pending list");
    }

    @Test
    @Order(7)
    public void testGetAllProviders() {
        List<ClassProvider> allProviders = providerController.getAll();
        
        assertNotNull(allProviders, "Providers list should not be null");
        assertFalse(allProviders.isEmpty(), "Providers list should not be empty");
        assertTrue(allProviders.stream().anyMatch(p -> p.getProviderId().equals(testProviderId)),
                  "Test provider should be in all providers list");
    }

    @Test
    @Order(8)
    public void testVerifyProvider() {
        assertNotNull(testProviderId, "Test provider ID should be set");
        
        boolean verified = providerController.verifyProvider(testProviderId, true);
        
        assertTrue(verified, "Provider should be verified successfully");
        
        // Verify the change
        ClassProvider updatedProvider = providerController.getById(testProviderId);
        assertTrue(updatedProvider.isVerified(), "Provider should now be verified");
        
        // Check verified status
        assertTrue(providerController.isUserVerifiedProvider(testUserId),
                  "User should now be identified as verified provider");
    }

    @Test
    @Order(9)
    public void testGetAllVerified() {
        List<ClassProvider> verifiedProviders = providerController.getAllVerified();
        
        assertNotNull(verifiedProviders, "Verified providers list should not be null");
        assertTrue(verifiedProviders.stream().anyMatch(p -> p.getProviderId().equals(testProviderId)),
                  "Test provider should be in verified list");
    }

    @Test
    @Order(10)
    public void testUpdateProvider() {
        assertNotNull(testProviderId, "Test provider ID should be set");
        
        ClassProvider provider = providerController.getById(testProviderId);
        provider.setExpertise("Advanced Software Development, Full Stack Web Development");
        provider.setCompanyName("Test Academy Pro");
        
        boolean updated = providerController.update(provider);
        
        assertTrue(updated, "Provider should be updated successfully");
        
        // Verify the update
        ClassProvider updatedProvider = providerController.getById(testProviderId);
        assertEquals("Test Academy Pro", updatedProvider.getCompanyName());
        assertTrue(updatedProvider.getExpertise().contains("Full Stack"));
    }

    @Test
    @Order(11)
    public void testUpdateRating() {
        assertNotNull(testProviderId, "Test provider ID should be set");
        
        double newRating = 4.5;
        boolean updated = providerController.updateRating(testProviderId, newRating);
        
        assertTrue(updated, "Rating should be updated successfully");
        
        // Verify the rating
        ClassProvider provider = providerController.getById(testProviderId);
        assertEquals(newRating, provider.getRating(), 0.01);
    }

    @Test
    @Order(12)
    public void testUnverifyProvider() {
        assertNotNull(testProviderId, "Test provider ID should be set");
        
        boolean unverified = providerController.verifyProvider(testProviderId, false);
        
        assertTrue(unverified, "Provider should be unverified successfully");
        
        // Verify the change
        ClassProvider provider = providerController.getById(testProviderId);
        assertFalse(provider.isVerified(), "Provider should now be unverified");
    }

    @Test
    @Order(13)
    public void testDeleteProvider() {
        assertNotNull(testProviderId, "Test provider ID should be set");
        
        boolean deleted = providerController.delete(testProviderId);
        
        assertTrue(deleted, "Provider should be deleted successfully");
        
        // Verify deletion
        ClassProvider provider = providerController.getById(testProviderId);
        assertNull(provider, "Provider should not be found after deletion");
        
        assertFalse(providerController.isUserProvider(testUserId),
                   "User should no longer be identified as provider");
    }

    @AfterAll
    public void cleanUp() {
        // Clean up test user
        if (testUserId != null) {
            userController.delete(testUserId);
            System.out.println("Cleaned up test user: " + testUserId);
        }
        System.out.println("ClassProviderController tests completed");
    }
}

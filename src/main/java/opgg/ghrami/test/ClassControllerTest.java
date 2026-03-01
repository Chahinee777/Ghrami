package opgg.ghrami.test;

import opgg.ghrami.controller.BookingController;
import opgg.ghrami.controller.ClassController;
import opgg.ghrami.controller.ClassProviderController;
import opgg.ghrami.controller.UserController;
import opgg.ghrami.model.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ClassControllerTest {
    
    private ClassController classController;
    private ClassProviderController providerController;
    private UserController userController;
    private ClassEntity testClass;
    private User testUser;
    private ClassProvider testProvider;
    private Long testClassId;
    private Long testProviderId;
    private Long testUserId;

    @BeforeAll
    public void setUp() {
        classController = ClassController.getInstance();
        providerController = ClassProviderController.getInstance();
        userController = new UserController();
        System.out.println("ClassController tests initialized");
        
        // Create test user
        testUser = new User();
        testUser.setUsername("classinstructor_" + System.currentTimeMillis());
        testUser.setFullName("Class Test Instructor");
        testUser.setEmail("classinstr" + System.currentTimeMillis() + "@test.com");
        testUser.setPassword("hashedPass123");
        testUser.setBio("Test bio");
        testUser.setLocation("Test City");
        testUser.setProfilePicture("");
        testUser.setOnline(false);
        testUser.setCreatedAt(LocalDateTime.now());
        
        testUser = userController.create(testUser);
        testUserId = testUser.getUserId();
        assertNotNull(testUserId, "Test user should be created");
        
        // Create test provider
        testProvider = new ClassProvider();
        testProvider.setUserId(testUserId);
        testProvider.setCompanyName("Test Class Academy");
        testProvider.setExpertise("Teaching, Training");
        testProvider.setRating(4.0);
        testProvider.setVerified(true);
        
        boolean providerCreated = providerController.create(testProvider);
        assertTrue(providerCreated, "Test provider should be created");
        
        testProvider = providerController.getByUserId(testUserId);
        testProviderId = testProvider.getProviderId();
        assertNotNull(testProviderId, "Test provider ID should be set");
    }

    @Test
    @Order(1)
    public void testCreateClass() {
        testClass = new ClassEntity();
        testClass.setProviderId(testProviderId);
        testClass.setTitle("Introduction to Java Programming");
        testClass.setDescription("Learn the fundamentals of Java programming language");
        testClass.setCategory("Programming");
        testClass.setPrice(49.99);
        testClass.setDuration(90);
        testClass.setMaxParticipants(20);
        testClass.setCurrentEnrollment(0);

        boolean created = classController.create(testClass);
        
        assertTrue(created, "Class should be created successfully");
        assertNotNull(testClass.getClassId(), "Class ID should be generated");
        
        testClassId = testClass.getClassId();
        System.out.println("Created class with ID: " + testClassId);
    }

    @Test
    @Order(2)
    public void testGetClassById() {
        assertNotNull(testClassId, "Test class ID should be set");
        
        ClassEntity foundClass = classController.getById(testClassId);
        
        assertNotNull(foundClass, "Class should be found");
        assertEquals("Introduction to Java Programming", foundClass.getTitle());
        assertEquals("Programming", foundClass.getCategory());
        assertEquals(49.99, foundClass.getPrice(), 0.01);
        assertEquals(90, foundClass.getDuration());
        assertEquals(20, foundClass.getMaxParticipants());
        assertEquals(0, foundClass.getCurrentEnrollment());
    }

    @Test
    @Order(3)
    public void testGetByProviderId() {
        assertNotNull(testProviderId, "Test provider ID should be set");
        
        List<ClassEntity> classes = classController.getByProviderId(testProviderId);
        
        assertNotNull(classes, "Classes list should not be null");
        assertFalse(classes.isEmpty(), "Provider should have at least one class");
        assertTrue(classes.stream().anyMatch(c -> c.getClassId().equals(testClassId)),
                  "Test class should be in the list");
    }

    @Test
    @Order(4)
    public void testGetAllClasses() {
        List<ClassEntity> allClasses = classController.getAll(null, null, null);
        
        assertNotNull(allClasses, "Classes list should not be null");
        assertFalse(allClasses.isEmpty(), "Classes list should not be empty");
        assertTrue(allClasses.stream().anyMatch(c -> c.getClassId().equals(testClassId)),
                  "Test class should be in all classes");
    }

    @Test
    @Order(5)
    public void testGetAllWithDetails() {
        // getAll already includes details with provider info
        List<ClassEntity> classesWithDetails = classController.getAll(null, null, null);
        
        assertNotNull(classesWithDetails, "Classes with details list should not be null");
        assertFalse(classesWithDetails.isEmpty(), "Should have classes with details");
        
        ClassEntity classWithDetails = classesWithDetails.stream()
                .filter(c -> c.getClassId().equals(testClassId))
                .findFirst()
                .orElse(null);
        
        assertNotNull(classWithDetails, "Test class should be in detailed list");
        assertNotNull(classWithDetails.getProviderName(), "Provider name should be loaded");
    }

    @Test
    @Order(6)
    public void testGetByCategory() {
        // Use getAll with category parameter
        List<ClassEntity> programmingClasses = classController.getAll("Programming", null, null);
        
        assertNotNull(programmingClasses, "Programming classes list should not be null");
        assertFalse(programmingClasses.isEmpty(), "Should have programming classes");
        assertTrue(programmingClasses.stream().anyMatch(c -> c.getClassId().equals(testClassId)),
                  "Test class should be in programming category");
    }

    @Test
    @Order(7)
    public void testSearchClasses() {
        List<ClassEntity> searchResults = classController.search("Java");
        
        assertNotNull(searchResults, "Search results should not be null");
        assertFalse(searchResults.isEmpty(), "Should find classes with 'Java' in title");
        assertTrue(searchResults.stream().anyMatch(c -> c.getClassId().equals(testClassId)),
                  "Test class should be in search results");
    }

    @Test
    @Order(8)
    public void testGetCategories() {
        List<String> categories = classController.getCategories();
        
        assertNotNull(categories, "Categories list should not be null");
        assertFalse(categories.isEmpty(), "Should have at least one category");
        assertTrue(categories.contains("Programming"), "Should contain 'Programming' category");
    }

    @Test
    @Order(9)
    public void testHasAvailableSpots() {
        // Use BookingController to check availability
        BookingController bookingController = BookingController.getInstance();
        boolean hasSpots = bookingController.checkAvailability(testClassId);
        
        assertTrue(hasSpots, "New class should have available spots");
        
        // Check the class enrollment
        ClassEntity currentClass = classController.getById(testClassId);
        int availableSpots = currentClass.getMaxParticipants() - currentClass.getCurrentEnrollment();
        assertEquals(20, availableSpots, "Should have 20 available spots");
    }

    @Test
    @Order(10)
    public void testUpdateClass() {
        assertNotNull(testClassId, "Test class ID should be set");
        
        ClassEntity classToUpdate = classController.getById(testClassId);
        classToUpdate.setTitle("Advanced Java Programming");
        classToUpdate.setPrice(79.99);
        classToUpdate.setDuration(120);
        classToUpdate.setDescription("Deep dive into advanced Java concepts");
        
        boolean updated = classController.update(classToUpdate);
        
        assertTrue(updated, "Class should be updated successfully");
        
        // Verify the update
        ClassEntity updatedClass = classController.getById(testClassId);
        assertEquals("Advanced Java Programming", updatedClass.getTitle());
        assertEquals(79.99, updatedClass.getPrice(), 0.01);
        assertEquals(120, updatedClass.getDuration());
    }

    @Test
    @Order(11)
    public void testIncrementEnrollment() {
        assertNotNull(testClassId, "Test class ID should be set");
        
        ClassEntity classBefore = classController.getById(testClassId);
        int enrollmentBefore = classBefore.getCurrentEnrollment();
        
        // Create a booking to increment enrollment
        BookingController bookingController = BookingController.getInstance();
        Booking testBooking = new Booking();
        testBooking.setClassId(testClassId);
        testBooking.setUserId(testUserId);
        testBooking.setBookingDate(LocalDateTime.now());
        testBooking.setStatus(BookingStatus.SCHEDULED);
        testBooking.setPaymentStatus(PaymentStatus.PENDING);
        testBooking.setTotalAmount(79.99);
        
        boolean bookingCreated = bookingController.create(testBooking);
        assertTrue(bookingCreated, "Booking should be created");
        
        // Verify the increment
        ClassEntity classAfter = classController.getById(testClassId);
        assertEquals(enrollmentBefore + 1, classAfter.getCurrentEnrollment(),
                    "Enrollment should increase by 1");
        
        // Clean up
        bookingController.delete(testBooking.getBookingId());
    }

    @Test
    @Order(12)
    public void testDecrementEnrollment() {
        assertNotNull(testClassId, "Test class ID should be set");
        
        // First create a booking to have something to decrement
        BookingController bookingController = BookingController.getInstance();
        Booking testBooking = new Booking();
        testBooking.setClassId(testClassId);
        testBooking.setUserId(testUserId);
        testBooking.setBookingDate(LocalDateTime.now());
        testBooking.setStatus(BookingStatus.SCHEDULED);
        testBooking.setPaymentStatus(PaymentStatus.PENDING);
        testBooking.setTotalAmount(79.99);
        bookingController.create(testBooking);
        
        ClassEntity classBefore = classController.getById(testClassId);
        int enrollmentBefore = classBefore.getCurrentEnrollment();
        
        // Cancel the booking to decrement enrollment
        boolean cancelled = bookingController.cancel(testBooking.getBookingId());
        assertTrue(cancelled, "Booking should be cancelled");
        
        // Verify the decrement
        ClassEntity classAfter = classController.getById(testClassId);
        assertEquals(enrollmentBefore - 1, classAfter.getCurrentEnrollment(),
                    "Enrollment should decrease by 1 when booking is cancelled");
        
        // Clean up
        bookingController.delete(testBooking.getBookingId());
    }

    @Test
    @Order(13)
    public void testFilterByPriceRange() {
        // Use getAll with price range parameters
        List<ClassEntity> affordableClasses = classController.getAll(null, 0.0, 100.0);
        
        assertNotNull(affordableClasses, "Filtered classes should not be null");
        assertTrue(affordableClasses.stream().anyMatch(c -> c.getClassId().equals(testClassId)),
                  "Test class should be in price range");
        assertTrue(affordableClasses.stream().allMatch(c -> c.getPrice() <= 100.0),
                  "All classes should be within price range");
    }

    @Test
    @Order(14)
    public void testDeleteClass() {
        assertNotNull(testClassId, "Test class ID should be set");
        
        boolean deleted = classController.delete(testClassId);
        
        assertTrue(deleted, "Class should be deleted successfully");
        
        // Verify deletion
        ClassEntity deletedClass = classController.getById(testClassId);
        assertNull(deletedClass, "Class should not be found after deletion");
    }

    @AfterAll
    public void cleanUp() {
        // Clean up test provider and user
        if (testProviderId != null) {
            providerController.delete(testProviderId);
            System.out.println("Cleaned up test provider: " + testProviderId);
        }
        if (testUserId != null) {
            userController.delete(testUserId);
            System.out.println("Cleaned up test user: " + testUserId);
        }
        System.out.println("ClassController tests completed");
    }
}

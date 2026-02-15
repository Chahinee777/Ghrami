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
public class BookingControllerTest {
    
    private BookingController bookingController;
    private ClassController classController;
    private ClassProviderController providerController;
    private UserController userController;
    
    private Booking testBooking;
    private ClassEntity testClass;
    private User testStudent;
    private User testInstructor;
    private ClassProvider testProvider;
    
    private Long testBookingId;
    private Long testClassId;
    private Long testStudentId;
    private Long testInstructorId;
    private Long testProviderId;

    @BeforeAll
    public void setUp() {
        bookingController = BookingController.getInstance();
        classController = ClassController.getInstance();
        providerController = ClassProviderController.getInstance();
        userController = new UserController();
        System.out.println("BookingController tests initialized");
        
        // Create test instructor
        testInstructor = new User();
        testInstructor.setUsername("bookinginstr_" + System.currentTimeMillis());
        testInstructor.setFullName("Booking Test Instructor");
        testInstructor.setEmail("bookinginstr" + System.currentTimeMillis() + "@test.com");
        testInstructor.setPassword("hashedPass123");
        testInstructor.setBio("Test instructor");
        testInstructor.setLocation("Test City");
        testInstructor.setProfilePicture("");
        testInstructor.setOnline(false);
        testInstructor.setCreatedAt(LocalDateTime.now());
        
        testInstructor = userController.create(testInstructor);
        testInstructorId = testInstructor.getUserId();
        assertNotNull(testInstructorId, "Test instructor should be created");
        
        // Create test student
        testStudent = new User();
        testStudent.setUsername("bookingstudent_" + System.currentTimeMillis());
        testStudent.setFullName("Booking Test Student");
        testStudent.setEmail("bookingstudent" + System.currentTimeMillis() + "@test.com");
        testStudent.setPassword("hashedPass123");
        testStudent.setBio("Test student");
        testStudent.setLocation("Test City");
        testStudent.setProfilePicture("");
        testStudent.setOnline(false);
        testStudent.setCreatedAt(LocalDateTime.now());
        
        testStudent = userController.create(testStudent);
        testStudentId = testStudent.getUserId();
        assertNotNull(testStudentId, "Test student should be created");
        
        // Create test provider
        testProvider = new ClassProvider();
        testProvider.setUserId(testInstructorId);
        testProvider.setCompanyName("Booking Test Academy");
        testProvider.setExpertise("Teaching");
        testProvider.setRating(4.5);
        testProvider.setVerified(true);
        
        boolean providerCreated = providerController.create(testProvider);
        assertTrue(providerCreated, "Test provider should be created");
        
        testProvider = providerController.getByUserId(testInstructorId);
        testProviderId = testProvider.getProviderId();
        assertNotNull(testProviderId, "Test provider ID should be set");
        
        // Create test class
        testClass = new ClassEntity();
        testClass.setProviderId(testProviderId);
        testClass.setTitle("Booking Test Class");
        testClass.setDescription("Test class for booking");
        testClass.setCategory("Testing");
        testClass.setPrice(29.99);
        testClass.setDuration(60);
        testClass.setMaxParticipants(5);
        testClass.setCurrentEnrollment(0);
        
        boolean classCreated = classController.create(testClass);
        assertTrue(classCreated, "Test class should be created");
        
        testClassId = testClass.getClassId();
        assertNotNull(testClassId, "Test class ID should be set");
    }

    @Test
    @Order(1)
    public void testCreateBooking() {
        testBooking = new Booking();
        testBooking.setClassId(testClassId);
        testBooking.setUserId(testStudentId);
        testBooking.setBookingDate(LocalDateTime.now());
        testBooking.setStatus(BookingStatus.SCHEDULED);
        testBooking.setPaymentStatus(PaymentStatus.PENDING);
        testBooking.setTotalAmount(29.99);

        boolean created = bookingController.create(testBooking);
        
        assertTrue(created, "Booking should be created successfully");
        assertNotNull(testBooking.getBookingId(), "Booking ID should be generated");
        
        testBookingId = testBooking.getBookingId();
        System.out.println("Created booking with ID: " + testBookingId);
    }

    @Test
    @Order(2)
    public void testGetBookingById() {
        assertNotNull(testBookingId, "Test booking ID should be set");
        
        Booking foundBooking = bookingController.getById(testBookingId);
        
        assertNotNull(foundBooking, "Booking should be found");
        assertEquals(testClassId, foundBooking.getClassId());
        assertEquals(testStudentId, foundBooking.getUserId());
        assertEquals(BookingStatus.SCHEDULED, foundBooking.getStatus());
        assertEquals(PaymentStatus.PENDING, foundBooking.getPaymentStatus());
        assertEquals(29.99, foundBooking.getTotalAmount(), 0.01);
    }

    @Test
    @Order(3)
    public void testGetByUserId() {
        assertNotNull(testStudentId, "Test student ID should be set");
        
        List<Booking> userBookings = bookingController.getByUserId(testStudentId, null);
        
        assertNotNull(userBookings, "User bookings list should not be null");
        assertFalse(userBookings.isEmpty(), "User should have bookings");
        assertTrue(userBookings.stream().anyMatch(b -> b.getBookingId().equals(testBookingId)),
                  "Test booking should be in user's bookings");
    }

    @Test
    @Order(4)
    public void testGetByClassId() {
        assertNotNull(testClassId, "Test class ID should be set");
        
        List<Booking> classBookings = bookingController.getByClassId(testClassId);
        
        assertNotNull(classBookings, "Class bookings list should not be null");
        assertFalse(classBookings.isEmpty(), "Class should have bookings");
        assertTrue(classBookings.stream().anyMatch(b -> b.getBookingId().equals(testBookingId)),
                  "Test booking should be in class bookings");
    }

    @Test
    @Order(5)
    public void testGetByProviderId() {
        assertNotNull(testProviderId, "Test provider ID should be set");
        
        List<Booking> providerBookings = bookingController.getByProviderId(testProviderId);
        
        assertNotNull(providerBookings, "Provider bookings list should not be null");
        assertFalse(providerBookings.isEmpty(), "Provider should have bookings");
        assertTrue(providerBookings.stream().anyMatch(b -> b.getBookingId().equals(testBookingId)),
                  "Test booking should be in provider's bookings");
    }

    @Test
    @Order(6)
    public void testHasUserBooked() {
        assertTrue(bookingController.hasUserBooked(testStudentId, testClassId),
                  "User should have booked this class");
        
        assertFalse(bookingController.hasUserBooked(testStudentId, 99999L),
                   "User should not have booked non-existent class");
    }

    @Test
    @Order(7)
    public void testCheckAvailability() {
        assertTrue(bookingController.checkAvailability(testClassId),
                  "Class should have available spots");
    }

    @Test
    @Order(8)
    public void testGetProviderRevenue() {
        double revenue = bookingController.getProviderRevenue(testProviderId, null);
        
        assertTrue(revenue >= 0, "Revenue should not be negative");
        // Revenue should be 0 since payment is pending
        assertEquals(0.0, revenue, 0.01, "Revenue should be 0 for pending payments");
    }

    @Test
    @Order(9)
    public void testUpdatePaymentStatus() {
        assertNotNull(testBookingId, "Test booking ID should be set");
        
        boolean updated = bookingController.updatePaymentStatus(testBookingId, PaymentStatus.PAID);
        
        assertTrue(updated, "Payment status should be updated");
        
        // Verify the update
        Booking updatedBooking = bookingController.getById(testBookingId);
        assertEquals(PaymentStatus.PAID, updatedBooking.getPaymentStatus());
        
        // Check revenue after payment
        double revenue = bookingController.getProviderRevenue(testProviderId, null);
        assertEquals(29.99, revenue, 0.01, "Revenue should now reflect paid booking");
    }

    @Test
    @Order(10)
    public void testGetProviderRevenueByPaymentStatus() {
        double paidRevenue = bookingController.getProviderRevenueByPaymentStatus(
                testProviderId, PaymentStatus.PAID);
        
        assertEquals(29.99, paidRevenue, 0.01, "Paid revenue should match booking amount");
        
        double pendingRevenue = bookingController.getProviderRevenueByPaymentStatus(
                testProviderId, PaymentStatus.PENDING);
        
        assertEquals(0.0, pendingRevenue, 0.01, "Pending revenue should be 0");
    }

    @Test
    @Order(11)
    public void testCompleteBooking() {
        assertNotNull(testBookingId, "Test booking ID should be set");
        
        boolean completed = bookingController.complete(testBookingId);
        
        assertTrue(completed, "Booking should be marked as completed");
        
        // Verify the status change
        Booking completedBooking = bookingController.getById(testBookingId);
        assertEquals(BookingStatus.COMPLETED, completedBooking.getStatus());
    }

    @Test
    @Order(12)
    public void testCancelBooking() {
        // Create another booking to cancel
        Booking bookingToCancel = new Booking();
        bookingToCancel.setClassId(testClassId);
        bookingToCancel.setUserId(testStudentId);
        bookingToCancel.setBookingDate(LocalDateTime.now());
        bookingToCancel.setStatus(BookingStatus.SCHEDULED);
        bookingToCancel.setPaymentStatus(PaymentStatus.PENDING);
        bookingToCancel.setTotalAmount(29.99);
        
        bookingController.create(bookingToCancel);
        Long cancelBookingId = bookingToCancel.getBookingId();
        
        boolean cancelled = bookingController.cancel(cancelBookingId);
        
        assertTrue(cancelled, "Booking should be cancelled");
        
        // Verify the status change
        Booking cancelledBooking = bookingController.getById(cancelBookingId);
        assertEquals(BookingStatus.CANCELLED, cancelledBooking.getStatus());
        
        // Clean up
        bookingController.delete(cancelBookingId);
    }

    @Test
    @Order(13)
    public void testGetByUserIdWithStatus() {
        // Test filtering by status - get only completed bookings
        List<Booking> completedBookings = bookingController.getByUserId(testStudentId, BookingStatus.COMPLETED);
        
        assertNotNull(completedBookings, "Completed bookings list should not be null");
        assertTrue(completedBookings.stream().anyMatch(b -> b.getBookingId().equals(testBookingId)),
                  "Test booking should be in completed bookings");
        assertTrue(completedBookings.stream().allMatch(b -> b.getStatus() == BookingStatus.COMPLETED),
                  "All bookings should have COMPLETED status");
    }

    @Test
    @Order(14)
    public void testGetByProviderIdVerification() {
        // Verify provider can see their bookings
        List<Booking> providerBookings = bookingController.getByProviderId(testProviderId);
        
        assertNotNull(providerBookings, "Provider bookings list should not be null");
        assertFalse(providerBookings.isEmpty(), "Provider should have bookings");
        assertTrue(providerBookings.stream().anyMatch(b -> b.getBookingId().equals(testBookingId)),
                  "Test booking should be in provider's bookings");
    }

    @Test
    @Order(15)
    public void testGetByClassIdVerification() {
        // Verify we can get all bookings for a specific class
        List<Booking> classBookings = bookingController.getByClassId(testClassId);
        
        assertNotNull(classBookings, "Class bookings list should not be null");
        assertFalse(classBookings.isEmpty(), "Class should have bookings");
        assertTrue(classBookings.stream().allMatch(b -> b.getClassId().equals(testClassId)),
                  "All bookings should be for the test class");
    }

    @Test
    @Order(16)
    public void testAvailabilityWhenFull() {
        // Fill up the class (max 5 participants, already has 1)
        for (int i = 1; i < 5; i++) {
            Booking extraBooking = new Booking();
            extraBooking.setClassId(testClassId);
            extraBooking.setUserId(testStudentId);
            extraBooking.setBookingDate(LocalDateTime.now());
            extraBooking.setStatus(BookingStatus.SCHEDULED);
            extraBooking.setPaymentStatus(PaymentStatus.PENDING);
            extraBooking.setTotalAmount(29.99);
            bookingController.create(extraBooking);
        }
        
        // Now the class should be full
        assertFalse(bookingController.checkAvailability(testClassId),
                   "Class should be full");
    }

    @Test
    @Order(17)
    public void testDeleteBooking() {
        assertNotNull(testBookingId, "Test booking ID should be set");
        
        boolean deleted = bookingController.delete(testBookingId);
        
        assertTrue(deleted, "Booking should be deleted successfully");
        
        // Verify deletion
        Booking deletedBooking = bookingController.getById(testBookingId);
        assertNull(deletedBooking, "Booking should not be found after deletion");
    }

    @AfterAll
    public void cleanUp() {
        // Clean up all test data
        if (testClassId != null) {
            classController.delete(testClassId);
            System.out.println("Cleaned up test class: " + testClassId);
        }
        if (testProviderId != null) {
            providerController.delete(testProviderId);
            System.out.println("Cleaned up test provider: " + testProviderId);
        }
        if (testInstructorId != null) {
            userController.delete(testInstructorId);
            System.out.println("Cleaned up test instructor: " + testInstructorId);
        }
        if (testStudentId != null) {
            userController.delete(testStudentId);
            System.out.println("Cleaned up test student: " + testStudentId);
        }
        System.out.println("BookingController tests completed");
    }
}

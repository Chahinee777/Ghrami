package opgg.ghrami.test;

import opgg.ghrami.controller.ConnectionController;
import opgg.ghrami.controller.MeetingController;
import opgg.ghrami.controller.UserController;
import opgg.ghrami.model.Connection;
import opgg.ghrami.model.Meeting;
import opgg.ghrami.model.User;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MeetingControllerTest {
    
    private MeetingController meetingController;
    private ConnectionController connectionController;
    private UserController userController;
    private User testUser1;
    private User testUser2;
    private Connection testConnection;
    private Meeting testMeeting;
    private String testMeetingId;
    private String testConnectionId;

    @BeforeAll
    public void setUp() {
        meetingController = MeetingController.getInstance();
        connectionController = ConnectionController.getInstance();
        userController = new UserController();
        System.out.println("MeetingController tests initialized");
        
        // Create test user 1 (organizer)
        testUser1 = new User();
        testUser1.setUsername("meeting_user1_" + System.currentTimeMillis());
        testUser1.setFullName("Meeting Test Organizer");
        testUser1.setEmail("meetuser1_" + System.currentTimeMillis() + "@test.com");
        testUser1.setPassword("hashedPass123");
        testUser1.setBio("Meeting organizer");
        testUser1.setLocation("Tunis");
        testUser1.setProfilePicture("");
        testUser1.setOnline(true);
        testUser1.setCreatedAt(LocalDateTime.now());
        
        testUser1 = userController.create(testUser1);
        assertNotNull(testUser1.getUserId(), "Test user 1 should be created");
        
        // Create test user 2
        testUser2 = new User();
        testUser2.setUsername("meeting_user2_" + System.currentTimeMillis());
        testUser2.setFullName("Meeting Test Participant");
        testUser2.setEmail("meetuser2_" + System.currentTimeMillis() + "@test.com");
        testUser2.setPassword("hashedPass456");
        testUser2.setBio("Meeting participant");
        testUser2.setLocation("Tunis");
        testUser2.setProfilePicture("");
        testUser2.setOnline(true);
        testUser2.setCreatedAt(LocalDateTime.now());
        
        testUser2 = userController.create(testUser2);
        assertNotNull(testUser2.getUserId(), "Test user 2 should be created");
        
        // Create test connection
        testConnection = new Connection();
        testConnection.setInitiatorId(testUser1.getUserId());
        testConnection.setReceiverId(testUser2.getUserId());
        testConnection.setConnectionType("skill");
        testConnection.setInitiatorSkill("Teaching");
        testConnection.setReceiverSkill("Learning");
        testConnection.setStatus("accepted");
        
        testConnection = connectionController.create(testConnection);
        assertNotNull(testConnection, "Test connection should be created");
        testConnectionId = testConnection.getConnectionId();
    }

    @Test
    @Order(1)
    public void testCreateMeeting() {
        testMeeting = new Meeting();
        testMeeting.setConnectionId(testConnectionId);
        testMeeting.setOrganizerId(testUser1.getUserId());
        testMeeting.setMeetingType("physical");
        testMeeting.setLocation("Café des Délices, Tunis");
        testMeeting.setScheduledAt(LocalDateTime.now().plusDays(2));
        testMeeting.setDuration(60);
        testMeeting.setStatus("scheduled");

        Meeting created = meetingController.create(testMeeting);
        
        assertNotNull(created, "Meeting should be created successfully");
        assertNotNull(created.getMeetingId(), "Meeting ID should be generated");
        assertEquals("scheduled", created.getStatus());
        assertEquals("physical", created.getMeetingType());
        
        testMeetingId = created.getMeetingId();
        System.out.println("Created meeting with ID: " + testMeetingId);
    }

    @Test
    @Order(2)
    public void testFindMeetingById() {
        assertNotNull(testMeetingId, "Test meeting ID should be set");
        
        Optional<Meeting> foundMeeting = meetingController.findById(testMeetingId);
        
        assertTrue(foundMeeting.isPresent(), "Meeting should be found");
        assertEquals(testConnectionId, foundMeeting.get().getConnectionId());
        assertEquals(testUser1.getUserId(), foundMeeting.get().getOrganizerId());
        assertEquals("physical", foundMeeting.get().getMeetingType());
        assertEquals(60, foundMeeting.get().getDuration());
    }

    @Test
    @Order(3)
    public void testFindByConnection() {
        List<Meeting> meetings = meetingController.findByConnection(testConnectionId);
        
        assertNotNull(meetings, "Meetings list should not be null");
        assertFalse(meetings.isEmpty(), "Connection should have meetings");
        assertTrue(meetings.stream()
            .anyMatch(m -> m.getMeetingId().equals(testMeetingId)));
    }

    @Test
    @Order(4)
    public void testFindByOrganizer() {
        List<Meeting> meetings = meetingController.findByOrganizer(testUser1.getUserId());
        
        assertNotNull(meetings, "Meetings list should not be null");
        assertFalse(meetings.isEmpty(), "User 1 should have organized meetings");
        assertTrue(meetings.stream()
            .anyMatch(m -> m.getMeetingId().equals(testMeetingId)));
    }

    @Test
    @Order(5)
    public void testFindByStatus() {
        List<Meeting> scheduledMeetings = meetingController.findByStatus("scheduled");
        
        assertNotNull(scheduledMeetings, "Scheduled meetings list should not be null");
        assertTrue(scheduledMeetings.stream()
            .anyMatch(m -> m.getMeetingId().equals(testMeetingId)));
    }

    @Test
    @Order(6)
    public void testFindUpcomingMeetings() {
        List<Meeting> upcomingMeetings = meetingController.findUpcomingMeetings(testUser1.getUserId());
        
        assertNotNull(upcomingMeetings, "Upcoming meetings list should not be null");
        // The meeting should be upcoming since it's scheduled for 2 days from now
        assertTrue(upcomingMeetings.stream()
            .anyMatch(m -> m.getMeetingId().equals(testMeetingId)));
    }

    @Test
    @Order(7)
    public void testFindByType() {
        List<Meeting> physicalMeetings = meetingController.findByType("physical");
        
        assertNotNull(physicalMeetings, "Physical meetings list should not be null");
        assertTrue(physicalMeetings.stream()
            .anyMatch(m -> m.getMeetingId().equals(testMeetingId)));
    }

    @Test
    @Order(8)
    public void testUpdateMeeting() {
        Optional<Meeting> meetingOpt = meetingController.findById(testMeetingId);
        assertTrue(meetingOpt.isPresent(), "Meeting should exist");
        
        Meeting meeting = meetingOpt.get();
        meeting.setDuration(90);
        meeting.setLocation("New Location: Library, Tunis");
        
        Meeting updated = meetingController.update(meeting);
        
        assertNotNull(updated, "Meeting should be updated");
        assertEquals(90, updated.getDuration());
        assertEquals("New Location: Library, Tunis", updated.getLocation());
    }

    @Test
    @Order(9)
    public void testUpdateMeetingStatus() {
        boolean statusUpdated = meetingController.updateStatus(testMeetingId, "in_progress");
        
        assertTrue(statusUpdated, "Meeting status should be updated");
        
        Optional<Meeting> updatedMeeting = meetingController.findById(testMeetingId);
        assertTrue(updatedMeeting.isPresent());
        assertEquals("in_progress", updatedMeeting.get().getStatus());
    }

    @Test
    @Order(10)
    public void testCompleteMeeting() {
        boolean completed = meetingController.completeMeeting(testMeetingId);
        
        assertTrue(completed, "Meeting should be completed successfully");
        
        Optional<Meeting> completedMeeting = meetingController.findById(testMeetingId);
        assertTrue(completedMeeting.isPresent());
        assertEquals("completed", completedMeeting.get().getStatus());
    }

    @Test
    @Order(11)
    public void testCreateVirtualMeeting() {
        Meeting virtualMeeting = new Meeting();
        virtualMeeting.setConnectionId(testConnectionId);
        virtualMeeting.setOrganizerId(testUser1.getUserId());
        virtualMeeting.setMeetingType("virtual");
        virtualMeeting.setLocation("https://meet.ghrami.tn/room123");
        virtualMeeting.setScheduledAt(LocalDateTime.now().plusDays(3));
        virtualMeeting.setDuration(45);
        virtualMeeting.setStatus("scheduled");

        Meeting created = meetingController.create(virtualMeeting);
        
        assertNotNull(created, "Virtual meeting should be created");
        assertEquals("virtual", created.getMeetingType());
        assertTrue(created.getLocation().startsWith("https://"));
        
        // Test cancel this virtual meeting
        boolean cancelled = meetingController.cancelMeeting(created.getMeetingId());
        assertTrue(cancelled, "Virtual meeting should be cancelled successfully");
        
        Optional<Meeting> cancelledMeeting = meetingController.findById(created.getMeetingId());
        assertTrue(cancelledMeeting.isPresent());
        assertEquals("cancelled", cancelledMeeting.get().getStatus());
    }

    @Test
    @Order(12)
    public void testFindPastMeetings() {
        // Create a past meeting
        Meeting pastMeeting = new Meeting();
        pastMeeting.setConnectionId(testConnectionId);
        pastMeeting.setOrganizerId(testUser1.getUserId());
        pastMeeting.setMeetingType("physical");
        pastMeeting.setLocation("Old Location");
        pastMeeting.setScheduledAt(LocalDateTime.now().minusDays(5));
        pastMeeting.setDuration(30);
        pastMeeting.setStatus("completed");
        
        Meeting created = meetingController.create(pastMeeting);
        assertNotNull(created, "Past meeting should be created");
        
        List<Meeting> pastMeetings = meetingController.findPastMeetings(testUser1.getUserId());
        
        assertNotNull(pastMeetings, "Past meetings list should not be null");
        assertTrue(pastMeetings.stream()
            .anyMatch(m -> m.getMeetingId().equals(created.getMeetingId())));
    }

    @Test
    @Order(13)
    public void testFindAllMeetings() {
        List<Meeting> allMeetings = meetingController.findAll();
        
        assertNotNull(allMeetings, "Meetings list should not be null");
        assertFalse(allMeetings.isEmpty(), "Should have at least one meeting");
    }

    @Test
    @Order(14)
    public void testCreateMultipleMeetingsForConnection() {
        // Test that a connection can have multiple meetings
        Meeting meeting1 = new Meeting();
        meeting1.setConnectionId(testConnectionId);
        meeting1.setOrganizerId(testUser1.getUserId());
        meeting1.setMeetingType("physical");
        meeting1.setLocation("First Meeting Place");
        meeting1.setScheduledAt(LocalDateTime.now().plusDays(7));
        meeting1.setDuration(60);
        meeting1.setStatus("scheduled");
        
        Meeting meeting2 = new Meeting();
        meeting2.setConnectionId(testConnectionId);
        meeting2.setOrganizerId(testUser2.getUserId());
        meeting2.setMeetingType("virtual");
        meeting2.setLocation("https://meeting2.url");
        meeting2.setScheduledAt(LocalDateTime.now().plusDays(14));
        meeting2.setDuration(90);
        meeting2.setStatus("scheduled");
        
        Meeting created1 = meetingController.create(meeting1);
        Meeting created2 = meetingController.create(meeting2);
        
        assertNotNull(created1, "First meeting should be created");
        assertNotNull(created2, "Second meeting should be created");
        
        List<Meeting> connectionMeetings = meetingController.findByConnection(testConnectionId);
        assertTrue(connectionMeetings.size() >= 2, "Connection should have multiple meetings");
    }

    @Test
    @Order(15)
    public void testDeleteMeeting() {
        // Create a meeting to delete
        Meeting tempMeeting = new Meeting();
        tempMeeting.setConnectionId(testConnectionId);
        tempMeeting.setOrganizerId(testUser1.getUserId());
        tempMeeting.setMeetingType("physical");
        tempMeeting.setLocation("Temp Location");
        tempMeeting.setScheduledAt(LocalDateTime.now().plusDays(1));
        tempMeeting.setDuration(30);
        tempMeeting.setStatus("scheduled");
        
        Meeting created = meetingController.create(tempMeeting);
        assertNotNull(created, "Temporary meeting should be created");
        String tempId = created.getMeetingId();
        
        // Delete it
        boolean deleted = meetingController.delete(tempId);
        assertTrue(deleted, "Meeting should be deleted successfully");
        
        // Verify deletion
        Optional<Meeting> deletedMeeting = meetingController.findById(tempId);
        assertFalse(deletedMeeting.isPresent(), "Deleted meeting should not be found");
    }

    @Test
    @Order(16)
    public void testMeetingDurationValidation() {
        // Test various duration values
        Meeting shortMeeting = new Meeting();
        shortMeeting.setConnectionId(testConnectionId);
        shortMeeting.setOrganizerId(testUser1.getUserId());
        shortMeeting.setMeetingType("virtual");
        shortMeeting.setLocation("https://short.meet");
        shortMeeting.setScheduledAt(LocalDateTime.now().plusHours(1));
        shortMeeting.setDuration(15); // 15 minutes
        shortMeeting.setStatus("scheduled");
        
        Meeting created = meetingController.create(shortMeeting);
        assertNotNull(created, "Short duration meeting should be created");
        assertEquals(15, created.getDuration());
        
        Meeting longMeeting = new Meeting();
        longMeeting.setConnectionId(testConnectionId);
        longMeeting.setOrganizerId(testUser1.getUserId());
        longMeeting.setMeetingType("physical");
        longMeeting.setLocation("Workshop Room");
        longMeeting.setScheduledAt(LocalDateTime.now().plusDays(1));
        longMeeting.setDuration(240); // 4 hours
        longMeeting.setStatus("scheduled");
        
        Meeting createdLong = meetingController.create(longMeeting);
        assertNotNull(createdLong, "Long duration meeting should be created");
        assertEquals(240, createdLong.getDuration());
    }

    @AfterAll
    public void tearDown() {
        // Clean up test data
        try {
            if (testMeetingId != null) {
                meetingController.delete(testMeetingId);
            }
            
            if (testConnectionId != null) {
                // Delete all meetings for this connection first
                List<Meeting> connectionMeetings = meetingController.findByConnection(testConnectionId);
                for (Meeting meeting : connectionMeetings) {
                    meetingController.delete(meeting.getMeetingId());
                }
                connectionController.delete(testConnectionId);
            }
            
            if (testUser1 != null && testUser1.getUserId() != null) {
                userController.delete(testUser1.getUserId());
            }
            if (testUser2 != null && testUser2.getUserId() != null) {
                userController.delete(testUser2.getUserId());
            }
            
            System.out.println("MeetingController test cleanup completed");
        } catch (Exception e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }
}

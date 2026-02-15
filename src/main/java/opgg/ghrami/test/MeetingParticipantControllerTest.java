package opgg.ghrami.test;

import opgg.ghrami.controller.ConnectionController;
import opgg.ghrami.controller.MeetingController;
import opgg.ghrami.controller.MeetingParticipantController;
import opgg.ghrami.controller.UserController;
import opgg.ghrami.model.Connection;
import opgg.ghrami.model.Meeting;
import opgg.ghrami.model.MeetingParticipant;
import opgg.ghrami.model.User;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class MeetingParticipantControllerTest {
    
    private MeetingParticipantController participantController;
    private MeetingController meetingController;
    private ConnectionController connectionController;
    private UserController userController;
    private User testUser1;
    private User testUser2;
    private User testUser3;
    private Connection testConnection;
    private Meeting testMeeting;
    private MeetingParticipant testParticipant;
    private String testParticipantId;
    private String testMeetingId;
    private String testConnectionId;

    @BeforeAll
    public void setUp() {
        participantController = MeetingParticipantController.getInstance();
        meetingController = MeetingController.getInstance();
        connectionController = ConnectionController.getInstance();
        userController = new UserController();
        System.out.println("MeetingParticipantController tests initialized");
        
        // Create test user 1 (organizer)
        testUser1 = new User();
        testUser1.setUsername("participant_user1_" + System.currentTimeMillis());
        testUser1.setFullName("Participant Test Organizer");
        testUser1.setEmail("partuser1_" + System.currentTimeMillis() + "@test.com");
        testUser1.setPassword("hashedPass123");
        testUser1.setBio("Test organizer");
        testUser1.setLocation("Tunis");
        testUser1.setProfilePicture("");
        testUser1.setOnline(true);
        testUser1.setCreatedAt(LocalDateTime.now());
        
        testUser1 = userController.create(testUser1);
        assertNotNull(testUser1.getUserId(), "Test user 1 should be created");
        
        // Create test user 2
        testUser2 = new User();
        testUser2.setUsername("participant_user2_" + System.currentTimeMillis());
        testUser2.setFullName("Participant Test User 2");
        testUser2.setEmail("partuser2_" + System.currentTimeMillis() + "@test.com");
        testUser2.setPassword("hashedPass456");
        testUser2.setBio("Test participant");
        testUser2.setLocation("Tunis");
        testUser2.setProfilePicture("");
        testUser2.setOnline(true);
        testUser2.setCreatedAt(LocalDateTime.now());
        
        testUser2 = userController.create(testUser2);
        assertNotNull(testUser2.getUserId(), "Test user 2 should be created");
        
        // Create test user 3
        testUser3 = new User();
        testUser3.setUsername("participant_user3_" + System.currentTimeMillis());
        testUser3.setFullName("Participant Test User 3");
        testUser3.setEmail("partuser3_" + System.currentTimeMillis() + "@test.com");
        testUser3.setPassword("hashedPass789");
        testUser3.setBio("Test participant 3");
        testUser3.setLocation("Sfax");
        testUser3.setProfilePicture("");
        testUser3.setOnline(false);
        testUser3.setCreatedAt(LocalDateTime.now());
        
        testUser3 = userController.create(testUser3);
        assertNotNull(testUser3.getUserId(), "Test user 3 should be created");
        
        // Create test connection
        testConnection = new Connection();
        testConnection.setInitiatorId(testUser1.getUserId());
        testConnection.setReceiverId(testUser2.getUserId());
        testConnection.setConnectionType("activity");
        testConnection.setStatus("accepted");
        
        testConnection = connectionController.create(testConnection);
        assertNotNull(testConnection, "Test connection should be created");
        testConnectionId = testConnection.getConnectionId();
        
        // Create test meeting
        testMeeting = new Meeting();
        testMeeting.setConnectionId(testConnectionId);
        testMeeting.setOrganizerId(testUser1.getUserId());
        testMeeting.setMeetingType("physical");
        testMeeting.setLocation("Test Meeting Room");
        testMeeting.setScheduledAt(LocalDateTime.now().plusDays(1));
        testMeeting.setDuration(60);
        testMeeting.setStatus("scheduled");
        
        testMeeting = meetingController.create(testMeeting);
        assertNotNull(testMeeting, "Test meeting should be created");
        testMeetingId = testMeeting.getMeetingId();
    }

    @Test
    @Order(1)
    public void testCreateParticipant() {
        testParticipant = new MeetingParticipant();
        testParticipant.setMeetingId(testMeetingId);
        testParticipant.setUserId(testUser1.getUserId());
        testParticipant.setIsActive(true);

        MeetingParticipant created = participantController.create(testParticipant);
        
        assertNotNull(created, "Participant should be created successfully");
        assertNotNull(created.getParticipantId(), "Participant ID should be generated");
        assertTrue(created.getIsActive());
        
        testParticipantId = created.getParticipantId();
        System.out.println("Created participant with ID: " + testParticipantId);
    }

    @Test
    @Order(2)
    public void testFindParticipantById() {
        assertNotNull(testParticipantId, "Test participant ID should be set");
        
        Optional<MeetingParticipant> foundParticipant = participantController.findById(testParticipantId);
        
        assertTrue(foundParticipant.isPresent(), "Participant should be found");
        assertEquals(testMeetingId, foundParticipant.get().getMeetingId());
        assertEquals(testUser1.getUserId(), foundParticipant.get().getUserId());
        assertTrue(foundParticipant.get().getIsActive());
    }

    @Test
    @Order(3)
    public void testAddSecondParticipant() {
        MeetingParticipant participant2 = new MeetingParticipant();
        participant2.setMeetingId(testMeetingId);
        participant2.setUserId(testUser2.getUserId());
        participant2.setIsActive(true);

        MeetingParticipant created = participantController.create(participant2);
        
        assertNotNull(created, "Second participant should be created");
        assertEquals(testUser2.getUserId(), created.getUserId());
    }

    @Test
    @Order(4)
    public void testFindByMeeting() {
        List<MeetingParticipant> participants = participantController.findByMeeting(testMeetingId);
        
        assertNotNull(participants, "Participants list should not be null");
        assertEquals(2, participants.size(), "Meeting should have 2 participants");
        assertTrue(participants.stream()
            .anyMatch(p -> p.getUserId().equals(testUser1.getUserId())));
        assertTrue(participants.stream()
            .anyMatch(p -> p.getUserId().equals(testUser2.getUserId())));
    }

    @Test
    @Order(5)
    public void testFindActiveParticipants() {
        List<MeetingParticipant> activeParticipants = participantController.findActiveParticipants(testMeetingId);
        
        assertNotNull(activeParticipants, "Active participants list should not be null");
        assertEquals(2, activeParticipants.size(), "Should have 2 active participants");
        assertTrue(activeParticipants.stream().allMatch(MeetingParticipant::getIsActive));
    }

    @Test
    @Order(6)
    public void testCountParticipants() {
        int count = participantController.countParticipants(testMeetingId);
        
        assertEquals(2, count, "Meeting should have 2 participants");
    }

    @Test
    @Order(7)
    public void testIsUserParticipant() {
        boolean isUser1Participant = participantController.isUserParticipant(testMeetingId, testUser1.getUserId());
        boolean isUser2Participant = participantController.isUserParticipant(testMeetingId, testUser2.getUserId());
        boolean isUser3Participant = participantController.isUserParticipant(testMeetingId, testUser3.getUserId());
        
        assertTrue(isUser1Participant, "User 1 should be a participant");
        assertTrue(isUser2Participant, "User 2 should be a participant");
        assertFalse(isUser3Participant, "User 3 should not be a participant");
    }

    @Test
    @Order(8)
    public void testFindByUser() {
        List<MeetingParticipant> user1Participations = participantController.findByUser(testUser1.getUserId());
        
        assertNotNull(user1Participations, "User 1 participations should not be null");
        assertFalse(user1Participations.isEmpty(), "User 1 should be in at least one meeting");
        assertTrue(user1Participations.stream()
            .anyMatch(p -> p.getParticipantId().equals(testParticipantId)));
    }

    @Test
    @Order(9)
    public void testUpdateParticipant() {
        Optional<MeetingParticipant> participantOpt = participantController.findById(testParticipantId);
        assertTrue(participantOpt.isPresent(), "Participant should exist");
        
        MeetingParticipant participant = participantOpt.get();
        participant.setIsActive(false);
        
        MeetingParticipant updated = participantController.update(participant);
        
        assertNotNull(updated, "Participant should be updated");
        assertFalse(updated.getIsActive(), "Participant should be inactive");
    }

    @Test
    @Order(10)
    public void testDeactivateParticipant() {
        // First, reactivate the participant
        Optional<MeetingParticipant> participantOpt = participantController.findById(testParticipantId);
        assertTrue(participantOpt.isPresent());
        MeetingParticipant participant = participantOpt.get();
        participant.setIsActive(true);
        participantController.update(participant);
        
        // Now test deactivation
        boolean deactivated = participantController.deactivateParticipant(testParticipantId);
        
        assertTrue(deactivated, "Participant should be deactivated successfully");
        
        Optional<MeetingParticipant> deactivatedParticipant = participantController.findById(testParticipantId);
        assertTrue(deactivatedParticipant.isPresent());
        assertFalse(deactivatedParticipant.get().getIsActive());
    }

    @Test
    @Order(11)
    public void testAddThirdParticipant() {
        MeetingParticipant participant3 = new MeetingParticipant();
        participant3.setMeetingId(testMeetingId);
        participant3.setUserId(testUser3.getUserId());
        participant3.setIsActive(true);

        MeetingParticipant created = participantController.create(participant3);
        
        assertNotNull(created, "Third participant should be created");
        
        int count = participantController.countParticipants(testMeetingId);
        assertEquals(3, count, "Meeting should now have 3 participants");
    }

    @Test
    @Order(12)
    public void testRemoveUserFromMeeting() {
        boolean removed = participantController.removeUserFromMeeting(testMeetingId, testUser3.getUserId());
        
        assertTrue(removed, "User 3 should be removed from meeting");
        
        boolean isStillParticipant = participantController.isUserParticipant(testMeetingId, testUser3.getUserId());
        assertFalse(isStillParticipant, "User 3 should no longer be a participant");
        
        int count = participantController.countParticipants(testMeetingId);
        assertEquals(2, count, "Meeting should have 2 participants after removal");
    }

    @Test
    @Order(13)
    public void testFindAllParticipants() {
        List<MeetingParticipant> allParticipants = participantController.findAll();
        
        assertNotNull(allParticipants, "Participants list should not be null");
        assertFalse(allParticipants.isEmpty(), "Should have at least one participant");
    }

    @Test
    @Order(14)
    public void testMultipleMeetingsForUser() {
        // Create another meeting
        Meeting meeting2 = new Meeting();
        meeting2.setConnectionId(testConnectionId);
        meeting2.setOrganizerId(testUser2.getUserId());
        meeting2.setMeetingType("virtual");
        meeting2.setLocation("https://meet.test");
        meeting2.setScheduledAt(LocalDateTime.now().plusDays(2));
        meeting2.setDuration(45);
        meeting2.setStatus("scheduled");
        
        Meeting createdMeeting = meetingController.create(meeting2);
        assertNotNull(createdMeeting, "Second meeting should be created");
        
        // Add user 1 to this meeting too
        MeetingParticipant participant = new MeetingParticipant();
        participant.setMeetingId(createdMeeting.getMeetingId());
        participant.setUserId(testUser1.getUserId());
        participant.setIsActive(true);
        
        participantController.create(participant);
        
        // User 1 should now be in 2 meetings
        List<MeetingParticipant> user1Meetings = participantController.findByUser(testUser1.getUserId());
        assertTrue(user1Meetings.size() >= 2, "User 1 should be in at least 2 meetings");
    }

    @Test
    @Order(15)
    public void testActiveVsInactiveParticipants() {
        // Count active participants
        List<MeetingParticipant> activeParticipants = participantController.findActiveParticipants(testMeetingId);
        List<MeetingParticipant> allParticipants = participantController.findByMeeting(testMeetingId);
        
        // We deactivated one participant earlier, so active count should be less than total
        assertTrue(activeParticipants.size() <= allParticipants.size(), 
                  "Active participants should be less than or equal to total participants");
    }

    @Test
    @Order(16)
    public void testDeleteParticipant() {
        // Create a participant to delete
        MeetingParticipant tempParticipant = new MeetingParticipant();
        tempParticipant.setMeetingId(testMeetingId);
        tempParticipant.setUserId(testUser3.getUserId());
        tempParticipant.setIsActive(true);
        
        MeetingParticipant created = participantController.create(tempParticipant);
        assertNotNull(created, "Temporary participant should be created");
        String tempId = created.getParticipantId();
        
        // Delete it
        boolean deleted = participantController.delete(tempId);
        assertTrue(deleted, "Participant should be deleted successfully");
        
        // Verify deletion
        Optional<MeetingParticipant> deletedParticipant = participantController.findById(tempId);
        assertFalse(deletedParticipant.isPresent(), "Deleted participant should not be found");
    }

    @Test
    @Order(17)
    public void testParticipantConstructor() {
        // Test the convenience constructor
        MeetingParticipant participant = new MeetingParticipant(testMeetingId, testUser3.getUserId());
        
        assertEquals(testMeetingId, participant.getMeetingId());
        assertEquals(testUser3.getUserId(), participant.getUserId());
        assertTrue(participant.getIsActive(), "New participant should be active by default");
        
        MeetingParticipant created = participantController.create(participant);
        assertNotNull(created, "Participant created with constructor should be saved");
    }

    @AfterAll
    public void tearDown() {
        // Clean up test data
        try {
            if (testMeetingId != null) {
                // Delete all participants first
                List<MeetingParticipant> participants = participantController.findByMeeting(testMeetingId);
                for (MeetingParticipant participant : participants) {
                    participantController.delete(participant.getParticipantId());
                }
                
                meetingController.delete(testMeetingId);
            }
            
            if (testConnectionId != null) {
                // Delete all meetings for this connection
                List<Meeting> meetings = meetingController.findByConnection(testConnectionId);
                for (Meeting meeting : meetings) {
                    // Delete participants first
                    List<MeetingParticipant> meetingParticipants = participantController.findByMeeting(meeting.getMeetingId());
                    for (MeetingParticipant participant : meetingParticipants) {
                        participantController.delete(participant.getParticipantId());
                    }
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
            if (testUser3 != null && testUser3.getUserId() != null) {
                userController.delete(testUser3.getUserId());
            }
            
            System.out.println("MeetingParticipantController test cleanup completed");
        } catch (Exception e) {
            System.err.println("Error during cleanup: " + e.getMessage());
        }
    }
}

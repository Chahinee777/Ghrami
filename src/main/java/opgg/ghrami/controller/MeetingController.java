package opgg.ghrami.controller;

import opgg.ghrami.model.Meeting;
import opgg.ghrami.util.DatabaseConnection;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MeetingController {
    private static MeetingController instance;

    private MeetingController() {}

    public static synchronized MeetingController getInstance() {
        if (instance == null) {
            instance = new MeetingController();
        }
        return instance;
    }

    public Meeting create(Meeting meeting) {
        String sql = "INSERT INTO meetings (meeting_id, connection_id, organizer_id, meeting_type, location, scheduled_at, duration, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (java.sql.Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            String meetingId = UUID.randomUUID().toString();
            meeting.setMeetingId(meetingId);

            stmt.setString(1, meetingId);
            stmt.setString(2, meeting.getConnectionId());
            stmt.setLong(3, meeting.getOrganizerId());
            stmt.setString(4, meeting.getMeetingType());
            stmt.setString(5, meeting.getLocation());
            stmt.setTimestamp(6, Timestamp.valueOf(meeting.getScheduledAt()));
            stmt.setInt(7, meeting.getDuration());
            stmt.setString(8, meeting.getStatus());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("Meeting created: " + meetingId);
                return meeting;
            }
        } catch (SQLException e) {
            System.err.println("Error creating meeting: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public Optional<Meeting> findById(String meetingId) {
        String sql = "SELECT * FROM meetings WHERE meeting_id = ?";

        try (java.sql.Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, meetingId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToMeeting(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding meeting by ID: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public List<Meeting> findAll() {
        List<Meeting> meetings = new ArrayList<>();
        String sql = "SELECT * FROM meetings ORDER BY scheduled_at DESC";

        try (java.sql.Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                meetings.add(mapResultSetToMeeting(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding all meetings: " + e.getMessage());
            e.printStackTrace();
        }
        return meetings;
    }

    public List<Meeting> findByConnection(String connectionId) {
        List<Meeting> meetings = new ArrayList<>();
        String sql = "SELECT * FROM meetings WHERE connection_id = ? ORDER BY scheduled_at DESC";

        try (java.sql.Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, connectionId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                meetings.add(mapResultSetToMeeting(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding meetings by connection: " + e.getMessage());
            e.printStackTrace();
        }
        return meetings;
    }

    public List<Meeting> findByOrganizer(Long organizerId) {
        List<Meeting> meetings = new ArrayList<>();
        String sql = "SELECT * FROM meetings WHERE organizer_id = ? ORDER BY scheduled_at DESC";

        try (java.sql.Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, organizerId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                meetings.add(mapResultSetToMeeting(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding meetings by organizer: " + e.getMessage());
            e.printStackTrace();
        }
        return meetings;
    }

    public List<Meeting> findByStatus(String status) {
        List<Meeting> meetings = new ArrayList<>();
        String sql = "SELECT * FROM meetings WHERE status = ? ORDER BY scheduled_at DESC";

        try (java.sql.Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                meetings.add(mapResultSetToMeeting(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding meetings by status: " + e.getMessage());
            e.printStackTrace();
        }
        return meetings;
    }

    public List<Meeting> findUpcomingMeetings(Long userId) {
        List<Meeting> meetings = new ArrayList<>();
        String sql = "SELECT m.* FROM meetings m " +
                     "JOIN meeting_participants mp ON m.meeting_id = mp.meeting_id " +
                     "WHERE mp.user_id = ? AND m.scheduled_at > NOW() AND m.status = 'scheduled' " +
                     "ORDER BY m.scheduled_at ASC";

        try (java.sql.Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                meetings.add(mapResultSetToMeeting(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding upcoming meetings: " + e.getMessage());
            e.printStackTrace();
        }
        return meetings;
    }

    public List<Meeting> findPastMeetings(Long userId) {
        List<Meeting> meetings = new ArrayList<>();
        String sql = "SELECT m.* FROM meetings m " +
                     "JOIN meeting_participants mp ON m.meeting_id = mp.meeting_id " +
                     "WHERE mp.user_id = ? AND (m.scheduled_at < NOW() OR m.status = 'completed') " +
                     "ORDER BY m.scheduled_at DESC";

        try (java.sql.Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                meetings.add(mapResultSetToMeeting(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding past meetings: " + e.getMessage());
            e.printStackTrace();
        }
        return meetings;
    }

    public List<Meeting> findByType(String meetingType) {
        List<Meeting> meetings = new ArrayList<>();
        String sql = "SELECT * FROM meetings WHERE meeting_type = ? ORDER BY scheduled_at DESC";

        try (java.sql.Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, meetingType);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                meetings.add(mapResultSetToMeeting(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding meetings by type: " + e.getMessage());
            e.printStackTrace();
        }
        return meetings;
    }

    public Meeting update(Meeting meeting) {
        String sql = "UPDATE meetings SET connection_id = ?, organizer_id = ?, meeting_type = ?, " +
                     "location = ?, scheduled_at = ?, duration = ?, status = ? WHERE meeting_id = ?";

        try (java.sql.Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, meeting.getConnectionId());
            stmt.setLong(2, meeting.getOrganizerId());
            stmt.setString(3, meeting.getMeetingType());
            stmt.setString(4, meeting.getLocation());
            stmt.setTimestamp(5, Timestamp.valueOf(meeting.getScheduledAt()));
            stmt.setInt(6, meeting.getDuration());
            stmt.setString(7, meeting.getStatus());
            stmt.setString(8, meeting.getMeetingId());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("Meeting updated: " + meeting.getMeetingId());
                return meeting;
            }
        } catch (SQLException e) {
            System.err.println("Error updating meeting: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public boolean updateStatus(String meetingId, String status) {
        String sql = "UPDATE meetings SET status = ? WHERE meeting_id = ?";

        try (java.sql.Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, status);
            stmt.setString(2, meetingId);
            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("Meeting status updated: " + meetingId + " -> " + status);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error updating meeting status: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean cancelMeeting(String meetingId) {
        return updateStatus(meetingId, "cancelled");
    }

    public boolean completeMeeting(String meetingId) {
        return updateStatus(meetingId, "completed");
    }

    public boolean delete(String meetingId) {
        String sql = "DELETE FROM meetings WHERE meeting_id = ?";

        try (java.sql.Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, meetingId);
            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("Meeting deleted: " + meetingId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error deleting meeting: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    private Meeting mapResultSetToMeeting(ResultSet rs) throws SQLException {
        Meeting meeting = new Meeting();
        meeting.setMeetingId(rs.getString("meeting_id"));
        meeting.setConnectionId(rs.getString("connection_id"));
        meeting.setOrganizerId(rs.getLong("organizer_id"));
        meeting.setMeetingType(rs.getString("meeting_type"));
        meeting.setLocation(rs.getString("location"));
        
        Timestamp scheduledAt = rs.getTimestamp("scheduled_at");
        if (scheduledAt != null) {
            meeting.setScheduledAt(scheduledAt.toLocalDateTime());
        }
        
        meeting.setDuration(rs.getInt("duration"));
        meeting.setStatus(rs.getString("status"));
        return meeting;
    }
}

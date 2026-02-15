package opgg.ghrami.controller;

import opgg.ghrami.model.MeetingParticipant;
import opgg.ghrami.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class MeetingParticipantController {
    private static MeetingParticipantController instance;

    private MeetingParticipantController() {}

    public static synchronized MeetingParticipantController getInstance() {
        if (instance == null) {
            instance = new MeetingParticipantController();
        }
        return instance;
    }

    public MeetingParticipant create(MeetingParticipant participant) {
        String sql = "INSERT INTO meeting_participants (participant_id, meeting_id, user_id, is_active) VALUES (?, ?, ?, ?)";

        try (java.sql.Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            String participantId = UUID.randomUUID().toString();
            participant.setParticipantId(participantId);

            stmt.setString(1, participantId);
            stmt.setString(2, participant.getMeetingId());
            stmt.setLong(3, participant.getUserId());
            stmt.setBoolean(4, participant.getIsActive());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("Meeting participant added: " + participantId);
                return participant;
            }
        } catch (SQLException e) {
            System.err.println("Error adding meeting participant: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public Optional<MeetingParticipant> findById(String participantId) {
        String sql = "SELECT * FROM meeting_participants WHERE participant_id = ?";

        try (java.sql.Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, participantId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return Optional.of(mapResultSetToParticipant(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding participant by ID: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public List<MeetingParticipant> findAll() {
        List<MeetingParticipant> participants = new ArrayList<>();
        String sql = "SELECT * FROM meeting_participants";

        try (java.sql.Connection conn = DatabaseConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                participants.add(mapResultSetToParticipant(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding all participants: " + e.getMessage());
            e.printStackTrace();
        }
        return participants;
    }

    public List<MeetingParticipant> findByMeeting(String meetingId) {
        List<MeetingParticipant> participants = new ArrayList<>();
        String sql = "SELECT * FROM meeting_participants WHERE meeting_id = ?";

        try (java.sql.Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, meetingId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                participants.add(mapResultSetToParticipant(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding participants by meeting: " + e.getMessage());
            e.printStackTrace();
        }
        return participants;
    }

    public List<MeetingParticipant> findActiveParticipants(String meetingId) {
        List<MeetingParticipant> participants = new ArrayList<>();
        String sql = "SELECT * FROM meeting_participants WHERE meeting_id = ? AND is_active = TRUE";

        try (java.sql.Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, meetingId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                participants.add(mapResultSetToParticipant(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding active participants: " + e.getMessage());
            e.printStackTrace();
        }
        return participants;
    }

    public List<MeetingParticipant> findByUser(Long userId) {
        List<MeetingParticipant> participants = new ArrayList<>();
        String sql = "SELECT * FROM meeting_participants WHERE user_id = ?";

        try (java.sql.Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setLong(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                participants.add(mapResultSetToParticipant(rs));
            }
        } catch (SQLException e) {
            System.err.println("Error finding participants by user: " + e.getMessage());
            e.printStackTrace();
        }
        return participants;
    }

    public int countParticipants(String meetingId) {
        String sql = "SELECT COUNT(*) FROM meeting_participants WHERE meeting_id = ? AND is_active = TRUE";

        try (java.sql.Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, meetingId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            System.err.println("Error counting participants: " + e.getMessage());
            e.printStackTrace();
        }
        return 0;
    }

    public boolean isUserParticipant(String meetingId, Long userId) {
        String sql = "SELECT COUNT(*) FROM meeting_participants WHERE meeting_id = ? AND user_id = ? AND is_active = TRUE";

        try (java.sql.Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, meetingId);
            stmt.setLong(2, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            System.err.println("Error checking participant: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public MeetingParticipant update(MeetingParticipant participant) {
        String sql = "UPDATE meeting_participants SET meeting_id = ?, user_id = ?, is_active = ? WHERE participant_id = ?";

        try (java.sql.Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, participant.getMeetingId());
            stmt.setLong(2, participant.getUserId());
            stmt.setBoolean(3, participant.getIsActive());
            stmt.setString(4, participant.getParticipantId());

            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("Participant updated: " + participant.getParticipantId());
                return participant;
            }
        } catch (SQLException e) {
            System.err.println("Error updating participant: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }

    public boolean deactivateParticipant(String participantId) {
        String sql = "UPDATE meeting_participants SET is_active = FALSE WHERE participant_id = ?";

        try (java.sql.Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, participantId);
            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("Participant deactivated: " + participantId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error deactivating participant: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean removeUserFromMeeting(String meetingId, Long userId) {
        String sql = "UPDATE meeting_participants SET is_active = FALSE WHERE meeting_id = ? AND user_id = ?";

        try (java.sql.Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, meetingId);
            stmt.setLong(2, userId);
            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("User removed from meeting: " + userId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error removing user from meeting: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    public boolean delete(String participantId) {
        String sql = "DELETE FROM meeting_participants WHERE participant_id = ?";

        try (java.sql.Connection conn = DatabaseConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, participantId);
            int affectedRows = stmt.executeUpdate();

            if (affectedRows > 0) {
                System.out.println("Participant deleted: " + participantId);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Error deleting participant: " + e.getMessage());
            e.printStackTrace();
        }
        return false;
    }

    private MeetingParticipant mapResultSetToParticipant(ResultSet rs) throws SQLException {
        MeetingParticipant participant = new MeetingParticipant();
        participant.setParticipantId(rs.getString("participant_id"));
        participant.setMeetingId(rs.getString("meeting_id"));
        participant.setUserId(rs.getLong("user_id"));
        participant.setIsActive(rs.getBoolean("is_active"));
        return participant;
    }
}

package opgg.ghrami.model;

public class MeetingParticipant {
    private String participantId;
    private String meetingId;
    private Long userId;
    private Boolean isActive;

    // Constructors
    public MeetingParticipant() {
        this.isActive = true;
    }

    public MeetingParticipant(String meetingId, Long userId) {
        this.meetingId = meetingId;
        this.userId = userId;
        this.isActive = true;
    }

    // Getters and Setters
    public String getParticipantId() {
        return participantId;
    }

    public void setParticipantId(String participantId) {
        this.participantId = participantId;
    }

    public String getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(String meetingId) {
        this.meetingId = meetingId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    @Override
    public String toString() {
        return "MeetingParticipant{" +
                "participantId='" + participantId + '\'' +
                ", meetingId='" + meetingId + '\'' +
                ", userId=" + userId +
                ", isActive=" + isActive +
                '}';
    }
}

package opgg.ghrami.Entites;

public class meetings_participants {





    private String participantId;
    private String meetingId;
    private long userId;
    private boolean isActive;


    public meetings_participants() {
    }


    public meetings_participants(String participantId, String meetingId,
                       long userId, boolean isActive) {

        this.participantId = participantId;
        this.meetingId = meetingId;
        this.userId = userId;
        this.isActive = isActive;
    }


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

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

}
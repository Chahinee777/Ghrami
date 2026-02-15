package opgg.ghrami.model;

import java.time.LocalDateTime;

public class Meeting {
    private String meetingId;
    private String connectionId;
    private Long organizerId;
    private String meetingType; // physical, virtual
    private String location;
    private LocalDateTime scheduledAt;
    private Integer duration; // in minutes
    private String status; // scheduled, in_progress, completed, cancelled

    // Constructors
    public Meeting() {}

    public Meeting(String connectionId, Long organizerId, String meetingType, 
                  String location, LocalDateTime scheduledAt, Integer duration) {
        this.connectionId = connectionId;
        this.organizerId = organizerId;
        this.meetingType = meetingType;
        this.location = location;
        this.scheduledAt = scheduledAt;
        this.duration = duration;
        this.status = "scheduled";
    }

    // Getters and Setters
    public String getMeetingId() {
        return meetingId;
    }

    public void setMeetingId(String meetingId) {
        this.meetingId = meetingId;
    }

    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public Long getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(Long organizerId) {
        this.organizerId = organizerId;
    }

    public String getMeetingType() {
        return meetingType;
    }

    public void setMeetingType(String meetingType) {
        this.meetingType = meetingType;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public void setScheduledAt(LocalDateTime scheduledAt) {
        this.scheduledAt = scheduledAt;
    }

    public Integer getDuration() {
        return duration;
    }

    public void setDuration(Integer duration) {
        this.duration = duration;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "Meeting{" +
                "meetingId='" + meetingId + '\'' +
                ", connectionId='" + connectionId + '\'' +
                ", organizerId=" + organizerId +
                ", meetingType='" + meetingType + '\'' +
                ", location='" + location + '\'' +
                ", scheduledAt=" + scheduledAt +
                ", duration=" + duration +
                ", status='" + status + '\'' +
                '}';
    }
}

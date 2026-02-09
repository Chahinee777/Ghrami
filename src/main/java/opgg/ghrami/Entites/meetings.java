package opgg.ghrami.Entites;

import java.sql.Timestamp;

public class meetings {





        private String meetingId;
        private String connectionId;
        private long organizerId;
        private String meetingType;
        private String location;
        private Timestamp scheduledAt;
        private int duration;
        private String status;


        public meetings() {
        }


        public meetings(String meetingId, String connectionId, long organizerId,
                       String meetingType, String location, Timestamp scheduledAt,
                       int duration, String status) {

            this.meetingId = meetingId;
            this.connectionId = connectionId;
            this.organizerId = organizerId;
            this.meetingType = meetingType;
            this.location = location;
            this.scheduledAt = scheduledAt;
            this.duration = duration;
            this.status = status;
        }



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

        public long getOrganizerId() {
            return organizerId;
        }

        public void setOrganizerId(long organizerId) {
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

        public Timestamp getScheduledAt() {
            return scheduledAt;
        }

        public void setScheduledAt(Timestamp scheduledAt) {
            this.scheduledAt = scheduledAt;
        }

        public int getDuration() {
            return duration;
        }

        public void setDuration(int duration) {
            this.duration = duration;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }

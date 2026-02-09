package opgg.ghrami.Entites;

public class connections {


        private String connectionId;
        private long initiatorId;
        private long receiverId;
        private String connectionType;
        private String receiverSkill;
        private String initiatorSkill;
        private String status;

        public connections() {
        }


        public connections(String connectionId, long initiatorId, long receiverId,
                          String connectionType, String receiverSkill,
                          String initiatorSkill, String status) {

            this.connectionId = connectionId;
            this.initiatorId = initiatorId;
            this.receiverId = receiverId;
            this.connectionType = connectionType;
            this.receiverSkill = receiverSkill;
            this.initiatorSkill = initiatorSkill;
            this.status = status;
        }



        public String getConnectionId() {
            return connectionId;
        }

        public void setConnectionId(String connectionId) {
            this.connectionId = connectionId;
        }

        public long getInitiatorId() {
            return initiatorId;
        }

        public void setInitiatorId(long initiatorId) {
            this.initiatorId = initiatorId;
        }

        public long getReceiverId() {
            return receiverId;
        }

        public void setReceiverId(long receiverId) {
            this.receiverId = receiverId;
        }

        public String getConnectionType() {
            return connectionType;
        }

        public void setConnectionType(String connectionType) {
            this.connectionType = connectionType;
        }

        public String getReceiverSkill() {
            return receiverSkill;
        }

        public void setReceiverSkill(String receiverSkill) {
            this.receiverSkill = receiverSkill;
        }

        public String getInitiatorSkill() {
            return initiatorSkill;
        }

        public void setInitiatorSkill(String initiatorSkill) {
            this.initiatorSkill = initiatorSkill;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

    @Override
    public String toString() {
        return "connections{" +
                "connectionId='" + connectionId + '\'' +
                ", initiatorId=" + initiatorId +
                ", receiverId=" + receiverId +
                ", connectionType='" + connectionType + '\'' +
                ", receiverSkill='" + receiverSkill + '\'' +
                ", initiatorSkill='" + initiatorSkill + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}

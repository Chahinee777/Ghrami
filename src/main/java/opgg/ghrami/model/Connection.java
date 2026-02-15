package opgg.ghrami.model;

public class Connection {
    private String connectionId;
    private Long initiatorId;
    private Long receiverId;
    private String connectionType;
    private String receiverSkill;
    private String initiatorSkill;
    private String status; // pending, accepted, rejected

    // Constructors
    public Connection() {}

    public Connection(Long initiatorId, Long receiverId, String connectionType, 
                     String receiverSkill, String initiatorSkill) {
        this.initiatorId = initiatorId;
        this.receiverId = receiverId;
        this.connectionType = connectionType;
        this.receiverSkill = receiverSkill;
        this.initiatorSkill = initiatorSkill;
        this.status = "pending";
    }

    // Getters and Setters
    public String getConnectionId() {
        return connectionId;
    }

    public void setConnectionId(String connectionId) {
        this.connectionId = connectionId;
    }

    public Long getInitiatorId() {
        return initiatorId;
    }

    public void setInitiatorId(Long initiatorId) {
        this.initiatorId = initiatorId;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public void setReceiverId(Long receiverId) {
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
        return "Connection{" +
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

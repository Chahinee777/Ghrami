package opgg.ghrami.model;

import java.time.LocalDateTime;

public class Friendship {
    private Long friendshipId;
    private Long user1Id;
    private Long user2Id;
    private FriendshipStatus status;
    private LocalDateTime createdDate;
    private LocalDateTime acceptedDate;

    // Constructors
    public Friendship() {
        this.createdDate = LocalDateTime.now();
        this.status = FriendshipStatus.PENDING;
    }

    public Friendship(Long user1Id, Long user2Id) {
        this();
        this.user1Id = user1Id;
        this.user2Id = user2Id;
    }

    // Getters and Setters
    public Long getFriendshipId() {
        return friendshipId;
    }

    public void setFriendshipId(Long friendshipId) {
        this.friendshipId = friendshipId;
    }

    public Long getUser1Id() {
        return user1Id;
    }

    public void setUser1Id(Long user1Id) {
        this.user1Id = user1Id;
    }

    public Long getUser2Id() {
        return user2Id;
    }

    public void setUser2Id(Long user2Id) {
        this.user2Id = user2Id;
    }

    public FriendshipStatus getStatus() {
        return status;
    }

    public void setStatus(FriendshipStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(LocalDateTime createdDate) {
        this.createdDate = createdDate;
    }

    public LocalDateTime getAcceptedDate() {
        return acceptedDate;
    }

    public void setAcceptedDate(LocalDateTime acceptedDate) {
        this.acceptedDate = acceptedDate;
    }

    @Override
    public String toString() {
        return "Friendship{" +
                "friendshipId=" + friendshipId +
                ", user1Id=" + user1Id +
                ", user2Id=" + user2Id +
                ", status='" + status + '\'' +
                ", createdDate=" + createdDate +
                '}';
    }
}

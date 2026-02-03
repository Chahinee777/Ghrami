package opgg.ghrami.model;

import java.time.LocalDateTime;

public class Badge {
    private Long badgeId;
    private Long userId;
    private String name;
    private String description;
    private LocalDateTime earnedDate;

    // Constructors
    public Badge() {
        this.earnedDate = LocalDateTime.now();
    }

    public Badge(Long userId, String name) {
        this();
        this.userId = userId;
        this.name = name;
    }

    public Badge(Long userId, String name, String description) {
        this();
        this.userId = userId;
        this.name = name;
        this.description = description;
    }

    // Getters and Setters
    public Long getBadgeId() {
        return badgeId;
    }

    public void setBadgeId(Long badgeId) {
        this.badgeId = badgeId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getEarnedDate() {
        return earnedDate;
    }

    public void setEarnedDate(LocalDateTime earnedDate) {
        this.earnedDate = earnedDate;
    }

    @Override
    public String toString() {
        return "Badge{" +
                "badgeId=" + badgeId +
                ", userId=" + userId +
                ", name='" + name + '\'' +
                ", earnedDate=" + earnedDate +
                '}';
    }
}

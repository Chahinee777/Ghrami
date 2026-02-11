package opgg.ghrami.model;

import java.time.LocalDate;

public class Milestone {
    private Long milestoneId;
    private Long hobbyId;
    private String title;
    private LocalDate targetDate;
    private Boolean isAchieved;

    // Constructors
    public Milestone() {
        this.isAchieved = false;
    }

    public Milestone(Long hobbyId, String title, LocalDate targetDate) {
        this.hobbyId = hobbyId;
        this.title = title;
        this.targetDate = targetDate;
        this.isAchieved = false;
    }

    // Getters and Setters
    public Long getMilestoneId() {
        return milestoneId;
    }

    public void setMilestoneId(Long milestoneId) {
        this.milestoneId = milestoneId;
    }

    public Long getHobbyId() {
        return hobbyId;
    }

    public void setHobbyId(Long hobbyId) {
        this.hobbyId = hobbyId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(LocalDate targetDate) {
        this.targetDate = targetDate;
    }

    public Boolean getIsAchieved() {
        return isAchieved;
    }

    public void setIsAchieved(Boolean isAchieved) {
        this.isAchieved = isAchieved;
    }

    @Override
    public String toString() {
        return "Milestone{" +
                "milestoneId=" + milestoneId +
                ", hobbyId=" + hobbyId +
                ", title='" + title + '\'' +
                ", targetDate=" + targetDate +
                ", isAchieved=" + isAchieved +
                '}';
    }
}

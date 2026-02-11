package opgg.ghrami.model;

public class Progress {
    private Long progressId;
    private Long hobbyId;
    private Double hoursSpent;
    private String notes;

    // Constructors
    public Progress() {
        this.hoursSpent = 0.0;
    }

    public Progress(Long hobbyId, Double hoursSpent) {
        this.hobbyId = hobbyId;
        this.hoursSpent = hoursSpent;
    }

    public Progress(Long hobbyId, Double hoursSpent, String notes) {
        this.hobbyId = hobbyId;
        this.hoursSpent = hoursSpent;
        this.notes = notes;
    }

    // Getters and Setters
    public Long getProgressId() {
        return progressId;
    }

    public void setProgressId(Long progressId) {
        this.progressId = progressId;
    }

    public Long getHobbyId() {
        return hobbyId;
    }

    public void setHobbyId(Long hobbyId) {
        this.hobbyId = hobbyId;
    }

    public Double getHoursSpent() {
        return hoursSpent;
    }

    public void setHoursSpent(Double hoursSpent) {
        this.hoursSpent = hoursSpent;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    @Override
    public String toString() {
        return "Progress{" +
                "progressId=" + progressId +
                ", hobbyId=" + hobbyId +
                ", hoursSpent=" + hoursSpent +
                '}';
    }
}

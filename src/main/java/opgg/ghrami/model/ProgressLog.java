package opgg.ghrami.model;

import java.time.LocalDate;

public class ProgressLog {
    private Long logId;
    private Long hobbyId;
    private Double hoursSpent;
    private String notes;
    private LocalDate logDate;

    public ProgressLog() {
        this.logDate = LocalDate.now();
    }

    public ProgressLog(Long hobbyId, Double hoursSpent, String notes, LocalDate logDate) {
        this.hobbyId = hobbyId;
        this.hoursSpent = hoursSpent;
        this.notes = notes;
        this.logDate = logDate != null ? logDate : LocalDate.now();
    }

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }

    public Long getHobbyId() { return hobbyId; }
    public void setHobbyId(Long hobbyId) { this.hobbyId = hobbyId; }

    public Double getHoursSpent() { return hoursSpent; }
    public void setHoursSpent(Double hoursSpent) { this.hoursSpent = hoursSpent; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDate getLogDate() { return logDate; }
    public void setLogDate(LocalDate logDate) { this.logDate = logDate; }

    @Override
    public String toString() {
        return "ProgressLog{logId=" + logId + ", hobbyId=" + hobbyId +
               ", hoursSpent=" + hoursSpent + ", logDate=" + logDate + "}";
    }
}

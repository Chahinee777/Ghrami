package opgg.ghrami.model;

/**
 * Model representing a class/course offering.
 * Named ClassEntity to avoid conflict with Java keyword "Class".
 */
public class ClassEntity {
    private Long classId;
    private Long providerId;
    private String title;
    private String description;
    private String category;
    private double price;
    private int duration; // in minutes
    private int maxParticipants;
    private String videoPath;   // optional local video file path
    private String imagePath;   // optional local thumbnail image path  ← NEW

    // Provider details (joined from class_providers table)
    private String providerName;
    private String providerCompany;
    private double providerRating;
    private boolean providerVerified;

    // Enrollment stats
    private int currentEnrollment;

    public ClassEntity() {}

    public ClassEntity(Long classId, Long providerId, String title, String description,
                       String category, double price, int duration, int maxParticipants) {
        this.classId = classId;
        this.providerId = providerId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.price = price;
        this.duration = duration;
        this.maxParticipants = maxParticipants;
    }

    // ── Getters & Setters ─────────────────────────────────────────────────────

    public Long getClassId()                          { return classId; }
    public void setClassId(Long classId)              { this.classId = classId; }

    public Long getProviderId()                       { return providerId; }
    public void setProviderId(Long providerId)        { this.providerId = providerId; }

    public String getTitle()                          { return title; }
    public void setTitle(String title)                { this.title = title; }

    public String getDescription()                    { return description; }
    public void setDescription(String description)    { this.description = description; }

    public String getCategory()                       { return category; }
    public void setCategory(String category)          { this.category = category; }

    public double getPrice()                          { return price; }
    public void setPrice(double price)                { this.price = price; }

    public int getDuration()                          { return duration; }
    public void setDuration(int duration)             { this.duration = duration; }

    public int getMaxParticipants()                   { return maxParticipants; }
    public void setMaxParticipants(int max)           { this.maxParticipants = max; }

    public String getVideoPath()                      { return videoPath; }
    public void setVideoPath(String videoPath)        { this.videoPath = videoPath; }

    /** Local path to the class thumbnail image (jpg/png). May be null. */
    public String getImagePath()                      { return imagePath; }
    public void setImagePath(String imagePath)        { this.imagePath = imagePath; }

    public String getProviderName()                   { return providerName; }
    public void setProviderName(String providerName)  { this.providerName = providerName; }

    public String getProviderCompany()                { return providerCompany; }
    public void setProviderCompany(String c)          { this.providerCompany = c; }

    public double getProviderRating()                 { return providerRating; }
    public void setProviderRating(double r)           { this.providerRating = r; }

    public boolean isProviderVerified()               { return providerVerified; }
    public void setProviderVerified(boolean v)        { this.providerVerified = v; }

    public int getCurrentEnrollment()                 { return currentEnrollment; }
    public void setCurrentEnrollment(int e)           { this.currentEnrollment = e; }

    public int getAvailableSpots()                    { return maxParticipants - currentEnrollment; }
    public boolean hasAvailableSpots()                { return currentEnrollment < maxParticipants; }

    @Override
    public String toString() {
        return "ClassEntity{classId=" + classId + ", title='" + title + '\'' +
               ", category='" + category + '\'' + ", price=" + price +
               ", imagePath='" + imagePath + '\'' + '}';
    }
}
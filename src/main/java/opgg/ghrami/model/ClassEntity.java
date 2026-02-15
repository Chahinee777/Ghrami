package opgg.ghrami.model;

/**
 * Model representing a class/course offering
 * Named ClassEntity to avoid conflict with Java keyword "Class"
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
    
    // Provider details (joined from class_providers table)
    private String providerName;
    private String providerCompany;
    private double providerRating;
    private boolean providerVerified;
    
    // Enrollment stats
    private int currentEnrollment;
    
    public ClassEntity() {
    }
    
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
    
    // Getters and Setters
    public Long getClassId() {
        return classId;
    }
    
    public void setClassId(Long classId) {
        this.classId = classId;
    }
    
    public Long getProviderId() {
        return providerId;
    }
    
    public void setProviderId(Long providerId) {
        this.providerId = providerId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public double getPrice() {
        return price;
    }
    
    public void setPrice(double price) {
        this.price = price;
    }
    
    public int getDuration() {
        return duration;
    }
    
    public void setDuration(int duration) {
        this.duration = duration;
    }
    
    public int getMaxParticipants() {
        return maxParticipants;
    }
    
    public void setMaxParticipants(int maxParticipants) {
        this.maxParticipants = maxParticipants;
    }
    
    public String getProviderName() {
        return providerName;
    }
    
    public void setProviderName(String providerName) {
        this.providerName = providerName;
    }
    
    public String getProviderCompany() {
        return providerCompany;
    }
    
    public void setProviderCompany(String providerCompany) {
        this.providerCompany = providerCompany;
    }
    
    public double getProviderRating() {
        return providerRating;
    }
    
    public void setProviderRating(double providerRating) {
        this.providerRating = providerRating;
    }
    
    public boolean isProviderVerified() {
        return providerVerified;
    }
    
    public void setProviderVerified(boolean providerVerified) {
        this.providerVerified = providerVerified;
    }
    
    public int getCurrentEnrollment() {
        return currentEnrollment;
    }
    
    public void setCurrentEnrollment(int currentEnrollment) {
        this.currentEnrollment = currentEnrollment;
    }
    
    public int getAvailableSpots() {
        return maxParticipants - currentEnrollment;
    }
    
    public boolean hasAvailableSpots() {
        return currentEnrollment < maxParticipants;
    }
    
    @Override
    public String toString() {
        return "ClassEntity{" +
                "classId=" + classId +
                ", title='" + title + '\'' +
                ", category='" + category + '\'' +
                ", price=" + price +
                ", duration=" + duration +
                ", maxParticipants=" + maxParticipants +
                ", currentEnrollment=" + currentEnrollment +
                '}';
    }
}

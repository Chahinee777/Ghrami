package opgg.ghrami.model;

/**
 * Model representing a class provider (instructor)
 */
public class ClassProvider {
    private Long providerId;
    private Long userId;
    private String companyName;
    private String expertise;
    private double rating;
    private boolean isVerified;
    
    // Full user details (joined from users table)
    private String username;
    private String fullName;
    private String email;
    private String profilePicture;
    private String bio;
    
    public ClassProvider() {
    }
    
    public ClassProvider(Long providerId, Long userId, String companyName, String expertise, 
                        double rating, boolean isVerified) {
        this.providerId = providerId;
        this.userId = userId;
        this.companyName = companyName;
        this.expertise = expertise;
        this.rating = rating;
        this.isVerified = isVerified;
    }
    
    // Getters and Setters
    public Long getProviderId() {
        return providerId;
    }
    
    public void setProviderId(Long providerId) {
        this.providerId = providerId;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public void setUserId(Long userId) {
        this.userId = userId;
    }
    
    public String getCompanyName() {
        return companyName;
    }
    
    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }
    
    public String getExpertise() {
        return expertise;
    }
    
    public void setExpertise(String expertise) {
        this.expertise = expertise;
    }
    
    public double getRating() {
        return rating;
    }
    
    public void setRating(double rating) {
        this.rating = rating;
    }
    
    public boolean isVerified() {
        return isVerified;
    }
    
    public void setVerified(boolean verified) {
        isVerified = verified;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getProfilePicture() {
        return profilePicture;
    }
    
    public void setProfilePicture(String profilePicture) {
        this.profilePicture = profilePicture;
    }
    
    public String getBio() {
        return bio;
    }
    
    public void setBio(String bio) {
        this.bio = bio;
    }
    
    @Override
    public String toString() {
        return "ClassProvider{" +
                "providerId=" + providerId +
                ", userId=" + userId +
                ", companyName='" + companyName + '\'' +
                ", expertise='" + expertise + '\'' +
                ", rating=" + rating +
                ", isVerified=" + isVerified +
                ", fullName='" + fullName + '\'' +
                '}';
    }
}

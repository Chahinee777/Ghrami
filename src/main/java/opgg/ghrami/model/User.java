package opgg.ghrami.model;

import java.time.LocalDateTime;

public class User {
    private Long userId;
    private String username;
    private String fullName;
    private String email;
    private String password;
    private String googleId;
    private String authProvider;
    private String profilePicture;
    private String bio;
    private String location;
    private boolean isOnline;
    private boolean isBanned;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;

    // Constructors
    public User() {
        this.createdAt = LocalDateTime.now();
        this.isOnline = false;
        this.authProvider = "local";
    }

    public User(String username, String email, String password) {
        this();
        this.username = username;
        this.email = email;
        this.password = password;
    }

    // Legacy constructor: (username, email, password, profilePicture, bio, location)
    public User(String username, String email, String password, String profilePicture, String bio, String location) {
        this();
        this.username = username;
        this.email = email;
        this.password = password;
        this.profilePicture = profilePicture;
        this.bio = bio;
        this.location = location;
    }

    public User(String username, String email, String password, String googleId, String authProvider) {
        this();
        this.username = username;
        this.email = email;
        this.password = password;
        this.googleId = googleId;
        this.authProvider = authProvider;
    }

    public User(String username, String email, String password, String googleId, String authProvider, String profilePicture, String bio, String location) {
        this();
        this.username = username;
        this.email = email;
        this.password = password;
        this.googleId = googleId;
        this.authProvider = authProvider;
        this.profilePicture = profilePicture;
        this.bio = bio;
        this.location = location;
    }
    public String getGoogleId() {
        return googleId;
    }

    public void setGoogleId(String googleId) {
        this.googleId = googleId;
    }

    public String getAuthProvider() {
        return authProvider;
    }

    public void setAuthProvider(String authProvider) {
        this.authProvider = authProvider;
    }

    // Getters and Setters
    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public void setOnline(boolean online) {
        isOnline = online;
    }

    public boolean isBanned() {
        return isBanned;
    }

    public void setBanned(boolean banned) {
        isBanned = banned;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getLastLogin() {
        return lastLogin;
    }

    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }

    @Override
    public String toString() {
        return "User{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", email='" + email + '\'' +
                ", location='" + location + '\'' +
                ", isOnline=" + isOnline +
                '}';
    }
}

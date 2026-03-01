package opgg.ghrami.model;

/**
 * Represents user information returned from Google OAuth API.
 */
public class GoogleUserInfo {
    private String id;
    private String email;
    private String name;
    private String picture;
    private boolean verifiedEmail;

    public GoogleUserInfo() {}

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPicture() { return picture; }
    public void setPicture(String picture) { this.picture = picture; }

    public boolean isVerifiedEmail() { return verifiedEmail; }
    public void setVerifiedEmail(boolean verifiedEmail) { this.verifiedEmail = verifiedEmail; }

    @Override
    public String toString() {
        return "GoogleUserInfo{id='" + id + "', email='" + email + "', name='" + name + "'}";
    }
}

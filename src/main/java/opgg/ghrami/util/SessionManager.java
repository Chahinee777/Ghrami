package opgg.ghrami.util;

public class SessionManager {
    private static SessionManager instance;
    private String token;
    private long userId;
    private String username;
    private String email;
    private boolean isAdmin;

    private SessionManager() {}

    public static SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }

    public void login(String token) {
        this.token = token;
        this.userId = JWTUtil.extractUserId(token);
        this.username = JWTUtil.extractUsername(token);
        this.email = JWTUtil.extractEmail(token);
        this.isAdmin = JWTUtil.isAdmin(token);
    }

    public void logout() {
        this.token = null;
        this.userId = 0;
        this.username = null;
        this.email = null;
        this.isAdmin = false;
    }

    public boolean isLoggedIn() {
        return token != null && JWTUtil.validateToken(token);
    }

    public String getToken() {
        return token;
    }

    public long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public boolean isAdmin() {
        return isAdmin;
    }
}

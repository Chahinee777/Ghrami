package opgg.ghrami.util;

import java.io.InputStream;
import java.util.Properties;

/**
 * Centralised configuration loader for Ghrami.
 *
 * Reads from two classpath property files:
 *   - ai_config.properties       (AI keys, Mailjet, Stripe)
 *   - google_oauth.properties    (Google OAuth credentials)
 *   - db.properties              (database credentials)
 *
 * NONE of these files should ever be committed to version control
 * (they are all listed in .gitignore).
 */
public final class ConfigManager {

    private static final Properties props = new Properties();
    private static ConfigManager instance;

    private ConfigManager() {
        loadFile("ai_config.properties");
        loadFile("google_oauth.properties");
        loadFile("db.properties");
    }

    public static synchronized ConfigManager getInstance() {
        if (instance == null) instance = new ConfigManager();
        return instance;
    }

    private void loadFile(String filename) {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(filename)) {
            if (in != null) {
                props.load(in);
            } else {
                System.err.println("[ConfigManager] WARNING: " + filename + " not found on classpath.");
            }
        } catch (Exception e) {
            System.err.println("[ConfigManager] Failed to load " + filename + ": " + e.getMessage());
        }
    }

    // ── Generic getter ────────────────────────────────────────────────────────

    public String get(String key) {
        String val = props.getProperty(key);
        if (val == null || val.isBlank()) {
            System.err.println("[ConfigManager] WARNING: key '" + key + "' is missing or empty in config files.");
        }
        return val != null ? val.trim() : "";
    }

    // ── Typed convenience getters ─────────────────────────────────────────────

    // Groq
    public String getGroqApiKey()      { return get("groq.api.key"); }
    public String getGroqApiUrl()      { return get("groq.api.url"); }
    public String getGroqTextModel()   { return get("groq.text.model"); }
    public String getGroqVisionModel() { return get("groq.vision.model"); }

    // Hugging Face
    public String getHfApiKey()        { return get("hf.api.key"); }
    public String getHfImageUrl()      { return get("hf.image.url"); }

    // Mailjet
    public String getMailjetApiKey()    { return get("mailjet.api.key"); }
    public String getMailjetApiSecret() { return get("mailjet.api.secret"); }
    public String getMailjetFromEmail() { return get("mailjet.from.email"); }
    public String getMailjetFromName()  { return get("mailjet.from.name"); }

    // Stripe
    public String getStripeSecretKey()      { return get("stripe.secret.key"); }
    public String getStripePublishableKey() { return get("stripe.publishable.key"); }

    // Google OAuth
    public String getGoogleClientId()     { return get("google.client.id"); }
    public String getGoogleClientSecret() { return get("google.client.secret"); }

    // Database
    public String getDbUrl()      { return get("db.url"); }
    public String getDbUsername() { return get("db.username"); }
    public String getDbPassword() { return get("db.password"); }
}

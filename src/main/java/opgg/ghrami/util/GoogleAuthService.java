package opgg.ghrami.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import opgg.ghrami.model.GoogleUserInfo;

import java.awt.Desktop;
import java.io.*;
import java.net.*;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Handles Google OAuth 2.0 authentication flow for JavaFX desktop app.
 * Flow: Open browser → User signs in → Capture callback code → Exchange for token → Get user info
 */
public class GoogleAuthService {

    private static final int CALLBACK_PORT = 8888;
    private static final String CALLBACK_PATH = "/callback";

    private String clientId;
    private String clientSecret;
    private String redirectUri;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GoogleAuthService() {
        loadProperties();
    }

    private void loadProperties() {
        try (InputStream is = getClass().getResourceAsStream("/google_oauth.properties")) {
            if (is == null) {
                throw new RuntimeException("google_oauth.properties not found in resources!");
            }
            Properties props = new Properties();
            props.load(is);
            this.clientId = props.getProperty("google.client.id");
            this.clientSecret = props.getProperty("google.client.secret");
            this.redirectUri = props.getProperty("google.redirect.uri");
        } catch (IOException e) {
            throw new RuntimeException("Failed to load Google OAuth properties: " + e.getMessage());
        }
    }

    /**
     * Full OAuth flow: opens browser, waits for callback, returns user info.
     * Returns null if user cancelled or an error occurred.
     */
    public GoogleUserInfo authenticate() throws Exception {
        // Step 1: Build and open the authorization URL
        String authUrl = buildAuthorizationUrl();
        openBrowser(authUrl);

        // Step 2: Start local server and wait for the authorization code
        String code = waitForAuthorizationCode();
        if (code == null) {
            return null;
        }

        // Step 3: Exchange the code for tokens
        String accessToken = exchangeCodeForToken(code);
        if (accessToken == null) {
            return null;
        }

        // Step 4: Fetch user info from Google
        return fetchUserInfo(accessToken);
    }

    private String buildAuthorizationUrl() throws UnsupportedEncodingException {
        return "https://accounts.google.com/o/oauth2/v2/auth?" +
                "client_id=" + URLEncoder.encode(clientId, "UTF-8") +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, "UTF-8") +
                "&response_type=code" +
                "&scope=" + URLEncoder.encode("openid email profile", "UTF-8") +
                "&access_type=offline" +
                "&prompt=select_account";
    }

    private void openBrowser(String url) throws Exception {
        if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
            Desktop.getDesktop().browse(new URI(url));
        } else {
            // Fallback for Linux/headless environments
            Runtime.getRuntime().exec(new String[]{"xdg-open", url});
        }
    }

    private String waitForAuthorizationCode() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<String> codeRef = new AtomicReference<>();

        HttpServer server = HttpServer.create(new InetSocketAddress(CALLBACK_PORT), 0);

        server.createContext(CALLBACK_PATH, exchange -> {
            try {
                String query = exchange.getRequestURI().getQuery();
                String code = extractParam(query, "code");
                String error = extractParam(query, "error");

                String responseBody;
                if (code != null) {
                    codeRef.set(code);
                    responseBody = buildSuccessPage();
                } else {
                    responseBody = buildErrorPage(error != null ? error : "Unknown error");
                }

                byte[] bytes = responseBody.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=utf-8");
                exchange.sendResponseHeaders(200, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) {
                    os.write(bytes);
                }
            } finally {
                latch.countDown();
            }
        });

        server.setExecutor(null);
        server.start();

        try {
            // Wait up to 3 minutes for the user to complete login
            boolean completed = latch.await(3, TimeUnit.MINUTES);
            if (!completed) {
                System.err.println("Google login timed out after 3 minutes");
                return null;
            }
            return codeRef.get();
        } finally {
            server.stop(1);
        }
    }

    private String exchangeCodeForToken(String code) throws Exception {
        String formData = "code=" + URLEncoder.encode(code, "UTF-8") +
                "&client_id=" + URLEncoder.encode(clientId, "UTF-8") +
                "&client_secret=" + URLEncoder.encode(clientSecret, "UTF-8") +
                "&redirect_uri=" + URLEncoder.encode(redirectUri, "UTF-8") +
                "&grant_type=authorization_code";

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://oauth2.googleapis.com/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formData))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            System.err.println("Token exchange failed: " + response.body());
            return null;
        }

        JsonNode json = objectMapper.readTree(response.body());
        return json.has("access_token") ? json.get("access_token").asText() : null;
    }

    private GoogleUserInfo fetchUserInfo(String accessToken) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://www.googleapis.com/oauth2/v2/userinfo"))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            System.err.println("Failed to fetch user info: " + response.body());
            return null;
        }

        JsonNode json = objectMapper.readTree(response.body());

        GoogleUserInfo userInfo = new GoogleUserInfo();
        userInfo.setId(json.has("id") ? json.get("id").asText() : null);
        userInfo.setEmail(json.has("email") ? json.get("email").asText() : null);
        userInfo.setName(json.has("name") ? json.get("name").asText() : null);
        userInfo.setPicture(json.has("picture") ? json.get("picture").asText() : null);
        userInfo.setVerifiedEmail(json.has("verified_email") && json.get("verified_email").asBoolean());

        return userInfo;
    }

    private String extractParam(String query, String paramName) {
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] parts = param.split("=", 2);
            if (parts.length == 2 && parts[0].equals(paramName)) {
                try {
                    return URLDecoder.decode(parts[1], "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    return parts[1];
                }
            }
        }
        return null;
    }

    private String buildLogoImg() {
        try {
            java.net.URL logoUrl = getClass().getResource("/images/assets/ghrami-logo.png");
            if (logoUrl != null) {
                byte[] bytes = logoUrl.openStream().readAllBytes();
                String b64 = java.util.Base64.getEncoder().encodeToString(bytes);
                // Make logo bigger: 120px
                return "<img src='data:image/png;base64," + b64 + "' alt='Ghrami' " +
                       "style='width:120px;height:120px;object-fit:contain;border-radius:24px;'/>";
            }
        } catch (Exception ignored) {}
        return "<div class='logo-icon'>G</div>";
    }

    private String buildSuccessPage() {
        String logoHtml = buildLogoImg();
        return "<!DOCTYPE html>" +
            "<html lang='fr'><head><meta charset='UTF-8'/>" +
            "<meta name='viewport' content='width=device-width,initial-scale=1'/>" +
            "<title>Ghrami – Connexion réussie</title>" +
            "<link rel='preconnect' href='https://fonts.googleapis.com'/>" +
            "<link href='https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap' rel='stylesheet'/>" +
            "<style>" +
            "*{margin:0;padding:0;box-sizing:border-box;}" +
            "body{font-family:'Inter',sans-serif;min-height:100vh;display:flex;align-items:center;" +
            "justify-content:center;background:linear-gradient(135deg,#5a3fb8 0%,#6b4fd1 45%,#7c5cff 100%);overflow:hidden;}" +
            ".bg-circle{position:fixed;border-radius:50%;opacity:.12;}" +
            ".bg-circle.c1{width:700px;height:700px;background:white;top:-200px;left:-200px;}" +
            ".bg-circle.c2{width:500px;height:500px;background:white;bottom:-150px;right:-150px;}" +
            ".bg-circle.c3{width:300px;height:300px;background:white;top:40%;right:5%;}" +
            ".card{position:relative;background:white;border-radius:28px;padding:60px 56px 52px;" +
            "text-align:center;width:100%;max-width:480px;margin:20px;" +
            "box-shadow:0 32px 80px rgba(90,63,184,.35),0 8px 24px rgba(90,63,184,.2);}" +
            ".logo-wrap{display:inline-flex;align-items:center;justify-content:center;gap:10px;margin-bottom:36px;}" +
            ".logo-icon{width:52px;height:52px;border-radius:14px;" +
            "background:linear-gradient(135deg,#5a3fb8,#7c5cff);" +
            "display:flex;align-items:center;justify-content:center;" +
            "font-size:26px;color:white;font-weight:800;box-shadow:0 8px 20px rgba(102,126,234,.45);}" +
            ".logo-text{font-size:26px;font-weight:800;" +
            "background:linear-gradient(135deg,#5a3fb8,#7c5cff);-webkit-background-clip:text;" +
            "-webkit-text-fill-color:transparent;background-clip:text;}" +
            ".check-ring{width:90px;height:90px;border-radius:50%;" +
            "background:linear-gradient(135deg,#5a3fb8 0%,#7c5cff 100%);" +
            "display:flex;align-items:center;justify-content:center;" +
            "margin:0 auto 28px;box-shadow:0 12px 32px rgba(102,126,234,.5);" +
            "animation:popIn .5s cubic-bezier(.34,1.56,.64,1) both;}" +
            "@keyframes popIn{from{transform:scale(0);opacity:0}to{transform:scale(1);opacity:1}}" +
            ".check-ring svg{width:44px;height:44px;}" +
            "h1{font-size:1.75rem;font-weight:800;color:#1c1e21;margin-bottom:14px;letter-spacing:-.3px;}" +
            ".subtitle{font-size:1rem;color:#65676b;line-height:1.6;margin-bottom:36px;}" +
            ".pill{display:inline-flex;align-items:center;gap:8px;padding:10px 22px;" +
            "background:linear-gradient(135deg,rgba(90,63,184,.1),rgba(124,92,255,.1));" +
            "border-radius:50px;border:1.5px solid rgba(102,126,234,.25);margin-bottom:36px;}" +
            ".pill-dot{width:8px;height:8px;border-radius:50%;background:#5a3fb8;" +
            "animation:pulse 1.5s ease-in-out infinite;}" +
            "@keyframes pulse{0%,100%{opacity:1;transform:scale(1)}50%{opacity:.5;transform:scale(.8)}}" +
            ".pill-text{font-size:.85rem;font-weight:600;color:#5a3fb8;}" +
            ".close-hint{font-size:.8rem;color:#adb5bd;margin-top:8px;}" +
            ".divider{height:1px;background:linear-gradient(to right,transparent,#e4e6eb,transparent);margin:0 0 28px;}" +
            ".by-opgg{font-size:.78rem;color:#b0b3b8;font-weight:500;letter-spacing:.5px;text-transform:uppercase;}" +
            "</style></head><body>" +
            "<div class='bg-circle c1'></div><div class='bg-circle c2'></div><div class='bg-circle c3'></div>" +
            "<div class='card'>" +
            "  <div class='logo-wrap'>" + logoHtml + "<span class='logo-text'>Ghrami</span></div>" +
            "  <div class='check-ring'>" +
            "    <svg viewBox='0 0 44 44' fill='none' xmlns='http://www.w3.org/2000/svg'>" +
            "      <path d='M8 22L18 32L36 12' stroke='white' stroke-width='4' stroke-linecap='round' stroke-linejoin='round'/>" +
            "    </svg>" +
            "  </div>" +
            "  <h1>Connexion réussie\u00a0!</h1>" +
            "  <p class='subtitle'>Votre compte Google a été vérifié avec succès.<br/>Retournez à l'application pour continuer.</p>" +
            "  <div class='pill'><div class='pill-dot'></div><span class='pill-text'>Vous êtes connecté(e)</span></div>" +
            "  <div class='divider'></div>" +
            "  <p class='close-hint'>Vous pouvez fermer cet onglet</p>" +
            "  <p class='by-opgg' style='margin-top:16px;'>by OPGG</p>" +
            "</div></body></html>";
    }

    private String buildErrorPage(String error) {
        String logoHtml = buildLogoImg();
        return "<!DOCTYPE html>" +
            "<html lang='fr'><head><meta charset='UTF-8'/>" +
            "<meta name='viewport' content='width=device-width,initial-scale=1'/>" +
            "<title>Ghrami – Erreur</title>" +
            "<link rel='preconnect' href='https://fonts.googleapis.com'/>" +
            "<link href='https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap' rel='stylesheet'/>" +
            "<style>" +
            "*{margin:0;padding:0;box-sizing:border-box;}" +
            "body{font-family:'Inter',sans-serif;min-height:100vh;display:flex;align-items:center;" +
            "justify-content:center;background:linear-gradient(135deg,#5a3fb8 0%,#6b4fd1 45%,#7c5cff 100%);overflow:hidden;}" +
            ".bg-circle{position:fixed;border-radius:50%;opacity:.12;}" +
            ".bg-circle.c1{width:700px;height:700px;background:white;top:-200px;left:-200px;}" +
            ".bg-circle.c2{width:500px;height:500px;background:white;bottom:-150px;right:-150px;}" +
            ".card{position:relative;background:white;border-radius:28px;padding:60px 56px 52px;" +
            "text-align:center;width:100%;max-width:480px;margin:20px;" +
            "box-shadow:0 32px 80px rgba(90,63,184,.35),0 8px 24px rgba(90,63,184,.2);}" +
            ".logo-wrap{display:inline-flex;align-items:center;justify-content:center;gap:10px;margin-bottom:36px;}" +
            ".logo-icon{width:52px;height:52px;border-radius:14px;" +
            "background:linear-gradient(135deg,#5a3fb8,#7c5cff);" +
            "display:flex;align-items:center;justify-content:center;" +
            "font-size:26px;color:white;font-weight:800;box-shadow:0 8px 20px rgba(102,126,234,.45);}" +
            ".logo-text{font-size:26px;font-weight:800;" +
            "background:linear-gradient(135deg,#5a3fb8,#7c5cff);-webkit-background-clip:text;" +
            "-webkit-text-fill-color:transparent;background-clip:text;}" +
            ".err-ring{width:90px;height:90px;border-radius:50%;" +
            "background:linear-gradient(135deg,#e74c3c,#ff6b6b);" +
            "display:flex;align-items:center;justify-content:center;" +
            "margin:0 auto 28px;box-shadow:0 12px 32px rgba(231,76,60,.4);" +
            "animation:popIn .5s cubic-bezier(.34,1.56,.64,1) both;}" +
            "@keyframes popIn{from{transform:scale(0);opacity:0}to{transform:scale(1);opacity:1}}" +
            ".err-ring svg{width:44px;height:44px;}" +
            "h1{font-size:1.75rem;font-weight:800;color:#1c1e21;margin-bottom:14px;letter-spacing:-.3px;}" +
            ".subtitle{font-size:1rem;color:#65676b;line-height:1.6;margin-bottom:16px;}" +
            ".error-box{background:#fff5f5;border:1.5px solid #fed7d7;border-radius:12px;" +
            "padding:14px 20px;margin-bottom:28px;font-size:.85rem;color:#c53030;font-weight:500;}" +
            ".divider{height:1px;background:linear-gradient(to right,transparent,#e4e6eb,transparent);margin:0 0 24px;}" +
            ".retry-hint{font-size:.85rem;color:#65676b;}" +
            ".retry-hint strong{color:#5a3fb8;}" +
            ".by-opgg{font-size:.78rem;color:#b0b3b8;font-weight:500;letter-spacing:.5px;text-transform:uppercase;margin-top:20px;}" +
            "</style></head><body>" +
            "<div class='bg-circle c1'></div><div class='bg-circle c2'></div>" +
            "<div class='card'>" +
            "  <div class='logo-wrap'>" + logoHtml + "<span class='logo-text'>Ghrami</span></div>" +
            "  <div class='err-ring'>" +
            "    <svg viewBox='0 0 44 44' fill='none' xmlns='http://www.w3.org/2000/svg'>" +
            "      <path d='M12 12L32 32M32 12L12 32' stroke='white' stroke-width='4' stroke-linecap='round'/>" +
            "    </svg>" +
            "  </div>" +
            "  <h1>Authentification échouée</h1>" +
            "  <p class='subtitle'>Une erreur est survenue lors de la connexion avec Google.</p>" +
            "  <div class='error-box'>" + error + "</div>" +
            "  <div class='divider'></div>" +
            "  <p class='retry-hint'>Fermez cet onglet et <strong>réessayez dans l'application</strong>.</p>" +
            "  <p class='by-opgg'>by OPGG</p>" +
            "</div></body></html>";
    }
}

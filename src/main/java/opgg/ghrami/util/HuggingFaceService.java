package opgg.ghrami.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * AI Service:
 *  1. completeText()  → Groq         (llama-3.1-8b-instant)               ✅
 *  2. captionImage()  → Groq         (llama-4-scout-17b vision)            ✅
 *  3. generateImage() → HuggingFace  (FLUX.1-schnell, free)                ✅
 *
 * FLUX.1-schnell is a fast, free text-to-image model served via the
 * HuggingFace Inference router. Requires a (free) HF token.
 */
public class HuggingFaceService {

    // ─── Config (loaded from ai_config.properties via ConfigManager) ──────────
    private static final String GROQ_TOKEN   = ConfigManager.getInstance().getGroqApiKey();
    private static final String GROQ_API_URL = ConfigManager.getInstance().getGroqApiUrl();
    private static final String TEXT_MODEL   = ConfigManager.getInstance().getGroqTextModel();
    private static final String VISION_MODEL = ConfigManager.getInstance().getGroqVisionModel();
    private static final String HF_TOKEN     = ConfigManager.getInstance().getHfApiKey();
    private static final String HF_IMAGE_URL = ConfigManager.getInstance().getHfImageUrl();

    // ─────────────────────────────────────────────────────────────────────────

    private final HttpClient   httpClient;
    private final ObjectMapper mapper;
    private static HuggingFaceService instance;

    private HuggingFaceService() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
        this.mapper = new ObjectMapper();
    }

    public static synchronized HuggingFaceService getInstance() {
        if (instance == null) instance = new HuggingFaceService();
        return instance;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  1. TEXT COMPLETION  (Groq — unchanged ✅)
    // ─────────────────────────────────────────────────────────────────────────

    public String completeText(String partialText) throws Exception {
        if (partialText == null || partialText.isBlank())
            throw new IllegalArgumentException("Le texte ne peut pas être vide.");

        String body = mapper.writeValueAsString(Map.of(
                "model", TEXT_MODEL,
                "messages", List.of(
                        Map.of("role", "system", "content",
                                "Tu es un assistant de réseau social. "
                                + "Complète le post de l'utilisateur en français, "
                                + "de façon naturelle, engageante et positive. "
                                + "Réponds avec maximum 2 phrases courtes. "
                                + "Ne répète pas la partie déjà écrite. "
                                + "Donne uniquement la suite du texte, sans explication."),
                        Map.of("role", "user", "content", partialText.trim())
                ),
                "max_tokens",  150,
                "temperature", 0.75
        ));

        HttpResponse<String> resp = postJson(GROQ_API_URL, GROQ_TOKEN, body, 30);

        if (resp.statusCode() != 200)
            throw new Exception("Groq completeText erreur " + resp.statusCode()
                    + ": " + truncate(resp.body()));

        JsonNode content = mapper.readTree(resp.body())
                .path("choices").path(0).path("message").path("content");

        if (!content.isMissingNode()) {
            System.out.println("[Groq] completeText OK");
            return content.asText("").trim();
        }
        throw new Exception("Réponse inattendue de completeText: " + resp.body());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  2. IMAGE CAPTIONING  (Groq vision — unchanged ✅)
    // ─────────────────────────────────────────────────────────────────────────

    public String captionImage(byte[] imageBytes) throws Exception {
        if (imageBytes == null || imageBytes.length == 0)
            throw new IllegalArgumentException("L'image est vide.");

        String mimeType = detectMimeType(imageBytes);
        String base64   = Base64.getEncoder().encodeToString(imageBytes);
        String dataUri  = "data:" + mimeType + ";base64," + base64;

        String body = mapper.writeValueAsString(Map.of(
                "model", VISION_MODEL,
                "messages", List.of(
                        Map.of("role", "user",
                               "content", List.of(
                                       Map.of("type", "image_url",
                                              "image_url", Map.of("url", dataUri)),
                                       Map.of("type", "text",
                                              "text", "Describe this image in one short sentence "
                                                    + "suitable as a social media post caption. "
                                                    + "Reply in French.")
                               ))
                ),
                "max_tokens", 100
        ));

        HttpResponse<String> resp = postJson(GROQ_API_URL, GROQ_TOKEN, body, 60);

        if (resp.statusCode() != 200)
            throw new Exception("Groq captionImage erreur " + resp.statusCode()
                    + ": " + truncate(resp.body()));

        JsonNode content = mapper.readTree(resp.body())
                .path("choices").path(0).path("message").path("content");

        if (!content.isMissingNode()) {
            System.out.println("[Groq] captionImage OK");
            return content.asText("").trim();
        }
        throw new Exception("Réponse inattendue de captionImage: " + resp.body());
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  3. IMAGE GENERATION  (HuggingFace FLUX.1-schnell — free, synchronous)
    // ─────────────────────────────────────────────────────────────────────────

    // ─────────────────────────────────────────────────────────────────────────
    //  3. IMAGE GENERATION  (HuggingFace FLUX.1-schnell — free, synchronous)
    //
    //  Simple direct POST: send prompt → receive raw PNG bytes.
    // ─────────────────────────────────────────────────────────────────────────

    public byte[] generateImage(String prompt) throws Exception {
        if (prompt == null || prompt.isBlank())
            throw new IllegalArgumentException("Le prompt ne peut pas être vide.");

        String body = mapper.writeValueAsString(Map.of("inputs", prompt.trim()));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(HF_IMAGE_URL))
                .header("Authorization", "Bearer " + HF_TOKEN)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(150))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<byte[]> resp = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

        // Model cold-start: retry once after a short wait
        if (resp.statusCode() == 503) {
            System.out.println("[HF] Model cold-start (503), retrying in 15s...");
            Thread.sleep(15_000);
            resp = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        }

        if (resp.statusCode() != 200) {
            throw new Exception("HF generateImage erreur " + resp.statusCode()
                    + ": " + truncate(new String(resp.body())));
        }

        System.out.println("[HF] generateImage OK — " + resp.body().length + " bytes");
        return resp.body();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private HttpResponse<String> postJson(String url, String token, String body, int timeoutSec)
            throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(timeoutSec))
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return httpClient.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private String detectMimeType(byte[] bytes) {
        if (bytes.length >= 4
                && bytes[0] == (byte) 0x89 && bytes[1] == (byte) 0x50
                && bytes[2] == (byte) 0x4E && bytes[3] == (byte) 0x47)
            return "image/png";
        if (bytes.length >= 2
                && bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8)
            return "image/jpeg";
        return "image/jpeg";
    }

    private String truncate(String s) {
        if (s == null) return "";
        return s.length() > 300 ? s.substring(0, 300) + "..." : s;
    }
}
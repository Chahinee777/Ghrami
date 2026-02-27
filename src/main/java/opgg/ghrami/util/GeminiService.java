package opgg.ghrami.util;

import com.google.gson.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Properties;

/**
 * AI chat service backed by OpenRouter (OpenAI-compatible API).
 * Maintains a multi-turn conversation history per instance.
 */
public class GeminiService {

    private static final String DEFAULT_BASE_URL = "https://api.groq.com/openai/v1/chat/completions";

    private final String apiKey;
    private final String model;
    private final String endpoint;
    private final HttpClient httpClient;
    // OpenAI-style messages: [{"role":"user","content":"..."},...]
    private final JsonArray messages;

    /**
     * @param systemContext  System-level instructions / user context injected as the first message.
     */
    public GeminiService(String systemContext) {
        this.apiKey     = loadProperty("ai.api.key");
        this.model      = loadProperty("ai.model");
        String ep       = loadProperty("ai.endpoint");
        this.endpoint   = (ep != null && !ep.isEmpty()) ? ep : DEFAULT_BASE_URL;
        this.httpClient = HttpClient.newHttpClient();
        this.messages   = new JsonArray();

        // Prime the conversation with a system message
        messages.add(buildMessage("system", systemContext));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Sends a user message and returns the assistant's text reply.
     * Conversation history is kept for multi-turn context.
     *
     * @throws Exception on network error or non-200 HTTP response
     */
    public String chat(String userMessage) throws Exception {
        // 1. Append user message
        messages.add(buildMessage("user", userMessage));

        // 2. Build request body
        JsonObject body = new JsonObject();
        body.addProperty("model", model);
        body.add("messages", messages);

        // 3. Send HTTP POST
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(endpoint))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer " + apiKey)
            .POST(HttpRequest.BodyPublishers.ofString(new Gson().toJson(body)))
            .build();

        HttpResponse<String> response =
                httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new Exception("AI API error " + response.statusCode()
                    + ":\n" + response.body());
        }

        // 4. Parse response — OpenAI format: choices[0].message.content
        JsonObject json    = JsonParser.parseString(response.body()).getAsJsonObject();
        String     aiReply = json.getAsJsonArray("choices")
                .get(0).getAsJsonObject()
                .getAsJsonObject("message")
                .get("content").getAsString();

        // 5. Append assistant reply to history
        messages.add(buildMessage("assistant", aiReply));

        return aiReply;
    }

    /** Clear conversation history (keeps the system message at index 0). */
    public void reset() {
        // Keep only the first element (system message)
        while (messages.size() > 1) {
            messages.remove(1);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private JsonObject buildMessage(String role, String content) {
        JsonObject msg = new JsonObject();
        msg.addProperty("role", role);
        msg.addProperty("content", content);
        return msg;
    }

    private String loadProperty(String key) {
        try (InputStream in = getClass().getResourceAsStream("/ai_config.properties")) {
            if (in != null) {
                Properties props = new Properties();
                props.load(in);
                return props.getProperty(key, "");
            }
        } catch (IOException e) {
            System.err.println("[GeminiService] Failed to load property '" + key + "': " + e.getMessage());
        }
        return "";
    }
}

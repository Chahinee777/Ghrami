package opgg.ghrami.view;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.PauseTransition;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import opgg.ghrami.controller.FriendshipController;
import opgg.ghrami.controller.MessageController;
import opgg.ghrami.controller.NotificationController;
import opgg.ghrami.controller.UserController;
import opgg.ghrami.model.Friendship;
import opgg.ghrami.model.Message;
import opgg.ghrami.model.User;
import opgg.ghrami.util.ChatClient;
import opgg.ghrami.util.SessionManager;

import javax.sound.sampled.*;
import java.io.*;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class MessagesViewController implements Initializable {

    @FXML private Label unreadBadgeLabel;
    @FXML private TextField searchConvField;

    // Left panel
    @FXML private VBox conversationsList;

    // Right panel – chat
    @FXML private HBox chatHeader;
    @FXML private Circle chatAvatarCircle;
    @FXML private Label chatAvatarInitial;
    @FXML private Label chatNameLabel;
    @FXML private Label chatStatusLabel;

    @FXML private VBox emptyState;
    @FXML private ScrollPane messagesScrollPane;
    @FXML private VBox messagesContainer;

    @FXML private HBox inputBar;
    @FXML private TextField messageInputField;
    @FXML private Button micButton;
    @FXML private HBox smartRepliesBar;

    // ── Groq Whisper STT ──
    private static final String GROQ_API_KEY  = "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"; // <-- paste your key
    private static final String GROQ_STT_URL  = "https://api.groq.com/openai/v1/audio/transcriptions";
    private static final String GROQ_MODEL    = "whisper-large-v3-turbo";

    private volatile boolean     isRecording  = false;
    private TargetDataLine       recordingLine;
    private File                 tempAudioFile;
    private final ExecutorService sttExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "groq-stt-thread");
        t.setDaemon(true);
        return t;
    });

    // ── Groq LLaMA – Smart Replies ──
    private static final String GROQ_CHAT_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROQ_CHAT_MODEL = "llama-3.1-8b-instant";
    private final ExecutorService aiExecutor = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "groq-ai-thread");
        t.setDaemon(true);
        return t;
    });

    // ── Typing indicator ──
    private static final String TYPING_SIGNAL    = "__TYPING__";
    private HBox                typingBubble     = null;   // the animated dots row
    private Timeline            typingDotAnim    = null;
    // Debounce: only send a typing signal every 2 s while the user is typing
    private long lastTypingSentMs = 0;

    // ── State ──
    private SessionManager sessionManager;
    private UserController userController;
    private FriendshipController friendshipController;
    private MessageController messageController;
    private NotificationController notificationController;

    private User currentUser;
    private User selectedFriend;
    private List<User> acceptedFriends = new ArrayList<>();

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM HH:mm");

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        sessionManager = SessionManager.getInstance();
        userController = new UserController();
        friendshipController = new FriendshipController();
        messageController = MessageController.getInstance();
        notificationController = NotificationController.getInstance();

        long uid = sessionManager.getUserId();
        currentUser = userController.findById((int) uid);

        loadFriendsAndConversations();

        // Live-filter conversations list on search
        searchConvField.textProperty().addListener((obs, old, q) -> filterConversations(q));

        // Update unread badge
        refreshUnreadBadge();

        // ── Connect to real-time chat server ──
        ChatClient chatClient = ChatClient.getInstance();
        chatClient.connect(currentUser.getUserId());
        chatClient.setMessageListener((fromId, content) -> Platform.runLater(() -> {

            // ── Typing indicator from friend ──
            if (TYPING_SIGNAL.equals(content)) {
                if (selectedFriend != null && selectedFriend.getUserId().equals(fromId)) {
                    showTypingIndicator();
                }
                return; // don't process as a real message
            }

            // Hide typing bubble on real message arrival
            hideTypingIndicator();

            // Refresh the conversation list so unread counts & previews update
            loadFriendsAndConversations();
            refreshUnreadBadge();

            // If the active chat is with the sender, append the bubble live
            if (selectedFriend != null && selectedFriend.getUserId().equals(fromId)) {
                Message incoming = new Message();
                incoming.setSenderId(fromId);
                incoming.setReceiverId(currentUser.getUserId());
                incoming.setContent(content);
                incoming.setSentAt(java.time.LocalDateTime.now());
                incoming.setRead(false);
                messagesContainer.getChildren().add(buildMessageBubble(incoming));
                messagesScrollPane.setVvalue(1.0);
                messageController.markAsRead(fromId, currentUser.getUserId());

                // Fetch smart replies for the new incoming message
                fetchSmartReplies(content);
            }
        }));

        // ── Send typing signal while user types (debounced to 2 s) ──
        messageInputField.textProperty().addListener((obs, oldVal, newVal) -> {
            // Hide smart replies as soon as user starts typing their own text
            if (!newVal.isBlank()) hideSmartReplies();

            if (selectedFriend == null || newVal.isBlank()) return;
            long now = System.currentTimeMillis();
            if (now - lastTypingSentMs > 2_000) {
                lastTypingSentMs = now;
                ChatClient.getInstance().send(selectedFriend.getUserId(), TYPING_SIGNAL);
            }
        });
    }

    // ══════════════════════════════════════════════════════════════
    //  Load & render left-panel conversations
    // ══════════════════════════════════════════════════════════════

    private void loadFriendsAndConversations() {
        acceptedFriends.clear();
        List<Friendship> friendships = friendshipController.getAcceptedFriendships(currentUser.getUserId());
        for (Friendship f : friendships) {
            long otherId = f.getUser1Id() == currentUser.getUserId() ? f.getUser2Id() : f.getUser1Id();
            User friend = userController.findById((int) otherId);
            if (friend != null) acceptedFriends.add(friend);
        }

        // Sort: friends with recent messages first, then alphabetically
        acceptedFriends.sort((a, b) -> {
            Message ma = messageController.getLastMessage(currentUser.getUserId(), a.getUserId());
            Message mb = messageController.getLastMessage(currentUser.getUserId(), b.getUserId());
            if (ma == null && mb == null) return a.getUsername().compareTo(b.getUsername());
            if (ma == null) return 1;
            if (mb == null) return -1;
            return mb.getSentAt().compareTo(ma.getSentAt());
        });

        renderConversationList(acceptedFriends);
    }

    private void filterConversations(String query) {
        if (query == null || query.isBlank()) {
            renderConversationList(acceptedFriends);
        } else {
            String q = query.toLowerCase();
            renderConversationList(acceptedFriends.stream()
                    .filter(u -> u.getUsername().toLowerCase().contains(q)
                            || (u.getFullName() != null && u.getFullName().toLowerCase().contains(q)))
                    .collect(Collectors.toList()));
        }
    }

    private void renderConversationList(List<User> friends) {
        conversationsList.getChildren().clear();
        if (friends.isEmpty()) {
            Label empty = new Label("Aucun ami trouvé");
            empty.setStyle("-fx-text-fill: #65676b; -fx-font-size: 13; -fx-padding: 20;");
            conversationsList.getChildren().add(empty);
            return;
        }
        for (User friend : friends) {
            conversationsList.getChildren().add(buildConversationItem(friend));
        }
    }

    private HBox buildConversationItem(User friend) {
        HBox item = new HBox();
        item.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        item.setSpacing(12);
        item.setPadding(new Insets(12, 16, 12, 16));
        item.setStyle("-fx-cursor: hand; -fx-background-color: " +
                (selectedFriend != null && selectedFriend.getUserId().equals(friend.getUserId())
                        ? "rgba(102,126,234,0.1);" : "transparent;"));

        // Avatar
        StackPane avatarPane = new StackPane();
        Circle avatar = new Circle(24);
        loadAvatarImage(avatar, friend);
        Label initial = new Label(getInitial(friend));
        initial.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 16;");
        if (friend.getProfilePicture() == null || friend.getProfilePicture().isEmpty()) {
            avatarPane.getChildren().addAll(avatar, initial);
        } else {
            avatarPane.getChildren().add(avatar);
        }

        // Text column
        VBox textCol = new VBox(3);
        HBox.setHgrow(textCol, Priority.ALWAYS);

        Label nameLabel = new Label(friend.getFullName() != null ? friend.getFullName() : friend.getUsername());
        nameLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-text-fill: #1c1e21;");

        Message last = messageController.getLastMessage(currentUser.getUserId(), friend.getUserId());
        Label previewLabel = new Label(last != null ? last.getContent() : "Commencer à discuter...");
        previewLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #65676b;");
        previewLabel.setMaxWidth(160);
        previewLabel.setEllipsisString("…");

        textCol.getChildren().addAll(nameLabel, previewLabel);

        // Right column: time + unread badge
        VBox rightCol = new VBox(4);
        rightCol.setAlignment(Pos.CENTER_RIGHT);

        if (last != null) {
            boolean isToday = last.getSentAt().toLocalDate().equals(java.time.LocalDate.now());
            Label timeLabel = new Label(isToday
                    ? last.getSentAt().format(TIME_FMT)
                    : last.getSentAt().format(DATE_FMT));
            timeLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #adb5bd;");
            rightCol.getChildren().add(timeLabel);
        }

        int unreadCount = messageController.countUnreadFrom(friend.getUserId(), currentUser.getUserId());
        if (unreadCount > 0) {
            Label badge = new Label(String.valueOf(unreadCount));
            badge.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; -fx-font-weight: bold; "
                    + "-fx-font-size: 11; -fx-background-radius: 20; -fx-padding: 2 7;");
            rightCol.getChildren().add(badge);
            previewLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #1c1e21; -fx-font-weight: bold;");
        }

        item.getChildren().addAll(avatarPane, textCol, rightCol);

        // Hover effect
        item.setOnMouseEntered(e -> {
            if (selectedFriend == null || !selectedFriend.getUserId().equals(friend.getUserId()))
                item.setStyle("-fx-cursor: hand; -fx-background-color: rgba(102,126,234,0.06);");
        });
        item.setOnMouseExited(e -> {
            if (selectedFriend == null || !selectedFriend.getUserId().equals(friend.getUserId()))
                item.setStyle("-fx-cursor: hand; -fx-background-color: transparent;");
        });

        item.setOnMouseClicked(e -> openConversation(friend));
        return item;
    }

    // ══════════════════════════════════════════════════════════════
    //  Open conversation
    // ══════════════════════════════════════════════════════════════

    private void openConversation(User friend) {
        selectedFriend = friend;

        // Show chat panel, hide empty state
        emptyState.setVisible(false);
        emptyState.setManaged(false);
        chatHeader.setVisible(true);
        chatHeader.setManaged(true);
        messagesScrollPane.setVisible(true);
        messagesScrollPane.setManaged(true);
        inputBar.setVisible(true);
        inputBar.setManaged(true);

        // Header
        chatNameLabel.setText(friend.getFullName() != null ? friend.getFullName() : friend.getUsername());
        chatAvatarInitial.setText(getInitial(friend));
        loadAvatarImage(chatAvatarCircle, friend);
        if (friend.getProfilePicture() != null && !friend.getProfilePicture().isEmpty()) {
            chatAvatarInitial.setVisible(false);
        } else {
            chatAvatarInitial.setVisible(true);
        }
        chatStatusLabel.setText(friend.isOnline() ? "● En ligne" : "● Hors ligne");
        chatStatusLabel.setStyle("-fx-font-size: 12; -fx-text-fill: " + (friend.isOnline() ? "#31a24c;" : "#adb5bd;"));

        // Mark messages as read
        messageController.markAsRead(friend.getUserId(), currentUser.getUserId());

        // Load messages
        loadMessages();

        // Refresh conversation list to update unread badges
        renderConversationList(acceptedFriends);
        refreshUnreadBadge();

        // Focus input
        messageInputField.requestFocus();
    }

    private void loadMessages() {
        messagesContainer.getChildren().clear();
        List<Message> messages = messageController.getConversation(
                currentUser.getUserId(), selectedFriend.getUserId());

        String lastDateStr = null;

        for (Message msg : messages) {
            // Date separator
            String dateStr = msg.getSentAt().toLocalDate().toString();
            if (!dateStr.equals(lastDateStr)) {
                lastDateStr = dateStr;
                messagesContainer.getChildren().add(buildDateSeparator(dateStr));
            }
            messagesContainer.getChildren().add(buildMessageBubble(msg));
        }

        if (messages.isEmpty()) {
            Label hint = new Label("Commencez la conversation ! 👋");
            hint.setStyle("-fx-text-fill: #adb5bd; -fx-font-size: 14; -fx-padding: 40;");
            hint.setAlignment(Pos.CENTER);
            VBox.setVgrow(hint, Priority.ALWAYS);
            messagesContainer.getChildren().add(hint);
        } else {
            // If the last message is from the friend, pre-generate smart replies
            Message lastMsg = messages.get(messages.size() - 1);
            if (!lastMsg.getSenderId().equals(currentUser.getUserId())) {
                fetchSmartReplies(lastMsg.getContent());
            }
        }

        // Scroll to bottom after layout
        Platform.runLater(() -> messagesScrollPane.setVvalue(1.0));
    }

    private HBox buildMessageBubble(Message msg) {
        boolean isMine = msg.getSenderId().equals(currentUser.getUserId());

        HBox row = new HBox();
        row.setAlignment(isMine ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        row.setPadding(new Insets(2, 0, 2, 0));

        VBox bubble = new VBox(4);
        bubble.setMaxWidth(420);
        bubble.setPadding(new Insets(10, 14, 8, 14));
        bubble.setStyle(isMine
                ? "-fx-background-color: linear-gradient(to bottom right, #667eea, #764ba2); -fx-background-radius: 18 18 4 18;"
                : "-fx-background-color: white; -fx-background-radius: 18 18 18 4; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 6, 0, 0, 2);");

        Label contentLabel = new Label(msg.getContent());
        contentLabel.setWrapText(true);
        contentLabel.setStyle("-fx-font-size: 14; -fx-text-fill: " + (isMine ? "white;" : "#1c1e21;"));

        String timeStr = msg.getSentAt().format(TIME_FMT);
        if (msg.getSentAt().toLocalDate().isBefore(java.time.LocalDate.now())) {
            timeStr = msg.getSentAt().format(DATE_FMT);
        }
        Label timeLabel = new Label(timeStr + (isMine ? (msg.isRead() ? "  ✓✓" : "  ✓") : ""));
        timeLabel.setStyle("-fx-font-size: 10; -fx-text-fill: " + (isMine ? "rgba(255,255,255,0.75);" : "#adb5bd;"));
        timeLabel.setAlignment(Pos.CENTER_RIGHT);

        bubble.getChildren().addAll(contentLabel, timeLabel);
        row.getChildren().add(bubble);

        if (isMine) {
            HBox.setMargin(bubble, new Insets(0, 0, 0, 80));
        } else {
            HBox.setMargin(bubble, new Insets(0, 80, 0, 0));
        }

        return row;
    }

    private HBox buildDateSeparator(String dateStr) {
        HBox row = new HBox();
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(8, 0, 8, 0));
        Label label = new Label(dateStr.equals(java.time.LocalDate.now().toString())
                ? "Aujourd'hui" : dateStr);
        label.setStyle("-fx-background-color: rgba(0,0,0,0.06); -fx-background-radius: 12; "
                + "-fx-padding: 4 14; -fx-font-size: 12; -fx-text-fill: #65676b;");
        row.getChildren().add(label);
        return row;
    }

    // ══════════════════════════════════════════════════════════════
    //  Typing indicator
    // ══════════════════════════════════════════════════════════════

    /** Shows an animated "..." bubble at the bottom of the chat, auto-hides after 4 s. */
    private void showTypingIndicator() {
        hideTypingIndicator(); // clear any existing one first

        // Three dot labels that animate in sequence
        Label d1 = new Label("●");
        Label d2 = new Label("●");
        Label d3 = new Label("●");
        String dimStyle  = "-fx-text-fill: #adb5bd; -fx-font-size: 10;";
        String fullStyle = "-fx-text-fill: #667eea; -fx-font-size: 14;";
        d1.setStyle(dimStyle); d2.setStyle(dimStyle); d3.setStyle(dimStyle);

        HBox dots = new HBox(4, d1, d2, d3);
        dots.setAlignment(Pos.CENTER);
        dots.setPadding(new Insets(8, 14, 8, 14));
        dots.setStyle("-fx-background-color: white; -fx-background-radius: 18 18 18 4; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 6, 0, 0, 2);");

        typingBubble = new HBox(dots);
        typingBubble.setAlignment(Pos.CENTER_LEFT);
        typingBubble.setPadding(new Insets(2, 0, 2, 0));
        HBox.setMargin(dots, new Insets(0, 80, 0, 0));

        // Bouncing dot animation — cycle through each dot
        typingDotAnim = new Timeline(
            new KeyFrame(javafx.util.Duration.ZERO, e -> {
                d1.setStyle(fullStyle); d2.setStyle(dimStyle); d3.setStyle(dimStyle);
            }),
            new KeyFrame(javafx.util.Duration.millis(350), e -> {
                d1.setStyle(dimStyle); d2.setStyle(fullStyle); d3.setStyle(dimStyle);
            }),
            new KeyFrame(javafx.util.Duration.millis(700), e -> {
                d1.setStyle(dimStyle); d2.setStyle(dimStyle); d3.setStyle(fullStyle);
            }),
            new KeyFrame(javafx.util.Duration.millis(1050))
        );
        typingDotAnim.setCycleCount(Animation.INDEFINITE);
        typingDotAnim.play();

        messagesContainer.getChildren().add(typingBubble);
        messagesScrollPane.setVvalue(1.0);

        // Auto-hide after 4 s in case the signal is not followed by a real message
        PauseTransition autoHide = new PauseTransition(javafx.util.Duration.seconds(4));
        autoHide.setOnFinished(e -> hideTypingIndicator());
        autoHide.play();
    }

    private void hideTypingIndicator() {
        if (typingDotAnim != null) { typingDotAnim.stop(); typingDotAnim = null; }
        if (typingBubble != null) {
            messagesContainer.getChildren().remove(typingBubble);
            typingBubble = null;
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  AI Smart Replies (Groq LLaMA)
    // ══════════════════════════════════════════════════════════════

    /**
     * Calls Groq LLaMA with the last few messages as context and renders
     * up to 3 clickable reply chips above the input bar.
     */
    private void fetchSmartReplies(String lastIncomingMessage) {
        if (selectedFriend == null) return;

        // Build a short context from the last 6 messages
        List<Message> history = messageController.getConversation(
                currentUser.getUserId(), selectedFriend.getUserId());
        int start = Math.max(0, history.size() - 6);
        StringBuilder context = new StringBuilder();
        for (Message m : history.subList(start, history.size())) {
            boolean mine = m.getSenderId().equals(currentUser.getUserId());
            context.append(mine ? "Moi" : selectedFriend.getUsername())
                   .append(": ").append(m.getContent()).append("\n");
        }
        // Also include the very latest message if not already in history
        context.append(selectedFriend.getUsername()).append(": ").append(lastIncomingMessage).append("\n");

        final String ctx = context.toString();
        aiExecutor.submit(() -> {
            try {
                List<String> replies = callGroqLlama(ctx);
                System.out.println("[SmartReplies] Parsed " + replies.size() + " replies: " + replies);
                Platform.runLater(() -> renderSmartReplies(replies));
            } catch (Exception e) {
                System.err.println("[SmartReplies] Exception: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }

    /** POST to Groq /v1/chat/completions and parse 3 short reply suggestions. */
    private List<String> callGroqLlama(String conversationContext) throws Exception {
        String systemPrompt = "Tu es un assistant de messagerie. "
                + "En te basant sur la conversation fournie, génère exactement 3 réponses courtes et naturelles "
                + "que l'utilisateur ('Moi') pourrait envoyer. "
                + "Réponds UNIQUEMENT avec les 3 réponses, une par ligne, sans numérotation ni ponctuation initiale. "
                + "Chaque réponse doit faire maximum 8 mots.";

        String requestBody = "{"
                + "\"model\":\"" + GROQ_CHAT_MODEL + "\","
                + "\"max_tokens\":120,"
                + "\"temperature\":0.8,"
                + "\"messages\":["
                + "{\"role\":\"system\",\"content\":" + jsonString(systemPrompt) + "},"
                + "{\"role\":\"user\",\"content\":" + jsonString(conversationContext) + "}"
                + "]}";

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_CHAT_URL))
                .header("Authorization", "Bearer " + GROQ_API_KEY)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200)
            throw new RuntimeException("Groq LLaMA error " + response.statusCode());

        System.out.println("[SmartReplies] Raw JSON: " + response.body());

        // Parse the "content" field from the first choice
        String json    = response.body();
        int ci         = json.indexOf("\"content\"");
        if (ci == -1) return Collections.emptyList();
        int q1 = json.indexOf('"', ci + 9);
        // find closing quote, respecting escaped chars
        int q2 = q1 + 1;
        while (q2 < json.length()) {
            char c = json.charAt(q2);
            if (c == '\\') { q2 += 2; continue; }
            if (c == '"')  break;
            q2++;
        }
        String content = json.substring(q1 + 1, q2)
                .replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\");

        // Split into up to 3 non-empty lines
        List<String> replies = new ArrayList<>();
        for (String line : content.split("\n")) {
            String trimmed = line.replaceAll("^[\\d.\\-*) ]+", "").trim();
            if (!trimmed.isEmpty()) replies.add(trimmed);
            if (replies.size() == 3) break;
        }
        return replies;
    }

    /** Renders the reply chip buttons in the smartRepliesBar. */
    private void renderSmartReplies(List<String> replies) {
        System.out.println("[SmartReplies] renderSmartReplies called, replies=" + replies.size()
                + ", smartRepliesBar null=" + (smartRepliesBar == null));
        if (replies.isEmpty()) return;

        smartRepliesBar.getChildren().clear();

        Label hint = new Label("✨");
        hint.setStyle("-fx-font-size: 14;");
        smartRepliesBar.getChildren().add(hint);

        for (String reply : replies) {
            Button chip = new Button(reply);
            chip.setStyle("-fx-background-color: #f0f2f5; -fx-text-fill: #1c1e21; "
                    + "-fx-font-size: 12; -fx-background-radius: 20; -fx-padding: 6 14; "
                    + "-fx-cursor: hand; -fx-border-color: #d0d2d6; -fx-border-radius: 20; -fx-border-width: 1;");
            chip.setOnMouseEntered(e -> chip.setStyle(
                    "-fx-background-color: #667eea; -fx-text-fill: white; "
                    + "-fx-font-size: 12; -fx-background-radius: 20; -fx-padding: 6 14; "
                    + "-fx-cursor: hand; -fx-border-color: #667eea; -fx-border-radius: 20; -fx-border-width: 1;"));
            chip.setOnMouseExited(e -> chip.setStyle(
                    "-fx-background-color: #f0f2f5; -fx-text-fill: #1c1e21; "
                    + "-fx-font-size: 12; -fx-background-radius: 20; -fx-padding: 6 14; "
                    + "-fx-cursor: hand; -fx-border-color: #d0d2d6; -fx-border-radius: 20; -fx-border-width: 1;"));
            chip.setOnAction(e -> {
                messageInputField.setText(reply);
                messageInputField.positionCaret(reply.length());
                messageInputField.requestFocus();
                hideSmartReplies();
            });
            smartRepliesBar.getChildren().add(chip);
        }

        smartRepliesBar.setVisible(true);
        smartRepliesBar.setManaged(true);
    }

    private void hideSmartReplies() {
        smartRepliesBar.getChildren().clear();
        smartRepliesBar.setVisible(false);
        smartRepliesBar.setManaged(false);
    }

    /** Escapes a Java string for embedding in a JSON string literal. */
    private static String jsonString(String s) {
        return "\"" + s.replace("\\", "\\\\").replace("\"", "\\\"")
                       .replace("\n", "\\n").replace("\r", "\\r") + "\"";
    }

    // ══════════════════════════════════════════════════════════════
    //  Voice-to-text (Groq Whisper)
    // ══════════════════════════════════════════════════════════════

    @FXML
    private void handleMicButton() {
        if (!isRecording) {
            startRecording();
        } else {
            stopRecordingAndTranscribe();
        }
    }

    private void startRecording() {
        try {
            // Standard 16 kHz mono PCM — ideal for Whisper
            AudioFormat format = new AudioFormat(16_000f, 16, 1, true, false);
            DataLine.Info info  = new DataLine.Info(TargetDataLine.class, format);

            if (!AudioSystem.isLineSupported(info)) {
                showAlert("Microphone", "Aucun microphone détecté sur cet appareil.");
                return;
            }

            recordingLine = (TargetDataLine) AudioSystem.getLine(info);
            recordingLine.open(format);
            recordingLine.start();
            isRecording = true;

            // Visual feedback: red pulsing mic
            micButton.setText("⏹");
            micButton.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; "
                    + "-fx-font-size: 18; -fx-background-radius: 50%; "
                    + "-fx-min-width: 50; -fx-min-height: 50; -fx-cursor: hand; "
                    + "-fx-effect: dropshadow(gaussian, rgba(231,76,60,0.6), 12, 0, 0, 0);");

            // Capture audio bytes in background
            sttExecutor.submit(() -> {
                try {
                    tempAudioFile = File.createTempFile("ghrami_voice_", ".wav");
                    tempAudioFile.deleteOnExit();
                    AudioSystem.write(
                            new AudioInputStream(recordingLine),
                            AudioFileFormat.Type.WAVE,
                            tempAudioFile
                    );
                } catch (IOException e) {
                    Platform.runLater(() -> showAlert("Erreur", "Impossible d'enregistrer l'audio."));
                }
            });

        } catch (LineUnavailableException e) {
            showAlert("Microphone", "Impossible d'accéder au microphone : " + e.getMessage());
        }
    }

    private void stopRecordingAndTranscribe() {
        if (recordingLine != null) {
            recordingLine.stop();
            recordingLine.close();
        }
        isRecording = false;

        // Restore mic button to "thinking" state while API call is in flight
        micButton.setText("⏳");
        micButton.setStyle("-fx-background-color: #f0f2f5; -fx-text-fill: #65676b; "
                + "-fx-font-size: 18; -fx-background-radius: 50%; "
                + "-fx-min-width: 50; -fx-min-height: 50; -fx-cursor: hand;");
        micButton.setDisable(true);

        sttExecutor.submit(() -> {
            try {
                String transcript = transcribeWithGroq(tempAudioFile);
                Platform.runLater(() -> {
                    if (transcript != null && !transcript.isBlank()) {
                        String existing = messageInputField.getText();
                        messageInputField.setText(
                                existing.isBlank() ? transcript : existing + " " + transcript
                        );
                        messageInputField.positionCaret(messageInputField.getText().length());
                    }
                    resetMicButton();
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    showAlert("Groq STT", "Erreur lors de la transcription : " + e.getMessage());
                    resetMicButton();
                });
            } finally {
                if (tempAudioFile != null) tempAudioFile.delete();
            }
        });
    }

    /** Sends the WAV file to Groq Whisper and returns the transcript text. */
    private String transcribeWithGroq(File audioFile) throws Exception {
        String boundary = "----GhramiBoundary" + System.currentTimeMillis();
        byte[] audioBytes = Files.readAllBytes(audioFile.toPath());

        // Build multipart body manually (no external lib needed)
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(body, false, "UTF-8");

        // Part 1: model
        ps.print("--" + boundary + "\r\n");
        ps.print("Content-Disposition: form-data; name=\"model\"\r\n\r\n");
        ps.print(GROQ_MODEL + "\r\n");

        // Part 2: language (French UI — change or remove to auto-detect)
        ps.print("--" + boundary + "\r\n");
        ps.print("Content-Disposition: form-data; name=\"language\"\r\n\r\n");
        ps.print("fr\r\n");

        // Part 3: response_format
        ps.print("--" + boundary + "\r\n");
        ps.print("Content-Disposition: form-data; name=\"response_format\"\r\n\r\n");
        ps.print("json\r\n");

        // Part 4: audio file
        ps.print("--" + boundary + "\r\n");
        ps.print("Content-Disposition: form-data; name=\"file\"; filename=\"audio.wav\"\r\n");
        ps.print("Content-Type: audio/wav\r\n\r\n");
        ps.flush();
        body.write(audioBytes);
        ps.print("\r\n--" + boundary + "--\r\n");
        ps.flush();

        HttpClient  client  = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(GROQ_STT_URL))
                .header("Authorization", "Bearer " + GROQ_API_KEY)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Groq API error " + response.statusCode() + ": " + response.body());
        }

        // Parse {"text":"..."} — no JSON lib dependency needed
        String json = response.body();
        int start = json.indexOf("\"text\"");
        if (start == -1) throw new RuntimeException("Réponse inattendue : " + json);
        int q1 = json.indexOf('"', start + 7);
        int q2 = json.indexOf('"', q1 + 1);
        // Handle escaped quotes inside the value
        while (q2 > 0 && json.charAt(q2 - 1) == '\\') q2 = json.indexOf('"', q2 + 1);
        return json.substring(q1 + 1, q2)
                   .replace("\\\"", "\"")
                   .replace("\\n", "\n")
                   .replace("\\\\", "\\");
    }

    private void resetMicButton() {
        micButton.setText("🎤");
        micButton.setStyle("-fx-background-color: #f0f2f5; -fx-text-fill: #65676b; "
                + "-fx-font-size: 18; -fx-background-radius: 50%; "
                + "-fx-min-width: 50; -fx-min-height: 50; -fx-cursor: hand;");
        micButton.setDisable(false);
    }

    // ══════════════════════════════════════════════════════════════
    //  Send message
    // ══════════════════════════════════════════════════════════════

    @FXML
    private void handleSendMessage() {
        if (selectedFriend == null) return;
        String text = messageInputField.getText().trim();
        if (text.isEmpty()) return;

        messageInputField.clear();
        hideSmartReplies();

        Message sent = messageController.send(currentUser.getUserId(), selectedFriend.getUserId(), text);
        if (sent != null) {
            // Create notification for recipient
            String senderName = currentUser.getFullName() != null ? currentUser.getFullName() : currentUser.getUsername();
            notificationController.notifyNewMessage(selectedFriend.getUserId(), senderName, currentUser.getUserId());

            // Relay message to recipient in real-time via socket
            ChatClient.getInstance().send(selectedFriend.getUserId(), text);

            // Append bubble immediately without full reload
            messagesContainer.getChildren().add(buildMessageBubble(sent));
            Platform.runLater(() -> messagesScrollPane.setVvalue(1.0));

            // Refresh conversation list order
            loadFriendsAndConversations();
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  New conversation picker dialog
    // ══════════════════════════════════════════════════════════════

    @FXML
    private void handleNewConversation() {
        if (acceptedFriends.isEmpty()) {
            showAlert("Aucun ami", "Ajoutez des amis pour pouvoir leur envoyer des messages.");
            return;
        }

        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Nouveau message");
        dialog.setHeaderText("Sélectionnez un ami");

        ButtonType selectType = new ButtonType("Ouvrir", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(selectType, ButtonType.CANCEL);

        ListView<User> listView = new ListView<>();
        listView.getItems().addAll(acceptedFriends);
        listView.setPrefHeight(300);
        listView.setCellFactory(lv -> new ListCell<User>() {
            @Override
            protected void updateItem(User u, boolean empty) {
                super.updateItem(u, empty);
                if (empty || u == null) { setText(null); setGraphic(null); }
                else setText((u.getFullName() != null ? u.getFullName() : u.getUsername())
                        + " (@" + u.getUsername() + ")");
            }
        });

        dialog.getDialogPane().setContent(listView);
        dialog.setResultConverter(btn -> btn == selectType ? listView.getSelectionModel().getSelectedItem() : null);

        dialog.showAndWait().ifPresent(friend -> {
            if (friend != null) openConversation(friend);
        });
    }

    // ══════════════════════════════════════════════════════════════
    //  Navigation
    // ══════════════════════════════════════════════════════════════

    @FXML
    private void handleBackToFeed() {
        try {
            Stage stage = (Stage) searchConvField.getScene().getWindow();
            double width = stage.getWidth();
            double height = stage.getHeight();
            boolean wasMaximized = stage.isMaximized();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/UserFeed.fxml"));
            Scene scene = new Scene(loader.load(), width, height);
            scene.getStylesheets().add(getClass().getResource("/css/social-style.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Ghrami - Mon Feed");
            if (wasMaximized) stage.setMaximized(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ══════════════════════════════════════════════════════════════
    //  Helpers
    // ══════════════════════════════════════════════════════════════

    private void refreshUnreadBadge() {
        int total = messageController.countUnread(currentUser.getUserId());
        if (total > 0) {
            unreadBadgeLabel.setText(total + " non lu" + (total > 1 ? "s" : ""));
            unreadBadgeLabel.setVisible(true);
        } else {
            unreadBadgeLabel.setVisible(false);
        }
    }

    private void loadAvatarImage(Circle circle, User user) {
        if (user.getProfilePicture() == null || user.getProfilePicture().isEmpty()) {
            circle.setStyle("-fx-fill: linear-gradient(to bottom right, #667eea, #764ba2);");
            return;
        }
        try {
            String pic = user.getProfilePicture();
            javafx.scene.image.Image image;
            if (pic.startsWith("http://") || pic.startsWith("https://")) {
                image = new javafx.scene.image.Image(pic, true);
            } else {
                Path p = Paths.get("src/main/resources/images/profile_pictures/" + pic);
                if (!Files.exists(p)) {
                    circle.setStyle("-fx-fill: linear-gradient(to bottom right, #667eea, #764ba2);");
                    return;
                }
                image = new javafx.scene.image.Image(p.toUri().toString());
            }
            circle.setFill(new ImagePattern(image));
        } catch (Exception ignored) {
            circle.setStyle("-fx-fill: linear-gradient(to bottom right, #667eea, #764ba2);");
        }
    }

    private String getInitial(User user) {
        String name = user.getFullName() != null ? user.getFullName() : user.getUsername();
        return name.isEmpty() ? "?" : String.valueOf(Character.toUpperCase(name.charAt(0)));
    }

    private void showAlert(String title, String message) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(message);
        a.showAndWait();
    }
}
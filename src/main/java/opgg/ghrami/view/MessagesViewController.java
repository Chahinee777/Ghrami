package opgg.ghrami.view;

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
import opgg.ghrami.util.SessionManager;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
    //  Send message
    // ══════════════════════════════════════════════════════════════

    @FXML
    private void handleSendMessage() {
        if (selectedFriend == null) return;
        String text = messageInputField.getText().trim();
        if (text.isEmpty()) return;

        messageInputField.clear();

        Message sent = messageController.send(currentUser.getUserId(), selectedFriend.getUserId(), text);
        if (sent != null) {
            // Create notification for recipient
            String senderName = currentUser.getFullName() != null ? currentUser.getFullName() : currentUser.getUsername();
            notificationController.notifyNewMessage(selectedFriend.getUserId(), senderName, currentUser.getUserId());

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

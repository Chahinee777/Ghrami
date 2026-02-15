package opgg.ghrami.view;

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
import opgg.ghrami.controller.UserController;
import opgg.ghrami.model.Friendship;
import opgg.ghrami.model.FriendshipStatus;
import opgg.ghrami.model.User;
import opgg.ghrami.util.SessionManager;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class FriendsViewController implements Initializable {
    
    @FXML private Label myFriendsCountLabel;
    @FXML private Label pendingRequestsCountLabel;
    @FXML private Label totalUsersCountLabel;
    
    @FXML private TextField browseSearchField;
    @FXML private TextField friendsSearchField;
    
    @FXML private VBox browseUsersContainer;
    @FXML private VBox pendingRequestsContainer;
    @FXML private VBox myFriendsContainer;
    
    private SessionManager sessionManager;
    private UserController userController;
    private FriendshipController friendshipController;
    private User currentUser;
    
    private List<User> allUsers;
    private List<Friendship> myFriendships;
    private List<Friendship> pendingRequests;
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        sessionManager = SessionManager.getInstance();
        userController = new UserController();
        friendshipController = new FriendshipController();
        
        loadCurrentUser();
        setupSearchListeners();
        loadAllData();
    }
    
    private void loadCurrentUser() {
        long userId = sessionManager.getUserId();
        currentUser = userController.findById((int) userId);
    }
    
    private void setupSearchListeners() {
        browseSearchField.textProperty().addListener((obs, old, newVal) -> filterBrowseUsers());
        friendsSearchField.textProperty().addListener((obs, old, newVal) -> filterMyFriends());
    }
    
    private void loadAllData() {
        loadBrowseUsers();
        loadPendingRequests();
        loadMyFriends();
        updateCounts();
    }
    
    private void loadBrowseUsers() {
        allUsers = userController.findAll().stream()
                .filter(user -> user.getUserId() != currentUser.getUserId() && user.getUserId() != 0)
                .collect(Collectors.toList());
        displayBrowseUsers();
    }
    
    private void displayBrowseUsers() {
        browseUsersContainer.getChildren().clear();
        
        if (allUsers.isEmpty()) {
            Label emptyLabel = new Label("Aucun utilisateur trouvé");
            emptyLabel.setStyle("-fx-text-fill: #65676b; -fx-font-size: 14;");
            browseUsersContainer.getChildren().add(emptyLabel);
            return;
        }
        
        for (User user : allUsers) {
            browseUsersContainer.getChildren().add(createUserCard(user));
        }
    }
    
    private void filterBrowseUsers() {
        String searchText = browseSearchField.getText().toLowerCase().trim();
        
        List<User> filtered = allUsers.stream()
                .filter(user -> searchText.isEmpty() || 
                        user.getUsername().toLowerCase().contains(searchText) ||
                        user.getFullName().toLowerCase().contains(searchText) ||
                        (user.getEmail() != null && user.getEmail().toLowerCase().contains(searchText)))
                .collect(Collectors.toList());
        
        browseUsersContainer.getChildren().clear();
        for (User user : filtered) {
            browseUsersContainer.getChildren().add(createUserCard(user));
        }
    }
    
    private HBox createUserCard(User user) {
        HBox card = new HBox(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 20; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 3); " +
                "-fx-border-color: #e4e6eb; -fx-border-radius: 15; -fx-border-width: 1;");
        card.setPrefWidth(900);
        
        // Avatar
        Circle avatar = new Circle(35);
        if (user.getProfilePicture() != null && !user.getProfilePicture().isEmpty()) {
            try {
                Path imagePath = Paths.get("src/main/resources/images/profile_pictures/" + user.getProfilePicture());
                if (Files.exists(imagePath)) {
                    javafx.scene.image.Image image = new javafx.scene.image.Image(imagePath.toUri().toString());
                    avatar.setFill(new ImagePattern(image));
                } else {
                    avatar.setStyle("-fx-fill: linear-gradient(135deg, #667eea, #764ba2);");
                }
            } catch (Exception e) {
                avatar.setStyle("-fx-fill: linear-gradient(135deg, #667eea, #764ba2);");
            }
        } else {
            avatar.setStyle("-fx-fill: linear-gradient(135deg, #667eea, #764ba2);");
        }
        
        // User Info
        VBox userInfo = new VBox(5);
        userInfo.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(userInfo, Priority.ALWAYS);
        
        HBox nameRow = new HBox(10);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        
        Label nameLabel = new Label(user.getFullName());
        nameLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #1c1e21;");
        
        // Online status
        Label statusLabel = new Label(user.isOnline() ? "🟢 En ligne" : "⚫ Hors ligne");
        statusLabel.setStyle("-fx-font-size: 11; -fx-text-fill: " + 
                (user.isOnline() ? "#22c55e" : "#6b7280") + ";");
        
        nameRow.getChildren().addAll(nameLabel, statusLabel);
        
        Label usernameLabel = new Label("@" + user.getUsername());
        usernameLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #65676b;");
        
        Label locationLabel = new Label("📍 " + (user.getLocation() != null ? user.getLocation() : "Non renseigné"));
        locationLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #65676b;");
        
        userInfo.getChildren().addAll(nameRow, usernameLabel, locationLabel);
        
        // Action Button
        Button actionBtn = createActionButton(user);
        
        card.getChildren().addAll(avatar, userInfo, actionBtn);
        return card;
    }
    
    private Button createActionButton(User user) {
        Optional<Friendship> friendshipOpt = friendshipController.getFriendshipBetweenUsers(
                currentUser.getUserId(), user.getUserId());
        
        Button btn = new Button();
        btn.setMinWidth(150);
        btn.setStyle("-fx-background-radius: 20; -fx-padding: 10 20; -fx-font-size: 13; " +
                "-fx-font-weight: bold; -fx-cursor: hand;");
        
        if (friendshipOpt.isPresent()) {
            Friendship friendship = friendshipOpt.get();
            
            switch (friendship.getStatus()) {
                case ACCEPTED:
                    btn.setText("✓ Ami");
                    btn.setStyle(btn.getStyle() + "-fx-background-color: #d4edda; -fx-text-fill: #155724;");
                    btn.setOnAction(e -> handleRemoveFriend(friendship));
                    break;
                    
                case PENDING:
                    if (friendship.getUser1Id().equals(currentUser.getUserId())) {
                        btn.setText("⏳ En attente");
                        btn.setStyle(btn.getStyle() + "-fx-background-color: #fff3cd; -fx-text-fill: #856404;");
                        btn.setOnAction(e -> handleCancelRequest(friendship));
                    } else {
                        btn.setText("Répondre");
                        btn.setStyle(btn.getStyle() + "-fx-background-color: #0095f6; -fx-text-fill: white;");
                        btn.setOnAction(e -> handleRespondToRequest(friendship, user));
                    }
                    break;
                    
                case REJECTED:
                    btn.setText("Demande rejetée");
                    btn.setStyle(btn.getStyle() + "-fx-background-color: #f8d7da; -fx-text-fill: #721c24;");
                    btn.setDisable(true);
                    break;
            }
        } else {
            btn.setText("➕ Ajouter");
            btn.setStyle(btn.getStyle() + "-fx-background-color: #0095f6; -fx-text-fill: white;");
            btn.setOnAction(e -> handleSendFriendRequest(user, btn));
        }
        
        return btn;
    }
    
    private void handleSendFriendRequest(User user, Button btn) {
        Friendship friendship = friendshipController.sendFriendRequest(
                currentUser.getUserId(), user.getUserId());
        
        if (friendship != null) {
            showAlert("Succès", "Demande d'amitié envoyée à " + user.getUsername(), Alert.AlertType.INFORMATION);
            btn.setText("⏳ En attente");
            btn.setStyle("-fx-background-color: #fff3cd; -fx-text-fill: #856404; " +
                    "-fx-background-radius: 20; -fx-padding: 10 20; -fx-font-size: 13; " +
                    "-fx-font-weight: bold; -fx-cursor: hand;");
            btn.setOnAction(e -> handleCancelRequest(friendship));
            updateCounts();
        } else {
            showAlert("Erreur", "Impossible d'envoyer la demande", Alert.AlertType.ERROR);
        }
    }
    
    private void handleCancelRequest(Friendship friendship) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Annuler la demande");
        confirmation.setHeaderText("Voulez-vous annuler cette demande d'amitié ?");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (friendshipController.delete(friendship.getFriendshipId())) {
                showAlert("Succès", "Demande annulée", Alert.AlertType.INFORMATION);
                loadBrowseUsers();
            }
        }
    }
    
    private void handleRemoveFriend(Friendship friendship) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Retirer l'ami");
        confirmation.setHeaderText("Êtes-vous sûr de vouloir retirer cet ami ?");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (friendshipController.delete(friendship.getFriendshipId())) {
                showAlert("Succès", "Ami retiré", Alert.AlertType.INFORMATION);
                loadAllData();
            }
        }
    }
    
    private void handleRespondToRequest(Friendship friendship, User user) {
        // Show dialog with accept/reject buttons
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Demande d'amitié");
        alert.setHeaderText("Demande de " + user.getUsername());
        alert.setContentText("Voulez-vous accepter cette demande d'amitié ?");
        
        ButtonType acceptBtn = new ButtonType("✓ Accepter");
        ButtonType rejectBtn = new ButtonType("✗ Refuser");
        ButtonType cancelBtn = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        
        alert.getButtonTypes().setAll(acceptBtn, rejectBtn, cancelBtn);
        
        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent()) {
            if (result.get() == acceptBtn) {
                if (friendshipController.acceptFriendRequest(friendship.getFriendshipId())) {
                    showAlert("Succès", "Vous êtes maintenant ami avec " + user.getUsername(), Alert.AlertType.INFORMATION);
                    loadAllData();
                }
            } else if (result.get() == rejectBtn) {
                if (friendshipController.rejectFriendRequest(friendship.getFriendshipId())) {
                    showAlert("Info", "Demande refusée", Alert.AlertType.INFORMATION);
                    loadAllData();
                }
            }
        }
    }
    
    private void loadPendingRequests() {
        pendingRequests = friendshipController.getPendingRequestsForUser(currentUser.getUserId());
        displayPendingRequests();
    }
    
    private void displayPendingRequests() {
        pendingRequestsContainer.getChildren().clear();
        
        if (pendingRequests.isEmpty()) {
            Label emptyLabel = new Label("😊 Aucune demande en attente");
            emptyLabel.setStyle("-fx-text-fill: #65676b; -fx-font-size: 14; -fx-alignment: center;");
            pendingRequestsContainer.getChildren().add(emptyLabel);
            return;
        }
        
        for (Friendship friendship : pendingRequests) {
            User requester = userController.findById(friendship.getUser1Id().intValue());
            if (requester != null) {
                pendingRequestsContainer.getChildren().add(createPendingRequestCard(friendship, requester));
            }
        }
    }
    
    private HBox createPendingRequestCard(Friendship friendship, User user) {
        HBox card = new HBox(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: #fffbf0; -fx-background-radius: 15; -fx-padding: 20; " +
                "-fx-effect: dropshadow(gaussian, rgba(255,152,0,0.2), 10, 0, 0, 3); " +
                "-fx-border-color: #ff9800; -fx-border-radius: 15; -fx-border-width: 2;");
        card.setPrefWidth(900);
        
        // Avatar
        Circle avatar = new Circle(35);
        if (user.getProfilePicture() != null && !user.getProfilePicture().isEmpty()) {
            try {
                Path imagePath = Paths.get("src/main/resources/images/profile_pictures/" + user.getProfilePicture());
                if (Files.exists(imagePath)) {
                    javafx.scene.image.Image image = new javafx.scene.image.Image(imagePath.toUri().toString());
                    avatar.setFill(new ImagePattern(image));
                } else {
                    avatar.setStyle("-fx-fill: linear-gradient(135deg, #667eea, #764ba2);");
                }
            } catch (Exception e) {
                avatar.setStyle("-fx-fill: linear-gradient(135deg, #667eea, #764ba2);");
            }
        } else {
            avatar.setStyle("-fx-fill: linear-gradient(135deg, #667eea, #764ba2);");
        }
        
        // User Info
        VBox userInfo = new VBox(5);
        userInfo.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(userInfo, Priority.ALWAYS);
        
        Label nameLabel = new Label(user.getFullName());
        nameLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #1c1e21;");
        
        Label usernameLabel = new Label("@" + user.getUsername() + " veut devenir votre ami");
        usernameLabel.setStyle("-fx-font-size: 13; -fx- fill: #65676b;");
        
        Label dateLabel = new Label("📅 " + friendship.getCreatedDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm")));
        dateLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #65676b;");
        
        userInfo.getChildren().addAll(nameLabel, usernameLabel, dateLabel);
        
        // Action Buttons
        HBox actionButtons = new HBox(10);
        
        Button acceptBtn = new Button("✓ Accepter");
        acceptBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-padding: 10 20; -fx-background-radius: 20; -fx-cursor: hand; -fx-font-size: 13;");
        acceptBtn.setOnAction(e -> {
            if (friendshipController.acceptFriendRequest(friendship.getFriendshipId())) {
                showAlert("Succès", "Vous êtes maintenant ami avec " + user.getUsername(), Alert.AlertType.INFORMATION);
                loadAllData();
            }
        });
        
        Button rejectBtn = new Button("✗ Refuser");
        rejectBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-padding: 10 20; -fx-background-radius: 20; -fx-cursor: hand; -fx-font-size: 13;");
        rejectBtn.setOnAction(e -> {
            if (friendshipController.rejectFriendRequest(friendship.getFriendshipId())) {
                showAlert("Info", "Demande refusée", Alert.AlertType.INFORMATION);
                loadAllData();
            }
        });
        
        actionButtons.getChildren().addAll(acceptBtn, rejectBtn);
        
        card.getChildren().addAll(avatar, userInfo, actionButtons);
        return card;
    }
    
    private void loadMyFriends() {
        myFriendships = friendshipController.getAcceptedFriendships(currentUser.getUserId());
        displayMyFriends();
    }
    
    private void displayMyFriends() {
        myFriendsContainer.getChildren().clear();
        
        if (myFriendships.isEmpty()) {
            Label emptyLabel = new Label("Vous n'avez pas encore d'amis\nCommencez à explorer!");
            emptyLabel.setStyle("-fx-text-fill: #65676b; -fx-font-size: 14; -fx-text-alignment: center;");
            emptyLabel.setWrapText(true);
            myFriendsContainer.getChildren().add(emptyLabel);
            return;
        }
        
        for (Friendship friendship : myFriendships) {
            Long friendId = friendship.getUser1Id().equals(currentUser.getUserId()) ? 
                    friendship.getUser2Id() : friendship.getUser1Id();
            User friend = userController.findById(friendId.intValue());
            
            if (friend != null) {
                myFriendsContainer.getChildren().add(createFriendCard(friendship, friend));
            }
        }
    }
    
    private void filterMyFriends() {
        String searchText = friendsSearchField.getText().toLowerCase().trim();
        
        myFriendsContainer.getChildren().clear();
        
        for (Friendship friendship : myFriendships) {
            Long friendId = friendship.getUser1Id().equals(currentUser.getUserId()) ? 
                    friendship.getUser2Id() : friendship.getUser1Id();
            User friend = userController.findById(friendId.intValue());
            
            if (friend != null) {
                if (searchText.isEmpty() || 
                    friend.getUsername().toLowerCase().contains(searchText) ||
                    friend.getFullName().toLowerCase().contains(searchText)) {
                    myFriendsContainer.getChildren().add(createFriendCard(friendship, friend));
                }
            }
        }
    }
    
    private HBox createFriendCard(Friendship friendship, User friend) {
        HBox card = new HBox(20);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 20; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 3); " +
                "-fx-border-color: #4CAF50; -fx-border-radius: 15; -fx-border-width: 2;");
        card.setPrefWidth(900);
        
        // Avatar
        Circle avatar = new Circle(35);
        if (friend.getProfilePicture() != null && !friend.getProfilePicture().isEmpty()) {
            try {
                Path imagePath = Paths.get("src/main/resources/images/profile_pictures/" + friend.getProfilePicture());
                if (Files.exists(imagePath)) {
                    javafx.scene.image.Image image = new javafx.scene.image.Image(imagePath.toUri().toString());
                    avatar.setFill(new ImagePattern(image));
                } else {
                    avatar.setStyle("-fx-fill: linear-gradient(135deg, #667eea, #764ba2);");
                }
            } catch (Exception e) {
                avatar.setStyle("-fx-fill: linear-gradient(135deg, #667eea, #764ba2);");
            }
        } else {
            avatar.setStyle("-fx-fill: linear-gradient(135deg, #667eea, #764ba2);");
        }
        
        // Friend Info
        VBox friendInfo = new VBox(5);
        friendInfo.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(friendInfo, Priority.ALWAYS);
        
        HBox nameRow = new HBox(10);
        nameRow.setAlignment(Pos.CENTER_LEFT);
        
        Label nameLabel = new Label(friend.getFullName());
        nameLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #1c1e21;");
        
        Label statusLabel = new Label(friend.isOnline() ? "🟢 En ligne" : "⚫ Hors ligne");
        statusLabel.setStyle("-fx-font-size: 11; -fx-text-fill: " + 
                (friend.isOnline() ? "#22c55e" : "#6b7280") + ";");
        
        nameRow.getChildren().addAll(nameLabel, statusLabel);
        
        Label usernameLabel = new Label("@" + friend.getUsername());
        usernameLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #65676b;");
        
        Label sinceLabel = new Label("✓ Amis depuis " + 
                friendship.getAcceptedDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy")));
        sinceLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #4CAF50; -fx-font-weight: bold;");
        
        friendInfo.getChildren().addAll(nameRow, usernameLabel, sinceLabel);
        
        // Action Buttons
        HBox actionButtons = new HBox(10);
        
        Button messageBtn = new Button("💬 Message");
        messageBtn.setStyle("-fx-background-color: #0095f6; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-padding: 10 20; -fx-background-radius: 20; -fx-cursor: hand; -fx-font-size: 13;");
        messageBtn.setOnAction(e -> showAlert("Info", "Fonctionnalité de messagerie à venir!", Alert.AlertType.INFORMATION));
        
        Button removeBtn = new Button("🗑️");
        removeBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-weight: bold; " +
                "-fx-padding: 10 15; -fx-background-radius: 20; -fx-cursor: hand; -fx-font-size: 13;");
        removeBtn.setTooltip(new Tooltip("Retirer l'ami"));
        removeBtn.setOnAction(e -> handleRemoveFriend(friendship));
        
        actionButtons.getChildren().addAll(messageBtn, removeBtn);
        
        card.getChildren().addAll(avatar, friendInfo, actionButtons);
        return card;
    }
    
    private void updateCounts() {
        myFriendsCountLabel.setText(String.valueOf(friendshipController.getAcceptedFriendships(currentUser.getUserId()).size()));
        pendingRequestsCountLabel.setText(String.valueOf(friendshipController.getPendingRequestsForUser(currentUser.getUserId()).size()));
        totalUsersCountLabel.setText(String.valueOf(userController.findAll().size() - 1)); // Exclude admin
    }
    
    @FXML
    private void handleRefreshBrowse() {
        loadBrowseUsers();
        updateCounts();
    }
    
    @FXML
    private void handleRefreshPending() {
        loadPendingRequests();
        updateCounts();
    }
    
    @FXML
    private void handleRefreshFriends() {
        loadMyFriends();
        updateCounts();
    }
    
    @FXML
    private void handleBackToFeed() {
        try {
            Stage stage = (Stage) browseUsersContainer.getScene().getWindow();
            double width = stage.getWidth();
            double height = stage.getHeight();
            boolean wasMaximized = stage.isMaximized();
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/UserFeed.fxml"));
            Scene scene = new Scene(loader.load(), width, height);
            scene.getStylesheets().add(getClass().getResource("/css/social-style.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Ghrami - Feed");
            if (wasMaximized) {
                stage.setMaximized(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger le feed: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

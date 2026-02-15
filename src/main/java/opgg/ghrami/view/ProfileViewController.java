package opgg.ghrami.view;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import opgg.ghrami.controller.BadgeController;
import opgg.ghrami.controller.FriendshipController;
import opgg.ghrami.controller.UserController;
import opgg.ghrami.model.Badge;
import opgg.ghrami.model.Friendship;
import opgg.ghrami.model.User;
import opgg.ghrami.util.PasswordUtil;
import opgg.ghrami.util.SessionManager;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ProfileViewController implements Initializable {
    
    // Profile Info
    @FXML private Circle profileImageCircle;
    @FXML private Label fullNameLabel;
    @FXML private Label usernameLabel;
    @FXML private Label postsCountLabel;
    @FXML private Label friendsCountLabel;
    @FXML private Label badgesCountLabel;
    @FXML private Label onlineStatusLabel;
    @FXML private Button toggleStatusButton;
    
    // Edit Fields
    @FXML private TextField fullNameField;
    @FXML private TextField usernameField;
    @FXML private TextField emailField;
    @FXML private TextField locationField;
    @FXML private TextArea bioArea;
    
    // Password Fields
    @FXML private PasswordField newPasswordField;
    @FXML private PasswordField confirmPasswordField;
    
    // Containers (no more tables!)
    @FXML private FlowPane badgesContainer;
    @FXML private VBox friendsContainer;
    
    private SessionManager sessionManager;
    private UserController userController;
    private BadgeController badgeController;
    private FriendshipController friendshipController;
    private User currentUser;
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        sessionManager = SessionManager.getInstance();
        userController = new UserController();
        badgeController = new BadgeController();
        friendshipController = new FriendshipController();
        
        loadUserData();
        loadBadges();
        loadFriends();
        updateOnlineStatusDisplay();
    }
    
    private void loadUserData() {
        try {
            long userId = sessionManager.getUserId();
            if (userId == 0) {
                showAlert("Erreur", "Session invalide. Veuillez vous reconnecter.");
                return;
            }
            
            currentUser = userController.findById((int) userId);
            
            if (currentUser == null) {
                showAlert("Erreur", "Utilisateur introuvable. Veuillez vous reconnecter.");
                return;
            }
        
        if (currentUser != null) {
            fullNameLabel.setText(currentUser.getFullName());
            usernameLabel.setText("@" + currentUser.getUsername());
            postsCountLabel.setText("0");
            
            // Load profile image
            loadProfileImage();
            
            // Fill edit fields
            fullNameField.setText(currentUser.getFullName());
            usernameField.setText(currentUser.getUsername());
            emailField.setText(currentUser.getEmail());
            locationField.setText(currentUser.getLocation() != null ? currentUser.getLocation() : "");
            bioArea.setText(currentUser.getBio() != null ? currentUser.getBio() : "");
        }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors du chargement du profil: " + e.getMessage());
        }
    }
    
    private void loadBadges() {
        if (currentUser.getUserId() == null) {
            badgesCountLabel.setText("0");
            return;
        }
        
        // Use the optimized method to get badges for this user
        List<Badge> userBadges = badgeController.findByUserId(currentUser.getUserId());
        
        badgesCountLabel.setText(String.valueOf(userBadges.size()));
        
        // Clear and populate badges container with cards
        badgesContainer.getChildren().clear();
        
        if (userBadges.isEmpty()) {
            VBox emptyBox = new VBox(10);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setStyle("-fx-padding: 40; -fx-background-color: #f8f9fa; -fx-background-radius: 15;");
            
            Label icon = new Label("🏆");
            icon.setStyle("-fx-font-size: 50; -fx-opacity: 0.5;");
            
            Label emptyLabel = new Label("No badges yet");
            emptyLabel.setStyle("-fx-text-fill: #65676b; -fx-font-size: 16; -fx-font-weight: bold;");
            
            Label subLabel = new Label("Keep being active to earn your first badge!");
            subLabel.setStyle("-fx-text-fill: #65676b; -fx-font-size: 12;");
            subLabel.setWrapText(true);
            subLabel.setMaxWidth(200);
            subLabel.setAlignment(Pos.CENTER);
            subLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
            
            emptyBox.getChildren().addAll(icon, emptyLabel, subLabel);
            badgesContainer.getChildren().add(emptyBox);
        } else {
            for (Badge badge : userBadges) {
                badgesContainer.getChildren().add(createBadgeCard(badge));
            }
        }
    }
    
    private VBox createBadgeCard(Badge badge) {
        VBox card = new VBox(10);
        card.setAlignment(Pos.CENTER);
        card.getStyleClass().add("badge-card");
        card.setPrefWidth(140);
        card.setMaxWidth(140);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; " +
                "-fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3); " +
                "-fx-border-color: #e4e6eb; -fx-border-radius: 15; -fx-border-width: 1; -fx-cursor: hand;");
        
        // Add hover effect
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 15; " +
                    "-fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(102,126,234,0.3), 15, 0, 0, 5); " +
                    "-fx-border-color: #667eea; -fx-border-radius: 15; -fx-border-width: 2; -fx-cursor: hand; " +
                    "-fx-scale-x: 1.05; -fx-scale-y: 1.05;");
        });
        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color: white; -fx-background-radius: 15; " +
                    "-fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3); " +
                    "-fx-border-color: #e4e6eb; -fx-border-radius: 15; -fx-border-width: 1; -fx-cursor: hand;");
        });
        
        // Badge Icon/Emoji - Intelligent icon selection
        String icon = getBadgeIcon(badge.getName());
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 45;");
        
        // Badge Name
        Label name = new Label(badge.getName());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 13; -fx-text-fill: #1c1e21;");
        name.setWrapText(true);
        name.setMaxWidth(120);
        name.setAlignment(Pos.CENTER);
        name.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        
        // Badge Description
        Label desc = new Label(badge.getDescription() != null && !badge.getDescription().isEmpty() 
            ? badge.getDescription() : "Special achievement");
        desc.setStyle("-fx-font-size: 10; -fx-text-fill: #65676b;");
        desc.setWrapText(true);
        desc.setMaxWidth(120);
        desc.setAlignment(Pos.CENTER);
        desc.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);
        
        // Earned Date
        String dateStr = badge.getEarnedDate() != null 
            ? badge.getEarnedDate().toLocalDate().toString() 
            : LocalDateTime.now().toLocalDate().toString();
        Label date = new Label("✓ " + dateStr);
        date.setStyle("-fx-font-size: 9; -fx-text-fill: #667eea; -fx-font-weight: bold; " +
                "-fx-background-color: #e8eaf6; -fx-padding: 4 8; -fx-background-radius: 10;");
        
        card.getChildren().addAll(iconLabel, name, desc, date);
        
        // Click to view details
        card.setOnMouseClicked(e -> showBadgeDetails(badge));
        
        return card;
    }
    
    /**
     * Get appropriate icon based on badge name
     */
    private String getBadgeIcon(String badgeName) {
        if (badgeName == null) return "🏆";
        
        String name = badgeName.toLowerCase();
        
        // Check for keywords and return appropriate icon
        if (name.contains("first") || name.contains("welcome") || name.contains("aboard")) return "🎉";
        if (name.contains("friend") || name.contains("social")) return "🤝";
        if (name.contains("creator") || name.contains("content") || name.contains("post")) return "📝";
        if (name.contains("vip") || name.contains("premium") || name.contains("diamond")) return "💎";
        if (name.contains("star") || name.contains("rising")) return "🌟";
        if (name.contains("fire") || name.contains("streak") || name.contains("on fire")) return "🔥";
        if (name.contains("goal") || name.contains("achiever") || name.contains("target")) return "🎯";
        if (name.contains("connect") || name.contains("butterfly")) return "🦋";
        if (name.contains("champion") || name.contains("winner")) return "🏆";
        if (name.contains("creative") || name.contains("artist")) return "🎨";
        if (name.contains("support") || name.contains("helper")) return "⭐";
        if (name.contains("early") || name.contains("pioneer") || name.contains("beta")) return "🚀";
        if (name.contains("gold") || name.contains("golden")) return "🥇";
        if (name.contains("silver")) return "🥈";
        if (name.contains("bronze")) return "🥉";
        if (name.contains("expert") || name.contains("master")) return "👨‍🎓";
        if (name.contains("love") || name.contains("heart")) return "❤️";
        if (name.contains("verified") || name.contains("authentic")) return "✅";
        
        return "🏆"; // Default
    }
    
    /**
     * Show detailed badge information in a modal
     */
    private void showBadgeDetails(Badge badge) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Badge Details");
        alert.setHeaderText(getBadgeIcon(badge.getName()) + " " + badge.getName());
        
        VBox content = new VBox(10);
        content.setStyle("-fx-padding: 15;");
        
        Label descLabel = new Label("Description:");
        descLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");
        
        Label descValue = new Label(badge.getDescription() != null && !badge.getDescription().isEmpty() 
            ? badge.getDescription() : "No description available");
        descValue.setWrapText(true);
        descValue.setMaxWidth(350);
        descValue.setStyle("-fx-font-size: 12; -fx-text-fill: #65676b;");
        
        Label earnedLabel = new Label("Earned on:");
        earnedLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12;");
        
        String earnedDate = badge.getEarnedDate() != null 
            ? badge.getEarnedDate().format(java.time.format.DateTimeFormatter.ofPattern("MMMM dd, yyyy 'at' HH:mm"))
            : "Unknown date";
        Label earnedValue = new Label(earnedDate);
        earnedValue.setStyle("-fx-font-size: 12; -fx-text-fill: #667eea; -fx-font-weight: bold;");
        
        content.getChildren().addAll(descLabel, descValue, new Separator(), earnedLabel, earnedValue);
        
        alert.getDialogPane().setContent(content);
        alert.getDialogPane().setStyle("-fx-background-color: white; -fx-background-radius: 20;");
        
        alert.showAndWait();
    }
    
    private void loadFriends() {
        List<Friendship> allFriendships = friendshipController.findAll();
        Long currentUserId = currentUser.getUserId();
        if (currentUserId == null) {
            friendsCountLabel.setText("0");
            return;
        }
        
        List<FriendInfo> friends = allFriendships.stream()
                .filter(f -> f.getUser1Id() != null && f.getUser2Id() != null 
                        && (f.getUser1Id().equals(currentUserId) || f.getUser2Id().equals(currentUserId)) 
                        && "ACCEPTED".equals(f.getStatus().name()))
                .map(f -> {
                    Long friendId = f.getUser1Id().equals(currentUserId) ? f.getUser2Id() : f.getUser1Id();
                    User friend = userController.findById(friendId.intValue());
                    if (friend != null) {
                        return new FriendInfo(friend.getUserId().intValue(), friend.getUsername(), friend.getEmail(), 
                                f.getStatus().name(), f.getCreatedDate().toString());
                    }
                    return null;
                })
                .filter(info -> info != null)
                .collect(Collectors.toList());
        
        friendsCountLabel.setText(String.valueOf(friends.size()));
        
        // Clear and populate friends container with cards
        friendsContainer.getChildren().clear();
        
        if (friends.isEmpty()) {
            Label emptyLabel = new Label("Aucun ami pour le moment\nCommencez à vous connecter!");
            emptyLabel.setStyle("-fx-text-fill: #65676b; -fx-font-size: 14; -fx-text-alignment: center;");
            emptyLabel.setWrapText(true);
            friendsContainer.getChildren().add(emptyLabel);
        } else {
            for (FriendInfo friend : friends) {
                friendsContainer.getChildren().add(createFriendCard(friend));
            }
        }
    }
    
    private HBox createFriendCard(FriendInfo friend) {
        HBox card = new HBox(15);
        card.setAlignment(Pos.CENTER_LEFT);
        card.getStyleClass().add("friend-card");
        card.setPadding(new Insets(15));
        
        // Friend Avatar
        Circle avatar = new Circle(25);
        avatar.setStyle("-fx-fill: linear-gradient(135deg, #667eea 0%, #764ba2 100%);");
        
        // Friend Info
        VBox info = new VBox(5);
        info.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(info, Priority.ALWAYS);
        
        Label name = new Label(friend.getUsername());
        name.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-text-fill: #1c1e21;");
        
        Label email = new Label(friend.getEmail());
        email.setStyle("-fx-font-size: 12; -fx-text-fill: #65676b;");
        
        Label since = new Label("Ami depuis: " + friend.getFriendSince().substring(0, 10));
        since.setStyle("-fx-font-size: 11; -fx-text-fill: #667eea;");
        
        info.getChildren().addAll(name, email, since);
        
        // Message Button
        Button msgBtn = new Button("💬");
        msgBtn.setStyle("-fx-background-color: #f0f2f5; -fx-background-radius: 20; -fx-padding: 8 15; -fx-cursor: hand;");
        msgBtn.setOnAction(e -> showAlert("Info", "Fonctionnalité de messagerie à venir!"));
        
        card.getChildren().addAll(avatar, info, msgBtn);
        
        return card;
    }
    
    @FXML
    private void handleSaveProfile() {
        try {
            String fullName = fullNameField.getText().trim();
            String username = usernameField.getText().trim();
            String location = locationField.getText().trim();
            String bio = bioArea.getText().trim();
            
            if (fullName.isEmpty() || username.isEmpty()) {
                showAlert("Erreur", "Le nom complet et le nom d'utilisateur sont obligatoires");
                return;
            }
            
            // Check if username is taken by another user
            if (!username.equals(currentUser.getUsername())) {
                User existingUser = userController.findAll().stream()
                        .filter(u -> u.getUsername().equals(username) && u.getUserId() != currentUser.getUserId())
                        .findFirst()
                        .orElse(null);
                
                if (existingUser != null) {
                    showAlert("Erreur", "Ce nom d'utilisateur est déjà pris");
                    return;
                }
            }
            
            currentUser.setFullName(fullName);
            currentUser.setUsername(username);
            currentUser.setLocation(location);
            currentUser.setBio(bio);
            
            userController.update(currentUser);
            
            // Update session username
            sessionManager.setUsername(username);
            
            loadUserData();
            showAlert("Succès", "Profil mis à jour avec succès!");
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors de la mise à jour: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleChangePassword() {
        try {
            String newPassword = newPasswordField.getText();
            String confirmPassword = confirmPasswordField.getText();
            
            if (newPassword.isEmpty() || confirmPassword.isEmpty()) {
                showAlert("Erreur", "Veuillez remplir tous les champs");
                return;
            }
            
            if (newPassword.length() < 6) {
                showAlert("Erreur", "Le mot de passe doit contenir au moins 6 caractères");
                return;
            }
            
            if (!newPassword.equals(confirmPassword)) {
                showAlert("Erreur", "Les mots de passe ne correspondent pas");
                return;
            }
            
            currentUser.setPassword(PasswordUtil.hashPassword(newPassword));
            userController.update(currentUser);
            
            newPasswordField.clear();
            confirmPasswordField.clear();
            
            showAlert("Succès", "Mot de passe changé avec succès!");
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors du changement de mot de passe: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleChangePhoto() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une photo de profil");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif")
        );
        
        Stage stage = (Stage) fullNameField.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);
        
        if (selectedFile != null) {
            try {
                // Validate file size (max 5MB)
                long fileSizeInMB = selectedFile.length() / (1024 * 1024);
                if (fileSizeInMB > 5) {
                    showAlert("Erreur", "L'image est trop grande. Taille maximale: 5MB");
                    return;
                }
                
                // Validate file extension
                String originalFileName = selectedFile.getName().toLowerCase();
                if (!originalFileName.endsWith(".png") && !originalFileName.endsWith(".jpg") && 
                    !originalFileName.endsWith(".jpeg") && !originalFileName.endsWith(".gif")) {
                    showAlert("Erreur", "Format d'image invalide. Utilisez PNG, JPG, JPEG ou GIF.");
                    return;
                }
                
                // Create filename with user ID and timestamp
                String extension = selectedFile.getName().substring(selectedFile.getName().lastIndexOf("."));
                String fileName = currentUser.getUserId() + "_" + System.currentTimeMillis() + extension;
                
                // Create destination path
                Path resourcesPath = Paths.get("src/main/resources/images/profile_pictures");
                if (!Files.exists(resourcesPath)) {
                    Files.createDirectories(resourcesPath);
                }
                
                Path destination = resourcesPath.resolve(fileName);
                
                // Copy file to resources
                Files.copy(selectedFile.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);
                
                // Update user profile picture in database
                currentUser.setProfilePicture(fileName);
                userController.update(currentUser);
                
                System.out.println("Profile picture updated: " + fileName);
                
                // Load the new image
                loadProfileImage();
                
                showAlert("Succès", "Photo de profil mise à jour avec succès!");
            } catch (IOException e) {
                e.printStackTrace();
                showAlert("Erreur", "Erreur lors du téléchargement de l'image: " + e.getMessage());
            }
        }
    }
    
    private void loadProfileImage() {
        // Try to load profile picture from profilePicture field
        if (currentUser.getProfilePicture() != null && !currentUser.getProfilePicture().isEmpty()) {
            try {
                Path imagePath = Paths.get("src/main/resources/images/profile_pictures/" + currentUser.getProfilePicture());
                if (Files.exists(imagePath)) {
                    javafx.scene.image.Image image = new javafx.scene.image.Image(
                        imagePath.toUri().toString()
                    );
                    profileImageCircle.setFill(new javafx.scene.paint.ImagePattern(image));
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // Default gradient if no image
        profileImageCircle.setFill(
            javafx.scene.paint.Color.web("#667eea")
        );
    }
    
    @FXML
    private void handleRefreshBadges() {
        loadBadges();
    }
    
    @FXML
    private void handleRefreshFriends() {
        loadFriends();
    }
    
    @FXML
    private void handleBackToDashboard() {
        try {
            System.out.println("Back button clicked - loading UserFeed...");
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/UserFeed.fxml"));
            loader.load();
            
            Stage stage = (Stage) fullNameLabel.getScene().getWindow();
            Scene scene = new Scene(loader.getRoot(), 1400, 900);
            scene.getStylesheets().add(getClass().getResource("/css/social-style.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Ghrami - Feed");
            System.out.println("Successfully loaded UserFeed!");
        } catch (Exception e) {
            System.err.println("ERROR in handleBackToDashboard: " + e.getMessage());
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors du retour au feed: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleToggleOnlineStatus() {
        try {
            if (currentUser == null) {
                showAlert("Erreur", "Utilisateur non trouvé");
                return;
            }
            
            // Toggle the online status
            boolean newStatus = !currentUser.isOnline();
            currentUser.setOnline(newStatus);
            userController.update(currentUser);
            
            // Update the display
            updateOnlineStatusDisplay();
            
            String statusText = newStatus ? "En ligne" : "Hors ligne";
            System.out.println("Status changed to: " + statusText);
            
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors du changement de statut: " + e.getMessage());
        }
    }
    
    private void updateOnlineStatusDisplay() {
        if (currentUser == null || onlineStatusLabel == null || toggleStatusButton == null) {
            return;
        }
        
        if (currentUser.isOnline()) {
            onlineStatusLabel.setText("🟢 En ligne");
            onlineStatusLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #22c55e;");
            toggleStatusButton.setText("Passer Hors ligne");
            toggleStatusButton.setStyle("-fx-background-color: #ef4444; -fx-text-fill: white; " +
                    "-fx-background-radius: 20; -fx-padding: 10 20; -fx-font-size: 13; " +
                    "-fx-font-weight: bold; -fx-cursor: hand;");
        } else {
            onlineStatusLabel.setText("⚫ Hors ligne");
            onlineStatusLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #6b7280;");
            toggleStatusButton.setText("Passer En ligne");
            toggleStatusButton.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; " +
                    "-fx-background-radius: 20; -fx-padding: 10 20; -fx-font-size: 13; " +
                    "-fx-font-weight: bold; -fx-cursor: hand;");
        }
    }
    
    @FXML
    private void handleLogout() {
        try {
            // Set user offline before logout
            if (currentUser != null) {
                currentUser.setOnline(false);
                userController.update(currentUser);
                System.out.println("User set to offline on logout: " + currentUser.getUsername());
            }
            
            sessionManager.logout();
            
            // Navigate to login
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/LoginView.fxml"));
            Scene scene = new Scene(loader.load(), 450, 600);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            Stage stage = (Stage) fullNameLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Ghrami - Connexion");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors de la déconnexion: " + e.getMessage());
        }
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(title.equals("Erreur") ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    // Helper class for friends table
    public static class FriendInfo {
        private int userId;
        private String username;
        private String email;
        private String status;
        private String friendSince;
        
        public FriendInfo(int userId, String username, String email, String status, String friendSince) {
            this.userId = userId;
            this.username = username;
            this.email = email;
            this.status = status;
            this.friendSince = friendSince;
        }
        
        public int getUserId() { return userId; }
        public String getUsername() { return username; }
        public String getEmail() { return email; }
        public String getStatus() { return status; }
        public String getFriendSince() { return friendSince; }
    }
}

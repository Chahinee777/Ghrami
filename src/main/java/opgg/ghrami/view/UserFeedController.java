package opgg.ghrami.view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.paint.ImagePattern;
import javafx.stage.Stage;
import opgg.ghrami.controller.FriendshipController;
import opgg.ghrami.controller.UserController;
import opgg.ghrami.model.Friendship;
import opgg.ghrami.model.User;
import opgg.ghrami.util.SessionManager;

import java.io.File;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ResourceBundle;

public class UserFeedController implements Initializable {
    
    // Top Nav Elements
    @FXML private TextField searchField;
    @FXML private Circle menuProfileCircle;
    @FXML private Circle sidebarProfileCircle;
    @FXML private Label userNameLabel;
    @FXML private Label userEmailLabel;
    @FXML private Label postsCountLabel;
    @FXML private Label friendsCountLabel;
    @FXML private Label hobbiesCountLabel;
    
    // Post Creation
    @FXML private TextField postTextField;
    @FXML private VBox feedContainer;
    
    private SessionManager sessionManager;
    private UserController userController;
    private FriendshipController friendshipController;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        sessionManager = SessionManager.getInstance();
        userController = new UserController();
        friendshipController = new FriendshipController();
        loadUserInfo();
        loadFeed();
    }
    
    private void loadUserInfo() {
        try {
            if (sessionManager == null || !sessionManager.isLoggedIn()) {
                System.err.println("❌ Session invalid in loadUserInfo");
                return;
            }
            
            String username = sessionManager.getUsername();
            String email = sessionManager.getEmail();
            
            userNameLabel.setText(username != null ? username : "Utilisateur");
            userEmailLabel.setText(email != null ? email : "");
            postsCountLabel.setText("0");
            
            // Load friend count
            long userId = sessionManager.getUserId();
            List<Friendship> acceptedFriendships = friendshipController.getAcceptedFriendships(userId);
            friendsCountLabel.setText(String.valueOf(acceptedFriendships.size()));
            
            hobbiesCountLabel.setText("0");
            
            // Load profile image
            loadProfileImage();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Error loading user info: " + e.getMessage());
        }
    }
    
    private void loadProfileImage() {
        try {
            long userId = sessionManager.getUserId();
            if (userId == 0) {
                return;
            }
            
        User currentUser = userController.findById((int) userId);
        if (currentUser != null && currentUser.getProfilePicture() != null && !currentUser.getProfilePicture().isEmpty()) {
            try {
                Path imagePath = Paths.get("src/main/resources/images/profile_pictures/" + currentUser.getProfilePicture());
                if (Files.exists(imagePath)) {
                    javafx.scene.image.Image image = new javafx.scene.image.Image(
                        imagePath.toUri().toString()
                    );
                    ImagePattern pattern = new ImagePattern(image);
                    menuProfileCircle.setFill(pattern);
                    sidebarProfileCircle.setFill(pattern);
                    return;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // Keep default gradient if no image
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("❌ Error loading profile image: " + e.getMessage());
        }
    }
    
    private void loadFeed() {
        // TODO: Load posts from database
    }
    
    // Navigation Handlers
    @FXML
    private void handleHome() {
        System.out.println("Home clicked");
    }
    
    @FXML
    private void handleFriends() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/FriendsView.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/css/social-style.css").toExternalForm());
            
            Stage stage = (Stage) searchField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Ghrami - Mes Amis");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir la page des amis: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleNotifications() {
        System.out.println("Notifications clicked");
    }
    
    @FXML
    private void handleMessages() {
        System.out.println("Messages clicked");
    }
    
    @FXML
    private void handleProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/ProfileView.fxml"));
            Scene scene = new Scene(loader.load(), 900, 700);
            scene.getStylesheets().add(getClass().getResource("/css/social-style.css").toExternalForm());
            
            Stage stage = (Stage) searchField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Ghrami - Mon Profil");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not open profile: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleSettings() {
        System.out.println("Settings clicked");
    }
    
    @FXML
    private void handleLogout() {
        try {
            // Set user offline before logout
            long userId = sessionManager.getUserId();
            User user = userController.findById((int) userId);
            if (user != null) {
                user.setOnline(false);
                userController.update(user);
                System.out.println("✅ User set to offline: " + user.getUsername());
            }
            
            sessionManager.logout();
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/LoginView.fxml"));
            Scene scene = new Scene(loader.load(), 450, 600);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            
            Stage stage = (Stage) searchField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Ghrami - Login");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not logout: " + e.getMessage());
        }
    }
    
    // Sidebar Actions
    @FXML
    private void handleFeed() {
        System.out.println("Feed clicked");
    }
    
    @FXML
    private void handleMyHobbies() {
        System.out.println("My Hobbies clicked");
    }
    
    @FXML
    private void handleExplore() {
        System.out.println("Explore clicked");
    }
    
    @FXML
    private void handleMyFriends() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/FriendsView.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/css/social-style.css").toExternalForm());
            
            Stage stage = (Stage) searchField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Ghrami - Mes Amis");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir la page des amis: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleBadges() {
        System.out.println("Badges clicked");
    }
    
    @FXML
    private void handleEvents() {
        System.out.println("Events clicked");
    }
    
    @FXML
    private void handleGroups() {
        System.out.println("Groups clicked");
    }
    
    @FXML
    private void handleClasses() {
        System.out.println("Classes clicked");
    }
    
    @FXML
    private void handleVRRooms() {
        System.out.println("VR Rooms clicked");
    }
    
    @FXML
    private void handleMatchings() {
        System.out.println("Matchings clicked");
    }
    
    // Post Actions
    @FXML
    private void handleAddMedia() {
        showAlert("Info", "Fonctionnalité de téléchargement média à venir!");
    }
    
    @FXML
    private void handleAddHobby() {
        showAlert("Info", "Fonctionnalité d'ajout de hobby à venir!");
    }
    
    @FXML
    private void handleAddFeeling() {
        showAlert("Info", "Fonctionnalité d'ajout de sentiment à venir!");
    }
    
    @FXML
    private void handlePublish() {
        try {
            String postText = postTextField.getText();
            if (postText == null || postText.trim().isEmpty()) {
                showAlert("Erreur", "Le contenu du post ne peut pas être vide");
                return;
            }
            
            // TODO: Save post to database when Post model is created
            System.out.println("Publishing post: " + postText);
            postTextField.clear();
            showAlert("Succès", "Post publié avec succès!");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors de la publication: " + e.getMessage());
        }
    }
    
    private void showAlert(String title, String message) {
        Alert alert = new Alert(title.equals("Error") ? Alert.AlertType.ERROR : Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

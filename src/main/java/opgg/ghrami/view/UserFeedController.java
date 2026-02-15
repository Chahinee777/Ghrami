package opgg.ghrami.view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.shape.Circle;
import javafx.scene.paint.ImagePattern;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.stage.FileChooser;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import opgg.ghrami.controller.FriendshipController;
import opgg.ghrami.controller.HobbyController;
import opgg.ghrami.controller.PostController;
import opgg.ghrami.controller.CommentController;
import opgg.ghrami.controller.UserController;
import opgg.ghrami.model.Friendship;
import opgg.ghrami.model.Hobby;
import opgg.ghrami.model.Post;
import opgg.ghrami.model.Comment;
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
    private PostController postController;
    private CommentController commentController;
    private HobbyController hobbyController;
    private String selectedImagePath = null;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        sessionManager = SessionManager.getInstance();
        userController = new UserController();
        friendshipController = new FriendshipController();
        postController = PostController.getInstance();
        commentController = CommentController.getInstance();
        hobbyController = new HobbyController();
        loadUserInfo();
        loadFeed();
    }
    
    private void loadUserInfo() {
        try {
            if (sessionManager == null || !sessionManager.isLoggedIn()) {
                System.err.println("Session invalid in loadUserInfo");
                return;
            }
            
            // Load counts
            long userId = sessionManager.getUserId();
            
            // Load user details
            User currentUser = userController.findById((int) userId);
            if (currentUser != null) {
                userNameLabel.setText(currentUser.getFullName() != null ? currentUser.getFullName() : currentUser.getUsername());
                userEmailLabel.setText(currentUser.getEmail());
            }
            
            // Posts count
            int postsCount = postController.countPostsByUser(userId);
            postsCountLabel.setText(String.valueOf(postsCount));
            
            // Friends count
            List<Friendship> acceptedFriendships = friendshipController.getAcceptedFriendships(userId);
            friendsCountLabel.setText(String.valueOf(acceptedFriendships.size()));
            
            // Hobbies count
            List<Hobby> hobbies = hobbyController.findByUserId(userId);
            hobbiesCountLabel.setText(String.valueOf(hobbies.size()));
            
            // Load profile image
            loadProfileImage();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error loading user info: " + e.getMessage());
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
                Path imagePath = Paths.get("src/main/resources/images/profile_pictures/" + currentUser.getProfilePicture());
                if (Files.exists(imagePath)) {
                    javafx.scene.image.Image image = new javafx.scene.image.Image(
                        imagePath.toUri().toString()
                    );
                    ImagePattern pattern = new ImagePattern(image);
                    menuProfileCircle.setFill(pattern);
                    sidebarProfileCircle.setFill(pattern);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error loading profile image: " + e.getMessage());
        }
    }
    
    private void loadFeed() {
        try {
            if (feedContainer == null) {
                System.err.println("feedContainer is null");
                return;
            }
            
            feedContainer.getChildren().clear();
            
            long userId = sessionManager.getUserId();
            List<Post> posts = postController.getFeedForUser(userId);
            
            if (posts.isEmpty()) {
                // Show empty state
                Label emptyLabel = new Label("Aucun post pour le moment. Créez votre premier post ! 📝");
                emptyLabel.setStyle("-fx-font-size: 16; -fx-text-fill: #65676b; -fx-padding: 40;");
                feedContainer.getChildren().add(emptyLabel);
                return;
            }
            
            // Display posts
            for (Post post : posts) {
                VBox postCard = createPostCard(post);
                feedContainer.getChildren().add(postCard);
            }
            
        } catch (Exception e) {
            System.err.println("Error loading feed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private VBox createPostCard(Post post) {
        VBox card = new VBox(15);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 20; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 10, 0, 0, 3);");
        VBox.setMargin(card, new Insets(0, 0, 20, 0));
        
        // Header (author info)
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Circle avatar = new Circle(20);
        // Load author profile picture if available
        if (post.getAuthorProfilePicture() != null && !post.getAuthorProfilePicture().isEmpty()) {
            try {
                Path imagePath = Paths.get("src/main/resources/images/profile_pictures/" + post.getAuthorProfilePicture());
                if (Files.exists(imagePath)) {
                    Image profileImage = new Image(imagePath.toUri().toString());
                    avatar.setFill(new ImagePattern(profileImage));
                } else {
                    avatar.setStyle("-fx-fill: linear-gradient(135deg, #667eea 0%, #764ba2 100%);");
                }
            } catch (Exception e) {
                avatar.setStyle("-fx-fill: linear-gradient(135deg, #667eea 0%, #764ba2 100%);");
            }
        } else {
            avatar.setStyle("-fx-fill: linear-gradient(135deg, #667eea 0%, #764ba2 100%);");
        }
        
        VBox authorInfo = new VBox(2);
        Label authorName = new Label(post.getAuthorName() != null ? post.getAuthorName() : "Utilisateur");
        authorName.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-text-fill: #1c1e21;");
        
        Label postTime = new Label(formatTime(post.getCreatedAt()));
        postTime.setStyle("-fx-font-size: 12; -fx-text-fill: #65676b;");
        
        authorInfo.getChildren().addAll(authorName, postTime);
        header.getChildren().addAll(avatar, authorInfo);
        
        // Add edit/delete buttons if current user is post owner
        long currentUserId = sessionManager.getUserId();
        if (post.getUserId() == currentUserId) {
            HBox postActions = new HBox(5);
            postActions.setAlignment(Pos.CENTER_RIGHT);
            HBox.setHgrow(postActions, javafx.scene.layout.Priority.ALWAYS);
            
            Button editBtn = new Button("✏️");
            editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #65676b; -fx-cursor: hand; -fx-font-size: 16;");
            editBtn.setOnAction(e -> handleEditPost(post));
            
            Button deleteBtn = new Button("🗑️");
            deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #65676b; -fx-cursor: hand; -fx-font-size: 16;");
            deleteBtn.setOnAction(e -> handleDeletePost(post));
            
            postActions.getChildren().addAll(editBtn, deleteBtn);
            header.getChildren().add(postActions);
        }
        
        // Content
        Label contentLabel = new Label(post.getContent());
        contentLabel.setWrapText(true);
        contentLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #1c1e21; -fx-line-spacing: 5;");
        
        // Action buttons
        HBox actions = new HBox(20);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setStyle("-fx-padding: 10 0 0 0; -fx-border-color: #e4e6eb; -fx-border-width: 1 0 0 0;");
        
        Button likeBtn = new Button("❤️ J'aime");
        styleActionButton(likeBtn);
        
        Button commentBtn = new Button("💬 Commenter (" + post.getCommentsCount() + ")");
        styleActionButton(commentBtn);
        commentBtn.setOnAction(e -> handleViewComments(post));
        
        Button shareBtn = new Button("🔄 Partager");
        styleActionButton(shareBtn);
        
        actions.getChildren().addAll(likeBtn, commentBtn, shareBtn);
        
        card.getChildren().addAll(header, contentLabel);
        
        // Add image if exists
        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            try {
                Path imagePath = Paths.get("src/main/resources/images/posts/" + post.getImageUrl());
                if (Files.exists(imagePath)) {
                    Image postImage = new Image(imagePath.toUri().toString());
                    ImageView imageView = new ImageView(postImage);
                    imageView.setFitWidth(500);
                    imageView.setPreserveRatio(true);
                    imageView.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
                    card.getChildren().add(imageView);
                } else {
                    Label imageLabel = new Label("📷 Image: " + post.getImageUrl());
                    imageLabel.setStyle("-fx-text-fill: #667eea; -fx-font-style: italic;");
                    card.getChildren().add(imageLabel);
                }
            } catch (Exception e) {
                Label imageLabel = new Label("📷 Image: " + post.getImageUrl());
                imageLabel.setStyle("-fx-text-fill: #667eea; -fx-font-style: italic;");
                card.getChildren().add(imageLabel);
            }
        }
        
        card.getChildren().add(actions);
        
        return card;
    }
    
    private void styleActionButton(Button button) {
        button.setStyle("-fx-background-color: transparent; -fx-text-fill: #65676b; " +
                       "-fx-font-size: 13; -fx-font-weight: bold; -fx-cursor: hand; " +
                       "-fx-padding: 8 15;");
        button.setOnMouseEntered(e -> button.setStyle(
            "-fx-background-color: #f0f2f5; -fx-text-fill: #667eea; " +
            "-fx-font-size: 13; -fx-font-weight: bold; -fx-cursor: hand; " +
            "-fx-padding: 8 15; -fx-background-radius: 5;"));
        button.setOnMouseExited(e -> button.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #65676b; " +
            "-fx-font-size: 13; -fx-font-weight: bold; -fx-cursor: hand; " +
            "-fx-padding: 8 15;"));
    }
    
    private String formatTime(java.time.LocalDateTime dateTime) {
        if (dateTime == null) return "À l'instant";
        
        java.time.Duration duration = java.time.Duration.between(dateTime, java.time.LocalDateTime.now());
        long seconds = duration.getSeconds();
        
        if (seconds < 60) return "À l'instant";
        if (seconds < 3600) return (seconds / 60) + " min";
        if (seconds < 86400) return (seconds / 3600) + " h";
        if (seconds < 604800) return (seconds / 86400) + " j";
        
        return dateTime.format(java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy"));
    }
    
    private void handleViewComments(Post post) {
        try {
            Stage dialog = new Stage();
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.setTitle("Commentaires");
            
            VBox container = new VBox(15);
            container.setPadding(new Insets(20));
            container.setStyle("-fx-background-color: #f0f2f5;");
            
            // Post info
            Label postAuthor = new Label("Post de: " + post.getAuthorName());
            postAuthor.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
            
            Label postContent = new Label(post.getContent());
            postContent.setWrapText(true);
            postContent.setStyle("-fx-font-size: 13; -fx-padding: 10; -fx-background-color: white; " +
                                "-fx-background-radius: 10;");
            
            // Comments list
            ScrollPane commentsScroll = new ScrollPane();
            commentsScroll.setFitToWidth(true);
            commentsScroll.setPrefHeight(300);
            commentsScroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
            
            VBox commentsContainer = new VBox(10);
            commentsContainer.setStyle("-fx-background-color: transparent;");
            
            List<Comment> comments = commentController.findByPostId(post.getPostId());
            
            if (comments.isEmpty()) {
                Label noComments = new Label("Aucun commentaire pour le moment. Soyez le premier ! 💬");
                noComments.setStyle("-fx-text-fill: #65676b; -fx-font-style: italic; -fx-padding: 20;");
                commentsContainer.getChildren().add(noComments);
            } else {
                for (Comment comment : comments) {
                    VBox commentCard = createCommentCard(comment, post);
                    commentsContainer.getChildren().add(commentCard);
                }
            }
            
            commentsScroll.setContent(commentsContainer);
            
            // Add comment area
            HBox addCommentBox = new HBox(10);
            addCommentBox.setAlignment(Pos.CENTER_LEFT);
            
            TextField commentField = new TextField();
            commentField.setPromptText("Écrivez un commentaire...");
            commentField.setPrefWidth(400);
            commentField.setStyle("-fx-background-radius: 20; -fx-padding: 10 15;");
            
            Button sendBtn = new Button("Envoyer");
            sendBtn.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; " +
                            "-fx-background-radius: 20; -fx-padding: 10 20; -fx-font-weight: bold;");
            sendBtn.setOnAction(e -> {
                String content = commentField.getText().trim();
                if (!content.isEmpty()) {
                    Comment newComment = new Comment(
                        post.getPostId(),
                        sessionManager.getUserId(),
                        content
                    );
                    Comment created = commentController.create(newComment);
                    if (created != null) {
                        commentField.clear();
                        // Refresh comments
                        dialog.close();
                        handleViewComments(post);
                        // Reload feed to update comment count
                        loadFeed();
                        loadUserInfo();
                    }
                }
            });
            
            addCommentBox.getChildren().addAll(commentField, sendBtn);
            
            container.getChildren().addAll(postAuthor, postContent, 
                                          new Label("Commentaires:"), 
                                          commentsScroll, addCommentBox);
            
            Scene scene = new Scene(container, 600, 500);
            dialog.setScene(scene);
            dialog.show();
            
        } catch (Exception e) {
            System.err.println("Error viewing comments: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private VBox createCommentCard(Comment comment, Post post) {
        VBox card = new VBox(5);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 10; " +
                     "-fx-padding: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.05), 5, 0, 0, 2);");
        
        HBox headerBox = new HBox(10);
        headerBox.setAlignment(Pos.CENTER_LEFT);
        
        Label authorLabel = new Label(comment.getAuthorName() != null ? comment.getAuthorName() : "Utilisateur");
        authorLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12; -fx-text-fill: #667eea;");
        
        headerBox.getChildren().add(authorLabel);
        
        // Add edit/delete buttons if current user is comment owner OR post owner
        long currentUserId = sessionManager.getUserId();
        if (comment.getUserId() == currentUserId || post.getUserId() == currentUserId) {
            HBox commentActions = new HBox(5);
            commentActions.setAlignment(Pos.CENTER_RIGHT);
            HBox.setHgrow(commentActions, javafx.scene.layout.Priority.ALWAYS);
            
            if (comment.getUserId() == currentUserId) {
                Button editBtn = new Button("✏️");
                editBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #65676b; -fx-cursor: hand; -fx-font-size: 12;");
                editBtn.setOnAction(e -> handleEditComment(comment, post));
                commentActions.getChildren().add(editBtn);
            }
            
            Button deleteBtn = new Button("🗑️");
            deleteBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #65676b; -fx-cursor: hand; -fx-font-size: 12;");
            deleteBtn.setOnAction(e -> handleDeleteComment(comment, post));
            commentActions.getChildren().add(deleteBtn);
            
            headerBox.getChildren().add(commentActions);
        }
        
        Label contentLabel = new Label(comment.getContent());
        contentLabel.setWrapText(true);
        contentLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #1c1e21;");
        
        Label timeLabel = new Label(formatTime(comment.getCreatedAt()));
        timeLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #65676b;");
        
        card.getChildren().addAll(headerBox, contentLabel, timeLabel);
        return card;
    }
    
    private void handleEditPost(Post post) {
        TextInputDialog dialog = new TextInputDialog(post.getContent());
        dialog.setTitle("Modifier le post");
        dialog.setHeaderText("Modifiez votre post");
        dialog.setContentText("Contenu:");
        
        dialog.showAndWait().ifPresent(newContent -> {
            if (newContent != null && !newContent.trim().isEmpty()) {
                if (newContent.length() > 5000) {
                    showAlert("Erreur", "Le contenu est trop long (max 5000 caractères)");
                    return;
                }
                post.setContent(newContent.trim());
                Post updated = postController.update(post);
                if (updated != null) {
                    loadFeed();
                    showAlert("Succès", "Post modifié avec succès! ✅");
                } else {
                    showAlert("Erreur", "Impossible de modifier le post");
                }
            }
        });
    }
    
    private void handleDeletePost(Post post) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Supprimer le post");
        confirmDialog.setHeaderText("Voulez-vous vraiment supprimer ce post?");
        confirmDialog.setContentText("Cette action est irréversible et supprimera aussi tous les commentaires.");
        
        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean deleted = postController.delete(post.getPostId());
                if (deleted) {
                    loadFeed();
                    loadUserInfo();
                    showAlert("Succès", "Post supprimé avec succès! 🗑️");
                } else {
                    showAlert("Erreur", "Impossible de supprimer le post");
                }
            }
        });
    }
    
    private void handleEditComment(Comment comment, Post post) {
        TextInputDialog dialog = new TextInputDialog(comment.getContent());
        dialog.setTitle("Modifier le commentaire");
        dialog.setHeaderText("Modifiez votre commentaire");
        dialog.setContentText("Contenu:");
        
        dialog.showAndWait().ifPresent(newContent -> {
            if (newContent != null && !newContent.trim().isEmpty()) {
                comment.setContent(newContent.trim());
                Comment updated = commentController.update(comment);
                if (updated != null) {
                    handleViewComments(post);
                    showAlert("Succès", "Commentaire modifié avec succès! ✅");
                } else {
                    showAlert("Erreur", "Impossible de modifier le commentaire");
                }
            }
        });
    }
    
    private void handleDeleteComment(Comment comment, Post post) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Supprimer le commentaire");
        confirmDialog.setHeaderText("Voulez-vous vraiment supprimer ce commentaire?");
        confirmDialog.setContentText("Cette action est irréversible.");
        
        confirmDialog.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                boolean deleted = commentController.delete(comment.getCommentId());
                if (deleted) {
                    handleViewComments(post);
                    loadFeed();
                    showAlert("Succès", "Commentaire supprimé avec succès! 🗑️");
                } else {
                    showAlert("Erreur", "Impossible de supprimer le commentaire");
                }
            }
        });
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
                System.out.println("User set to offline: " + user.getUsername());
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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/HobbiesView.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/css/social-style.css").toExternalForm());
            
            Stage stage = (Stage) searchField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Ghrami - My Hobbies");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Could not open hobbies page: " + e.getMessage());
        }
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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/BadgesView.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/css/social-style.css").toExternalForm());
            
            Stage stage = (Stage) searchField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Ghrami - Mes Badges");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir la page des badges: " + e.getMessage());
        }
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
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/ClassMarketplace.fxml"));
            Scene scene = new Scene(loader.load(), 1400, 900);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            
            Stage stage = (Stage) searchField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Ghrami - Class Marketplace");
        } catch (Exception e) {
            System.err.println("Error loading Class Marketplace: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Unable to open Class Marketplace");
        }
    }
    
    @FXML
    private void handleMeetups() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/MeetingsView.fxml"));
            Scene scene = new Scene(loader.load(), 1400, 900);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            
            Stage stage = (Stage) searchField.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Ghrami - Meetups & Connections");
        } catch (Exception e) {
            System.err.println("Error loading Meetups: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Unable to open Meetups");
        }
    }
    
    @FXML
    private void handleVRRooms() {
        System.out.println("VR Rooms clicked");
    }
    
    // Post Actions
    @FXML
    private void handleAddMedia() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Sélectionner une image");
            fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
            );
            
            File selectedFile = fileChooser.showOpenDialog(postTextField.getScene().getWindow());
            
            if (selectedFile != null) {
                // Create posts directory if it doesn't exist
                Path postsDir = Paths.get("src/main/resources/images/posts/");
                if (!Files.exists(postsDir)) {
                    Files.createDirectories(postsDir);
                }
                
                // Generate unique filename
                String fileName = sessionManager.getUserId() + "_" + System.currentTimeMillis() + "_" + selectedFile.getName();
                Path destinationPath = postsDir.resolve(fileName);
                
                // Copy file to resources directory
                Files.copy(selectedFile.toPath(), destinationPath);
                
                selectedImagePath = fileName;
                showAlert("Succès", "Image sélectionnée: " + selectedFile.getName());
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors de la sélection de l'image: " + e.getMessage());
        }
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
            
            if (postText.length() > 5000) {
                showAlert("Erreur", "Le contenu du post est trop long (max 5000 caractères)");
                return;
            }
            
            // Create and save post
            long userId = sessionManager.getUserId();
            Post newPost;
            if (selectedImagePath != null && !selectedImagePath.isEmpty()) {
                newPost = new Post(userId, postText.trim(), selectedImagePath);
            } else {
                newPost = new Post(userId, postText.trim());
            }
            
            Post created = postController.create(newPost);
            
            if (created != null) {
                postTextField.clear();
                selectedImagePath = null; // Reset selected image
                loadFeed(); // Reload feed to show new post
                loadUserInfo(); // Update post count
                showAlert("Succès", "Post publié avec succès! 🎉");
            } else {
                showAlert("Erreur", "Impossible de publier le post");
            }
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

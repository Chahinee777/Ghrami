package opgg.ghrami.view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.ImagePattern;
import javafx.scene.shape.Circle;
import javafx.animation.PauseTransition;
import javafx.animation.FadeTransition;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import opgg.ghrami.controller.CommentController;
import opgg.ghrami.controller.FriendshipController;
import opgg.ghrami.controller.HobbyController;
import opgg.ghrami.controller.MessageController;
import opgg.ghrami.controller.NotificationController;
import opgg.ghrami.controller.PostController;
import opgg.ghrami.controller.StoryController;
import opgg.ghrami.controller.UserController;
import opgg.ghrami.model.Comment;
import opgg.ghrami.model.Friendship;
import opgg.ghrami.model.Hobby;
import opgg.ghrami.model.Notification;
import opgg.ghrami.model.Post;
import opgg.ghrami.model.Story;
import opgg.ghrami.model.User;
import opgg.ghrami.util.HuggingFaceService;
import opgg.ghrami.util.SessionManager;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
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
    @FXML private Label notificationsBadge;
    @FXML private Label messagesBadge;
    
    // Post Creation
    @FXML private Circle postProfileCircle;
    @FXML private TextArea postTextArea;
    @FXML private VBox feedContainer;
    @FXML private HBox storiesContainer;

    // AI feature controls
    @FXML private Label aiStatusLabel;
    @FXML private javafx.scene.image.ImageView aiImagePreview;
    @FXML private StackPane aiImageContainer;
    @FXML private Button btnAiComplete;
    @FXML private Button btnAiImage;

    // AI state
    private byte[] aiGeneratedImageBytes = null;
    
    private SessionManager sessionManager;
    private UserController userController;
    private FriendshipController friendshipController;
    private PostController postController;
    private CommentController commentController;
    private HobbyController hobbyController;
    private MessageController messageController;
    private NotificationController notificationController;
    private StoryController storyController;
    private String selectedImagePath = null;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        sessionManager = SessionManager.getInstance();
        userController = new UserController();
        friendshipController = new FriendshipController();
        postController = PostController.getInstance();
        commentController = CommentController.getInstance();
        hobbyController = new HobbyController();
        messageController = MessageController.getInstance();
        notificationController = NotificationController.getInstance();
        storyController = StoryController.getInstance();
        storyController.purgeExpired();
        loadUserInfo();
        loadFeed();
        loadStories();
        refreshNavBadges();
    }
    
    private void loadUserInfo() {
        try {
            if (sessionManager == null || !sessionManager.isLoggedIn()) {
                System.err.println("Session invalid in loadUserInfo");
                return;
            }
            
            long userId = sessionManager.getUserId();
            
            User currentUser = userController.findById((int) userId);
            if (currentUser != null) {
                userNameLabel.setText(currentUser.getFullName() != null ? currentUser.getFullName() : currentUser.getUsername());
                userEmailLabel.setText(currentUser.getEmail());
            }
            
            int postsCount = postController.countPostsByUser(userId);
            postsCountLabel.setText(String.valueOf(postsCount));
            
            List<Friendship> acceptedFriendships = friendshipController.getAcceptedFriendships(userId);
            friendsCountLabel.setText(String.valueOf(acceptedFriendships.size()));
            
            List<Hobby> hobbies = hobbyController.findByUserId(userId);
            hobbiesCountLabel.setText(String.valueOf(hobbies.size()));
            
            loadProfileImage();
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error loading user info: " + e.getMessage());
        }
    }
    
    private void loadProfileImage() {
        try {
            long userId = sessionManager.getUserId();
            if (userId == 0) return;

            User currentUser = userController.findById((int) userId);
            if (currentUser == null) return;
            String pic = currentUser.getProfilePicture();
            if (pic == null || pic.isEmpty()) return;

            if (pic.startsWith("http://") || pic.startsWith("https://")) {
                javafx.scene.image.Image image = new javafx.scene.image.Image(pic, true);

                image.progressProperty().addListener((obs, oldVal, newVal) -> {
                    if (newVal.doubleValue() >= 1.0 && !image.isError()) {
                        javafx.application.Platform.runLater(() -> {
                            ImagePattern pattern = new ImagePattern(image);
                            menuProfileCircle.setFill(pattern);
                            sidebarProfileCircle.setFill(pattern);
                            postProfileCircle.setFill(pattern);
                        });
                    }
                });

                image.errorProperty().addListener((obs, oldVal, hasError) -> {
                    if (hasError) {
                        System.err.println("Failed to load remote profile image: " + pic);
                    }
                });

            } else {
                Path imagePath = Paths.get("src/main/resources/images/profile_pictures/" + pic);
                if (!Files.exists(imagePath)) return;

                javafx.scene.image.Image image = new javafx.scene.image.Image(imagePath.toUri().toString());
                ImagePattern pattern = new ImagePattern(image);
                menuProfileCircle.setFill(pattern);
                sidebarProfileCircle.setFill(pattern);
                postProfileCircle.setFill(pattern);
            }

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error loading profile image: " + e.getMessage());
        }
    }
    
    private void loadStories() {
        if (storiesContainer == null) return;
        storiesContainer.getChildren().clear();
        long currentUserId = sessionManager.getUserId();

        User currentUser = userController.findById((int) currentUserId);
        String currentUserProfilePic = (currentUser != null && currentUser.getProfilePicture() != null) 
                ? currentUser.getProfilePicture() : null;

        boolean myHasStory = storyController.hasActiveStory(currentUserId);
        VBox addStory = buildStoryCard(currentUserId, "Ma Story", currentUserProfilePic, myHasStory, true);
        addStory.setOnMouseClicked(e -> handleCreateStory());
        storiesContainer.getChildren().add(addStory);

        List<Story> stories = storyController.getActiveStoriesForFeed(currentUserId);
        for (Story story : stories) {
            if (story.getUserId() == currentUserId) continue;
            String name = story.getAuthorName() != null ? story.getAuthorName() : "?";
            String firstName = name.split(" ")[0];
            VBox card = buildStoryCard(story.getUserId(), firstName, story.getAuthorProfilePicture(), true, false);
            card.setOnMouseClicked(e -> showStoryViewer(story, name));
            storiesContainer.getChildren().add(card);
        }
    }

    private VBox buildStoryCard(long userId, String label, String profilePicture, boolean hasStory, boolean showPlus) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setMinWidth(110); card.setMaxWidth(110);
        card.setStyle("-fx-cursor: hand;");

        javafx.scene.layout.StackPane stack = new javafx.scene.layout.StackPane();

        Circle ring = new Circle(44);
        if (hasStory) {
            ring.setStyle("-fx-fill: transparent; -fx-stroke: #667eea; -fx-stroke-width: 3;");
        } else {
            ring.setStyle("-fx-fill: transparent; -fx-stroke: #d0d0d0; -fx-stroke-width: 2;");
        }

        Circle avatar = new Circle(36);
        avatar.setStyle("-fx-fill: linear-gradient(135deg, #667eea 0%, #764ba2 100%);");
        if (profilePicture != null && !profilePicture.isEmpty()) {
            try {
                if (profilePicture.startsWith("http")) {
                    avatar.setFill(new ImagePattern(new Image(profilePicture, true)));
                } else {
                    Path pp = Paths.get("src/main/resources/images/profile_pictures/" + profilePicture);
                    if (Files.exists(pp)) avatar.setFill(new ImagePattern(new Image(pp.toUri().toString())));
                }
            } catch (Exception ignored) {}
        }

        stack.getChildren().addAll(ring, avatar);

        if (showPlus) {
            javafx.scene.layout.StackPane plusBadge = new javafx.scene.layout.StackPane();
            plusBadge.setStyle("-fx-background-color: #667eea; -fx-background-radius: 10;");
            plusBadge.setMinSize(20, 20); plusBadge.setMaxSize(20, 20);
            Label plusLbl = new Label("+");
            plusLbl.setStyle("-fx-text-fill: white; -fx-font-size: 13; -fx-font-weight: bold;");
            plusBadge.getChildren().add(plusLbl);
            javafx.scene.layout.StackPane.setAlignment(plusBadge, Pos.BOTTOM_RIGHT);
            stack.getChildren().add(plusBadge);
        }

        String displayLabel = label.length() > 12 ? label.substring(0, 12) : label;
        Label nameLbl = new Label(displayLabel);
        nameLbl.setStyle("-fx-font-size: 12; -fx-text-fill: #1c1e21;");

        card.getChildren().addAll(stack, nameLbl);
        card.setOnMouseEntered(e -> card.setStyle("-fx-cursor: hand; -fx-opacity: 0.8;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-cursor: hand; -fx-opacity: 1;"));
        return card;
    }

    private void handleCreateStory() {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Créer une Story");

        VBox content = new VBox(15);
        content.setPrefWidth(400);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: white;");

        Label title = new Label("📸 Nouvelle Story");
        title.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #1c1e21;");

        Label sub = new Label("Votre story sera visible pendant 24 heures.");
        sub.setStyle("-fx-font-size: 12; -fx-text-fill: #65676b;");

        TextArea captionArea = new TextArea();
        captionArea.setPromptText("Que voulez-vous partager? 📝");
        captionArea.setPrefHeight(100);
        captionArea.setWrapText(true);
        captionArea.setStyle("-fx-background-radius: 10; -fx-border-radius: 10; -fx-font-size: 13;");

        final String[] imagePath = {null};
        Button pickImg = new Button("🖼️ Ajouter une image (optionnel)");
        pickImg.setStyle("-fx-background-color: #f0f2f5; -fx-text-fill: #667eea; -fx-font-size: 12; -fx-background-radius: 8; -fx-cursor: hand; -fx-padding: 8 14;");
        pickImg.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle("Élire une image");
            fc.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif"));
            File f = fc.showOpenDialog(null);
            if (f != null) {
                imagePath[0] = f.getAbsolutePath();
                pickImg.setText("✅ " + f.getName());
            }
        });

        content.getChildren().addAll(title, sub, captionArea, pickImg);
        dialog.getDialogPane().setContent(content);

        ButtonType publishBtn = new ButtonType("Publier la Story", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(publishBtn, ButtonType.CANCEL);

        dialog.getDialogPane().lookupButton(publishBtn).setStyle(
            "-fx-background-color: linear-gradient(135deg, #667eea 0%, #764ba2 100%); -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 8;"
        );

        dialog.setResultConverter(btn -> btn == publishBtn ? captionArea.getText().trim() : null);

        dialog.showAndWait().ifPresent(caption -> {
            if (caption.isEmpty() && imagePath[0] == null) {
                showAlert("Attention", "Ajoutez un texte ou une image pour créer une story.");
                return;
            }
            String savedImage = null;
            if (imagePath[0] != null) {
                try {
                    File src = new File(imagePath[0]);
                    String ext = imagePath[0].substring(imagePath[0].lastIndexOf('.'));
                    String fname = sessionManager.getUserId() + "_story_" + System.currentTimeMillis() + ext;
                    Path dest = Paths.get("src/main/resources/images/posts/" + fname);
                    Files.copy(src.toPath(), dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    savedImage = fname;
                } catch (Exception ex) { ex.printStackTrace(); }
            }
            Story story = new Story(sessionManager.getUserId(), caption.isEmpty() ? null : caption, savedImage);
            if (storyController.create(story) != null) {
                loadStories();
            } else {
                showAlert("Erreur", "Impossible de publier la story.");
            }
        });
    }

    private void showStoryViewer(Story story, String authorName) {
        Stage stageRef = (Stage) searchField.getScene().getWindow();

        Dialog<Void> dialog = new Dialog<>();
        dialog.initOwner(stageRef);
        dialog.setTitle(authorName + " • Story");

        VBox root = new VBox(0);
        root.setAlignment(Pos.CENTER);
        root.setPrefWidth(460);
        root.setStyle("-fx-background-color: #1a1a2e;");

        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(14, 16, 14, 16));
        header.setStyle("-fx-background-color: rgba(0,0,0,0.5);");

        Circle ava = new Circle(22);
        ava.setStyle("-fx-fill: linear-gradient(135deg, #667eea 0%, #764ba2 100%);");
        if (story.getAuthorProfilePicture() != null && !story.getAuthorProfilePicture().isEmpty()) {
            try {
                String pic = story.getAuthorProfilePicture();
                if (pic.startsWith("http")) {
                    ava.setFill(new ImagePattern(new Image(pic, true)));
                } else {
                    Path pp = Paths.get("src/main/resources/images/profile_pictures/" + pic);
                    if (Files.exists(pp)) ava.setFill(new ImagePattern(new Image(pp.toUri().toString())));
                }
            } catch (Exception ignored) {}
        }

        VBox info = new VBox(2);
        Label nameLbl = new Label(authorName);
        nameLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-text-fill: white;");

        long minutesLeft = java.time.Duration.between(java.time.LocalDateTime.now(), story.getExpiresAt()).toMinutes();
        String timeLeft = minutesLeft > 60 ? (minutesLeft / 60) + "h restantes" : minutesLeft + " min restantes";
        Label timeLbl = new Label("⏳ " + timeLeft);
        timeLbl.setStyle("-fx-font-size: 11; -fx-text-fill: #adb5bd;");
        info.getChildren().addAll(nameLbl, timeLbl);
        header.getChildren().addAll(ava, info);
        root.getChildren().add(header);

        if (story.getImageUrl() != null && !story.getImageUrl().isEmpty()) {
            try {
                Path imgPath = Paths.get("src/main/resources/images/posts/" + story.getImageUrl());
                if (Files.exists(imgPath)) {
                    ImageView iv = new ImageView(new Image(imgPath.toUri().toString()));
                    iv.setFitWidth(460);
                    iv.setFitHeight(360);
                    iv.setPreserveRatio(true);
                    root.getChildren().add(iv);
                }
            } catch (Exception ignored) {}
        }

        if (story.getCaption() != null && !story.getCaption().isEmpty()) {
            Label captionLbl = new Label(story.getCaption());
            captionLbl.setWrapText(true);
            captionLbl.setStyle("-fx-font-size: 15; -fx-text-fill: white; -fx-padding: 20 20 20 20;");
            root.getChildren().add(captionLbl);
        }

        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.getDialogPane().setStyle("-fx-background-color: #1a1a2e; -fx-padding: 0;");
        dialog.showAndWait();
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
                Label emptyLabel = new Label("Aucun post pour le moment. Créez votre premier post ! 📝");
                emptyLabel.setStyle("-fx-font-size: 16; -fx-text-fill: #65676b; -fx-padding: 40;");
                feedContainer.getChildren().add(emptyLabel);
                return;
            }
            
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

        boolean alreadyLiked = postController.isLikedByUser(currentUserId, post.getPostId());
        post.setLikedByMe(alreadyLiked);
        int[] likesRef = { postController.countLikes(post.getPostId()) };

        Button likeBtn = new Button(alreadyLiked ? "❤️ J'aime (" + likesRef[0] + ")" : "🤍 J'aime (" + likesRef[0] + ")");
        if (alreadyLiked) {
            likeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e74c3c; -fx-font-size: 13; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 15;");
        } else {
            styleActionButton(likeBtn);
        }
        likeBtn.setOnAction(e -> {
            boolean nowLiked = postController.toggleLike(currentUserId, post.getPostId());
            likesRef[0] = postController.countLikes(post.getPostId());
            if (nowLiked) {
                likeBtn.setText("❤️ J'aime (" + likesRef[0] + ")");
                likeBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e74c3c; -fx-font-size: 13; -fx-font-weight: bold; -fx-cursor: hand; -fx-padding: 8 15;");
            } else {
                likeBtn.setText("🤍 J'aime (" + likesRef[0] + ")");
                styleActionButton(likeBtn);
            }
        });

        Button commentBtn = new Button("💬 Commenter (" + post.getCommentsCount() + ")");
        styleActionButton(commentBtn);
        commentBtn.setOnAction(e -> handleViewComments(post));

        // ── SHARE BUTTON with live count tracking ──────────────────────────
        // sharesCount is tracked locally and persisted via postController if supported
        int[] sharesRef = { 0 };
        try {
            // If your PostController exposes getSharesCount, use it here:
            // sharesRef[0] = postController.getSharesCount(post.getPostId());
        } catch (Exception ignored) {}

        Button shareBtn = new Button("↗️ Partager (" + sharesRef[0] + ")");
        styleActionButton(shareBtn);
        shareBtn.setOnAction(e -> {
            boolean shared = showShareDialog(post);
            if (shared) {
                sharesRef[0]++;
                shareBtn.setText("↗️ Partager (" + sharesRef[0] + ")");
                // Persist if backend supports it:
                // postController.incrementShareCount(post.getPostId());
            }
        });
        // ───────────────────────────────────────────────────────────────────

        actions.getChildren().addAll(likeBtn, commentBtn, shareBtn);
        
        card.getChildren().addAll(header, contentLabel);
        
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

    // ── IMPROVED SHARE DIALOG ─────────────────────────────────────────────────
    /**
     * Shows the Facebook share dialog.
     *
     * How it works: Facebook's sharer API only accepts a public URL — it cannot
     * accept raw text from a desktop app. So we copy the edited text to the
     * clipboard automatically, open Facebook's new-post composer, and show a
     * prominent "Press Ctrl+V" instruction so the user just pastes.
     *
     * @return true if the user clicked "Ouvrir Facebook", false if they cancelled.
     */
    private boolean showShareDialog(Post post) {
        final boolean[] didShare = { false };
        final int MAX_CHARS = 500;

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Partager sur Facebook");

        VBox root = new VBox(0);
        root.setPrefWidth(430);

        // ── Blue Facebook header ──────────────────────────────────────────
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(16, 20, 16, 20));
        header.setStyle("-fx-background-color: #1877F2;");
        try {
            java.net.URL logoUrl = getClass().getResource("/images/assets/facebook-logo.png");
            if (logoUrl != null) {
                ImageView logo = new ImageView(new Image(logoUrl.toExternalForm()));
                logo.setFitWidth(28); logo.setFitHeight(28); logo.setPreserveRatio(true);
                header.getChildren().add(logo);
            }
        } catch (Exception ignored) {}
        Label fbTitle = new Label("Partager sur Facebook");
        fbTitle.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: white;");
        header.getChildren().add(fbTitle);
        root.getChildren().add(header);

        // ── Body ─────────────────────────────────────────────────────────
        VBox body = new VBox(14);
        body.setPadding(new Insets(18));
        body.setStyle("-fx-background-color: #f0f2f5;");

        // ── 1. Image thumbnail ────────────────────────────────────────────
        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            try {
                Path imgPath = Paths.get("src/main/resources/images/posts/" + post.getImageUrl());
                if (Files.exists(imgPath)) {
                    ImageView thumb = new ImageView(new Image(imgPath.toUri().toString()));
                    thumb.setFitWidth(390);
                    thumb.setFitHeight(200);
                    thumb.setPreserveRatio(true);
                    VBox thumbBox = new VBox(thumb);
                    thumbBox.setAlignment(Pos.CENTER);
                    thumbBox.setStyle(
                        "-fx-background-color: white; -fx-background-radius: 10; " +
                        "-fx-padding: 8; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.12), 6, 0, 0, 2);"
                    );
                    body.getChildren().add(thumbBox);
                }
            } catch (Exception ignored) {}
        }

        // ── 2. Editable text area ─────────────────────────────────────────
        Label editLabel = new Label("✏️  Personnalisez votre message :");
        editLabel.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #65676b;");

        TextArea editArea = new TextArea(post.getContent());
        editArea.setWrapText(true);
        editArea.setPrefHeight(110);
        editArea.setStyle(
            "-fx-background-color: white; -fx-background-radius: 10; " +
            "-fx-border-radius: 10; -fx-font-size: 13; -fx-padding: 10;"
        );

        // ── 3. Character counter ──────────────────────────────────────────
        Label charCounter = new Label(post.getContent().length() + " / " + MAX_CHARS);
        charCounter.setStyle("-fx-font-size: 11; -fx-text-fill: #65676b;");
        HBox counterRow = new HBox(charCounter);
        counterRow.setAlignment(Pos.CENTER_RIGHT);

        editArea.textProperty().addListener((obs, oldVal, newVal) -> {
            int len = newVal.length();
            charCounter.setText(len + " / " + MAX_CHARS);
            if (len > MAX_CHARS) {
                charCounter.setStyle("-fx-font-size: 11; -fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                editArea.setStyle(
                    "-fx-background-color: white; -fx-background-radius: 10; " +
                    "-fx-border-color: #e74c3c; -fx-border-width: 1.5; " +
                    "-fx-border-radius: 10; -fx-font-size: 13; -fx-padding: 10;"
                );
            } else {
                charCounter.setStyle("-fx-font-size: 11; -fx-text-fill: #65676b;");
                editArea.setStyle(
                    "-fx-background-color: white; -fx-background-radius: 10; " +
                    "-fx-border-radius: 10; -fx-font-size: 13; -fx-padding: 10;"
                );
            }
        });

        // ── 4. "How to share" instruction banner ─────────────────────────
        // Facebook's web API doesn't allow desktop apps to pre-fill post text,
        // so we copy it automatically and guide the user to paste.
        HBox howTo = new HBox(10);
        howTo.setAlignment(Pos.CENTER_LEFT);
        howTo.setPadding(new Insets(12, 14, 12, 14));
        howTo.setStyle(
            "-fx-background-color: #e7f3ff; -fx-background-radius: 10; " +
            "-fx-border-color: #1877F2; -fx-border-width: 0 0 0 4; -fx-border-radius: 10;"
        );
        Label clipIcon = new Label("📋");
        clipIcon.setStyle("-fx-font-size: 18;");
        VBox howToText = new VBox(3);
        Label howToMain = new Label("Votre texte sera copié automatiquement");
        howToMain.setStyle("-fx-font-weight: bold; -fx-font-size: 12; -fx-text-fill: #1877F2;");
        Label howToSub = new Label("Le texte sera pré-rempli si possible. Le presse-papiers est aussi copié — utilisez Ctrl+V si le champ est vide.");
        howToSub.setWrapText(true);
        howToSub.setStyle("-fx-font-size: 11; -fx-text-fill: #4b6584;");
        howToText.getChildren().addAll(howToMain, howToSub);
        howTo.getChildren().addAll(clipIcon, howToText);

        // ── 5. Open Facebook button ───────────────────────────────────────
        Button openFbBtn = new Button("  Ouvrir Facebook et coller");
        openFbBtn.setMaxWidth(Double.MAX_VALUE);
        openFbBtn.setStyle(
            "-fx-background-color: #1877F2; -fx-text-fill: white; " +
            "-fx-font-size: 14; -fx-font-weight: bold; -fx-padding: 12; " +
            "-fx-background-radius: 8; -fx-cursor: hand;"
        );
        openFbBtn.setOnMouseEntered(e -> openFbBtn.setStyle(
            "-fx-background-color: #145db2; -fx-text-fill: white; " +
            "-fx-font-size: 14; -fx-font-weight: bold; -fx-padding: 12; " +
            "-fx-background-radius: 8; -fx-cursor: hand;"
        ));
        openFbBtn.setOnMouseExited(e -> openFbBtn.setStyle(
            "-fx-background-color: #1877F2; -fx-text-fill: white; " +
            "-fx-font-size: 14; -fx-font-weight: bold; -fx-padding: 12; " +
            "-fx-background-radius: 8; -fx-cursor: hand;"
        ));
        try {
            java.net.URL logoUrl = getClass().getResource("/images/assets/facebook-logo.png");
            if (logoUrl != null) {
                ImageView btnLogo = new ImageView(new Image(logoUrl.toExternalForm()));
                btnLogo.setFitWidth(18); btnLogo.setFitHeight(18);
                openFbBtn.setGraphic(btnLogo);
            }
        } catch (Exception ignored) {}

        openFbBtn.setOnAction(e -> {
            if (editArea.getText().length() > MAX_CHARS) {
                showAlert("Attention",
                    "Le texte dépasse " + MAX_CHARS + " caractères.\nVeuillez le raccourcir avant de partager.");
                return;
            }
            // 1. Copy the (possibly edited) text to clipboard
            javafx.scene.input.Clipboard cb = javafx.scene.input.Clipboard.getSystemClipboard();
            javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
            cc.putString(editArea.getText().trim());
            cb.setContent(cc);

            // 2. Open Facebook's sharer with the text pre-filled (best effort).
            //    The clipboard copy above acts as guaranteed fallback if Facebook
            //    doesn't pick up the quote parameter.
            try {
                String encoded = java.net.URLEncoder.encode(editArea.getText().trim(), "UTF-8");
                java.awt.Desktop.getDesktop().browse(
                    new java.net.URI("https://www.facebook.com/sharer/sharer.php?quote=" + encoded)
                );
                didShare[0] = true;
                dialog.close();
                PauseTransition delay = new PauseTransition(Duration.millis(400));
                delay.setOnFinished(ev -> showToast("Facebook ouvert ! Si le texte est vide, appuyez sur Ctrl+V 📋"));
                delay.play();
            } catch (Exception ex) {
                ex.printStackTrace();
                showAlert("Erreur", "Impossible d'ouvrir le navigateur : " + ex.getMessage());
            }
        });

        body.getChildren().addAll(editLabel, editArea, counterRow, howTo, openFbBtn);
        root.getChildren().add(body);

        dialog.getDialogPane().setContent(root);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();

        return didShare[0];
    }

    /**
     * Shows a non-blocking toast notification at the bottom of the main window.
     * Auto-dismisses after 3 seconds with a fade-out animation.
     */
    private void showToast(String message) {
        Stage mainStage = (Stage) searchField.getScene().getWindow();

        Stage toastStage = new Stage();
        toastStage.initOwner(mainStage);
        toastStage.setResizable(false);
        toastStage.initStyle(javafx.stage.StageStyle.TRANSPARENT);
        toastStage.initModality(Modality.NONE); // non-blocking

        Label toastLabel = new Label(message);
        toastLabel.setStyle(
            "-fx-background-color: rgba(28,30,33,0.90); -fx-text-fill: white; " +
            "-fx-padding: 14 28; -fx-background-radius: 30; " +
            "-fx-font-size: 13; -fx-font-weight: bold;"
        );

        StackPane toastRoot = new StackPane(toastLabel);
        toastRoot.setStyle("-fx-background-color: transparent;");

        Scene toastScene = new Scene(toastRoot);
        toastScene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        toastStage.setScene(toastScene);

        // Position: bottom-centre of the main window
        toastStage.setOnShown(ev -> {
            toastStage.setX(mainStage.getX() + (mainStage.getWidth()  - toastStage.getWidth())  / 2);
            toastStage.setY(mainStage.getY() +  mainStage.getHeight() - toastStage.getHeight() - 60);
        });

        toastStage.show();

        // Fade out then close
        FadeTransition fadeOut = new FadeTransition(Duration.millis(600), toastRoot);
        fadeOut.setFromValue(1.0);
        fadeOut.setToValue(0.0);
        fadeOut.setDelay(Duration.seconds(2.4));
        fadeOut.setOnFinished(ev -> toastStage.close());
        fadeOut.play();
    }
    // ── END SHARE IMPROVEMENTS ────────────────────────────────────────────────
    
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
            
            Label postAuthor = new Label("Post de: " + post.getAuthorName());
            postAuthor.setStyle("-fx-font-weight: bold; -fx-font-size: 14;");
            
            Label postContent = new Label(post.getContent());
            postContent.setWrapText(true);
            postContent.setStyle("-fx-font-size: 13; -fx-padding: 10; -fx-background-color: white; " +
                                "-fx-background-radius: 10;");
            
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
                        dialog.close();
                        handleViewComments(post);
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
            Stage stage = (Stage) searchField.getScene().getWindow();
            double width = stage.getWidth();
            double height = stage.getHeight();
            boolean wasMaximized = stage.isMaximized();
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/FriendsView.fxml"));
            Scene scene = new Scene(loader.load(), width, height);
            scene.getStylesheets().add(getClass().getResource("/css/social-style.css").toExternalForm());
            
            stage.setScene(scene);
            stage.setTitle("Ghrami - Mes Amis");
            if (wasMaximized) {
                stage.setMaximized(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir la page des amis: " + e.getMessage());
        }
    }
    
    private void refreshNavBadges() {
        try {
            long userId = sessionManager.getUserId();
            int unreadNotifs = notificationController.countUnread(userId);
            int unreadMsgs = messageController.countUnread(userId);
            if (notificationsBadge != null) {
                notificationsBadge.setText(unreadNotifs > 9 ? "9+" : String.valueOf(unreadNotifs));
                notificationsBadge.setVisible(unreadNotifs > 0);
            }
            if (messagesBadge != null) {
                messagesBadge.setText(unreadMsgs > 9 ? "9+" : String.valueOf(unreadMsgs));
                messagesBadge.setVisible(unreadMsgs > 0);
            }
        } catch (Exception ignored) {}
    }

    @FXML
    private void handleNotifications() {
        long userId = sessionManager.getUserId();
        java.util.List<Notification> notifications = notificationController.getForUser(userId);

        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Notifications");

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(0);
        content.setPrefWidth(440);

        javafx.scene.layout.HBox header = new javafx.scene.layout.HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.setStyle("-fx-padding: 16 20 12 20; -fx-background-color: white; -fx-border-color: #e4e6eb; -fx-border-width: 0 0 1 0;");
        Label titleLbl = new Label("🔔  Notifications");
        titleLbl.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #1c1e21;");
        Region spacer = new Region();
        javafx.scene.layout.HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);
        Button markAllBtn = new Button("Tout marquer comme lu");
        markAllBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #667eea; -fx-font-size: 12; -fx-cursor: hand; -fx-border-width: 0;");
        markAllBtn.setOnAction(e -> {
            notificationController.markAllRead(userId);
            dialog.close();
            refreshNavBadges();
        });
        header.getChildren().addAll(titleLbl, spacer, markAllBtn);
        content.getChildren().add(header);

        javafx.scene.control.ScrollPane sp = new javafx.scene.control.ScrollPane();
        sp.setFitToWidth(true);
        sp.setPrefHeight(380);
        sp.setStyle("-fx-background-color: #f0f2f5; -fx-background: #f0f2f5;");
        javafx.scene.layout.VBox list = new javafx.scene.layout.VBox(4);
        list.setStyle("-fx-padding: 12;");

        if (notifications.isEmpty()) {
            Label empty = new Label("Aucune notification pour le moment");
            empty.setStyle("-fx-text-fill: #65676b; -fx-font-size: 13; -fx-padding: 30;");
            list.getChildren().add(empty);
        } else {
            for (Notification n : notifications) {
                javafx.scene.layout.HBox row = new javafx.scene.layout.HBox(12);
                row.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                row.setPadding(new javafx.geometry.Insets(12, 16, 12, 16));
                row.setStyle("-fx-background-color: " + (n.isRead() ? "white" : "rgba(102,126,234,0.08)")
                        + "; -fx-background-radius: 12; -fx-cursor: hand;");

                Label icon = new Label(n.getIcon());
                icon.setStyle("-fx-font-size: 22;");

                javafx.scene.layout.VBox txt = new javafx.scene.layout.VBox(3);
                javafx.scene.layout.HBox.setHgrow(txt, javafx.scene.layout.Priority.ALWAYS);
                Label msg = new Label(n.getContent());
                msg.setWrapText(true);
                msg.setStyle("-fx-font-size: 13; -fx-text-fill: " + (n.isRead() ? "#65676b" : "#1c1e21") + "; -fx-font-weight: " + (n.isRead() ? "normal" : "bold") + ";");
                String timeStr = n.getCreatedAt().toLocalDate().equals(java.time.LocalDate.now())
                        ? n.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
                        : n.getCreatedAt().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM HH:mm"));
                Label time = new Label(timeStr);
                time.setStyle("-fx-font-size: 11; -fx-text-fill: #adb5bd;");
                txt.getChildren().addAll(msg, time);

                if (!n.isRead()) {
                    Label dot = new Label("●");
                    dot.setStyle("-fx-text-fill: #667eea; -fx-font-size: 10;");
                    row.getChildren().addAll(icon, txt, dot);
                } else {
                    row.getChildren().addAll(icon, txt);
                }

                row.setOnMouseClicked(e -> {
                    notificationController.markRead(n.getNotificationId());
                    refreshNavBadges();
                });
                list.getChildren().add(row);
            }
        }
        sp.setContent(list);
        content.getChildren().add(sp);

        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
        refreshNavBadges();
    }

    @FXML
    private void handleMessages() {
        try {
            Stage stage = (Stage) searchField.getScene().getWindow();
            double width = stage.getWidth();
            double height = stage.getHeight();
            boolean wasMaximized = stage.isMaximized();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/MessagesView.fxml"));
            Scene scene = new Scene(loader.load(), width, height);
            scene.getStylesheets().add(getClass().getResource("/css/social-style.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Ghrami - Messages");
            if (wasMaximized) stage.setMaximized(true);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir les messages: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleProfile() {
        try {
            Stage stage = (Stage) searchField.getScene().getWindow();
            double width = stage.getWidth();
            double height = stage.getHeight();
            boolean wasMaximized = stage.isMaximized();
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/ProfileView.fxml"));
            Scene scene = new Scene(loader.load(), width, height);
            scene.getStylesheets().add(getClass().getResource("/css/social-style.css").toExternalForm());
            
            stage.setScene(scene);
            stage.setTitle("Ghrami - Mon Profil");
            if (wasMaximized) {
                stage.setMaximized(true);
            }
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
            long userId = sessionManager.getUserId();
            User user = userController.findById((int) userId);
            if (user != null) {
                user.setOnline(false);
                userController.update(user);
                System.out.println("User set to offline: " + user.getUsername());
            }
            
            sessionManager.logout();
            
            Stage stage = (Stage) searchField.getScene().getWindow();
            double width = stage.getWidth();
            double height = stage.getHeight();
            boolean wasMaximized = stage.isMaximized();
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/LoginView.fxml"));
            Scene scene = new Scene(loader.load(), width, height);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            
            stage.setScene(scene);
            stage.setTitle("Ghrami - Login");
            if (wasMaximized) {
                stage.setMaximized(true);
            }
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
            Stage stage = (Stage) searchField.getScene().getWindow();
            double width = stage.getWidth();
            double height = stage.getHeight();
            boolean wasMaximized = stage.isMaximized();
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/HobbiesView.fxml"));
            Scene scene = new Scene(loader.load(), width, height);
            scene.getStylesheets().add(getClass().getResource("/css/social-style.css").toExternalForm());
            
            stage.setScene(scene);
            stage.setTitle("Ghrami - My Hobbies");
            if (wasMaximized) {
                stage.setMaximized(true);
            }
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
            Stage stage = (Stage) searchField.getScene().getWindow();
            double width = stage.getWidth();
            double height = stage.getHeight();
            boolean wasMaximized = stage.isMaximized();
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/FriendsView.fxml"));
            Scene scene = new Scene(loader.load(), width, height);
            scene.getStylesheets().add(getClass().getResource("/css/social-style.css").toExternalForm());
            
            stage.setScene(scene);
            stage.setTitle("Ghrami - Mes Amis");
            if (wasMaximized) {
                stage.setMaximized(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir la page des amis: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleBadges() {
        try {
            Stage stage = (Stage) searchField.getScene().getWindow();
            double width = stage.getWidth();
            double height = stage.getHeight();
            boolean wasMaximized = stage.isMaximized();

            // Read FXML bytes and strip UTF-8 BOM (EF BB BF) if present,
            // which would otherwise cause "Content is not allowed in prolog" from the XML parser.
            byte[] fxmlBytes;
            try (InputStream raw = getClass().getResourceAsStream("/opgg/ghrami/view/BadgesView.fxml")) {
                fxmlBytes = raw.readAllBytes();
            }
            int start = 0;
            if (fxmlBytes.length >= 3
                    && fxmlBytes[0] == (byte) 0xEF
                    && fxmlBytes[1] == (byte) 0xBB
                    && fxmlBytes[2] == (byte) 0xBF) {
                start = 3;
            }

            FXMLLoader loader = new FXMLLoader();
            loader.setLocation(getClass().getResource("/opgg/ghrami/view/BadgesView.fxml"));
            Scene scene = new Scene(loader.load(new ByteArrayInputStream(fxmlBytes, start, fxmlBytes.length - start)), width, height);
            scene.getStylesheets().add(getClass().getResource("/css/social-style.css").toExternalForm());

            stage.setScene(scene);
            stage.setTitle("Ghrami - Mes Badges");
            if (wasMaximized) {
                stage.setMaximized(true);
            }
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
            Stage stage = (Stage) searchField.getScene().getWindow();
            double width = stage.getWidth();
            double height = stage.getHeight();
            boolean wasMaximized = stage.isMaximized();
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/ClassMarketplace.fxml"));
            Scene scene = new Scene(loader.load(), width, height);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            
            stage.setScene(scene);
            stage.setTitle("Ghrami - Class Marketplace");
            if (wasMaximized) {
                stage.setMaximized(true);
            }
        } catch (Exception e) {
            System.err.println("Error loading Class Marketplace: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Unable to open Class Marketplace");
        }
    }
    
    @FXML
    private void handleMeetups() {
        try {
            Stage stage = (Stage) searchField.getScene().getWindow();
            double width = stage.getWidth();
            double height = stage.getHeight();
            boolean wasMaximized = stage.isMaximized();
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/MeetingsView.fxml"));
            Scene scene = new Scene(loader.load(), width, height);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            
            stage.setScene(scene);
            stage.setTitle("Ghrami - Meetups & Connections");
            if (wasMaximized) {
                stage.setMaximized(true);
            }
        } catch (Exception e) {
            System.err.println("Error loading Meetups: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Unable to open Meetups");
        }
    }
    
    @FXML
    private void handleVRRooms() {
        try {
            Stage stage = (Stage) searchField.getScene().getWindow();
            double w = stage.getWidth();
            double h = stage.getHeight();
            boolean maximized = stage.isMaximized();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/VRRoomsView.fxml"));
            Scene scene = new Scene(loader.load(), w, h);
            scene.getStylesheets().add(getClass().getResource("/css/social-style.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Ghrami — 🎮 Salles VR");
            if (maximized) stage.setMaximized(true);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir les Salles VR : " + e.getMessage());
        }
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
            
            File selectedFile = fileChooser.showOpenDialog(postTextArea.getScene().getWindow());
            
            if (selectedFile != null) {
                Path postsDir = Paths.get("src/main/resources/images/posts/");
                if (!Files.exists(postsDir)) {
                    Files.createDirectories(postsDir);
                }
                
                String fileName = sessionManager.getUserId() + "_" + System.currentTimeMillis() + "_" + selectedFile.getName();
                Path destinationPath = postsDir.resolve(fileName);
                
                Files.copy(selectedFile.toPath(), destinationPath);
                
                selectedImagePath = fileName;

                // ── Auto-caption the uploaded image with AI ──────────────────────
                showAiStatus("🤖 Analyse de l'image en cours...");
                setAiButtonsDisabled(true);
                final File finalFile = selectedFile;
                Thread captionThread = new Thread(() -> {
                    try {
                        byte[] imageBytes = java.nio.file.Files.readAllBytes(finalFile.toPath());
                        String caption = HuggingFaceService.getInstance().captionImage(imageBytes);
                        javafx.application.Platform.runLater(() -> {
                            if (postTextArea.getText().isBlank()) {
                                postTextArea.setText(caption);
                            } else {
                                postTextArea.appendText("\n" + caption);
                            }
                            showAiStatus("✅ Légende générée par l'IA !");
                            setAiButtonsDisabled(false);
                            // Auto-hide status after 4 seconds
                            PauseTransition hide = new PauseTransition(Duration.seconds(4));
                            hide.setOnFinished(ev -> hideAiStatus());
                            hide.play();
                        });
                    } catch (Exception ex) {
                        javafx.application.Platform.runLater(() -> {
                            showAiStatus("⚠️ Impossible de générer la légende: " + ex.getMessage());
                            setAiButtonsDisabled(false);
                        });
                        ex.printStackTrace();
                    }
                });
                captionThread.setDaemon(true);
                captionThread.start();
                // ────────────────────────────────────────────────────────────────
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

    // ─────────────────────────────────────────────────────────────────────────
    //  AI FEATURE HANDLERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Feature 1 – AI text completion.
     * Takes the current text and asks Hugging Face to continue it.
     */
    @FXML
    private void handleAiComplete() {
        String currentText = postTextArea.getText();
        if (currentText == null || currentText.isBlank()) {
            showAlert("Info", "Écrivez quelques mots d'abord, puis l'IA complétera votre post.");
            return;
        }
        showAiStatus("🤖 L'IA complète votre texte...");
        setAiButtonsDisabled(true);

        Thread t = new Thread(() -> {
            try {
                String completion = HuggingFaceService.getInstance().completeText(currentText);
                javafx.application.Platform.runLater(() -> {
                    // Append the completion to what the user already wrote
                    String separator = currentText.endsWith(" ") ? "" : " ";
                    postTextArea.setText(currentText + separator + completion);
                    postTextArea.positionCaret(postTextArea.getText().length());
                    showAiStatus("✅ Texte complété !");
                    setAiButtonsDisabled(false);
                    PauseTransition hide = new PauseTransition(Duration.seconds(4));
                    hide.setOnFinished(ev -> hideAiStatus());
                    hide.play();
                });
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> {
                    showAiStatus("⚠️ Erreur: " + ex.getMessage());
                    setAiButtonsDisabled(false);
                });
                ex.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    /**
     * Feature 2 – Image generation.
     * Generates an image based on the current post text.
     */
    @FXML
    private void handleAiGenerateImage() {
        String prompt = postTextArea.getText();
        if (prompt == null || prompt.isBlank()) {
            showAlert("Info", "Décrivez d'abord votre post, puis l'IA créera une image correspondante.");
            return;
        }
        showAiStatus("🎨 Génération de l'image en cours (peut prendre ~30s)...");
        setAiButtonsDisabled(true);

        Thread t = new Thread(() -> {
            try {
                byte[] imageBytes = HuggingFaceService.getInstance().generateImage(prompt);
                javafx.application.Platform.runLater(() -> {
                    try {
                        aiGeneratedImageBytes = imageBytes;
                        javafx.scene.image.Image img = new javafx.scene.image.Image(
                                new ByteArrayInputStream(imageBytes));
                        aiImagePreview.setImage(img);
                        aiImageContainer.setVisible(true);
                        aiImageContainer.setManaged(true);
                        showAiStatus("✅ Image générée !");
                        setAiButtonsDisabled(false);
                        PauseTransition hide = new PauseTransition(Duration.seconds(5));
                        hide.setOnFinished(ev -> hideAiStatus());
                        hide.play();
                    } catch (Exception inner) {
                        showAiStatus("⚠️ Impossible d'afficher l'image.");
                        setAiButtonsDisabled(false);
                    }
                });
            } catch (Exception ex) {
                javafx.application.Platform.runLater(() -> {
                    showAiStatus("⚠️ Erreur: " + ex.getMessage());
                    setAiButtonsDisabled(false);
                });
                ex.printStackTrace();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    /**
     * Removes the AI-generated image preview.
     */
    @FXML
    private void handleRemoveAiImage() {
        aiGeneratedImageBytes = null;
        aiImagePreview.setImage(null);
        aiImageContainer.setVisible(false);
        aiImageContainer.setManaged(false);
    }

    // ── AI UI helpers ─────────────────────────────────────────────────────────

    private void showAiStatus(String message) {
        if (aiStatusLabel != null) {
            aiStatusLabel.setText(message);
            aiStatusLabel.setVisible(true);
            aiStatusLabel.setManaged(true);
        }
    }

    private void hideAiStatus() {
        if (aiStatusLabel != null) {
            aiStatusLabel.setVisible(false);
            aiStatusLabel.setManaged(false);
        }
    }

    private void setAiButtonsDisabled(boolean disabled) {
        if (btnAiComplete != null) btnAiComplete.setDisable(disabled);
        if (btnAiImage != null)    btnAiImage.setDisable(disabled);
    }
    
    @FXML
    private void handlePublish() {
        try {
            String postText = postTextArea.getText();
            if (postText == null || postText.trim().isEmpty()) {
                showAlert("Erreur", "Le contenu du post ne peut pas être vide");
                return;
            }
            if (postText.length() > 5000) {
                showAlert("Erreur", "Le contenu du post est trop long (max 5000 caractères)");
                return;
            }

            long userId = sessionManager.getUserId();

            // If an AI-generated image exists, save it as a file first
            if (aiGeneratedImageBytes != null && (selectedImagePath == null || selectedImagePath.isEmpty())) {
                try {
                    Path postsDir = Paths.get("src/main/resources/images/posts/");
                    if (!Files.exists(postsDir)) Files.createDirectories(postsDir);
                    String fname = userId + "_ai_" + System.currentTimeMillis() + ".png";
                    Path dest = postsDir.resolve(fname);
                    Files.write(dest, aiGeneratedImageBytes);
                    selectedImagePath = fname;
                } catch (Exception saveEx) {
                    saveEx.printStackTrace();
                }
            }

            Post newPost;
            if (selectedImagePath != null && !selectedImagePath.isEmpty()) {
                newPost = new Post(userId, postText.trim(), selectedImagePath);
            } else {
                newPost = new Post(userId, postText.trim());
            }

            Post created = postController.create(newPost);

            if (created != null) {
                postTextArea.clear();
                selectedImagePath = null;
                aiGeneratedImageBytes = null;
                handleRemoveAiImage();
                hideAiStatus();
                loadFeed();
                loadUserInfo();
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
package opgg.ghrami.view;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import opgg.ghrami.controller.*;
import opgg.ghrami.model.*;
import opgg.ghrami.util.ConfigManager;
import opgg.ghrami.util.SessionManager;

import com.sun.net.httpserver.HttpServer;
import java.awt.Desktop;
import java.io.File;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MeetingsViewController {
    
    // FXML Fields - Statistics
    @FXML private Label totalConnectionsLabel;
    @FXML private Label upcomingMeetingsLabel;
    @FXML private Label pendingRequestsLabel;
    @FXML private Label potentialMatchesLabel;
    @FXML private Label connectionsCountLabel;
    @FXML private Label pendingCountLabel;
    
    // FXML Fields - Discovery Tab
    @FXML private GridPane discoveryGrid;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> skillFilterCombo;
    @FXML private ComboBox<String> locationFilterCombo;
    
    // FXML Fields - Other Tabs
    @FXML private VBox connectionsContainer;
    @FXML private VBox meetingsContainer;
    @FXML private VBox pendingRequestsContainer;
    @FXML private ComboBox<String> connectionFilterCombo;
    @FXML private ComboBox<String> meetingFilterCombo;
    @FXML private Button backButton;
    
    // Controllers
    private final ConnectionController connectionController = ConnectionController.getInstance();
    private final MeetingController meetingController = MeetingController.getInstance();
    private final MeetingParticipantController participantController = MeetingParticipantController.getInstance();
    private final UserController userController = new UserController();
    private final HobbyController hobbyController = new HobbyController();
    private final BadgeController badgeController = new BadgeController();
    private final ProgressController progressController = new ProgressController();
    
    private Long currentUserId;
    private List<User> allUsers;
    private List<MatchScore> potentialMatches;
    /** Cached Google Calendar access token – survives for the lifetime of this controller instance. */
    private String calendarAccessToken = null;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    // ── Google OAuth2 config – fill in your CLIENT_ID from Google Cloud Console ──
    // Steps: console.cloud.google.com → New Project → Enable "Google Calendar API"
    //        → OAuth 2.0 Credentials → Desktop App → copy Client ID below
    private static final String GOOGLE_CLIENT_ID     = ConfigManager.getInstance().getGoogleClientId();
    private static final String GOOGLE_CLIENT_SECRET = ConfigManager.getInstance().getGoogleClientSecret();
    private static final String GOOGLE_REDIRECT_URI  = "http://localhost:8765/callback";
    private static final String GOOGLE_SCOPE         = "https://www.googleapis.com/auth/calendar.events";
    
    // Data class for match scoring
    private static class MatchScore {
        User user;
        int score;
        String matchReason;
        List<String> commonInterests;
        
        MatchScore(User user, int score, String matchReason, List<String> commonInterests) {
            this.user = user;
            this.score = score;
            this.matchReason = matchReason;
            this.commonInterests = commonInterests;
        }
    }

    
    @FXML
    public void initialize() {
        currentUserId = SessionManager.getInstance().getUserId();
        if (currentUserId == null) {
            showAlert("Erreur", "Veuillez vous connecter d'abord");
            return;
        }
        
        loadAllUsers();
        setupFilters();
        setupSearchListener();
        loadDashboardData();
        loadDiscoveryFeed();
    }
    
    private void loadAllUsers() {
        allUsers = userController.findAll();
        // Remove current user and already connected users
        List<Connection> myConnections = connectionController.findByUser(currentUserId);
        Set<Long> connectedUserIds = myConnections.stream()
            .flatMap(c -> List.of(c.getInitiatorId(), c.getReceiverId()).stream())
            .collect(Collectors.toSet());
        connectedUserIds.add(currentUserId);
        
        allUsers = allUsers.stream()
            .filter(user -> !connectedUserIds.contains(user.getUserId()))
            .filter(user -> user.getUserId() != 0) // Exclude admin (user_id = 0)
            .collect(Collectors.toList());
    }
    
    private void setupSearchListener() {
        if (searchField != null) {
            searchField.textProperty().addListener((obs, oldVal, newVal) -> {
                loadDiscoveryFeed();
            });
        }
    }
    
    private void setupFilters() {
        // Skill filter - populate from all users
        if (skillFilterCombo != null) {
            Set<String> allSkills = new HashSet<>();
            allUsers.forEach(user -> {
                List<Hobby> hobbies = hobbyController.findByUserId(user.getUserId());
                hobbies.forEach(hobby -> allSkills.add(hobby.getName()));
            });
            skillFilterCombo.getItems().add("Toutes les Compétences");
            skillFilterCombo.getItems().addAll(allSkills.stream().sorted().collect(Collectors.toList()));
            skillFilterCombo.setValue("Toutes les Compétences");
            skillFilterCombo.setOnAction(e -> loadDiscoveryFeed());
        }
        
        // Location filter
        if (locationFilterCombo != null) {
            Set<String> locations = allUsers.stream()
                .map(User::getLocation)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            locationFilterCombo.getItems().add("Toutes les Localisations");
            locationFilterCombo.getItems().addAll(locations.stream().sorted().collect(Collectors.toList()));
            locationFilterCombo.setValue("Toutes les Localisations");
            locationFilterCombo.setOnAction(e -> loadDiscoveryFeed());
        }
        
        // Connection filters
        if (connectionFilterCombo != null) {
            connectionFilterCombo.getItems().addAll(
                "Toutes les Connexions",
                "Acceptées",
                "En attente",
                "Échange de Compétences",
                "Activité"
            );
            connectionFilterCombo.setValue("Toutes les Connexions");
        }
        
        // Meeting filters
        if (meetingFilterCombo != null) {
            meetingFilterCombo.getItems().addAll(
                "Tous les Rendez-vous",
                "À Venir",
                "Passés",
                "Programmés",
                "Terminés",
                "Annulés",
                "Physiques",
                "Virtuels"
            );
            meetingFilterCombo.setValue("À Venir");
        }
    }
    
    private void loadDashboardData() {
        loadStats();
        loadConnections();
        loadMeetings();
        loadPendingRequests();
    }
    
    private void loadStats() {
        // Count connections
        List<Connection> allConnections = connectionController.findByUser(currentUserId);
        long acceptedConnections = allConnections.stream()
            .filter(c -> "accepted".equalsIgnoreCase(c.getStatus()))
            .count();
       totalConnectionsLabel.setText(String.valueOf(acceptedConnections));
        
        // Count upcoming meetings
        List<Meeting> upcomingMeetings = meetingController.findUpcomingMeetings(currentUserId);
        upcomingMeetingsLabel.setText(String.valueOf(upcomingMeetings.size()));
        
        // Count pending requests
        List<Connection> pendingRequests = connectionController.findPendingForUser(currentUserId);
        pendingRequestsLabel.setText(String.valueOf(pendingRequests.size()));
        
        // Count potential matches
        if (potentialMatchesLabel != null) {
            potentialMatchesLabel.setText(String.valueOf(allUsers.size()));
        }
    }
    
    // ========================================
    // SMART MATCHING ALGORITHM (WOW FACTOR!)
    // ========================================
    
    @FXML
    private void handleSmartMatch() {
        calculateMatchScores();
        loadDiscoveryFeed();
    }
    
    @FXML
    private void handleRefreshDiscovery() {
        loadAllUsers();
        calculateMatchScores();
        loadDiscoveryFeed();
    }
    
    private void calculateMatchScores() {
        potentialMatches = new ArrayList<>();
        List<Hobby> myHobbies = hobbyController.findByUserId(currentUserId);
        User currentUser = userController.findById(currentUserId).orElse(null);
        if (currentUser == null) return;
        
        for (User user : allUsers) {
            int score = 0;
            List<String> commonInterests = new ArrayList<>();
            StringBuilder matchReason = new StringBuilder();
            
            // 1. Shared hobbies/interests (40 points max)
            List<Hobby> theirHobbies = hobbyController.findByUserId(user.getUserId());
            for (Hobby myHobby : myHobbies) {
                for (Hobby theirHobby : theirHobbies) {
                    if (myHobby.getName().equalsIgnoreCase(theirHobby.getName())) {
                        score += 10;
                        commonInterests.add(myHobby.getName());
                    } else if (myHobby.getCategory() != null && myHobby.getCategory().equals(theirHobby.getCategory())) {
                        score += 5;
                        commonInterests.add(myHobby.getCategory() + " enthusiast");
                    }
                }
            }
            
            // 2. Complementary skills - I want to learn what they know (30 points max)
            for (Hobby myHobby : myHobbies) {
                Progress myProgress = progressController.findByHobbyId(myHobby.getHobbyId()).orElse(null);
                if (myProgress != null && myProgress.getHoursSpent() < 50) { // I'm a beginner
                    for (Hobby theirHobby : theirHobbies) {
                        if (myHobby.getName().equalsIgnoreCase(theirHobby.getName())) {
                            Progress theirProgress = progressController.findByHobbyId(theirHobby.getHobbyId()).orElse(null);
                            if (theirProgress != null && theirProgress.getHoursSpent() > 100) { // They're experienced
                                score += 15;
                                matchReason.append("Can teach you ").append(theirHobby.getName()).append(". ");
                            }
                        }
                    }
                }
            }
            
            // 3. Location proximity (20 points)
            if (currentUser.getLocation() != null && currentUser.getLocation().equals(user.getLocation())) {
                score += 20;
                matchReason.append("Same location. ");
            }
            
            // 4. User activity level (10 points)
            int theirHobbyCount = theirHobbies.size();
            int myHobbyCount = myHobbies.size();
            if (Math.abs(theirHobbyCount - myHobbyCount) <= 2) {
                score += 10;
                matchReason.append("Similar activity level. ");
            }
            
            // 5. Badges/achievements (bonus points)
            List<Badge> theirBadges = badgeController.findByUserId(user.getUserId());
            if (theirBadges.size() >= 3) {
                score += 5;
            }
            
            if (matchReason.length() == 0) {
                matchReason.append("New connection opportunity");
            }
            
            potentialMatches.add(new MatchScore(user, score, matchReason.toString().trim(), commonInterests));
        }
        
        // Sort by score descending
        potentialMatches.sort((a, b) -> Integer.compare(b.score, a.score));
    }
    
    private void loadDiscoveryFeed() {
        if (discoveryGrid == null) return;
        discoveryGrid.getChildren().clear();
        
        // Calculate match scores if not done yet
        if (potentialMatches == null) {
            calculateMatchScores();
        }
        
        // Apply search filter
        String searchText = searchField != null ? searchField.getText().toLowerCase() : "";
        String skillFilter = skillFilterCombo != null ? skillFilterCombo.getValue() : "Toutes les Compétences";
        String locationFilter = locationFilterCombo != null ? locationFilterCombo.getValue() : "Toutes les Localisations";
        
        List<MatchScore> filteredMatches = potentialMatches.stream()
            .filter(match -> {
                if (!searchText.isEmpty()) {
                    return match.user.getUsername().toLowerCase().contains(searchText) ||
                           (match.user.getFullName() != null && match.user.getFullName().toLowerCase().contains(searchText)) ||
                           (match.user.getBio() != null && match.user.getBio().toLowerCase().contains(searchText));
                }
                return true;
            })
            .filter(match -> {
                if (!"Toutes les Compétences".equals(skillFilter)) {
                    List<Hobby> hobbies = hobbyController.findByUserId(match.user.getUserId());
                    return hobbies.stream().anyMatch(h -> h.getName().equals(skillFilter));
                }
                return true;
            })
            .filter(match -> {
                if (!"Toutes les Localisations".equals(locationFilter)) {
                    return locationFilter.equals(match.user.getLocation());
                }
                return true;
            })
            .collect(Collectors.toList());
        
        // Display in grid (3 columns)
        int col = 0, row = 0;
        for (MatchScore match : filteredMatches) {
            VBox card = createUserDiscoveryCard(match);
            discoveryGrid.add(card, col, row);
            col++;
            if (col >= 3) {
                col = 0;
                row++;
            }
        }
        
        // Show empty state
        if (filteredMatches.isEmpty()) {
            Label emptyLabel = new Label("🔍 No matches found. Try adjusting your filters!");
            emptyLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #7f8c8d; -fx-padding: 40;");
            discoveryGrid.add(emptyLabel, 0, 0, 3, 1);
        }
    }
    
    // ========================================
    // USER DISCOVERY CARD (WOW FACTOR!)
    // ========================================
    
    private VBox createUserDiscoveryCard(MatchScore match) {
        User user = match.user;
        VBox card = new VBox(15);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 20; " +
                      "-fx-effect: dropshadow(gaussian, rgba(102,126,234,0.15), 15, 0, 0, 3); " +
                      "-fx-padding: 0; -fx-pref-width: 380; -fx-pref-height: 480; -fx-cursor: hand;");
        
        // Profile image background with gradient
        StackPane imageContainer = new StackPane();
        imageContainer.setStyle("-fx-background-color: linear-gradient(135deg, #667eea 0%, #764ba2 100%); " +
                               "-fx-background-radius: 20 20 0 0; -fx-pref-height: 200;");
        
        // Profile Image
        ImageView profileImage = createProfileImageView(user.getProfilePicture(), 100);
        profileImage.setFitHeight(100);
        profileImage.setFitWidth(100);
        Circle clip = new Circle(50);
        clip.setCenterX(50);
        clip.setCenterY(50);
        profileImage.setClip(clip);
        profileImage.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 3);");
        
        // Match Score Badge
        String scoreColor = match.score >= 60 ? "#4CAF50" : match.score >= 40 ? "#667eea" : "#FF9800";
        Label scoreBadge = new Label(match.score + "%");
        scoreBadge.setStyle("-fx-background-color: " + scoreColor + "; -fx-text-fill: white; " +
                           "-fx-font-size: 16px; -fx-font-weight: bold; -fx-padding: 8 15; " +
                           "-fx-background-radius: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.2), 5, 0, 0, 2);");
        StackPane.setAlignment(scoreBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(scoreBadge, new Insets(15, 15, 0, 0));
        
        imageContainer.getChildren().addAll(profileImage, scoreBadge);
        
        // Content
        VBox content = new VBox(10);
        content.setPadding(new Insets(15, 20, 20, 20));
        
        // Name and Location
        Label nameLabel = new Label(user.getFullName() != null ? user.getFullName() : user.getUsername());
        nameLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        if (user.getLocation() != null) {
            Label locationLabel = new Label("📍 " + user.getLocation());
            locationLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #7f8c8d;");
            content.getChildren().addAll(nameLabel, locationLabel);
        } else {
            content.getChildren().add(nameLabel);
        }
        
        // Bio (truncated)
        if (user.getBio() != null && !user.getBio().isEmpty()) {
            String bio = user.getBio().length() > 80 ? user.getBio().substring(0, 80) + "..." : user.getBio();
            Label bioLabel = new Label(bio);
            bioLabel.setWrapText(true);
            bioLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d; -fx-padding: 5 0;");
            content.getChildren().add(bioLabel);
        }
        
        // Match Reason
        Label matchReasonLabel = new Label("💫 " + match.matchReason);
        matchReasonLabel.setWrapText(true);
        matchReasonLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #667eea; -fx-font-weight: 600; " +
                                  "-fx-background-color: #f8f9ff; -fx-padding: 8; -fx-background-radius: 8;");
        content.getChildren().add(matchReasonLabel);
        
        // Common Interests Tags
        if (!match.commonInterests.isEmpty()) {
            FlowPane tagsPane = new FlowPane();
            tagsPane.setHgap(5);
            tagsPane.setVgap(5);
            for (String interest : match.commonInterests.stream().limit(3).collect(Collectors.toList())) {
                Label tag = new Label(interest);
                tag.setStyle("-fx-background-color: #e8eaf6; -fx-text-fill: #667eea; " +
                           "-fx-font-size: 11px; -fx-padding: 4 10; -fx-background-radius: 12; -fx-font-weight: 600;");
                tagsPane.getChildren().add(tag);
            }
            content.getChildren().add(tagsPane);
        }
        
        // Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        content.getChildren().add(spacer);
        
        // Action Buttons
        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER);
        
        Button viewProfileBtn = new Button("👤 Voir");
        viewProfileBtn.setStyle("-fx-background-color: #f8f9ff; -fx-text-fill: #667eea; " +
                               "-fx-font-size: 13px; -fx-padding: 10 20; -fx-background-radius: 10; " +
                               "-fx-cursor: hand; -fx-font-weight: 600; -fx-border-color: #667eea; -fx-border-width: 1; -fx-border-radius: 10;");
        viewProfileBtn.setOnAction(e -> showUserProfile(match));
        
        Button connectBtn = new Button("💫 Connecter");
        connectBtn.setStyle("-fx-background-color: linear-gradient(to right, #667eea, #764ba2); -fx-text-fill: white; " +
                           "-fx-font-size: 13px; -fx-padding: 10 25; -fx-background-radius: 10; " +
                           "-fx-cursor: hand; -fx-font-weight: bold;");
        connectBtn.setOnAction(e -> handleQuickConnect(user));
        
        buttons.getChildren().addAll(viewProfileBtn, connectBtn);
        content.getChildren().add(buttons);
        
        card.getChildren().addAll(imageContainer, content);
        
        // Hover effect
        card.setOnMouseEntered(e -> 
            card.setStyle(card.getStyle() + "-fx-scale-y: 1.03; -fx-scale-x: 1.03;")
        );
        card.setOnMouseExited(e -> 
            card.setStyle(card.getStyle().replace("-fx-scale-y: 1.03; -fx-scale-x: 1.03;", ""))
        );
        
        return card;
    }
    
    private ImageView createProfileImageView(String profilePicture, double size) {
        ImageView imageView = new ImageView();
        imageView.setFitHeight(size);
        imageView.setFitWidth(size);
        imageView.setPreserveRatio(true);
        
        if (profilePicture != null && !profilePicture.isEmpty()) {
            try {
                File imageFile = new File("src/main/resources/images/profile_pictures/" + profilePicture);
                if (imageFile.exists()) {
                    imageView.setImage(new Image(imageFile.toURI().toString()));
                } else {
                    setDefaultImage(imageView);
                }
            } catch (Exception e) {
                setDefaultImage(imageView);
            }
        } else {
            setDefaultImage(imageView);
        }
        
        return imageView;
    }
    
    private void setDefaultImage(ImageView imageView) {
        try {
            File defaultImage = new File("src/main/resources/images/profile_pictures/default-avatar.png");
            if (defaultImage.exists()) {
                imageView.setImage(new Image(defaultImage.toURI().toString()));
            }
        } catch (Exception ignored) {
        }
    }
    
    @FXML
    private void handleQuickConnect(User user) {
        Dialog<Connection> dialog = new Dialog<>();
        dialog.setTitle("Connecter avec " + user.getFullName());
        dialog.setHeaderText("Choisissez le type de connexion et spécifiez les compétences");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("skill", "activity", "hobby", "general");
        typeCombo.setValue("skill");
        
        TextField initiatorSkillField = new TextField();
        initiatorSkillField.setPromptText("Votre compétence à partager (optionnel)");
        
        TextField receiverSkillField = new TextField();
        receiverSkillField.setPromptText("Compétence que vous voulez apprendre (optionnel)");
        
        grid.add(new Label("Type de Connexion :"), 0, 0);
        grid.add(typeCombo, 1, 0);
        grid.add(new Label("Votre Compétence :"), 0, 1);
        grid.add(initiatorSkillField, 1, 1);
        grid.add(new Label("Leur Compétence :"), 0, 2);
        grid.add(receiverSkillField, 1, 2);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        // VALIDATION: Disable OK button initially if needed
        Button okButton = (Button) dialog.getDialogPane().lookupButton(ButtonType.OK);
        
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                // Validation
                if (typeCombo.getValue() == null || typeCombo.getValue().trim().isEmpty()) {
                    showAlert("Erreur de Validation", "Veuillez sélectionner un type de connexion.");
                    return null;
                }
                
                String initiatorSkill = initiatorSkillField.getText().trim();
                String receiverSkill = receiverSkillField.getText().trim();
                
                // For skill type, at least one skill should be specified
                if ("skill".equals(typeCombo.getValue()) && initiatorSkill.isEmpty() && receiverSkill.isEmpty()) {
                    showAlert("Erreur de Validation", "Pour un échange de compétences, veuillez spécifier au moins une compétence.");
                    return null;
                }
                
                Connection connection = new Connection(
                    currentUserId,
                    user.getUserId(),
                    typeCombo.getValue(),
                    receiverSkill.isEmpty() ? null : receiverSkill,
                    initiatorSkill.isEmpty() ? null : initiatorSkill
                );
                return connection;
            }
            return null;
        });
        
        Optional<Connection> result = dialog.showAndWait();
        result.ifPresent(connection -> {
            Connection created = connectionController.create(connection);
            if (created != null) {
                showAlert("Succès", "Demande de connexion envoyée à " + user.getFullName() + "!");
                loadAllUsers();
                loadDiscoveryFeed();
                loadStats();
            } else {
                showAlert("Erreur", "Échec de l'envoi de la demande de connexion.");
            }
        });
    }
    
    private void showUserProfile(MatchScore match) {
        User user = match.user;
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Profile: " + user.getFullName());
        
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: #f5f6fa;");
        
        // Profile Header
        HBox header = new HBox(20);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-background-color: white; -fx-padding: 20; -fx-background-radius: 15;");
        
        ImageView profileImage = createProfileImageView(user.getProfilePicture(), 80);
        Circle clip = new Circle(40);
        clip.setCenterX(40);
        clip.setCenterY(40);
        profileImage.setClip(clip);
        
        VBox userInfo = new VBox(8);
        Label nameLabel = new Label(user.getFullName() != null ? user.getFullName() : user.getUsername());
        nameLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        Label usernameLabel = new Label("@" + user.getUsername());
        usernameLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #7f8c8d;");
        if (user.getLocation() != null) {
            Label locationLabel = new Label("📍 " + user.getLocation());
            locationLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #667eea;");
            userInfo.getChildren().addAll(nameLabel, usernameLabel, locationLabel);
        } else {
            userInfo.getChildren().addAll(nameLabel, usernameLabel);
        }
        
        header.getChildren().addAll(profileImage, userInfo);
        content.getChildren().add(header);
        
        // Match Score
        HBox matchScore = new HBox(10);
        matchScore.setAlignment(Pos.CENTER_LEFT);
        matchScore.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 15;");
        String scoreColor = match.score >= 60 ? "#4CAF50" : match.score >= 40 ? "#667eea" : "#FF9800";
        Label scoreLabel = new Label("Score de Match : " + match.score + "%");
        scoreLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + scoreColor + ";");
        Label reasonLabel = new Label(match.matchReason);
        reasonLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d;");
        reasonLabel.setWrapText(true);
        VBox scoreBox = new VBox(5, scoreLabel, reasonLabel);
        matchScore.getChildren().add(scoreBox);
        content.getChildren().add(matchScore);
        
        // Bio
        if (user.getBio() != null && !user.getBio().isEmpty()) {
            VBox bioSection = new VBox(10);
            bioSection.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 15;");
            Label bioTitle = new Label("À Propos");
            bioTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
            Label bioText = new Label(user.getBio());
            bioText.setWrapText(true);
            bioText.setStyle("-fx-font-size: 13px; -fx-text-fill: #7f8c8d;");
            bioSection.getChildren().addAll(bioTitle, bioText);
            content.getChildren().add(bioSection);
        }
        
        // Hobbies & Skills
        List<Hobby> hobbies = hobbyController.findByUserId(user.getUserId());
        if (!hobbies.isEmpty()) {
            VBox hobbiesSection = new VBox(10);
            hobbiesSection.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 15;");
            Label hobbiesTitle = new Label("Intérêts & Compétences");
            hobbiesTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
            FlowPane hobbiesFlow = new FlowPane();
            hobbiesFlow.setHgap(8);
            hobbiesFlow.setVgap(8);
            for (Hobby hobby : hobbies) {
                Label hobbyTag = new Label(hobby.getName());
                hobbyTag.setStyle("-fx-background-color: #e8eaf6; -fx-text-fill: #667eea; " +
                               "-fx-font-size: 12px; -fx-padding: 6 12; -fx-background-radius: 15; -fx-font-weight: 600;");
                hobbiesFlow.getChildren().add(hobbyTag);
            }
            hobbiesSection.getChildren().addAll(hobbiesTitle, hobbiesFlow);
            content.getChildren().add(hobbiesSection);
        }
        
        // Badges
        List<Badge> badges = badgeController.findByUserId(user.getUserId());
        if (!badges.isEmpty()) {
            VBox badgesSection = new VBox(10);
            badgesSection.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 15;");
            Label badgesTitle = new Label("Réalisations");
            badgesTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
            VBox badgesList = new VBox(8);
            for (Badge badge : badges) {
                HBox badgeItem = new HBox(10);
                badgeItem.setAlignment(Pos.CENTER_LEFT);
                Label badgeIcon = new Label("🏆");
                badgeIcon.setStyle("-fx-font-size: 18px;");
                VBox badgeInfo = new VBox(2);
                Label badgeName = new Label(badge.getName());
                badgeName.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #2c3e50;");
                Label badgeDesc = new Label(badge.getDescription());
                badgeDesc.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");
                badgeInfo.getChildren().addAll(badgeName, badgeDesc);
                badgeItem.getChildren().addAll(badgeIcon, badgeInfo);
                badgesList.getChildren().add(badgeItem);
            }
            badgesSection.getChildren().addAll(badgesTitle, badgesList);
            content.getChildren().add(badgesSection);
        }
        
        // Common Interests
        if (!match.commonInterests.isEmpty()) {
            VBox commonSection = new VBox(10);
            commonSection.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 15;");
            Label commonTitle = new Label("Points Communs");
            commonTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
            FlowPane commonFlow = new FlowPane();
            commonFlow.setHgap(8);
            commonFlow.setVgap(8);
            for (String interest : match.commonInterests) {
                Label tag = new Label("✨ " + interest);
                tag.setStyle("-fx-background-color: #fef3e8; -fx-text-fill: #FF9800; " +
                           "-fx-font-size: 12px; -fx-padding: 6 12; -fx-background-radius: 15; -fx-font-weight: 600;");
                commonFlow.getChildren().add(tag);
            }
            commonSection.getChildren().addAll(commonTitle, commonFlow);
            content.getChildren().add(commonSection);
        }
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #f5f6fa; -fx-border-color: transparent;");
        scrollPane.setPrefViewportHeight(500);
        scrollPane.setPrefViewportWidth(600);
        
        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }
    
    // ========================================
    // CONNECTIONS & MEETINGS (UPDATED)
    // ========================================
    
    @FXML
    private void loadConnections() {
        if (connectionsContainer == null) return;
        connectionsContainer.getChildren().clear();
        List<Connection> connections = connectionController.findByUser(currentUserId);
        
        if (connections.isEmpty()) {
            Label emptyLabel = new Label("No connections yet. Start connecting with others!");
            emptyLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px;");
            connectionsContainer.getChildren().add(emptyLabel);
            return;
        }
        
        for (Connection connection : connections) {
            connectionsContainer.getChildren().add(createConnectionCard(connection));
        }
    }
    
    @FXML
    private void filterConnections() {
        connectionsContainer.getChildren().clear();
        String filter = connectionFilterCombo.getValue();
        List<Connection> connections;
        
        switch (filter) {
            case "Accepted":
                connections = connectionController.findByUser(currentUserId).stream()
                    .filter(c -> "accepted".equalsIgnoreCase(c.getStatus()))
                    .toList();
                break;
            case "Pending Sent":
                connections = connectionController.findByInitiator(currentUserId).stream()
                    .filter(c -> "pending".equalsIgnoreCase(c.getStatus()))
                    .toList();
                break;
            case "Skill Exchange":
                connections = connectionController.findByUser(currentUserId).stream()
                    .filter(c -> "skill".equalsIgnoreCase(c.getConnectionType()))
                    .toList();
                break;
            case "Activity":
                connections = connectionController.findByUser(currentUserId).stream()
                    .filter(c -> "activity".equalsIgnoreCase(c.getConnectionType()))
                    .toList();
                break;
            default:
                connections = connectionController.findByUser(currentUserId);
        }
        
        for (Connection connection : connections) {
            connectionsContainer.getChildren().add(createConnectionCard(connection));
        }
    }
    
    private VBox createConnectionCard(Connection connection) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 8; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 2);");
        
        // Determine other user
        boolean isInitiator = connection.getInitiatorId().equals(currentUserId);
        Long otherUserId = isInitiator ? connection.getReceiverId() : connection.getInitiatorId();
        Optional<User> otherUserOpt = userController.findById(otherUserId);
        String otherUserName = otherUserOpt.map(User::getUsername).orElse("Unknown User");
        
        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label nameLabel = new Label(otherUserName);
        nameLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        Label statusBadge = new Label(connection.getStatus().toUpperCase());
        statusBadge.setStyle(getConnectionStatusStyle(connection.getStatus()));
        
        Label typeBadge = new Label(connection.getConnectionType().toUpperCase());
        typeBadge.setStyle("-fx-background-color: #ecf0f1; -fx-text-fill: #2c3e50; -fx-padding: 3 10; " +
                          "-fx-background-radius: 12; -fx-font-size: 11px;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        header.getChildren().addAll(nameLabel, statusBadge, typeBadge, spacer);
        
        // Skills info
        VBox skillsBox = new VBox(5);
        if (connection.getInitiatorSkill() != null || connection.getReceiverSkill() != null) {
            Label initiatorSkillLabel = new Label(" 🎯 " + (isInitiator ? "Votre" : "Leur") + " Compétence : " + 
                                                  (isInitiator ? connection.getInitiatorSkill() : connection.getReceiverSkill()));
            initiatorSkillLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #34495e;");
            
            Label receiverSkillLabel = new Label("💼 " + (isInitiator ? "Leur" : "Votre") + " Compétence : " + 
                                                 (isInitiator ? connection.getReceiverSkill() : connection.getInitiatorSkill()));
            receiverSkillLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #34495e;");
            
            skillsBox.getChildren().addAll(initiatorSkillLabel, receiverSkillLabel);
        }
        
        // Actions
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);
        
        if ("accepted".equalsIgnoreCase(connection.getStatus())) {
            Button scheduleMeetingBtn = new Button("📅 Planifier Rendez-vous");
            scheduleMeetingBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; " +
                                       "-fx-padding: 8 15; -fx-background-radius: 5; -fx-cursor: hand;");
            scheduleMeetingBtn.setOnAction(e -> handleScheduleMeetingForConnection(connection));
            actions.getChildren().add(scheduleMeetingBtn);
        }
        
        Button deleteBtn = new Button("Supprimer");
        deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                          "-fx-padding: 8 15; -fx-background-radius: 5; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> handleDeleteConnection(connection));
        
        actions.getChildren().add(deleteBtn);
        
        card.getChildren().addAll(header, skillsBox, actions);
        return card;
    }
    
    private void loadPendingRequests() {
        pendingRequestsContainer.getChildren().clear();
        List<Connection> pendingRequests = connectionController.findPendingForUser(currentUserId);
        
        if (pendingRequests.isEmpty()) {
            Label emptyLabel = new Label("Aucune demande en attente");
            emptyLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px;");
            pendingRequestsContainer.getChildren().add(emptyLabel);
            return;
        }
        
        for (Connection connection : pendingRequests) {
            pendingRequestsContainer.getChildren().add(createPendingRequestCard(connection));
        }
    }
    
    private VBox createPendingRequestCard(Connection connection) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: #fff3cd; -fx-padding: 15; -fx-background-radius: 8; " +
                     "-fx-effect: dropshadow(gaussian, rgba(255,193,7,0.3), 8, 0, 0, 2);");
        
        Optional<User> initiatorOpt = userController.findById(connection.getInitiatorId());
        String initiatorName = initiatorOpt.map(User::getUsername).orElse("Unknown User");
        
        Label header = new Label("🤝 Demande de Connexion de " + initiatorName);
        header.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        Label typeLabel = new Label("Type : " + connection.getConnectionType());
        typeLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #34495e;");
        
        if (connection.getInitiatorSkill() != null) {
            Label skillLabel = new Label("Leur Compétence : " + connection.getInitiatorSkill());
            skillLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #34495e;");
            card.getChildren().add(skillLabel);
        }
        
        if (connection.getReceiverSkill() != null) {
            Label wantsLabel = new Label("Veut apprendre : " + connection.getReceiverSkill());
            wantsLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #34495e;");
            card.getChildren().add(wantsLabel);
        }
        
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);
        
        Button acceptBtn = new Button("✓ Accepter");
        acceptBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; " +
                          "-fx-padding: 8 20; -fx-background-radius: 5; -fx-cursor: hand;");
        acceptBtn.setOnAction(e -> {
            if (connectionController.acceptConnection(connection.getConnectionId())) {
                showAlert("Succès", "Demande de connexion acceptée!");
                loadDashboardData();
            }
        });
        
        Button rejectBtn = new Button("✗ Rejeter");
        rejectBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                          "-fx-padding: 8 20; -fx-background-radius: 5; -fx-cursor: hand;");
        rejectBtn.setOnAction(e -> {
            if (connectionController.rejectConnection(connection.getConnectionId())) {
                showAlert("Succès", "Demande de connexion rejetée");
                loadDashboardData();
            }
        });
        
        actions.getChildren().addAll(acceptBtn, rejectBtn);
        
        card.getChildren().addAll(header, typeLabel, actions);
        return card;
    }
    
    @FXML
    private void loadMeetings() {
        meetingsContainer.getChildren().clear();
        List<Meeting> meetings = meetingController.findUpcomingMeetings(currentUserId);
        
        if (meetings.isEmpty()) {
            Label emptyLabel = new Label("No upcoming meetings. Schedule one with your connections!");
            emptyLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px;");
            meetingsContainer.getChildren().add(emptyLabel);
            return;
        }
        
        for (Meeting meeting : meetings) {
            meetingsContainer.getChildren().add(createMeetingCard(meeting));
        }
    }
    
    @FXML
    private void filterMeetings() {
        meetingsContainer.getChildren().clear();
        String filter = meetingFilterCombo.getValue();
        List<Meeting> meetings;
        
        switch (filter) {
            case "Upcoming":
                meetings = meetingController.findUpcomingMeetings(currentUserId);
                break;
            case "Past":
                meetings = meetingController.findPastMeetings(currentUserId);
                break;
            case "Scheduled":
                meetings = meetingController.findByStatus("scheduled").stream()
                    .filter(m -> participantController.isUserParticipant(m.getMeetingId(), currentUserId))
                    .toList();
                break;
            case "Completed":
                meetings = meetingController.findByStatus("completed").stream()
                    .filter(m -> participantController.isUserParticipant(m.getMeetingId(), currentUserId))
                    .toList();
                break;
            case "Cancelled":
                meetings = meetingController.findByStatus("cancelled").stream()
                    .filter(m -> participantController.isUserParticipant(m.getMeetingId(), currentUserId))
                    .toList();
                break;
            case "Physical":
                meetings = meetingController.findByType("physical").stream()
                    .filter(m -> participantController.isUserParticipant(m.getMeetingId(), currentUserId))
                    .toList();
                break;
            case "Virtual":
                meetings = meetingController.findByType("virtual").stream()
                    .filter(m -> participantController.isUserParticipant(m.getMeetingId(), currentUserId))
                    .toList();
                break;
            default:
                meetings = meetingController.findUpcomingMeetings(currentUserId);
        }
        
        for (Meeting meeting : meetings) {
            meetingsContainer.getChildren().add(createMeetingCard(meeting));
        }
    }
    
    private VBox createMeetingCard(Meeting meeting) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-padding: 15; -fx-background-radius: 8; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 8, 0, 0, 2);");
        
        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label dateLabel = new Label("📅 " + (meeting.getScheduledAt() != null ? 
            meeting.getScheduledAt().format(DATETIME_FORMATTER) : "TBD"));
        dateLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        Label statusBadge = new Label(meeting.getStatus().toUpperCase());
        statusBadge.setStyle(getMeetingStatusStyle(meeting.getStatus()));
        
        Label typeBadge = new Label(getMeetingTypeIcon(meeting.getMeetingType()) + " " + meeting.getMeetingType().toUpperCase());
        typeBadge.setStyle("-fx-background-color: #ecf0f1; -fx-text-fill: #2c3e50; -fx-padding: 3 10; " +
                          "-fx-background-radius: 12; -fx-font-size: 11px;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        header.getChildren().addAll(dateLabel, statusBadge, typeBadge, spacer);
        
        // Details
        VBox details = new VBox(6);

        // For virtual meetings show the Meet link prominently with a join button
        if ("virtual".equalsIgnoreCase(meeting.getMeetingType()) && meeting.getLocation() != null
                && meeting.getLocation().startsWith("http")) {
            HBox meetRow = new HBox(10);
            meetRow.setAlignment(Pos.CENTER_LEFT);
            Label meetIcon = new Label("🎥");
            meetIcon.setStyle("-fx-font-size: 14px;");
            Label meetLink = new Label(meeting.getLocation());
            meetLink.setStyle("-fx-font-size: 12px; -fx-text-fill: #1a73e8; -fx-underline: true; -fx-cursor: hand;");
            meetLink.setMaxWidth(260);
            meetLink.setEllipsisString("…");
            meetLink.setWrapText(false);
            Button joinBtn = new Button("🚀 Rejoindre");
            joinBtn.setStyle("-fx-background-color: linear-gradient(to right,#667eea,#764ba2); -fx-text-fill: white; " +
                            "-fx-font-size: 12px; -fx-font-weight: bold; -fx-padding: 6 16; " +
                            "-fx-background-radius: 20; -fx-cursor: hand;");
            joinBtn.setOnAction(e -> {
                try { java.awt.Desktop.getDesktop().browse(new java.net.URI(meeting.getLocation())); }
                catch (Exception ex) { showAlert("Erreur", "Impossible d'ouvrir le lien : " + ex.getMessage()); }
            });
            // Copy link button
            Button copyBtn = new Button("📋");
            copyBtn.setStyle("-fx-background-color: #f0f2ff; -fx-text-fill: #667eea; " +
                            "-fx-padding: 6 10; -fx-background-radius: 20; -fx-cursor: hand;");
            copyBtn.setTooltip(new Tooltip("Copier le lien"));
            copyBtn.setOnAction(e -> {
                javafx.scene.input.ClipboardContent content = new javafx.scene.input.ClipboardContent();
                content.putString(meeting.getLocation());
                javafx.scene.input.Clipboard.getSystemClipboard().setContent(content);
                copyBtn.setText("✅");
                new Thread(() -> {
                    try { Thread.sleep(1500); } catch (Exception ignored) {}
                    javafx.application.Platform.runLater(() -> copyBtn.setText("📋"));
                }).start();
            });
            meetRow.getChildren().addAll(meetIcon, meetLink, joinBtn, copyBtn);
            details.getChildren().add(meetRow);
        } else {
            Label locationLabel = new Label("📍 " + meeting.getLocation());
            locationLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #34495e;");
            details.getChildren().add(locationLabel);
        }
        
        Label durationLabel = new Label("⏱ Durée : " + meeting.getDuration() + " minutes");
        durationLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #34495e;");
        
        int participantCount = participantController.countParticipants(meeting.getMeetingId());
        Label participantsLabel = new Label("👥 Participants : " + participantCount);
        participantsLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #34495e;");
        
        details.getChildren().addAll(durationLabel, participantsLabel);
        
        // Actions
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_LEFT);
        
        if ("scheduled".equalsIgnoreCase(meeting.getStatus())) {
            Button completeBtn = new Button("✓ Marquer Terminé");
            completeBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; " +
                               "-fx-padding: 8 15; -fx-background-radius: 5; -fx-cursor: hand;");
            completeBtn.setOnAction(e -> {
                if (meetingController.completeMeeting(meeting.getMeetingId())) {
                    showAlert("Succès", "Rendez-vous marqué comme terminé");
                    loadDashboardData();
                }
            });
            
            Button editBtn = new Button("✏️ Modifier");
            editBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; " +
                            "-fx-padding: 8 15; -fx-background-radius: 5; -fx-cursor: hand;");
            editBtn.setOnAction(e -> handleEditMeeting(meeting));
            
            Button cancelBtn = new Button("✗ Annuler");
            cancelBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; " +
                              "-fx-padding: 8 15; -fx-background-radius: 5; -fx-cursor: hand;");
            cancelBtn.setOnAction(e -> {
                if (meetingController.cancelMeeting(meeting.getMeetingId())) {
                    showAlert("Succès", "Rendez-vous annulé");
                    loadDashboardData();
                }
            });
            
            actions.getChildren().addAll(completeBtn, editBtn, cancelBtn);
        }
        
        Button viewParticipantsBtn = new Button("👥 Participants");
        viewParticipantsBtn.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; " +
                                    "-fx-padding: 8 15; -fx-background-radius: 5; -fx-cursor: hand;");
        viewParticipantsBtn.setOnAction(e -> viewMeetingParticipants(meeting));
        
        Button deleteBtn = new Button("🗑️ Supprimer");
        deleteBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; " +
                          "-fx-padding: 8 15; -fx-background-radius: 5; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> handleDeleteMeeting(meeting));
        
        actions.getChildren().addAll(viewParticipantsBtn, deleteBtn);
        
        card.getChildren().addAll(header, details, actions);
        return card;
    }
    
    @FXML
    private void handleCreateConnection() {
        Dialog<Connection> dialog = new Dialog<>();
        dialog.setTitle("Créer Nouvelle Connexion");
        dialog.setHeaderText("Envoyer une demande de connexion à un autre utilisateur");
        
        // Dialog content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        TextField receiverIdField = new TextField();
        receiverIdField.setPromptText("ID utilisateur pour se connecter");
        
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("skill", "activity", "hobby", "general");
        typeCombo.setValue("general");
        
        TextField initiatorSkillField = new TextField();
        initiatorSkillField.setPromptText("Votre compétence/intérêt (optionnel)");
        
        TextField receiverSkillField = new TextField();
        receiverSkillField.setPromptText("Ce que vous voulez apprendre (optionnel)");
        
        grid.add(new Label("ID Utilisateur :"), 0, 0);
        grid.add(receiverIdField, 1, 0);
        grid.add(new Label("Type :"), 0, 1);
        grid.add(typeCombo, 1, 1);
        grid.add(new Label("Votre Compétence :"), 0, 2);
        grid.add(initiatorSkillField, 1, 2);
        grid.add(new Label("Veux Apprendre :"), 0, 3);
        grid.add(receiverSkillField, 1, 3);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                // Validation 1: User ID is required and must be numeric
                if (receiverIdField.getText() == null || receiverIdField.getText().trim().isEmpty()) {
                    showAlert("Erreur de Validation", "L'ID utilisateur est obligatoire.");
                    return null;
                }
                
                try {
                    Long receiverId = Long.parseLong(receiverIdField.getText().trim());
                    
                    // Validation 2: Cannot connect with yourself
                    if (receiverId.equals(currentUserId)) {
                        showAlert("Erreur de Validation", "Vous ne pouvez pas vous connecter avec vous-même.");
                        return null;
                    }
                    
                    // Validation 3: Cannot connect with admin
                    if (receiverId == 0) {
                        showAlert("Erreur de Validation", "Impossible de se connecter avec l'administrateur.");
                        return null;
                    }
                    
                    // Validation 4: User must exist
                    Optional<User> targetUser = userController.findById(receiverId);
                    if (targetUser.isEmpty()) {
                        showAlert("Erreur de Validation", "Utilisateur introuvable avec l'ID: " + receiverId);
                        return null;
                    }
                    
                    // Validation 4: Check for existing connection
                    List<Connection> existing = connectionController.findByUser(currentUserId);
                    boolean alreadyConnected = existing.stream()
                        .anyMatch(c -> c.getInitiatorId().equals(receiverId) || c.getReceiverId().equals(receiverId));
                    if (alreadyConnected) {
                        showAlert("Erreur de Validation", "Une connexion existe déjà avec cet utilisateur.");
                        return null;
                    }
                    
                    // Validation 5: Connection type is required
                    if (typeCombo.getValue() == null || typeCombo.getValue().trim().isEmpty()) {
                        showAlert("Erreur de Validation", "Le type de connexion est obligatoire.");
                        return null;
                    }
                    
                    String initiatorSkill = initiatorSkillField.getText().trim();
                    String receiverSkill = receiverSkillField.getText().trim();
                    
                    // Validation 6: For skill type, at least one skill should be specified
                    if ("skill".equals(typeCombo.getValue()) && initiatorSkill.isEmpty() && receiverSkill.isEmpty()) {
                        showAlert("Erreur de Validation", "Pour un échange de compétences, spécifiez au moins une compétence.");
                        return null;
                    }
                    
                    Connection connection = new Connection(
                        currentUserId,
                        receiverId,
                        typeCombo.getValue(),
                        receiverSkill.isEmpty() ? null : receiverSkill,
                        initiatorSkill.isEmpty() ? null : initiatorSkill
                    );
                    return connection;
                } catch (NumberFormatException e) {
                    showAlert("Erreur de Validation", "L'ID utilisateur doit être un nombre valide.");
                    return null;
                }
            }
            return null;
        });
        
        Optional<Connection> result = dialog.showAndWait();
        result.ifPresent(connection -> {
            Connection created = connectionController.create(connection);
            if (created != null) {
                showAlert("Succès", "Demande de connexion envoyée!");
                loadDashboardData();
            } else {
                showAlert("Erreur", "Échec de l'envoi de la demande de connexion");
            }
        });
    }
    
    @FXML
    private void handleCreateMeeting() {
        Meeting created = showMeetingCreationDialog(null);
        if (created != null) {
            // Only the organizer is added here – no second party known at this point
            participantController.create(new MeetingParticipant(created.getMeetingId(), currentUserId));
            loadDashboardData();
        }
    }
    
    private void handleEditMeeting(Meeting meeting) {
        // ── State ──────────────────────────────────────────────────
        ObjectProperty<LocalDate> selectedDate = new SimpleObjectProperty<>(
                meeting.getScheduledAt() != null ? meeting.getScheduledAt().toLocalDate() : null);
        Set<LocalDate> bookedDates = getBookedDates();
        // exclude this meeting's own date from booked
        if (meeting.getScheduledAt() != null) bookedDates.remove(meeting.getScheduledAt().toLocalDate());

        // ── Root ───────────────────────────────────────────────────
        VBox root = new VBox(14);
        root.setPadding(new Insets(22));
        root.setStyle("-fx-background-color: #f1f5f9;");
        root.setPrefWidth(490);

        // ── (1) Meeting type toggle ────────────────────────────────
        Label typeLabel = new Label("Type de Rendez-vous");
        typeLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13; -fx-text-fill: #212529;");
        ToggleGroup typeGroup = new ToggleGroup();
        ToggleButton physicalBtn = new ToggleButton("📍 Présentiel");
        ToggleButton virtualBtn  = new ToggleButton("💻 Virtuel");
        physicalBtn.setToggleGroup(typeGroup);
        virtualBtn.setToggleGroup(typeGroup);
        boolean startsVirtual = "virtual".equalsIgnoreCase(meeting.getMeetingType());
        physicalBtn.setSelected(!startsVirtual);
        virtualBtn.setSelected(startsVirtual);
        String btnBase = "-fx-padding: 8 20; -fx-background-radius: 20; -fx-cursor: hand; -fx-font-size: 13;";
        physicalBtn.setStyle(btnBase + (!startsVirtual
                ? "-fx-background-color: #667eea; -fx-text-fill: white;"
                : "-fx-background-color: #e9ecef; -fx-text-fill: #495057;"));
        virtualBtn.setStyle(btnBase + (startsVirtual
                ? "-fx-background-color: #667eea; -fx-text-fill: white;"
                : "-fx-background-color: #e9ecef; -fx-text-fill: #495057;"));
        HBox typeRow = new HBox(10, physicalBtn, virtualBtn);

        // ── (2) Location ───────────────────────────────────────────
        Label locationLabel = new Label(startsVirtual ? "Lien de réunion" : "Lieu");
        locationLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13; -fx-text-fill: #212529;");

        TextField locationField = new TextField(meeting.getLocation());
        locationField.setStyle("-fx-padding: 8; -fx-background-radius: 8;");

        Button mapBtn = new Button("🗺 Choisir sur la carte");
        mapBtn.setStyle("-fx-background-color: #28a745; -fx-text-fill: white; -fx-background-radius: 20; " +
                        "-fx-padding: 7 16; -fx-cursor: hand;");
        mapBtn.setOnAction(e -> {
            String picked = showMapPickerDialog();
            if (picked != null && !picked.isEmpty()) locationField.setText(picked);
        });

        // Virtual info box (shows current link for virtual meetings)
        Label virtualInfoLabel = new Label(
            "🎥  Le lien Google Meet existant sera conservé.\n    Modifiez-le manuellement si nécessaire.");
        virtualInfoLabel.setWrapText(true);
        virtualInfoLabel.setStyle(
            "-fx-text-fill: #1a73e8; -fx-font-size: 12; -fx-padding: 8 12; " +
            "-fx-background-color: #e8f0fe; -fx-background-radius: 8;");

        VBox physicalBox = new VBox(6, locationField, mapBtn);
        VBox virtualBox  = new VBox(8, virtualInfoLabel, locationField);
        physicalBox.setVisible(!startsVirtual); physicalBox.setManaged(!startsVirtual);
        virtualBox.setVisible(startsVirtual);   virtualBox.setManaged(startsVirtual);

        physicalBtn.setOnAction(e -> {
            physicalBtn.setStyle(btnBase + "-fx-background-color: #667eea; -fx-text-fill: white;");
            virtualBtn.setStyle(btnBase  + "-fx-background-color: #e9ecef; -fx-text-fill: #495057;");
            physicalBox.setVisible(true); physicalBox.setManaged(true);
            virtualBox.setVisible(false); virtualBox.setManaged(false);
            locationLabel.setText("Lieu");
            locationLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13; -fx-text-fill: #212529;");
        });
        virtualBtn.setOnAction(e -> {
            virtualBtn.setStyle(btnBase  + "-fx-background-color: #667eea; -fx-text-fill: white;");
            physicalBtn.setStyle(btnBase + "-fx-background-color: #e9ecef; -fx-text-fill: #495057;");
            physicalBox.setVisible(false); physicalBox.setManaged(false);
            virtualBox.setVisible(true);   virtualBox.setManaged(true);
            locationLabel.setText("Lien de réunion");
            locationLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13; -fx-text-fill: #212529;");
        });

        // ── (3) Calendar ───────────────────────────────────────────
        Label calLabel = new Label("Choisir une date");
        calLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 13; -fx-text-fill: #212529;");
        VBox calBox = new VBox();
        buildCalendarPanel(selectedDate, bookedDates, calBox, 
                selectedDate.get() != null ? YearMonth.from(selectedDate.get()) : YearMonth.now());

        // ── (4) Time & Duration ────────────────────────────────────
        Label timeLabel = new Label("Heure (HH:MM)");
        timeLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #212529;");
        TextField timeField = new TextField(meeting.getScheduledAt() != null
                ? String.format("%02d:%02d", meeting.getScheduledAt().getHour(), meeting.getScheduledAt().getMinute())
                : "14:00");
        timeField.setStyle("-fx-padding: 8; -fx-background-radius: 8;");

        Label durLabel = new Label("Durée (minutes)");
        durLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #212529;");
        TextField durationField = new TextField(String.valueOf(meeting.getDuration()));
        durationField.setStyle("-fx-padding: 8; -fx-background-radius: 8;");

        GridPane bottomGrid = new GridPane();
        bottomGrid.setHgap(16); bottomGrid.setVgap(6);
        bottomGrid.add(timeLabel, 0, 0); bottomGrid.add(timeField, 0, 1);
        bottomGrid.add(durLabel, 1, 0);  bottomGrid.add(durationField, 1, 1);
        ColumnConstraints cc1 = new ColumnConstraints(); cc1.setPercentWidth(50);
        ColumnConstraints cc2 = new ColumnConstraints(); cc2.setPercentWidth(50);
        bottomGrid.getColumnConstraints().addAll(cc1, cc2);

        // ── Assemble ───────────────────────────────────────────────
        root.getChildren().addAll(
            typeLabel, typeRow, new Separator(),
            locationLabel, physicalBox, virtualBox, new Separator(),
            calLabel, calBox,
            bottomGrid
        );

        ScrollPane scroll = new ScrollPane(root);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: transparent; -fx-background-color: transparent;");

        // ── Dialog (keeping native dialog for edit, but with polished pane style) ──
        ButtonType confirmType = new ButtonType("✅ Sauvegarder", ButtonBar.ButtonData.OK_DONE);
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Modifier le Rendez-vous");
        dialog.getDialogPane().setContent(scroll);
        dialog.getDialogPane().getButtonTypes().addAll(confirmType, ButtonType.CANCEL);
        dialog.getDialogPane().setPrefWidth(510);
        dialog.getDialogPane().setStyle(
            "-fx-background-color: #f1f5f9;" +
            "-fx-border-color: transparent;"
        );
        // Style the confirm button
        Platform.runLater(() -> {
            Button okBtn = (Button) dialog.getDialogPane().lookupButton(confirmType);
            if (okBtn != null) {
                okBtn.setStyle(
                    "-fx-background-color: linear-gradient(to right, #4f46e5, #7c3aed);" +
                    "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13;" +
                    "-fx-padding: 9 22; -fx-background-radius: 10; -fx-cursor: hand;" +
                    "-fx-effect: dropshadow(gaussian, rgba(79,70,229,0.4), 10, 0, 0, 2);"
                );
            }
            Button cancelBtn2 = (Button) dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
            if (cancelBtn2 != null) {
                cancelBtn2.setStyle(
                    "-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-font-size: 13;" +
                    "-fx-padding: 9 22; -fx-background-radius: 10;" +
                    "-fx-border-color: #e2e8f0; -fx-border-width: 1.5; -fx-border-radius: 10; -fx-cursor: hand;"
                );
            }
        });

        Optional<ButtonType> btn = dialog.showAndWait();
        if (btn.isEmpty() || btn.get() != confirmType) return;

        // ── Validate ───────────────────────────────────────────────
        String location = locationField.getText().trim();
        if (location.isEmpty()) { showAlert("Validation", "Le lieu est obligatoire."); return; }

        if (selectedDate.get() == null) { showAlert("Validation", "Sélectionnez une date."); return; }

        String timeText = timeField.getText().trim();
        if (!timeText.matches("^([01]?[0-9]|2[0-3]):[0-5][0-9]$")) {
            showAlert("Validation", "Format d'heure invalide. Utilisez HH:MM."); return;
        }
        String[] parts = timeText.split(":");
        LocalDateTime scheduledAt = selectedDate.get().atTime(
                Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        if ("scheduled".equalsIgnoreCase(meeting.getStatus()) && scheduledAt.isBefore(LocalDateTime.now())) {
            showAlert("Validation", "La date/heure doit être dans le futur."); return;
        }

        String durText = durationField.getText().trim();
        if (!durText.matches("^\\d+$")) { showAlert("Validation", "Durée invalide."); return; }
        int duration = Integer.parseInt(durText);
        if (duration <= 0 || duration > 1440) { showAlert("Validation", "Durée entre 1 et 1440 minutes."); return; }

        meeting.setMeetingType(virtualBtn.isSelected() ? "virtual" : "physical");
        meeting.setLocation(location);
        meeting.setScheduledAt(scheduledAt);
        meeting.setDuration(duration);

        Meeting updated = meetingController.update(meeting);
        if (updated != null) {
            showAlert("Succès", "Rendez-vous modifié avec succès !");
            loadDashboardData();
        } else {
            showAlert("Erreur", "Échec de la modification du rendez-vous.");
        }
    }
    
    private void handleDeleteMeeting(Meeting meeting) {
        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Confirmer la Suppression");
        confirmDialog.setHeaderText("Supprimer le Rendez-vous");
        confirmDialog.setContentText("Êtes-vous sûr de vouloir supprimer ce rendez-vous ?\n" +
                                    "Date: " + (meeting.getScheduledAt() != null ? 
                                    meeting.getScheduledAt().format(DATETIME_FORMATTER) : "TBD") + "\n" +
                                    "Lieu: " + meeting.getLocation() + "\n\n" +
                                    "Cette action est irréversible!");
        
        Optional<ButtonType> result = confirmDialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (meetingController.delete(meeting.getMeetingId())) {
                showAlert("Succès", "Rendez-vous supprimé avec succès!");
                loadDashboardData();
            } else {
                showAlert("Erreur", "Échec de la suppression du rendez-vous");
            }
        }
    }
    
    private void handleScheduleMeetingForConnection(Connection connection) {
        Meeting created = showMeetingCreationDialog(connection.getConnectionId());
        if (created != null) {
            // Add exactly 2 participants: organizer + the other person
            participantController.create(new MeetingParticipant(created.getMeetingId(), currentUserId));
            Long otherUserId = connection.getInitiatorId().equals(currentUserId) ?
                    connection.getReceiverId() : connection.getInitiatorId();
            participantController.create(new MeetingParticipant(created.getMeetingId(), otherUserId));
            loadDashboardData();
        }
    }
    
    private void viewConnectionMeetings(Connection connection) {
        List<Meeting> meetings = meetingController.findByConnection(connection.getConnectionId());
        
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Connection Meetings");
        dialog.setHeaderText("Meetings for this connection: " + meetings.size());
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        
        if (meetings.isEmpty()) {
            content.getChildren().add(new Label("No meetings scheduled yet"));
        } else {
            for (Meeting meeting : meetings) {
                content.getChildren().add(createMeetingCard(meeting));
            }
        }
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400);
        
        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }
    
    private void viewMeetingParticipants(Meeting meeting) {
        List<MeetingParticipant> participants = participantController.findByMeeting(meeting.getMeetingId());
        
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Meeting Participants");
        dialog.setHeaderText("Participants: " + participants.size());
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        
        for (MeetingParticipant participant : participants) {
            Optional<User> userOpt = userController.findById(participant.getUserId());
            if (userOpt.isPresent()) {
                Label userLabel = new Label("👤 " + userOpt.get().getUsername() + 
                    (participant.getIsActive() ? " ✓" : " (inactive)"));
                userLabel.setStyle("-fx-font-size:14px;");
                content.getChildren().add(userLabel);
            }
        }
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }
    
    private void handleDeleteConnection(Connection connection) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Supprimer Connexion");
        alert.setHeaderText("Êtes-vous sûr de vouloir supprimer cette connexion?");
        alert.setContentText("Cela supprimera également tous les rendez-vous associés.");
        
        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                if (connectionController.delete(connection.getConnectionId())) {
                    showAlert("Succès", "Connexion supprimée");
                    loadDashboardData();
                }
            }
        });
    }
    
    private String getConnectionStatusStyle(String status) {
        return switch (status.toLowerCase()) {
            case "accepted" -> "-fx-background-color: #d4edda; -fx-text-fill: #155724; -fx-padding: 3 10; " +
                              "-fx-background-radius: 12; -fx-font-size: 11px;";
            case "pending" -> "-fx-background-color: #fff3cd; -fx-text-fill: #856404; -fx-padding: 3 10; " +
                             "-fx-background-radius: 12; -fx-font-size: 11px;";
            case "rejected" -> "-fx-background-color: #f8d7da; -fx-text-fill: #721c24; -fx-padding: 3 10; " +
                              "-fx-background-radius: 12; -fx-font-size: 11px;";
            default -> "-fx-background-color: #e2e3e5; -fx-text-fill: #383d41; -fx-padding: 3 10; " +
                      "-fx-background-radius: 12; -fx-font-size: 11px;";
        };
    }
    
    private String getMeetingStatusStyle(String status) {
        return switch (status.toLowerCase()) {
            case "scheduled" -> "-fx-background-color: #cfe2ff; -fx-text-fill: #084298; -fx-padding: 3 10; " +
                               "-fx-background-radius: 12; -fx-font-size: 11px;";
            case "in_progress" -> "-fx-background-color: #fff3cd; -fx-text-fill: #856404; -fx-padding: 3 10; " +
                                 "-fx-background-radius: 12; -fx-font-size: 11px;";
            case "completed" -> "-fx-background-color: #d4edda; -fx-text-fill: #155724; -fx-padding: 3 10; " +
                               "-fx-background-radius: 12; -fx-font-size: 11px;";
            case "cancelled" -> "-fx-background-color: #f8d7da; -fx-text-fill: #721c24; -fx-padding: 3 10; " +
                               "-fx-background-radius: 12; -fx-font-size: 11px;";
            default -> "-fx-background-color: #e2e3e5; -fx-text-fill: #383d41; -fx-padding: 3 10; " +
                      "-fx-background-radius: 12; -fx-font-size: 11px;";
        };
    }
    
    private String getMeetingTypeIcon(String type) {
        return switch (type.toLowerCase()) {
            case "physical" -> "📍";
            case "virtual" -> "💻";
            default -> "📅";
        };
    }
    
    @FXML
    private void handleBack() {
        try {
            Stage stage = (Stage) backButton.getScene().getWindow();
            double width = stage.getWidth();
            double height = stage.getHeight();
            boolean wasMaximized = stage.isMaximized();
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/UserFeed.fxml"));
            Scene scene = new Scene(loader.load(), width, height);
            stage.setScene(scene);
            stage.setTitle("Ghrami - Accueil");
            if (wasMaximized) {
                stage.setMaximized(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de retourner à l'accueil : " + e.getMessage());
        }
    }
    
    // ══════════════════════════════════════════════════════════════
    //  Shared meeting creation dialog (map, Meet link, calendar)
    // ══════════════════════════════════════════════════════════════

    /**
     * Shows the full meeting creation dialog.
     * @param prefilledConnectionId if not null, locks the connection ID field
     * @return the created Meeting, or null if cancelled/failed
     */
    private Meeting showMeetingCreationDialog(String prefilledConnectionId) {
        // ── State ──────────────────────────────────────────────────
        Set<LocalDate> bookedDates = getBookedDates();
        ObjectProperty<LocalDate> selectedDate = new SimpleObjectProperty<>(null);
        String[] locationHolder = {""};
        int[] durationHolder = {60};
        Meeting[] createdMeeting = {null};

        // ══════════════════════════════════════════════════════════
        //  CUSTOM STAGE  (full styling control, no system chrome)
        // ══════════════════════════════════════════════════════════
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.initStyle(StageStyle.TRANSPARENT);
        modal.setResizable(false);

        // ── Outer wrapper (for drop shadow via padding trick) ──────
        StackPane outerWrapper = new StackPane();
        outerWrapper.setStyle("-fx-background-color: transparent;");
        outerWrapper.setPadding(new Insets(18));

        VBox rootCard = new VBox(0);
        rootCard.setStyle(
            "-fx-background-color: white;" +
            "-fx-background-radius: 18;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.38), 48, 0.1, 0, 10);"
        );
        rootCard.setPrefWidth(520);
        rootCard.setMaxWidth(520);
        outerWrapper.getChildren().add(rootCard);

        // ══ HEADER ════════════════════════════════════════════════
        StackPane header = new StackPane();
        header.setMinHeight(118);
        header.setStyle(
            "-fx-background-color: linear-gradient(to bottom right, #1e1b4b, #312e81, #4338ca);" +
            "-fx-background-radius: 18 18 0 0;" +
            "-fx-padding: 26 26 22 26;"
        );

        VBox headerText = new VBox(5);
        headerText.setAlignment(Pos.CENTER_LEFT);

        Label badgeLbl = new Label("  NOUVEAU RENDEZ-VOUS  ");
        badgeLbl.setStyle(
            "-fx-background-color: rgba(255,255,255,0.12);" +
            "-fx-text-fill: rgba(199,210,254,0.95);" +
            "-fx-font-size: 9.5; -fx-font-weight: bold;" +
            "-fx-background-radius: 20; -fx-padding: 3 10;"
        );
        Label titleLbl = new Label("Planifier un Rendez-vous");
        titleLbl.setStyle(
            "-fx-text-fill: white; -fx-font-size: 21; -fx-font-weight: bold;"
        );
        Label subtitleLbl = new Label("Organisez votre prochaine rencontre facilement");
        subtitleLbl.setStyle(
            "-fx-text-fill: rgba(199,210,254,0.75); -fx-font-size: 12;"
        );
        headerText.getChildren().addAll(badgeLbl, titleLbl, subtitleLbl);
        StackPane.setAlignment(headerText, Pos.CENTER_LEFT);

        Button closeHeaderBtn = new Button("✕");
        closeHeaderBtn.setStyle(
            "-fx-background-color: rgba(255,255,255,0.13);" +
            "-fx-text-fill: rgba(255,255,255,0.85);" +
            "-fx-font-size: 13; -fx-background-radius: 50;" +
            "-fx-min-width: 28; -fx-min-height: 28;" +
            "-fx-max-width: 28; -fx-max-height: 28;" +
            "-fx-cursor: hand; -fx-padding: 0;"
        );
        closeHeaderBtn.setOnAction(e -> modal.close());
        StackPane.setAlignment(closeHeaderBtn, Pos.TOP_RIGHT);

        header.getChildren().addAll(headerText, closeHeaderBtn);

        // Draggable via header
        double[] drag = {0, 0};
        header.setOnMousePressed(ev -> {
            drag[0] = modal.getX() - ev.getScreenX();
            drag[1] = modal.getY() - ev.getScreenY();
        });
        header.setOnMouseDragged(ev -> {
            modal.setX(ev.getScreenX() + drag[0]);
            modal.setY(ev.getScreenY() + drag[1]);
        });

        // ══ SCROLLABLE CONTENT ════════════════════════════════════
        VBox content = new VBox(14);
        content.setPadding(new Insets(22, 26, 10, 26));
        content.setStyle("-fx-background-color: #f1f5f9;");

        // ── (1) Connection ID (only shown when not pre-filled) ─────
        TextField connectionIdField = new TextField(prefilledConnectionId != null ? prefilledConnectionId : "");
        connectionIdField.setDisable(prefilledConnectionId != null);
        if (prefilledConnectionId == null) {
            VBox connCard = buildSectionCard("🔗", "Connexion", "#e0e7ff", "#6366f1");
            VBox connInner = (VBox) connCard.getChildren().get(1);
            Label connLbl = new Label("ID de la connexion");
            connLbl.setStyle("-fx-text-fill: #475569; -fx-font-size: 11.5; -fx-font-weight: 600;");
            connectionIdField.setPromptText("Entrez l'ID de connexion...");
            styleInputField(connectionIdField);
            connInner.getChildren().addAll(connLbl, connectionIdField);
            content.getChildren().add(connCard);
        }

        // ── (2) Meeting type toggle ────────────────────────────────
        VBox typeCard = buildSectionCard("🗓", "Type de rendez-vous", "#dcfce7", "#22c55e");
        VBox typeInner = (VBox) typeCard.getChildren().get(1);
        ToggleGroup typeGroup = new ToggleGroup();
        ToggleButton physicalBtn = new ToggleButton("📍   Présentiel");
        ToggleButton virtualBtn  = new ToggleButton("💻   Google Meet (virtuel)");
        physicalBtn.setToggleGroup(typeGroup);
        virtualBtn.setToggleGroup(typeGroup);
        physicalBtn.setSelected(true);
        physicalBtn.setMaxWidth(Double.MAX_VALUE);
        virtualBtn.setMaxWidth(Double.MAX_VALUE);
        HBox.setHgrow(physicalBtn, Priority.ALWAYS);
        HBox.setHgrow(virtualBtn,  Priority.ALWAYS);
        String togOn  = "-fx-background-color: #4f46e5; -fx-text-fill: white; -fx-font-weight: bold;" +
                        "-fx-padding: 10 18; -fx-background-radius: 10; -fx-cursor: hand; -fx-font-size: 12.5;" +
                        "-fx-effect: dropshadow(gaussian, rgba(79,70,229,0.4), 8, 0, 0, 2);";
        String togOff = "-fx-background-color: white; -fx-text-fill: #64748b;" +
                        "-fx-padding: 10 18; -fx-background-radius: 10; -fx-cursor: hand; -fx-font-size: 12.5;" +
                        "-fx-border-color: #e2e8f0; -fx-border-width: 1.5; -fx-border-radius: 10;";
        physicalBtn.setStyle(togOn);
        virtualBtn.setStyle(togOff);
        HBox togRow = new HBox(10, physicalBtn, virtualBtn);
        typeInner.getChildren().add(togRow);
        content.getChildren().add(typeCard);

        // ── (3) Location section ───────────────────────────────────
        Label locSectionTitle = new Label("Lieu");
        VBox locationCard = buildSectionCard("📌", "Lieu", "#fff7ed", "#f97316");
        VBox locationInner = (VBox) locationCard.getChildren().get(1);
        HBox locHeaderRow = (HBox) locationCard.getChildren().get(0);
        Label locTitleInHeader = (Label) locHeaderRow.getChildren().get(1);

        Label locFieldLabel = new Label("Adresse ou lieu de rencontre");
        locFieldLabel.setStyle("-fx-text-fill: #475569; -fx-font-size: 11.5; -fx-font-weight: 600;");
        TextField locationField = new TextField();
        locationField.setPromptText("Ex: Café Central, Avenue Habib Bourguiba, Tunis");
        styleInputField(locationField);

        Button mapBtn = new Button("🗺   Choisir sur la carte");
        mapBtn.setStyle(
            "-fx-background-color: #f0fdf4; -fx-text-fill: #16a34a; -fx-font-size: 12; -fx-font-weight: 600;" +
            "-fx-padding: 8 16; -fx-background-radius: 8;" +
            "-fx-border-color: #86efac; -fx-border-width: 1.5; -fx-border-radius: 8; -fx-cursor: hand;"
        );
        mapBtn.setOnAction(e -> {
            String picked = showMapPickerDialog();
            if (picked != null && !picked.isEmpty()) {
                locationField.setText(picked);
                locationHolder[0] = picked;
            }
        });

        Label meetInfoLabel = new Label(
            "🎥  Un lien Google Meet réel sera automatiquement créé\n" +
            "    à la confirmation via votre compte Google.");
        meetInfoLabel.setWrapText(true);
        meetInfoLabel.setStyle(
            "-fx-text-fill: #1d4ed8; -fx-font-size: 12; -fx-padding: 11 14;" +
            "-fx-background-color: #eff6ff; -fx-background-radius: 10;" +
            "-fx-border-color: #93c5fd; -fx-border-width: 1.5; -fx-border-radius: 10;"
        );

        VBox physicalBox = new VBox(8, locFieldLabel, locationField, mapBtn);
        VBox virtualBox  = new VBox(8, meetInfoLabel);
        virtualBox.setVisible(false); virtualBox.setManaged(false);
        locationInner.getChildren().addAll(physicalBox, virtualBox);
        content.getChildren().add(locationCard);

        // Toggle handlers
        physicalBtn.setOnAction(e -> {
            physicalBtn.setStyle(togOn); virtualBtn.setStyle(togOff);
            physicalBox.setVisible(true); physicalBox.setManaged(true);
            virtualBox.setVisible(false); virtualBox.setManaged(false);
            locTitleInHeader.setText("Lieu");
        });
        virtualBtn.setOnAction(e -> {
            virtualBtn.setStyle(togOn); physicalBtn.setStyle(togOff);
            physicalBox.setVisible(false); physicalBox.setManaged(false);
            virtualBox.setVisible(true); virtualBox.setManaged(true);
            locTitleInHeader.setText("Réunion virtuelle");
        });

        // ── (4) Calendar ──────────────────────────────────────────
        VBox calCard = buildSectionCard("📆", "Choisir une date", "#fdf4ff", "#a855f7");
        VBox calInner = (VBox) calCard.getChildren().get(1);
        VBox calBox = new VBox();
        buildCalendarPanel(selectedDate, bookedDates, calBox, YearMonth.now());
        calInner.getChildren().add(calBox);
        content.getChildren().add(calCard);

        // ── (5) Time & Duration ───────────────────────────────────
        VBox timeCard = buildSectionCard("⏰", "Heure & Durée", "#fffbeb", "#d97706");
        VBox timeInner = (VBox) timeCard.getChildren().get(1);

        Label timeLbl = new Label("Heure de début (HH:MM)");
        timeLbl.setStyle("-fx-text-fill: #475569; -fx-font-size: 11.5; -fx-font-weight: 600;");
        TextField timeField = new TextField("14:00");
        timeField.setPrefWidth(100);
        timeField.setStyle(
            "-fx-padding: 9 14; -fx-background-radius: 10;" +
            "-fx-background-color: white; -fx-border-color: #e2e8f0;" +
            "-fx-border-radius: 10; -fx-border-width: 1.5;" +
            "-fx-font-size: 15; -fx-font-weight: bold; -fx-alignment: center;"
        );

        Label durLbl = new Label("Durée");
        durLbl.setStyle("-fx-text-fill: #475569; -fx-font-size: 11.5; -fx-font-weight: 600;");

        // Duration chips
        TextField customDurField = new TextField();
        customDurField.setPromptText("min");
        customDurField.setPrefWidth(72);
        customDurField.setStyle(
            "-fx-padding: 8 10; -fx-background-radius: 10;" +
            "-fx-background-color: white; -fx-border-color: #e2e8f0;" +
            "-fx-border-radius: 10; -fx-border-width: 1.5; -fx-font-size: 13; -fx-alignment: center;"
        );
        customDurField.setVisible(false); customDurField.setManaged(false);

        String chipOn  = "-fx-background-color: #4f46e5; -fx-text-fill: white; -fx-font-weight: bold;" +
                         "-fx-padding: 7 14; -fx-background-radius: 20; -fx-cursor: hand; -fx-font-size: 11.5;" +
                         "-fx-effect: dropshadow(gaussian, rgba(79,70,229,0.35), 6, 0, 0, 2);";
        String chipOff = "-fx-background-color: white; -fx-text-fill: #64748b;" +
                         "-fx-padding: 7 14; -fx-background-radius: 20; -fx-cursor: hand; -fx-font-size: 11.5;" +
                         "-fx-border-color: #e2e8f0; -fx-border-width: 1.5; -fx-border-radius: 20;";

        int[] presetMins  = {30, 45, 60, 90, 120};
        String[] presetTx = {"30 min", "45 min", "1 heure", "1h 30", "2 heures"};
        Button[] chips = new Button[presetMins.length + 1];
        FlowPane chipPane = new FlowPane(8, 8);

        for (int i = 0; i < presetMins.length; i++) {
            final int dur = presetMins[i];
            Button ch = new Button(presetTx[i]);
            ch.setStyle(dur == 60 ? chipOn : chipOff);
            chips[i] = ch;
            final int ci = i;
            ch.setOnAction(ev -> {
                durationHolder[0] = dur;
                customDurField.setVisible(false); customDurField.setManaged(false);
                for (Button b : chips) if (b != null) b.setStyle(chipOff);
                ch.setStyle(chipOn);
            });
            chipPane.getChildren().add(ch);
        }
        Button customChip = new Button("✏  Autre");
        customChip.setStyle(chipOff);
        chips[presetMins.length] = customChip;
        customChip.setOnAction(ev -> {
            customDurField.setVisible(true); customDurField.setManaged(true);
            for (Button b : chips) b.setStyle(chipOff);
            customChip.setStyle(chipOn);
        });
        chipPane.getChildren().addAll(customChip, customDurField);

        HBox timeRow = new HBox(20);
        timeRow.setAlignment(Pos.TOP_LEFT);
        VBox timeLeft = new VBox(6, timeLbl, timeField);
        VBox durRight = new VBox(6, durLbl, chipPane);
        HBox.setHgrow(durRight, Priority.ALWAYS);
        timeRow.getChildren().addAll(timeLeft, durRight);
        timeInner.getChildren().add(timeRow);
        content.getChildren().add(timeCard);

        // Bottom spacer
        Region bottomSpacer = new Region();
        bottomSpacer.setMinHeight(6);
        content.getChildren().add(bottomSpacer);

        // ══ FOOTER ════════════════════════════════════════════════
        HBox footer = new HBox(12);
        footer.setAlignment(Pos.CENTER_RIGHT);
        footer.setPadding(new Insets(16, 26, 22, 26));
        footer.setStyle(
            "-fx-background-color: white;" +
            "-fx-border-color: #e2e8f0; -fx-border-width: 1 0 0 0;" +
            "-fx-background-radius: 0 0 18 18;"
        );

        Button cancelFooterBtn = new Button("Annuler");
        cancelFooterBtn.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #64748b; -fx-font-size: 13;" +
            "-fx-padding: 10 22; -fx-background-radius: 10;" +
            "-fx-border-color: #e2e8f0; -fx-border-width: 1.5; -fx-border-radius: 10; -fx-cursor: hand;"
        );
        cancelFooterBtn.setOnAction(e -> modal.close());

        Button confirmBtn = new Button("✅   Confirmer le rendez-vous");
        confirmBtn.setStyle(
            "-fx-background-color: linear-gradient(to right, #4f46e5, #7c3aed);" +
            "-fx-text-fill: white; -fx-font-size: 13; -fx-font-weight: bold;" +
            "-fx-padding: 10 26; -fx-background-radius: 10; -fx-cursor: hand;" +
            "-fx-effect: dropshadow(gaussian, rgba(79,70,229,0.45), 12, 0, 0, 3);"
        );

        // ── Confirm action: validate then create ───────────────────
        confirmBtn.setOnAction(e -> {
            String connectionId = connectionIdField.getText().trim();
            if (connectionId.isEmpty()) { showAlert("Validation", "L'ID de connexion est obligatoire."); return; }

            Optional<Connection> connOpt = connectionController.findById(connectionId);
            if (connOpt.isEmpty()) { showAlert("Validation", "Connexion introuvable."); return; }
            Connection conn = connOpt.get();
            if (!conn.getInitiatorId().equals(currentUserId) && !conn.getReceiverId().equals(currentUserId)) {
                showAlert("Validation", "Vous ne faites pas partie de cette connexion."); return;
            }

            boolean isVirtual = virtualBtn.isSelected();
            if (!isVirtual && locationField.getText().trim().isEmpty()) {
                showAlert("Validation", "Le lieu est obligatoire."); return;
            }
            if (selectedDate.get() == null) { showAlert("Validation", "Veuillez sélectionner une date."); return; }
            if (bookedDates.contains(selectedDate.get())) {
                showAlert("Date déjà réservée",
                    "Vous avez déjà un rendez-vous le " +
                    selectedDate.get().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                    ".\nVeuillez choisir une autre date.");
                return;
            }
            String timeText = timeField.getText().trim();
            if (!timeText.matches("^([01]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                showAlert("Validation", "Format d'heure invalide. Utilisez HH:MM."); return;
            }
            String[] timeParts = timeText.split(":");
            LocalDateTime scheduledAt = selectedDate.get().atTime(
                Integer.parseInt(timeParts[0]), Integer.parseInt(timeParts[1]));
            if (scheduledAt.isBefore(LocalDateTime.now())) {
                showAlert("Validation", "La date/heure doit être dans le futur."); return;
            }
            int duration;
            if (customDurField.isVisible()) {
                String durTxt = customDurField.getText().trim();
                if (!durTxt.matches("^\\d+$")) { showAlert("Validation", "Durée invalide."); return; }
                duration = Integer.parseInt(durTxt);
            } else {
                duration = durationHolder[0];
            }
            if (duration <= 0 || duration > 1440) {
                showAlert("Validation", "Durée entre 1 et 1440 minutes."); return;
            }

            // Close modal before potentially-blocking OAuth
            modal.close();

            String location;
            if (isVirtual) {
                location = createRealGoogleMeetLink(scheduledAt, duration, "Rendez-vous Ghrami");
                if (location == null) return;
            } else {
                location = locationField.getText().trim();
            }

            Meeting meeting = new Meeting(connectionId, currentUserId,
                isVirtual ? "virtual" : "physical", location, scheduledAt, duration);
            Meeting created = meetingController.create(meeting);
            if (created == null) { showAlert("Erreur", "Échec de la planification du rendez-vous."); return; }

            createdMeeting[0] = created;

            StringBuilder msg = new StringBuilder("Rendez-vous planifié avec succès !\n\n");
            msg.append("📅 Date : ").append(scheduledAt.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n");
            if (isVirtual) msg.append("🔗 Google Meet : ").append(location).append("\n");
            else           msg.append("📍 Lieu : ").append(location).append("\n");
            showAlert("Succès", msg.toString());
        });

        footer.getChildren().addAll(cancelFooterBtn, confirmBtn);

        // ── Assemble card ──────────────────────────────────────────
        ScrollPane scrollContent = new ScrollPane(content);
        scrollContent.setFitToWidth(true);
        scrollContent.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollContent.setStyle(
            "-fx-background: transparent; -fx-background-color: transparent;" +
            "-fx-border-color: transparent;"
        );
        scrollContent.setPrefViewportHeight(430);

        rootCard.getChildren().addAll(header, scrollContent, footer);

        Scene modalScene = new Scene(outerWrapper);
        modalScene.setFill(Color.TRANSPARENT);
        modal.setScene(modalScene);
        modal.showAndWait();
        return createdMeeting[0];
    }

    /**
     * Builds a styled section card used in the meeting creation/edit modals.
     * Returns a VBox where children[0] is the header HBox (icon + title)
     * and children[1] is the inner VBox for content.
     */
    private VBox buildSectionCard(String icon, String title, String iconBg, String accentColor) {
        HBox sectionHeader = new HBox(9);
        sectionHeader.setAlignment(Pos.CENTER_LEFT);
        sectionHeader.setPadding(new Insets(0, 0, 10, 0));

        Label iconLbl = new Label(icon);
        iconLbl.setStyle(
            "-fx-font-size: 12; -fx-padding: 5 8;" +
            "-fx-background-color: " + iconBg + ";" +
            "-fx-background-radius: 8;"
        );
        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #1e293b;");
        sectionHeader.getChildren().addAll(iconLbl, titleLbl);

        VBox inner = new VBox(8);

        VBox card = new VBox(0, sectionHeader, inner);
        card.setStyle(
            "-fx-background-color: white; -fx-background-radius: 14;" +
            "-fx-padding: 15 16;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.055), 10, 0, 0, 2);" +
            "-fx-border-color: " + accentColor + "28;" +
            "-fx-border-width: 1.5; -fx-border-radius: 14;"
        );
        return card;
    }

    /** Applies consistent styling to a text input field in the modals. */
    private void styleInputField(TextField field) {
        field.setStyle(
            "-fx-padding: 10 14; -fx-background-radius: 10;" +
            "-fx-background-color: white; -fx-border-color: #e2e8f0;" +
            "-fx-border-radius: 10; -fx-border-width: 1.5; -fx-font-size: 13;"
        );
    }

    // ── Build month calendar panel ─────────────────────────────────────────
    private void buildCalendarPanel(ObjectProperty<LocalDate> selectedDate,
                                     Set<LocalDate> bookedDates,
                                     VBox container,
                                     YearMonth yearMonth) {
        container.getChildren().clear();
        container.setSpacing(4);

        // Header row: prev / month+year / next
        Button prevBtn = new Button("◀");
        Button nextBtn = new Button("▶");
        prevBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 14;");
        nextBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size: 14;");
        Label monthLabel = new Label(yearMonth.getMonth().getDisplayName(
                java.time.format.TextStyle.FULL, java.util.Locale.FRENCH) + " " + yearMonth.getYear());
        monthLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14; -fx-min-width: 160; -fx-alignment: center; -fx-text-fill: #212529;");
        HBox header = new HBox(8, prevBtn, monthLabel, nextBtn);
        header.setAlignment(Pos.CENTER);

        prevBtn.setOnAction(e -> buildCalendarPanel(selectedDate, bookedDates, container, yearMonth.minusMonths(1)));
        nextBtn.setOnAction(e -> buildCalendarPanel(selectedDate, bookedDates, container, yearMonth.plusMonths(1)));

        // Weekday labels
        String[] days = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
        GridPane grid = new GridPane();
        grid.setHgap(4); grid.setVgap(4);
        for (int i = 0; i < 7; i++) {
            Label dl = new Label(days[i]);
            dl.setMinWidth(40); dl.setAlignment(Pos.CENTER);
            dl.setStyle("-fx-font-weight: bold; -fx-font-size: 11; -fx-text-fill: #6c757d;");
            grid.add(dl, i, 0);
        }

        // Days of month
        LocalDate first = yearMonth.atDay(1);
        int startCol = first.getDayOfWeek().getValue() - 1; // Mon=0
        int daysInMonth = yearMonth.lengthOfMonth();
        LocalDate today = LocalDate.now();

        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = yearMonth.atDay(day);
            int col = (startCol + day - 1) % 7;
            int row = (startCol + day - 1) / 7 + 1;

            Button dayBtn = new Button(String.valueOf(day));
            dayBtn.setMinWidth(40); dayBtn.setMinHeight(36);
            dayBtn.setAlignment(Pos.CENTER);

            boolean isPast   = date.isBefore(today);
            boolean isBooked = bookedDates.contains(date);
            boolean isSelected = date.equals(selectedDate.get());
            boolean isToday  = date.equals(today);

            String style;
            if (isBooked) {
                style = "-fx-background-color: #f8d7da; -fx-text-fill: #721c24; -fx-background-radius: 20; -fx-cursor: default;";
                dayBtn.setDisable(true);
                dayBtn.setTooltip(new Tooltip("Rendez-vous déjà prévu ce jour"));
            } else if (isPast) {
                style = "-fx-background-color: #e9ecef; -fx-text-fill: #adb5bd; -fx-background-radius: 20; -fx-cursor: default;";
                dayBtn.setDisable(true);
            } else if (isSelected) {
                style = "-fx-background-color: #667eea; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-cursor: hand;";
            } else if (isToday) {
                style = "-fx-background-color: #d4edda; -fx-text-fill: #155724; -fx-font-weight: bold; -fx-background-radius: 20; -fx-cursor: hand;";
            } else {
                style = "-fx-background-color: white; -fx-text-fill: #212529; -fx-background-radius: 20; -fx-cursor: hand; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 4, 0, 0, 1);";
            }
            dayBtn.setStyle(style);

            if (!isPast && !isBooked) {
                final LocalDate d = date;
                dayBtn.setOnAction(e -> {
                    selectedDate.set(d);
                    buildCalendarPanel(selectedDate, bookedDates, container, yearMonth); // redraw
                });
                // Hover effects
                final String base = style;
                if (!isSelected) {
                    dayBtn.setOnMouseEntered(ev -> dayBtn.setStyle(
                            "-fx-background-color: #e8e9ff; -fx-text-fill: #212529; -fx-background-radius: 20; -fx-cursor: hand;"));
                    dayBtn.setOnMouseExited(ev -> dayBtn.setStyle(base));
                }
            }
            grid.add(dayBtn, col, row);
        }

        // Legend
        HBox legend = new HBox(12);
        legend.setAlignment(Pos.CENTER);
        legend.getChildren().addAll(
            legendDot("#f8d7da", "#721c24", "Réservé"),
            legendDot("#667eea", "white",   "Sélectionné"),
            legendDot("#d4edda", "#155724", "Aujourd'hui"),
            legendDot("white",   "#333",    "Disponible")
        );

        container.getChildren().addAll(header, grid, legend);
    }

    private HBox legendDot(String bg, String fg, String text) {
        Label dot = new Label("●");
        dot.setStyle("-fx-text-fill: " + bg + "; -fx-font-size: 16;");
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 11; -fx-text-fill: #495057;");
        HBox h = new HBox(4, dot, lbl);
        h.setAlignment(Pos.CENTER_LEFT);
        return h;
    }

    // ── Booked dates for current user ──────────────────────────────────────
    private Set<LocalDate> getBookedDates() {
        Set<LocalDate> booked = new HashSet<>();
        List<Meeting> myMeetings = meetingController.findByOrganizer(currentUserId);
        for (Meeting m : myMeetings) {
            if (m.getScheduledAt() != null &&
                    !"cancelled".equalsIgnoreCase(m.getStatus()) &&
                    !"completed".equalsIgnoreCase(m.getStatus())) {
                booked.add(m.getScheduledAt().toLocalDate());
            }
        }
        return booked;
    }

    // ── Map picker using WebView + OpenStreetMap / Leaflet ─────────────────
    private String showMapPickerDialog() {
        Stage mapStage = new Stage();
        mapStage.initModality(Modality.APPLICATION_MODAL);
        mapStage.setTitle("📍 Choisir un lieu sur la carte");

        WebView webView = new WebView();
        WebEngine engine = webView.getEngine();
        webView.setPrefSize(700, 450);

        String html = """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="utf-8"/>
              <link rel="stylesheet" href="https://unpkg.com/leaflet@1.9.4/dist/leaflet.css"/>
              <script src="https://unpkg.com/leaflet@1.9.4/dist/leaflet.js"></script>
              <style>html,body,#map{height:100%;margin:0;padding:0;}</style>
            </head>
            <body>
              <div id="map"></div>
              <script>
                window.selectedAddress = '';
                var map = L.map('map').setView([36.8, 10.18], 7);
                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png',
                  {attribution:'© OpenStreetMap contributors'}).addTo(map);
                var marker = null;
                map.on('click', function(e){
                  var lat = e.latlng.lat.toFixed(6);
                  var lng = e.latlng.lng.toFixed(6);
                  if(marker) map.removeLayer(marker);
                  marker = L.marker([lat, lng]).addTo(map);
                  // Reverse geocode via Nominatim
                  fetch('https://nominatim.openstreetmap.org/reverse?format=json&lat='+lat+'&lon='+lng)
                    .then(r => r.json())
                    .then(d => {
                      window.selectedAddress = d.display_name || (lat+', '+lng);
                      marker.bindPopup('<b>'+window.selectedAddress+'</b>').openPopup();
                    })
                    .catch(()=>{ window.selectedAddress = lat+', '+lng; });
                });
              </script>
            </body>
            </html>
            """;

        engine.loadContent(html);

        Label hint = new Label("Cliquez sur la carte pour choisir l'emplacement");
        hint.setStyle("-fx-font-size: 13; -fx-text-fill: #495057; -fx-padding: 6;");

        TextField addrField = new TextField();
        addrField.setPromptText("L'adresse s'affichera ici après avoir cliqué");
        addrField.setEditable(false);
        addrField.setStyle("-fx-padding: 8;");

        Button confirmBtn = new Button("✅ Confirmer ce lieu");
        confirmBtn.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; " +
                "-fx-background-radius: 20; -fx-padding: 8 20; -fx-cursor: hand;");

        // Poll for JS result every 500 ms so the address field stays up to date
        javafx.animation.Timeline poller = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.millis(500), ae -> {
                try {
                    Object res = engine.executeScript("window.selectedAddress");
                    if (res instanceof String s && !s.isEmpty()) addrField.setText(s);
                } catch (Exception ignored) {}
            })
        );
        poller.setCycleCount(javafx.animation.Animation.INDEFINITE);
        poller.play();

        Button cancelBtn = new Button("Annuler");
        cancelBtn.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");

        String[] result = {null};
        confirmBtn.setOnAction(e -> {
            if (!addrField.getText().isEmpty()) {
                result[0] = addrField.getText();
                poller.stop();
                mapStage.close();
            }
        });
        cancelBtn.setOnAction(e -> { poller.stop(); mapStage.close(); });

        HBox btnRow = new HBox(10, confirmBtn, cancelBtn);
        btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setPadding(new Insets(8));

        VBox root = new VBox(4, hint, webView, addrField, btnRow);
        mapStage.setScene(new Scene(root, 700, 560));
        mapStage.showAndWait();
        return result[0];
    }

    // ── Real Google Meet via OAuth2 PKCE + Google Calendar API ───────────────
    /**
     * Opens the user's browser for Google sign-in, then calls the Google Calendar API
     * to create a real event with a Google Meet conference link.
     *
     * Prerequisites (one-time setup):
     *  1. Go to https://console.cloud.google.com and create a project.
     *  2. Enable the "Google Calendar API".
     *  3. Create OAuth 2.0 credentials → Application type: "Desktop app".
     *  4. Copy the Client ID into the GOOGLE_CLIENT_ID constant at the top of this file.
     *
     * @return the real hangoutLink (e.g. https://meet.google.com/abc-defg-hij) or null on failure
     */
    /**
     * Creates a REAL Google Meet room via the Google Calendar API and returns the hangout link.
     *
     * Token priority:
     *  1. Cached token from a previous call this session (calendarAccessToken field)
     *  2. Full browser OAuth flow — opens once, then the token is cached for the rest of the session
     *
     * NOTE: Your Google Sign-In must request the extra scope:
     *       https://www.googleapis.com/auth/calendar.events
     *       If it currently only uses openid/email/profile you'll hit step 3 the first time.
     */
    private String createRealGoogleMeetLink(LocalDateTime scheduledAt, int durationMinutes, String meetingTitle) {
        try {
            // ── 1. Try cached token ──────────────────────────────────────────
            String accessToken = calendarAccessToken;

            // ── 2. Full PKCE browser OAuth flow (only if nothing is cached) ──
            // The token is cached in calendarAccessToken after the first successful login,
            // so the browser will only open once per app session.
            if (accessToken == null || accessToken.isEmpty()) {
                accessToken = doGoogleCalendarOAuthFlow();
                if (accessToken == null) return null; // user cancelled or error
            }

            // ── Try calling the API; if 401 clear cache and retry once ───────
            String link = callCalendarApiCreateMeet(accessToken, scheduledAt, durationMinutes, meetingTitle);
            if (link == null && calendarAccessToken != null) {
                // Token may be expired – clear and retry via OAuth
                calendarAccessToken = null;
                accessToken = doGoogleCalendarOAuthFlow();
                if (accessToken == null) return null;
                link = callCalendarApiCreateMeet(accessToken, scheduledAt, durationMinutes, meetingTitle);
            }

            // ── Cache the working token ──────────────────────────────────────
            if (link != null) calendarAccessToken = accessToken;
            return link;

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur Google Meet", "Impossible de créer la réunion : " + e.getMessage());
            return null;
        }
    }

    /** Performs the PKCE OAuth consent flow and returns the access token, or null on failure. */
    private String doGoogleCalendarOAuthFlow() {
        if (GOOGLE_CLIENT_ID.startsWith("YOUR_GOOGLE") || GOOGLE_CLIENT_SECRET.startsWith("YOUR_CLIENT_SECRET")) {
            showAlert("Configuration requise",
                "Renseignez votre Google Client ID dans la constante GOOGLE_CLIENT_ID.\n\n" +
                "1. https://console.cloud.google.com → nouveau projet\n" +
                "2. Activez « Google Calendar API »\n" +
                "3. Créez un identifiant OAuth 2.0 (Application de bureau)\n" +
                "4. Ajoutez le scope : calendar.events\n" +
                "5. Copiez le Client ID dans le code source.");
            return null;
        }
        try {
            // PKCE code_verifier + code_challenge
            byte[] verifierBytes = new byte[32];
            new SecureRandom().nextBytes(verifierBytes);
            String codeVerifier = Base64.getUrlEncoder().withoutPadding().encodeToString(verifierBytes);
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(
                    codeVerifier.getBytes(StandardCharsets.UTF_8));
            String codeChallenge = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);

            // Local callback HTTP server
            HttpServer callbackServer = HttpServer.create(new InetSocketAddress(8765), 0);
            CompletableFuture<String> authCodeFuture = new CompletableFuture<>();
            callbackServer.createContext("/callback", exchange -> {
                String query = exchange.getRequestURI().getQuery();
                String code = null;
                if (query != null)
                    for (String param : query.split("&"))
                        if (param.startsWith("code=")) { code = param.substring(5); break; }
                String html = "<!DOCTYPE html><html lang='fr'><head><meta charset='UTF-8'/>" +
                    "<meta name='viewport' content='width=device-width,initial-scale=1'/>" +
                    "<title>Ghrami – Connexion réussie</title>" +
                    "<link href='https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700;800&display=swap' rel='stylesheet'/>" +
                    "<style>" +
                    "*{margin:0;padding:0;box-sizing:border-box;}" +
                    "body{font-family:'Inter',sans-serif;min-height:100vh;display:flex;align-items:center;" +
                    "justify-content:center;background:linear-gradient(135deg,#5a3fb8 0%,#6b4fd1 45%,#7c5cff 100%);overflow:hidden;}" +
                    ".blob{position:fixed;border-radius:50%;opacity:.13;pointer-events:none;}" +
                    ".b1{width:700px;height:700px;background:white;top:-220px;left:-200px;}" +
                    ".b2{width:500px;height:500px;background:white;bottom:-160px;right:-150px;}" +
                    ".b3{width:280px;height:280px;background:white;top:42%;right:4%;}" +
                    ".card{position:relative;background:white;border-radius:28px;padding:56px 52px 48px;" +
                    "text-align:center;max-width:460px;width:calc(100% - 40px);" +
                    "box-shadow:0 32px 80px rgba(90,63,184,.38),0 8px 24px rgba(90,63,184,.18);}" +
                    ".logo{display:inline-flex;align-items:center;gap:10px;margin-bottom:32px;}" +
                    ".logo-box{width:48px;height:48px;border-radius:13px;" +
                    "background:linear-gradient(135deg,#5a3fb8,#7c5cff);" +
                    "display:flex;align-items:center;justify-content:center;" +
                    "font-size:24px;color:white;font-weight:800;}" +
                    ".logo-name{font-size:24px;font-weight:800;" +
                    "background:linear-gradient(135deg,#5a3fb8,#7c5cff);" +
                    "-webkit-background-clip:text;-webkit-text-fill-color:transparent;background-clip:text;}" +
                    ".ring{width:88px;height:88px;border-radius:50%;" +
                    "background:linear-gradient(135deg,#5a3fb8,#7c5cff);" +
                    "display:flex;align-items:center;justify-content:center;" +
                    "margin:0 auto 26px;" +
                    "box-shadow:0 12px 36px rgba(102,126,234,.55);" +
                    "animation:pop .55s cubic-bezier(.34,1.56,.64,1) both;}" +
                    "@keyframes pop{from{transform:scale(0);opacity:0}to{transform:scale(1);opacity:1}}" +
                    "h1{font-size:1.7rem;font-weight:800;color:#1c1e21;margin-bottom:12px;}" +
                    "p{font-size:.97rem;color:#65676b;line-height:1.65;margin-bottom:30px;}" +
                    ".badge{display:inline-flex;align-items:center;gap:8px;" +
                    "padding:10px 22px;border-radius:50px;" +
                    "background:linear-gradient(135deg,rgba(90,63,184,.1),rgba(124,92,255,.1));" +
                    "border:1.5px solid rgba(102,126,234,.25);margin-bottom:32px;}" +
                    ".dot{width:8px;height:8px;border-radius:50%;background:#5a3fb8;" +
                    "animation:pulse 1.6s ease-in-out infinite;}" +
                    "@keyframes pulse{0%,100%{opacity:1;transform:scale(1)}50%{opacity:.4;transform:scale(.75)}}" +
                    ".badge-text{font-size:.84rem;font-weight:700;color:#5a3fb8;}" +
                    ".divider{height:1px;background:linear-gradient(to right,transparent,#e4e6eb,transparent);margin-bottom:22px;}" +
                    ".hint{font-size:.78rem;color:#adb5bd;}" +
                    ".brand{font-size:.72rem;color:#c0c4cc;font-weight:600;letter-spacing:.6px;text-transform:uppercase;margin-top:18px;}" +
                    "</style></head><body>" +
                    "<div class='blob b1'></div><div class='blob b2'></div><div class='blob b3'></div>" +
                    "<div class='card'>" +
                    "<div class='logo'><div class='logo-box'>G</div><span class='logo-name'>Ghrami</span></div>" +
                    "<div class='ring'><svg width='42' height='42' viewBox='0 0 42 42' fill='none'>" +
                    "<path d='M7 21L17 31L35 11' stroke='white' stroke-width='4' stroke-linecap='round' stroke-linejoin='round'/>" +
                    "</svg></div>" +
                    "<h1>Connexion réussie\u00a0!</h1>" +
                    "<p>Votre compte Google a été autorisé avec succès.<br/>Retournez à l'application — votre réunion est en cours de création.</p>" +
                    "<div class='badge'><div class='dot'></div><span class='badge-text'>Google Meet activé</span></div>" +
                    "<div class='divider'></div>" +
                    "<p class='hint'>Vous pouvez fermer cet onglet</p>" +
                    "<p class='brand'>by OPGG</p>" +
                    "</div></body></html>";
                exchange.getResponseHeaders().set("Content-Type", "text/html; charset=UTF-8");
                exchange.sendResponseHeaders(200, html.getBytes(StandardCharsets.UTF_8).length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(html.getBytes(StandardCharsets.UTF_8)); }
                authCodeFuture.complete(code);
            });
            callbackServer.setExecutor(null);
            callbackServer.start();

            // Open Google consent screen in browser
            String authUrl = "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id="           + URLEncoder.encode(GOOGLE_CLIENT_ID, StandardCharsets.UTF_8)
                + "&redirect_uri="        + URLEncoder.encode(GOOGLE_REDIRECT_URI, StandardCharsets.UTF_8)
                + "&response_type=code"
                + "&scope="              + URLEncoder.encode(GOOGLE_SCOPE, StandardCharsets.UTF_8)
                + "&code_challenge="     + codeChallenge
                + "&code_challenge_method=S256"
                + "&access_type=offline"
                + "&prompt=consent";

            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Connexion Google requise");
            info.setHeaderText("Votre navigateur va s'ouvrir");
            info.setContentText("Connectez-vous à Google pour autoriser la création de réunions Meet.\nRevenez ici – la fenêtre se fermera automatiquement.");
            info.showAndWait();

            Desktop.getDesktop().browse(new URI(authUrl));

            // Wait for the auth code (2-minute timeout)
            String authCode = authCodeFuture.get(120, TimeUnit.SECONDS);
            callbackServer.stop(0);
            if (authCode == null) { showAlert("Erreur", "Code d'autorisation non reçu."); return null; }

            // Exchange auth code → access token
            String tokenBody = "client_id="     + URLEncoder.encode(GOOGLE_CLIENT_ID, StandardCharsets.UTF_8)
                + "&client_secret="             + URLEncoder.encode(GOOGLE_CLIENT_SECRET, StandardCharsets.UTF_8)
                + "&code="                      + URLEncoder.encode(authCode, StandardCharsets.UTF_8)
                + "&code_verifier="             + URLEncoder.encode(codeVerifier, StandardCharsets.UTF_8)
                + "&grant_type=authorization_code"
                + "&redirect_uri="              + URLEncoder.encode(GOOGLE_REDIRECT_URI, StandardCharsets.UTF_8);

            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest tokenRequest = HttpRequest.newBuilder()
                .uri(new URI("https://oauth2.googleapis.com/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(tokenBody))
                .build();

            HttpResponse<String> tokenResponse = httpClient.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
            String accessToken = extractJsonValue(tokenResponse.body(), "access_token");
            if (accessToken == null) {
                showAlert("Erreur Google", "Token d'accès non obtenu.\n" + tokenResponse.body());
                return null;
            }
            return accessToken;

        } catch (java.util.concurrent.TimeoutException e) {
            showAlert("Délai dépassé", "L'authentification Google a expiré (2 min). Veuillez réessayer.");
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur OAuth", "Erreur d'authentification Google : " + e.getMessage());
            return null;
        }
    }

    /** Calls the Google Calendar API to create an event with a Meet conference and returns the hangoutLink. */
    private String callCalendarApiCreateMeet(String accessToken,
                                              LocalDateTime scheduledAt,
                                              int durationMinutes,
                                              String title) throws Exception {
        String startIso = scheduledAt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        String endIso   = scheduledAt.plusMinutes(durationMinutes)
                                     .format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
        String requestId = "ghrami-" + System.currentTimeMillis();

        String eventJson = "{"
            + "\"summary\": \"" + title.replace("\"", "'") + "\","
            + "\"start\": {\"dateTime\": \"" + startIso + "\", \"timeZone\": \"Africa/Tunis\"},"
            + "\"end\":   {\"dateTime\": \"" + endIso   + "\", \"timeZone\": \"Africa/Tunis\"},"
            + "\"conferenceData\": {"
            + "  \"createRequest\": {"
            + "    \"requestId\": \"" + requestId + "\","
            + "    \"conferenceSolutionKey\": {\"type\": \"hangoutsMeet\"}"
            + "  }"
            + "}"
            + "}";

        HttpClient httpClient = HttpClient.newHttpClient();
        HttpRequest eventRequest = HttpRequest.newBuilder()
            .uri(new URI("https://www.googleapis.com/calendar/v3/calendars/primary/events"
                         + "?conferenceDataVersion=1"))
            .header("Authorization", "Bearer " + accessToken)
            .header("Content-Type", "application/json; charset=UTF-8")
            .POST(HttpRequest.BodyPublishers.ofString(eventJson, StandardCharsets.UTF_8))
            .build();

        HttpResponse<String> eventResponse = httpClient.send(eventRequest, HttpResponse.BodyHandlers.ofString());

        // 401 = token expired → caller will retry with fresh token
        if (eventResponse.statusCode() == 401) return null;

        String hangoutLink = extractJsonValue(eventResponse.body(), "hangoutLink");
        if (hangoutLink == null) {
            showAlert("Erreur Google Calendar",
                "Impossible de créer la réunion Meet.\nCode: " + eventResponse.statusCode() +
                "\nRéponse: " + eventResponse.body().substring(0, Math.min(300, eventResponse.body().length())));
        }
        return hangoutLink;
    }

    /** Simple JSON string-field extractor (no external library needed). */
    private String extractJsonValue(String json, String key) {
        String search = "\"" + key + "\"";
        int keyIdx = json.indexOf(search);
        if (keyIdx < 0) return null;
        int colon = json.indexOf(':', keyIdx + search.length());
        if (colon < 0) return null;
        // Find the opening quote
        int start = json.indexOf('"', colon + 1);
        if (start < 0) return null;
        int end = start + 1;
        while (end < json.length()) {
            if (json.charAt(end) == '"' && json.charAt(end - 1) != '\\') break;
            end++;
        }
        return json.substring(start + 1, end);
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
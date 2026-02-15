package opgg.ghrami.view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import opgg.ghrami.controller.*;
import opgg.ghrami.model.*;
import opgg.ghrami.util.SessionManager;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
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
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");
    
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
        
        Button viewMeetingsBtn = new Button("Voir Rendez-vous");
        viewMeetingsBtn.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; " +
                                "-fx-padding: 8 15; -fx-background-radius: 5; -fx-cursor: hand;");
        viewMeetingsBtn.setOnAction(e -> viewConnectionMeetings(connection));
        
        Button deleteBtn = new Button("Supprimer");
        deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                          "-fx-padding: 8 15; -fx-background-radius: 5; -fx-cursor: hand;");
        deleteBtn.setOnAction(e -> handleDeleteConnection(connection));
        
        actions.getChildren().addAll(viewMeetingsBtn, deleteBtn);
        
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
        VBox details = new VBox(5);
        
        Label locationLabel = new Label("📍 " + meeting.getLocation());
        locationLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #34495e;");
        
        Label durationLabel = new Label("⏱ Durée : " + meeting.getDuration() + " minutes");
        durationLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #34495e;");
        
        int participantCount = participantController.countParticipants(meeting.getMeetingId());
        Label participantsLabel = new Label("👥 Participants : " + participantCount);
        participantsLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #34495e;");
        
        details.getChildren().addAll(locationLabel, durationLabel, participantsLabel);
        
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
            
            Button cancelBtn = new Button("✗ Annuler");
            cancelBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                              "-fx-padding: 8 15; -fx-background-radius: 5; -fx-cursor: hand;");
            cancelBtn.setOnAction(e -> {
                if (meetingController.cancelMeeting(meeting.getMeetingId())) {
                    showAlert("Succès", "Rendez-vous annulé");
                    loadDashboardData();
                }
            });
            
            actions.getChildren().addAll(completeBtn, cancelBtn);
        }
        
        Button viewParticipantsBtn = new Button("Voir Participants");
        viewParticipantsBtn.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; " +
                                    "-fx-padding: 8 15; -fx-background-radius: 5; -fx-cursor: hand;");
        viewParticipantsBtn.setOnAction(e -> viewMeetingParticipants(meeting));
        
        actions.getChildren().add(viewParticipantsBtn);
        
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
        Dialog<Meeting> dialog = new Dialog<>();
        dialog.setTitle("Planifier Nouveau Rendez-vous");
        dialog.setHeaderText("Créer un rendez-vous avec une de vos connexions");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        TextField connectionIdField = new TextField();
        connectionIdField.setPromptText("ID Connexion");
        
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("physical", "virtual");
        typeCombo.setValue("physical");
        
        TextField locationField = new TextField();
        locationField.setPromptText("Location or URL");
        
        DatePicker datePicker = new DatePicker();
        datePicker.setPromptText("Date");
        
        TextField timeField = new TextField();
        timeField.setPromptText("Heure (HH:MM)");
        
        TextField durationField = new TextField();
        durationField.setPromptText("Durée (minutes)");
        durationField.setText("60");
        
        grid.add(new Label("ID Connexion :"), 0, 0);
        grid.add(connectionIdField, 1, 0);
        grid.add(new Label("Type :"), 0, 1);
        grid.add(typeCombo, 1, 1);
        grid.add(new Label("Lieu :"), 0, 2);
        grid.add(locationField, 1, 2);
        grid.add(new Label("Date :"), 0, 3);
        grid.add(datePicker, 1, 3);
        grid.add(new Label("Heure :"), 0, 4);
        grid.add(timeField, 1, 4);
        grid.add(new Label("Durée :"), 0, 5);
        grid.add(durationField, 1, 5);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                try {
                    // Validation 1: Connection ID is required
                    String connectionId = connectionIdField.getText().trim();
                    if (connectionId.isEmpty()) {
                        showAlert("Erreur de Validation", "L'ID de connexion est obligatoire.");
                        return null;
                    }
                    
                    // Validation 2: Connection must exist and belong to user
                    Optional<Connection> connOpt = connectionController.findById(connectionId);
                    if (connOpt.isEmpty()) {
                        showAlert("Erreur de Validation", "Connexion introuvable.");
                        return null;
                    }
                    Connection conn = connOpt.get();
                    if (!conn.getInitiatorId().equals(currentUserId) && !conn.getReceiverId().equals(currentUserId)) {
                        showAlert("Erreur de Validation", "Vous ne faites pas partie de cette connexion.");
                        return null;
                    }
                    
                    // Validation 3: Meeting type is required
                    if (typeCombo.getValue() == null || typeCombo.getValue().trim().isEmpty()) {
                        showAlert("Erreur de Validation", "Le type de réunion est obligatoire.");
                        return null;
                    }
                    
                    // Validation 4: Location is required
                    String location = locationField.getText().trim();
                    if (location.isEmpty()) {
                        showAlert("Erreur de Validation", "Le lieu est obligatoire.");
                        return null;
                    }
                    
                    // Validation 5: Date is required
                    if (datePicker.getValue() == null) {
                        showAlert("Erreur de Validation", "La date est obligatoire.");
                        return null;
                    }
                    
                    // Validation 6: Time format validation
                    String timeText = timeField.getText().trim();
                    if (!timeText.matches("^([01]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                        showAlert("Erreur de Validation", "Format d'heure invalide. Utilisez HH:MM (ex: 14:30).");
                        return null;
                    }
                    
                    String[] timeParts = timeText.split(":");
                    LocalDateTime scheduledAt = datePicker.getValue().atTime(
                        Integer.parseInt(timeParts[0]),
                        Integer.parseInt(timeParts[1])
                    );
                    
                    // Validation 7: Date must be in the future
                    if (scheduledAt.isBefore(LocalDateTime.now())) {
                        showAlert("Erreur de Validation", "La date/heure doit être dans le futur.");
                        return null;
                    }
                    
                    // Validation 8: Duration must be positive
                    String durationText = durationField.getText().trim();
                    if (durationText.isEmpty() || !durationText.matches("^\\d+$")) {
                        showAlert("Erreur de Validation", "La durée doit être un nombre positif.");
                        return null;
                    }
                    int duration = Integer.parseInt(durationText);
                    if (duration <= 0 || duration > 1440) {
                        showAlert("Erreur de Validation", "La durée doit être entre 1 et 1440 minutes (24h).");
                        return null;
                    }
                    
                    Meeting meeting = new Meeting(
                        connectionId,
                        currentUserId,
                        typeCombo.getValue(),
                        location,
                        scheduledAt,
                        duration
                    );
                    return meeting;
                } catch (Exception e) {
                    showAlert("Erreur de Validation", "Saisie invalide: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });
        
        Optional<Meeting> result = dialog.showAndWait();
        result.ifPresent(meeting -> {
            Meeting created = meetingController.create(meeting);
            if (created != null) {
                // Add organizer as participant
                MeetingParticipant participant = new MeetingParticipant(created.getMeetingId(), currentUserId);
                participantController.create(participant);
                
                showAlert("Succès", "Rendez-vous planifié avec succès!");
                loadDashboardData();
            } else {
                showAlert("Erreur", "Échec de la planification du rendez-vous");
            }
        });
    }
    
    private void handleScheduleMeetingForConnection(Connection connection) {
        // Pre-fill connection ID when scheduling from connection card
        Dialog<Meeting> dialog = new Dialog<>();
        dialog.setTitle("Planifier Rendez-vous");
        dialog.setHeaderText("Planifier un rendez-vous pour cette connexion");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        ComboBox<String> typeCombo = new ComboBox<>();
        typeCombo.getItems().addAll("physical", "virtual");
        typeCombo.setValue("physical");
        
        TextField locationField = new TextField();
        locationField.setPromptText("Lieu ou URL");
        
        DatePicker datePicker = new DatePicker();
        TextField timeField = new TextField();
        timeField.setPromptText("HH:MM");
        timeField.setText("14:00");
        
        TextField durationField = new TextField();
        durationField.setText("60");
        
        grid.add(new Label("Type :"), 0, 0);
        grid.add(typeCombo, 1, 0);
        grid.add(new Label("Lieu :"), 0, 1);
        grid.add(locationField, 1, 1);
        grid.add(new Label("Date :"), 0, 2);
        grid.add(datePicker, 1, 2);
        grid.add(new Label("Heure :"), 0, 3);
        grid.add(timeField, 1, 3);
        grid.add(new Label("Durée (min) :"), 0, 4);
        grid.add(durationField, 1, 4);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                try {
                    // Validation 1: Meeting type is required
                    if (typeCombo.getValue() == null || typeCombo.getValue().trim().isEmpty()) {
                        showAlert("Erreur de Validation", "Le type de réunion est obligatoire.");
                        return null;
                    }
                    
                    // Validation 2: Location is required
                    String location = locationField.getText().trim();
                    if (location.isEmpty()) {
                        showAlert("Erreur de Validation", "Le lieu est obligatoire.");
                        return null;
                    }
                    
                    // Validation 3: Date is required
                    if (datePicker.getValue() == null) {
                        showAlert("Erreur de Validation", "La date est obligatoire.");
                        return null;
                    }
                    
                    // Validation 4: Time format validation
                    String timeText = timeField.getText().trim();
                    if (!timeText.matches("^([01]?[0-9]|2[0-3]):[0-5][0-9]$")) {
                        showAlert("Erreur de Validation", "Format d'heure invalide. Utilisez HH:MM (ex: 14:30).");
                        return null;
                    }
                    
                    String[] time = timeText.split(":");
                    LocalDateTime scheduledAt = datePicker.getValue().atTime(
                        Integer.parseInt(time[0]), Integer.parseInt(time[1])
                    );
                    
                    // Validation 5: Date must be in the future
                    if (scheduledAt.isBefore(LocalDateTime.now())) {
                        showAlert("Erreur de Validation", "La date/heure doit être dans le futur.");
                        return null;
                    }
                    
                    // Validation 6: Duration must be valid
                    String durationText = durationField.getText().trim();
                    if (durationText.isEmpty() || !durationText.matches("^\\d+$")) {
                        showAlert("Erreur de Validation", "La durée doit être un nombre positif.");
                        return null;
                    }
                    int duration = Integer.parseInt(durationText);
                    if (duration <= 0 || duration > 1440) {
                        showAlert("Erreur de Validation", "La durée doit être entre 1 et 1440 minutes (24h).");
                        return null;
                    }
                    
                    return new Meeting(
                        connection.getConnectionId(),
                        currentUserId,
                        typeCombo.getValue(),
                        location,
                        scheduledAt,
                        duration
                    );
                } catch (Exception e) {
                    showAlert("Erreur de Validation", "Saisie invalide: " + e.getMessage());
                    return null;
                }
            }
            return null;
        });
        
        Optional<Meeting> result = dialog.showAndWait();
        result.ifPresent(meeting -> {
            Meeting created = meetingController.create(meeting);
            if (created != null) {
                // Add both users as participants
                participantController.create(new MeetingParticipant(created.getMeetingId(), currentUserId));
                Long otherUserId = connection.getInitiatorId().equals(currentUserId) ? 
                    connection.getReceiverId() : connection.getInitiatorId();
                participantController.create(new MeetingParticipant(created.getMeetingId(), otherUserId));
                
                showAlert("Succès", "Rendez-vous planifié!");
                loadDashboardData();
            }
        });
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
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/UserFeed.fxml"));
            Scene scene = new Scene(loader.load(), 1400, 900);
            Stage stage = (Stage) backButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Ghrami - Accueil");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de retourner à l'accueil : " + e.getMessage());
        }
    }
    
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

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
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import opgg.ghrami.controller.BadgeController;
import opgg.ghrami.controller.ClassProviderController;
import opgg.ghrami.controller.FriendshipController;
import opgg.ghrami.controller.UserController;
import opgg.ghrami.model.Badge;
import opgg.ghrami.model.ClassProvider;
import opgg.ghrami.model.Friendship;
import opgg.ghrami.model.FriendshipStatus;
import opgg.ghrami.model.User;
import opgg.ghrami.util.BadgeNotificationUtil;
import opgg.ghrami.util.PasswordUtil;
import opgg.ghrami.util.SessionManager;

import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AdminDashboardController implements Initializable {
    
    @FXML private Label adminNameLabel;
    @FXML private Button logoutButton;

    // Count Labels - Now showing just numbers
    @FXML private Label userCountLabel;
    @FXML private Label friendshipCountLabel;
    @FXML private Label badgeCountLabel;
    @FXML private Label instructorCountLabel;

    // Search Fields
    @FXML private TextField userSearchField;
    @FXML private TextField friendshipSearchField;
    @FXML private TextField badgeSearchField;
    @FXML private TextField instructorSearchField;

    // User Tab
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Long> userIdCol;
    @FXML private TableColumn<User, String> profilePicCol;
    @FXML private TableColumn<User, String> usernameCol;
    @FXML private TableColumn<User, String> emailCol;
    @FXML private TableColumn<User, String> locationCol;
    @FXML private TableColumn<User, Boolean> onlineCol;
    @FXML private TableColumn<User, Void> userActionsCol;
    
    // Friendship Tab
    @FXML private TableView<Friendship> friendshipTable;
    @FXML private TableColumn<Friendship, Long> friendshipIdCol;
    @FXML private TableColumn<Friendship, Long> user1IdCol;
    @FXML private TableColumn<Friendship, Long> user2IdCol;
    @FXML private TableColumn<Friendship, FriendshipStatus> statusCol;
    @FXML private TableColumn<Friendship, Void> friendshipActionsCol;
    
    // Badge Tab
    @FXML private TableView<Badge> badgeTable;
    @FXML private TableColumn<Badge, Long> badgeIdCol;
    @FXML private TableColumn<Badge, Long> badgeUserIdCol;
    @FXML private TableColumn<Badge, String> badgeNameCol;
    @FXML private TableColumn<Badge, String> descriptionCol;
    @FXML private TableColumn<Badge, Void> badgeActionsCol;
    
    // Instructor Tab
    @FXML private TableView<ClassProvider> instructorTable;
    @FXML private TableColumn<ClassProvider, Long> providerIdCol;
    @FXML private TableColumn<ClassProvider, String> instructorUsernameCol;
    @FXML private TableColumn<ClassProvider, String> instructorEmailCol;
    @FXML private TableColumn<ClassProvider, String> companyNameCol;
    @FXML private TableColumn<ClassProvider, String> expertiseCol;
    @FXML private TableColumn<ClassProvider, Double> ratingCol;
    @FXML private TableColumn<ClassProvider, Boolean> verifiedCol;
    @FXML private TableColumn<ClassProvider, Void> instructorActionsCol;

    private UserController userController;
    private FriendshipController friendshipController;
    private BadgeController badgeController;
    private ClassProviderController providerController;
    
    private ObservableList<User> userList;
    private ObservableList<User> filteredUserList;
    private ObservableList<Friendship> friendshipList;
    private ObservableList<Friendship> filteredFriendshipList;
    private ObservableList<Badge> badgeList;
    private ObservableList<Badge> filteredBadgeList;
    private ObservableList<ClassProvider> instructorList;
    private ObservableList<ClassProvider> filteredInstructorList;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        userController = new UserController();
        friendshipController = new FriendshipController();
        badgeController = new BadgeController();
        providerController = ClassProviderController.getInstance();
        
        SessionManager session = SessionManager.getInstance();
        adminNameLabel.setText(session.getUsername());
        
        userList = FXCollections.observableArrayList();
        filteredUserList = FXCollections.observableArrayList();
        friendshipList = FXCollections.observableArrayList();
        filteredFriendshipList = FXCollections.observableArrayList();
        badgeList = FXCollections.observableArrayList();
        filteredBadgeList = FXCollections.observableArrayList();
        instructorList = FXCollections.observableArrayList();
        filteredInstructorList = FXCollections.observableArrayList();
        
        setupUserTable();
        setupFriendshipTable();
        setupBadgeTable();
        setupInstructorTable();
        
        loadAllData();
        setupSearchListeners();
    }
    
    @FXML
    private void handleLogout() {
        try {
            long adminId = SessionManager.getInstance().getUserId();
            Optional<User> adminOpt = userController.findById(adminId);
            if (adminOpt.isPresent()) {
                User admin = adminOpt.get();
                admin.setOnline(false);
                userController.update(admin);
            }
            
            SessionManager.getInstance().logout();
            
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            double width = stage.getWidth();
            double height = stage.getHeight();
            boolean wasMaximized = stage.isMaximized();
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/LoginView.fxml"));
            Scene scene = new Scene(loader.load(), width, height);
            stage.setScene(scene);
            stage.setTitle("Ghrami - Connexion");
            if (wasMaximized) {
                stage.setMaximized(true);
            }
        } catch (Exception e) {
            showError("Erreur lors de la déconnexion: " + e.getMessage());
        }
    }

    private void setupUserTable() {
        userIdCol.setCellValueFactory(new PropertyValueFactory<>("userId"));
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        locationCol.setCellValueFactory(new PropertyValueFactory<>("location"));
        
        // Profile Picture Column with circular avatars - FIXED property name
        profilePicCol.setCellValueFactory(new PropertyValueFactory<>("profilePicture"));
        profilePicCol.setCellFactory(param -> new TableCell<User, String>() {
            private final ImageView imageView = new ImageView();
            private final StackPane container = new StackPane();
            
            {
                imageView.setFitWidth(50);
                imageView.setFitHeight(50);
                imageView.setPreserveRatio(true);
                
                Circle clip = new Circle(25);
                clip.setCenterX(25);
                clip.setCenterY(25);
                imageView.setClip(clip);
                
                container.getChildren().add(imageView);
                container.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 3);");
            }
            
            @Override
            protected void updateItem(String profilePicUrl, boolean empty) {
                super.updateItem(profilePicUrl, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    User user = getTableView().getItems().get(getIndex());
                    String picUrl = user.getProfilePicture();
                    
                    if (picUrl == null || picUrl.trim().isEmpty()) {
                        setGraphic(createDefaultAvatar());
                    } else {
                        try {
                            Image image = loadProfileImage(picUrl);
                            
                            if (image != null && !image.isError()) {
                                imageView.setImage(image);
                                setGraphic(container);
                            } else {
                                setGraphic(createDefaultAvatar());
                            }
                        } catch (Exception e) {
                            System.err.println("Error loading profile picture: " + e.getMessage());
                            setGraphic(createDefaultAvatar());
                        }
                    }
                }
            }
        });
        
        // Online Status Column with beautiful badges
        onlineCol.setCellValueFactory(new PropertyValueFactory<>("online"));
        onlineCol.setCellFactory(param -> new TableCell<User, Boolean>() {
            @Override
            protected void updateItem(Boolean online, boolean empty) {
                super.updateItem(online, empty);
                if (empty || online == null) {
                    setGraphic(null);
                } else {
                    Label statusLabel = new Label(online ? "● Online" : "○ Offline");
                    if (online) {
                        statusLabel.setStyle("-fx-background-color: #d4edda; -fx-text-fill: #155724; " +
                                "-fx-padding: 6 14; -fx-background-radius: 15; -fx-font-weight: bold; " +
                                "-fx-font-size: 11;");
                    } else {
                        statusLabel.setStyle("-fx-background-color: #f8d7da; -fx-text-fill: #721c24; " +
                                "-fx-padding: 6 14; -fx-background-radius: 15; -fx-font-weight: bold; " +
                                "-fx-font-size: 11;");
                    }
                    setGraphic(statusLabel);
                }
            }
        });
        
        // Enhanced Action Buttons with VIEW
        userActionsCol.setCellFactory(param -> new TableCell<>() {
            private final Button viewBtn = new Button("👁️");
            private final Button editBtn = new Button("✏️");
            private final Button deleteBtn = new Button("🗑️");
            private final HBox pane = new HBox(6, viewBtn, editBtn, deleteBtn);
            
            {
                pane.setAlignment(Pos.CENTER);
                
                viewBtn.setStyle("-fx-background-color: #2196F3; " +
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 12; " +
                        "-fx-background-radius: 20; -fx-cursor: hand; -fx-font-size: 13; " +
                        "-fx-effect: dropshadow(gaussian, rgba(33,150,243,0.3), 8, 0, 0, 2);");
                
                editBtn.setStyle("-fx-background-color: #FF9800; " +
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 12; " +
                        "-fx-background-radius: 20; -fx-cursor: hand; -fx-font-size: 13; " +
                        "-fx-effect: dropshadow(gaussian, rgba(255,152,0,0.3), 8, 0, 0, 2);");
                
                deleteBtn.setStyle("-fx-background-color: #f44336; " +
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 12; " +
                        "-fx-background-radius: 20; -fx-cursor: hand; -fx-font-size: 13; " +
                        "-fx-effect: dropshadow(gaussian, rgba(244,67,54,0.3), 8, 0, 0, 2);");
                
                viewBtn.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleViewUser(user);
                });
                
                editBtn.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleEditUser(user);
                });
                
                deleteBtn.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleDeleteUserInline(user);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
        
        userTable.setItems(userList);
    }

    private void setupFriendshipTable() {
        friendshipIdCol.setCellValueFactory(new PropertyValueFactory<>("friendshipId"));
        user1IdCol.setCellValueFactory(new PropertyValueFactory<>("user1Id"));
        user2IdCol.setCellValueFactory(new PropertyValueFactory<>("user2Id"));
        
        // Status Column with colorful badges - FIXED
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusCol.setCellFactory(param -> new TableCell<Friendship, FriendshipStatus>() {
            @Override
            protected void updateItem(FriendshipStatus status, boolean empty) {
                super.updateItem(status, empty);
                if (empty || status == null) {
                    setGraphic(null);
                } else {
                    Label statusLabel = new Label(status.toString());
                    switch (status) {
                        case ACCEPTED:
                            statusLabel.setStyle("-fx-background-color: #d4edda; -fx-text-fill: #155724; " +
                                    "-fx-padding: 8 16; -fx-background-radius: 15; -fx-font-weight: bold; -fx-font-size: 12;");
                            break;
                        case PENDING:
                            statusLabel.setStyle("-fx-background-color: #fff3cd; -fx-text-fill: #856404; " +
                                    "-fx-padding: 8 16; -fx-background-radius: 15; -fx-font-weight: bold; -fx-font-size: 12;");
                            break;
                        case REJECTED:
                            statusLabel.setStyle("-fx-background-color: #f8d7da; -fx-text-fill: #721c24; " +
                                    "-fx-padding: 8 16; -fx-background-radius: 15; -fx-font-weight: bold; -fx-font-size: 12;");
                            break;
                    }
                    setGraphic(statusLabel);
                }
            }
        });
        
        friendshipActionsCol.setCellFactory(param -> new TableCell<>() {
            private final Button viewBtn = new Button("👁️");
            private final Button editBtn = new Button("✏️");
            private final Button deleteBtn = new Button("🗑️");
            private final HBox pane = new HBox(6, viewBtn, editBtn, deleteBtn);
            
            {
                pane.setAlignment(Pos.CENTER);
                
                viewBtn.setStyle("-fx-background-color: #2196F3; " +
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 12; " +
                        "-fx-background-radius: 20; -fx-cursor: hand; -fx-font-size: 13; " +
                        "-fx-effect: dropshadow(gaussian, rgba(33,150,243,0.3), 8, 0, 0, 2);");
                
                editBtn.setStyle("-fx-background-color: #FF9800; " +
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 12; " +
                        "-fx-background-radius: 20; -fx-cursor: hand; -fx-font-size: 13; " +
                        "-fx-effect: dropshadow(gaussian, rgba(255,152,0,0.3), 8, 0, 0, 2);");
                
                deleteBtn.setStyle("-fx-background-color: #f44336; " +
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 12; " +
                        "-fx-background-radius: 20; -fx-cursor: hand; -fx-font-size: 13; " +
                        "-fx-effect: dropshadow(gaussian, rgba(244,67,54,0.3), 8, 0, 0, 2);");
                
                viewBtn.setOnAction(event -> {
                    Friendship friendship = getTableView().getItems().get(getIndex());
                    handleViewFriendship(friendship);
                });
                
                editBtn.setOnAction(event -> {
                    Friendship friendship = getTableView().getItems().get(getIndex());
                    handleEditFriendship(friendship);
                });
                
                deleteBtn.setOnAction(event -> {
                    Friendship friendship = getTableView().getItems().get(getIndex());
                    handleDeleteFriendshipInline(friendship);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
        
        friendshipTable.setItems(friendshipList);
    }

    private void setupBadgeTable() {
        badgeIdCol.setCellValueFactory(new PropertyValueFactory<>("badgeId"));
        badgeUserIdCol.setCellValueFactory(new PropertyValueFactory<>("userId"));
        badgeNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        descriptionCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        
        badgeActionsCol.setCellFactory(param -> new TableCell<>() {
            private final Button viewBtn = new Button("👁️");
            private final Button editBtn = new Button("✏️");
            private final Button deleteBtn = new Button("🗑️");
            private final HBox pane = new HBox(6, viewBtn, editBtn, deleteBtn);
            
            {
                pane.setAlignment(Pos.CENTER);
                
                viewBtn.setStyle("-fx-background-color: #2196F3; " +
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 12; " +
                        "-fx-background-radius: 20; -fx-cursor: hand; -fx-font-size: 13; " +
                        "-fx-effect: dropshadow(gaussian, rgba(33,150,243,0.3), 8, 0, 0, 2);");
                
                editBtn.setStyle("-fx-background-color: #FF9800; " +
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 12; " +
                        "-fx-background-radius: 20; -fx-cursor: hand; -fx-font-size: 13; " +
                        "-fx-effect: dropshadow(gaussian, rgba(255,152,0,0.3), 8, 0, 0, 2);");
                
                deleteBtn.setStyle("-fx-background-color: #f44336; " +
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 12; " +
                        "-fx-background-radius: 20; -fx-cursor: hand; -fx-font-size: 13; " +
                        "-fx-effect: dropshadow(gaussian, rgba(244,67,54,0.3), 8, 0, 0, 2);");
                
                viewBtn.setOnAction(event -> {
                    Badge badge = getTableView().getItems().get(getIndex());
                    handleViewBadge(badge);
                });
                
                editBtn.setOnAction(event -> {
                    Badge badge = getTableView().getItems().get(getIndex());
                    handleEditBadge(badge);
                });
                
                deleteBtn.setOnAction(event -> {
                    Badge badge = getTableView().getItems().get(getIndex());
                    handleDeleteBadgeInline(badge);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
        
        badgeTable.setItems(badgeList);
    }
    
    private void setupInstructorTable() {
        providerIdCol.setCellValueFactory(new PropertyValueFactory<>("providerId"));
        instructorUsernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        instructorEmailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        companyNameCol.setCellValueFactory(new PropertyValueFactory<>("companyName"));
        expertiseCol.setCellValueFactory(new PropertyValueFactory<>("expertise"));
        ratingCol.setCellValueFactory(new PropertyValueFactory<>("rating"));
        
        // Verified status column
        verifiedCol.setCellValueFactory(new PropertyValueFactory<>("verified"));
        verifiedCol.setCellFactory(param -> new TableCell<>() {
            @Override
            protected void updateItem(Boolean isVerified, boolean empty) {
                super.updateItem(isVerified, empty);
                if (empty || isVerified == null) {
                    setGraphic(null);
                } else {
                    Label statusLabel = new Label(isVerified ? "✅ Verified" : "⏳ Pending");
                    statusLabel.setStyle(isVerified ? 
                        "-fx-background-color: #e8f5e9; -fx-text-fill: #4CAF50; -fx-padding: 5 12; -fx-background-radius: 15; -fx-font-weight: bold; -fx-font-size: 12;" :
                        "-fx-background-color: #fff3e0; -fx-text-fill: #ff9800; -fx-padding: 5 12; -fx-background-radius: 15; -fx-font-weight: bold; -fx-font-size: 12;");
                    setGraphic(statusLabel);
                }
            }
        });
        
        // Actions column
        instructorActionsCol.setCellFactory(param -> new TableCell<>() {
            private final Button verifyBtn = new Button("✅ Verify");
            private final Button rejectBtn = new Button("❌ Reject");
            private final Button viewBtn = new Button("👁️ View");
            private final HBox pane = new HBox(6, viewBtn, verifyBtn, rejectBtn);
            
            {
                pane.setAlignment(Pos.CENTER);
                
                viewBtn.setStyle("-fx-background-color: #2196F3; " +
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 12; " +
                        "-fx-background-radius: 20; -fx-cursor: hand; -fx-font-size: 12; " +
                        "-fx-effect: dropshadow(gaussian, rgba(33,150,243,0.3), 8, 0, 0, 2);");
                
                verifyBtn.setStyle("-fx-background-color: #4CAF50; " +
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 12; " +
                        "-fx-background-radius: 20; -fx-cursor: hand; -fx-font-size: 12; " +
                        "-fx-effect: dropshadow(gaussian, rgba(76,175,80,0.3), 8, 0, 0, 2);");
                
                rejectBtn.setStyle("-fx-background-color: #f44336; " +
                        "-fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 8 12; " +
                        "-fx-background-radius: 20; -fx-cursor: hand; -fx-font-size: 12; " +
                        "-fx-effect: dropshadow(gaussian, rgba(244,67,54,0.3), 8, 0, 0, 2);");
                
                viewBtn.setOnAction(event -> {
                    ClassProvider provider = getTableView().getItems().get(getIndex());
                    handleViewInstructor(provider);
                });
                
                verifyBtn.setOnAction(event -> {
                    ClassProvider provider = getTableView().getItems().get(getIndex());
                    handleVerifyInstructor(provider);
                });
                
                rejectBtn.setOnAction(event -> {
                    ClassProvider provider = getTableView().getItems().get(getIndex());
                    handleRejectInstructor(provider);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    ClassProvider provider = getTableView().getItems().get(getIndex());
                    if (provider.isVerified()) {
                        // Only show view button for verified instructors
                        HBox verifiedPane = new HBox(6, viewBtn);
                        verifiedPane.setAlignment(Pos.CENTER);
                        setGraphic(verifiedPane);
                    } else {
                        // Show all buttons for pending instructors
                        setGraphic(pane);
                    }
                }
            }
        });
        
        instructorTable.setItems(instructorList);
    }

    private void loadAllData() {
        loadUsers();
        loadFriendships();
        loadBadges();
        loadInstructors();
    }

    private void loadUsers() {
        userList.clear();
        userList.addAll(userController.findAll());
        userCountLabel.setText(String.valueOf(userList.size()));
        filterUsers();
    }

    @FXML
    private void handleRefreshUser() {
        loadUsers();
    }

    private void loadFriendships() {
        friendshipList.clear();
        friendshipList.addAll(friendshipController.findAll());
        friendshipCountLabel.setText(String.valueOf(friendshipList.size()));
        filterFriendships();
    }

    @FXML
    private void handleRefreshFriendship() {
        loadFriendships();
    }

    private void loadBadges() {
        badgeList.clear();
        badgeList.addAll(badgeController.findAll());
        badgeCountLabel.setText(String.valueOf(badgeList.size()));
        filterBadges();
    }

    @FXML
    private void handleRefreshBadge() {
        loadBadges();
    }
    
    private void loadInstructors() {
        instructorList.clear();
        instructorList.addAll(providerController.getAll());
        
        // Count pending instructors
        long pendingCount = instructorList.stream()
                .filter(p -> !p.isVerified())
                .count();
        instructorCountLabel.setText(String.valueOf(pendingCount));
        
        filterInstructors();
    }

    @FXML
    private void handleRefreshInstructors() {
        loadInstructors();
    }
    
    private void handleViewInstructor(ClassProvider provider) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Instructor Details");
        alert.setHeaderText(provider.getUsername() + " - Instructor Application");
        
        VBox content = new VBox(12);
        content.setPadding(new Insets(15));
        content.setStyle("-fx-background-color: white;");
        
        content.getChildren().addAll(
            createDetailRow("ID:", String.valueOf(provider.getProviderId())),
            createDetailRow("Username:", provider.getUsername()),
            createDetailRow("Email:", provider.getEmail()),
            createDetailRow("Company/Studio:", provider.getCompanyName()),
            createDetailRow("Expertise:", provider.getExpertise()),
            createDetailRow("Rating:", String.format("⭐ %.1f", provider.getRating())),
            createDetailRow("Status:", provider.isVerified() ? "✅ Verified" : "⏳ Pending Verification")
        );
        
        alert.getDialogPane().setContent(content);
        alert.showAndWait();
    }
    
    private void handleVerifyInstructor(ClassProvider provider) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Verify Instructor");
        confirmation.setHeaderText("Verify this instructor application?");
        confirmation.setContentText("Username: " + provider.getUsername() + "\nCompany: " + provider.getCompanyName());
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = providerController.verifyProvider(provider.getProviderId(), true);
            if (success) {
                showStyledAlert("Success", "Instructor verified successfully!", Alert.AlertType.INFORMATION);
                loadInstructors();
            } else {
                showError("Failed to verify instructor");
            }
        }
    }
    
    private void handleRejectInstructor(ClassProvider provider) {
        Alert confirmation = new Alert(Alert.AlertType.WARNING);
        confirmation.setTitle("Reject Instructor");
        confirmation.setHeaderText("Reject this instructor application?");
        confirmation.setContentText("Username: " + provider.getUsername() + "\nCompany: " + provider.getCompanyName() + 
                                   "\n\nThis will remove their instructor status.");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = providerController.delete(provider.getProviderId());
            if (success) {
                showStyledAlert("Success", "Instructor application rejected and removed!", Alert.AlertType.INFORMATION);
                loadInstructors();
            } else {
                showError("Failed to reject instructor");
            }
        }
    }

    // ============= SEARCH FUNCTIONALITY =============
    
    private void setupSearchListeners() {
        // User search listener
        userSearchField.textProperty().addListener((observable, oldValue, newValue) -> filterUsers());
        
        // Friendship search listener
        friendshipSearchField.textProperty().addListener((observable, oldValue, newValue) -> filterFriendships());
        
        // Badge search listener
        badgeSearchField.textProperty().addListener((observable, oldValue, newValue) -> filterBadges());
        
        // Instructor search listener
        instructorSearchField.textProperty().addListener((observable, oldValue, newValue) -> filterInstructors());
    }
    
    private void filterUsers() {
        String searchText = userSearchField.getText().toLowerCase().trim();
        
        if (searchText.isEmpty()) {
            userTable.setItems(userList);
        } else {
            filteredUserList.clear();
            filteredUserList.addAll(userList.stream()
                .filter(user -> 
                    user.getUsername().toLowerCase().contains(searchText) ||
                    user.getEmail().toLowerCase().contains(searchText) ||
                    user.getFullName().toLowerCase().contains(searchText) ||
                    (user.getLocation() != null && user.getLocation().toLowerCase().contains(searchText)) ||
                    String.valueOf(user.getUserId()).contains(searchText)
                )
                .collect(Collectors.toList())
            );
            userTable.setItems(filteredUserList);
        }
    }
    
    private void filterFriendships() {
        String searchText = friendshipSearchField.getText().toLowerCase().trim();
        
        if (searchText.isEmpty()) {
            friendshipTable.setItems(friendshipList);
        } else {
            filteredFriendshipList.clear();
            filteredFriendshipList.addAll(friendshipList.stream()
                .filter(friendship -> 
                    String.valueOf(friendship.getFriendshipId()).contains(searchText) ||
                    String.valueOf(friendship.getUser1Id()).contains(searchText) ||
                    String.valueOf(friendship.getUser2Id()).contains(searchText) ||
                    friendship.getStatus().toString().toLowerCase().contains(searchText)
                )
                .collect(Collectors.toList())
            );
            friendshipTable.setItems(filteredFriendshipList);
        }
    }
    
    private void filterBadges() {
        String searchText = badgeSearchField.getText().toLowerCase().trim();
        
        if (searchText.isEmpty()) {
            badgeTable.setItems(badgeList);
        } else {
            filteredBadgeList.clear();
            filteredBadgeList.addAll(badgeList.stream()
                .filter(badge -> 
                    badge.getName().toLowerCase().contains(searchText) ||
                    badge.getDescription().toLowerCase().contains(searchText) ||
                    String.valueOf(badge.getBadgeId()).contains(searchText) ||
                    String.valueOf(badge.getUserId()).contains(searchText)
                )
                .collect(Collectors.toList())
            );
            badgeTable.setItems(filteredBadgeList);
        }
    }
    
    private void filterInstructors() {
        String searchText = instructorSearchField.getText().toLowerCase().trim();
        
        if (searchText.isEmpty()) {
            instructorTable.setItems(instructorList);
        } else {
            filteredInstructorList.clear();
            filteredInstructorList.addAll(instructorList.stream()
                .filter(provider -> 
                    provider.getUsername().toLowerCase().contains(searchText) ||
                    provider.getEmail().toLowerCase().contains(searchText) ||
                    provider.getCompanyName().toLowerCase().contains(searchText) ||
                    provider.getExpertise().toLowerCase().contains(searchText) ||
                    String.valueOf(provider.getProviderId()).contains(searchText)
                )
                .collect(Collectors.toList())
            );
            instructorTable.setItems(filteredInstructorList);
        }
    }
    
    private HBox createDetailRow(String label, String value) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.CENTER_LEFT);
        
        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-font-weight: bold; -fx-text-fill: #667eea; -fx-min-width: 120;");
        
        Label valueNode = new Label(value);
        valueNode.setStyle("-fx-text-fill: #1c1e21; -fx-font-size: 13;");
        valueNode.setWrapText(true);
        
        row.getChildren().addAll(labelNode, valueNode);
        return row;
    }
    
    // ============= PDF EXPORT FUNCTIONALITY =============
    
    @FXML
    private void handleExportUsersPDF() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export Users to PDF");
            fileChooser.setInitialFileName("Ghrami_Users_Report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            
            File file = fileChooser.showSaveDialog(userTable.getScene().getWindow());
            if (file != null) {
                exportUsersToPDF(file);
                showStyledAlert("Success", "Users exported successfully to:\n" + file.getAbsolutePath(), Alert.AlertType.INFORMATION);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showStyledAlert("Error", "Failed to export users: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    @FXML
    private void handleExportFriendshipsPDF() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export Friendships to PDF");
            fileChooser.setInitialFileName("Ghrami_Friendships_Report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            
            File file = fileChooser.showSaveDialog(friendshipTable.getScene().getWindow());
            if (file != null) {
                exportFriendshipsToPDF(file);
                showStyledAlert("Success", "Friendships exported successfully to:\n" + file.getAbsolutePath(), Alert.AlertType.INFORMATION);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showStyledAlert("Error", "Failed to export friendships: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    @FXML
    private void handleExportBadgesPDF() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Export Badges to PDF");
            fileChooser.setInitialFileName("Ghrami_Badges_Report_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".pdf");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
            
            File file = fileChooser.showSaveDialog(badgeTable.getScene().getWindow());
            if (file != null) {
                exportBadgesToPDF(file);
                showStyledAlert("Success", "Badges exported successfully to:\n" + file.getAbsolutePath(), Alert.AlertType.INFORMATION);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showStyledAlert("Error", "Failed to export badges: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }
    
    private void exportUsersToPDF(File file) throws Exception {
        PdfWriter writer = new PdfWriter(file);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        
        // Title
        Paragraph title = new Paragraph("Ghrami Platform - Users Report")
            .setFontSize(20)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER)
            .setFontColor(ColorConstants.BLUE);
        document.add(title);
        
        // Date
        Paragraph date = new Paragraph("Generated on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            .setFontSize(10)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20);
        document.add(date);
        
        // Summary
        Paragraph summary = new Paragraph("Total Users: " + userList.size())
            .setFontSize(12)
            .setBold()
            .setMarginBottom(15);
        document.add(summary);
        
        // Table
        float[] columnWidths = {1, 3, 4, 3, 2};
        Table table = new Table(UnitValue.createPercentArray(columnWidths));
        table.setWidth(UnitValue.createPercentValue(100));
        
        // Headers
        String[] headers = {"ID", "Username", "Email", "Location", "Status"};
        for (String header : headers) {
            Cell cell = new Cell().add(new Paragraph(header).setBold())
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setTextAlignment(TextAlignment.CENTER);
            table.addHeaderCell(cell);
        }
        
        // Data
        ObservableList<User> dataToExport = userTable.getItems();
        for (User user : dataToExport) {
            table.addCell(new Cell().add(new Paragraph(String.valueOf(user.getUserId()))));
            table.addCell(new Cell().add(new Paragraph(user.getUsername())));
            table.addCell(new Cell().add(new Paragraph(user.getEmail())));
            table.addCell(new Cell().add(new Paragraph(user.getLocation() != null ? user.getLocation() : "N/A")));
            table.addCell(new Cell().add(new Paragraph(user.isOnline() ? "Online" : "Offline")));
        }
        
        document.add(table);
        
        // Footer
        Paragraph footer = new Paragraph("© 2026 Ghrami by OPGG - Admin Report")
            .setFontSize(9)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(20)
            .setFontColor(ColorConstants.GRAY);
        document.add(footer);
        
        document.close();
    }
    
    private void exportFriendshipsToPDF(File file) throws Exception {
        PdfWriter writer = new PdfWriter(file);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        
        // Title
        Paragraph title = new Paragraph("Ghrami Platform - Friendships Report")
            .setFontSize(20)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER)
            .setFontColor(ColorConstants.BLUE);
        document.add(title);
        
        // Date
        Paragraph date = new Paragraph("Generated on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            .setFontSize(10)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20);
        document.add(date);
        
        // Summary
        Paragraph summary = new Paragraph("Total Friendships: " + friendshipList.size())
            .setFontSize(12)
            .setBold()
            .setMarginBottom(15);
        document.add(summary);
        
        // Table
        float[] columnWidths = {1, 2, 2, 2, 3};
        Table table = new Table(UnitValue.createPercentArray(columnWidths));
        table.setWidth(UnitValue.createPercentValue(100));
        
        // Headers
        String[] headers = {"ID", "User 1 ID", "User 2 ID", "Status", "Created Date"};
        for (String header : headers) {
            Cell cell = new Cell().add(new Paragraph(header).setBold())
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setTextAlignment(TextAlignment.CENTER);
            table.addHeaderCell(cell);
        }
        
        // Data
        ObservableList<Friendship> dataToExport = friendshipTable.getItems();
        for (Friendship friendship : dataToExport) {
            table.addCell(new Cell().add(new Paragraph(String.valueOf(friendship.getFriendshipId()))));
            table.addCell(new Cell().add(new Paragraph(String.valueOf(friendship.getUser1Id()))));
            table.addCell(new Cell().add(new Paragraph(String.valueOf(friendship.getUser2Id()))));
            table.addCell(new Cell().add(new Paragraph(friendship.getStatus().toString())));
            table.addCell(new Cell().add(new Paragraph(friendship.getCreatedDate() != null ? 
                friendship.getCreatedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "N/A")));
        }
        
        document.add(table);
        
        // Footer
        Paragraph footer = new Paragraph("© 2026 Ghrami by OPGG - Admin Report")
            .setFontSize(9)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(20)
            .setFontColor(ColorConstants.GRAY);
        document.add(footer);
        
        document.close();
    }
    
    private void exportBadgesToPDF(File file) throws Exception {
        PdfWriter writer = new PdfWriter(file);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);
        
        // Title
        Paragraph title = new Paragraph("Ghrami Platform - Badges Report")
            .setFontSize(20)
            .setBold()
            .setTextAlignment(TextAlignment.CENTER)
            .setFontColor(ColorConstants.BLUE);
        document.add(title);
        
        // Date
        Paragraph date = new Paragraph("Generated on: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
            .setFontSize(10)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginBottom(20);
        document.add(date);
        
        // Summary
        Paragraph summary = new Paragraph("Total Badges: " + badgeList.size())
            .setFontSize(12)
            .setBold()
            .setMarginBottom(15);
        document.add(summary);
        
        // Table
        float[] columnWidths = {1, 2, 3, 5, 3};
        Table table = new Table(UnitValue.createPercentArray(columnWidths));
        table.setWidth(UnitValue.createPercentValue(100));
        
        // Headers
        String[] headers = {"ID", "User ID", "Badge Name", "Description", "Earned Date"};
        for (String header : headers) {
            Cell cell = new Cell().add(new Paragraph(header).setBold())
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setTextAlignment(TextAlignment.CENTER);
            table.addHeaderCell(cell);
        }
        
        // Data
        ObservableList<Badge> dataToExport = badgeTable.getItems();
        for (Badge badge : dataToExport) {
            table.addCell(new Cell().add(new Paragraph(String.valueOf(badge.getBadgeId()))));
            table.addCell(new Cell().add(new Paragraph(String.valueOf(badge.getUserId()))));
            table.addCell(new Cell().add(new Paragraph(badge.getName())));
            table.addCell(new Cell().add(new Paragraph(badge.getDescription())));
            table.addCell(new Cell().add(new Paragraph(badge.getEarnedDate() != null ? 
                badge.getEarnedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")) : "N/A")));
        }
        
        document.add(table);
        
        // Footer
        Paragraph footer = new Paragraph("© 2026 Ghrami by OPGG - Admin Report")
            .setFontSize(9)
            .setTextAlignment(TextAlignment.CENTER)
            .setMarginTop(20)
            .setFontColor(ColorConstants.GRAY);
        document.add(footer);
        
        document.close();
    }

    // ============= BEAUTIFUL MODAL CREATION =============
    
    private Stage createBeautifulModal(String title, String icon) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.TRANSPARENT);
        dialog.setTitle(title);
        return dialog;
    }
    
    private VBox createModalContent(String title, String subtitle, String icon) {
        VBox content = new VBox(20);
        content.setStyle("-fx-background-color: white; -fx-background-radius: 24; " +
                "-fx-padding: 35; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 40, 0, 0, 10);");
        
        // Header
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        
        StackPane iconPane = new StackPane();
        Circle iconCircle = new Circle(28);
        iconCircle.setStyle("-fx-fill: linear-gradient(135deg, #667eea, #764ba2);");
        Label iconLabel = new Label(icon);
        iconLabel.setStyle("-fx-font-size: 30;");
        iconPane.getChildren().addAll(iconCircle, iconLabel);
        
        VBox titleBox = new VBox(3);
        Label titleLabel = new Label(title);
        titleLabel.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: #1c1e21;");
        Label subtitleLabel = new Label(subtitle);
        subtitleLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #65676b;");
        titleBox.getChildren().addAll(titleLabel, subtitleLabel);
        
        header.getChildren().addAll(iconPane, titleBox);
        content.getChildren().add(header);
        
        Separator separator = new Separator();
        separator.setStyle("-fx-background-color: #e4e6eb;");
        content.getChildren().add(separator);
        
        return content;
    }
    
    private HBox createModalButtons(Button primaryBtn, Button cancelBtn) {
        HBox buttonBox = new HBox(12);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        
        cancelBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-font-size: 13; -fx-padding: 12 28; " +
                "-fx-background-radius: 22; -fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(244,67,54,0.3), 10, 0, 0, 3);");
        
        primaryBtn.setStyle("-fx-background-color: linear-gradient(135deg, #667eea, #764ba2); " +
                "-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13; " +
                "-fx-padding: 12 28; -fx-background-radius: 22; -fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(102,126,234,0.4), 12, 0, 0, 4);");
        
        buttonBox.getChildren().addAll(cancelBtn, primaryBtn);
        return buttonBox;
    }

    // ============= USER CRUD WITH BEAUTIFUL MODALS =============
    
    @FXML
    private void handleAddUser() {
        Stage dialog = createBeautifulModal("Add User", "👤");
        VBox modalContent = createModalContent("Create New User", "Add a new user to the platform", "👤");
        
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(10, 0, 20, 0));

        TextField usernameField = createStyledTextField("Username");
        TextField fullNameField = createStyledTextField("Full Name");
        TextField emailField = createStyledTextField("Email");
        PasswordField passwordField = createStyledPasswordField("Password");
        TextField locationField = createStyledTextField("Location");
        TextArea bioArea = createStyledTextArea("Bio");
        TextField profilePicField = createStyledTextField("Profile Picture URL");

        grid.add(createFieldLabel("Username"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(createFieldLabel("Full Name"), 0, 1);
        grid.add(fullNameField, 1, 1);
        grid.add(createFieldLabel("Email"), 0, 2);
        grid.add(emailField, 1, 2);
        grid.add(createFieldLabel("Password"), 0, 3);
        grid.add(passwordField, 1, 3);
        grid.add(createFieldLabel("Location"), 0, 4);
        grid.add(locationField, 1, 4);
        grid.add(createFieldLabel("Bio"), 0, 5);
        grid.add(bioArea, 1, 5);
        grid.add(createFieldLabel("Profile Picture"), 0, 6);
        grid.add(profilePicField, 1, 6);

        Button createBtn = new Button("✨ Create User");
        Button cancelBtn = new Button("Cancel");
        
        cancelBtn.setOnAction(e -> dialog.close());
        createBtn.setOnAction(e -> {
            String validationError = validateUserInput(usernameField.getText(), emailField.getText(), passwordField.getText());
            if (validationError != null) {
                showStyledAlert("❌ Validation Error", validationError, Alert.AlertType.ERROR);
                return;
            }

            if (emailExists(emailField.getText())) {
                showStyledAlert("❌ Error", "This email is already in use", Alert.AlertType.ERROR);
                return;
            }

            if (usernameExists(usernameField.getText())) {
                showStyledAlert("❌ Error", "This username is already taken", Alert.AlertType.ERROR);
                return;
            }

            User user = new User(
                usernameField.getText().trim(),
                emailField.getText().trim().toLowerCase(),
                PasswordUtil.hashPassword(passwordField.getText()),
                profilePicField.getText().trim(),
                bioArea.getText().trim(),
                locationField.getText().trim()
            );
            user.setFullName(fullNameField.getText().trim().isEmpty() ? 
                usernameField.getText().trim() : fullNameField.getText().trim());
            user.setOnline(false);
            
            userController.create(user);
            loadUsers();
            dialog.close();
            showStyledAlert("✅ Success", "User created successfully!", Alert.AlertType.INFORMATION);
        });

        HBox buttonBox = createModalButtons(createBtn, cancelBtn);
        
        modalContent.getChildren().addAll(grid, buttonBox);
        
        StackPane root = new StackPane(modalContent);
        root.setStyle("-fx-background-color: transparent;");
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
    
    private void handleEditUser(User user) {
        Stage dialog = createBeautifulModal("Edit User", "✏️");
        VBox modalContent = createModalContent("Edit User", "Update user information", "✏️");
        
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(10, 0, 20, 0));

        TextField usernameField = createStyledTextField("Username");
        usernameField.setText(user.getUsername());
        TextField fullNameField = createStyledTextField("Full Name");
        fullNameField.setText(user.getFullName());
        TextField emailField = createStyledTextField("Email");
        emailField.setText(user.getEmail());
        PasswordField passwordField = createStyledPasswordField("Leave empty to keep current");
        TextField locationField = createStyledTextField("Location");
        locationField.setText(user.getLocation());
        TextArea bioArea = createStyledTextArea("Bio");
        bioArea.setText(user.getBio());
        TextField profilePicField = createStyledTextField("Profile Picture URL");
        profilePicField.setText(user.getProfilePicture());
        CheckBox onlineCheckBox = new CheckBox("Online");
        onlineCheckBox.setSelected(user.isOnline());
        onlineCheckBox.setStyle("-fx-font-weight: bold;");

        grid.add(createFieldLabel("Username"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(createFieldLabel("Full Name"), 0, 1);
        grid.add(fullNameField, 1, 1);
        grid.add(createFieldLabel("Email"), 0, 2);
        grid.add(emailField, 1, 2);
        grid.add(createFieldLabel("Password"), 0, 3);
        grid.add(passwordField, 1, 3);
        grid.add(createFieldLabel("Location"), 0, 4);
        grid.add(locationField, 1, 4);
        grid.add(createFieldLabel("Bio"), 0, 5);
        grid.add(bioArea, 1, 5);
        grid.add(createFieldLabel("Profile Picture"), 0, 6);
        grid.add(profilePicField, 1, 6);
        grid.add(createFieldLabel("Status"), 0, 7);
        grid.add(onlineCheckBox, 1, 7);

        Button updateBtn = new Button("💾 Update User");
        Button cancelBtn = new Button("Cancel");
        
        cancelBtn.setOnAction(e -> dialog.close());
        updateBtn.setOnAction(e -> {
            String validationError = validateUserInput(usernameField.getText(), emailField.getText(), null);
            if (validationError != null && !validationError.contains("password")) {
                showStyledAlert("❌ Validation Error", validationError, Alert.AlertType.ERROR);
                return;
            }

            if (emailExistsForOtherUser(emailField.getText(), user.getUserId())) {
                showStyledAlert("❌ Error", "This email is already in use", Alert.AlertType.ERROR);
                return;
            }

            if (usernameExistsForOtherUser(usernameField.getText(), user.getUserId())) {
                showStyledAlert("❌ Error", "This username is already taken", Alert.AlertType.ERROR);
                return;
            }

            user.setUsername(usernameField.getText().trim());
            user.setFullName(fullNameField.getText().trim());
            user.setEmail(emailField.getText().trim().toLowerCase());
            
            if (!passwordField.getText().isEmpty()) {
                user.setPassword(PasswordUtil.hashPassword(passwordField.getText()));
            }
            
            user.setProfilePicture(profilePicField.getText().trim());
            user.setBio(bioArea.getText().trim());
            user.setLocation(locationField.getText().trim());
            user.setOnline(onlineCheckBox.isSelected());
            
            userController.update(user);
            loadUsers();
            dialog.close();
            showStyledAlert("✅ Success", "User updated successfully!", Alert.AlertType.INFORMATION);
        });

        HBox buttonBox = createModalButtons(updateBtn, cancelBtn);
        modalContent.getChildren().addAll(grid, buttonBox);
        
        StackPane root = new StackPane(modalContent);
        root.setStyle("-fx-background-color: transparent;");
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
    
    private void handleDeleteUserInline(User user) {
        showStyledConfirmation("🗑️ Delete User", 
            "Are you sure you want to delete user: " + user.getUsername() + "?",
            "This action cannot be undone.",
            () -> {
                userController.delete(user.getUserId());
                loadUsers();
                showStyledAlert("✅ Success", "User deleted successfully!", Alert.AlertType.INFORMATION);
            });
    }

    // ============= FRIENDSHIP CRUD WITH BEAUTIFUL MODALS =============
    
    @FXML
    private void handleAddFriendship() {
        Stage dialog = createBeautifulModal("Add Friendship", "🤝");
        VBox modalContent = createModalContent("Create Friendship", "Connect two users", "🤝");
        
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(10, 0, 20, 0));

        TextField user1Field = createStyledTextField("User 1 ID");
        TextField user2Field = createStyledTextField("User 2 ID");
        ComboBox<FriendshipStatus> statusComboBox = new ComboBox<>();
        statusComboBox.setItems(FXCollections.observableArrayList(FriendshipStatus.values()));
        statusComboBox.setValue(FriendshipStatus.PENDING);
        statusComboBox.setStyle("-fx-background-color: white; -fx-border-color: #e4e6eb; " +
                "-fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 10; " +
                "-fx-font-size: 13; -fx-pref-width: 300;");

        grid.add(createFieldLabel("User 1 ID"), 0, 0);
        grid.add(user1Field, 1, 0);
        grid.add(createFieldLabel("User 2 ID"), 0, 1);
        grid.add(user2Field, 1, 1);
        grid.add(createFieldLabel("Status"), 0, 2);
        grid.add(statusComboBox, 1, 2);

        Button createBtn = new Button("✨ Create Connection");
        Button cancelBtn = new Button("Cancel");
        
        cancelBtn.setOnAction(e -> dialog.close());
        createBtn.setOnAction(e -> {
            try {
                Long user1Id = Long.parseLong(user1Field.getText().trim());
                Long user2Id = Long.parseLong(user2Field.getText().trim());

                if (user1Id.equals(user2Id)) {
                    showStyledAlert("❌ Error", "A user cannot be friends with themselves", Alert.AlertType.ERROR);
                    return;
                }

                if (!userController.findById(user1Id).isPresent()) {
                    showStyledAlert("❌ Error", "User with ID " + user1Id + " does not exist", Alert.AlertType.ERROR);
                    return;
                }

                if (!userController.findById(user2Id).isPresent()) {
                    showStyledAlert("❌ Error", "User with ID " + user2Id + " does not exist", Alert.AlertType.ERROR);
                    return;
                }

                if (friendshipExists(user1Id, user2Id)) {
                    showStyledAlert("❌ Error", "Friendship already exists between these users", Alert.AlertType.ERROR);
                    return;
                }

                Friendship friendship = new Friendship(user1Id, user2Id);
                friendship.setStatus(statusComboBox.getValue());
                
                friendshipController.create(friendship);
                loadFriendships();
                dialog.close();
                showStyledAlert("✅ Success", "Friendship created successfully!", Alert.AlertType.INFORMATION);
            } catch (NumberFormatException ex) {
                showStyledAlert("❌ Error", "Invalid user IDs. Please enter numbers", Alert.AlertType.ERROR);
            }
        });

        HBox buttonBox = createModalButtons(createBtn, cancelBtn);
        modalContent.getChildren().addAll(grid, buttonBox);
        
        StackPane root = new StackPane(modalContent);
        root.setStyle("-fx-background-color: transparent;");
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
    
    private void handleEditFriendship(Friendship friendship) {
        Stage dialog = createBeautifulModal("Edit Friendship", "✏️");
        VBox modalContent = createModalContent("Edit Friendship", "Update friendship status", "✏️");
        
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(10, 0, 20, 0));

        Label user1Label = new Label(String.valueOf(friendship.getUser1Id()));
        user1Label.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");
        Label user2Label = new Label(String.valueOf(friendship.getUser2Id()));
        user2Label.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");
        ComboBox<FriendshipStatus> statusComboBox = new ComboBox<>();
        statusComboBox.setItems(FXCollections.observableArrayList(FriendshipStatus.values()));
        statusComboBox.setValue(friendship.getStatus());
        statusComboBox.setStyle("-fx-background-color: white; -fx-border-color: #e4e6eb; " +
                "-fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 10; " +
                "-fx-font-size: 13; -fx-pref-width: 300;");

        grid.add(createFieldLabel("User 1 ID"), 0, 0);
        grid.add(user1Label, 1, 0);
        grid.add(createFieldLabel("User 2 ID"), 0, 1);
        grid.add(user2Label, 1, 1);
        grid.add(createFieldLabel("Status"), 0, 2);
        grid.add(statusComboBox, 1, 2);

        Button updateBtn = new Button("💾 Update");
        Button cancelBtn = new Button("Cancel");
        
        cancelBtn.setOnAction(e -> dialog.close());
        updateBtn.setOnAction(e -> {
            friendship.setStatus(statusComboBox.getValue());
            friendshipController.update(friendship);
            loadFriendships();
            dialog.close();
            showStyledAlert("✅ Success", "Friendship updated successfully!", Alert.AlertType.INFORMATION);
        });

        HBox buttonBox = createModalButtons(updateBtn, cancelBtn);
        modalContent.getChildren().addAll(grid, buttonBox);
        
        StackPane root = new StackPane(modalContent);
        root.setStyle("-fx-background-color: transparent;");
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
    
    private void handleDeleteFriendshipInline(Friendship friendship) {
        showStyledConfirmation("🗑️ Delete Friendship", 
            "Are you sure you want to delete this friendship connection?",
            "This action cannot be undone.",
            () -> {
                friendshipController.delete(friendship.getFriendshipId());
                loadFriendships();
                showStyledAlert("✅ Success", "Friendship deleted successfully!", Alert.AlertType.INFORMATION);
            });
    }

    // ============= BADGE CRUD WITH BEAUTIFUL MODALS =============
    
    @FXML
    private void handleAddBadge() {
        Stage dialog = createBeautifulModal("Award Badge", "🏆");
        VBox modalContent = createModalContent("Award Badge", "Give recognition to a user", "🏆");
        
        // Main container
        VBox mainBox = new VBox(20);
        mainBox.setPadding(new Insets(10, 0, 20, 0));
        
        // Section 1: User Selection
        VBox userSection = new VBox(10);
        Label userLabel = createFieldLabel("Select User");
        
        HBox userSelectionBox = new HBox(10);
        TextField userSearchField = createStyledTextField("Search by username or email...");
        userSearchField.setPrefWidth(250);
        
        ComboBox<String> userComboBox = new ComboBox<>();
        userComboBox.setPromptText("Select User");
        userComboBox.setPrefWidth(250);
        userComboBox.setStyle("-fx-background-color: white; -fx-border-color: #e4e6eb; " +
                "-fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 10; -fx-font-size: 13;");
        
        // Populate users
        ObservableList<String> userOptions = FXCollections.observableArrayList();
        for (User user : userList) {
            userOptions.add(user.getUserId() + " - " + user.getUsername() + " (" + user.getEmail() + ")");
        }
        userComboBox.setItems(userOptions);
        
        // Search functionality
        userSearchField.textProperty().addListener((obs, old, newVal) -> {
            if (newVal == null || newVal.trim().isEmpty()) {
                userComboBox.setItems(userOptions);
            } else {
                ObservableList<String> filtered = FXCollections.observableArrayList(
                    userOptions.stream()
                        .filter(u -> u.toLowerCase().contains(newVal.toLowerCase()))
                        .collect(Collectors.toList())
                );
                userComboBox.setItems(filtered);
            }
        });
        
        userSelectionBox.getChildren().addAll(userSearchField, userComboBox);
        userSection.getChildren().addAll(userLabel, userSelectionBox);
        
        // Section 2: Badge Templates
        VBox templateSection = new VBox(10);
        Label templateLabel = createFieldLabel("Badge Template (Optional)");
        
        ComboBox<String> templateComboBox = new ComboBox<>();
        templateComboBox.setPromptText("Select a template or create custom");
        templateComboBox.setPrefWidth(520);
        templateComboBox.setStyle("-fx-background-color: white; -fx-border-color: #e4e6eb; " +
                "-fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 10; -fx-font-size: 13;");
        
        // Predefined badge templates
        ObservableList<String> templates = FXCollections.observableArrayList(
            "🥇 First Friend - Made their first friend on Ghrami",
            "🎉 Welcome Aboard - Successfully registered on Ghrami",
            "💬 Social Butterfly - Created 10+ connections",
            "📝 Content Creator - Posted 50+ times",
            "👑 VIP Member - Premium subscriber",
            "🌟 Rising Star - Highly engaged user",
            "🔥 On Fire - 7 day login streak",
            "🎯 Goal Achiever - Completed personal goals",
            "🤝 Friend Connector - Helped connect 20+ people",
            "💎 Diamond Member - 1 year anniversary",
            "🎨 Creative Mind - Shared unique content",
            "🏆 Champion - Won community challenge",
            "⭐ Super Supporter - Helped community members",
            "🚀 Early Adopter - Beta tester badge",
            "--- Custom Badge ---"
        );
        templateComboBox.setItems(templates);
        templateSection.getChildren().addAll(templateLabel, templateComboBox);
        
        // Section 3: Badge Details
        VBox detailsSection = new VBox(10);
        TextField nameField = createStyledTextField("Badge Name");
        TextArea descArea = createStyledTextArea("Description");
        nameField.setPrefWidth(520);
        descArea.setPrefWidth(520);
        
        // Auto-fill from template
        templateComboBox.setOnAction(e -> {
            String selected = templateComboBox.getValue();
            if (selected != null && !selected.equals("--- Custom Badge ---")) {
                String[] parts = selected.split(" - ", 2);
                nameField.setText(parts[0]);
                if (parts.length > 1) {
                    descArea.setText(parts[1]);
                }
            } else {
                nameField.clear();
                descArea.clear();
            }
        });
        
        detailsSection.getChildren().addAll(
            createFieldLabel("Badge Name"),
            nameField,
            createFieldLabel("Description"),
            descArea
        );

        mainBox.getChildren().addAll(userSection, templateSection, detailsSection);
        
        Button createBtn = new Button("✨ Award Badge");
        Button cancelBtn = new Button("Cancel");
        
        cancelBtn.setOnAction(e -> dialog.close());
        createBtn.setOnAction(e -> {
            try {
                if (userComboBox.getValue() == null) {
                    showStyledAlert("❌ Error", "Please select a user", Alert.AlertType.ERROR);
                    return;
                }
                
                // Extract user ID
                String userSelection = userComboBox.getValue();
                Long userId = Long.parseLong(userSelection.split(" - ")[0]);
                
                if (nameField.getText().trim().isEmpty()) {
                    showStyledAlert("❌ Error", "Badge name is required", Alert.AlertType.ERROR);
                    return;
                }

                // Check if user already has this badge
                if (badgeController.userHasBadge(userId, nameField.getText().trim())) {
                    showStyledAlert("❌ Error", "User already has this badge!", Alert.AlertType.WARNING);
                    return;
                }

                Badge badge = new Badge();
                badge.setUserId(userId);
                badge.setName(nameField.getText().trim());
                badge.setDescription(descArea.getText().trim());
                
                Badge created = badgeController.create(badge);
                if (created != null) {
                    String username = userController.findById(userId).get().getUsername();
                    loadBadges();
                    dialog.close();
                    
                    // Show beautiful notification
                    BadgeNotificationUtil.showBadgeAwardNotification(created, username);
                    
                    // Also show a success message
                    showStyledAlert("✅ Success", 
                        "Badge '" + badge.getName() + "' awarded to " + username + "!", 
                        Alert.AlertType.INFORMATION);
                } else {
                    showStyledAlert("❌ Error", "Failed to create badge", Alert.AlertType.ERROR);
                }
            } catch (Exception ex) {
                showStyledAlert("❌ Error", "Error awarding badge: " + ex.getMessage(), Alert.AlertType.ERROR);
                ex.printStackTrace();
            }
        });

        HBox buttonBox = createModalButtons(createBtn, cancelBtn);
        modalContent.getChildren().addAll(mainBox, buttonBox);
        
        StackPane root = new StackPane(modalContent);
        root.setStyle("-fx-background-color: transparent;");
        Scene scene = new Scene(root, 600, 700);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
    
    private void handleEditBadge(Badge badge) {
        Stage dialog = createBeautifulModal("Edit Badge", "✏️");
        VBox modalContent = createModalContent("Edit Badge", "Update badge details", "✏️");
        
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(15);
        grid.setPadding(new Insets(10, 0, 20, 0));

        Label userIdLabel = new Label(String.valueOf(badge.getUserId()));
        userIdLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");
        TextField nameField = createStyledTextField("Badge Name");
        nameField.setText(badge.getName());
        TextArea descArea = createStyledTextArea("Description");
        descArea.setText(badge.getDescription());

        grid.add(createFieldLabel("User ID"), 0, 0);
        grid.add(userIdLabel, 1, 0);
        grid.add(createFieldLabel("Badge Name"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(createFieldLabel("Description"), 0, 2);
        grid.add(descArea, 1, 2);

        Button updateBtn = new Button("💾 Update Badge");
        Button cancelBtn = new Button("Cancel");
        
        cancelBtn.setOnAction(e -> dialog.close());
        updateBtn.setOnAction(e -> {
            if (nameField.getText().trim().isEmpty()) {
                showStyledAlert("❌ Error", "Badge name is required", Alert.AlertType.ERROR);
                return;
            }
            
            badge.setName(nameField.getText().trim());
            badge.setDescription(descArea.getText().trim());
            
            badgeController.update(badge);
            loadBadges();
            dialog.close();
            showStyledAlert("✅ Success", "Badge updated successfully!", Alert.AlertType.INFORMATION);
        });

        HBox buttonBox = createModalButtons(updateBtn, cancelBtn);
        modalContent.getChildren().addAll(grid, buttonBox);
        
        StackPane root = new StackPane(modalContent);
        root.setStyle("-fx-background-color: transparent;");
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
    
    private void handleDeleteBadgeInline(Badge badge) {
        showStyledConfirmation("🗑️ Delete Badge", 
            "Are you sure you want to delete the badge: " + badge.getName() + "?",
            "This action cannot be undone.",
            () -> {
                badgeController.delete(badge.getBadgeId());
                loadBadges();
                showStyledAlert("✅ Success", "Badge deleted successfully!", Alert.AlertType.INFORMATION);
            });
    }
    
    // ============= VIEW METHODS =============
    
    private void handleViewUser(User user) {
        // Fetch fresh data
        Optional<User> userOpt = userController.findById(user.getUserId());
        if (!userOpt.isPresent()) {
            showStyledAlert("❌ Error", "User not found", Alert.AlertType.ERROR);
            return;
        }
        
        User freshUser = userOpt.get();
        
        Stage dialog = createBeautifulModal("View User", "👁️");
        VBox modalContent = createModalContent("User Details", "View complete user information", "👁️");
        
        VBox detailsBox = new VBox(15);
        detailsBox.setStyle("-fx-padding: 10 0 20 0;");
        
        // Add profile picture if available
        if (freshUser.getProfilePicture() != null && !freshUser.getProfilePicture().trim().isEmpty()) {
            try {
                Image profileImage = loadProfileImage(freshUser.getProfilePicture());
                if (profileImage != null && !profileImage.isError()) {
                    ImageView profileImg = new ImageView(profileImage);
                    profileImg.setFitWidth(100);
                    profileImg.setFitHeight(100);
                    profileImg.setPreserveRatio(true);
                    Circle clip = new Circle(50);
                    clip.setCenterX(50);
                    clip.setCenterY(50);
                    profileImg.setClip(clip);
                    
                    StackPane imgContainer = new StackPane(profileImg);
                    imgContainer.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 15, 0, 0, 5);");
                    detailsBox.getChildren().add(imgContainer);
                }
            } catch (Exception e) {
                // Skip if image fails to load
            }
        }
        
        detailsBox.getChildren().addAll(
                createDetailRow("ID:", String.valueOf(freshUser.getUserId())),
                createDetailRow("Username:", freshUser.getUsername()),
                createDetailRow("Full Name:", freshUser.getFullName() != null ? freshUser.getFullName() : "N/A"),
                createDetailRow("Email:", freshUser.getEmail()),
                createDetailRow("Location:", freshUser.getLocation() != null ? freshUser.getLocation() : "N/A"),
                createDetailRow("Bio:", freshUser.getBio() != null ? freshUser.getBio() : "N/A"),
                createDetailRow("Status:", freshUser.isOnline() ? "🟢 Online" : "🔴 Offline"),
                createDetailRow("Created At:", freshUser.getCreatedAt() != null ? freshUser.getCreatedAt().toString() : "N/A"),
                createDetailRow("Last Login:", freshUser.getLastLogin() != null ? freshUser.getLastLogin().toString() : "N/A")
        );
        
        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-font-size: 13; -fx-padding: 12 32; " +
                "-fx-background-radius: 22; -fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(33,150,243,0.4), 12, 0, 0, 4);");
        closeBtn.setOnAction(e -> dialog.close());
        
        HBox buttonBox = new HBox(closeBtn);
        buttonBox.setAlignment(Pos.CENTER);
        
        modalContent.getChildren().addAll(detailsBox, buttonBox);
        
        StackPane root = new StackPane(modalContent);
        root.setStyle("-fx-background-color: transparent;");
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
    
    private void handleViewFriendship(Friendship friendship) {
        // Fetch fresh data
        Optional<Friendship> friendshipOpt = friendshipController.findById(friendship.getFriendshipId());
        if (!friendshipOpt.isPresent()) {
            showStyledAlert("❌ Error", "Friendship not found", Alert.AlertType.ERROR);
            return;
        }
        
        Friendship freshFriendship = friendshipOpt.get();
        
        // Get user details
        Optional<User> user1Opt = userController.findById(freshFriendship.getUser1Id());
        Optional<User> user2Opt = userController.findById(freshFriendship.getUser2Id());
        
        Stage dialog = createBeautifulModal("View Friendship", "👁️");
        VBox modalContent = createModalContent("Friendship Details", "View connection information", "👁️");
        
        VBox detailsBox = new VBox(15);
        detailsBox.setStyle("-fx-padding: 10 0 20 0;");
        
        detailsBox.getChildren().addAll(
                createDetailRow("Friendship ID:", String.valueOf(freshFriendship.getFriendshipId())),
                createDetailRow("User 1 ID:", String.valueOf(freshFriendship.getUser1Id())),
                createDetailRow("User 1 Name:", user1Opt.isPresent() ? user1Opt.get().getUsername() : "Unknown"),
                createDetailRow("User 2 ID:", String.valueOf(freshFriendship.getUser2Id())),
                createDetailRow("User 2 Name:", user2Opt.isPresent() ? user2Opt.get().getUsername() : "Unknown"),
                createDetailRow("Status:", freshFriendship.getStatus().toString()),
                createDetailRow("Created Date:", freshFriendship.getCreatedDate() != null ? freshFriendship.getCreatedDate().toString() : "N/A"),
                createDetailRow("Accepted Date:", freshFriendship.getAcceptedDate() != null ? freshFriendship.getAcceptedDate().toString() : "N/A")
        );
        
        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-font-size: 13; -fx-padding: 12 32; " +
                "-fx-background-radius: 22; -fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(33,150,243,0.4), 12, 0, 0, 4);");
        closeBtn.setOnAction(e -> dialog.close());
        
        HBox buttonBox = new HBox(closeBtn);
        buttonBox.setAlignment(Pos.CENTER);
        
        modalContent.getChildren().addAll(detailsBox, buttonBox);
        
        StackPane root = new StackPane(modalContent);
        root.setStyle("-fx-background-color: transparent;");
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
    
    private void handleViewBadge(Badge badge) {
        // Fetch fresh data
        Optional<Badge> badgeOpt = badgeController.findById(badge.getBadgeId());
        if (!badgeOpt.isPresent()) {
            showStyledAlert("❌ Error", "Badge not found", Alert.AlertType.ERROR);
            return;
        }
        
        Badge freshBadge = badgeOpt.get();
        
        // Get user details
        Optional<User> userOpt = userController.findById(freshBadge.getUserId());
        
        Stage dialog = createBeautifulModal("View Badge", "👁️");
        VBox modalContent = createModalContent("Badge Details", "View achievement information", "👁️");
        
        VBox detailsBox = new VBox(15);
        detailsBox.setStyle("-fx-padding: 10 0 20 0;");
        
        detailsBox.getChildren().addAll(
                createDetailRow("Badge ID:", String.valueOf(freshBadge.getBadgeId())),
                createDetailRow("User ID:", String.valueOf(freshBadge.getUserId())),
                createDetailRow("Username:", userOpt.isPresent() ? userOpt.get().getUsername() : "Unknown"),
                createDetailRow("Badge Name:", freshBadge.getName()),
                createDetailRow("Description:", freshBadge.getDescription() != null ? freshBadge.getDescription() : "N/A"),
                createDetailRow("Earned Date:", freshBadge.getEarnedDate() != null ? freshBadge.getEarnedDate().toString() : "N/A")
        );
        
        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; " +
                "-fx-font-weight: bold; -fx-font-size: 13; -fx-padding: 12 32; " +
                "-fx-background-radius: 22; -fx-cursor: hand; " +
                "-fx-effect: dropshadow(gaussian, rgba(33,150,243,0.4), 12, 0, 0, 4);");
        closeBtn.setOnAction(e -> dialog.close());
        
        HBox buttonBox = new HBox(closeBtn);
        buttonBox.setAlignment(Pos.CENTER);
        
        modalContent.getChildren().addAll(detailsBox, buttonBox);
        
        StackPane root = new StackPane(modalContent);
        root.setStyle("-fx-background-color: transparent;");
        Scene scene = new Scene(root);
        scene.setFill(Color.TRANSPARENT);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
    
    // ============= BEAUTIFUL UI HELPER METHODS =============
    
    private TextField createStyledTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setStyle("-fx-background-color: white; -fx-border-color: #e4e6eb; " +
                "-fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 10; " +
                "-fx-font-size: 13; -fx-pref-width: 300;");
        return field;
    }
    
    private PasswordField createStyledPasswordField(String prompt) {
        PasswordField field = new PasswordField();
        field.setPromptText(prompt);
        field.setStyle("-fx-background-color: white; -fx-border-color: #e4e6eb; " +
                "-fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 10; " +
                "-fx-font-size: 13; -fx-pref-width: 300;");
        return field;
    }
    
    private TextArea createStyledTextArea(String prompt) {
        TextArea area = new TextArea();
        area.setPromptText(prompt);
        area.setPrefRowCount(3);
        area.setStyle("-fx-background-color: white; -fx-border-color: #e4e6eb; " +
                "-fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 10; " +
                "-fx-font-size: 13; -fx-pref-width: 300;");
        return area;
    }
    
    private Label createFieldLabel(String text) {
        Label label = new Label(text + ":");
        label.setStyle("-fx-font-weight: bold; -fx-text-fill: #1c1e21; -fx-font-size: 13;");
        return label;
    }
    
    
    private Image loadProfileImage(String picUrl) {
        if (picUrl == null || picUrl.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Case 1: If it's a full URL (http/https), load directly
            if (picUrl.startsWith("http://") || picUrl.startsWith("https://")) {
                return new Image(picUrl, 50, 50, true, true, true);
            }
            
            // Case 2: If it's a file path starting with file://
            if (picUrl.startsWith("file://")) {
                return new Image(picUrl, 50, 50, true, true, true);
            }
            
            // Case 3: Try to load from resources folder (images/profile_pictures/)
            String resourcePath = "/images/profile_pictures/" + picUrl;
            URL resourceUrl = getClass().getResource(resourcePath);
            if (resourceUrl != null) {
                return new Image(resourceUrl.toExternalForm(), 50, 50, true, true, true);
            }
            
            // Case 4: Try just /images/
            resourcePath = "/images/" + picUrl;
            resourceUrl = getClass().getResource(resourcePath);
            if (resourceUrl != null) {
                return new Image(resourceUrl.toExternalForm(), 50, 50, true, true, true);
            }
            
            // Case 5: Try as absolute file path
            java.io.File file = new java.io.File(picUrl);
            if (file.exists()) {
                return new Image(file.toURI().toString(), 50, 50, true, true, true);
            }
            
            // If nothing works, return null (will show default avatar)
            return null;
        } catch (Exception e) {
            System.err.println("Failed to load profile image: " + picUrl + " - " + e.getMessage());
            return null;
        }
    }
    
    private StackPane createDefaultAvatar() {
        StackPane avatar = new StackPane();
        Circle circle = new Circle(25);
        circle.setStyle("-fx-fill: linear-gradient(135deg, #667eea, #764ba2);");
        Label icon = new Label("👤");
        icon.setStyle("-fx-font-size: 28;");
        avatar.getChildren().addAll(circle, icon);
        avatar.setStyle("-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 10, 0, 0, 3);");
        return avatar;
    }
    
    private void showStyledAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        DialogPane dialogPane = alert.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white; -fx-background-radius: 20; " +
                "-fx-padding: 25; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 30, 0, 0, 10);");
        
        alert.showAndWait();
    }
    
    private void showStyledConfirmation(String title, String message, String detail, Runnable onConfirm) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle(title);
        confirmation.setHeaderText(message);
        confirmation.setContentText(detail);
        
        DialogPane dialogPane = confirmation.getDialogPane();
        dialogPane.setStyle("-fx-background-color: white; -fx-background-radius: 20; " +
                "-fx-padding: 25; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 30, 0, 0, 10);");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            onConfirm.run();
        }
    }
    
    private void showError(String message) {
        showStyledAlert("❌ Error", message, Alert.AlertType.ERROR);
    }

    // ============= VALIDATION METHODS =============
    
    private String validateUserInput(String username, String email, String password) {
        if (username == null || username.trim().isEmpty()) {
            return "Le nom d'utilisateur est obligatoire";
        }
        username = username.trim();
        if (username.length() < 3) {
            return "Le nom d'utilisateur doit contenir au moins 3 caractères";
        }
        if (username.length() > 50) {
            return "Le nom d'utilisateur ne peut pas dépasser 50 caractères";
        }
        if (!username.matches("^[a-zA-Z0-9_-]+$")) {
            return "Le nom d'utilisateur ne peut contenir que des lettres, chiffres, tirets et underscores";
        }

        if (email == null || email.trim().isEmpty()) {
            return "L'email est obligatoire";
        }
        email = email.trim();
        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            return "Format d'email invalide (exemple: utilisateur@ghrami.tn)";
        }
        if (email.length() > 100) {
            return "L'email ne peut pas dépasser 100 caractères";
        }

        if (password != null && !password.isEmpty()) {
            if (password.length() < 6) {
                return "Le mot de passe doit contenir au moins 6 caractères";
            }
            if (password.length() > 255) {
                return "Le mot de passe ne peut pas dépasser 255 caractères";
            }
        }

        return null;
    }

    private boolean emailExists(String email) {
        return userList.stream()
                .anyMatch(u -> u.getEmail().equalsIgnoreCase(email.trim()));
    }

    private boolean usernameExists(String username) {
        return userList.stream()
                .anyMatch(u -> u.getUsername().equalsIgnoreCase(username.trim()));
    }

    private boolean emailExistsForOtherUser(String email, Long currentUserId) {
        return userList.stream()
                .anyMatch(u -> !u.getUserId().equals(currentUserId) && 
                              u.getEmail().equalsIgnoreCase(email.trim()));
    }

    private boolean usernameExistsForOtherUser(String username, Long currentUserId) {
        return userList.stream()
                .anyMatch(u -> !u.getUserId().equals(currentUserId) && 
                              u.getUsername().equalsIgnoreCase(username.trim()));
    }

    private boolean friendshipExists(Long user1Id, Long user2Id) {
        return friendshipList.stream()
                .anyMatch(f -> (f.getUser1Id().equals(user1Id) && f.getUser2Id().equals(user2Id)) ||
                              (f.getUser1Id().equals(user2Id) && f.getUser2Id().equals(user1Id)));
    }
}
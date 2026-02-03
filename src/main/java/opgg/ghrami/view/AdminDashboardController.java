package opgg.ghrami.view;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import opgg.ghrami.controller.BadgeController;
import opgg.ghrami.controller.FriendshipController;
import opgg.ghrami.controller.UserController;
import opgg.ghrami.model.Badge;
import opgg.ghrami.model.Friendship;
import opgg.ghrami.model.FriendshipStatus;
import opgg.ghrami.model.User;
import opgg.ghrami.util.PasswordUtil;
import opgg.ghrami.util.SessionManager;

import java.net.URL;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.ResourceBundle;

public class AdminDashboardController implements Initializable {
    
    @FXML private Label adminNameLabel;
    @FXML private Button logoutButton;

    // User Tab
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Long> userIdCol;
    @FXML private TableColumn<User, String> usernameCol;
    @FXML private TableColumn<User, String> emailCol;
    @FXML private TableColumn<User, String> locationCol;
    @FXML private TableColumn<User, Boolean> onlineCol;
    
    @FXML private TextField usernameField;
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private TextField locationField;
    @FXML private TextArea bioArea;
    @FXML private TextField profilePicField;
    @FXML private CheckBox onlineCheckBox;

    // Friendship Tab
    @FXML private TableView<Friendship> friendshipTable;
    @FXML private TableColumn<Friendship, Long> friendshipIdCol;
    @FXML private TableColumn<Friendship, Long> user1IdCol;
    @FXML private TableColumn<Friendship, Long> user2IdCol;
    @FXML private TableColumn<Friendship, FriendshipStatus> statusCol;
    
    @FXML private TextField friendUser1Field;
    @FXML private TextField friendUser2Field;
    @FXML private ComboBox<FriendshipStatus> statusComboBox;

    // Badge Tab
    @FXML private TableView<Badge> badgeTable;
    @FXML private TableColumn<Badge, Long> badgeIdCol;
    @FXML private TableColumn<Badge, Long> badgeUserIdCol;
    @FXML private TableColumn<Badge, String> badgeNameCol;
    @FXML private TableColumn<Badge, String> descriptionCol;
    
    @FXML private TextField badgeUserIdField;
    @FXML private TextField badgeNameField;
    @FXML private TextArea badgeDescArea;

    private UserController userController;
    private FriendshipController friendshipController;
    private BadgeController badgeController;
    
    private ObservableList<User> userList;
    private ObservableList<Friendship> friendshipList;
    private ObservableList<Badge> badgeList;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Initialize controllers
        userController = new UserController();
        friendshipController = new FriendshipController();
        badgeController = new BadgeController();
        
        // Set admin name from session
        SessionManager session = SessionManager.getInstance();
        adminNameLabel.setText("Admin: " + session.getUsername());
        
        // Initialize lists
        userList = FXCollections.observableArrayList();
        friendshipList = FXCollections.observableArrayList();
        badgeList = FXCollections.observableArrayList();
        
        // Setup User Table
        setupUserTable();
        
        // Setup Friendship Table
        setupFriendshipTable();
        
        // Setup Badge Table
        setupBadgeTable();
        
        // Load data
        loadAllData();
    }
    
    @FXML
    private void handleLogout() {
        try {
            // Get current admin user and set offline
            long adminId = SessionManager.getInstance().getUserId();
            Optional<User> adminOpt = userController.findById(adminId);
            if (adminOpt.isPresent()) {
                User admin = adminOpt.get();
                admin.setOnline(false);
                userController.update(admin);
            }
            
            // Clear session
            SessionManager.getInstance().logout();
            
            // Navigate to login
            Stage stage = (Stage) logoutButton.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/LoginView.fxml"));
            Scene scene = new Scene(loader.load(), 450, 600);
            stage.setScene(scene);
            stage.setTitle("Ghrami - Connexion");
        } catch (Exception e) {
            showError("Erreur lors de la déconnexion: " + e.getMessage());
        }
    }

    private void setupUserTable() {
        userIdCol.setCellValueFactory(new PropertyValueFactory<>("userId"));
        usernameCol.setCellValueFactory(new PropertyValueFactory<>("username"));
        emailCol.setCellValueFactory(new PropertyValueFactory<>("email"));
        locationCol.setCellValueFactory(new PropertyValueFactory<>("location"));
        onlineCol.setCellValueFactory(new PropertyValueFactory<>("online"));
        
        userTable.setItems(userList);
        
        // Selection listener
        userTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                fillUserFields(newSelection);
            }
        });
    }

    private void setupFriendshipTable() {
        friendshipIdCol.setCellValueFactory(new PropertyValueFactory<>("friendshipId"));
        user1IdCol.setCellValueFactory(new PropertyValueFactory<>("user1Id"));
        user2IdCol.setCellValueFactory(new PropertyValueFactory<>("user2Id"));
        statusCol.setCellValueFactory(new PropertyValueFactory<>("status"));
        
        friendshipTable.setItems(friendshipList);
        
        // Setup status ComboBox
        statusComboBox.setItems(FXCollections.observableArrayList(FriendshipStatus.values()));
        statusComboBox.setValue(FriendshipStatus.PENDING);
        
        // Selection listener
        friendshipTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                fillFriendshipFields(newSelection);
            }
        });
    }

    private void setupBadgeTable() {
        badgeIdCol.setCellValueFactory(new PropertyValueFactory<>("badgeId"));
        badgeUserIdCol.setCellValueFactory(new PropertyValueFactory<>("userId"));
        badgeNameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        descriptionCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        
        badgeTable.setItems(badgeList);
        
        // Selection listener
        badgeTable.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                fillBadgeFields(newSelection);
            }
        });
    }

    private void loadAllData() {
        loadUsers();
        loadFriendships();
        loadBadges();
    }

    private void loadUsers() {
        userList.clear();
        userList.addAll(userController.findAll());
    }

    @FXML
    private void handleRefreshUser() {
        loadUsers();
    }

    private void loadFriendships() {
        friendshipList.clear();
        friendshipList.addAll(friendshipController.findAll());
    }

    @FXML
    private void handleRefreshFriendship() {
        loadFriendships();
    }

    private void loadBadges() {
        badgeList.clear();
        badgeList.addAll(badgeController.findAll());
    }

    @FXML
    private void handleRefreshBadge() {
        loadBadges();
    }

    // User Actions
    @FXML
    private void handleAddUser() {
        // Validation des champs obligatoires
        String validationError = validateUserInput(usernameField.getText(), emailField.getText(), passwordField.getText());
        if (validationError != null) {
            showAlert("Erreur de validation", validationError);
            return;
        }

        // Vérifier si l'email existe déjà
        if (emailExists(emailField.getText())) {
            showAlert("Erreur", "Cet email est déjà utilisé par un autre utilisateur");
            return;
        }

        // Vérifier si le nom d'utilisateur existe déjà
        if (usernameExists(usernameField.getText())) {
            showAlert("Erreur", "Ce nom d'utilisateur est déjà pris");
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
        user.setOnline(onlineCheckBox.isSelected());
        
        userController.create(user);
        loadUsers();
        clearUserFields();
        showInfo("Succès", "Utilisateur créé avec succès");
    }

    @FXML
    private void handleUpdateUser() {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert("Erreur", "Veuillez sélectionner un utilisateur");
            return;
        }

        // Validation des champs obligatoires
        String validationError = validateUserInput(usernameField.getText(), emailField.getText(), passwordField.getText());
        if (validationError != null) {
            showAlert("Erreur de validation", validationError);
            return;
        }

        // Vérifier si l'email existe déjà (sauf pour l'utilisateur actuel)
        if (emailExistsForOtherUser(emailField.getText(), selectedUser.getUserId())) {
            showAlert("Erreur", "Cet email est déjà utilisé par un autre utilisateur");
            return;
        }

        // Vérifier si le nom d'utilisateur existe déjà (sauf pour l'utilisateur actuel)
        if (usernameExistsForOtherUser(usernameField.getText(), selectedUser.getUserId())) {
            showAlert("Erreur", "Ce nom d'utilisateur est déjà pris");
            return;
        }

        selectedUser.setUsername(usernameField.getText().trim());
        selectedUser.setFullName(fullNameField.getText().trim().isEmpty() ? 
            usernameField.getText().trim() : fullNameField.getText().trim());
        selectedUser.setEmail(emailField.getText().trim().toLowerCase());
        
        // Only update password if field is not empty and hash it
        String newPassword = passwordField.getText();
        if (newPassword != null && !newPassword.trim().isEmpty()) {
            selectedUser.setPassword(PasswordUtil.hashPassword(newPassword));
        }
        
        selectedUser.setProfilePicture(profilePicField.getText().trim());
        selectedUser.setBio(bioArea.getText().trim());
        selectedUser.setLocation(locationField.getText().trim());
        selectedUser.setOnline(onlineCheckBox.isSelected());
        
        userController.update(selectedUser);
        loadUsers();
        clearUserFields();
        showInfo("Succès", "Utilisateur modifié avec succès");
    }

    @FXML
    private void handleDeleteUser() {
        User selectedUser = userTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showAlert("Erreur", "Veuillez sélectionner un utilisateur");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer l'utilisateur");
        confirmation.setContentText("Êtes-vous sûr de vouloir supprimer cet utilisateur ?");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            userController.delete(selectedUser.getUserId());
            loadUsers();
            clearUserFields();
            showInfo("Succès", "Utilisateur supprimé avec succès");
        }
    }

    @FXML
    private void handleClearUser() {
        clearUserFields();
        userTable.getSelectionModel().clearSelection();
    }

    // Friendship Actions
    @FXML
    private void handleAddFriendship() {
        if (friendUser1Field.getText().isEmpty() || friendUser2Field.getText().isEmpty()) {
            showAlert("Erreur", "Veuillez remplir les IDs des deux utilisateurs");
            return;
        }

        try {
            Long user1Id = Long.parseLong(friendUser1Field.getText().trim());
            Long user2Id = Long.parseLong(friendUser2Field.getText().trim());
            
            // Validation: les deux utilisateurs doivent être différents
            if (user1Id.equals(user2Id)) {
                showAlert("Erreur", "Un utilisateur ne peut pas être ami avec lui-même");
                return;
            }

            // Vérifier que les deux utilisateurs existent
            Optional<User> user1Opt = userController.findById(user1Id);
            Optional<User> user2Opt = userController.findById(user2Id);
            
            if (!user1Opt.isPresent()) {
                showAlert("Erreur", "L'utilisateur avec l'ID " + user1Id + " n'existe pas");
                return;
            }
            
            if (!user2Opt.isPresent()) {
                showAlert("Erreur", "L'utilisateur avec l'ID " + user2Id + " n'existe pas");
                return;
            }

            // Vérifier si une relation existe déjà
            if (friendshipExists(user1Id, user2Id)) {
                showAlert("Erreur", "Une relation existe déjà entre ces deux utilisateurs");
                return;
            }
            
            Friendship friendship = new Friendship(user1Id, user2Id);
            friendship.setStatus(statusComboBox.getValue());
            
            friendshipController.create(friendship);
            loadFriendships();
            clearFriendshipFields();
            showInfo("Succès", "Demande d'amitié créée avec succès");
        } catch (NumberFormatException e) {
            showAlert("Erreur", "IDs invalides. Veuillez entrer des nombres entiers");
        }
    }

    @FXML
    private void handleUpdateFriendship() {
        Friendship selectedFriendship = friendshipTable.getSelectionModel().getSelectedItem();
        if (selectedFriendship == null) {
            showAlert("Erreur", "Veuillez sélectionner une relation");
            return;
        }

        try {
            Long user1Id = Long.parseLong(friendUser1Field.getText().trim());
            Long user2Id = Long.parseLong(friendUser2Field.getText().trim());
            
            // Validation: les deux utilisateurs doivent être différents
            if (user1Id.equals(user2Id)) {
                showAlert("Erreur", "Un utilisateur ne peut pas être ami avec lui-même");
                return;
            }

            // Vérifier que les deux utilisateurs existent
            Optional<User> user1Opt = userController.findById(user1Id);
            Optional<User> user2Opt = userController.findById(user2Id);
            
            if (!user1Opt.isPresent()) {
                showAlert("Erreur", "L'utilisateur avec l'ID " + user1Id + " n'existe pas");
                return;
            }
            
            if (!user2Opt.isPresent()) {
                showAlert("Erreur", "L'utilisateur avec l'ID " + user2Id + " n'existe pas");
                return;
            }
            
            selectedFriendship.setUser1Id(user1Id);
            selectedFriendship.setUser2Id(user2Id);
            selectedFriendship.setStatus(statusComboBox.getValue());
            
            if (statusComboBox.getValue() == FriendshipStatus.ACCEPTED) {
                selectedFriendship.setAcceptedDate(LocalDateTime.now());
            }
            
            friendshipController.update(selectedFriendship);
            loadFriendships();
            clearFriendshipFields();
            showInfo("Succès", "Relation modifiée avec succès");
        } catch (NumberFormatException e) {
            showAlert("Erreur", "IDs invalides. Veuillez entrer des nombres entiers");
        }
    }

    @FXML
    private void handleDeleteFriendship() {
        Friendship selectedFriendship = friendshipTable.getSelectionModel().getSelectedItem();
        if (selectedFriendship == null) {
            showAlert("Erreur", "Veuillez sélectionner une relation");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer la relation");
        confirmation.setContentText("Êtes-vous sûr de vouloir supprimer cette relation ?");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            friendshipController.delete(selectedFriendship.getFriendshipId());
            loadFriendships();
            clearFriendshipFields();
            showInfo("Succès", "Relation supprimée avec succès");
        }
    }

    @FXML
    private void handleClearFriendship() {
        clearFriendshipFields();
        friendshipTable.getSelectionModel().clearSelection();
    }

    // Badge Actions
    @FXML
    private void handleAddBadge() {
        if (badgeUserIdField.getText().isEmpty() || badgeNameField.getText().isEmpty()) {
            showAlert("Erreur", "Veuillez remplir tous les champs obligatoires");
            return;
        }

        // Validation du nom du badge
        String badgeName = badgeNameField.getText().trim();
        if (badgeName.length() < 3) {
            showAlert("Erreur", "Le nom du badge doit contenir au moins 3 caractères");
            return;
        }

        if (badgeName.length() > 100) {
            showAlert("Erreur", "Le nom du badge ne peut pas dépasser 100 caractères");
            return;
        }

        try {
            Long userId = Long.parseLong(badgeUserIdField.getText().trim());
            
            // Vérifier que l'utilisateur existe
            Optional<User> userOpt = userController.findById(userId);
            if (!userOpt.isPresent()) {
                showAlert("Erreur", "L'utilisateur avec l'ID " + userId + " n'existe pas");
                return;
            }
            
            Badge badge = new Badge(userId, badgeName, badgeDescArea.getText().trim());
            
            badgeController.create(badge);
            loadBadges();
            clearBadgeFields();
            showInfo("Succès", "Badge créé avec succès");
        } catch (NumberFormatException e) {
            showAlert("Erreur", "ID utilisateur invalide. Veuillez entrer un nombre entier");
        }
    }

    @FXML
    private void handleUpdateBadge() {
        Badge selectedBadge = badgeTable.getSelectionModel().getSelectedItem();
        if (selectedBadge == null) {
            showAlert("Erreur", "Veuillez sélectionner un badge");
            return;
        }

        // Validation du nom du badge
        String badgeName = badgeNameField.getText().trim();
        if (badgeName.isEmpty()) {
            showAlert("Erreur", "Le nom du badge est obligatoire");
            return;
        }

        if (badgeName.length() < 3) {
            showAlert("Erreur", "Le nom du badge doit contenir au moins 3 caractères");
            return;
        }

        if (badgeName.length() > 100) {
            showAlert("Erreur", "Le nom du badge ne peut pas dépasser 100 caractères");
            return;
        }

        try {
            Long userId = Long.parseLong(badgeUserIdField.getText().trim());
            
            // Vérifier que l'utilisateur existe
            Optional<User> userOpt = userController.findById(userId);
            if (!userOpt.isPresent()) {
                showAlert("Erreur", "L'utilisateur avec l'ID " + userId + " n'existe pas");
                return;
            }
            
            selectedBadge.setUserId(userId);
            selectedBadge.setName(badgeName);
            selectedBadge.setDescription(badgeDescArea.getText().trim());
            
            badgeController.update(selectedBadge);
            loadBadges();
            clearBadgeFields();
            showInfo("Succès", "Badge modifié avec succès");
        } catch (NumberFormatException e) {
            showAlert("Erreur", "ID utilisateur invalide. Veuillez entrer un nombre entier");
        }
    }

    @FXML
    private void handleDeleteBadge() {
        Badge selectedBadge = badgeTable.getSelectionModel().getSelectedItem();
        if (selectedBadge == null) {
            showAlert("Erreur", "Veuillez sélectionner un badge");
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer le badge");
        confirmation.setContentText("Êtes-vous sûr de vouloir supprimer ce badge ?");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            badgeController.delete(selectedBadge.getBadgeId());
            loadBadges();
            clearBadgeFields();
            showInfo("Succès", "Badge supprimé avec succès");
        }
    }

    @FXML
    private void handleClearBadge() {
        clearBadgeFields();
        badgeTable.getSelectionModel().clearSelection();
    }

    // Helper methods
    private void fillUserFields(User user) {
        usernameField.setText(user.getUsername());
        fullNameField.setText(user.getFullName() != null ? user.getFullName() : "");
        emailField.setText(user.getEmail());
        passwordField.setText(user.getPassword());
        locationField.setText(user.getLocation());
        bioArea.setText(user.getBio());
        profilePicField.setText(user.getProfilePicture());
        onlineCheckBox.setSelected(user.isOnline());
    }

    private void clearUserFields() {
        usernameField.clear();
        fullNameField.clear();
        emailField.clear();
        passwordField.clear();
        locationField.clear();
        bioArea.clear();
        profilePicField.clear();
        onlineCheckBox.setSelected(false);
    }

    private void fillFriendshipFields(Friendship friendship) {
        friendUser1Field.setText(String.valueOf(friendship.getUser1Id()));
        friendUser2Field.setText(String.valueOf(friendship.getUser2Id()));
        statusComboBox.setValue(friendship.getStatus());
    }

    private void clearFriendshipFields() {
        friendUser1Field.clear();
        friendUser2Field.clear();
        statusComboBox.setValue(FriendshipStatus.PENDING);
    }

    private void fillBadgeFields(Badge badge) {
        badgeUserIdField.setText(String.valueOf(badge.getUserId()));
        badgeNameField.setText(badge.getName());
        badgeDescArea.setText(badge.getDescription());
    }

    private void clearBadgeFields() {
        badgeUserIdField.clear();
        badgeNameField.clear();
        badgeDescArea.clear();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Validation Methods
    private String validateUserInput(String username, String email, String password) {
        // Validation du nom d'utilisateur
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

        // Validation de l'email
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

        // Validation du mot de passe
        if (password == null || password.isEmpty()) {
            return "Le mot de passe est obligatoire";
        }
        if (password.length() < 6) {
            return "Le mot de passe doit contenir au moins 6 caractères";
        }
        if (password.length() > 255) {
            return "Le mot de passe ne peut pas dépasser 255 caractères";
        }

        return null; // Pas d'erreur
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

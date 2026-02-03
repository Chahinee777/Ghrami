package opgg.ghrami.view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import opgg.ghrami.controller.UserController;
import opgg.ghrami.model.User;
import opgg.ghrami.util.PasswordUtil;

public class RegisterViewController {

    @FXML private TextField usernameField;
    @FXML private TextField fullNameField;
    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private TextField locationField;
    @FXML private TextArea bioArea;
    @FXML private Label errorLabel;
    @FXML private Button registerButton;
    @FXML private Hyperlink loginLink;

    private UserController userController;

    @FXML
    public void initialize() {
        userController = new UserController();
        errorLabel.setVisible(false);
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String fullName = fullNameField.getText().trim();
        String email = emailField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String location = locationField.getText().trim();
        String bio = bioArea.getText().trim();

        // Validation
        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs obligatoires");
            return;
        }

        if (username.length() < 3) {
            showError("Le nom d'utilisateur doit contenir au moins 3 caractères");
            return;
        }

        if (!email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
            showError("Format d'email invalide");
            return;
        }

        if (password.length() < 6) {
            showError("Le mot de passe doit contenir au moins 6 caractères");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Les mots de passe ne correspondent pas");
            return;
        }

        // Check if email already exists
        boolean emailExists = userController.findAll().stream()
                .anyMatch(u -> u.getEmail().equalsIgnoreCase(email));
        
        if (emailExists) {
            showError("Cet email est déjà utilisé");
            return;
        }

        // Check if username already exists
        boolean usernameExists = userController.findAll().stream()
                .anyMatch(u -> u.getUsername().equalsIgnoreCase(username));
        
        if (usernameExists) {
            showError("Ce nom d'utilisateur est déjà pris");
            return;
        }

        // Create user
        User newUser = new User(username, email, PasswordUtil.hashPassword(password), "", bio, location);
        newUser.setFullName(fullName.isEmpty() ? username : fullName);
        newUser.setOnline(false);
        
        userController.create(newUser);

        // Show success and redirect to login
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText("Compte créé avec succès ! Vous pouvez maintenant vous connecter.");
        alert.showAndWait();

        handleBackToLogin();
    }

    @FXML
    private void handleBackToLogin() {
        try {
            Stage stage = (Stage) registerButton.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/LoginView.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Ghrami - Connexion");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}

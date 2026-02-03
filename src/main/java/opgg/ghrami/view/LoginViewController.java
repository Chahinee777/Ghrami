package opgg.ghrami.view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import opgg.ghrami.controller.UserController;
import opgg.ghrami.model.User;
import opgg.ghrami.util.JWTUtil;
import opgg.ghrami.util.PasswordUtil;
import opgg.ghrami.util.SessionManager;

import java.util.Optional;

public class LoginViewController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    @FXML private Hyperlink registerLink;

    private UserController userController;

    @FXML
    public void initialize() {
        userController = new UserController();
        errorLabel.setVisible(false);
    }

    @FXML
    private void handleLogin() {
        String email = emailField.getText().trim();
        String password = passwordField.getText();

        // Validation
        if (email.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs");
            return;
        }

        // Find user by email using optimized query
        Optional<User> userOpt = userController.findByEmail(email);

        if (!userOpt.isPresent()) {
            showError("Email ou mot de passe incorrect");
            return;
        }

        User user = userOpt.get();

        // Check password using BCrypt
        if (!PasswordUtil.checkPassword(password, user.getPassword())) {
            showError("Email ou mot de passe incorrect");
            return;
        }

        // Generate JWT token
        boolean isAdmin = user.getUserId() == 0;
        String token = JWTUtil.generateToken(user.getUserId(), user.getUsername(), user.getEmail(), isAdmin);

        // Store session
        SessionManager.getInstance().login(token);

        // Update user online status
        user.setOnline(true);
        userController.update(user);

        // Navigate to appropriate dashboard
        try {
            Stage stage = (Stage) loginButton.getScene().getWindow();
            
            if (isAdmin) {
                openAdminDashboard(stage);
            } else {
                openUserDashboard(stage);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur lors de la connexion");
        }
    }

    @FXML
    private void handleRegister() {
        try {
            Stage stage = (Stage) registerLink.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/RegisterView.fxml"));
            Scene scene = new Scene(loader.load());
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Ghrami - Inscription");
        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur lors de l'ouverture de la page d'inscription");
        }
    }

    private void openAdminDashboard(Stage stage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/AdminDashboard.fxml"));
        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("Ghrami - Administration");
        stage.setWidth(1400);
        stage.setHeight(900);
        stage.setMinWidth(1200);
        stage.setMinHeight(700);
        stage.setResizable(true);
        stage.centerOnScreen();
    }

    private void openUserDashboard(Stage stage) throws Exception {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/UserFeed.fxml"));
        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(getClass().getResource("/css/social-style.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("Ghrami - Mon Feed");
        stage.setWidth(1400);
        stage.setHeight(900);
        stage.setMinWidth(1200);
        stage.setMinHeight(700);
        stage.setResizable(true);
        stage.centerOnScreen();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}

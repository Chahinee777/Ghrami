package opgg.ghrami.view;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import opgg.ghrami.controller.PasswordResetController;
import opgg.ghrami.controller.UserController;
import opgg.ghrami.model.GoogleUserInfo;
import opgg.ghrami.model.User;
import opgg.ghrami.util.GoogleAuthService;
import opgg.ghrami.util.JWTUtil;
import opgg.ghrami.util.SessionManager;
import opgg.ghrami.util.PasswordUtil;

import java.util.Optional;

public class LoginViewController {

    @FXML private TextField emailField;
    @FXML private PasswordField passwordField;
    @FXML private Label errorLabel;
    @FXML private Button loginButton;
    @FXML private Button googleLoginButton;
    @FXML private Hyperlink registerLink;
    @FXML private Hyperlink forgotPasswordLink;

    private UserController userController;
    private PasswordResetController passwordResetController;

    @FXML
    public void initialize() {
        userController = new UserController();
        passwordResetController = PasswordResetController.getInstance();
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

        // Check if user is banned
        if (user.isBanned()) {
            showError("Votre compte a été suspendu. Veuillez contacter l'administrateur.");
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
            double width = stage.getWidth();
            double height = stage.getHeight();
            boolean wasMaximized = stage.isMaximized();
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/RegisterView.fxml"));
            Scene scene = new Scene(loader.load(), width, height);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Ghrami - Inscription");
            if (wasMaximized) {
                stage.setMaximized(true);
            }
        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur lors de l'ouverture de la page d'inscription");
        }
    }

    private void openAdminDashboard(Stage stage) throws Exception {
        double width = stage.getWidth();
        double height = stage.getHeight();
        boolean wasMaximized = stage.isMaximized();
        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/AdminDashboard.fxml"));
        Scene scene = new Scene(loader.load(), width, height);
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("Ghrami - Administration");
        stage.setMinWidth(1200);
        stage.setMinHeight(700);
        stage.setResizable(true);
        if (wasMaximized) {
            stage.setMaximized(true);
        }
    }

    private void openUserDashboard(Stage stage) throws Exception {
        double width = stage.getWidth();
        double height = stage.getHeight();
        boolean wasMaximized = stage.isMaximized();
        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/UserFeed.fxml"));
        Scene scene = new Scene(loader.load(), width, height);
        scene.getStylesheets().add(getClass().getResource("/css/social-style.css").toExternalForm());
        stage.setScene(scene);
        stage.setTitle("Ghrami - Mon Feed");
        stage.setMinWidth(1200);
        stage.setMinHeight(700);
        stage.setResizable(true);
        if (wasMaximized) {
            stage.setMaximized(true);
        }
    }

    @FXML
    private void handleForgotPassword() {
        // Step 1: Get email from user
        TextInputDialog emailDialog = new TextInputDialog();
        emailDialog.setTitle("Mot de passe oublié");
        emailDialog.setHeaderText("🔐 Réinitialisation de mot de passe");
        emailDialog.setContentText("Entrez votre email:");
        
        Optional<String> emailResult = emailDialog.showAndWait();
        if (!emailResult.isPresent() || emailResult.get().trim().isEmpty()) {
            return;
        }
        
        String email = emailResult.get().trim();
        
        // Validate email format
        if (!email.contains("@") || !email.contains(".")) {
            showError("Format d'email invalide");
            return;
        }
        
        // Show loading alert
        Alert loadingAlert = new Alert(Alert.AlertType.INFORMATION);
        loadingAlert.setTitle("Envoi en cours");
        loadingAlert.setHeaderText("📧 Envoi du code de réinitialisation...");
        loadingAlert.setContentText("Veuillez patienter...");
        loadingAlert.show();
        
        // Initiate password reset in background thread
        new Thread(() -> {
            boolean success = passwordResetController.initiatePasswordReset(email);
            
            // Update UI on JavaFX thread
            javafx.application.Platform.runLater(() -> {
                loadingAlert.close();
                
                if (success) {
                    // Step 2: Show code entry dialog
                    showResetCodeDialog(email);
                } else {
                    Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                    errorAlert.setTitle("Erreur");
                    errorAlert.setHeaderText("❌ Réinitialisation impossible");
                    errorAlert.setContentText("L'email n'existe pas dans notre système ou l'envoi a échoué.\n\nVérifiez :\n• L'email est bien enregistré\n• La configuration Brevo (EmailService.java)\n• Votre connexion Internet");
                    errorAlert.showAndWait();
                }
            });
        }).start();
    }
    
    private void showResetCodeDialog(String email) {
        // Create custom dialog for code and new password
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Réinitialisation de mot de passe");
        dialog.setHeaderText("📬 Code envoyé à " + email + "\n⏱️ Valide pendant 15 minutes");
        
        // Create form fields
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        TextField codeField = new TextField();
        codeField.setPromptText("123456");
        codeField.setPrefWidth(300);
        
        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Nouveau mot de passe (8+ caractères)");
        newPasswordField.setPrefWidth(300);
        
        PasswordField confirmPasswordField = new PasswordField();
        confirmPasswordField.setPromptText("Confirmer le mot de passe");
        confirmPasswordField.setPrefWidth(300);
        
        Label infoLabel = new Label("💡 Vérifiez votre boîte email pour le code à 6 chiffres");
        infoLabel.setStyle("-fx-text-fill: #667eea; -fx-font-weight: bold;");
        
        grid.add(infoLabel, 0, 0, 2, 1);
        grid.add(new Label("Code de réinitialisation:"), 0, 1);
        grid.add(codeField, 1, 1);
        grid.add(new Label("Nouveau mot de passe:"), 0, 2);
        grid.add(newPasswordField, 1, 2);
        grid.add(new Label("Confirmer le mot de passe:"), 0, 3);
        grid.add(confirmPasswordField, 1, 3);
        
        dialog.getDialogPane().setContent(grid);
        
        // Add buttons
        ButtonType resetButtonType = new ButtonType("Réinitialiser", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelButtonType = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(resetButtonType, cancelButtonType);
        
        // Handle reset button click
        Optional<ButtonType> result = dialog.showAndWait();
        
        if (result.isPresent() && result.get() == resetButtonType) {
            String code = codeField.getText().trim();
            String newPassword = newPasswordField.getText();
            String confirmPassword = confirmPasswordField.getText();
            
            // Validation
            if (code.isEmpty() || newPassword.isEmpty()) {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Erreur");
                errorAlert.setHeaderText("Champs requis");
                errorAlert.setContentText("Veuillez remplir tous les champs");
                errorAlert.showAndWait();
                showResetCodeDialog(email); // Show again
                return;
            }
            
            if (code.length() != 6 || !code.matches("\\d+")) {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Erreur");
                errorAlert.setHeaderText("Code invalide");
                errorAlert.setContentText("Le code doit contenir exactement 6 chiffres");
                errorAlert.showAndWait();
                showResetCodeDialog(email); // Show again
                return;
            }
            
            if (newPassword.length() < 8) {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Erreur");
                errorAlert.setHeaderText("Mot de passe trop court");
                errorAlert.setContentText("Le mot de passe doit contenir au moins 8 caractères");
                errorAlert.showAndWait();
                showResetCodeDialog(email); // Show again
                return;
            }
            
            if (!newPassword.equals(confirmPassword)) {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Erreur");
                errorAlert.setHeaderText("Mots de passe différents");
                errorAlert.setContentText("Les mots de passe ne correspondent pas");
                errorAlert.showAndWait();
                showResetCodeDialog(email); // Show again
                return;
            }
            
            // Attempt password reset
            boolean success = passwordResetController.resetPassword(email, code, newPassword);
            
            if (success) {
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Succès");
                successAlert.setHeaderText("✅ Mot de passe réinitialisé");
                successAlert.setContentText("Votre mot de passe a été changé avec succès!\nVous pouvez maintenant vous connecter.");
                successAlert.showAndWait();
            } else {
                Alert errorAlert = new Alert(Alert.AlertType.ERROR);
                errorAlert.setTitle("Erreur");
                errorAlert.setHeaderText("❌ Réinitialisation échouée");
                errorAlert.setContentText("Code invalide, expiré ou déjà utilisé.\nVeuillez réessayer ou demander un nouveau code.");
                errorAlert.showAndWait();
            }
        }
    }

    @FXML
    private void handleGoogleLogin() {
        // Disable buttons during auth flow
        loginButton.setDisable(true);
        googleLoginButton.setDisable(true);
        googleLoginButton.setText("⏳  Authentification en cours...");
        errorLabel.setVisible(false);

        Task<GoogleUserInfo> task = new Task<>() {
            @Override
            protected GoogleUserInfo call() throws Exception {
                return new GoogleAuthService().authenticate();
            }
        };

        task.setOnSucceeded(event -> {
            GoogleUserInfo info = task.getValue();
            if (info == null) {
                restoreGoogleButton();
                showError("Authentification Google annulée");
                return;
            }

            // Find or create user
            Optional<User> userOpt = userController.findByGoogleId(info.getId());
            User user;
            if (userOpt.isPresent()) {
                user = userOpt.get();
            } else {
                // Check if email already registered with local account
                Optional<User> byEmail = userController.findByEmail(info.getEmail());
                if (byEmail.isPresent()) {
                    // Link Google ID to existing account
                    user = byEmail.get();
                    user.setGoogleId(info.getId());
                    user.setAuthProvider("google");
                    userController.update(user);
                } else {
                    user = userController.createGoogleUser(
                            info.getId(), info.getEmail(), info.getName(), info.getPicture());
                }
            }

            if (user == null) {
                restoreGoogleButton();
                showError("Impossible de créer ou trouver votre compte");
                return;
            }

            // Create session
            boolean isAdmin = user.getUserId() == 0;
            String token = JWTUtil.generateToken(user.getUserId(), user.getUsername(), user.getEmail(), isAdmin);
            SessionManager.getInstance().login(token);
            user.setOnline(true);
            userController.update(user);

            try {
                Stage stage = (Stage) loginButton.getScene().getWindow();
                if (isAdmin) {
                    openAdminDashboard(stage);
                } else {
                    openUserDashboard(stage);
                }
            } catch (Exception e) {
                e.printStackTrace();
                restoreGoogleButton();
                showError("Erreur lors de l'ouverture du tableau de bord");
            }
        });

        task.setOnFailed(event -> {
            restoreGoogleButton();
            showError("Erreur Google: " + task.getException().getMessage());
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void restoreGoogleButton() {
        Platform.runLater(() -> {
            loginButton.setDisable(false);
            googleLoginButton.setDisable(false);
            googleLoginButton.setText("🔵  Se connecter avec Google");
        });
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }
}

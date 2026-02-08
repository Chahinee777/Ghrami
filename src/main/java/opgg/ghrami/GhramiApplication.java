package opgg.ghrami;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import opgg.ghrami.controller.UserController;
import opgg.ghrami.model.User;
import opgg.ghrami.util.PasswordUtil;

import java.time.LocalDateTime;

public class GhramiApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Initialize admin user on startup
        initializeAdmin();
        
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/LoginView.fxml"));
        Scene scene = new Scene(loader.load());
        scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
        
        primaryStage.setTitle("Ghrami - Connexion");
        primaryStage.setScene(scene);
        primaryStage.setWidth(450);
        primaryStage.setHeight(600);
        primaryStage.setResizable(true);
        primaryStage.setMaximized(false);
        
        // Set user offline when closing application window
        primaryStage.setOnCloseRequest(event -> {
            opgg.ghrami.util.SessionManager session = opgg.ghrami.util.SessionManager.getInstance();
            if (session.isLoggedIn()) {
                try {
                    UserController userController = new UserController();
                    long userId = session.getUserId();
                    User user = userController.findById((int) userId);
                    if (user != null) {
                        user.setOnline(false);
                        userController.update(user);
                        System.out.println("✅ User set to offline on window close: " + user.getUsername());
                    }
                } catch (Exception e) {
                    System.err.println("❌ Error setting user offline on close: " + e.getMessage());
                }
            }
        });
        
        primaryStage.show();
    }

    private void initializeAdmin() {
        UserController userController = new UserController();
        
        // Check if admin exists
        User existingAdmin = userController.findById(0);
        if (existingAdmin == null) {
            System.out.println("🔧 Admin user not found. Creating admin...");
            
            // Create admin user with ID = 0
            User admin = new User();
            admin.setUserId(0L);
            admin.setUsername("chahine");
            admin.setFullName("Chahine Admin");
            admin.setEmail("chahine@ghrami.tn");
            admin.setPassword(PasswordUtil.hashPassword("admin123"));
            admin.setBio("Administrateur système");
            admin.setLocation("Tunis");
            admin.setProfilePicture("");
            admin.setOnline(false);
            admin.setCreatedAt(LocalDateTime.now());
            
            User createdAdmin = userController.create(admin);
            if (createdAdmin != null) {
                System.out.println("✅ Admin user created successfully!");
            } else {
                System.err.println("❌ Failed to create admin user");
            }
        } else {
            System.out.println("✅ Admin user already exists (ID: " + existingAdmin.getUserId() + ")");
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}

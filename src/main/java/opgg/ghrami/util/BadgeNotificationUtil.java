package opgg.ghrami.util;

import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.Duration;
import opgg.ghrami.model.Badge;

/**
 * Utility class for showing beautiful badge award notifications
 */
public class BadgeNotificationUtil {
    
    /**
     * Show a badge award notification to the user
     * @param badge The badge that was awarded
     * @param username The username of the recipient
     */
    public static void showBadgeAwardNotification(Badge badge, String username) {
        Stage notification = new Stage();
        notification.initStyle(StageStyle.TRANSPARENT);
        notification.initModality(Modality.NONE);
        
        // Create notification content
        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(25, 40, 25, 40));
        content.setStyle(
            "-fx-background-color: linear-gradient(135deg, #667eea 0%, #764ba2 100%);" +
            "-fx-background-radius: 20;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.4), 30, 0, 0, 10);"
        );
        
        // Badge icon with animation
        Label icon = new Label(getBadgeIcon(badge.getName()));
        icon.setStyle("-fx-font-size: 60;");
        
        // Title
        Label title = new Label("🎉 Badge Awarded!");
        title.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: white;");
        
        // Badge name
        Label badgeName = new Label(badge.getName());
        badgeName.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: #ffd700;");
        
        // Description
        Label description = new Label(badge.getDescription() != null ? badge.getDescription() : "");
        description.setStyle("-fx-font-size: 13; -fx-text-fill: rgba(255,255,255,0.9); -fx-text-alignment: center;");
        description.setWrapText(true);
        description.setMaxWidth(300);
        description.setAlignment(Pos.CENTER);
        
        // Recipient info
        Label recipient = new Label("Awarded to: " + username);
        recipient.setStyle("-fx-font-size: 12; -fx-text-fill: rgba(255,255,255,0.8); -fx-font-style: italic;");
        
        content.getChildren().addAll(icon, title, badgeName, description, recipient);
        
        Scene scene = new Scene(content);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        notification.setScene(scene);
        
        // Position at top-right of screen
        notification.setX(javafx.stage.Screen.getPrimary().getVisualBounds().getWidth() - 400);
        notification.setY(50);
        
        // Create animations
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), content);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        
        ScaleTransition scaleIn = new ScaleTransition(Duration.millis(500), content);
        scaleIn.setFromX(0.5);
        scaleIn.setFromY(0.5);
        scaleIn.setToX(1.0);
        scaleIn.setToY(1.0);
        
        // Icon bounce animation
        TranslateTransition bounce = new TranslateTransition(Duration.millis(200), icon);
        bounce.setByY(-20);
        bounce.setCycleCount(4);
        bounce.setAutoReverse(true);
        
        ParallelTransition showAnimation = new ParallelTransition(fadeIn, scaleIn);
        
        // Auto-close after 4 seconds
        PauseTransition delay = new PauseTransition(Duration.seconds(4));
        delay.setOnFinished(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(500), content);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(ev -> notification.close());
            fadeOut.play();
        });
        
        // Show notification
        notification.show();
        showAnimation.play();
        showAnimation.setOnFinished(e -> {
            bounce.play();
            delay.play();
        });
        
        // Click to close
        content.setOnMouseClicked(e -> notification.close());
    }
    
    /**
     * Show a simple toast notification
     */
    public static void showToast(String message) {
        Stage toast = new Stage();
        toast.initStyle(StageStyle.TRANSPARENT);
        toast.initModality(Modality.NONE);
        
        HBox content = new HBox(15);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(20, 30, 20, 30));
        content.setStyle(
            "-fx-background-color: rgba(40, 44, 52, 0.95);" +
            "-fx-background-radius: 30;" +
            "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.3), 20, 0, 0, 8);"
        );
        
        Label icon = new Label("✅");
        icon.setStyle("-fx-font-size: 24;");
        
        Label text = new Label(message);
        text.setStyle("-fx-font-size: 14; -fx-text-fill: white; -fx-font-weight: bold;");
        
        content.getChildren().addAll(icon, text);
        
        Scene scene = new Scene(content);
        scene.setFill(javafx.scene.paint.Color.TRANSPARENT);
        toast.setScene(scene);
        
        // Position at bottom-center
        toast.setX((javafx.stage.Screen.getPrimary().getVisualBounds().getWidth() - 300) / 2);
        toast.setY(javafx.stage.Screen.getPrimary().getVisualBounds().getHeight() - 150);
        
        // Fade animation
        FadeTransition fadeIn = new FadeTransition(Duration.millis(300), content);
        fadeIn.setFromValue(0.0);
        fadeIn.setToValue(1.0);
        
        PauseTransition delay = new PauseTransition(Duration.seconds(2));
        delay.setOnFinished(e -> {
            FadeTransition fadeOut = new FadeTransition(Duration.millis(300), content);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(ev -> toast.close());
            fadeOut.play();
        });
        
        toast.show();
        fadeIn.play();
        fadeIn.setOnFinished(e -> delay.play());
    }
    
    /**
     * Get appropriate icon based on badge name
     */
    private static String getBadgeIcon(String badgeName) {
        if (badgeName == null) return "🏆";
        
        String name = badgeName.toLowerCase();
        
        if (name.contains("first") || name.contains("welcome") || name.contains("aboard")) return "🎉";
        if (name.contains("friend") || name.contains("social")) return "🤝";
        if (name.contains("creator") || name.contains("content") || name.contains("post")) return "📝";
        if (name.contains("vip") || name.contains("premium") || name.contains("diamond")) return "💎";
        if (name.contains("star") || name.contains("rising")) return "🌟";
        if (name.contains("fire") || name.contains("streak")) return "🔥";
        if (name.contains("goal") || name.contains("achiever")) return "🎯";
        if (name.contains("connect") || name.contains("butterfly")) return "🦋";
        if (name.contains("champion") || name.contains("winner")) return "🏆";
        if (name.contains("creative") || name.contains("artist")) return "🎨";
        if (name.contains("support") || name.contains("helper")) return "⭐";
        if (name.contains("early") || name.contains("pioneer")) return "🚀";
        if (name.contains("gold")) return "🥇";
        if (name.contains("silver")) return "🥈";
        if (name.contains("bronze")) return "🥉";
        
        return "🏆";
    }
}

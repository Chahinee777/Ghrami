package opgg.ghrami.view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import opgg.ghrami.controller.BadgeController;
import opgg.ghrami.model.Badge;
import opgg.ghrami.util.SessionManager;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class BadgesViewController {
    
    @FXML private Label badgeCountLabel;
    @FXML private Label totalBadgesLabel;
    @FXML private Label recentBadgesLabel;
    @FXML private Label progressLabel;
    @FXML private ComboBox<String> filterCombo;
    @FXML private FlowPane badgesContainer;
    @FXML private VBox emptyState;
    
    private BadgeController badgeController;
    private Long currentUserId;
    private List<Badge> allBadges;
    
    @FXML
    public void initialize() {
        badgeController = new BadgeController();
        currentUserId = SessionManager.getInstance().getUserId();
        
        setupFilterCombo();
        loadBadges();
    }
    
    private void setupFilterCombo() {
        filterCombo.getItems().addAll("All Time", "This Month", "This Year", "Older");
        filterCombo.setValue("All Time");
        filterCombo.setOnAction(e -> filterBadges());
    }
    
    private void loadBadges() {
        if (currentUserId == null) {
            showAlert("Error", "No user logged in");
            return;
        }
        
        allBadges = badgeController.findByUserId(currentUserId);
        updateStats();
        displayBadges(allBadges);
    }
    
    private void updateStats() {
        int totalCount = allBadges.size();
        totalBadgesLabel.setText(String.valueOf(totalCount));
        badgeCountLabel.setText(totalCount + " badge" + (totalCount > 1 ? "s" : "") + " earned");
        
        // Count badges earned this month
        LocalDateTime nowDateTime = LocalDateTime.now();
        LocalDateTime monthStart = nowDateTime.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
        long recentCount = allBadges.stream()
                .filter(b -> b.getEarnedDate() != null && b.getEarnedDate().isAfter(monthStart))
                .count();
        recentBadgesLabel.setText(String.valueOf(recentCount));
        
        // Calculate progress (assuming max 50 badges for demo)
        int maxBadges = 50;
        int progress = (int) ((totalCount / (double) maxBadges) * 100);
        progressLabel.setText(progress + "%");
        
        // Show/hide empty state
        emptyState.setVisible(totalCount == 0);
        emptyState.setManaged(totalCount == 0);
    }
    
    private void displayBadges(List<Badge> badges) {
        badgesContainer.getChildren().clear();
        
        if (badges.isEmpty()) {
            return;
        }
        
        for (Badge badge : badges) {
            VBox badgeCard = createBadgeCard(badge);
            badgesContainer.getChildren().add(badgeCard);
        }
    }
    
    private VBox createBadgeCard(Badge badge) {
        VBox card = new VBox(12);
        card.setAlignment(Pos.CENTER);
        card.setStyle("-fx-background-color: white; " +
                     "-fx-background-radius: 20; " +
                     "-fx-padding: 25; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 5); " +
                     "-fx-cursor: hand;");
        card.setPrefWidth(220);
        card.setMaxWidth(220);
        card.setMinHeight(280);
        
        // Badge icon
        String badgeIcon = getBadgeIcon(badge.getName());
        Label icon = new Label(badgeIcon);
        icon.setStyle("-fx-font-size: 56;");
        
        // Badge name
        Label nameLabel = new Label(badge.getName());
        nameLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #1c1e21; -fx-wrap-text: true; -fx-text-alignment: center;");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(190);
        nameLabel.setAlignment(Pos.CENTER);
        
        // Badge description
        Label descLabel = new Label(badge.getDescription() != null ? badge.getDescription() : "");
        descLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #65676b; -fx-wrap-text: true; -fx-text-alignment: center;");
        descLabel.setWrapText(true);
        descLabel.setMaxWidth(190);
        descLabel.setAlignment(Pos.CENTER);
        descLabel.setMaxHeight(45);
        
        // Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);
        
        // Earned date
        String dateText = "";
        if (badge.getEarnedDate() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
            dateText = "🗓️ " + badge.getEarnedDate().format(formatter);
        }
        Label dateLabel = new Label(dateText);
        dateLabel.setStyle("-fx-font-size: 11; -fx-text-fill: #95a5a6; -fx-font-style: italic;");
        
        card.getChildren().addAll(icon, nameLabel, descLabel, spacer, dateLabel);
        
        // Hover effect - enhanced
        card.setOnMouseEntered(e -> {
            card.setStyle("-fx-background-color: white; " +
                         "-fx-background-radius: 20; " +
                         "-fx-padding: 25; " +
                         "-fx-effect: dropshadow(gaussian, rgba(102,126,234,0.3), 20, 0, 0, 8); " +
                         "-fx-cursor: hand; " +
                         "-fx-scale-x: 1.03; -fx-scale-y: 1.03;");
        });
        card.setOnMouseExited(e -> {
            card.setStyle("-fx-background-color: white; " +
                         "-fx-background-radius: 20; " +
                         "-fx-padding: 25; " +
                         "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 5); " +
                         "-fx-cursor: hand;");
        });
        
        // Click to show details
        card.setOnMouseClicked(e -> showBadgeDetails(badge));
        
        return card;
    }
    
    private String getBadgeIcon(String badgeName) {
        // Map badge names to emoji icons
        if (badgeName == null) return "🏆";
        
        String lowerName = badgeName.toLowerCase();
        if (lowerName.contains("friend") || lowerName.contains("ami")) return "👥";
        if (lowerName.contains("first") || lowerName.contains("premier")) return "🌟";
        if (lowerName.contains("class") || lowerName.contains("cours")) return "📚";
        if (lowerName.contains("milestone") || lowerName.contains("étape")) return "🎯";
        if (lowerName.contains("hobby") || lowerName.contains("loisir")) return "🎨";
        if (lowerName.contains("social") || lowerName.contains("partage")) return "💬";
        if (lowerName.contains("expert")) return "⭐";
        if (lowerName.contains("champion")) return "🥇";
        if (lowerName.contains("master") || lowerName.contains("maître")) return "👑";
        if (lowerName.contains("creator") || lowerName.contains("créateur")) return "✨";
        if (lowerName.contains("explorer")) return "🧭";
        if (lowerName.contains("achiever")) return "🎖️";
        
        return "🏆";
    }
    
    private void showBadgeDetails(Badge badge) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Badge Details");
        alert.setHeaderText(getBadgeIcon(badge.getName()) + " " + badge.getName());
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        
        Label descLabel = new Label("Description:");
        descLabel.setStyle("-fx-font-weight: bold;");
        
        Label desc = new Label(badge.getDescription() != null ? badge.getDescription() : "No description available");
        desc.setWrapText(true);
        desc.setMaxWidth(350);
        
        Label earnedLabel = new Label("Earned on:");
        earnedLabel.setStyle("-fx-font-weight: bold; -fx-padding: 10 0 0 0;");
        
        String earnedText = "N/A";
        if (badge.getEarnedDate() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy 'at' HH:mm");
            earnedText = badge.getEarnedDate().format(formatter);
        }
        Label earnedValue = new Label(earnedText);
        
        content.getChildren().addAll(descLabel, desc, earnedLabel, earnedValue);
        alert.getDialogPane().setContent(content);
        alert.showAndWait();
    }
    
    @FXML
    private void filterBadges() {
        String filter = filterCombo.getValue();
        if (filter == null || allBadges == null) return;
        
        List<Badge> filteredBadges;
        LocalDateTime now = LocalDateTime.now();
        
        switch (filter) {
            case "This Month":
                LocalDateTime monthStart = now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
                filteredBadges = allBadges.stream()
                        .filter(b -> b.getEarnedDate() != null && b.getEarnedDate().isAfter(monthStart))
                        .toList();
                break;
            case "This Year":
                LocalDateTime yearStart = now.withDayOfYear(1).truncatedTo(ChronoUnit.DAYS);
                filteredBadges = allBadges.stream()
                        .filter(b -> b.getEarnedDate() != null && b.getEarnedDate().isAfter(yearStart))
                        .toList();
                break;
            case "Older":
                LocalDateTime lastYear = now.minusYears(1);
                filteredBadges = allBadges.stream()
                        .filter(b -> b.getEarnedDate() != null && b.getEarnedDate().isBefore(lastYear))
                        .toList();
                break;
            default: // "All Time"
                filteredBadges = allBadges;
                break;
        }
        
        displayBadges(filteredBadges);
    }
    
    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/UserFeed.fxml"));
            Scene scene = new Scene(loader.load(), 1400, 900);
            scene.getStylesheets().add(getClass().getResource("/css/social-style.css").toExternalForm());
            
            Stage stage = (Stage) badgesContainer.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Ghrami - News Feed");
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Unable to return to feed: " + e.getMessage());
        }
    }
    
    @FXML
    private void showBadgeInfo() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("How to Earn Badges");
        alert.setHeaderText("💡 Earning Badges on Ghrami");
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-font-size: 13;");
        
        String[] tips = {
            "👥 Connect with Others - Make friends and build your network",
            "🎯 Pursue Your Hobbies - Actively participate in hobbies and events",
            "📚 Complete Classes - Enroll in classes and achieve milestones",
            "🌟 Share & Help - Share your experience and help the community grow",
            "💬 Stay Active - Regular engagement earns you more badges",
            "🏆 Reach Milestones - Complete goals and challenges"
        };
        
        for (String tip : tips) {
            Label tipLabel = new Label(tip);
            tipLabel.setWrapText(true);
            tipLabel.setMaxWidth(400);
            content.getChildren().add(tipLabel);
        }
        
        alert.getDialogPane().setContent(content);
        alert.showAndWait();
    }
    
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

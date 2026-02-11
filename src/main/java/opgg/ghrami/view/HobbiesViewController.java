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
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import opgg.ghrami.controller.HobbyController;
import opgg.ghrami.controller.MilestoneController;
import opgg.ghrami.controller.ProgressController;
import opgg.ghrami.model.Hobby;
import opgg.ghrami.model.Milestone;
import opgg.ghrami.model.Progress;
import opgg.ghrami.util.SessionManager;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class HobbiesViewController implements Initializable {
    
    @FXML private Label totalHobbiesLabel;
    @FXML private Label totalHoursLabel;
    @FXML private Label totalMilestonesLabel;
    @FXML private ComboBox<String> categoryFilterCombo;
    @FXML private TextField searchField;
    @FXML private FlowPane hobbiesContainer;
    
    private HobbyController hobbyController;
    private ProgressController progressController;
    private MilestoneController milestoneController;
    private SessionManager sessionManager;
    private List<Hobby> allHobbies;
    
    // Predefined categories
    private static final String[] CATEGORIES = {
        "All Categories", "Sports & Fitness", "Arts & Crafts", "Music", "Cooking", 
        "Gaming", "Reading", "Technology", "Photography", "Gardening", 
        "Writing", "Learning Languages", "Dancing", "Traveling", "Other"
    };
    
    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        hobbyController = new HobbyController();
        progressController = new ProgressController();
        milestoneController = new MilestoneController();
        sessionManager = SessionManager.getInstance();
        
        // Setup category filter
        categoryFilterCombo.setItems(FXCollections.observableArrayList(CATEGORIES));
        categoryFilterCombo.setValue("All Categories");
        categoryFilterCombo.setOnAction(e -> filterHobbies());
        
        // Setup search
        searchField.textProperty().addListener((obs, old, newVal) -> filterHobbies());
        
        // Load hobbies
        loadHobbies();
        updateStats();
    }
    
    private void loadHobbies() {
        long userId = sessionManager.getUserId();
        allHobbies = hobbyController.findByUserId(userId);
        displayHobbies(allHobbies);
    }
    
    private void displayHobbies(List<Hobby> hobbies) {
        hobbiesContainer.getChildren().clear();
        
        if (hobbies.isEmpty()) {
            VBox emptyBox = createEmptyState();
            hobbiesContainer.getChildren().add(emptyBox);
        } else {
            for (Hobby hobby : hobbies) {
                hobbiesContainer.getChildren().add(createHobbyCard(hobby));
            }
        }
    }
    
    private VBox createEmptyState() {
        VBox emptyBox = new VBox(15);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.setPrefSize(1100, 400);
        emptyBox.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-padding: 60;");
        
        Label icon = new Label("🎯");
        icon.setStyle("-fx-font-size: 80; -fx-opacity: 0.5;");
        
        Label title = new Label("No hobbies yet");
        title.setStyle("-fx-font-size: 24; -fx-font-weight: bold; -fx-text-fill: #1c1e21;");
        
        Label subtitle = new Label("Start tracking your passions and watch your progress grow!");
        subtitle.setStyle("-fx-font-size: 14; -fx-text-fill: #65676b;");
        
        Button addButton = new Button("➕ Add Your First Hobby");
        addButton.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; -fx-background-radius: 25; " +
                "-fx-padding: 15 40; -fx-font-size: 15; -fx-font-weight: bold; -fx-cursor: hand;");
        addButton.setOnAction(e -> handleAddHobby());
        
        emptyBox.getChildren().addAll(icon, title, subtitle, addButton);
        return emptyBox;
    }
    
    private VBox createHobbyCard(Hobby hobby) {
        VBox card = new VBox(18);
        card.setPrefWidth(420);
        card.setMaxWidth(420);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-padding: 30; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 5); -fx-cursor: hand;");
        
        // Hover effect
        card.setOnMouseEntered(e -> card.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 20; -fx-padding: 30; " +
                "-fx-effect: dropshadow(gaussian, rgba(102,126,234,0.3), 20, 0, 0, 8); -fx-cursor: hand; " +
                "-fx-scale-x: 1.02; -fx-scale-y: 1.02;"));
        card.setOnMouseExited(e -> card.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-padding: 30; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 5); -fx-cursor: hand;"));
        
        // Header
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        
        String iconEmoji = getCategoryIcon(hobby.getCategory());
        Label icon = new Label(iconEmoji);
        icon.setStyle("-fx-font-size: 45;");
        
        VBox titleBox = new VBox(5);
        Label name = new Label(hobby.getName());
        name.setStyle("-fx-font-size: 20; -fx-font-weight: bold; -fx-text-fill: #1c1e21;");
        name.setWrapText(true);
        name.setMaxWidth(300);
        
        Label category = new Label(hobby.getCategory() != null ? hobby.getCategory() : "Uncategorized");
        category.setStyle("-fx-font-size: 12; -fx-text-fill: white; -fx-background-color: #667eea; " +
                "-fx-padding: 4 12; -fx-background-radius: 12;");
        
        titleBox.getChildren().addAll(name, category);
        header.getChildren().addAll(icon, titleBox);
        
        // Description
        Label desc = new Label(hobby.getDescription() != null && !hobby.getDescription().isEmpty() 
                ? hobby.getDescription() : "No description");
        desc.setStyle("-fx-font-size: 14; -fx-text-fill: #65676b;");
        desc.setWrapText(true);
        desc.setMaxHeight(70);
        
        // Progress Info
        Optional<Progress> progressOpt = progressController.findByHobbyId(hobby.getHobbyId());
        double hours = progressOpt.map(Progress::getHoursSpent).orElse(0.0);
        
        HBox progressBox = new HBox(12);
        progressBox.setAlignment(Pos.CENTER_LEFT);
        progressBox.setStyle("-fx-background-color: #f0f2f5; -fx-background-radius: 12; -fx-padding: 15;");
        
        Label hoursLabel = new Label(String.format("⏱️ %.1f hrs", hours));
        hoursLabel.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");
        
        // Milestones
        List<Milestone> milestones = milestoneController.findByHobbyId(hobby.getHobbyId());
        int total = milestones.size();
        int achieved = (int) milestones.stream().filter(Milestone::getIsAchieved).count();
        
        Label milestonesLabel = new Label(String.format("🏆 %d/%d goals", achieved, total));
        milestonesLabel.setStyle("-fx-font-size: 15; -fx-font-weight: bold; -fx-text-fill: #FF9800;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        progressBox.getChildren().addAll(hoursLabel, spacer, milestonesLabel);
        
        // Action Buttons
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER);
        
        Button viewBtn = new Button("📊 Details");
        styleActionButton(viewBtn, "#2196F3");
        viewBtn.setOnAction(e -> handleViewHobby(hobby));
        
        Button progressBtn = new Button("➕ Log Time");
        styleActionButton(progressBtn, "#4CAF50");
        progressBtn.setOnAction(e -> handleLogProgress(hobby));
        
        Button editBtn = new Button("✏️");
        styleSmallButton(editBtn, "#FF9800");
        editBtn.setOnAction(e -> handleEditHobby(hobby));
        
        Button deleteBtn = new Button("🗑️");
        styleSmallButton(deleteBtn, "#f44336");
        deleteBtn.setOnAction(e -> handleDeleteHobby(hobby));
        
        actions.getChildren().addAll(viewBtn, progressBtn, editBtn, deleteBtn);
        
        card.getChildren().addAll(header, desc, progressBox, new Separator(), actions);
        
        return card;
    }
    
    private void styleActionButton(Button button, String color) {
        button.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                "-fx-background-radius: 18; -fx-padding: 10 18; -fx-font-size: 13; " +
                "-fx-font-weight: bold; -fx-cursor: hand;");
    }
    
    private void styleSmallButton(Button button, String color) {
        button.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                "-fx-background-radius: 18; -fx-padding: 10 14; -fx-font-size: 13; " +
                "-fx-font-weight: bold; -fx-cursor: hand; -fx-min-width: 45;");
    }
    
    private String getCategoryIcon(String category) {
        if (category == null) return "🎯";
        switch (category) {
            case "Sports & Fitness": return "⚽";
            case "Arts & Crafts": return "🎨";
            case "Music": return "🎵";
            case "Cooking": return "🍳";
            case "Gaming": return "🎮";
            case "Reading": return "📚";
            case "Technology": return "💻";
            case "Photography": return "📷";
            case "Gardening": return "🌱";
            case "Writing": return "✍️";
            case "Learning Languages": return "🗣️";
            case "Dancing": return "💃";
            case "Traveling": return "✈️";
            default: return "🎯";
        }
    }
    
    private void filterHobbies() {
        String category = categoryFilterCombo.getValue();
        String searchText = searchField.getText().toLowerCase();
        
        List<Hobby> filtered = allHobbies.stream()
                .filter(h -> (category.equals("All Categories") || category.equals(h.getCategory())))
                .filter(h -> searchText.isEmpty() || 
                        h.getName().toLowerCase().contains(searchText) ||
                        (h.getDescription() != null && h.getDescription().toLowerCase().contains(searchText)))
                .collect(Collectors.toList());
        
        displayHobbies(filtered);
    }
    
    private void updateStats() {
        long userId = sessionManager.getUserId();
        
        // Total hobbies
        int totalHobbies = hobbyController.countByUserId(userId);
        totalHobbiesLabel.setText(String.valueOf(totalHobbies));
        
        // Total hours
        List<Hobby> userHobbies = hobbyController.findByUserId(userId);
        double totalHours = userHobbies.stream()
                .mapToDouble(h -> progressController.findByHobbyId(h.getHobbyId())
                        .map(Progress::getHoursSpent).orElse(0.0))
                .sum();
        totalHoursLabel.setText(String.format("%.1f", totalHours));
        
        // Total milestones
        int totalMilestones = 0;
        int achievedMilestones = 0;
        for (Hobby hobby : userHobbies) {
            List<Milestone> milestones = milestoneController.findByHobbyId(hobby.getHobbyId());
            totalMilestones += milestones.size();
            achievedMilestones += milestones.stream().filter(Milestone::getIsAchieved).count();
        }
        totalMilestonesLabel.setText(achievedMilestones + " / " + totalMilestones);
    }
    
    @FXML
    private void handleBackToDashboard() {
        try {
            Stage stage = (Stage) hobbiesContainer.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/UserFeed.fxml"));
            Scene scene = new Scene(loader.load(), 1200, 800);
            scene.getStylesheets().add(getClass().getResource("/css/social-style.css").toExternalForm());
            stage.setScene(scene);
        } catch (Exception e) {
            showError("Failed to navigate: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleRefresh() {
        loadHobbies();
        updateStats();
        showToast("✅ Refreshed!");
    }
    
    @FXML
    private void handleAddHobby() {
        Stage dialog = createModal("Add Hobby", "🎯");
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: white; -fx-background-radius: 20;");
        
        Label title = new Label("Create New Hobby");
        title.setStyle("-fx-font-size: 22; -fx-font-weight: bold;");
        
        TextField nameField = createTextField("Hobby name (e.g., Guitar, Running)");
        ComboBox<String> categoryCombo = new ComboBox<>(FXCollections.observableArrayList(
                java.util.Arrays.copyOfRange(CATEGORIES, 1, CATEGORIES.length)));
        categoryCombo.setPromptText("Select category");
        categoryCombo.setPrefWidth(400);
        styleComboBox(categoryCombo);
        
        TextArea descArea = new TextArea();
        descArea.setPromptText("Description (optional)");
        descArea.setPrefRowCount(3);
        descArea.setWrapText(true);
        styleTextArea(descArea);
        
        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        
        Button cancelBtn = new Button("Cancel");
        Button createBtn = new Button("✨ Create Hobby");
        styleButton(cancelBtn, "#9e9e9e");
        styleButton(createBtn, "#4CAF50");
        
        cancelBtn.setOnAction(e -> dialog.close());
        createBtn.setOnAction(e -> {
            if (nameField.getText().trim().isEmpty()) {
                showError("Please enter a hobby name");
                return;
            }
            if (categoryCombo.getValue() == null) {
                showError("Please select a category");
                return;
            }
            
            Hobby hobby = new Hobby();
            hobby.setUserId(sessionManager.getUserId());
            hobby.setName(nameField.getText().trim());
            hobby.setCategory(categoryCombo.getValue());
            hobby.setDescription(descArea.getText().trim());
            
            Hobby created = hobbyController.create(hobby);
            if (created != null) {
                // Also create initial progress entry
                Progress progress = new Progress(created.getHobbyId(), 0.0, "Started tracking");
                progressController.create(progress);
                
                dialog.close();
                loadHobbies();
                updateStats();
                showSuccess("Hobby created successfully!");
            } else {
                showError("Failed to create hobby");
            }
        });
        
        buttons.getChildren().addAll(cancelBtn, createBtn);
        content.getChildren().addAll(title, new Label("Name:"), nameField, 
                new Label("Category:"), categoryCombo, new Label("Description:"), descArea, buttons);
        
        Scene scene = new Scene(content, 450, 500);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
    
    private void handleEditHobby(Hobby hobby) {
        Stage dialog = createModal("Edit Hobby", "✏️");
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: white; -fx-background-radius: 20;");
        
        Label title = new Label("Edit Hobby");
        title.setStyle("-fx-font-size: 22; -fx-font-weight: bold;");
        
        TextField nameField = createTextField("Hobby name");
        nameField.setText(hobby.getName());
        
        ComboBox<String> categoryCombo = new ComboBox<>(FXCollections.observableArrayList(
                java.util.Arrays.copyOfRange(CATEGORIES, 1, CATEGORIES.length)));
        categoryCombo.setValue(hobby.getCategory());
        categoryCombo.setPrefWidth(400);
        styleComboBox(categoryCombo);
        
        TextArea descArea = new TextArea();
        descArea.setText(hobby.getDescription());
        descArea.setPrefRowCount(3);
        descArea.setWrapText(true);
        styleTextArea(descArea);
        
        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        
        Button cancelBtn = new Button("Cancel");
        Button saveBtn = new Button("💾 Save Changes");
        styleButton(cancelBtn, "#9e9e9e");
        styleButton(saveBtn, "#FF9800");
        
        cancelBtn.setOnAction(e -> dialog.close());
        saveBtn.setOnAction(e -> {
            if (nameField.getText().trim().isEmpty()) {
                showError("Please enter a hobby name");
                return;
            }
            
            hobby.setName(nameField.getText().trim());
            hobby.setCategory(categoryCombo.getValue());
            hobby.setDescription(descArea.getText().trim());
            
            Hobby updated = hobbyController.update(hobby);
            if (updated != null) {
                dialog.close();
                loadHobbies();
                showSuccess("Hobby updated successfully!");
            } else {
                showError("Failed to update hobby");
            }
        });
        
        buttons.getChildren().addAll(cancelBtn, saveBtn);
        content.getChildren().addAll(title, new Label("Name:"), nameField, 
                new Label("Category:"), categoryCombo, new Label("Description:"), descArea, buttons);
        
        Scene scene = new Scene(content, 450, 500);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
    
    private void handleDeleteHobby(Hobby hobby) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Hobby");
        confirm.setHeaderText("Are you sure?");
        confirm.setContentText("This will delete the hobby and all associated progress and milestones.");
        
        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (hobbyController.delete(hobby.getHobbyId())) {
                loadHobbies();
                updateStats();
                showSuccess("Hobby deleted successfully");
            } else {
                showError("Failed to delete hobby");
            }
        }
    }
    
    private void handleLogProgress(Hobby hobby) {
        Stage dialog = createModal("Log Progress", "➕");
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: white; -fx-background-radius: 20;");
        
        Label title = new Label("Log Progress: " + hobby.getName());
        title.setStyle("-fx-font-size: 20; -fx-font-weight: bold;");
        title.setWrapText(true);
        title.setMaxWidth(350);
        
        TextField hoursField = createTextField("Hours spent (e.g., 1.5)");
        TextArea notesArea = new TextArea();
        notesArea.setPromptText("Notes about this session (optional)");
        notesArea.setPrefRowCount(3);
        notesArea.setWrapText(true);
        styleTextArea(notesArea);
        
        // Show current total
        Optional<Progress> currentProgress = progressController.findByHobbyId(hobby.getHobbyId());
        double currentHours = currentProgress.map(Progress::getHoursSpent).orElse(0.0);
        Label currentLabel = new Label(String.format("Current total: %.1f hours", currentHours));
        currentLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #667eea; -fx-font-weight: bold;");
        
        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        
        Button cancelBtn = new Button("Cancel");
        Button logBtn = new Button("✅ Log Time");
        styleButton(cancelBtn, "#9e9e9e");
        styleButton(logBtn, "#4CAF50");
        
        cancelBtn.setOnAction(e -> dialog.close());
        logBtn.setOnAction(e -> {
            try {
                double hours = Double.parseDouble(hoursField.getText().trim());
                if (hours <= 0) {
                    showError("Please enter a positive number");
                    return;
                }
                
                Progress updated = progressController.addHours(hobby.getHobbyId(), hours, notesArea.getText().trim());
                if (updated != null) {
                    dialog.close();
                    loadHobbies();
                    updateStats();
                    showSuccess(String.format("Logged %.1f hours!", hours));
                } else {
                    showError("Failed to log progress");
                }
            } catch (NumberFormatException ex) {
                showError("Please enter a valid number");
            }
        });
        
        buttons.getChildren().addAll(cancelBtn, logBtn);
        content.getChildren().addAll(title, currentLabel, new Label("Hours to add:"), hoursField, 
                new Label("Notes:"), notesArea, buttons);
        
        Scene scene = new Scene(content, 400, 450);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
    
    private void handleViewHobby(Hobby hobby) {
        Stage dialog = createModal("Hobby Details", "📊");
        dialog.setWidth(600);
        dialog.setHeight(700);
        
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #f0f2f5; -fx-border-color: transparent;");
        
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: #f0f2f5;");
        
        // Header
        VBox header = new VBox(10);
        header.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 25;");
        
        HBox titleBox = new HBox(15);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label(getCategoryIcon(hobby.getCategory()));
        icon.setStyle("-fx-font-size: 50;");
        
        VBox titleText = new VBox(5);
        Label name = new Label(hobby.getName());
        name.setStyle("-fx-font-size: 24; -fx-font-weight: bold;");
        Label category = new Label(hobby.getCategory());
        category.setStyle("-fx-font-size: 13; -fx-text-fill: #667eea;");
        titleText.getChildren().addAll(name, category);
        
        titleBox.getChildren().addAll(icon, titleText);
        
        Label desc = new Label(hobby.getDescription() != null ? hobby.getDescription() : "No description");
        desc.setStyle("-fx-font-size: 14; -fx-text-fill: #65676b;");
        desc.setWrapText(true);
        
        header.getChildren().addAll(titleBox, new Separator(), desc);
        
        // Progress Section
        VBox progressSection = createProgressSection(hobby);
        
        // Milestones Section
        VBox milestonesSection = createMilestonesSection(hobby);
        
        Button closeBtn = new Button("Close");
        styleButton(closeBtn, "#2196F3");
        closeBtn.setMaxWidth(Double.MAX_VALUE);
        closeBtn.setOnAction(e -> dialog.close());
        
        content.getChildren().addAll(header, progressSection, milestonesSection, closeBtn);
        scrollPane.setContent(content);
        
        Scene scene = new Scene(scrollPane);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
    
    private VBox createProgressSection(Hobby hobby) {
        VBox section = new VBox(15);
        section.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 25;");
        
        Label title = new Label("⏱️ Progress Tracking");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
        
        Optional<Progress> progressOpt = progressController.findByHobbyId(hobby.getHobbyId());
        if (progressOpt.isPresent()) {
            Progress progress = progressOpt.get();
            Label hours = new Label(String.format("Total Hours: %.1f", progress.getHoursSpent()));
            hours.setStyle("-fx-font-size: 28; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");
            
            if (progress.getNotes() != null && !progress.getNotes().isEmpty()) {
                Label notesLabel = new Label("Latest Notes:");
                notesLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #65676b; -fx-font-weight: bold;");
                Label notes = new Label(progress.getNotes());
                notes.setStyle("-fx-font-size: 13; -fx-text-fill: #65676b;");
                notes.setWrapText(true);
                section.getChildren().addAll(title, hours, notesLabel, notes);
            } else {
                section.getChildren().addAll(title, hours);
            }
        } else {
            Label noData = new Label("No progress recorded yet");
            noData.setStyle("-fx-font-size: 14; -fx-text-fill: #65676b;");
            section.getChildren().addAll(title, noData);
        }
        
        return section;
    }
    
    private VBox createMilestonesSection(Hobby hobby) {
        VBox section = new VBox(15);
        section.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 25;");
        
        HBox titleBox = new HBox(15);
        titleBox.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label("🏆 Milestones");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button addBtn = new Button("➕ Add Milestone");
        styleButton(addBtn, "#4CAF50");
        addBtn.setPrefWidth(150);
        addBtn.setOnAction(e -> handleAddMilestone(hobby));
        
        titleBox.getChildren().addAll(title, spacer, addBtn);
        section.getChildren().add(titleBox);
        
        List<Milestone> milestones = milestoneController.findByHobbyId(hobby.getHobbyId());
        
        if (milestones.isEmpty()) {
            Label noMilestones = new Label("No milestones set yet. Add your first goal!");
            noMilestones.setStyle("-fx-font-size: 13; -fx-text-fill: #65676b;");
            section.getChildren().add(noMilestones);
        } else {
            for (Milestone milestone : milestones) {
                section.getChildren().add(createMilestoneItem(milestone, hobby));
            }
        }
        
        return section;
    }
    
    private HBox createMilestoneItem(Milestone milestone, Hobby hobby) {
        HBox item = new HBox(15);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setStyle("-fx-background-color: " + (milestone.getIsAchieved() ? "#e8f5e9" : "#fff3e0") + 
                "; -fx-background-radius: 10; -fx-padding: 15;");
        
        CheckBox checkBox = new CheckBox();
        checkBox.setSelected(milestone.getIsAchieved());
        checkBox.setStyle("-fx-font-size: 16;");
        checkBox.setOnAction(e -> {
            milestoneController.toggleAchieved(milestone.getMilestoneId());
            loadHobbies();
            updateStats();
            handleViewHobby(hobby); // Refresh the detail view
        });
        
        VBox textBox = new VBox(5);
        Label titleLabel = new Label(milestone.getTitle());
        titleLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; " +
                (milestone.getIsAchieved() ? "-fx-text-fill: #4CAF50; -fx-strikethrough: true;" : ""));
        
        String dateStr = milestone.getTargetDate() != null ? 
                milestone.getTargetDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) : "No date";
        Label dateLabel = new Label("Target: " + dateStr);
        dateLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #65676b;");
        
        textBox.getChildren().addAll(titleLabel, dateLabel);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button deleteBtn = new Button("🗑️");
        styleSmallButton(deleteBtn, "#f44336");
        deleteBtn.setOnAction(e -> {
            milestoneController.delete(milestone.getMilestoneId());
            handleViewHobby(hobby);
            updateStats();
        });
        
        item.getChildren().addAll(checkBox, textBox, spacer, deleteBtn);
        return item;
    }
    
    private void handleAddMilestone(Hobby hobby) {
        Stage dialog = createModal("Add Milestone", "🏆");
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: white; -fx-background-radius: 20;");
        
        Label title = new Label("Add Milestone");
        title.setStyle("-fx-font-size: 20; -fx-font-weight: bold;");
        
        TextField titleField = createTextField("Milestone title (e.g., Learn 10 songs)");
        DatePicker datePicker = new DatePicker();
        datePicker.setPromptText("Target date (optional)");
        datePicker.setPrefWidth(350);
        datePicker.setStyle("-fx-background-color: white; -fx-border-color: #e4e6eb; " +
                "-fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 10;");
        
        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        
        Button cancelBtn = new Button("Cancel");
        Button createBtn = new Button("✨ Create Milestone");
        styleButton(cancelBtn, "#9e9e9e");
        styleButton(createBtn, "#4CAF50");
        
        cancelBtn.setOnAction(e -> dialog.close());
        createBtn.setOnAction(e -> {
            if (titleField.getText().trim().isEmpty()) {
                showError("Please enter a milestone title");
                return;
            }
            
            Milestone milestone = new Milestone();
            milestone.setHobbyId(hobby.getHobbyId());
            milestone.setTitle(titleField.getText().trim());
            milestone.setTargetDate(datePicker.getValue());
            milestone.setIsAchieved(false);
            
            Milestone created = milestoneController.create(milestone);
            if (created != null) {
                dialog.close();
                handleViewHobby(hobby);
                updateStats();
                showSuccess("Milestone created!");
            } else {
                showError("Failed to create milestone");
            }
        });
        
        buttons.getChildren().addAll(cancelBtn, createBtn);
        content.getChildren().addAll(title, new Label("Title:"), titleField, 
                new Label("Target Date:"), datePicker, buttons);
        
        Scene scene = new Scene(content, 400, 350);
        dialog.setScene(scene);
        dialog.showAndWait();
    }
    
    // Helper methods for styling
    private TextField createTextField(String prompt) {
        TextField field = new TextField();
        field.setPromptText(prompt);
        field.setPrefWidth(400);
        field.setStyle("-fx-background-color: white; -fx-border-color: #e4e6eb; " +
                "-fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 12; -fx-font-size: 13;");
        return field;
    }
    
    private void styleTextArea(TextArea area) {
        area.setPrefWidth(400);
        area.setStyle("-fx-background-color: white; -fx-border-color: #e4e6eb; " +
                "-fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 12; -fx-font-size: 13;");
    }
    
    private void styleComboBox(ComboBox<String> combo) {
        combo.setStyle("-fx-background-color: white; -fx-border-color: #e4e6eb; " +
                "-fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 10;");
    }
    
    private void styleButton(Button button, String color) {
        button.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                "-fx-background-radius: 20; -fx-padding: 12 25; -fx-font-size: 13; " +
                "-fx-font-weight: bold; -fx-cursor: hand;");
    }
    
    private Stage createModal(String title, String icon) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.initStyle(StageStyle.UTILITY);
        dialog.setTitle(icon + " " + title);
        dialog.setResizable(false);
        return dialog;
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showToast(String message) {
        // Simple toast notification (can be enhanced with BadgeNotificationUtil)
        System.out.println(message);
    }
}

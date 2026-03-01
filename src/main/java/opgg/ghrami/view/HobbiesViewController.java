package opgg.ghrami.view;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
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
import opgg.ghrami.util.GeminiService;
import opgg.ghrami.controller.MilestoneController;
import opgg.ghrami.controller.ProgressController;
import opgg.ghrami.controller.ProgressLogController;
import opgg.ghrami.model.Hobby;
import opgg.ghrami.model.Milestone;
import opgg.ghrami.model.Progress;
import opgg.ghrami.model.ProgressLog;
import opgg.ghrami.util.SessionManager;

import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
    private ProgressLogController progressLogController;
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
        progressLogController = new ProgressLogController();
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
            double width = stage.getWidth();
            double height = stage.getHeight();
            boolean wasMaximized = stage.isMaximized();
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/UserFeed.fxml"));
            Scene scene = new Scene(loader.load(), width, height);
            scene.getStylesheets().add(getClass().getResource("/css/social-style.css").toExternalForm());
            stage.setScene(scene);
            if (wasMaximized) {
                stage.setMaximized(true);
            }
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
        VBox content = new VBox(16);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: white; -fx-background-radius: 20;");

        Label title = new Label("Log Progress: " + hobby.getName());
        title.setStyle("-fx-font-size: 20; -fx-font-weight: bold;");
        title.setWrapText(true);
        title.setMaxWidth(370);

        // Show current total
        Optional<Progress> currentProgress = progressController.findByHobbyId(hobby.getHobbyId());
        double currentHours = currentProgress.map(Progress::getHoursSpent).orElse(0.0);
        Label currentLabel = new Label(String.format("📊 Current total: %.1f hours", currentHours));
        currentLabel.setStyle("-fx-font-size: 13; -fx-text-fill: #667eea; -fx-font-weight: bold; "
                + "-fx-background-color: #f0f0ff; -fx-padding: 8 14; -fx-background-radius: 10;");

        // --- Date picker (defaulting to today) ---
        Label dateLabel = new Label("📅  Session Date:");
        dateLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold;");
        DatePicker sessionDatePicker = new DatePicker(LocalDate.now());
        sessionDatePicker.setPrefWidth(400);
        sessionDatePicker.setStyle("-fx-background-color: white; -fx-border-color: #e4e6eb; "
                + "-fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 10;");

        // --- Hours ---
        Label hoursLabel = new Label("⏱️  Hours spent:");
        hoursLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold;");
        TextField hoursField = createTextField("e.g.  1.5");

        // --- Notes ---
        Label notesLabel = new Label("📝  Notes (optional):");
        notesLabel.setStyle("-fx-font-size: 13; -fx-font-weight: bold;");
        TextArea notesArea = new TextArea();
        notesArea.setPromptText("What did you practice today?");
        notesArea.setPrefRowCount(3);
        notesArea.setWrapText(true);
        styleTextArea(notesArea);

        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_RIGHT);

        Button cancelBtn = new Button("Cancel");
        Button logBtn   = new Button("✅ Log Time");
        styleButton(cancelBtn, "#9e9e9e");
        styleButton(logBtn,    "#4CAF50");

        cancelBtn.setOnAction(e -> dialog.close());
        logBtn.setOnAction(e -> {
            try {
                double hours = Double.parseDouble(hoursField.getText().trim());
                if (hours <= 0) { showError("Please enter a positive number"); return; }

                LocalDate sessionDate = sessionDatePicker.getValue() != null
                        ? sessionDatePicker.getValue() : LocalDate.now();

                // 1. Update cumulative progress (existing behaviour)
                Progress updated = progressController.addHours(
                        hobby.getHobbyId(), hours, notesArea.getText().trim());

                // 2. Insert individual session log with date
                ProgressLog log = new ProgressLog(
                        hobby.getHobbyId(), hours, notesArea.getText().trim(), sessionDate);
                progressLogController.create(log);

                if (updated != null) {
                    dialog.close();
                    loadHobbies();
                    updateStats();
                    showSuccess(String.format("✅ Logged %.1f hrs on %s!", hours,
                            sessionDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))));
                } else {
                    showError("Failed to log progress");
                }
            } catch (NumberFormatException ex) {
                showError("Please enter a valid number");
            }
        });

        buttons.getChildren().addAll(cancelBtn, logBtn);
        content.getChildren().addAll(
                title, currentLabel,
                dateLabel, sessionDatePicker,
                hoursLabel, hoursField,
                notesLabel, notesArea,
                buttons);

        Scene scene = new Scene(content, 440, 530);
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
        VBox section = new VBox(20);
        section.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 25;");

        Label title = new Label("⏱️ Progress Tracking");
        title.setStyle("-fx-font-size: 18; -fx-font-weight: bold;");

        // Total hours summary
        Optional<Progress> progressOpt = progressController.findByHobbyId(hobby.getHobbyId());
        double totalHrs = progressOpt.map(Progress::getHoursSpent).orElse(0.0);
        Label hoursLabel = new Label(String.format("Total Hours: %.1f", totalHrs));
        hoursLabel.setStyle("-fx-font-size: 28; -fx-font-weight: bold; -fx-text-fill: #4CAF50;");

        // Session logs indexed by date
        List<ProgressLog> logs = progressLogController.findAllByHobbyId(hobby.getHobbyId());
        Map<LocalDate, Double> dayHours = new HashMap<>();
        for (ProgressLog log : logs) {
            if (log.getLogDate() != null) {
                dayHours.merge(log.getLogDate(), log.getHoursSpent(), Double::sum);
            }
        }

        // Calendar section wrapper with navigation state
        VBox calendarWrapper = new VBox(12);
        final YearMonth[] current = {YearMonth.now()};
        buildCalendarGrid(calendarWrapper, current, dayHours);

        // Recent sessions list
        VBox sessionList = buildSessionList(logs);

        section.getChildren().addAll(title, hoursLabel, calendarWrapper, new Separator(), sessionList);
        return section;
    }

    private void buildCalendarGrid(VBox wrapper, YearMonth[] current,
                                   Map<LocalDate, Double> dayHours) {
        wrapper.getChildren().clear();
        YearMonth ym = current[0];

        // Header row: prev | "March 2026" | next
        HBox nav = new HBox(10);
        nav.setAlignment(Pos.CENTER);
        Button prevBtn = new Button("❮");
        Button nextBtn = new Button("❯");
        for (Button b : new Button[]{prevBtn, nextBtn}) {
            b.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; "
                    + "-fx-background-radius: 20; -fx-padding: 6 16; -fx-font-size: 14; "
                    + "-fx-font-weight: bold; -fx-cursor: hand;");
        }
        Label monthLabel = new Label(ym.format(DateTimeFormatter.ofPattern("MMMM yyyy")));
        monthLabel.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-min-width: 160; "
                + "-fx-alignment: center;");
        prevBtn.setOnAction(e -> { current[0] = current[0].minusMonths(1);
                buildCalendarGrid(wrapper, current, dayHours); });
        nextBtn.setOnAction(e -> { current[0] = current[0].plusMonths(1);
                buildCalendarGrid(wrapper, current, dayHours); });
        nav.getChildren().addAll(prevBtn, monthLabel, nextBtn);

        // Day-of-week header
        GridPane grid = new GridPane();
        grid.setHgap(6);
        grid.setVgap(6);
        String[] dayNames = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        for (int col = 0; col < 7; col++) {
            Label d = new Label(dayNames[col]);
            d.setMinWidth(52);
            d.setAlignment(javafx.geometry.Pos.CENTER);
            d.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: #9e9e9e;");
            grid.add(d, col, 0);
        }

        // Calculate start offset (Mon = 0)
        LocalDate firstDay = ym.atDay(1);
        int startCol = firstDay.getDayOfWeek().getValue() - 1; // Mon=0 ... Sun=6
        int daysInMonth = ym.lengthOfMonth();
        LocalDate today = LocalDate.now();

        int col = startCol;
        int row = 1;
        for (int day = 1; day <= daysInMonth; day++) {
            LocalDate date = ym.atDay(day);
            Double hrs = dayHours.get(date);

            VBox cell = new VBox(2);
            cell.setMinSize(52, 48);
            cell.setMaxSize(52, 48);
            cell.setAlignment(Pos.CENTER);

            Label numLbl = new Label(String.valueOf(day));
            numLbl.setStyle("-fx-font-size: 13;");

            if (hrs != null && hrs > 0) {
                // Day with a session — purple badge
                cell.setStyle("-fx-background-color: #667eea; -fx-background-radius: 12;");
                numLbl.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: white;");
                Label hrsLbl = new Label(String.format("%.1fh", hrs));
                hrsLbl.setStyle("-fx-font-size: 10; -fx-text-fill: #d0d8ff;");
                cell.getChildren().addAll(numLbl, hrsLbl);

                // Tooltip on hover
                double finalHrs = hrs;
                cell.setOnMouseEntered(ev -> cell.setStyle(
                        "-fx-background-color: #5a6fd6; -fx-background-radius: 12;"));
                cell.setOnMouseExited(ev -> cell.setStyle(
                        "-fx-background-color: #667eea; -fx-background-radius: 12;"));
            } else if (date.equals(today)) {
                // Today — light accent
                cell.setStyle("-fx-background-color: #e8eaff; -fx-background-radius: 12;");
                numLbl.setStyle("-fx-font-size: 13; -fx-font-weight: bold; -fx-text-fill: #667eea;");
                cell.getChildren().add(numLbl);
            } else {
                cell.setStyle("-fx-background-color: #f7f8fc; -fx-background-radius: 10;");
                numLbl.setStyle("-fx-font-size: 13; -fx-text-fill: #444;");
                cell.getChildren().add(numLbl);
            }

            grid.add(cell, col, row);
            col++;
            if (col == 7) { col = 0; row++; }
        }

        // Legend
        HBox legend = new HBox(18);
        legend.setAlignment(Pos.CENTER_LEFT);
        legend.setStyle("-fx-padding: 4 0 0 0;");
        HBox sessLegend = makeLegendDot("#667eea", "Session logged");
        HBox todayLegend = makeLegendDot("#e8eaff", "Today");
        legend.getChildren().addAll(sessLegend, todayLegend);

        wrapper.getChildren().addAll(nav, grid, legend);
    }

    private HBox makeLegendDot(String color, String text) {
        HBox box = new HBox(6);
        box.setAlignment(Pos.CENTER_LEFT);
        Region dot = new Region();
        dot.setMinSize(14, 14);
        dot.setMaxSize(14, 14);
        dot.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 7;");
        Label lbl = new Label(text);
        lbl.setStyle("-fx-font-size: 12; -fx-text-fill: #65676b;");
        box.getChildren().addAll(dot, lbl);
        return box;
    }

    private VBox buildSessionList(List<ProgressLog> logs) {
        VBox sessionList = new VBox(8);
        if (logs.isEmpty()) {
            Label none = new Label("No sessions yet — use ➕ Log Time to start! "
                    + "Sessions will appear as purple dots on the calendar.");
            none.setStyle("-fx-font-size: 13; -fx-text-fill: #65676b;");
            none.setWrapText(true);
            sessionList.getChildren().add(none);
        } else {
            Label recentTitle = new Label("📌 Recent Sessions (latest 5)");
            recentTitle.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: #333;");
            sessionList.getChildren().add(recentTitle);
            logs.stream().limit(5).forEach(log -> {
                HBox row = new HBox(12);
                row.setAlignment(Pos.CENTER_LEFT);
                row.setStyle("-fx-background-color: #f5f6ff; -fx-background-radius: 10; -fx-padding: 10 15;");
                Label dateLbl = new Label(log.getLogDate() != null
                        ? log.getLogDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy"))
                        : "Unknown date");
                dateLbl.setStyle("-fx-font-size: 13; -fx-text-fill: #667eea; "
                        + "-fx-font-weight: bold; -fx-min-width: 110;");
                Label hrsLbl = new Label(String.format("%.1f hrs", log.getHoursSpent()));
                hrsLbl.setStyle("-fx-font-size: 13; -fx-font-weight: bold; "
                        + "-fx-text-fill: #4CAF50; -fx-min-width: 60;");
                Label noteLbl = new Label(log.getNotes() != null ? log.getNotes() : "");
                noteLbl.setStyle("-fx-font-size: 12; -fx-text-fill: #65676b;");
                noteLbl.setWrapText(true);
                row.getChildren().addAll(dateLbl, hrsLbl, noteLbl);
                sessionList.getChildren().add(row);
            });
        }
        return sessionList;
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
        
        // Check if milestone is overdue
        boolean isOverdue = !milestone.getIsAchieved() && 
                           milestone.getTargetDate() != null && 
                           milestone.getTargetDate().isBefore(LocalDate.now());
        
        String backgroundColor;
        if (milestone.getIsAchieved()) {
            backgroundColor = "#e8f5e9"; // Green for achieved
        } else if (isOverdue) {
            backgroundColor = "#ffebee"; // Red for overdue
        } else {
            backgroundColor = "#fff3e0"; // Orange for pending
        }
        
        item.setStyle("-fx-background-color: " + backgroundColor + 
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
                (milestone.getIsAchieved() ? "-fx-text-fill: #4CAF50; -fx-strikethrough: true;" : 
                 isOverdue ? "-fx-text-fill: #d32f2f;" : ""));
        
        String dateStr = milestone.getTargetDate() != null ? 
                milestone.getTargetDate().format(DateTimeFormatter.ofPattern("MMM dd, yyyy")) : "No date";
        String status = isOverdue ? " ⚠️ OVERDUE" : "";
        Label dateLabel = new Label("Target: " + dateStr + status);
        dateLabel.setStyle("-fx-font-size: 12; " + 
                (isOverdue ? "-fx-text-fill: #d32f2f; -fx-font-weight: bold;" : "-fx-text-fill: #65676b;"));
        
        textBox.getChildren().addAll(titleLabel, dateLabel);
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button editBtn = new Button("✏️");
        styleSmallButton(editBtn, "#FF9800");
        editBtn.setOnAction(e -> handleEditMilestone(milestone, hobby));
        
        Button deleteBtn = new Button("🗑️");
        styleSmallButton(deleteBtn, "#f44336");
        deleteBtn.setOnAction(e -> {
            milestoneController.delete(milestone.getMilestoneId());
            handleViewHobby(hobby);
            updateStats();
        });
        
        item.getChildren().addAll(checkBox, textBox, spacer, editBtn, deleteBtn);
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
    
    private void handleEditMilestone(Milestone milestone, Hobby hobby) {
        Stage dialog = createModal("Edit Milestone", "✏️");
        VBox content = new VBox(20);
        content.setPadding(new Insets(30));
        content.setStyle("-fx-background-color: white; -fx-background-radius: 20;");
        
        Label title = new Label("Edit Milestone");
        title.setStyle("-fx-font-size: 20; -fx-font-weight: bold;");
        
        TextField titleField = createTextField("Milestone title");
        titleField.setText(milestone.getTitle());
        
        DatePicker datePicker = new DatePicker();
        datePicker.setValue(milestone.getTargetDate());
        datePicker.setPromptText("Target date (optional)");
        datePicker.setPrefWidth(350);
        datePicker.setStyle("-fx-background-color: white; -fx-border-color: #e4e6eb; " +
                "-fx-border-radius: 12; -fx-background-radius: 12; -fx-padding: 10;");
        
        CheckBox achievedCheckBox = new CheckBox("Mark as achieved");
        achievedCheckBox.setSelected(milestone.getIsAchieved());
        achievedCheckBox.setStyle("-fx-font-size: 14; -fx-font-weight: bold;");
        
        HBox buttons = new HBox(10);
        buttons.setAlignment(Pos.CENTER_RIGHT);
        
        Button cancelBtn = new Button("Cancel");
        Button saveBtn = new Button("💾 Save Changes");
        styleButton(cancelBtn, "#9e9e9e");
        styleButton(saveBtn, "#FF9800");
        
        cancelBtn.setOnAction(e -> dialog.close());
        saveBtn.setOnAction(e -> {
            if (titleField.getText().trim().isEmpty()) {
                showError("Please enter a milestone title");
                return;
            }
            
            milestone.setTitle(titleField.getText().trim());
            milestone.setTargetDate(datePicker.getValue());
            milestone.setIsAchieved(achievedCheckBox.isSelected());
            
            Milestone updated = milestoneController.update(milestone);
            if (updated != null) {
                dialog.close();
                handleViewHobby(hobby);
                updateStats();
                loadHobbies();
                showSuccess("Milestone updated!");
            } else {
                showError("Failed to update milestone");
            }
        });
        
        buttons.getChildren().addAll(cancelBtn, saveBtn);
        content.getChildren().addAll(title, new Label("Title:"), titleField, 
                new Label("Target Date:"), datePicker, achievedCheckBox, buttons);
        
        Scene scene = new Scene(content, 400, 400);
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

    // ─────────────────────────────────────────────────────────────────────────
    //  AI COMPANION
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void handleOpenAICompanion() {
        Stage dialog = new Stage();
        dialog.setTitle("🤖 AI Hobby Coach");
        dialog.setWidth(660);
        dialog.setHeight(760);
        dialog.setMinWidth(500);
        dialog.setMinHeight(500);

        String systemContext = buildAISystemContext();
        GeminiService gemini = new GeminiService(systemContext);

        // ── Header ──────────────────────────────────────────────────────────
        HBox header = new HBox(14);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 24, 18, 24));
        header.setStyle("-fx-background-color: linear-gradient(to right, #667eea, #764ba2);");

        Label botIcon = new Label("🤖");
        botIcon.setStyle("-fx-font-size: 30;");

        VBox headerText = new VBox(2);
        Label headerTitle = new Label("AI Hobby Coach");
        headerTitle.setStyle("-fx-font-size: 18; -fx-font-weight: bold; -fx-text-fill: white;");
        Label headerSub = new Label("Powered by Gemini • Ask anything about your hobby journey");
        headerSub.setStyle("-fx-font-size: 12; -fx-text-fill: rgba(255,255,255,0.75);");
        headerText.getChildren().addAll(headerTitle, headerSub);

        header.getChildren().addAll(botIcon, headerText);

        // ── Messages area ────────────────────────────────────────────────────
        VBox messagesBox = new VBox(14);
        messagesBox.setPadding(new Insets(20));
        messagesBox.setStyle("-fx-background-color: #f0f2f5;");

        ScrollPane scrollPane = new ScrollPane(messagesBox);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: #f0f2f5; -fx-background-color: #f0f2f5; " +
                            "-fx-border-color: transparent;");
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        // Auto-scroll to bottom when content grows
        messagesBox.heightProperty().addListener((obs, o, n) -> scrollPane.setVvalue(1.0));

        // ── Input area ───────────────────────────────────────────────────────
        TextField inputField = new TextField();
        inputField.setPromptText("Ask me anything about your hobbies...");
        inputField.setStyle("-fx-background-color: white; -fx-border-color: #dde0e8; " +
                "-fx-border-radius: 25; -fx-background-radius: 25; " +
                "-fx-padding: 12 20; -fx-font-size: 14;");
        HBox.setHgrow(inputField, Priority.ALWAYS);

        Button sendBtn = new Button("Send 🚀");
        sendBtn.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; " +
                "-fx-background-radius: 25; -fx-padding: 12 22; -fx-font-size: 14; " +
                "-fx-font-weight: bold; -fx-cursor: hand;");

        HBox inputArea = new HBox(12);
        inputArea.setAlignment(Pos.CENTER);
        inputArea.setPadding(new Insets(14, 20, 18, 20));
        inputArea.setStyle("-fx-background-color: white; " +
                "-fx-border-color: #e4e6eb; -fx-border-width: 1 0 0 0;");
        inputArea.getChildren().addAll(inputField, sendBtn);

        // ── Main layout ──────────────────────────────────────────────────────
        VBox mainLayout = new VBox(header, scrollPane, inputArea);
        mainLayout.setStyle("-fx-background-color: #f0f2f5;");

        Scene scene = new Scene(mainLayout, 660, 760);
        dialog.setScene(scene);
        dialog.show();

        // ── Send-action logic ────────────────────────────────────────────────
        Runnable sendAction = () -> {
            String text = inputField.getText().trim();
            if (text.isEmpty()) return;

            inputField.clear();
            sendBtn.setDisable(true);

            addMessageBubble(messagesBox, text, true);

            Label typing = new Label("🤖  typing…");
            typing.setStyle("-fx-font-size: 13; -fx-text-fill: #9b59b6; " +
                            "-fx-font-style: italic; -fx-padding: 0 15;");
            messagesBox.getChildren().add(typing);

            Task<String> task = new Task<>() {
                @Override protected String call() throws Exception {
                    return gemini.chat(text);
                }
            };
            task.setOnSucceeded(ev -> {
                messagesBox.getChildren().remove(typing);
                addMessageBubble(messagesBox, task.getValue(), false);
                sendBtn.setDisable(false);
                inputField.requestFocus();
            });
            task.setOnFailed(ev -> {
                messagesBox.getChildren().remove(typing);
                String err = task.getException() != null
                        ? task.getException().getMessage() : "Unknown error";
                addMessageBubble(messagesBox, "❌ " + err, false);
                sendBtn.setDisable(false);
            });
            new Thread(task).start();
        };

        sendBtn.setOnAction(e -> sendAction.run());
        inputField.setOnAction(e -> sendAction.run());

        // ── Initial greeting ─────────────────────────────────────────────────
        sendBtn.setDisable(true);
        Label greetTyping = new Label("🤖  typing…");
        greetTyping.setStyle("-fx-font-size: 13; -fx-text-fill: #9b59b6; " +
                             "-fx-font-style: italic; -fx-padding: 0 15;");
        messagesBox.getChildren().add(greetTyping);

        Task<String> greetTask = new Task<>() {
            @Override protected String call() throws Exception {
                return gemini.chat(
                    "Hello! Greet me warmly and in 2-3 sentences give me a personalized " +
                    "tip or motivation based on my current hobby data. Keep it under 80 words.");
            }
        };
        greetTask.setOnSucceeded(ev -> {
            messagesBox.getChildren().remove(greetTyping);
            addMessageBubble(messagesBox, greetTask.getValue(), false);
            sendBtn.setDisable(false);
            inputField.requestFocus();
        });
        greetTask.setOnFailed(ev -> {
            messagesBox.getChildren().remove(greetTyping);
            addMessageBubble(messagesBox,
                    "👋 Hello! I'm your AI Hobby Coach. Ask me for tips, motivation, " +
                    "practice plans, or anything about your hobbies!", false);
            sendBtn.setDisable(false);
        });
        new Thread(greetTask).start();
    }

    /**
     * Builds a rich system prompt injecting the user's live hobby data.
     */
    private String buildAISystemContext() {
        StringBuilder ctx = new StringBuilder();
        ctx.append("You are an enthusiastic AI hobby coach called 'Hobby Coach'. ");
        ctx.append("Help users improve in their hobbies with personalized tips, practice plans, and motivation. ");
        ctx.append("Be friendly, concise (under 120 words unless asked for more), and use emojis occasionally. ");
        ctx.append("Always reference the user's actual hobby data when it is relevant.\n\n");
        ctx.append("User's current hobby data:\n");

        List<Hobby> hobbies = hobbyController.findByUserId(sessionManager.getUserId());
        if (hobbies.isEmpty()) {
            ctx.append("• No hobbies tracked yet — encourage the user to start.\n");
        } else {
            for (Hobby hobby : hobbies) {
                Optional<Progress> prog = progressController.findByHobbyId(hobby.getHobbyId());
                double hours = prog.map(Progress::getHoursSpent).orElse(0.0);
                List<Milestone> miles = milestoneController.findByHobbyId(hobby.getHobbyId());
                long achieved = miles.stream().filter(Milestone::getIsAchieved).count();
                ctx.append(String.format("• %s (%s): %.1f hrs logged, %d/%d milestones achieved%n",
                        hobby.getName(), hobby.getCategory(),
                        hours, achieved, miles.size()));
            }
        }
        ctx.append("\nBe encouraging, realistic, and actionable in all your responses.");
        return ctx.toString();
    }

    /**
     * Adds a styled message bubble to the chat container.
     * @param isUser true → right-aligned purple bubble; false → left-aligned white bubble with bot icon
     */
    private void addMessageBubble(VBox container, String message, boolean isUser) {
        HBox row = new HBox();
        row.setMaxWidth(Double.MAX_VALUE);

        Label msgLabel = new Label(message);
        msgLabel.setWrapText(true);
        msgLabel.setMaxWidth(430);

        VBox bubble = new VBox(msgLabel);

        if (isUser) {
            row.setAlignment(Pos.CENTER_RIGHT);
            msgLabel.setStyle("-fx-font-size: 14; -fx-text-fill: white;");
            bubble.setStyle("-fx-background-color: #667eea; " +
                    "-fx-background-radius: 18 18 4 18; -fx-padding: 12 18;");
            row.getChildren().add(bubble);
        } else {
            row.setAlignment(Pos.CENTER_LEFT);
            msgLabel.setStyle("-fx-font-size: 14; -fx-text-fill: #1c1e21;");
            bubble.setStyle("-fx-background-color: white; " +
                    "-fx-background-radius: 18 18 18 4; -fx-padding: 12 18; " +
                    "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2);");

            Label icon = new Label("🤖");
            icon.setStyle("-fx-font-size: 22;");
            icon.setAlignment(Pos.TOP_CENTER);

            HBox withIcon = new HBox(10, icon, bubble);
            withIcon.setAlignment(Pos.TOP_LEFT);
            row.getChildren().add(withIcon);
        }

        container.getChildren().add(row);
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  STATISTICS
    // ─────────────────────────────────────────────────────────────────────────

    @FXML
    private void handleOpenStats() {
        List<Hobby> hobbies = hobbyController.findByUserId(sessionManager.getUserId());

        // ── Aggregate data ───────────────────────────────────────────────────
        List<ProgressLog> allLogs = new ArrayList<>();
        Map<String, Double> hoursByHobby    = new LinkedHashMap<>();
        Map<String, Double> hoursByCategory = new LinkedHashMap<>();

        for (Hobby hobby : hobbies) {
            List<ProgressLog> logs = progressLogController.findAllByHobbyId(hobby.getHobbyId());
            allLogs.addAll(logs);
            double hrs = logs.stream().mapToDouble(ProgressLog::getHoursSpent).sum();
            hoursByHobby.put(hobby.getName(), hrs);
            String cat = hobby.getCategory() != null ? hobby.getCategory() : "Other";
            hoursByCategory.merge(cat, hrs, Double::sum);
        }

        int    totalSessions   = allLogs.size();
        double totalHrs        = allLogs.stream().mapToDouble(ProgressLog::getHoursSpent).sum();
        double avgHrsPerSess   = totalSessions > 0 ? totalHrs / totalSessions : 0;
        int    streak          = calculateStreak(allLogs);
        long   activeDays30    = allLogs.stream()
                .filter(l -> l.getLogDate() != null
                          && !l.getLogDate().isBefore(LocalDate.now().minusDays(29)))
                .map(ProgressLog::getLogDate).distinct().count();

        // Day-of-week distribution
        Map<DayOfWeek, Long> dayCount = allLogs.stream()
                .filter(l -> l.getLogDate() != null)
                .collect(Collectors.groupingBy(l -> l.getLogDate().getDayOfWeek(), Collectors.counting()));
        DayOfWeek bestDay = dayCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey).orElse(null);

        // ── Dialog ──────────────────────────────────────────────────────────
        Stage dialog = new Stage();
        dialog.setTitle("📊 Hobby Statistics");
        dialog.setMinWidth(680);
        dialog.setMinHeight(600);

        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background: #f0f2f5; -fx-background-color: #f0f2f5; -fx-border-color: transparent;");

        VBox root = new VBox(20);
        root.setPadding(new Insets(28));
        root.setStyle("-fx-background-color: #f0f2f5;");

        // Title
        Label titleLbl = new Label("📊 Your Hobby Statistics");
        titleLbl.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: #1c1e21;");

        // ── KPI cards ────────────────────────────────────────────────────────
        HBox kpiRow = new HBox(16);
        kpiRow.setAlignment(Pos.CENTER);
        kpiRow.getChildren().addAll(
            makeKpiCard("🗓️", String.valueOf(totalSessions), "Total Sessions",  "#667eea"),
            makeKpiCard("⏱️", String.format("%.1f", avgHrsPerSess), "Avg hrs / Session", "#4CAF50"),
            makeKpiCard("🔥", String.valueOf(streak) + " days", "Current Streak",    "#FF9800"),
            makeKpiCard("📅", activeDays30 + " / 30", "Active Days (30d)",    "#e91e63")
        );

        // ── Top hobbies bar chart ────────────────────────────────────────────
        VBox topHobbiesCard = makeStatCard("🏅 Top Hobbies by Hours");
        List<Map.Entry<String, Double>> sortedHobbies = hoursByHobby.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(6).collect(Collectors.toList());
        double maxHobbyHrs = sortedHobbies.isEmpty() ? 1 :
                sortedHobbies.get(0).getValue();
        String[] barColors = {"#667eea","#4CAF50","#FF9800","#e91e63","#00bcd4","#9c27b0"};
        for (int i = 0; i < sortedHobbies.size(); i++) {
            Map.Entry<String, Double> e = sortedHobbies.get(i);
            topHobbiesCard.getChildren().add(
                makeHorizBar(e.getKey(), e.getValue(), maxHobbyHrs,
                             barColors[i % barColors.length], "hrs"));
        }
        if (sortedHobbies.isEmpty()) {
            topHobbiesCard.getChildren().add(new Label("No sessions logged yet."));
        }

        // ── Hours by category ────────────────────────────────────────────────
        VBox catCard = makeStatCard("🏷️ Hours by Category");
        List<Map.Entry<String, Double>> sortedCats = hoursByCategory.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .collect(Collectors.toList());
        double maxCatHrs = sortedCats.isEmpty() ? 1 : sortedCats.get(0).getValue();
        String[] catColors = {"#4CAF50","#667eea","#FF9800","#e91e63","#00bcd4","#9c27b0","#795548"};
        for (int i = 0; i < sortedCats.size(); i++) {
            Map.Entry<String, Double> e = sortedCats.get(i);
            catCard.getChildren().add(
                makeHorizBar(e.getKey(), e.getValue(), maxCatHrs,
                             catColors[i % catColors.length], "hrs"));
        }
        if (sortedCats.isEmpty()) {
            catCard.getChildren().add(new Label("No sessions logged yet."));
        }

        // ── Day-of-week activity ─────────────────────────────────────────────
        VBox dowCard = makeStatCard("📆 Sessions by Day of Week");
        DayOfWeek[] days = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY,
                            DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY,
                            DayOfWeek.SUNDAY};
        String[] dayLabels = {"Mon","Tue","Wed","Thu","Fri","Sat","Sun"};
        long maxDayCount = dayCount.values().stream().max(Long::compareTo).orElse(1L);
        HBox dowRow = new HBox(10);
        dowRow.setAlignment(Pos.BOTTOM_CENTER);
        for (int i = 0; i < days.length; i++) {
            long count  = dayCount.getOrDefault(days[i], 0L);
            double frac = maxDayCount > 0 ? (double) count / maxDayCount : 0;
            boolean isBest = days[i].equals(bestDay) && count > 0;
            VBox col = new VBox(5);
            col.setAlignment(Pos.BOTTOM_CENTER);
            col.setPrefWidth(60);
            Region bar = new Region();
            bar.setPrefWidth(40);
            bar.setPrefHeight(Math.max(4, frac * 100));
            bar.setStyle("-fx-background-color: " + (isBest ? "#FF9800" : "#667eea")
                    + "; -fx-background-radius: 6 6 0 0;");
            Label cntLbl = new Label(String.valueOf(count));
            cntLbl.setStyle("-fx-font-size: 11; -fx-font-weight: bold; -fx-text-fill: #444;");
            Label dayLbl = new Label(dayLabels[i] + (isBest ? " 🔥" : ""));
            dayLbl.setStyle("-fx-font-size: 11; -fx-text-fill: " + (isBest ? "#FF9800" : "#65676b") + ";");
            col.getChildren().addAll(cntLbl, bar, dayLbl);
            dowRow.getChildren().add(col);
        }
        dowCard.getChildren().add(dowRow);
        if (bestDay != null) {
            Label bestLbl = new Label("🔥 Most active: " + bestDay.name().charAt(0)
                    + bestDay.name().substring(1).toLowerCase());
            bestLbl.setStyle("-fx-font-size: 13; -fx-text-fill: #FF9800; -fx-font-weight: bold;");
            dowCard.getChildren().add(bestLbl);
        }

        // ── Close button ─────────────────────────────────────────────────────
        Button closeBtn = new Button("Close");
        styleButton(closeBtn, "#667eea");
        closeBtn.setMaxWidth(Double.MAX_VALUE);
        closeBtn.setOnAction(e -> dialog.close());

        root.getChildren().addAll(titleLbl, kpiRow, topHobbiesCard, catCard, dowCard, closeBtn);
        scroll.setContent(root);
        dialog.setScene(new Scene(scroll, 700, 680));
        dialog.show();
    }

    /** Card container with a bold title label already added. */
    private VBox makeStatCard(String title) {
        VBox card = new VBox(14);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16;"
                + "-fx-padding: 22; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.08),12,0,0,3);");
        Label lbl = new Label(title);
        lbl.setStyle("-fx-font-size: 16; -fx-font-weight: bold; -fx-text-fill: #1c1e21;");
        card.getChildren().add(lbl);
        return card;
    }

    /** Single horizontal bar row: label | bar | value */
    private HBox makeHorizBar(String label, double value, double maxValue,
                              String color, String unit) {
        HBox row = new HBox(12);
        row.setAlignment(Pos.CENTER_LEFT);

        Label nameLbl = new Label(label);
        nameLbl.setMinWidth(160);
        nameLbl.setMaxWidth(160);
        nameLbl.setWrapText(true);
        nameLbl.setStyle("-fx-font-size: 13; -fx-text-fill: #333;");

        double fraction = maxValue > 0 ? value / maxValue : 0;
        Region bar = new Region();
        bar.setPrefHeight(22);
        bar.setPrefWidth(Math.max(4, fraction * 260));
        bar.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 6;");

        Label valLbl = new Label(String.format("%.1f %s", value, unit));
        valLbl.setStyle("-fx-font-size: 12; -fx-font-weight: bold; -fx-text-fill: " + color + ";");

        row.getChildren().addAll(nameLbl, bar, valLbl);
        return row;
    }

    /** Small KPI card. */
    private VBox makeKpiCard(String icon, String value, String subtitle, String color) {
        VBox card = new VBox(6);
        card.setAlignment(Pos.CENTER);
        card.setPrefWidth(148);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 16;"
                + "-fx-padding: 20 10; -fx-effect: dropshadow(gaussian,rgba(0,0,0,0.08),10,0,0,3);");
        Label iconLbl  = new Label(icon);
        iconLbl.setStyle("-fx-font-size: 28;");
        Label valLbl   = new Label(value);
        valLbl.setStyle("-fx-font-size: 22; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
        Label subLbl   = new Label(subtitle);
        subLbl.setStyle("-fx-font-size: 11; -fx-text-fill: #65676b; -fx-text-alignment: center;");
        subLbl.setWrapText(true);
        subLbl.setMaxWidth(130);
        card.getChildren().addAll(iconLbl, valLbl, subLbl);
        return card;
    }

    /**
     * Returns the current streak: number of consecutive days (ending today or yesterday)
     * that have at least one ProgressLog entry.
     */
    private int calculateStreak(List<ProgressLog> logs) {
        java.util.Set<LocalDate> logDates = logs.stream()
                .filter(l -> l.getLogDate() != null)
                .map(ProgressLog::getLogDate)
                .collect(Collectors.toSet());

        if (logDates.isEmpty()) return 0;

        LocalDate cursor = LocalDate.now();
        // Allow streak if today OR yesterday has a log (so it doesn't break at midnight)
        if (!logDates.contains(cursor)) {
            cursor = cursor.minusDays(1);
        }
        int streak = 0;
        while (logDates.contains(cursor)) {
            streak++;
            cursor = cursor.minusDays(1);
        }
        return streak;
    }
}


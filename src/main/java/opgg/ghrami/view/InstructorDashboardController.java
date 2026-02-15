package opgg.ghrami.view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import opgg.ghrami.controller.BookingController;
import opgg.ghrami.controller.ClassController;
import opgg.ghrami.controller.ClassProviderController;
import opgg.ghrami.model.*;
import opgg.ghrami.util.SessionManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class InstructorDashboardController {
    
    @FXML private Label instructorNameLabel;
    @FXML private Label ratingLabel;
    @FXML private Label activeClassesLabel;
    @FXML private Label totalStudentsLabel;
    @FXML private Label totalRevenueLabel;
    @FXML private Label pendingBookingsLabel;
    @FXML private Label totalEarningsLabel;
    @FXML private Label pendingRevenueLabel;
    @FXML private VBox classesContainer;
    @FXML private VBox bookingsContainer;
    @FXML private VBox revenueBreakdownContainer;
    @FXML private ComboBox<String> bookingStatusCombo;
    @FXML private TabPane mainTabPane;
    
    private final ClassProviderController providerController = ClassProviderController.getInstance();
    private final ClassController classController = ClassController.getInstance();
    private final BookingController bookingController = BookingController.getInstance();
    
    private ClassProvider currentProvider;
    private Long providerId;
    
    @FXML
    public void initialize() {
        loadProviderInfo();
        setupBookingStatusCombo();
        loadDashboardData();
    }
    
    private void loadProviderInfo() {
        Long userId = SessionManager.getInstance().getUserId();
        if (userId == null) {
            System.err.println("InstructorDashboard: User ID is null");
            showAlert("Not Logged In", "Please log in to access instructor dashboard");
            return;
        }
        
        System.out.println("InstructorDashboard: Loading provider info for user ID: " + userId);
        currentProvider = providerController.getByUserId(userId);
        
        if (currentProvider == null) {
            System.err.println("InstructorDashboard: No provider found for user ID: " + userId);
            showAlert("Not an Instructor", 
                     "You are not registered as an instructor.\n\nPlease apply to become an instructor first.");
            return;
        }
        
        System.out.println("InstructorDashboard: Provider found - ID: " + currentProvider.getProviderId() + 
                          ", Verified: " + currentProvider.isVerified());
        
        if (!currentProvider.isVerified()) {
            System.err.println("InstructorDashboard: Provider not verified");
            showAlert("Pending Verification", 
                     "Your instructor application is pending admin approval.\n\n" +
                     "You will be able to access the instructor dashboard once an admin verifies your application.");
            return;
        }
        
        providerId = currentProvider.getProviderId();
        instructorNameLabel.setText(currentProvider.getUsername());
        ratingLabel.setText(String.format("⭐ %.1f", currentProvider.getRating()));
        System.out.println("InstructorDashboard: Successfully loaded provider info");
    }
    
    private void setupBookingStatusCombo() {
        bookingStatusCombo.getItems().addAll("All", "Scheduled", "Completed", "Cancelled");
        bookingStatusCombo.setValue("All");
    }
    
    private void loadDashboardData() {
        if (providerId == null) return;
        
        // Load all sections
        loadStats();
        loadMyClasses();
        loadAllBookings();
        loadRevenue();
    }
    
    private void loadStats() {
        if (providerId == null) return;
        
        // Get active classes
        List<ClassEntity> classes = classController.getByProviderId(providerId);
        activeClassesLabel.setText(String.valueOf(classes.size()));
        
        // Get all bookings
        List<Booking> allBookings = bookingController.getByProviderId(providerId);
        
        // Count total unique students
        long uniqueStudents = allBookings.stream()
                                        .map(Booking::getUserId)
                                        .distinct()
                                        .count();
        totalStudentsLabel.setText(String.valueOf(uniqueStudents));
        
        // Total revenue (paid bookings)
        double totalRevenue = bookingController.getProviderRevenue(providerId, null);
        totalRevenueLabel.setText(String.format("%.2f TND", totalRevenue));
        
        // Pending bookings count
        List<Booking> pendingBookings = bookingController.getByProviderId(providerId).stream()
                .filter(b -> b.getStatus() == BookingStatus.SCHEDULED)
                .collect(Collectors.toList());
        pendingBookingsLabel.setText(String.valueOf(pendingBookings.size()));
    }
    
    @FXML
    private void loadMyClasses() {
        if (providerId == null) return;
        
        classesContainer.getChildren().clear();
        List<ClassEntity> classes = classController.getByProviderId(providerId);
        
        if (classes.isEmpty()) {
            Label emptyLabel = new Label("No classes yet. Create your first class!");
            emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: gray;");
            classesContainer.getChildren().add(emptyLabel);
            return;
        }
        
        for (ClassEntity classEntity : classes) {
            classesContainer.getChildren().add(createClassCard(classEntity));
        }
    }
    
    private VBox createClassCard(ClassEntity classEntity) {
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: white; -fx-border-color: transparent; -fx-border-radius: 15; " +
                     "-fx-background-radius: 15; -fx-padding: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 5);");
        
        // Header with title and actions
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label title = new Label(classEntity.getTitle());
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1c1e21;");
        
        Label categoryBadge = new Label(classEntity.getCategory());
        categoryBadge.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; -fx-padding: 5 12; " +
                              "-fx-background-radius: 15; -fx-font-size: 12px; -fx-font-weight: bold;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button editBtn = new Button("✏️ Edit");
        editBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-padding: 8 18; -fx-cursor: hand; " +
                        "-fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 13;");
        editBtn.setOnAction(e -> handleEditClass(classEntity));
        
        Button deleteBtn = new Button("🗑️ Delete");
        deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 8 18; -fx-cursor: hand; " +
                          "-fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 13;");
        deleteBtn.setOnAction(e -> handleDeleteClass(classEntity));
        
        header.getChildren().addAll(title, categoryBadge, spacer, editBtn, deleteBtn);
        
        Separator sep1 = new Separator();
        sep1.setStyle("-fx-background-color: #e4e6eb;");
        
        // Description
        Label description = new Label(classEntity.getDescription());
        description.setWrapText(true);
        description.setStyle("-fx-font-size: 14px; -fx-text-fill: #65676b;");
        
        // Details row
        HBox details = new HBox(20);
        details.setAlignment(Pos.CENTER_LEFT);
        details.setStyle("-fx-background-color: #f0f2f5; -fx-background-radius: 10; -fx-padding: 12 15;");
        
        Label priceLabel = new Label("💰 " + String.format("%.2f TND", classEntity.getPrice()));
        priceLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #27ae60; -fx-font-size: 14;");
        
        Label durationLabel = new Label("⏱️ " + classEntity.getDuration() + " min");
        durationLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 13;");
        
        Label enrollmentLabel = new Label("👥 " + classEntity.getCurrentEnrollment() + "/" + 
                                         classEntity.getMaxParticipants() + " enrolled");
        enrollmentLabel.setStyle("-fx-font-weight: 600; -fx-font-size: 13;");
        
        String availabilityText = classEntity.hasAvailableSpots() ? 
                                 "✅ " + classEntity.getAvailableSpots() + " spots left" : 
                                 "❌ Full";
        Label availabilityLabel = new Label(availabilityText);
        availabilityLabel.setStyle(classEntity.hasAvailableSpots() ? 
                                   "-fx-text-fill: #27ae60; -fx-font-weight: bold;" : 
                                   "-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        
        details.getChildren().addAll(priceLabel, durationLabel, enrollmentLabel, availabilityLabel);
        
        // View bookings button
        Button viewBookingsBtn = new Button("👥 View Students");
        viewBookingsBtn.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-padding: 10 20; " +
                                "-fx-background-radius: 20; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 13;");
        viewBookingsBtn.setOnAction(e -> showClassBookings(classEntity));
        
        card.getChildren().addAll(header, sep1, description, details, viewBookingsBtn);
        
        return card;
    }
    
    @FXML
    private void handleCreateClass() {
        Dialog<ClassEntity> dialog = new Dialog<>();
        dialog.setTitle("Create New Class");
        dialog.setHeaderText("Enter class details");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        TextField titleField = new TextField();
        titleField.setPromptText("Class title");
        
        TextArea descArea = new TextArea();
        descArea.setPromptText("Description");
        descArea.setPrefRowCount(3);
        
        TextField categoryField = new TextField();
        categoryField.setPromptText("Category (e.g., fitness, tech, art)");
        
        TextField priceField = new TextField();
        priceField.setPromptText("Price (TND)");
        
        TextField durationField = new TextField();
        durationField.setPromptText("Duration (minutes)");
        
        TextField maxParticipantsField = new TextField();
        maxParticipantsField.setPromptText("Max participants");
        
        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descArea, 1, 1);
        grid.add(new Label("Category:"), 0, 2);
        grid.add(categoryField, 1, 2);
        grid.add(new Label("Price:"), 0, 3);
        grid.add(priceField, 1, 3);
        grid.add(new Label("Duration:"), 0, 4);
        grid.add(durationField, 1, 4);
        grid.add(new Label("Max Students:"), 0, 5);
        grid.add(maxParticipantsField, 1, 5);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try {
                    ClassEntity newClass = new ClassEntity();
                    newClass.setProviderId(providerId);
                    newClass.setTitle(titleField.getText().trim());
                    newClass.setDescription(descArea.getText().trim());
                    newClass.setCategory(categoryField.getText().trim());
                    newClass.setPrice(Double.parseDouble(priceField.getText().trim()));
                    newClass.setDuration(Integer.parseInt(durationField.getText().trim()));
                    newClass.setMaxParticipants(Integer.parseInt(maxParticipantsField.getText().trim()));
                    
                    return newClass;
                } catch (NumberFormatException e) {
                    showAlert("Invalid Input", "Please enter valid numbers for price, duration, and max participants");
                    return null;
                }
            }
            return null;
        });
        
        Optional<ClassEntity> result = dialog.showAndWait();
        result.ifPresent(classEntity -> {
            if (classEntity.getTitle().isEmpty() || classEntity.getDescription().isEmpty()) {
                showAlert("Invalid Input", "Title and description are required");
                return;
            }
            
            boolean success = classController.create(classEntity);
            if (success) {
                showAlert("Success", "Class created successfully!");
                loadMyClasses();
                loadStats();
            } else {
                showAlert("Error", "Failed to create class");
            }
        });
    }
    
    private void handleEditClass(ClassEntity classEntity) {
        Dialog<ClassEntity> dialog = new Dialog<>();
        dialog.setTitle("Edit Class");
        dialog.setHeaderText("Update class details");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));
        
        TextField titleField = new TextField(classEntity.getTitle());
        TextArea descArea = new TextArea(classEntity.getDescription());
        descArea.setPrefRowCount(3);
        TextField categoryField = new TextField(classEntity.getCategory());
        TextField priceField = new TextField(String.valueOf(classEntity.getPrice()));
        TextField durationField = new TextField(String.valueOf(classEntity.getDuration()));
        TextField maxParticipantsField = new TextField(String.valueOf(classEntity.getMaxParticipants()));
        
        grid.add(new Label("Title:"), 0, 0);
        grid.add(titleField, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(descArea, 1, 1);
        grid.add(new Label("Category:"), 0, 2);
        grid.add(categoryField, 1, 2);
        grid.add(new Label("Price:"), 0, 3);
        grid.add(priceField, 1, 3);
        grid.add(new Label("Duration:"), 0, 4);
        grid.add(durationField, 1, 4);
        grid.add(new Label("Max Students:"), 0, 5);
        grid.add(maxParticipantsField, 1, 5);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try {
                    classEntity.setTitle(titleField.getText().trim());
                    classEntity.setDescription(descArea.getText().trim());
                    classEntity.setCategory(categoryField.getText().trim());
                    classEntity.setPrice(Double.parseDouble(priceField.getText().trim()));
                    classEntity.setDuration(Integer.parseInt(durationField.getText().trim()));
                    classEntity.setMaxParticipants(Integer.parseInt(maxParticipantsField.getText().trim()));
                    return classEntity;
                } catch (NumberFormatException e) {
                    showAlert("Invalid Input", "Please enter valid numbers");
                    return null;
                }
            }
            return null;
        });
        
        Optional<ClassEntity> result = dialog.showAndWait();
        result.ifPresent(updatedClass -> {
            boolean success = classController.update(updatedClass);
            if (success) {
                showAlert("Success", "Class updated successfully!");
                loadMyClasses();
            } else {
                showAlert("Error", "Failed to update class");
            }
        });
    }
    
    private void handleDeleteClass(ClassEntity classEntity) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Delete Class");
        confirmation.setHeaderText("Are you sure?");
        confirmation.setContentText("This will delete the class and all its bookings. This action cannot be undone.");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = classController.delete(classEntity.getClassId());
            if (success) {
                showAlert("Success", "Class deleted successfully");
                loadMyClasses();
                loadStats();
                loadAllBookings();
            } else {
                showAlert("Error", "Failed to delete class");
            }
        }
    }
    
    private void showClassBookings(ClassEntity classEntity) {
        List<Booking> bookings = bookingController.getByClassId(classEntity.getClassId());
        
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Class Bookings");
        dialog.setHeaderText("Students for: " + classEntity.getTitle());
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        
        if (bookings.isEmpty()) {
            Label noBookings = new Label("No bookings yet");
            noBookings.setStyle("-fx-font-size: 14px; -fx-text-fill: gray;");
            content.getChildren().add(noBookings);
        } else {
            for (Booking booking : bookings) {
                VBox bookingCard = createBookingCardSimple(booking);
                content.getChildren().add(bookingCard);
            }
        }
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400);
        
        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }
    
    @FXML
    private void loadAllBookings() {
        if (providerId == null) return;
        
        bookingsContainer.getChildren().clear();
        List<Booking> bookings = bookingController.getByProviderId(providerId);
        
        if (bookings.isEmpty()) {
            Label emptyLabel = new Label("No bookings yet");
            emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: gray;");
            bookingsContainer.getChildren().add(emptyLabel);
            return;
        }
        
        for (Booking booking : bookings) {
            bookingsContainer.getChildren().add(createBookingCardDetailed(booking));
        }
    }
    
    @FXML
    private void filterBookings() {
        if (providerId == null) return;
        
        String statusFilter = bookingStatusCombo.getValue();
        final BookingStatus status = "All".equals(statusFilter) ? null : BookingStatus.fromString(statusFilter.toUpperCase());
        
        bookingsContainer.getChildren().clear();
        List<Booking> bookings;
        if (status == null) {
            bookings = bookingController.getByProviderId(providerId);
        } else {
            bookings = bookingController.getByProviderId(providerId).stream()
                    .filter(b -> b.getStatus() == status)
                    .collect(Collectors.toList());
        }
        
        if (bookings.isEmpty()) {
            Label emptyLabel = new Label("No bookings with this status");
            emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: gray;");
            bookingsContainer.getChildren().add(emptyLabel);
            return;
        }
        
        for (Booking booking : bookings) {
            bookingsContainer.getChildren().add(createBookingCardDetailed(booking));
        }
    }
    
    private VBox createBookingCardSimple(Booking booking) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; -fx-border-color: transparent; -fx-border-radius: 12; " +
                     "-fx-background-radius: 12; -fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 3);");
        
        Label studentLabel = new Label("👤 " + booking.getUserFullName() + " (@" + booking.getUsername() + ")");
        studentLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1c1e21;");
        
        HBox statusRow = new HBox(12);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        Label statusLabel = new Label(booking.getStatus().getValue());
        statusLabel.setStyle(getStatusStyle(booking.getStatus()));
        Label paymentLabel = new Label(booking.getPaymentStatus().getValue());
        paymentLabel.setStyle(getPaymentStatusStyle(booking.getPaymentStatus()));
        statusRow.getChildren().addAll(statusLabel, new Label("•"), paymentLabel);
        
        Label dateLabel = new Label("Booked: " + booking.getBookingDate().toString());
        dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #65676b;");
        
        card.getChildren().addAll(studentLabel, statusRow, dateLabel);
        
        return card;
    }
    
    private VBox createBookingCardDetailed(Booking booking) {
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: white; -fx-border-color: transparent; -fx-border-radius: 15; " +
                     "-fx-background-radius: 15; -fx-padding: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 5);");
        
        // Header
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label classLabel = new Label(booking.getClassTitle());
        classLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1c1e21;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label priceLabel = new Label(String.format("%.2f TND", booking.getTotalAmount()));
        priceLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
        
        header.getChildren().addAll(classLabel, spacer, priceLabel);
        
        Separator sep1 = new Separator();
        sep1.setStyle("-fx-background-color: #e4e6eb;");
        
        // Student info
        Label studentLabel = new Label("👤 " + booking.getUserFullName() + " (@" + booking.getUsername() + ")");
        studentLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: #1c1e21;");
        
        Label emailLabel = new Label("📧 " + booking.getUserEmail());
        emailLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #65676b;");
        
        // Status row
        HBox statusRow = new HBox(20);
        statusRow.setAlignment(Pos.CENTER_LEFT);
        statusRow.setStyle("-fx-background-color: #f0f2f5; -fx-background-radius: 10; -fx-padding: 10 15;");
        
        Label statusLabel = new Label("Status: " + booking.getStatus().getValue());
        statusLabel.setStyle(getStatusStyle(booking.getStatus()));
        
        Label paymentLabel = new Label("Payment: " + booking.getPaymentStatus().getValue());
        paymentLabel.setStyle(getPaymentStatusStyle(booking.getPaymentStatus()));
        
        Label dateLabel = new Label("📅 " + booking.getBookingDate().toString());
        dateLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 600;");
        
        statusRow.getChildren().addAll(statusLabel, new Label("•"), paymentLabel, new Label("•"), dateLabel);
        
        // Action buttons
        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_RIGHT);
        
        if (booking.getStatus() == BookingStatus.SCHEDULED) {
            Button completeBtn = new Button("✅ Mark Complete");
            completeBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 10 20; " +
                                "-fx-background-radius: 20; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 13;");
            completeBtn.setOnAction(e -> handleCompleteBooking(booking));
            actions.getChildren().add(completeBtn);
        }
        
        card.getChildren().addAll(header, sep1, studentLabel, emailLabel, statusRow, actions);
        
        return card;
    }
    
    private void handleCompleteBooking(Booking booking) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Complete Booking");
        confirmation.setHeaderText("Mark this class as completed?");
        confirmation.setContentText("Student: " + booking.getUserFullName());
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = bookingController.complete(booking.getBookingId());
            if (success) {
                showAlert("Success", "Booking marked as completed");
                loadAllBookings();
                loadStats();
            } else {
                showAlert("Error", "Failed to update booking");
            }
        }
    }
    
    private void loadRevenue() {
        if (providerId == null) return;
        
        // Total paid revenue
        double paidRevenue = bookingController.getProviderRevenueByPaymentStatus(providerId, PaymentStatus.PAID);
        totalEarningsLabel.setText(String.format("%.2f TND", paidRevenue));
        
        // Pending revenue
        double pendingRevenue = bookingController.getProviderRevenueByPaymentStatus(providerId, PaymentStatus.PENDING);
        pendingRevenueLabel.setText(String.format("%.2f TND", pendingRevenue));
        
        // Revenue breakdown by class
        loadRevenueBreakdown();
    }
    
    private void loadRevenueBreakdown() {
        revenueBreakdownContainer.getChildren().clear();
        
        List<ClassEntity> classes = classController.getByProviderId(providerId);
        
        if (classes.isEmpty()) {
            Label emptyLabel = new Label("No classes yet");
            emptyLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: gray;");
            revenueBreakdownContainer.getChildren().add(emptyLabel);
            return;
        }
        
        for (ClassEntity classEntity : classes) {
            List<Booking> classBookings = bookingController.getByClassId(classEntity.getClassId());
            
            double paidAmount = classBookings.stream()
                    .filter(b -> b.getPaymentStatus() == PaymentStatus.PAID)
                    .mapToDouble(Booking::getTotalAmount)
                    .sum();
            
            double pendingAmount = classBookings.stream()
                    .filter(b -> b.getPaymentStatus() == PaymentStatus.PENDING)
                    .mapToDouble(Booking::getTotalAmount)
                    .sum();
            
            VBox revenueCard = createRevenueCard(classEntity, paidAmount, pendingAmount, classBookings.size());
            revenueBreakdownContainer.getChildren().add(revenueCard);
        }
    }
    
    private VBox createRevenueCard(ClassEntity classEntity, double paid, double pending, int totalBookings) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-border-color: transparent; -fx-border-radius: 12; " +
                     "-fx-background-radius: 12; -fx-padding: 15; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 3);");
        
        Label titleLabel = new Label(classEntity.getTitle());
        titleLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1c1e21;");
        
        HBox amounts = new HBox(15);
        amounts.setAlignment(Pos.CENTER_LEFT);
        amounts.setStyle("-fx-background-color: #f0f2f5; -fx-background-radius: 8; -fx-padding: 8 12;");
        
        Label paidLabel = new Label(String.format("💵 Paid: %.2f TND", paid));
        paidLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 13;");
        
        Label pendingLabel = new Label(String.format("⏳ Pending: %.2f TND", pending));
        pendingLabel.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold; -fx-font-size: 13;");
        
        Label bookingsLabel = new Label("📊 " + totalBookings + " bookings");
        bookingsLabel.setStyle("-fx-text-fill: #65676b; -fx-font-weight: 600; -fx-font-size: 13;");
        
        amounts.getChildren().addAll(paidLabel, new Label("•"), pendingLabel, new Label("•"), bookingsLabel);
        
        card.getChildren().addAll(titleLabel, amounts);
        
        return card;
    }
    
    private String getStatusStyle(BookingStatus status) {
        return switch (status) {
            case SCHEDULED -> "-fx-background-color: #e3f2fd; -fx-text-fill: #2196F3; -fx-font-weight: bold; -fx-padding: 5 10; -fx-background-radius: 12; -fx-font-size: 12;";
            case COMPLETED -> "-fx-background-color: #e8f5e9; -fx-text-fill: #4CAF50; -fx-font-weight: bold; -fx-padding: 5 10; -fx-background-radius: 12; -fx-font-size: 12;";
            case CANCELLED -> "-fx-background-color: #ffebee; -fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-padding: 5 10; -fx-background-radius: 12; -fx-font-size: 12;";
        };
    }
    
    private String getPaymentStatusStyle(PaymentStatus status) {
        return switch (status) {
            case PENDING -> "-fx-background-color: #fff3e0; -fx-text-fill: #ff9800; -fx-font-weight: bold; -fx-padding: 5 10; -fx-background-radius: 12; -fx-font-size: 12;";
            case PAID -> "-fx-background-color: #e8f5e9; -fx-text-fill: #4CAF50; -fx-font-weight: bold; -fx-padding: 5 10; -fx-background-radius: 12; -fx-font-size: 12;";
            case REFUNDED -> "-fx-background-color: #f5f5f5; -fx-text-fill: #95a5a6; -fx-font-weight: bold; -fx-padding: 5 10; -fx-background-radius: 12; -fx-font-size: 12;";
        };
    }
    
    @FXML
    private void handleBack() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/ClassMarketplace.fxml"));
            Scene scene = new Scene(loader.load(), 1400, 900);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            
            Stage stage = (Stage) instructorNameLabel.getScene().getWindow();
            stage.setScene(scene);
            stage.setTitle("Ghrami - Class Marketplace");
        } catch (Exception e) {
            System.err.println("Error loading Class Marketplace: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}

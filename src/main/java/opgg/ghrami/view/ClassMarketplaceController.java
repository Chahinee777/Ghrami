package opgg.ghrami.view;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import opgg.ghrami.controller.BookingController;
import opgg.ghrami.controller.ClassController;
import opgg.ghrami.controller.ClassProviderController;
import opgg.ghrami.model.Booking;
import opgg.ghrami.model.BookingStatus;
import opgg.ghrami.model.ClassEntity;
import opgg.ghrami.model.ClassProvider;
import opgg.ghrami.model.PaymentStatus;
import opgg.ghrami.util.SessionManager;

import java.util.List;
import java.util.Optional;

public class ClassMarketplaceController {
    
    @FXML private TextField searchField;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private TextField maxPriceField;
    @FXML private ComboBox<String> sortCombo;
    @FXML private Label resultsLabel;
    @FXML private VBox classesContainer;
    @FXML private Button becomeInstructorBtn;
    @FXML private Button myBookingsBtn;
    
    private final ClassController classController = ClassController.getInstance();
    private final BookingController bookingController = BookingController.getInstance();
    private final ClassProviderController providerController = ClassProviderController.getInstance();
    
    private List<ClassEntity> currentClasses;
    
    @FXML
    public void initialize() {
        setupCategoryCombo();
        setupSortCombo();
        setupButtonActions();
        loadAllClasses();
        checkInstructorStatus();
    }
    
    private void checkInstructorStatus() {
        Long userId = SessionManager.getInstance().getUserId();
        if (userId != null && providerController.isUserProvider(userId)) {
            // Check if verified
            if (providerController.isUserVerifiedProvider(userId)) {
                becomeInstructorBtn.setText("📊 Tableau de Bord Instructeur");
                becomeInstructorBtn.setOnAction(e -> handleInstructorDashboard());
            } else {
                becomeInstructorBtn.setText("⏳ Application Pending");
                becomeInstructorBtn.setOnAction(e -> showAlert("Pending Verification", 
                    "Your instructor application is being reviewed by an admin.\n\n" +
                    "You will be notified once your application is approved."));
            }
        }
    }
    
    private void handleInstructorDashboard() {
        try {
            Stage stage = (Stage) searchField.getScene().getWindow();
            double width = stage.getWidth();
            double height = stage.getHeight();
            boolean wasMaximized = stage.isMaximized();
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/InstructorDashboard.fxml"));
            Scene scene = new Scene(loader.load(), width, height);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            
            stage.setScene(scene);
            stage.setTitle("Ghrami - Instructor Dashboard");
            if (wasMaximized) {
                stage.setMaximized(true);
            }
        } catch (Exception e) {
            System.err.println("Error loading Instructor Dashboard: " + e.getMessage());
            e.printStackTrace();
            showAlert("Error", "Unable to open Instructor Dashboard");
        }
    }
    
    private void setupCategoryCombo() {
        categoryCombo.getItems().add("All Categories");
        List<String> categories = classController.getCategories();
        categoryCombo.getItems().addAll(categories);
        categoryCombo.setValue("All Categories");
    }
    
    private void setupSortCombo() {
        sortCombo.getItems().addAll("Most Popular", "Price: Low to High", "Price: High to Low", "Newest First");
        sortCombo.setValue("Most Popular");
        sortCombo.setOnAction(e -> applySorting());
    }
    
    private void setupButtonActions() {
        becomeInstructorBtn.setOnAction(e -> handleBecomeInstructor());
        myBookingsBtn.setOnAction(e -> handleMyBookings());
    }
    
    private void loadAllClasses() {
        currentClasses = classController.getAll(null, null, null);
        displayClasses(currentClasses);
    }
    
    @FXML
    private void handleSearch() {
        String keyword = searchField.getText().trim();
        if (keyword.isEmpty()) {
            loadAllClasses();
            return;
        }
        
        currentClasses = classController.search(keyword);
        displayClasses(currentClasses);
    }
    
    @FXML
    private void handleFilter() {
        String category = categoryCombo.getValue();
        if ("All Categories".equals(category)) {
            category = null;
        }
        
        Double maxPrice = null;
        String priceText = maxPriceField.getText().trim();
        if (!priceText.isEmpty()) {
            try {
                maxPrice = Double.parseDouble(priceText);
            } catch (NumberFormatException e) {
                showAlert("Invalid Price", "Please enter a valid price");
                return;
            }
        }
        
        currentClasses = classController.getAll(category, null, maxPrice);
        displayClasses(currentClasses);
    }
    
    @FXML
    private void handleClearFilters() {
        searchField.clear();
        categoryCombo.setValue("All Categories");
        maxPriceField.clear();
        sortCombo.setValue("Most Popular");
        loadAllClasses();
    }
    
    private void applySorting() {
        if (currentClasses == null || currentClasses.isEmpty()) {
            return;
        }
        
        String sortOption = sortCombo.getValue();
        switch (sortOption) {
            case "Price: Low to High":
                currentClasses.sort((c1, c2) -> Double.compare(c1.getPrice(), c2.getPrice()));
                break;
            case "Price: High to Low":
                currentClasses.sort((c1, c2) -> Double.compare(c2.getPrice(), c1.getPrice()));
                break;
            case "Most Popular":
                currentClasses.sort((c1, c2) -> Integer.compare(c2.getCurrentEnrollment(), c1.getCurrentEnrollment()));
                break;
        }
        
        displayClasses(currentClasses);
    }
    
    private void displayClasses(List<ClassEntity> classes) {
        classesContainer.getChildren().clear();
        
        if (classes == null || classes.isEmpty()) {
            Label noResultsLabel = new Label("No classes found");
            noResultsLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: gray;");
            classesContainer.getChildren().add(noResultsLabel);
            resultsLabel.setText("No results");
            return;
        }
        
        resultsLabel.setText(classes.size() + " classes found");
        
        for (ClassEntity classEntity : classes) {
            classesContainer.getChildren().add(createClassCard(classEntity));
        }
    }
    
    private VBox createClassCard(ClassEntity classEntity) {
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: white; -fx-border-color: transparent; -fx-border-radius: 15; " +
                     "-fx-background-radius: 15; -fx-padding: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 5);");
        
        // Title and Category
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label titleLabel = new Label(classEntity.getTitle());
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1c1e21;");
        
        Label categoryBadge = new Label(classEntity.getCategory());
        categoryBadge.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; -fx-padding: 5 12; " +
                              "-fx-background-radius: 15; -fx-font-size: 12px; -fx-font-weight: bold;");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label priceLabel = new Label(String.format("%.2f TND", classEntity.getPrice()));
        priceLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
        
        header.getChildren().addAll(titleLabel, categoryBadge, spacer, priceLabel);
        
        // Instructor Info
        HBox instructorBox = new HBox(8);
        instructorBox.setAlignment(Pos.CENTER_LEFT);
        Label instructorLabel = new Label("👨‍🏫 " + classEntity.getProviderName());
        instructorLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #65676b; -fx-font-weight: 600;");
        
        if (classEntity.getProviderRating() > 0) {
            Label ratingLabel = new Label(String.format("⭐ %.1f", classEntity.getProviderRating()));
            ratingLabel.setStyle("-fx-font-size: 14px; -fx-text-fill: #f39c12; -fx-font-weight: bold;");
            instructorBox.getChildren().addAll(instructorLabel, new Label("•"), ratingLabel);
        } else {
            instructorBox.getChildren().add(instructorLabel);
        }
        
        Separator sep1 = new Separator();
        sep1.setStyle("-fx-background-color: #e4e6eb;");
        
        // Description
        Text description = new Text(classEntity.getDescription());
        description.setWrappingWidth(900);
        description.setStyle("-fx-font-size: 14px; -fx-fill: #65676b;");
        
        // Details Row
        HBox details = new HBox(20);
        details.setAlignment(Pos.CENTER_LEFT);
        details.setStyle("-fx-background-color: #f0f2f5; -fx-background-radius: 10; -fx-padding: 12 15;");
        
        Label durationLabel = new Label("⏱️ " + classEntity.getDuration() + " min");
        durationLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #1c1e21;");
        
        Label capacityLabel = new Label("👥 " + classEntity.getAvailableSpots() + "/" + classEntity.getMaxParticipants() + " spots");
        capacityLabel.setStyle("-fx-font-size: 13px; -fx-font-weight: 600; -fx-text-fill: #1c1e21;");
        
        if (!classEntity.hasAvailableSpots()) {
            Label fullLabel = new Label("❌ FULL");
            fullLabel.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 5 12; " +
                              "-fx-background-radius: 15; -fx-font-size: 12px; -fx-font-weight: bold;");
            details.getChildren().addAll(durationLabel, capacityLabel, fullLabel);
        } else {
            Label availableLabel = new Label("✅ Available");
            availableLabel.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 5 12; " +
                                   "-fx-background-radius: 15; -fx-font-size: 12px; -fx-font-weight: bold;");
            details.getChildren().addAll(durationLabel, capacityLabel, availableLabel);
        }
        
        // Action Buttons
        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_RIGHT);
        
        Button viewBtn = new Button("👁️ View Details");
        viewBtn.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; -fx-padding: 10 20; " +
                        "-fx-background-radius: 20; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 13;");
        viewBtn.setOnAction(e -> showClassDetails(classEntity));
        
        Button bookBtn = new Button("📚 Book Now");
        bookBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 10 20; " +
                        "-fx-background-radius: 20; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 13;");
        
        // Disable if no spots available OR if user is the instructor
        Long currentUserId = SessionManager.getInstance().getUserId();
        boolean isOwnClass = false;
        if (currentUserId != null) {
            ClassProvider provider = providerController.getById(classEntity.getProviderId());
            isOwnClass = (provider != null && provider.getUserId().equals(currentUserId));
        }
        
        if (isOwnClass) {
            bookBtn.setText("🎓 Your Class");
            bookBtn.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-padding: 10 20; " +
                            "-fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 13;");
            bookBtn.setDisable(true);
        } else {
            bookBtn.setDisable(!classEntity.hasAvailableSpots());
        }
        bookBtn.setOnAction(e -> handleBookClass(classEntity));
        
        actions.getChildren().addAll(viewBtn, bookBtn);
        
        card.getChildren().addAll(header, instructorBox, sep1, description, details, actions);
        
        return card;
    }
    
    private void showClassDetails(ClassEntity classEntity) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Class Details");
        dialog.setHeaderText(classEntity.getTitle());
        
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setStyle("-fx-background-color: white;");
        
        // Category and Price
        HBox topInfo = new HBox(20);
        Label categoryLabel = new Label("📚 Category: " + classEntity.getCategory());
        Label priceLabel = new Label("💰 Price: " + String.format("%.2f TND", classEntity.getPrice()));
        priceLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #27ae60; -fx-font-size: 16px;");
        topInfo.getChildren().addAll(categoryLabel, priceLabel);
        
        // Instructor Info
        VBox instructorInfo = new VBox(5);
        Label instructorTitle = new Label("Instructor Information");
        instructorTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Label instructorName = new Label("Name: " + classEntity.getProviderName());
        Label instructorCompany = new Label("Company: " + (classEntity.getProviderCompany() != null ? 
                                           classEntity.getProviderCompany() : "Independent"));
        Label instructorRating = new Label("Rating: ⭐ " + 
                                          (classEntity.getProviderRating() > 0 ? 
                                           String.format("%.1f/5.0", classEntity.getProviderRating()) : "New Instructor"));
        instructorInfo.getChildren().addAll(instructorTitle, instructorName, instructorCompany, instructorRating);
        
        // Description
        Label descTitle = new Label("Description");
        descTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Text description = new Text(classEntity.getDescription());
        description.setWrappingWidth(500);
        
        // Class Details
        VBox detailsBox = new VBox(5);
        Label detailsTitle = new Label("Class Details");
        detailsTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Label duration = new Label("⏱️ Duration: " + classEntity.getDuration() + " minutes");
        Label capacity = new Label("👥 Capacity: " + classEntity.getCurrentEnrollment() + "/" + 
                                   classEntity.getMaxParticipants() + " enrolled");
        Label availability = new Label("📊 Status: " + (classEntity.hasAvailableSpots() ? 
                                       "✅ " + classEntity.getAvailableSpots() + " spots available" : 
                                       "❌ Fully booked"));
        detailsBox.getChildren().addAll(detailsTitle, duration, capacity, availability);
        
        content.getChildren().addAll(topInfo, new Separator(), instructorInfo, new Separator(), 
                                     descTitle, description, new Separator(), detailsBox);
        
        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(400);
        
        dialog.getDialogPane().setContent(scrollPane);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
        
        // Add Book button if spots available
        if (classEntity.hasAvailableSpots()) {
            ButtonType bookButtonType = new ButtonType("Book Now", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().add(0, bookButtonType);
            
            Button bookButton = (Button) dialog.getDialogPane().lookupButton(bookButtonType);
            bookButton.setOnAction(e -> {
                dialog.close();
                handleBookClass(classEntity);
            });
        }
        
        dialog.showAndWait();
    }
    
    private void handleBookClass(ClassEntity classEntity) {
        Long userId = SessionManager.getInstance().getUserId();
        if (userId == null) {
            showAlert("Not Logged In", "Please log in to book a class");
            return;
        }
        
        // Check if user is the instructor of this class
        ClassProvider provider = providerController.getById(classEntity.getProviderId());
        if (provider != null && provider.getUserId().equals(userId)) {
            showAlert("Cannot Book Own Class", "You cannot book your own class as an instructor.");
            return;
        }
        
        // Check if already booked
        if (bookingController.hasUserBooked(userId, classEntity.getClassId())) {
            showAlert("Already Booked", "You have already booked this class");
            return;
        }
        
        // Check availability
        if (!bookingController.checkAvailability(classEntity.getClassId())) {
            showAlert("Class Full", "Sorry, this class is fully booked");
            loadAllClasses(); // Refresh
            return;
        }
        
        // Show booking confirmation dialog
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirm Booking");
        confirmation.setHeaderText("Book: " + classEntity.getTitle());
        confirmation.setContentText(String.format(
            "Instructor: %s\nDuration: %d minutes\nPrice: %.2f TND\n\nProceed with booking?",
            classEntity.getProviderName(),
            classEntity.getDuration(),
            classEntity.getPrice()
        ));
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            // Create booking
            Booking booking = new Booking();
            booking.setClassId(classEntity.getClassId());
            booking.setUserId(userId);
            booking.setBookingDate(java.time.LocalDateTime.now());
            booking.setStatus(BookingStatus.SCHEDULED);
            booking.setPaymentStatus(PaymentStatus.PENDING);
            booking.setTotalAmount(classEntity.getPrice());
            
            boolean success = bookingController.create(booking);
            if (success) {
                showAlert("Booking Successful", 
                         "Your booking has been confirmed!\nPlease complete payment to secure your spot.");
                loadAllClasses(); // Refresh to show updated enrollment
            } else {
                showAlert("Booking Failed", "Unable to complete booking. Please try again.");
            }
        }
    }
    
    private void handleBecomeInstructor() {
        Long userId = SessionManager.getInstance().getUserId();
        if (userId == null) {
            showAlert("Not Logged In", "Please log in to become an instructor");
            return;
        }
        
        // Check if already an instructor
        if (providerController.isUserProvider(userId)) {
            showAlert("Already Registered", "You are already registered as an instructor");
            return;
        }
        
        // Show application dialog
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Become an Instructor");
        dialog.setHeaderText("Apply to teach on Ghrami");
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        
        TextField companyField = new TextField();
        companyField.setPromptText("Company name (optional)");
        
        TextArea expertiseArea = new TextArea();
        expertiseArea.setPromptText("Describe your expertise and qualifications");
        expertiseArea.setPrefRowCount(5);
        
        content.getChildren().addAll(
            new Label("Company Name:"), companyField,
            new Label("Expertise:"), expertiseArea
        );
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            String expertise = expertiseArea.getText().trim();
            if (expertise.isEmpty()) {
                showAlert("Invalid Input", "Please describe your expertise");
                return;
            }
            
            // Create provider application
            opgg.ghrami.model.ClassProvider provider = new opgg.ghrami.model.ClassProvider();
            provider.setUserId(userId);
            provider.setCompanyName(companyField.getText().trim().isEmpty() ? null : companyField.getText().trim());
            provider.setExpertise(expertise);
            provider.setVerified(false); // Requires admin approval
            
            boolean success = providerController.create(provider);
            if (success) {
                showAlert("Application Submitted", 
                         "Your instructor application has been submitted!\nAn admin will review it shortly.");
            } else {
                showAlert("Application Failed", "Unable to submit application. Please try again.");
            }
        }
    }
    
    private void handleMyBookings() {
        Long userId = SessionManager.getInstance().getUserId();
        if (userId == null) {
            showAlert("Not Logged In", "Please log in to view your bookings");
            return;
        }
        
        List<Booking> bookings = bookingController.getByUserId(userId, null);
        
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("My Bookings");
        dialog.setHeaderText("Your Class Bookings");
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(20));
        
        if (bookings.isEmpty()) {
            Label noBookings = new Label("You haven't booked any classes yet");
            noBookings.setStyle("-fx-font-size: 14px; -fx-text-fill: gray;");
            content.getChildren().add(noBookings);
        } else {
            for (Booking booking : bookings) {
                VBox bookingCard = createBookingCard(booking);
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
    
    private VBox createBookingCard(Booking booking) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; -fx-border-color: #e0e0e0; -fx-border-radius: 5; " +
                     "-fx-background-radius: 5; -fx-padding: 12;");
        
        Label title = new Label(booking.getClassTitle());
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        
        Label instructor = new Label("Instructor: " + booking.getProviderName());
        Label bookingDate = new Label("Booked: " + booking.getBookingDate().toString());
        Label price = new Label("Price: " + String.format("%.2f TND", booking.getTotalAmount()));
        
        HBox statusBox = new HBox(10);
        Label statusLabel = new Label("Status: " + booking.getStatus().getValue());
        Label paymentLabel = new Label("Payment: " + booking.getPaymentStatus().getValue());
        
        // Color code statuses
        statusLabel.setStyle(getStatusStyle(booking.getStatus()));
        paymentLabel.setStyle(getPaymentStatusStyle(booking.getPaymentStatus()));
        
        statusBox.getChildren().addAll(statusLabel, paymentLabel);
        
        card.getChildren().addAll(title, instructor, bookingDate, price, statusBox);
        
        // Add cancel button if booking is scheduled
        if (booking.getStatus() == BookingStatus.SCHEDULED) {
            Button cancelBtn = new Button("Cancel Booking");
            cancelBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 5 15;");
            cancelBtn.setOnAction(e -> handleCancelBooking(booking));
            card.getChildren().add(cancelBtn);
        }
        
        return card;
    }
    
    private String getStatusStyle(BookingStatus status) {
        return switch (status) {
            case SCHEDULED -> "-fx-text-fill: #3498db; -fx-font-weight: bold;";
            case COMPLETED -> "-fx-text-fill: #27ae60; -fx-font-weight: bold;";
            case CANCELLED -> "-fx-text-fill: #e74c3c; -fx-font-weight: bold;";
        };
    }
    
    private String getPaymentStatusStyle(PaymentStatus status) {
        return switch (status) {
            case PENDING -> "-fx-text-fill: #f39c12; -fx-font-weight: bold;";
            case PAID -> "-fx-text-fill: #27ae60; -fx-font-weight: bold;";
            case REFUNDED -> "-fx-text-fill: #95a5a6; -fx-font-weight: bold;";
        };
    }
    
    private void handleCancelBooking(Booking booking) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Cancel Booking");
        confirmation.setHeaderText("Are you sure?");
        confirmation.setContentText("Do you want to cancel this booking?");
        
        Optional<ButtonType> result = confirmation.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            boolean success = bookingController.cancel(booking.getBookingId());
            if (success) {
                showAlert("Booking Cancelled", "Your booking has been cancelled");
                handleMyBookings(); // Refresh
                loadAllClasses(); // Refresh marketplace
            } else {
                showAlert("Error", "Unable to cancel booking");
            }
        }
    }
    
    @FXML
    private void handleBack() {
        try {
            Stage stage = (Stage) searchField.getScene().getWindow();
            double width = stage.getWidth();
            double height = stage.getHeight();
            boolean wasMaximized = stage.isMaximized();
            
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/UserFeed.fxml"));
            Scene scene = new Scene(loader.load(), width, height);
            scene.getStylesheets().add(getClass().getResource("/css/social-style.css").toExternalForm());
            
            stage.setScene(scene);
            stage.setTitle("Ghrami - Social Platform");
            if (wasMaximized) {
                stage.setMaximized(true);
            }
        } catch (Exception e) {
            System.err.println("Error loading User Feed: " + e.getMessage());
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

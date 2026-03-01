package opgg.ghrami.view;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import opgg.ghrami.controller.BookingController;
import opgg.ghrami.controller.ClassController;
import opgg.ghrami.controller.ClassProviderController;
import opgg.ghrami.model.*;
import opgg.ghrami.util.CertificateService;
import opgg.ghrami.util.SessionManager;

import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
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
        if (userId == null) { showAlert("Not Logged In", "Please log in to access instructor dashboard"); return; }

        currentProvider = providerController.getByUserId(userId);
        if (currentProvider == null) {
            showAlert("Not an Instructor", "You are not registered as an instructor.");
            return;
        }
        if (!currentProvider.isVerified()) {
            showAlert("Pending Verification", "Your application is pending admin approval.");
            return;
        }
        providerId = currentProvider.getProviderId();
        instructorNameLabel.setText(currentProvider.getUsername());
        ratingLabel.setText(String.format("⭐ %.1f", currentProvider.getRating()));
    }

    private void setupBookingStatusCombo() {
        bookingStatusCombo.getItems().addAll("All", "Pending", "Scheduled", "Completed", "Cancelled");
        bookingStatusCombo.setValue("All");
    }

    private void loadDashboardData() {
        if (providerId == null) return;
        loadStats(); loadMyClasses(); loadAllBookings(); loadRevenue();
    }

    private void loadStats() {
        if (providerId == null) return;
        List<ClassEntity> classes = classController.getByProviderId(providerId);
        activeClassesLabel.setText(String.valueOf(classes.size()));
        List<Booking> allBookings = bookingController.getByProviderId(providerId);
        long uniqueStudents = allBookings.stream().map(Booking::getUserId).distinct().count();
        totalStudentsLabel.setText(String.valueOf(uniqueStudents));
        double totalRevenue = bookingController.getProviderRevenue(providerId, null);
        totalRevenueLabel.setText(String.format("%.2f TND", totalRevenue));
        long pendingCount = bookingController.getByProviderId(providerId).stream()
                .filter(b -> b.getStatus() == BookingStatus.SCHEDULED).count();
        pendingBookingsLabel.setText(String.valueOf(pendingCount));
    }

    @FXML
    private void loadMyClasses() {
        if (providerId == null) return;
        classesContainer.getChildren().clear();
        List<ClassEntity> classes = classController.getByProviderId(providerId);
        if (classes.isEmpty()) {
            Label lbl = new Label("No classes yet. Create your first class!");
            lbl.setStyle("-fx-font-size: 14px; -fx-text-fill: gray;");
            classesContainer.getChildren().add(lbl);
            return;
        }
        for (ClassEntity ce : classes) classesContainer.getChildren().add(createClassCard(ce));
    }

    private VBox createClassCard(ClassEntity classEntity) {
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: white; -fx-border-radius: 15; -fx-background-radius: 15; " +
                     "-fx-padding: 20; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 5);");

        HBox header = new HBox(12); header.setAlignment(Pos.CENTER_LEFT);
        Label title = new Label(classEntity.getTitle());
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1c1e21;");
        Label catBadge = new Label(classEntity.getCategory());
        catBadge.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; -fx-padding: 5 12; -fx-background-radius: 15; -fx-font-size: 12px;");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        Button editBtn = new Button("✏️ Edit");
        editBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-padding: 8 18; -fx-cursor: hand; -fx-background-radius: 20; -fx-font-weight: bold;");
        editBtn.setOnAction(e -> handleEditClass(classEntity));

        Button deleteBtn = new Button("🗑️ Delete");
        deleteBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 8 18; -fx-cursor: hand; -fx-background-radius: 20; -fx-font-weight: bold;");
        deleteBtn.setOnAction(e -> handleDeleteClass(classEntity));

        header.getChildren().addAll(title, catBadge, spacer, editBtn, deleteBtn);

        Label description = new Label(classEntity.getDescription());
        description.setWrapText(true);
        description.setStyle("-fx-font-size: 14px; -fx-text-fill: #65676b;");

        HBox details = new HBox(20); details.setAlignment(Pos.CENTER_LEFT);
        details.setStyle("-fx-background-color: #f0f2f5; -fx-background-radius: 10; -fx-padding: 12 15;");
        Label priceL = new Label("💰 " + String.format("%.2f TND", classEntity.getPrice()));
        priceL.setStyle("-fx-font-weight: bold; -fx-text-fill: #27ae60; -fx-font-size: 14;");
        Label durL = new Label("⏱️ " + classEntity.getDuration() + " min");
        Label enrollL = new Label("👥 " + classEntity.getCurrentEnrollment() + "/" + classEntity.getMaxParticipants());
        Label availL = new Label(classEntity.hasAvailableSpots()
                ? "✅ " + classEntity.getAvailableSpots() + " spots left" : "❌ Full");
        durL.setStyle("-fx-font-weight: 600;"); enrollL.setStyle("-fx-font-weight: 600;");
        availL.setStyle(classEntity.hasAvailableSpots()
                ? "-fx-text-fill: #27ae60; -fx-font-weight: bold;"
                : "-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        details.getChildren().addAll(priceL, durL, enrollL, availL);

        Button viewBookingsBtn = new Button("👥 View Students");
        viewBookingsBtn.setStyle("-fx-background-color: #9b59b6; -fx-text-fill: white; -fx-padding: 10 20; -fx-background-radius: 20; -fx-font-weight: bold; -fx-cursor: hand;");
        viewBookingsBtn.setOnAction(e -> showClassBookings(classEntity));

        card.getChildren().addAll(header, new Separator(), description, details, viewBookingsBtn);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CREATE CLASS — includes video + image browse
    // ─────────────────────────────────────────────────────────────────────────
    @FXML
    private void handleCreateClass() {
        Dialog<ClassEntity> dialog = new Dialog<>();
        dialog.setTitle("Create New Class");
        dialog.setHeaderText("Enter class details");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));

        TextField titleField    = new TextField(); titleField.setPromptText("Class title");
        TextArea  descArea      = new TextArea();  descArea.setPromptText("Description"); descArea.setPrefRowCount(3);
        TextField categoryField = new TextField(); categoryField.setPromptText("e.g. fitness, tech, art");
        TextField priceField    = new TextField(); priceField.setPromptText("Price (TND)");
        TextField durationField = new TextField(); durationField.setPromptText("Duration (minutes)");
        TextField maxPField     = new TextField(); maxPField.setPromptText("Max participants");

        // ── Video browse ──────────────────────────────────────────────────────
        TextField videoPathField = new TextField(); videoPathField.setPromptText("Optional: video file path"); videoPathField.setPrefWidth(220);
        Button videoBrowse = browseBtn("Select Video", videoPathField,
                new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.avi", "*.mkv", "*.mov", "*.wmv"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        HBox videoBox = new HBox(8, videoPathField, videoBrowse); videoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(videoPathField, Priority.ALWAYS);

        // ── Image browse ──────────────────────────────────────────────────────
        TextField imagePathField = new TextField(); imagePathField.setPromptText("Optional: thumbnail image"); imagePathField.setPrefWidth(220);
        Button imageBrowse = browseBtn("Select Image", imagePathField,
                new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.jpeg", "*.png", "*.gif", "*.webp"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        HBox imageBox = new HBox(8, imagePathField, imageBrowse); imageBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(imagePathField, Priority.ALWAYS);

        grid.add(new Label("Title:"),        0, 0); grid.add(titleField,    1, 0);
        grid.add(new Label("Description:"),  0, 1); grid.add(descArea,      1, 1);
        grid.add(new Label("Category:"),     0, 2); grid.add(categoryField, 1, 2);
        grid.add(new Label("Price:"),        0, 3); grid.add(priceField,    1, 3);
        grid.add(new Label("Duration:"),     0, 4); grid.add(durationField, 1, 4);
        grid.add(new Label("Max Students:"), 0, 5); grid.add(maxPField,     1, 5);
        grid.add(new Label("🎬 Video:"),     0, 6); grid.add(videoBox,      1, 6);
        grid.add(new Label("🖼 Thumbnail:"), 0, 7); grid.add(imageBox,      1, 7);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    ClassEntity c = new ClassEntity();
                    c.setProviderId(providerId);
                    c.setTitle(titleField.getText().trim());
                    c.setDescription(descArea.getText().trim());
                    c.setCategory(categoryField.getText().trim());
                    c.setPrice(Double.parseDouble(priceField.getText().trim()));
                    c.setDuration(Integer.parseInt(durationField.getText().trim()));
                    c.setMaxParticipants(Integer.parseInt(maxPField.getText().trim()));
                    String vp = videoPathField.getText().trim();
                    c.setVideoPath(vp.isEmpty() ? null : vp);
                    String ip = imagePathField.getText().trim();
                    c.setImagePath(ip.isEmpty() ? null : ip);
                    return c;
                } catch (NumberFormatException e) {
                    showAlert("Invalid Input", "Please enter valid numbers for price, duration, and max participants.");
                    return null;
                }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(ce -> {
            if (ce.getTitle().isEmpty() || ce.getDescription().isEmpty()) {
                showAlert("Invalid Input", "Title and description are required."); return;
            }
            if (classController.create(ce)) { showAlert("Success", "Class created!"); loadMyClasses(); loadStats(); }
            else showAlert("Error", "Failed to create class.");
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  EDIT CLASS — includes video + image browse
    // ─────────────────────────────────────────────────────────────────────────
    private void handleEditClass(ClassEntity ce) {
        Dialog<ClassEntity> dialog = new Dialog<>();
        dialog.setTitle("Edit Class");
        dialog.setHeaderText("Update class details");

        GridPane grid = new GridPane();
        grid.setHgap(10); grid.setVgap(10); grid.setPadding(new Insets(20));

        TextField titleField    = new TextField(ce.getTitle());
        TextArea  descArea      = new TextArea(ce.getDescription()); descArea.setPrefRowCount(3);
        TextField categoryField = new TextField(ce.getCategory());
        TextField priceField    = new TextField(String.valueOf(ce.getPrice()));
        TextField durationField = new TextField(String.valueOf(ce.getDuration()));
        TextField maxPField     = new TextField(String.valueOf(ce.getMaxParticipants()));

        TextField videoPathField = new TextField(ce.getVideoPath() != null ? ce.getVideoPath() : "");
        videoPathField.setPrefWidth(220);
        Button videoBrowse = browseBtn("Select Video", videoPathField,
                new FileChooser.ExtensionFilter("Video Files", "*.mp4", "*.avi", "*.mkv", "*.mov"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        HBox videoBox = new HBox(8, videoPathField, videoBrowse); videoBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(videoPathField, Priority.ALWAYS);

        TextField imagePathField = new TextField(ce.getImagePath() != null ? ce.getImagePath() : "");
        imagePathField.setPrefWidth(220);
        Button imageBrowse = browseBtn("Select Image", imagePathField,
                new FileChooser.ExtensionFilter("Image Files", "*.jpg", "*.jpeg", "*.png", "*.gif"),
                new FileChooser.ExtensionFilter("All Files", "*.*"));
        HBox imageBox = new HBox(8, imagePathField, imageBrowse); imageBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(imagePathField, Priority.ALWAYS);

        grid.add(new Label("Title:"),        0, 0); grid.add(titleField,    1, 0);
        grid.add(new Label("Description:"),  0, 1); grid.add(descArea,      1, 1);
        grid.add(new Label("Category:"),     0, 2); grid.add(categoryField, 1, 2);
        grid.add(new Label("Price:"),        0, 3); grid.add(priceField,    1, 3);
        grid.add(new Label("Duration:"),     0, 4); grid.add(durationField, 1, 4);
        grid.add(new Label("Max Students:"), 0, 5); grid.add(maxPField,     1, 5);
        grid.add(new Label("🎬 Video:"),     0, 6); grid.add(videoBox,      1, 6);
        grid.add(new Label("🖼 Thumbnail:"), 0, 7); grid.add(imageBox,      1, 7);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                try {
                    ce.setTitle(titleField.getText().trim());
                    ce.setDescription(descArea.getText().trim());
                    ce.setCategory(categoryField.getText().trim());
                    ce.setPrice(Double.parseDouble(priceField.getText().trim()));
                    ce.setDuration(Integer.parseInt(durationField.getText().trim()));
                    ce.setMaxParticipants(Integer.parseInt(maxPField.getText().trim()));
                    String vp = videoPathField.getText().trim();
                    ce.setVideoPath(vp.isEmpty() ? null : vp);
                    String ip = imagePathField.getText().trim();
                    ce.setImagePath(ip.isEmpty() ? null : ip);
                    return ce;
                } catch (NumberFormatException e) { showAlert("Invalid Input", "Please enter valid numbers."); return null; }
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updated -> {
            if (classController.update(updated)) { showAlert("Success", "Class updated!"); loadMyClasses(); }
            else showAlert("Error", "Failed to update class.");
        });
    }

    /** Reusable file browse button that fills a TextField with the chosen path. */
    private Button browseBtn(String title, TextField target, FileChooser.ExtensionFilter... filters) {
        Button btn = new Button("📂 Browse");
        btn.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; -fx-padding: 6 14; -fx-background-radius: 15; -fx-cursor: hand;");
        btn.setOnAction(e -> {
            FileChooser fc = new FileChooser();
            fc.setTitle(title);
            fc.getExtensionFilters().addAll(filters);
            File file = fc.showOpenDialog(null);
            if (file != null) target.setText(file.getAbsolutePath());
        });
        return btn;
    }

    private void handleDeleteClass(ClassEntity classEntity) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Delete Class");
        confirm.setHeaderText("Are you sure?");
        confirm.setContentText("This will delete the class and all its bookings. This cannot be undone.");
        confirm.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                if (classController.delete(classEntity.getClassId())) {
                    showAlert("Deleted", "Class deleted successfully.");
                    loadMyClasses(); loadStats(); loadAllBookings();
                } else showAlert("Error", "Failed to delete class.");
            }
        });
    }

    private void showClassBookings(ClassEntity classEntity) {
        List<Booking> bookings = bookingController.getByClassId(classEntity.getClassId());
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Class Bookings");
        dialog.setHeaderText("Students for: " + classEntity.getTitle());
        VBox content = new VBox(10); content.setPadding(new Insets(20));
        if (bookings.isEmpty()) {
            Label l = new Label("No bookings yet"); l.setStyle("-fx-font-size: 14px; -fx-text-fill: gray;");
            content.getChildren().add(l);
        } else bookings.forEach(b -> content.getChildren().add(createBookingCardSimple(b)));
        ScrollPane sp = new ScrollPane(content); sp.setFitToWidth(true); sp.setPrefHeight(400);
        dialog.getDialogPane().setContent(sp);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialog.showAndWait();
    }

    @FXML private void loadAllBookings() {
        if (providerId == null) return;
        bookingsContainer.getChildren().clear();
        List<Booking> bookings = bookingController.getByProviderId(providerId);
        if (bookings.isEmpty()) {
            Label l = new Label("No bookings yet"); l.setStyle("-fx-font-size: 14px; -fx-text-fill: gray;");
            bookingsContainer.getChildren().add(l);
            return;
        }
        bookings.forEach(b -> bookingsContainer.getChildren().add(createBookingCardDetailed(b)));
    }

    @FXML private void filterBookings() {
        if (providerId == null) return;
        String filter = bookingStatusCombo.getValue();
        BookingStatus status = "All".equals(filter) ? null : BookingStatus.fromString(filter.toUpperCase());
        bookingsContainer.getChildren().clear();
        List<Booking> bookings = (status == null)
                ? bookingController.getByProviderId(providerId)
                : bookingController.getByProviderId(providerId).stream()
                    .filter(b -> b.getStatus() == status).collect(Collectors.toList());
        if (bookings.isEmpty()) {
            Label l = new Label("No bookings with this status"); l.setStyle("-fx-font-size: 14px; -fx-text-fill: gray;");
            bookingsContainer.getChildren().add(l);
            return;
        }
        bookings.forEach(b -> bookingsContainer.getChildren().add(createBookingCardDetailed(b)));
    }

    private VBox createBookingCardSimple(Booking booking) {
        VBox card = new VBox(8);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 15; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 3);");
        Label student = new Label("👤 " + booking.getUserFullName() + " (@" + booking.getUsername() + ")");
        student.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #1c1e21;");
        HBox row = new HBox(12); row.setAlignment(Pos.CENTER_LEFT);
        Label sl = new Label(booking.getStatus().getValue()); sl.setStyle(getStatusStyle(booking.getStatus()));
        Label pl = new Label(booking.getPaymentStatus().getValue()); pl.setStyle(getPaymentStatusStyle(booking.getPaymentStatus()));
        row.getChildren().addAll(sl, new Label("•"), pl);
        Label date = new Label("Booked: " + booking.getBookingDate()); date.setStyle("-fx-font-size: 12px; -fx-text-fill: #65676b;");
        card.getChildren().addAll(student, row, date);
        return card;
    }

    private VBox createBookingCardDetailed(Booking booking) {
        VBox card = new VBox(12);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 15; -fx-padding: 20; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 5);");

        HBox header = new HBox(15); header.setAlignment(Pos.CENTER_LEFT);
        Label classL = new Label(booking.getClassTitle()); classL.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1c1e21;");
        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);
        Label priceL = new Label(String.format("%.2f TND", booking.getTotalAmount())); priceL.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
        header.getChildren().addAll(classL, sp, priceL);

        Label studentL = new Label("👤 " + booking.getUserFullName() + " (@" + booking.getUsername() + ")");
        studentL.setStyle("-fx-font-size: 15px; -fx-font-weight: 600; -fx-text-fill: #1c1e21;");
        Label emailL = new Label("📧 " + booking.getUserEmail()); emailL.setStyle("-fx-font-size: 13px; -fx-text-fill: #65676b;");

        HBox statusRow = new HBox(20); statusRow.setAlignment(Pos.CENTER_LEFT);
        statusRow.setStyle("-fx-background-color: #f0f2f5; -fx-background-radius: 10; -fx-padding: 10 15;");
        Label sl = new Label("Status: " + booking.getStatus().getValue()); sl.setStyle(getStatusStyle(booking.getStatus()));
        Label pl = new Label("Payment: " + booking.getPaymentStatus().getValue()); pl.setStyle(getPaymentStatusStyle(booking.getPaymentStatus()));
        Label dl = new Label("📅 " + booking.getBookingDate()); dl.setStyle("-fx-font-size: 13px; -fx-font-weight: 600;");
        statusRow.getChildren().addAll(sl, new Label("•"), pl, new Label("•"), dl);

        HBox actions = new HBox(12); actions.setAlignment(Pos.CENTER_RIGHT);

        // ── Mark Complete button ──────────────────────────────────────────────
        if (booking.getStatus() == BookingStatus.SCHEDULED || booking.getStatus() == BookingStatus.PENDING) {
            Button completeBtn = new Button("✅ Mark Complete");
            completeBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 10 20; " +
                                "-fx-background-radius: 20; -fx-font-weight: bold; -fx-cursor: hand; -fx-font-size: 13;");
            completeBtn.setOnAction(e -> handleCompleteBooking(booking));
            actions.getChildren().add(completeBtn);
        }

        // ── Rating display (if student has rated) ─────────────────────────────
        if (booking.isRated()) {
            HBox ratingBox = new HBox(4); ratingBox.setAlignment(Pos.CENTER);
            ratingBox.setPadding(new Insets(6, 12, 6, 12));
            ratingBox.setStyle("-fx-background-color: #fff3cd; -fx-background-radius: 16;");
            for (int i = 1; i <= 5; i++) {
                Label s = new Label(i <= booking.getRating() ? "★" : "☆");
                s.setStyle("-fx-font-size: 14px; -fx-text-fill: " + (i <= booking.getRating() ? "#f39c12" : "#ccc") + ";");
                ratingBox.getChildren().add(s);
            }
            Label rLbl = new Label("  " + booking.getRating() + "/5");
            rLbl.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #856404;");
            ratingBox.getChildren().add(rLbl);
            actions.getChildren().add(ratingBox);

            // Show review in a tooltip if present
            if (booking.getReview() != null && !booking.getReview().isBlank()) {
                Button reviewBtn = new Button("💬 View Review");
                reviewBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #667eea; -fx-cursor: hand; " +
                                  "-fx-border-color: #667eea; -fx-border-radius: 16; -fx-padding: 6 14;");
                reviewBtn.setOnAction(e -> showAlert("Student Review", "\"" + booking.getReview() + "\""));
                actions.getChildren().add(reviewBtn);
            }
        }

        card.getChildren().addAll(header, new Separator(), studentL, emailL, statusRow, actions);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  MARK COMPLETE — generates PDF certificate automatically
    // ─────────────────────────────────────────────────────────────────────────
    private void handleCompleteBooking(Booking booking) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Complete Booking");
        confirm.setHeaderText("Mark this class as completed?");
        confirm.setContentText("Student: " + booking.getUserFullName());
        confirm.showAndWait().ifPresent(btn -> {
            if (btn != ButtonType.OK) return;

            boolean success = bookingController.complete(booking.getBookingId());
            if (!success) { showAlert("Error", "Failed to update booking."); return; }

            loadAllBookings();
            loadStats();

            // ── Auto-generate PDF certificate ─────────────────────────────────
            String certPath = CertificateService.generate(
                    booking.getUserFullName(),
                    booking.getClassTitle(),
                    currentProvider != null ? currentProvider.getFullName() : "Instructor",
                    LocalDateTime.now()
            );

            if (certPath != null) {
                // Show success with option to open
                Alert certAlert = new Alert(Alert.AlertType.INFORMATION);
                certAlert.setTitle("✅ Booking Completed");
                certAlert.setHeaderText("Certificate generated!");
                certAlert.setContentText(
                    "🎓 A certificate of completion has been generated for:\n\n" +
                    "Student:  " + booking.getUserFullName() + "\n" +
                    "Class:    " + booking.getClassTitle() + "\n\n" +
                    "📄 Saved to:\n" + certPath
                );
                ButtonType openBtn = new ButtonType("📂 Open PDF", ButtonBar.ButtonData.LEFT);
                certAlert.getButtonTypes().add(openBtn);
                certAlert.showAndWait().ifPresent(b -> {
                    if (b == openBtn) {
                        try { java.awt.Desktop.getDesktop().open(new File(certPath)); }
                        catch (Exception ex) { showAlert("Error", "Could not open PDF: " + ex.getMessage()); }
                    }
                });
            } else {
                showAlert("Completed", "Booking marked as completed.\n(Certificate generation failed — check iTextPDF dependency.)");
            }
        });
    }

    private void loadRevenue() {
        if (providerId == null) return;
        double paid = bookingController.getProviderRevenueByPaymentStatus(providerId, PaymentStatus.PAID);
        totalEarningsLabel.setText(String.format("%.2f TND", paid));
        double pending = bookingController.getProviderRevenueByPaymentStatus(providerId, PaymentStatus.PENDING);
        pendingRevenueLabel.setText(String.format("%.2f TND", pending));
        loadRevenueBreakdown();
    }

    private void loadRevenueBreakdown() {
        revenueBreakdownContainer.getChildren().clear();
        List<ClassEntity> classes = classController.getByProviderId(providerId);
        if (classes.isEmpty()) {
            Label l = new Label("No classes yet"); l.setStyle("-fx-font-size: 14px; -fx-text-fill: gray;");
            revenueBreakdownContainer.getChildren().add(l); return;
        }
        for (ClassEntity ce : classes) {
            List<Booking> cb = bookingController.getByClassId(ce.getClassId());
            double paid    = cb.stream().filter(b -> b.getPaymentStatus() == PaymentStatus.PAID).mapToDouble(Booking::getTotalAmount).sum();
            double pending = cb.stream().filter(b -> b.getPaymentStatus() == PaymentStatus.PENDING).mapToDouble(Booking::getTotalAmount).sum();
            revenueBreakdownContainer.getChildren().add(createRevenueCard(ce, paid, pending, cb.size()));
        }
    }

    private VBox createRevenueCard(ClassEntity ce, double paid, double pending, int total) {
        VBox card = new VBox(10);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-padding: 15; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 10, 0, 0, 3);");
        Label title = new Label(ce.getTitle()); title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1c1e21;");
        HBox amounts = new HBox(15); amounts.setAlignment(Pos.CENTER_LEFT);
        amounts.setStyle("-fx-background-color: #f0f2f5; -fx-background-radius: 8; -fx-padding: 8 12;");
        Label paidL    = new Label(String.format("💵 Paid: %.2f TND", paid));    paidL.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
        Label pendingL = new Label(String.format("⏳ Pending: %.2f TND", pending)); pendingL.setStyle("-fx-text-fill: #f39c12; -fx-font-weight: bold;");
        Label totalL   = new Label("📊 " + total + " bookings");                totalL.setStyle("-fx-text-fill: #65676b; -fx-font-weight: 600;");
        amounts.getChildren().addAll(paidL, new Label("•"), pendingL, new Label("•"), totalL);
        card.getChildren().addAll(title, amounts);
        return card;
    }

    // ── Style helpers ─────────────────────────────────────────────────────────
    private String getStatusStyle(BookingStatus status) {
        return switch (status) {
            case PENDING   -> "-fx-background-color: #fff3e0; -fx-text-fill: #ff9800; -fx-font-weight: bold; -fx-padding: 5 10; -fx-background-radius: 12; -fx-font-size: 12;";
            case SCHEDULED -> "-fx-background-color: #e3f2fd; -fx-text-fill: #2196F3; -fx-font-weight: bold; -fx-padding: 5 10; -fx-background-radius: 12; -fx-font-size: 12;";
            case COMPLETED -> "-fx-background-color: #e8f5e9; -fx-text-fill: #4CAF50; -fx-font-weight: bold; -fx-padding: 5 10; -fx-background-radius: 12; -fx-font-size: 12;";
            case CANCELLED -> "-fx-background-color: #ffebee; -fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-padding: 5 10; -fx-background-radius: 12; -fx-font-size: 12;";
        };
    }

    private String getPaymentStatusStyle(PaymentStatus status) {
        return switch (status) {
            case PENDING  -> "-fx-background-color: #fff3e0; -fx-text-fill: #ff9800; -fx-font-weight: bold; -fx-padding: 5 10; -fx-background-radius: 12; -fx-font-size: 12;";
            case PAID     -> "-fx-background-color: #e8f5e9; -fx-text-fill: #4CAF50; -fx-font-weight: bold; -fx-padding: 5 10; -fx-background-radius: 12; -fx-font-size: 12;";
            case REFUNDED -> "-fx-background-color: #f5f5f5; -fx-text-fill: #95a5a6; -fx-font-weight: bold; -fx-padding: 5 10; -fx-background-radius: 12; -fx-font-size: 12;";
        };
    }

    @FXML
    private void handleBack() {
        try {
            Stage stage = (Stage) instructorNameLabel.getScene().getWindow();
            double w = stage.getWidth(), h = stage.getHeight();
            boolean max = stage.isMaximized();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/ClassMarketplace.fxml"));
            Scene scene = new Scene(loader.load(), w, h);
            scene.getStylesheets().add(getClass().getResource("/css/style.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Ghrami - Class Marketplace");
            if (max) stage.setMaximized(true);
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(content);
        alert.showAndWait();
    }
}
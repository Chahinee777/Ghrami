package opgg.ghrami.view;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.scene.media.MediaView;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import opgg.ghrami.controller.BookingController;
import opgg.ghrami.controller.ClassController;
import opgg.ghrami.controller.ClassProviderController;
import opgg.ghrami.model.Booking;
import opgg.ghrami.model.BookingStatus;
import opgg.ghrami.model.ClassEntity;
import opgg.ghrami.model.ClassProvider;
import opgg.ghrami.model.PaymentStatus;
import opgg.ghrami.util.CertificateService;
import opgg.ghrami.util.SessionManager;
import opgg.ghrami.util.StripePaymentService;

import java.io.File;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
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
            if (wasMaximized) stage.setMaximized(true);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Error", "Unable to open Instructor Dashboard");
        }
    }

    private void setupCategoryCombo() {
        categoryCombo.getItems().add("All Categories");
        categoryCombo.getItems().addAll(classController.getCategories());
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
        if (keyword.isEmpty()) { loadAllClasses(); return; }
        currentClasses = classController.search(keyword);
        displayClasses(currentClasses);
    }

    @FXML
    private void handleFilter() {
        String category = categoryCombo.getValue();
        if ("All Categories".equals(category)) category = null;
        Double maxPrice = null;
        String priceText = maxPriceField.getText().trim();
        if (!priceText.isEmpty()) {
            try { maxPrice = Double.parseDouble(priceText); }
            catch (NumberFormatException e) { showAlert("Invalid Price", "Please enter a valid price"); return; }
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
        if (currentClasses == null || currentClasses.isEmpty()) return;
        switch (sortCombo.getValue()) {
            case "Price: Low to High" -> currentClasses.sort((a, b) -> Double.compare(a.getPrice(), b.getPrice()));
            case "Price: High to Low" -> currentClasses.sort((a, b) -> Double.compare(b.getPrice(), a.getPrice()));
            case "Most Popular"       -> currentClasses.sort((a, b) -> Integer.compare(b.getCurrentEnrollment(), a.getCurrentEnrollment()));
        }
        displayClasses(currentClasses);
    }

    private void displayClasses(List<ClassEntity> classes) {
        classesContainer.getChildren().clear();
        if (classes == null || classes.isEmpty()) {
            Label lbl = new Label("No classes found");
            lbl.setStyle("-fx-font-size: 16px; -fx-text-fill: gray;");
            classesContainer.getChildren().add(lbl);
            resultsLabel.setText("No results");
            return;
        }
        resultsLabel.setText(classes.size() + " classes found");
        for (ClassEntity ce : classes) classesContainer.getChildren().add(createClassCard(ce));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CLASS CARD
    // ─────────────────────────────────────────────────────────────────────────
    private VBox createClassCard(ClassEntity classEntity) {
        VBox card = new VBox(0);   // 0 gap — sections separated individually

        // ── Resolve user relationship to this class ───────────────────────────
        Long currentUserId = SessionManager.getInstance().getUserId();
        boolean isOwnClass = false;
        boolean isPaid     = hasPaidBooking(classEntity.getClassId());
        boolean isPending  = false;

        if (currentUserId != null && !isPaid) {
            ClassProvider provider = providerController.getById(classEntity.getProviderId());
            isOwnClass = (provider != null && provider.getUserId().equals(currentUserId));
            if (!isOwnClass) {
                Booking existing = bookingController.getUserBookingForClass(currentUserId, classEntity.getClassId());
                isPending = existing != null && existing.getStatus() == BookingStatus.PENDING;
            }
        }

        // ── Card shell ────────────────────────────────────────────────────────
        if (isPaid) {
            card.setStyle("-fx-background-color: white; " +
                          "-fx-border-color: #27ae60; -fx-border-width: 0 0 0 5; " +
                          "-fx-border-radius: 15; -fx-background-radius: 15; " +
                          "-fx-effect: dropshadow(gaussian, rgba(39,174,96,0.2), 15, 0, 0, 5);");
        } else {
            card.setStyle("-fx-background-color: white; -fx-border-color: transparent; " +
                          "-fx-border-radius: 15; -fx-background-radius: 15; " +
                          "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 15, 0, 0, 5);");
        }

        // ── [1] Thumbnail banner ──────────────────────────────────────────────
        String imgPath = classEntity.getImagePath();
        boolean thumbAdded = false;
        if (imgPath != null && !imgPath.isBlank()) {
            File imgFile = new File(imgPath);
            if (imgFile.exists()) {
                try {
                    // Use a Region with a CSS background-image — this stretches correctly
                    // without breaking the card layout the way ImageView does.
                    Region thumbRegion = new Region();
                    thumbRegion.setPrefHeight(160);
                    thumbRegion.setMaxWidth(Double.MAX_VALUE);
                    thumbRegion.setStyle(
                        "-fx-background-image: url('" + imgFile.toURI().toString().replace("'", "\\'") + "'); " +
                        "-fx-background-size: cover; " +
                        "-fx-background-position: center; " +
                        "-fx-background-repeat: no-repeat; " +
                        "-fx-background-radius: 15 15 0 0;"
                    );
                    VBox.setVgrow(thumbRegion, Priority.NEVER);
                    card.getChildren().add(thumbRegion);
                    thumbAdded = true;
                } catch (Exception ignored) { /* fall through to placeholder */ }
            }
        }
        if (!thumbAdded) {
            // Coloured gradient placeholder with category emoji
            String catColor = categoryColor(classEntity.getCategory());
            HBox placeholder = new HBox();
            placeholder.setPrefHeight(130);
            placeholder.setMaxWidth(Double.MAX_VALUE);
            placeholder.setAlignment(Pos.CENTER);
            placeholder.setStyle("-fx-background-color: " + catColor + "; -fx-background-radius: 15 15 0 0;");
            Label catIcon = new Label(categoryEmoji(classEntity.getCategory()) + "  " + classEntity.getCategory());
            catIcon.setStyle("-fx-font-size: 20px; -fx-text-fill: white; -fx-font-weight: bold;");
            placeholder.getChildren().add(catIcon);
            card.getChildren().add(placeholder);
        }

        // ── [2] Main content area ─────────────────────────────────────────────
        VBox content = new VBox(10);
        content.setPadding(new Insets(16, 20, 16, 20));

        // Header: title + badges + price
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        Label titleLabel = new Label(classEntity.getTitle());
        titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1c1e21;");
        header.getChildren().add(titleLabel);
        if (isPaid) {
            Label b = new Label("✅ Enrolled");
            b.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; " +
                       "-fx-padding: 3 9; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");
            header.getChildren().add(b);
        }
        if (isPending) {
            Label b = new Label("⏳ Pending");
            b.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; " +
                       "-fx-padding: 3 9; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");
            header.getChildren().add(b);
        }
        Label catBadge = new Label(classEntity.getCategory());
        catBadge.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; " +
                          "-fx-padding: 4 11; -fx-background-radius: 15; -fx-font-size: 11px; -fx-font-weight: bold;");
        Region hSpacer = new Region(); HBox.setHgrow(hSpacer, Priority.ALWAYS);
        Label priceLabel = new Label(String.format("%.2f TND", classEntity.getPrice()));
        priceLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");
        header.getChildren().addAll(catBadge, hSpacer, priceLabel);

        // ── Instructor row — CLICKABLE ────────────────────────────────────────
        HBox instructorBox = new HBox(8);
        instructorBox.setAlignment(Pos.CENTER_LEFT);
        Hyperlink instructorLink = new Hyperlink("👨‍🏫  " + classEntity.getProviderName());
        instructorLink.setStyle("-fx-font-size: 13px; -fx-text-fill: #667eea; -fx-font-weight: 600; " +
                                "-fx-border-color: transparent; -fx-cursor: hand;");
        instructorLink.setOnAction(e -> showInstructorProfile(classEntity.getProviderId()));
        instructorBox.getChildren().add(instructorLink);
        if (classEntity.getProviderRating() > 0) {
            Label ratingLbl = new Label(String.format("⭐ %.1f", classEntity.getProviderRating()));
            ratingLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #f39c12; -fx-font-weight: bold;");
            instructorBox.getChildren().addAll(new Label("·"), ratingLbl);
        }

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #f0f2f5;");

        // Description
        Text description = new Text(classEntity.getDescription());
        description.setWrappingWidth(860);
        description.setStyle("-fx-font-size: 13px; -fx-fill: #65676b;");

        // Details strip
        HBox details = new HBox(16);
        details.setAlignment(Pos.CENTER_LEFT);
        details.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 10; -fx-padding: 10 14;");
        Label dur  = new Label("⏱️  " + classEntity.getDuration() + " min");
        Label cap  = new Label("👥  " + classEntity.getAvailableSpots() + "/" + classEntity.getMaxParticipants() + " spots");
        dur.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #1c1e21;");
        cap.setStyle("-fx-font-size: 12px; -fx-font-weight: 600; -fx-text-fill: #1c1e21;");
        details.getChildren().addAll(dur, cap);
        if (isPaid) {
            Label pill = new Label("🎓 Enrolled");
            pill.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; " +
                          "-fx-padding: 4 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");
            details.getChildren().add(pill);
        } else if (!classEntity.hasAvailableSpots()) {
            Label pill = new Label("❌ FULL");
            pill.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                          "-fx-padding: 4 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");
            details.getChildren().add(pill);
        } else {
            Label pill = new Label("✅ Available");
            pill.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; " +
                          "-fx-padding: 4 10; -fx-background-radius: 12; -fx-font-size: 11px; -fx-font-weight: bold;");
            details.getChildren().add(pill);
        }

        // Action buttons
        HBox actions = new HBox(10);
        actions.setAlignment(Pos.CENTER_RIGHT);
        Button viewBtn = new Button("👁️ View Details");
        viewBtn.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; -fx-padding: 9 18; " +
                         "-fx-background-radius: 20; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 12;");
        viewBtn.setOnAction(e -> showClassDetails(classEntity));
        actions.getChildren().add(viewBtn);

        if (isOwnClass) {
            Button b = new Button("🎓 Your Class");
            b.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; -fx-padding: 9 18; " +
                       "-fx-background-radius: 20; -fx-font-weight: bold; -fx-font-size: 12;");
            b.setDisable(true); actions.getChildren().add(b);
        } else if (isPaid) {
            Button watchBtn = new Button("▶️ Watch Class");
            watchBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 9 18; " +
                              "-fx-background-radius: 20; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 12;");
            if (classEntity.getVideoPath() == null || classEntity.getVideoPath().isBlank()) {
                watchBtn.setText("🎬 No Video Yet"); watchBtn.setDisable(true);
            } else {
                watchBtn.setOnAction(e -> showInAppVideoPlayer(classEntity));
            }
            actions.getChildren().add(watchBtn);
        } else if (isPending) {
            Button payBtn = new Button("💳 Complete Payment");
            payBtn.setStyle("-fx-background-color: #635bff; -fx-text-fill: white; -fx-padding: 9 18; " +
                            "-fx-background-radius: 20; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 12;");
            payBtn.setOnAction(e -> handleBookClass(classEntity));
            actions.getChildren().add(payBtn);
        } else {
            Button bookBtn = new Button("📚 Book Now");
            bookBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 9 18; " +
                             "-fx-background-radius: 20; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 12;");
            bookBtn.setDisable(!classEntity.hasAvailableSpots());
            bookBtn.setOnAction(e -> handleBookClass(classEntity));
            actions.getChildren().add(bookBtn);
        }

        content.getChildren().addAll(header, instructorBox, sep, description, details, actions);
        card.getChildren().add(content);
        return card;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CLASS DETAILS DIALOG
    // ─────────────────────────────────────────────────────────────────────────
    private void showClassDetails(ClassEntity classEntity) {
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Class Details");
        dialog.setHeaderText(classEntity.getTitle());

        VBox content = new VBox(15);
        content.setPadding(new Insets(20));

        HBox topInfo = new HBox(20);
        Label priceLabel = new Label("💰 Price: " + String.format("%.2f TND", classEntity.getPrice()));
        priceLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #27ae60; -fx-font-size: 16px;");
        topInfo.getChildren().addAll(new Label("📚 Category: " + classEntity.getCategory()), priceLabel);

        Text description = new Text(classEntity.getDescription());
        description.setWrappingWidth(500);

        content.getChildren().addAll(topInfo, new Separator(), new Label("Description"), description, new Separator());

        // ── Video section — gated by paid status ──────────────────────────────
        if (classEntity.getVideoPath() != null && !classEntity.getVideoPath().isBlank()) {
            VBox videoBox = new VBox(8);
            Label videoTitle = new Label("🎬 Class Video");
            videoTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            videoBox.getChildren().add(videoTitle);

            if (hasPaidBooking(classEntity.getClassId())) {
                Button watchBtn = new Button("▶️ Watch Video");
                watchBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 8 18; " +
                                  "-fx-background-radius: 20; -fx-cursor: hand; -fx-font-weight: bold;");
                watchBtn.setOnAction(evt -> { dialog.close(); showInAppVideoPlayer(classEntity); });
                videoBox.getChildren().add(watchBtn);
            } else {
                Label locked = new Label("🔒 Available after enrollment & payment");
                locked.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 13px; -fx-font-style: italic;");
                videoBox.getChildren().add(locked);
            }
            content.getChildren().add(videoBox);
        }

        ScrollPane sp = new ScrollPane(content);
        sp.setFitToWidth(true);
        sp.setPrefHeight(400);
        dialog.getDialogPane().setContent(sp);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        // Dynamic bottom button
        Long curUserId = SessionManager.getInstance().getUserId();
        if (curUserId != null && !hasPaidBooking(classEntity.getClassId())) {
            Booking existing = bookingController.getUserBookingForClass(curUserId, classEntity.getClassId());
            if (existing != null && existing.getStatus() == BookingStatus.PENDING) {
                ButtonType payType = new ButtonType("💳 Complete Payment", ButtonBar.ButtonData.OK_DONE);
                dialog.getDialogPane().getButtonTypes().add(0, payType);
                ((Button) dialog.getDialogPane().lookupButton(payType))
                        .setOnAction(e -> { dialog.close(); handleStripeCheckout(classEntity, existing); });
            } else if (classEntity.hasAvailableSpots()) {
                ButtonType bookType = new ButtonType("Book Now", ButtonBar.ButtonData.OK_DONE);
                dialog.getDialogPane().getButtonTypes().add(0, bookType);
                ((Button) dialog.getDialogPane().lookupButton(bookType))
                        .setOnAction(e -> { dialog.close(); handleBookClass(classEntity); });
            }
        }
        dialog.showAndWait();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  BOOKING
    // ─────────────────────────────────────────────────────────────────────────
    private void handleBookClass(ClassEntity classEntity) {
        Long userId = SessionManager.getInstance().getUserId();
        if (userId == null) { showAlert("Not Logged In", "Please log in to book a class"); return; }

        ClassProvider provider = providerController.getById(classEntity.getProviderId());
        if (provider != null && provider.getUserId().equals(userId)) {
            showAlert("Cannot Book Own Class", "You cannot book your own class.");
            return;
        }

        Booking existing = bookingController.getUserBookingForClass(userId, classEntity.getClassId());
        if (existing != null) {
            if (existing.getPaymentStatus() == PaymentStatus.PAID) {
                showAlert("Already Enrolled", "You are already enrolled in this class.");
                return;
            }
            handleStripeCheckout(classEntity, existing);
            return;
        }

        if (!bookingController.checkAvailability(classEntity.getClassId())) {
            showAlert("Class Full", "Sorry, this class is fully booked.");
            loadAllClasses();
            return;
        }

        handleStripeCheckout(classEntity, null);
    }

    private void handleStripeCheckout(ClassEntity classEntity, Booking existingBooking) {
        if (!StripePaymentService.isConfigured()) {
            if (existingBooking == null) {
                Booking b = new Booking();
                b.setClassId(classEntity.getClassId());
                b.setUserId(SessionManager.getInstance().getUserId());
                b.setBookingDate(java.time.LocalDateTime.now());
                b.setStatus(BookingStatus.PENDING);
                b.setPaymentStatus(PaymentStatus.PENDING);
                b.setTotalAmount(classEntity.getPrice());
                bookingController.create(b);
            }
            showAlert("Stripe Not Configured", "Saved as Pending.\nSet your API key to enable live payments.");
            loadAllClasses();
            return;
        }

        try {
            final Booking booking;
            if (existingBooking != null) {
                booking = existingBooking;
            } else {
                booking = new Booking();
                booking.setClassId(classEntity.getClassId());
                booking.setUserId(SessionManager.getInstance().getUserId());
                booking.setBookingDate(java.time.LocalDateTime.now());
                booking.setStatus(BookingStatus.PENDING);
                booking.setPaymentStatus(PaymentStatus.PENDING);
                booking.setTotalAmount(classEntity.getPrice());
                if (!bookingController.create(booking)) return;
            }

            long cents = StripePaymentService.tndToEurCents(classEntity.getPrice());
            com.stripe.model.checkout.Session session = StripePaymentService.createCheckoutSession(
                    classEntity.getTitle(), cents, "eur", booking.getBookingId());

            Stage paymentStage = new Stage();
            paymentStage.setTitle("🔒 Secure Payment — " + classEntity.getTitle());
            paymentStage.initModality(Modality.APPLICATION_MODAL);

            WebView webView = new WebView();
            WebEngine webEngine = webView.getEngine();

            webEngine.locationProperty().addListener((obs, oldLoc, newLoc) -> {
                if (newLoc == null) return;
                if (newLoc.contains("/stripe/success")) {
                    String sessionId = extractUrlParam(newLoc, "session_id");
                    String bIdS      = extractUrlParam(newLoc, "booking_id");
                    Platform.runLater(() -> {
                        paymentStage.close();
                        try {
                            if (StripePaymentService.verifySessionPaid(sessionId)) {
                                Long bId = Long.parseLong(bIdS);
                                bookingController.updateStatus(bId, BookingStatus.SCHEDULED);
                                bookingController.updatePaymentStatus(bId, PaymentStatus.PAID);
                                showAlert("Payment Successful! 🎉",
                                        "Enrollment confirmed!\nClass: " + classEntity.getTitle() +
                                        "\n\nClick '▶️ Watch Class' on the card to start watching.");
                                loadAllClasses();
                            }
                        } catch (Exception ex) { ex.printStackTrace(); }
                    });
                } else if (newLoc.contains("/stripe/cancel")) {
                    Platform.runLater(paymentStage::close);
                }
            });

            // ── Header bar with security info and cancel button ──────────────
            HBox payHeader = new HBox(10);
            payHeader.setAlignment(Pos.CENTER_LEFT);
            payHeader.setStyle("-fx-background-color: #1c1e21; -fx-padding: 10 18;");
            Label secureLabel = new Label("🔒 Secure Stripe Checkout");
            secureLabel.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 14px;");
            Label amtLabel = new Label(String.format("%.2f TND", classEntity.getPrice()));
            amtLabel.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 0 0 0 20;");
            Region payHeaderSpacer = new Region();
            HBox.setHgrow(payHeaderSpacer, Priority.ALWAYS);
            Button payCancelBtn = new Button("✕ Cancel");
            payCancelBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 5 15; -fx-background-radius: 15; -fx-cursor: hand;");
            payCancelBtn.setOnAction(e -> paymentStage.close());
            payHeader.getChildren().addAll(secureLabel, amtLabel, payHeaderSpacer, payCancelBtn);

            VBox payRoot = new VBox(0, payHeader, webView);
            VBox.setVgrow(webView, Priority.ALWAYS);

            webEngine.load(session.getUrl());
            paymentStage.setScene(new Scene(payRoot, 920, 760));
            paymentStage.show();
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  IN-APP VIDEO PLAYER  (always plays inside the app)
    // ─────────────────────────────────────────────────────────────────────────
    private void showInAppVideoPlayer(ClassEntity classEntity) {
        String path = classEntity.getVideoPath();
        if (path == null || path.isBlank()) { showAlert("No Video", "No video is attached."); return; }
        File videoFile = new File(path);
        if (!videoFile.exists()) { showAlert("File Not Found", "Video not found at:\n" + path); return; }

        Long userId = SessionManager.getInstance().getUserId();
        Booking booking = (userId != null)
                ? bookingController.getUserBookingForClass(userId, classEntity.getClassId()) : null;
        int savedSeconds = (booking != null) ? booking.getWatchProgress() : 0;

        Media media;
        try { media = new Media(videoFile.toURI().toString()); }
        catch (Exception ex) { showAlert("Error", "Cannot load video file:\n" + ex.getMessage()); return; }

        MediaPlayer player = new MediaPlayer(media);

        // Resume from saved position
        if (savedSeconds > 0)
            player.setOnReady(() -> player.seek(Duration.seconds(savedSeconds)));

        // On error just show a simple alert — never open externally
        player.setOnError(() -> Platform.runLater(() -> {
            String msg = player.getError() != null ? player.getError().getMessage() : "Unknown error";
            showAlert("Playback Error", "The video encountered an error:\n" + msg +
                    "\n\nTry re-encoding to H.264 MP4 if the issue persists.");
        }));

        final Booking[] bRef = { booking };
        Timeline autoSave = new Timeline(new KeyFrame(Duration.seconds(15), e -> {
            if (bRef[0] != null) bookingController.updateWatchProgress(
                    bRef[0].getBookingId(), (int) player.getCurrentTime().toSeconds());
        }));
        autoSave.setCycleCount(Timeline.INDEFINITE);
        autoSave.play();

        // ── UI ────────────────────────────────────────────────────────────────
        MediaView mediaView = new MediaView(player);
        mediaView.setFitWidth(960); mediaView.setFitHeight(540); mediaView.setPreserveRatio(true);

        Stage videoStage = new Stage();
        videoStage.setTitle("▶️  " + classEntity.getTitle());
        videoStage.initModality(Modality.APPLICATION_MODAL);

        Label titleBar = new Label("▶️  " + classEntity.getTitle());
        titleBar.setMaxWidth(Double.MAX_VALUE);
        titleBar.setStyle("-fx-background-color: #1c1e21; -fx-text-fill: white; " +
                          "-fx-font-weight: bold; -fx-font-size: 14px; -fx-padding: 10 18;");

        Button playPauseBtn = new Button("⏸");
        styleControlBtn(playPauseBtn, "#667eea");
        playPauseBtn.setOnAction(e -> {
            if (player.getStatus() == MediaPlayer.Status.PLAYING) { player.pause(); playPauseBtn.setText("▶"); }
            else { player.play(); playPauseBtn.setText("⏸"); }
        });
        Button stopBtn = new Button("⏹");
        styleControlBtn(stopBtn, "#e74c3c");
        stopBtn.setOnAction(e -> { player.stop(); playPauseBtn.setText("▶"); });

        Slider progressSlider = new Slider(0, 1, 0);
        progressSlider.setStyle("-fx-accent: #667eea;");
        HBox.setHgrow(progressSlider, Priority.ALWAYS);
        Label timeLabel = new Label("0:00 / 0:00");
        timeLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px; -fx-min-width: 90;");

        Label volIcon = new Label("🔊");
        volIcon.setStyle("-fx-text-fill: white;");
        Slider volumeSlider = new Slider(0, 1, 1);
        volumeSlider.setStyle("-fx-accent: #667eea;"); volumeSlider.setPrefWidth(90);
        volumeSlider.valueProperty().addListener((obs, o, v) -> player.setVolume(v.doubleValue()));

        player.currentTimeProperty().addListener((obs, oldT, newT) -> {
            javafx.util.Duration total = player.getTotalDuration();
            if (!progressSlider.isValueChanging() && total != null && total.toSeconds() > 0) {
                progressSlider.setValue(newT.toSeconds() / total.toSeconds());
                int cur = (int) newT.toSeconds(), tot = (int) total.toSeconds();
                timeLabel.setText(String.format("%d:%02d / %d:%02d", cur/60, cur%60, tot/60, tot%60));
            }
        });
        progressSlider.valueChangingProperty().addListener((obs, was, changing) -> {
            if (!changing) player.seek(player.getTotalDuration().multiply(progressSlider.getValue()));
        });
        progressSlider.setOnMousePressed(e ->
            player.seek(player.getTotalDuration().multiply(progressSlider.getValue())));

        HBox controls = new HBox(10, playPauseBtn, stopBtn, progressSlider, timeLabel, volIcon, volumeSlider);
        controls.setAlignment(Pos.CENTER_LEFT);
        controls.setStyle("-fx-background-color: #1c1e21; -fx-padding: 10 18;");

        StackPane videoPane = new StackPane(mediaView);
        videoPane.setStyle("-fx-background-color: black;");

        if (savedSeconds > 0) {
            Label resumeBanner = new Label(String.format("⏩  Resuming from %d:%02d", savedSeconds/60, savedSeconds%60));
            resumeBanner.setStyle("-fx-background-color: rgba(0,0,0,0.7); -fx-text-fill: #4CAF50; " +
                                  "-fx-font-size: 13px; -fx-font-weight: bold; -fx-padding: 6 16; -fx-background-radius: 20;");
            StackPane.setAlignment(resumeBanner, Pos.TOP_CENTER);
            videoPane.getChildren().add(resumeBanner);
            new Timeline(new KeyFrame(Duration.seconds(3), e -> videoPane.getChildren().remove(resumeBanner))).play();
        }

        VBox root = new VBox(0, titleBar, videoPane, controls);
        VBox.setVgrow(videoPane, Priority.ALWAYS);
        root.setStyle("-fx-background-color: black;");

        videoStage.setScene(new Scene(root, 980, 640));
        videoStage.setOnCloseRequest(e -> {
            autoSave.stop();
            if (bRef[0] != null) bookingController.updateWatchProgress(
                    bRef[0].getBookingId(), (int) player.getCurrentTime().toSeconds());
            player.stop(); player.dispose();
        });

        // ── Completion → certificate + rating ────────────────────────────────
        player.setOnEndOfMedia(() -> Platform.runLater(() -> {
            autoSave.stop();
            playPauseBtn.setText("▶");
            if (bRef[0] != null && player.getTotalDuration() != null)
                bookingController.updateWatchProgress(bRef[0].getBookingId(),
                        (int) player.getTotalDuration().toSeconds());

            if (bRef[0] != null
                    && bRef[0].getPaymentStatus() == PaymentStatus.PAID
                    && bRef[0].getStatus() != BookingStatus.COMPLETED) {

                bookingController.complete(bRef[0].getBookingId());
                bRef[0].setStatus(BookingStatus.COMPLETED);

                String certPath = CertificateService.generate(
                        bRef[0].getUserFullName() != null ? bRef[0].getUserFullName()
                                : SessionManager.getInstance().getUsername(),
                        classEntity.getTitle(),
                        bRef[0].getProviderName() != null ? bRef[0].getProviderName() : "Instructor",
                        java.time.LocalDateTime.now());

                Alert doneAlert = new Alert(Alert.AlertType.INFORMATION);
                doneAlert.setTitle("🎉 Video Complete!");
                doneAlert.setHeaderText("You've finished: " + classEntity.getTitle());
                doneAlert.setContentText(certPath != null
                        ? "🎓 Certificate saved to:\n" + certPath + "\n\nWould you like to rate this class?"
                        : "Great job finishing the class! Would you like to rate it?");

                ButtonType rateType = new ButtonType("⭐ Rate Class", ButtonBar.ButtonData.OK_DONE);
                ButtonType certType = certPath != null
                        ? new ButtonType("📂 Open Certificate", ButtonBar.ButtonData.LEFT) : null;
                ButtonType laterType = new ButtonType("Later", ButtonBar.ButtonData.CANCEL_CLOSE);
                if (certType != null) doneAlert.getButtonTypes().setAll(certType, rateType, laterType);
                else                  doneAlert.getButtonTypes().setAll(rateType, laterType);

                final String fp = certPath;
                doneAlert.showAndWait().ifPresent(b -> {
                    if (certType != null && b == certType) {
                        try { java.awt.Desktop.getDesktop().open(new File(fp)); }
                        catch (Exception ex) { showAlert("Error", "Could not open certificate: " + ex.getMessage()); }
                    } else if (b == rateType) {
                        videoStage.close();
                        player.dispose();
                        showRatingDialog(bRef[0], null);
                    }
                });
                loadAllClasses();
            } else {
                showAlert("🎉 Done!", "You've finished watching:\n" + classEntity.getTitle());
            }
        }));

        videoStage.show();
        player.play();
    }

    private void styleControlBtn(Button btn, String color) {
        btn.setStyle("-fx-background-color: " + color + "; -fx-text-fill: white; " +
                     "-fx-padding: 8 18; -fx-background-radius: 20; -fx-cursor: hand; -fx-font-size: 15;");
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  MY BOOKINGS
    // ─────────────────────────────────────────────────────────────────────────
    // ─────────────────────────────────────────────────────────────────────────
    //  MY BOOKINGS — redesigned modal
    // ─────────────────────────────────────────────────────────────────────────
    private void handleMyBookings() {
        Long userId = SessionManager.getInstance().getUserId();
        if (userId == null) { showAlert("Not Logged In", "Please log in to view your bookings."); return; }

        List<Booking> bookings = bookingController.getByUserId(userId, null);

        // ── Custom Stage for full layout control ─────────────────────────────
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle("My Bookings");
        modal.setMinWidth(680);
        modal.setMinHeight(500);

        // ── Top header bar ───────────────────────────────────────────────────
        HBox headerBar = new HBox();
        headerBar.setAlignment(Pos.CENTER_LEFT);
        headerBar.setPadding(new Insets(22, 28, 22, 28));
        headerBar.setStyle("-fx-background-color: linear-gradient(to right, #667eea, #764ba2);");

        VBox headerText = new VBox(4);
        Label headerTitle = new Label("📋  My Bookings");
        headerTitle.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label headerSub = new Label(bookings.size() + " booking" + (bookings.size() != 1 ? "s" : "") + " found");
        headerSub.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.75);");
        headerText.getChildren().addAll(headerTitle, headerSub);

        Region headerSpacer = new Region();
        HBox.setHgrow(headerSpacer, Priority.ALWAYS);

        // Stats pills row
        long paidCount    = bookings.stream().filter(b -> b.getPaymentStatus() == PaymentStatus.PAID).count();
        long pendingCount = bookings.stream().filter(b -> b.getStatus() == BookingStatus.PENDING).count();

        HBox pills = new HBox(8);
        pills.setAlignment(Pos.CENTER_RIGHT);
        if (paidCount > 0) {
            Label paidPill = new Label("✅ " + paidCount + " Enrolled");
            paidPill.setStyle("-fx-background-color: rgba(255,255,255,0.25); -fx-text-fill: white; " +
                             "-fx-padding: 5 12; -fx-background-radius: 20; -fx-font-size: 12px; -fx-font-weight: bold;");
            pills.getChildren().add(paidPill);
        }
        if (pendingCount > 0) {
            Label pendingPill = new Label("⏳ " + pendingCount + " Pending");
            pendingPill.setStyle("-fx-background-color: rgba(255,255,255,0.25); -fx-text-fill: white; " +
                                "-fx-padding: 5 12; -fx-background-radius: 20; -fx-font-size: 12px; -fx-font-weight: bold;");
            pills.getChildren().add(pendingPill);
        }

        headerBar.getChildren().addAll(headerText, headerSpacer, pills);

        // ── Scrollable card list ─────────────────────────────────────────────
        VBox cardList = new VBox(14);
        cardList.setPadding(new Insets(24, 28, 24, 28));
        cardList.setStyle("-fx-background-color: #f4f5f7;");

        if (bookings.isEmpty()) {
            // Beautiful empty state
            VBox emptyState = new VBox(12);
            emptyState.setAlignment(Pos.CENTER);
            emptyState.setPadding(new Insets(60, 0, 60, 0));
            Label emptyIcon = new Label("🎓");
            emptyIcon.setStyle("-fx-font-size: 52px;");
            Label emptyTitle = new Label("No bookings yet");
            emptyTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #1c1e21;");
            Label emptyDesc = new Label("Browse the marketplace and enroll in a class!");
            emptyDesc.setStyle("-fx-font-size: 13px; -fx-text-fill: #65676b;");
            emptyState.getChildren().addAll(emptyIcon, emptyTitle, emptyDesc);
            cardList.getChildren().add(emptyState);
        } else {
            for (Booking b : bookings) {
                cardList.getChildren().add(createBookingCard(b, modal));
            }
        }

        ScrollPane scrollPane = new ScrollPane(cardList);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: #f4f5f7; -fx-background: #f4f5f7;");
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        // ── Bottom close bar ─────────────────────────────────────────────────
        HBox bottomBar = new HBox();
        bottomBar.setAlignment(Pos.CENTER_RIGHT);
        bottomBar.setPadding(new Insets(14, 28, 14, 28));
        bottomBar.setStyle("-fx-background-color: white; -fx-border-color: #e4e6eb; -fx-border-width: 1 0 0 0;");

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; -fx-padding: 10 30; " +
                         "-fx-background-radius: 20; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 13px;");
        closeBtn.setOnAction(e -> modal.close());
        bottomBar.getChildren().add(closeBtn);

        VBox root = new VBox(0, headerBar, scrollPane, bottomBar);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);

        Scene scene = new Scene(root, 700, 560);
        modal.setScene(scene);
        modal.showAndWait();
    }

    /**
     * Creates a fully redesigned booking card.
     * @param modal the parent Stage, passed so cancel can close & reopen it
     */
    private VBox createBookingCard(Booking booking, Stage modal) {
        boolean isPaid    = booking.getPaymentStatus() == PaymentStatus.PAID;
        boolean isPending = booking.getStatus() == BookingStatus.PENDING;
        boolean canCancel = booking.getStatus() == BookingStatus.SCHEDULED || isPending;

        // ── Card container ───────────────────────────────────────────────────
        VBox card = new VBox(0);
        card.setStyle("-fx-background-color: white; -fx-background-radius: 14; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 12, 0, 0, 4);");

        // ── Coloured top accent strip (green=paid, orange=pending, grey=other)
        String accentColor = isPaid ? "#27ae60" : isPending ? "#f39c12" : "#95a5a6";
        HBox accentStrip = new HBox();
        accentStrip.setPrefHeight(5);
        accentStrip.setStyle("-fx-background-color: " + accentColor + "; " +
                            "-fx-background-radius: 14 14 0 0;");

        // ── Card body ────────────────────────────────────────────────────────
        VBox body = new VBox(12);
        body.setPadding(new Insets(18, 20, 18, 20));

        // Row 1: Title + Price
        HBox titleRow = new HBox(10);
        titleRow.setAlignment(Pos.CENTER_LEFT);

        Label titleLbl = new Label(booking.getClassTitle());
        titleLbl.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #1c1e21;");
        titleLbl.setWrapText(true);
        HBox.setHgrow(titleLbl, Priority.ALWAYS);

        Label priceLbl = new Label(String.format("%.2f TND", booking.getTotalAmount()));
        priceLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #27ae60;");

        titleRow.getChildren().addAll(titleLbl, priceLbl);

        // Row 2: Instructor + Date as icon-text pairs
        HBox metaRow = new HBox(20);
        metaRow.setAlignment(Pos.CENTER_LEFT);

        Label instructorLbl = new Label("👨‍🏫  " + booking.getProviderName());
        instructorLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #65676b;");

        // Format date nicely: yyyy-MM-dd HH:mm
        String dateStr = booking.getBookingDate()
            .format(java.time.format.DateTimeFormatter.ofPattern("MMM dd, yyyy  HH:mm"));
        Label dateLbl = new Label("🗓  " + dateStr);
        dateLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #65676b;");

        metaRow.getChildren().addAll(instructorLbl, dateLbl);

        // Row 3: Status badges
        HBox badgeRow = new HBox(8);
        badgeRow.setAlignment(Pos.CENTER_LEFT);

        // Booking status badge
        String statusColor = switch (booking.getStatus()) {
            case PENDING   -> "#fff3cd;-fx-text-fill:#856404";
            case SCHEDULED -> "#cce5ff;-fx-text-fill:#004085";
            case COMPLETED -> "#d4edda;-fx-text-fill:#155724";
            case CANCELLED -> "#f8d7da;-fx-text-fill:#721c24";
        };
        String statusIcon = switch (booking.getStatus()) {
            case PENDING   -> "⏳";
            case SCHEDULED -> "📅";
            case COMPLETED -> "✅";
            case CANCELLED -> "❌";
        };
        Label statusBadge = new Label(statusIcon + "  " + booking.getStatus().getValue());
        statusBadge.setStyle("-fx-background-color: " + statusColor + "; -fx-padding: 4 12; " +
                            "-fx-background-radius: 20; -fx-font-size: 12px; -fx-font-weight: bold;");

        // Payment status badge
        String payColor = switch (booking.getPaymentStatus()) {
            case PAID     -> "#d4edda;-fx-text-fill:#155724";
            case PENDING  -> "#fff3cd;-fx-text-fill:#856404";
            case REFUNDED -> "#e2e3e5;-fx-text-fill:#383d41";
        };
        String payIcon = switch (booking.getPaymentStatus()) {
            case PAID     -> "💳";
            case PENDING  -> "⏳";
            case REFUNDED -> "↩️";
        };
        Label payBadge = new Label(payIcon + "  " + booking.getPaymentStatus().getValue());
        payBadge.setStyle("-fx-background-color: " + payColor + "; -fx-padding: 4 12; " +
                         "-fx-background-radius: 20; -fx-font-size: 12px; -fx-font-weight: bold;");

        badgeRow.getChildren().addAll(statusBadge, payBadge);

        // Separator
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: #f0f2f5;");

        // Row 4: Action buttons
        HBox actionRow = new HBox(10);
        actionRow.setAlignment(Pos.CENTER_RIGHT);
        actionRow.setPadding(new Insets(4, 0, 0, 0));

        // ▶️ Watch — paid bookings with video
        if (isPaid) {
            ClassEntity ce = classController.getById(booking.getClassId());
            if (ce != null && ce.getVideoPath() != null && !ce.getVideoPath().isBlank()) {
                Button watchBtn = new Button("▶️  Watch Video");
                watchBtn.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-padding: 9 20; " +
                                 "-fx-background-radius: 20; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 13px;");
                watchBtn.setOnAction(e -> showInAppVideoPlayer(ce));
                actionRow.getChildren().add(watchBtn);
            }
        }

        // 💳 Pay — pending bookings
        if (isPending) {
            Button payBtn = new Button("💳  Complete Payment");
            payBtn.setStyle("-fx-background-color: #635bff; -fx-text-fill: white; -fx-padding: 9 20; " +
                           "-fx-background-radius: 20; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 13px;");
            payBtn.setOnAction(e -> {
                ClassEntity ce = classController.getById(booking.getClassId());
                if (ce != null) { modal.close(); handleStripeCheckout(ce, booking); }
            });
            actionRow.getChildren().add(payBtn);
        }

        // ⭐ Rate — completed & not yet rated
        if (booking.getStatus() == BookingStatus.COMPLETED && !booking.isRated()) {
            Button rateBtn = new Button("⭐  Rate Class");
            rateBtn.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-padding: 9 20; " +
                            "-fx-background-radius: 20; -fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 13px;");
            rateBtn.setOnAction(e -> showRatingDialog(booking, modal));
            actionRow.getChildren().add(rateBtn);
        }

        // ✅ Rated badge — already rated
        if (booking.isRated()) {
            HBox ratedBadge = new HBox(4);
            ratedBadge.setAlignment(Pos.CENTER);
            ratedBadge.setPadding(new Insets(6, 14, 6, 14));
            ratedBadge.setStyle("-fx-background-color: #fff3cd; -fx-background-radius: 20;");
            // Show the star fill
            for (int i = 1; i <= 5; i++) {
                Label s = new Label(i <= booking.getRating() ? "★" : "☆");
                s.setStyle("-fx-font-size: 14px; -fx-text-fill: " + (i <= booking.getRating() ? "#f39c12" : "#ccc") + ";");
                ratedBadge.getChildren().add(s);
            }
            Label ratedLbl = new Label("  Your rating");
            ratedLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #856404; -fx-font-weight: bold;");
            ratedBadge.getChildren().add(ratedLbl);
            actionRow.getChildren().add(ratedBadge);
        }

        // ❌ Cancel — pending or scheduled
        if (canCancel) {
            Button cancelBtn = new Button("✕  Cancel");
            cancelBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #e74c3c; -fx-padding: 9 20; " +
                              "-fx-border-color: #e74c3c; -fx-border-radius: 20; -fx-background-radius: 20; " +
                              "-fx-cursor: hand; -fx-font-weight: bold; -fx-font-size: 13px;");
            cancelBtn.setOnAction(e -> { modal.close(); handleCancelBooking(booking); });
            actionRow.getChildren().add(cancelBtn);
        }

        body.getChildren().addAll(titleRow, metaRow, badgeRow, sep, actionRow);
        card.getChildren().addAll(accentStrip, body);
        return card;
    }

    // legacy overload so existing internal calls still compile
    private VBox createBookingCard(Booking booking) {
        return createBookingCard(booking, new Stage());
    }

    private void handleCancelBooking(Booking booking) {
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Cancel Booking");
        confirmation.setHeaderText("Are you sure?");
        confirmation.setContentText("Do you want to cancel your booking for \"" + booking.getClassTitle() + "\"?");

        confirmation.showAndWait().ifPresent(btn -> {
            if (btn == ButtonType.OK) {
                boolean success = bookingController.cancel(booking.getBookingId());
                if (success) {
                    showAlert("Booking Cancelled", "Your booking has been cancelled.");
                    handleMyBookings(); // Refresh the dialog
                    loadAllClasses();  // Refresh the marketplace
                } else {
                    showAlert("Error", "Unable to cancel booking. Please try again.");
                }
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  BECOME INSTRUCTOR
    // ─────────────────────────────────────────────────────────────────────────
    private void handleBecomeInstructor() {
        Long userId = SessionManager.getInstance().getUserId();
        if (userId == null) { showAlert("Not Logged In", "Please log in first."); return; }
        if (providerController.isUserProvider(userId)) {
            showAlert("Already Registered", "You are already registered as an instructor.");
            return;
        }

        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Become an Instructor");
        dialog.setHeaderText("Apply to teach on Ghrami");
        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        TextField companyField = new TextField();
        companyField.setPromptText("Company name (optional)");
        TextArea expertiseArea = new TextArea();
        expertiseArea.setPromptText("Describe your expertise and qualifications");
        expertiseArea.setPrefRowCount(4);
        content.getChildren().addAll(new Label("Company Name:"), companyField,
                new Label("Expertise:"), expertiseArea);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        if (dialog.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            String expertise = expertiseArea.getText().trim();
            if (expertise.isEmpty()) { showAlert("Invalid Input", "Please describe your expertise."); return; }
            ClassProvider p = new ClassProvider();
            p.setUserId(userId);
            p.setCompanyName(companyField.getText().trim().isEmpty() ? null : companyField.getText().trim());
            p.setExpertise(expertise);
            p.setVerified(false);
            if (providerController.create(p)) showAlert("Submitted", "Admin review pending.");
            else showAlert("Error", "Could not submit application. Please try again.");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  HELPERS
    // ─────────────────────────────────────────────────────────────────────────
    private boolean hasPaidBooking(Long classId) {
        Long userId = SessionManager.getInstance().getUserId();
        if (userId == null || classId == null) return false;
        Booking b = bookingController.getUserBookingForClass(userId, classId);
        return b != null && b.getPaymentStatus() == PaymentStatus.PAID;
    }

    private String extractUrlParam(String url, String param) {
        try {
            int qIdx = url.indexOf('?');
            if (qIdx < 0) return null;
            for (String pair : url.substring(qIdx + 1).split("&")) {
                String[] kv = pair.split("=", 2);
                if (kv.length == 2 && kv[0].equals(param))
                    return URLDecoder.decode(kv[1], StandardCharsets.UTF_8);
            }
        } catch (Exception e) { e.printStackTrace(); }
        return null;
    }

    private String getStatusStyle(BookingStatus s) {
        return switch (s) {
            case PENDING   -> "-fx-text-fill: #ff9800; -fx-font-weight: bold;";
            case SCHEDULED -> "-fx-text-fill: #3498db; -fx-font-weight: bold;";
            case COMPLETED -> "-fx-text-fill: #27ae60; -fx-font-weight: bold;";
            case CANCELLED -> "-fx-text-fill: #e74c3c; -fx-font-weight: bold;";
        };
    }

    private String getPaymentStatusStyle(PaymentStatus s) {
        return switch (s) {
            case PENDING  -> "-fx-text-fill: #f39c12; -fx-font-weight: bold;";
            case PAID     -> "-fx-text-fill: #27ae60; -fx-font-weight: bold;";
            case REFUNDED -> "-fx-text-fill: #95a5a6; -fx-font-weight: bold;";
        };
    }

    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    private void handleBack() {
        try {
            Stage stage = (Stage) searchField.getScene().getWindow();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/UserFeed.fxml"));
            Scene scene = new Scene(loader.load(), stage.getWidth(), stage.getHeight());
            scene.getStylesheets().add(getClass().getResource("/css/social-style.css").toExternalForm());
            stage.setScene(scene);
            stage.setTitle("Ghrami - Social Platform");
        } catch (Exception e) { e.printStackTrace(); }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  FEATURE A — INSTRUCTOR PROFILE POPUP
    // ─────────────────────────────────────────────────────────────────────────
    private void showInstructorProfile(Long providerId) {
        ClassProvider provider = providerController.getById(providerId);
        if (provider == null) { showAlert("Not Found", "Instructor profile not available."); return; }
        List<ClassEntity> theirClasses = classController.getByProviderId(providerId);

        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle("Instructor Profile");

        // Header gradient
        VBox headerVBox = new VBox(10);
        headerVBox.setAlignment(Pos.CENTER);
        headerVBox.setPadding(new Insets(28, 24, 22, 24));
        headerVBox.setStyle("-fx-background-color: linear-gradient(to bottom right, #667eea, #764ba2);");

        // Avatar: profile pic or initials circle
        StackPane avatar = new StackPane();
        avatar.setPrefSize(90, 90); avatar.setMaxSize(90, 90);
        String pic = provider.getProfilePicture();
        if (pic != null && !pic.isBlank() && new File(pic).exists()) {
            try {
                ImageView iv = new ImageView(new Image(new File(pic).toURI().toString()));
                iv.setFitWidth(90); iv.setFitHeight(90); iv.setPreserveRatio(false);
                Circle clip = new Circle(45, 45, 45); iv.setClip(clip);
                avatar.getChildren().add(iv);
            } catch (Exception ignored) {}
        }
        if (avatar.getChildren().isEmpty()) {
            Circle bg = new Circle(45); bg.setFill(Color.web("#ffffff", 0.25));
            String initials = (provider.getFullName() != null && !provider.getFullName().isBlank())
                    ? String.valueOf(provider.getFullName().charAt(0)).toUpperCase() : "?";
            Label init = new Label(initials);
            init.setStyle("-fx-font-size: 38px; -fx-font-weight: bold; -fx-text-fill: white;");
            avatar.getChildren().addAll(bg, init);
        }

        Label nameLabel = new Label(provider.getFullName() != null ? provider.getFullName() : provider.getUsername());
        nameLabel.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label usernameLabel = new Label("@" + provider.getUsername());
        usernameLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.75);");
        HBox stars = buildStaticStars(provider.getRating());
        headerVBox.getChildren().addAll(avatar, nameLabel, usernameLabel, stars);
        if (provider.getCompanyName() != null && !provider.getCompanyName().isBlank()) {
            Label co = new Label("🏢  " + provider.getCompanyName());
            co.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.85);");
            headerVBox.getChildren().add(co);
        }

        // Stats bar
        HBox statsBar = new HBox(0);
        statsBar.setStyle("-fx-background-color: #1c1e21;");
        statsBar.getChildren().addAll(
            statCell("📚", String.valueOf(theirClasses.size()), "Classes"),
            statSep(),
            statCell("⭐", provider.getRating() > 0 ? String.format("%.1f", provider.getRating()) : "New", "Rating"),
            statSep(),
            statCell("✅", provider.isVerified() ? "Verified" : "Pending", "Status")
        );

        // Body
        VBox body = new VBox(16);
        body.setPadding(new Insets(20, 24, 24, 24));
        body.setStyle("-fx-background-color: #f4f5f7;");

        if (provider.getBio() != null && !provider.getBio().isBlank()) {
            Label bioTitle = new Label("About"); bioTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1c1e21;");
            Label bioText = new Label(provider.getBio()); bioText.setWrapText(true);
            bioText.setStyle("-fx-font-size: 13px; -fx-text-fill: #65676b;");
            body.getChildren().addAll(bioTitle, bioText);
        }
        if (provider.getExpertise() != null && !provider.getExpertise().isBlank()) {
            Label expTitle = new Label("🎯  Expertise"); expTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1c1e21;");
            Label expText = new Label(provider.getExpertise()); expText.setWrapText(true);
            expText.setStyle("-fx-font-size: 13px; -fx-text-fill: #65676b; -fx-background-color: white; -fx-padding: 10 14; -fx-background-radius: 10;");
            body.getChildren().addAll(expTitle, expText);
        }
        if (!theirClasses.isEmpty()) {
            Label classesTitle = new Label("📖  Classes");
            classesTitle.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #1c1e21;");
            body.getChildren().add(classesTitle);
            for (ClassEntity ce : theirClasses) {
                HBox row = new HBox(12); row.setAlignment(Pos.CENTER_LEFT);
                row.setPadding(new Insets(12, 14, 12, 14));
                row.setStyle("-fx-background-color: white; -fx-background-radius: 12; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 8, 0, 0, 2);");
                VBox info = new VBox(3);
                Label t = new Label(ce.getTitle()); t.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #1c1e21;");
                Label sub = new Label("⏱ " + ce.getDuration() + " min  ·  👥 " + ce.getCurrentEnrollment() + "/" + ce.getMaxParticipants());
                sub.setStyle("-fx-font-size: 11px; -fx-text-fill: #65676b;");
                info.getChildren().addAll(t, sub); HBox.setHgrow(info, Priority.ALWAYS);
                Label priceL = new Label(String.format("%.0f TND", ce.getPrice()));
                priceL.setStyle("-fx-font-weight: bold; -fx-text-fill: #27ae60; -fx-font-size: 14px;");
                Button enrollBtn = new Button(ce.hasAvailableSpots() ? "Book" : "Full");
                enrollBtn.setStyle("-fx-background-color: " + (ce.hasAvailableSpots() ? "#667eea" : "#95a5a6") +
                        "; -fx-text-fill: white; -fx-padding: 6 14; -fx-background-radius: 15; -fx-cursor: hand; -fx-font-size: 11px; -fx-font-weight: bold;");
                enrollBtn.setDisable(!ce.hasAvailableSpots());
                enrollBtn.setOnAction(e -> { modal.close(); handleBookClass(ce); });
                row.getChildren().addAll(info, priceL, enrollBtn);
                body.getChildren().add(row);
            }
        }

        ScrollPane scroll = new ScrollPane(body);
        scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color: #f4f5f7; -fx-background: #f4f5f7;");
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);

        Button closeBtn = new Button("Close");
        closeBtn.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; -fx-padding: 10 30; -fx-background-radius: 20; -fx-cursor: hand; -fx-font-weight: bold;");
        closeBtn.setOnAction(e -> modal.close());
        HBox bottom = new HBox(closeBtn); bottom.setAlignment(Pos.CENTER_RIGHT);
        bottom.setPadding(new Insets(12, 24, 12, 24));
        bottom.setStyle("-fx-background-color: white; -fx-border-color: #e4e6eb; -fx-border-width: 1 0 0 0;");

        VBox root = new VBox(0, headerVBox, statsBar, scroll, bottom);
        VBox.setVgrow(scroll, Priority.ALWAYS);
        modal.setScene(new Scene(root, 520, 660));
        modal.showAndWait();
    }

    private HBox buildStaticStars(double rating) {
        HBox row = new HBox(3); row.setAlignment(Pos.CENTER);
        for (int i = 1; i <= 5; i++) {
            Label s = new Label(i <= Math.round(rating) ? "★" : "☆");
            s.setStyle("-fx-font-size: 18px; -fx-text-fill: " + (i <= Math.round(rating) ? "#f39c12" : "rgba(255,255,255,0.4)") + ";");
            row.getChildren().add(s);
        }
        if (rating > 0) { Label n = new Label(String.format("  %.1f", rating)); n.setStyle("-fx-font-size: 13px; -fx-text-fill: rgba(255,255,255,0.8);"); row.getChildren().add(n); }
        return row;
    }

    private VBox statCell(String icon, String value, String label) {
        VBox c = new VBox(2); c.setAlignment(Pos.CENTER); c.setPadding(new Insets(12, 0, 12, 0)); HBox.setHgrow(c, Priority.ALWAYS);
        Label ic = new Label(icon + "  " + value); ic.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label lb = new Label(label); lb.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.6);");
        c.getChildren().addAll(ic, lb); return c;
    }

    private javafx.scene.shape.Line statSep() {
        javafx.scene.shape.Line l = new javafx.scene.shape.Line(0, 0, 0, 36);
        l.setStroke(Color.web("#ffffff", 0.2)); return l;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  FEATURE B — RATING & REVIEW DIALOG
    // ─────────────────────────────────────────────────────────────────────────
    void showRatingDialog(Booking booking, Stage parentModal) {
        Stage modal = new Stage();
        modal.initModality(Modality.APPLICATION_MODAL);
        modal.setTitle("Rate this class");

        HBox headerBar = new HBox(); headerBar.setAlignment(Pos.CENTER);
        headerBar.setPadding(new Insets(20, 24, 20, 24));
        headerBar.setStyle("-fx-background-color: linear-gradient(to right, #667eea, #764ba2);");
        Label ht = new Label("⭐  How was the class?");
        ht.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        headerBar.getChildren().add(ht);

        Label classTitleLbl = new Label(booking.getClassTitle());
        classTitleLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #65676b; -fx-font-style: italic;");
        classTitleLbl.setWrapText(true);

        int[] selectedRating = { 0 };
        Label[] starLabels = new Label[5];
        HBox starsRow = new HBox(6); starsRow.setAlignment(Pos.CENTER);
        for (int i = 0; i < 5; i++) {
            final int starNum = i + 1;
            Label star = new Label("☆");
            star.setStyle("-fx-font-size: 40px; -fx-text-fill: #d0d0d0; -fx-cursor: hand;");
            star.setOnMouseEntered(e -> {
                for (int j = 0; j < starNum; j++) starLabels[j].setStyle("-fx-font-size: 40px; -fx-text-fill: #f39c12; -fx-cursor: hand;");
                for (int j = starNum; j < 5; j++) starLabels[j].setStyle("-fx-font-size: 40px; -fx-text-fill: #d0d0d0; -fx-cursor: hand;");
            });
            star.setOnMouseClicked(e -> {
                selectedRating[0] = starNum;
                for (int j = 0; j < starNum; j++) { starLabels[j].setText("★"); starLabels[j].setStyle("-fx-font-size: 40px; -fx-text-fill: #f39c12; -fx-cursor: hand;"); }
                for (int j = starNum; j < 5; j++) { starLabels[j].setText("☆"); starLabels[j].setStyle("-fx-font-size: 40px; -fx-text-fill: #d0d0d0; -fx-cursor: hand;"); }
            });
            starLabels[i] = star; starsRow.getChildren().add(star);
        }

        TextArea reviewArea = new TextArea();
        reviewArea.setPromptText("Share your experience (optional)...");
        reviewArea.setPrefRowCount(4); reviewArea.setWrapText(true);

        VBox body = new VBox(14, classTitleLbl, starsRow, new Label("Your review:"), reviewArea);
        body.setPadding(new Insets(20, 24, 16, 24)); body.setAlignment(Pos.CENTER_LEFT);

        Button submitBtn = new Button("Submit Rating");
        submitBtn.setStyle("-fx-background-color: #667eea; -fx-text-fill: white; -fx-padding: 10 28; -fx-background-radius: 20; -fx-cursor: hand; -fx-font-weight: bold;");
        Button skipBtn = new Button("Skip");
        skipBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: #65676b; -fx-padding: 10 18; -fx-border-color: #d0d0d0; -fx-border-radius: 20; -fx-cursor: hand;");
        skipBtn.setOnAction(e -> modal.close());
        submitBtn.setOnAction(e -> {
            if (selectedRating[0] == 0) { showAlert("No Rating", "Please select at least 1 star."); return; }
            bookingController.updateRatingAndReview(booking.getBookingId(), selectedRating[0],
                    reviewArea.getText().trim().isEmpty() ? null : reviewArea.getText().trim());
            if (booking.getProviderId() != null) providerController.recalculateAndUpdateRating(booking.getProviderId());
            modal.close();
            if (parentModal != null) { parentModal.close(); handleMyBookings(); }
            showAlert("Thank you! ⭐", "Your rating has been submitted.");
        });
        HBox btnRow = new HBox(12, skipBtn, submitBtn); btnRow.setAlignment(Pos.CENTER_RIGHT);
        btnRow.setPadding(new Insets(0, 24, 20, 24));

        VBox root = new VBox(0, headerBar, body, btnRow);
        modal.setScene(new Scene(root, 420, 420));
        modal.showAndWait();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  FEATURE C — CATEGORY HELPERS (thumbnail placeholder colours & emojis)
    // ─────────────────────────────────────────────────────────────────────────
    private String categoryColor(String category) {
        if (category == null) return "linear-gradient(135deg, #667eea, #764ba2)";
        return switch (category.toLowerCase()) {
            case "fitness", "sport", "yoga"            -> "linear-gradient(135deg, #f093fb, #f5576c)";
            case "tech", "programming", "coding", "it" -> "linear-gradient(135deg, #4facfe, #00f2fe)";
            case "art", "design", "music"              -> "linear-gradient(135deg, #43e97b, #38f9d7)";
            case "business", "finance", "marketing"    -> "linear-gradient(135deg, #fa709a, #fee140)";
            case "cooking", "food"                     -> "linear-gradient(135deg, #f6d365, #fda085)";
            case "language", "languages"               -> "linear-gradient(135deg, #a18cd1, #fbc2eb)";
            default                                    -> "linear-gradient(135deg, #667eea, #764ba2)";
        };
    }

    private String categoryEmoji(String category) {
        if (category == null) return "📚";
        return switch (category.toLowerCase()) {
            case "fitness", "sport", "yoga"            -> "🏋️";
            case "tech", "programming", "coding", "it" -> "💻";
            case "art", "design"                       -> "🎨";
            case "music"                               -> "🎵";
            case "business", "finance", "marketing"    -> "💼";
            case "cooking", "food"                     -> "🍳";
            case "language", "languages"               -> "🌍";
            default                                    -> "📚";
        };
    }

}
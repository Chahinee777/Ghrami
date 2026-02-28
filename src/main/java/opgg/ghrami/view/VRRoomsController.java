package opgg.ghrami.view;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.util.Duration;
import opgg.ghrami.util.SessionManager;

import java.awt.Desktop;
import java.net.URI;
import java.net.URL;
import java.util.*;

/**
 * VR Rooms controller — powered by FrameVR (framevr.io).
 *
 * FrameVR rooms are auto-created on first visit — no setup needed.
 * URL format: https://framevr.io/<room-name>
 *
 * JavaFX WebView cannot run WebGL, so the primary flow is:
 *   User clicks a room → hero card appears → "Rejoindre" opens Chrome/Firefox
 *   → Full WebGL VR experience in the real browser.
 */
public class VRRoomsController implements Initializable {

    // ── FXML ─────────────────────────────────────────────────────────────────
    @FXML private Button      btnBack;
    @FXML private Button      btnOpenBrowser;
    @FXML private Label       liveCountLabel;
    @FXML private TextField   urlBar;
    @FXML private TextField   roomSearchField;
    @FXML private VBox        roomListContainer;
    @FXML private TextField   newRoomNameField;

    @FXML private WebView     frameWebView;
    @FXML private VBox        idlePane;

    // Hero card
    @FXML private VBox        roomHeroCard;
    @FXML private Label       heroEmoji;
    @FXML private Label       heroRoomName;
    @FXML private Label       heroDescription;
    @FXML private Label       heroUrl;
    @FXML private Label       heroOnline;

    @FXML private Circle      statusDot;
    @FXML private Label       statusLabel;

    // ── State ─────────────────────────────────────────────────────────────────
    private WebEngine    webEngine;
    private List<VRRoom> allRooms;
    private VRRoom       activeRoom;
    private final Random random = new Random();

    private static final String FRAME_BASE = "https://framevr.io/";

    // ── Data model ────────────────────────────────────────────────────────────
    record VRRoom(String emoji, String name, String description, String frameRoomId, int basePeople) {
        String url() { return FRAME_BASE + frameRoomId; }
    }

    // ─────────────────────────────────────────────────────────────────────────
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        webEngine = frameWebView.getEngine();
        webEngine.setJavaScriptEnabled(true);
        webEngine.setUserAgent(
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            + "AppleWebKit/537.36 (KHTML, like Gecko) "
            + "Chrome/124.0.0.0 Safari/537.36"
        );

        buildRoomList();
        renderRooms(allRooms);
        startLiveCounter();
    }

    // ── Room catalog ──────────────────────────────────────────────────────────

    private void buildRoomList() {
        allRooms = List.of(
            new VRRoom("📸", "Photographie",    "Galerie & atelier photo 360°",        "ghrami-photo",    12),
            new VRRoom("🎸", "Musique",          "Scène live & jam session",            "ghrami-music",     8),
            new VRRoom("📚", "Club de Lecture",  "Bibliothèque virtuelle & discussion", "ghrami-lecture",   6),
            new VRRoom("🏃", "Sport & Fitness",  "Coaching et défi sportif en VR",      "ghrami-sport",    15),
            new VRRoom("🎮", "Gaming Lounge",    "Salon jeux vidéo & esport",           "ghrami-gaming",   20),
            new VRRoom("🎨", "Art & Dessin",     "Studio créatif collaboratif",         "ghrami-art",       5),
            new VRRoom("🍳", "Cuisine",          "Échange de recettes en immersion",    "ghrami-cuisine",   4),
            new VRRoom("💻", "Tech & Dev",       "Hackspace et conférences tech",       "ghrami-tech",     11),
            new VRRoom("✈️", "Voyage",           "Destinations du monde en VR",         "ghrami-voyage",    9),
            new VRRoom("🎭", "Théâtre & Cinéma", "Séances de cinéma & impro théâtre",   "ghrami-cinema",    7),
            new VRRoom("🌿", "Nature & Yoga",    "Espaces de méditation et relaxation", "ghrami-nature",    3),
            new VRRoom("🔭", "Science",          "Planétarium & lab virtuel",           "ghrami-science",   6)
        );
    }

    private void renderRooms(List<VRRoom> rooms) {
        roomListContainer.getChildren().clear();
        for (VRRoom room : rooms) {
            roomListContainer.getChildren().add(buildRoomCard(room));
        }
    }

    // ── Room card ─────────────────────────────────────────────────────────────

    private HBox buildRoomCard(VRRoom room) {
        boolean isActive = (room == activeRoom);

        HBox card = new HBox(10);
        card.setAlignment(Pos.CENTER_LEFT);
        card.setPadding(new Insets(10, 12, 10, 12));
        applyCardStyle(card, isActive, false);

        Label emojiLbl = new Label(room.emoji());
        emojiLbl.setStyle("-fx-font-size: 20;");

        VBox textBox = new VBox(2);
        Label nameLbl = new Label(room.name());
        nameLbl.setStyle(
            "-fx-font-weight: bold; -fx-font-size: 13;"
            + "-fx-text-fill: " + (isActive ? "#667eea" : "#1c1e21") + ";"
        );
        Label descLbl = new Label(room.description());
        descLbl.setStyle("-fx-text-fill: #65676b; -fx-font-size: 10;");
        descLbl.setWrapText(true);
        textBox.getChildren().addAll(nameLbl, descLbl);
        HBox.setHgrow(textBox, Priority.ALWAYS);

        int online = room.basePeople() + random.nextInt(5);
        Label onlineLbl = new Label("● " + online);
        onlineLbl.setStyle("-fx-text-fill: #2e7d32; -fx-font-size: 10; -fx-font-weight: bold;");

        card.getChildren().addAll(emojiLbl, textBox, onlineLbl);

        card.setOnMouseClicked(e -> selectRoom(room));
        card.setOnMouseEntered(e -> applyCardStyle(card, isActive, true));
        card.setOnMouseExited(e  -> applyCardStyle(card, isActive, false));

        return card;
    }

    private void applyCardStyle(HBox card, boolean isActive, boolean hovered) {
        card.setStyle(
            "-fx-background-color: " + (hovered ? "#f0f2f5" : isActive ? "#e7f3ff" : "white") + ";"
            + "-fx-background-radius: 10; -fx-cursor: hand;"
            + "-fx-border-color: " + (hovered || isActive ? "#667eea" : "#e4e6eb") + ";"
            + "-fx-border-radius: 10;"
            + "-fx-border-width: " + (isActive && !hovered ? "1.5" : "1") + ";"
        );
    }

    // ── Select room → show hero, open in browser ──────────────────────────────

    private void selectRoom(VRRoom room) {
        activeRoom = room;

        heroEmoji.setText(room.emoji());
        heroRoomName.setText(room.name());
        heroDescription.setText(room.description());
        heroUrl.setText(room.url());
        heroOnline.setText((room.basePeople() + random.nextInt(8)) + " en ligne");

        roomHeroCard.setVisible(true);
        roomHeroCard.setManaged(true);

        urlBar.setText(room.url());
        setStatus("Salle sélectionnée : " + room.name() + " — cliquez Rejoindre pour entrer", "#667eea");

        renderRooms(allRooms);

        // Auto-open in browser for instant VR experience
        openInBrowser(room.url());
    }

    // ── FXML handlers ─────────────────────────────────────────────────────────

    @FXML
    private void handleBack() {
        try {
            webEngine.load("about:blank");
            Stage stage = (Stage) btnBack.getScene().getWindow();
            double w = stage.getWidth();
            double h = stage.getHeight();
            boolean maximized = stage.isMaximized();

            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/opgg/ghrami/view/UserFeed.fxml")
            );
            Scene scene = new Scene(loader.load(), w, h);
            scene.getStylesheets().add(
                getClass().getResource("/css/social-style.css").toExternalForm()
            );
            stage.setScene(scene);
            stage.setTitle("Ghrami — Fil d'actualité");
            if (maximized) stage.setMaximized(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleGoToUrl() {
        String raw = urlBar.getText().trim();
        if (raw.isBlank()) return;
        if (!raw.startsWith("http://") && !raw.startsWith("https://")) {
            raw = FRAME_BASE + raw;
        }
        urlBar.setText(raw);
        activeRoom = null;
        roomHeroCard.setVisible(false);
        roomHeroCard.setManaged(false);
        setStatus("Ouverture de " + raw, "#667eea");
        openInBrowser(raw);
    }

    @FXML
    private void handleOpenInBrowser() {
        String target = (activeRoom != null) ? activeRoom.url() : urlBar.getText().trim();
        if (target.isBlank()) {
            setStatus("⚠️ Aucune salle sélectionnée", "#e53935");
            return;
        }
        openInBrowser(target);
    }

    @FXML
    private void handleCreateRoom() {
        String name = newRoomNameField.getText().trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9-]", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");

        if (name.isBlank()) {
            newRoomNameField.setStyle(
                "-fx-background-color: #fff0f0; -fx-background-radius: 8;"
                + "-fx-border-color: #e53935; -fx-border-radius: 8; -fx-padding: 8 12;"
            );
            return;
        }
        if (!name.startsWith("ghrami-")) name = "ghrami-" + name;

        VRRoom custom = new VRRoom("🌐", name, "Salle personnalisée", name, 1);
        newRoomNameField.clear();
        // Reset field style
        newRoomNameField.setStyle(
            "-fx-background-color: #f0f2f5; -fx-background-radius: 8;"
            + "-fx-border-color: transparent; -fx-padding: 8 12; -fx-font-size: 12;"
        );
        selectRoom(custom);
    }

    @FXML
    private void handleRoomSearch() {
        String query = roomSearchField.getText().toLowerCase().trim();
        if (query.isBlank()) {
            renderRooms(allRooms);
        } else {
            renderRooms(allRooms.stream()
                .filter(r -> r.name().toLowerCase().contains(query)
                          || r.description().toLowerCase().contains(query))
                .toList());
        }
    }

    // ── Live counter ──────────────────────────────────────────────────────────

    private void startLiveCounter() {
        // Show initial count immediately
        updateLiveCount();
        Timeline tl = new Timeline(new KeyFrame(Duration.seconds(5), e -> updateLiveCount()));
        tl.setCycleCount(Timeline.INDEFINITE);
        tl.play();
    }

    private void updateLiveCount() {
        int total = allRooms.stream()
            .mapToInt(r -> r.basePeople() + random.nextInt(6))
            .sum();
        liveCountLabel.setText(total + " en ligne");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void openInBrowser(String url) {
        try {
            if (Desktop.isDesktopSupported()
                    && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                Desktop.getDesktop().browse(new URI(url));
            } else {
                new ProcessBuilder("xdg-open", url).start();
            }
            setStatus("🚀 FrameVR ouvert dans votre navigateur ✅", "#2e7d32");
        } catch (Exception ex) {
            setStatus("⚠️ Impossible d'ouvrir le navigateur : " + ex.getMessage(), "#e53935");
            ex.printStackTrace();
        }
    }

    private void setStatus(String msg, String colorHex) {
        statusLabel.setText(msg);
        statusDot.setFill(Color.web(colorHex));
    }
}
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
import java.util.*;

public class BadgesViewController {

    @FXML private Label badgeCountLabel;
    @FXML private Label totalBadgesLabel;
    @FXML private Label recentBadgesLabel;
    @FXML private Label progressLabel;
    @FXML private Label earnedCountLabel;
    @FXML private Label lockedCountLabel;
    @FXML private Label rankLabel;
    @FXML private Label rankIconLabel;
    @FXML private Label rankSubLabel;
    @FXML private ProgressBar questProgressBar;
    @FXML private ComboBox<String> filterCombo;
    @FXML private ComboBox<String> categoryCombo;
    @FXML private FlowPane badgesContainer;
    @FXML private VBox emptyState;

    public static class BadgeDef {
        public final String name;
        public final String icon;
        public final String description;
        public final String category;
        public final String rarity;
        public final String unlockHint;
        BadgeDef(String name, String icon, String description, String category, String rarity, String unlockHint) {
            this.name = name; this.icon = icon; this.description = description;
            this.category = category; this.rarity = rarity; this.unlockHint = unlockHint;
        }
    }

    // Métadonnées d'enrichissement (icône, catégorie, rareté, astuce)
    // La source réelle des badges disponibles vient de la base de données.
    private static final List<BadgeDef> BADGE_META_LIST = List.of(
        new BadgeDef("Premier Ami",        "\uD83D\uDC65", "Vous avez fait votre première connexion sur Ghrami",      "Social",       "COMMUN",     "Connectez-vous avec un autre utilisateur"),
        new BadgeDef("Papillon Social",    "\uD83E\uDD8B", "Connecté avec 10 utilisateurs ou plus",                   "Social",       "RARE",       "Connectez-vous avec 10 utilisateurs"),
        new BadgeDef("Maître du Réseau",   "\uD83C\uDF10", "Connecté avec 50 utilisateurs ou plus",                   "Social",       "ÉPIQUE",     "Connectez-vous avec 50 utilisateurs"),
        new BadgeDef("Pilier de la Communauté", "\uD83C\uDFDB\uFE0F", "Une pierre angulaire de la communauté Ghrami", "Social",       "LÉGENDAIRE", "Connectez-vous avec 100+ utilisateurs"),
        new BadgeDef("Début des Loisirs",  "\uD83C\uDFA8", "Vous avez ajouté votre premier loisir",                   "Loisirs",      "COMMUN",     "Ajoutez un loisir à votre profil"),
        new BadgeDef("Explorateur de Loisirs", "\uD83E\uDDED", "Ajouté 5 loisirs différents",                         "Loisirs",      "RARE",       "Ajoutez 5 loisirs"),
        new BadgeDef("Maître des Loisirs", "\uD83C\uDFAF", "Enregistré 100 séances pour vos loisirs",                 "Loisirs",      "ÉPIQUE",     "Enregistrez 100 séances de pratique"),
        new BadgeDef("Esprit Créatif",     "\u2728",       "Enregistré 500+ heures pour tous vos loisirs",            "Loisirs",      "LÉGENDAIRE", "Enregistrez 500 heures au total"),
        new BadgeDef("Premier Cours",      "\uD83D\uDCDA", "Inscrit à votre premier cours",                           "Cours",        "COMMUN",     "Inscrivez-vous à n'importe quel cours"),
        new BadgeDef("Chercheur de Savoir","\uD83D\uDD2D", "Terminé 5 cours",                                         "Cours",        "RARE",       "Terminez 5 cours"),
        new BadgeDef("Maître des Cours",   "\uD83C\uDF93", "Terminé 20 cours",                                        "Cours",        "ÉPIQUE",     "Terminez 20 cours"),
        new BadgeDef("Meilleur Étudiant",  "\uD83C\uDFC5", "Excellence atteinte sur 50 cours",                        "Cours",        "LÉGENDAIRE", "Terminez 50 cours"),
        new BadgeDef("Premier Jalon",      "\uD83C\uDFAF", "Vous avez complété votre premier jalon",                  "Jalons",       "COMMUN",     "Complétez un jalon"),
        new BadgeDef("Chasseur d'Objectifs","\uD83D\uDE80","Complété 10 jalons",                                      "Jalons",       "RARE",       "Complétez 10 jalons"),
        new BadgeDef("Maître des Jalons",  "\u2B50",       "Complété 50 jalons",                                      "Jalons",       "ÉPIQUE",     "Complétez 50 jalons"),
        new BadgeDef("Bienvenue !",        "\uD83C\uDF1F", "Vous avez rejoint la plateforme Ghrami",                  "Activité",     "COMMUN",     "Créez votre compte"),
        new BadgeDef("Membre Actif",       "\uD83D\uDD25", "Vous avez partagé votre première publication",            "Activité",     "COMMUN",     "Publiez votre première mise à jour"),
        new BadgeDef("Conteur",            "\uD83D\uDCD6", "Partagé 20 publications ou plus",                         "Activité",     "RARE",       "Partagez 20 publications"),
        new BadgeDef("Roi du Contenu",     "\uD83D\uDC51", "Partagé 100 publications ou plus",                        "Activité",     "ÉPIQUE",     "Partagez 100 publications"),
        new BadgeDef("Légende Ghrami",     "\uD83C\uDFC6", "L'accomplissement ultime sur Ghrami",                     "Activité",     "LÉGENDAIRE", "Gagnez tous les autres badges")
    );
    private static final Map<String, BadgeDef> BADGE_META;
    static { BADGE_META = new HashMap<>(); for (BadgeDef d : BADGE_META_LIST) BADGE_META.put(d.name, d); }

    private BadgeController badgeController;
    private Long currentUserId;
    private List<Badge> allBadges;
    private List<Badge> dbCatalog = new ArrayList<>();

    @FXML
    public void initialize() {
        badgeController = new BadgeController();
        long uid = SessionManager.getInstance().getUserId();
        currentUserId = uid > 0 ? uid : null;
        setupFilterCombo();
        setupCategoryCombo();
        loadBadges();
    }

    private void setupFilterCombo() {
        filterCombo.getItems().addAll("Tout le temps", "Ce mois-ci", "Cette année", "Plus ancien");
        filterCombo.setValue("Tout le temps");
    }

    private void setupCategoryCombo() {
        categoryCombo.getItems().addAll("Toutes les catégories", "Social", "Loisirs", "Cours", "Jalons", "Activité");
        categoryCombo.setValue("Toutes les catégories");
    }

    private void loadBadges() {
        try {
            allBadges = (currentUserId != null)
                    ? badgeController.findByUserId(currentUserId)
                    : new ArrayList<>();
        } catch (Exception e) {
            allBadges = new ArrayList<>();
        }
        // Construire le catalogue réel depuis la BDD (dédupliqué par nom)
        try {
            List<Badge> allDbBadges = badgeController.findAll();
            Set<String> seen = new LinkedHashSet<>();
            dbCatalog = new ArrayList<>();
            for (Badge b : allDbBadges) {
                if (b.getName() != null && seen.add(b.getName())) {
                    dbCatalog.add(b);
                }
            }
        } catch (Exception e) {
            dbCatalog = new ArrayList<>();
        }
        updateStats(allBadges);
        displayQuestBadges(allBadges);
    }

    private void updateStats(List<Badge> badges) {
        int earned = badges.size();
        int total  = dbCatalog.isEmpty() ? BADGE_META_LIST.size() : dbCatalog.size();
        int locked = total - earned;
        double pct = total > 0 ? (double) earned / total : 0;

        LocalDateTime monthStart = LocalDateTime.now().withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
        long recent = badges.stream()
                .filter(b -> b.getEarnedDate() != null && b.getEarnedDate().isAfter(monthStart))
                .count();

        badgeCountLabel.setText(earned + " Badge" + (earned != 1 ? "s" : ""));
        if (earnedCountLabel != null) earnedCountLabel.setText(String.valueOf(earned));
        if (lockedCountLabel != null) lockedCountLabel.setText(String.valueOf(locked));
        recentBadgesLabel.setText(String.valueOf(recent));
        totalBadgesLabel.setText(earned + " / " + total);
        progressLabel.setText((int)(pct * 100) + "% Complété");
        if (questProgressBar != null) questProgressBar.setProgress(pct);
        updateRank(earned);
    }

    private void updateRank(int earned) {
        String icon, name, sub;
        if      (earned == 0)  { icon = "\uD83C\uDF31"; name = "Novice";          sub = "Commencez votre quête \u2014 gagnez votre premier badge !"; }
        else if (earned < 3)   { icon = "\uD83E\uDDD9"; name = "Apprenti";        sub = "Vous avez commencé votre voyage. Continuez !"; }
        else if (earned < 6)   { icon = "\u2694\uFE0F"; name = "Aventurier";      sub = "Un explorateur chevronné de Ghrami !"; }
        else if (earned < 10)  { icon = "\uD83D\uDEE1\uFE0F"; name = "Guerrier";  sub = "Vous prouvez votre valeur sur Ghrami !"; }
        else if (earned < 15)  { icon = "\uD83E\uDD85"; name = "Champion";        sub = "Un vrai champion de la plateforme !"; }
        else if (earned < 20)  { icon = "\uD83D\uDC51"; name = "Légende";         sub = "Votre héritage sur Ghrami est indéniable !"; }
        else                   { icon = "\uD83C\uDFC6"; name = "Grand Maître";    sub = "Le sommet \u2014 vous avez conquis toutes les quêtes !"; }

        if (rankIconLabel != null) rankIconLabel.setText(icon);
        if (rankLabel     != null) rankLabel.setText(name);
        if (rankSubLabel  != null) rankSubLabel.setText(sub);
    }

    private void displayQuestBadges(List<Badge> earned) {
        badgesContainer.getChildren().clear();
        String timeFilter = filterCombo.getValue();
        String catFilter  = (categoryCombo != null) ? categoryCombo.getValue() : "Toutes les catégories";

        Set<String>        earnedNames = new HashSet<>();
        Map<String, Badge> earnedMap   = new HashMap<>();
        for (Badge b : earned) { earnedNames.add(b.getName()); earnedMap.put(b.getName(), b); }

        List<Badge> timeFiltered      = applyTimeFilter(earned, timeFilter);
        Set<String> timeFilteredNames = new HashSet<>();
        for (Badge b : timeFiltered) timeFilteredNames.add(b.getName());

        // Utiliser le catalogue de la BDD ; repli sur les métadonnées locales si vide
        List<Badge> catalog = dbCatalog.isEmpty() ? new ArrayList<>() : dbCatalog;
        // Si la BDD ne contient aucun badge du tout, afficher quand même les badges
        // gagnés par l'utilisateur enrichis des métadonnées locales
        if (catalog.isEmpty()) {
            for (BadgeDef def : BADGE_META_LIST) {
                if (!"Toutes les catégories".equals(catFilter) && !def.category.equals(catFilter)) continue;
                boolean isEarned = earnedNames.contains(def.name);
                if (!"Tout le temps".equals(timeFilter) && isEarned && !timeFilteredNames.contains(def.name)) continue;
                badgesContainer.getChildren().add(buildQuestCard(def, isEarned ? earnedMap.get(def.name) : null));
            }
        } else {
            for (Badge dbBadge : catalog) {
                BadgeDef def = getMetadata(dbBadge);
                if (!"Toutes les catégories".equals(catFilter) && !def.category.equals(catFilter)) continue;
                boolean isEarned = earnedNames.contains(dbBadge.getName());
                if (!"Tout le temps".equals(timeFilter) && isEarned && !timeFilteredNames.contains(dbBadge.getName())) continue;
                badgesContainer.getChildren().add(buildQuestCard(def, isEarned ? earnedMap.get(dbBadge.getName()) : null));
            }
        }

        int cardCount = badgesContainer.getChildren().size();
        boolean isEmpty = cardCount == 0 && earned.isEmpty();
        if (emptyState != null) { emptyState.setVisible(isEmpty); emptyState.setManaged(isEmpty); }
    }

    private List<Badge> applyTimeFilter(List<Badge> badges, String filter) {
        if (filter == null || "Tout le temps".equals(filter)) return badges;
        LocalDateTime now = LocalDateTime.now();
        return switch (filter) {
            case "Ce mois-ci" -> {
                LocalDateTime s = now.withDayOfMonth(1).truncatedTo(ChronoUnit.DAYS);
                yield badges.stream().filter(b -> b.getEarnedDate() != null && b.getEarnedDate().isAfter(s)).toList();
            }
            case "Cette année" -> {
                LocalDateTime s = now.withDayOfYear(1).truncatedTo(ChronoUnit.DAYS);
                yield badges.stream().filter(b -> b.getEarnedDate() != null && b.getEarnedDate().isAfter(s)).toList();
            }
            case "Plus ancien" -> {
                LocalDateTime s = now.minusYears(1);
                yield badges.stream().filter(b -> b.getEarnedDate() != null && b.getEarnedDate().isBefore(s)).toList();
            }
            default -> badges;
        };
    }

    private VBox buildQuestCard(BadgeDef def, Badge earned) {
        boolean isEarned = earned != null;

        String cardBg, borderColor, glowColor, rarityPillBg, rarityPillText;
        if (!isEarned) {
            cardBg        = "#f0f2f5";
            borderColor   = "#dadde1";
            glowColor     = "rgba(0,0,0,0.04)";
            rarityPillBg  = "#e4e6ea";
            rarityPillText= "#8d949e";
        } else {
            switch (def.rarity) {
                case "LÉGENDAIRE" -> {
                    cardBg        = "linear-gradient(135deg, #fff8e1, #fff3cd)";
                    borderColor   = "#ffc107";
                    glowColor     = "rgba(255,193,7,0.28)";
                    rarityPillBg  = "#fff3cd";
                    rarityPillText= "#b45309";
                }
                case "ÉPIQUE" -> {
                    cardBg        = "linear-gradient(135deg, #f3e8ff, #fce7f3)";
                    borderColor   = "#c084fc";
                    glowColor     = "rgba(167,139,250,0.28)";
                    rarityPillBg  = "#ede9fe";
                    rarityPillText= "#7c3aed";
                }
                case "RARE" -> {
                    cardBg        = "linear-gradient(135deg, #eff6ff, #dbeafe)";
                    borderColor   = "#93c5fd";
                    glowColor     = "rgba(59,130,246,0.22)";
                    rarityPillBg  = "#dbeafe";
                    rarityPillText= "#1d4ed8";
                }
                default -> { // COMMUN
                    cardBg        = "white";
                    borderColor   = "#c3e6cb";
                    glowColor     = "rgba(76,175,80,0.15)";
                    rarityPillBg  = "#d1fae5";
                    rarityPillText= "#065f46";
                }
            }
        }
        String textColor    = isEarned ? "#1c1e21" : "#8d949e";
        String subTextColor = isEarned ? "#65676b" : "#b0b3b8";

        VBox card = new VBox(10);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPrefWidth(210); card.setMaxWidth(210); card.setMinHeight(265);
        String baseStyle  = buildCardStyle(cardBg, borderColor, glowColor, "1.5", false);
        String hoverStyle = buildCardStyle(cardBg, borderColor, glowColor, "2",   true);
        card.setStyle(baseStyle);

        Label rarityPill = new Label(def.rarity);
        rarityPill.setStyle("-fx-font-size: 10; -fx-font-weight: bold; -fx-text-fill: " + rarityPillText +
            "; -fx-background-color: " + rarityPillBg + "; -fx-background-radius: 10; -fx-padding: 2 10;");

        StackPane iconPane = new StackPane();
        Label iconLabel = new Label(def.icon);
        iconLabel.setStyle("-fx-font-size: 52;" + (isEarned ? "" : " -fx-opacity: 0.35;"));
        iconPane.getChildren().add(iconLabel);
        if (!isEarned) {
            Label lock = new Label("\uD83D\uDD12");
            lock.setStyle("-fx-font-size: 22; -fx-translate-x: 22; -fx-translate-y: 22; -fx-opacity: 0.55;");
            iconPane.getChildren().add(lock);
        }

        Label statusChip = new Label(isEarned ? "\u2705 Obtenu" : "\uD83D\uDD0F Pas encore obtenu");
        statusChip.setStyle("-fx-font-size: 10; -fx-font-weight: bold; -fx-text-fill: " +
            (isEarned ? "#15803d" : "#9ca3af") + "; -fx-background-color: " +
            (isEarned ? "#dcfce7" : "#f3f4f6") +
            "; -fx-background-radius: 10; -fx-padding: 3 10;");

        Label nameLabel = new Label(def.name);
        nameLabel.setStyle("-fx-font-size: 14; -fx-font-weight: bold; -fx-text-fill: " + textColor +
            "; -fx-wrap-text: true; -fx-text-alignment: center;");
        nameLabel.setWrapText(true); nameLabel.setMaxWidth(182); nameLabel.setAlignment(Pos.CENTER);

        String bodyText = isEarned ? def.description : "\uD83D\uDD13 " + def.unlockHint;
        Label bodyLabel = new Label(bodyText);
        bodyLabel.setStyle("-fx-font-size: 11; -fx-text-fill: " + subTextColor +
            "; -fx-wrap-text: true; -fx-text-alignment: center;" + (isEarned ? "" : " -fx-font-style: italic;"));
        bodyLabel.setWrapText(true); bodyLabel.setMaxWidth(182); bodyLabel.setAlignment(Pos.CENTER);

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        String footerText, footerStyle;
        if (isEarned && earned.getEarnedDate() != null) {
            footerText  = "\uD83D\uDCC5 " + earned.getEarnedDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
            footerStyle = "-fx-font-size: 10; -fx-text-fill: " + subTextColor + "; -fx-font-style: italic;";
        } else {
            footerText  = "\uD83D\uDCC2 " + def.category;
            footerStyle = "-fx-font-size: 10; -fx-text-fill: " + subTextColor + ";";
        }
        Label footerLabel = new Label(footerText);
        footerLabel.setStyle(footerStyle);

        card.getChildren().addAll(rarityPill, iconPane, statusChip, nameLabel, bodyLabel, spacer, footerLabel);

        card.setOnMouseEntered(e -> card.setStyle(hoverStyle));
        card.setOnMouseExited(e  -> card.setStyle(baseStyle));
        final BadgeDef defF = def;
        final Badge earF = earned;
        card.setOnMouseClicked(e -> showQuestBadgeDetails(defF, earF));
        return card;
    }

    private String buildCardStyle(String bg, String border, String glow, String borderWidth, boolean scaled) {
        return "-fx-background-color: " + bg + "; -fx-background-radius: 20; -fx-padding: 18 14; " +
               "-fx-effect: dropshadow(gaussian, " + glow + ", " + (scaled ? "28" : "18") +
               ", 0, 0, " + (scaled ? "10" : "6") + "); " +
               "-fx-border-color: " + border + "; -fx-border-width: " + borderWidth +
               "; -fx-border-radius: 20; -fx-cursor: hand;" +
               (scaled ? " -fx-scale-x: 1.04; -fx-scale-y: 1.04;" : "");
    }

    private void showQuestBadgeDetails(BadgeDef def, Badge earned) {
        boolean isEarned = earned != null;
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(isEarned ? "Badge Débloqué !" : "Badge Verrouillé");
        alert.setHeaderText(def.icon + "  " + def.name + "  [" + def.rarity + "]");

        VBox content = new VBox(12);
        content.setPadding(new Insets(15));
        content.setMaxWidth(420);

        Label descLabel = new Label(def.description);
        descLabel.setWrapText(true); descLabel.setStyle("-fx-font-size: 13;");
        Label catLabel = new Label("Catégorie : " + def.category + "   |   Rareté : " + def.rarity);
        catLabel.setStyle("-fx-font-size: 12; -fx-text-fill: #65676b;");
        content.getChildren().addAll(descLabel, catLabel);

        if (isEarned) {
            Label el = new Label("\u2705 Obtenu le :"); el.setStyle("-fx-font-weight: bold; -fx-font-size: 13;");
            String date = earned.getEarnedDate() != null
                    ? earned.getEarnedDate().format(DateTimeFormatter.ofPattern("dd MMMM yyyy 'à' HH:mm"))
                    : "Inconnu";
            content.getChildren().addAll(el, new Label(date));
        } else {
            Label ll = new Label("\uD83D\uDD12 Pas encore obtenu");
            ll.setStyle("-fx-font-weight: bold; -fx-text-fill: #e74c3c; -fx-font-size: 13;");
            Label hl = new Label("Comment débloquer :"); hl.setStyle("-fx-font-weight: bold; -fx-font-size: 13;");
            Label hint = new Label(def.unlockHint); hint.setStyle("-fx-font-style: italic; -fx-text-fill: #65676b;");
            content.getChildren().addAll(ll, hl, hint);
        }
        alert.getDialogPane().setContent(content);
        alert.showAndWait();
    }

    /** Retourne les m\u00e9tadonn\u00e9es d'un badge DB, ou g\u00e9n\u00e8re des valeurs par d\u00e9faut si inconnu. */
    private BadgeDef getMetadata(Badge b) {
        BadgeDef meta = BADGE_META.get(b.getName());
        if (meta != null) return meta;
        return new BadgeDef(
            b.getName(),
            getBadgeIcon(b.getName()),
            b.getDescription() != null ? b.getDescription() : "Badge Ghrami",
            "Sp\u00e9cial", "COMMUN", "Continuez \u00e0 vous engager !"
        );
    }

    private String getBadgeIcon(String badgeName) {
        if (badgeName == null) return "\uD83C\uDFC6";
        String n = badgeName.toLowerCase();
        if (n.contains("friend") || n.contains("ami"))       return "\uD83D\uDC65";
        if (n.contains("first") || n.contains("premier"))    return "\uD83C\uDF1F";
        if (n.contains("class") || n.contains("cours"))      return "\uD83D\uDCDA";
        if (n.contains("milestone") || n.contains("jalon"))  return "\uD83C\uDFAF";
        if (n.contains("hobby") || n.contains("loisir"))     return "\uD83C\uDFA8";
        if (n.contains("social") || n.contains("partage"))   return "\uD83D\uDCAC";
        if (n.contains("expert"))                             return "\u2B50";
        if (n.contains("champion"))                           return "\uD83E\uDD47";
        if (n.contains("master") || n.contains("maitre"))    return "\uD83D\uDC51";
        if (n.contains("creator") || n.contains("createur")) return "\u2728";
        if (n.contains("explorer"))                           return "\uD83E\uDDED";
        return "\uD83C\uDFC6";
    }

    @FXML
    private void filterBadges() {
        if (allBadges != null) displayQuestBadges(allBadges);
    }

    @FXML
    private void handleBack() {
        try {
            Stage stage = (Stage) badgesContainer.getScene().getWindow();
            double w = stage.getWidth(); double h = stage.getHeight(); boolean max = stage.isMaximized();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/opgg/ghrami/view/UserFeed.fxml"));
            Scene scene = new Scene(loader.load(), w, h);
            scene.getStylesheets().add(getClass().getResource("/css/social-style.css").toExternalForm());
            stage.setScene(scene); stage.setTitle("Ghrami - Fil d'Actualité");
            if (max) stage.setMaximized(true);
        } catch (Exception e) {
            e.printStackTrace(); showAlert("Erreur", "Impossible de revenir au fil : " + e.getMessage());
        }
    }

    @FXML
    private void showBadgeInfo() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Guide de la Quête des Badges");
        alert.setHeaderText("\uD83D\uDCA1  Le Système de Quête des Badges");
        VBox c = new VBox(14);
        c.setPadding(new Insets(20)); c.setStyle("-fx-font-size: 13;");
        String[] tips = {
            "\uD83D\uDC65 Connectez-vous avec d'autres \u2014 développez votre réseau",
            "\uD83C\uDFAF Poursuivez des loisirs \u2014 enregistrez des séances et des heures",
            "\uD83D\uDCDA Terminez des cours \u2014 inscrivez-vous et finissez les formations",
            "\uD83C\uDF1F Partagez et aidez \u2014 publiez des mises à jour et assistez la communauté",
            "\uD83D\uDCAC Restez actif \u2014 l'engagement régulier rapporte des badges",
            "\uD83C\uDFC6 Atteignez des jalons \u2014 accomplissez des objectifs et des défis",
            "\uD83D\uDC51 Badges Rares / Épiques \u2014 nécessitent un effort soutenu",
            "\uD83D\uDD2E Badges Légendaires \u2014 le niveau le plus rare et le plus prestigieux"
        };
        for (String t : tips) { Label l = new Label(t); l.setWrapText(true); l.setMaxWidth(420); c.getChildren().add(l); }
        alert.getDialogPane().setContent(c);
        alert.showAndWait();
    }

    private void showAlert(String title, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title); a.setHeaderText(null); a.setContentText(content); a.showAndWait();
    }
}
package com.example.project7.controller.edition;

import com.example.project7.FxmlLoader;
import com.example.project7.model.Projet;
import com.example.project7.model.Theme;
import com.jfoenix.controls.JFXButton;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import sql_connection.SqlConnection;

import java.net.URL;
import java.sql.*;
import java.util.*;

public class ThemeView implements Initializable {

    @FXML private Label lblNbExercices;
    @FXML private Label lblNbThemes;
    @FXML private Label lblNbQuestions;
    @FXML private JFXButton btnGererThemes;
    @FXML private TextField txtRecherche;
    @FXML private FlowPane flowPaneFiltres;
    @FXML private FlowPane flowPaneExercices;
    @FXML private ScrollPane scrollPaneExercices;

    private List<Theme> allThemes = new ArrayList<>();
    private AnchorPane parentPane;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("✅ ThemeView initialisé");

        configurerFlowPane();
        chargerThemes();
        chargerStatistiques();
        afficherExercices(null);

        // Ajuster après le chargement
        Platform.runLater(this::ajusterLargeurCartes);
    }
    private void ajusterHauteurFlowPane() {
        Platform.runLater(() -> {
            flowPaneExercices.layout(); // Forcer le layout d'abord

            int nbCartes = flowPaneExercices.getChildren().size();

            if (nbCartes == 0) {
                flowPaneExercices.setPrefHeight(200);
                return;
            }

            double viewportWidth = scrollPaneExercices.getViewportBounds().getWidth();
            int cartesParLigne = 3;

            // Hauteur réelle d'une carte (mesurée)
            double hauteurCarte = 150;

            // Si on a des cartes, on peut mesurer la hauteur réelle
            if (!flowPaneExercices.getChildren().isEmpty()) {
                javafx.scene.Node premiereCarte = flowPaneExercices.getChildren().get(0);
                if (premiereCarte.getBoundsInParent().getHeight() > 0) {
                    hauteurCarte = premiereCarte.getBoundsInParent().getHeight();
                }
            }

            double vgap = flowPaneExercices.getVgap();
            double padding = flowPaneExercices.getPadding().getTop() + flowPaneExercices.getPadding().getBottom();

            // Calculer le nombre de lignes nécessaires
            int nbLignes = (int) Math.ceil((double) nbCartes / cartesParLigne);

            // Calculer la hauteur totale avec une marge de sécurité
            double hauteurTotale = (nbLignes * hauteurCarte) + ((nbLignes - 1) * vgap) + padding + 40;

            flowPaneExercices.setPrefHeight(hauteurTotale);
            flowPaneExercices.setMinHeight(hauteurTotale);

            System.out.println("✅ Hauteur ajustée: " + hauteurTotale + "px | " + nbCartes + " cartes | " + nbLignes + " lignes | hauteur carte: " + hauteurCarte + "px");
        });
    }
    private void configurerFlowPane() {
        flowPaneExercices.setPadding(new Insets(10));
        flowPaneExercices.setHgap(16);
        flowPaneExercices.setVgap(16);

        // ⭐ IMPORTANT : Forcer le recalcul de la hauteur quand les enfants changent
        flowPaneExercices.getChildren().addListener((javafx.collections.ListChangeListener<? super javafx.scene.Node>) change -> {
            Platform.runLater(() -> {
                ajusterHauteurFlowPane();
            });
        });

        // Recalculer largeur cartes si taille viewport change
        scrollPaneExercices.viewportBoundsProperty().addListener((obs, oldVal, newVal) -> {
            ajusterLargeurCartes();
            Platform.runLater(this::ajusterHauteurFlowPane);
        });
    }
    private void ajusterLargeurCartes() {
        double viewportWidth = scrollPaneExercices.getViewportBounds().getWidth();

        if (viewportWidth <= 0) return;

        int cardsPerRow = 3;
        double hgap = flowPaneExercices.getHgap();
        double padding = flowPaneExercices.getPadding().getLeft() + flowPaneExercices.getPadding().getRight();

        // largeur disponible pour toutes les cartes
        double totalGapWidth = hgap * (cardsPerRow - 1);
        double availableWidth = viewportWidth - padding - totalGapWidth;
        double cardWidth = availableWidth / cardsPerRow;

        flowPaneExercices.getChildren().forEach(child -> {
            if (child instanceof VBox) {
                VBox card = (VBox) child;
                card.setPrefWidth(cardWidth);
                card.setMaxWidth(cardWidth);
                card.setMinWidth(cardWidth);
            }
        });

        flowPaneExercices.setPrefWrapLength(viewportWidth);

        System.out.println("✅ 3 CARTES PAR LIGNE | Viewport: " + viewportWidth + "px | Card: " + cardWidth + "px");
    }

    public void setParentPane(AnchorPane pane) {
        this.parentPane = pane;
    }

    private void chargerStatistiques() {
        try (Connection conn = SqlConnection.getConnection()) {
            PreparedStatement stmtEx = conn.prepareStatement("SELECT COUNT(*) FROM Exercice");
            ResultSet rsEx = stmtEx.executeQuery();
            if (rsEx.next()) lblNbExercices.setText(String.valueOf(rsEx.getInt(1)));

            PreparedStatement stmtTh = conn.prepareStatement("SELECT COUNT(*) FROM Theme");
            ResultSet rsTh = stmtTh.executeQuery();
            if (rsTh.next()) lblNbThemes.setText(String.valueOf(rsTh.getInt(1)));

            try {
                PreparedStatement stmtQ = conn.prepareStatement("SELECT COUNT(*) FROM QCM");
                ResultSet rsQ = stmtQ.executeQuery();
                if (rsQ.next()) lblNbQuestions.setText(String.valueOf(rsQ.getInt(1)));
            } catch (SQLException e) {
                lblNbQuestions.setText("0");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur de chargement", "Impossible de charger les statistiques");
        }
    }

    @FXML
    private void handleGererThemes(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Gérer les Thèmes");
        alert.setHeaderText("Fonctionnalité en cours de développement");
        alert.setContentText("Vous pourrez bientôt créer, modifier et supprimer des thèmes ici.");
        alert.showAndWait();
    }

    @FXML
    private void handleRechercher(ActionEvent event) {
        String recherche = txtRecherche.getText().trim().toLowerCase();
        if (recherche.isEmpty()) afficherExercices(null);
        else rechercherExercices(recherche);
    }

    private void rechercherExercices(String motCle) {
        flowPaneExercices.getChildren().clear();

        String query = """
            SELECT DISTINCT e.idExercice, e.titre
            FROM Exercice e
            LEFT JOIN Exercice_Theme et ON e.idExercice = et.exerciceID
            LEFT JOIN Theme t ON et.themeID = t.idTheme
            WHERE LOWER(e.titre) LIKE ? OR LOWER(t.nomTheme) LIKE ?
            """;

        try (Connection conn = SqlConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            String pattern = "%" + motCle + "%";
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);

            ResultSet rs = stmt.executeQuery();
            int count = 0;

            while (rs.next()) {
                int id = rs.getInt("idExercice");
                String titre = rs.getString("titre");
                List<Theme> themes = getThemesForExercice(id);
                flowPaneExercices.getChildren().add(creerCarteExercice(id, titre, themes));
                count++;
            }

            if (count == 0) afficherMessageVide("Aucun exercice trouvé pour : \"" + motCle + "\"");

            Platform.runLater(this::ajusterLargeurCartes);

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur de recherche", "Impossible de rechercher les exercices");
        }
    }

    private void chargerThemes() {
        String query = "SELECT idTheme, nomTheme, couleur FROM Theme";

        try (Connection conn = SqlConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            allThemes.clear();
            while (rs.next()) {
                allThemes.add(new Theme(
                        rs.getInt("idTheme"),
                        rs.getString("nomTheme"),
                        rs.getString("couleur")
                ));
            }

            afficherBoutonsThemes();

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur de chargement", "Impossible de charger les thèmes");
        }
    }

    private void afficherBoutonsThemes() {
        flowPaneFiltres.getChildren().clear();

        // Bouton "Tous"
        JFXButton btnTous = new JFXButton("📋 Tous");
        btnTous.setStyle("-fx-background-color: #5E35B1; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 20; -fx-padding: 8 15; -fx-cursor: hand;");
        btnTous.setOnAction(e -> afficherExercices(null));
        flowPaneFiltres.getChildren().add(btnTous);

        for (Theme t : allThemes) {
            JFXButton btn = new JFXButton(t.getNomTheme());
            btn.setStyle(
                    "-fx-background-color: " + t.getCouleur() + "; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-weight: bold; " +
                            "-fx-background-radius: 20; " +
                            "-fx-padding: 8 15; -fx-cursor: hand;"
            );
            btn.setOnAction(e -> afficherExercices(t));
            flowPaneFiltres.getChildren().add(btn);
        }
    }

    private void afficherExercices(Theme themeFiltre) {
        flowPaneExercices.getChildren().clear();

        String query = themeFiltre == null ?
                "SELECT idExercice, titre FROM Exercice" :
                "SELECT DISTINCT e.idExercice, e.titre FROM Exercice e JOIN Exercice_Theme et ON e.idExercice = et.exerciceID WHERE et.themeID = ?";

        try (Connection conn = SqlConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            if (themeFiltre != null) stmt.setInt(1, themeFiltre.getIdTheme());

            ResultSet rs = stmt.executeQuery();
            int count = 0;

            while (rs.next()) {
                int id = rs.getInt("idExercice");
                String titre = rs.getString("titre");
                List<Theme> themes = getThemesForExercice(id);
                flowPaneExercices.getChildren().add(creerCarteExercice(id, titre, themes));
                count++;
            }

            if (count == 0) {
                String message = themeFiltre == null ?
                        "Aucun exercice disponible" :
                        "Aucun exercice pour le thème : " + themeFiltre.getNomTheme();
                afficherMessageVide(message);
            }

            Platform.runLater(() -> {
                ajusterLargeurCartes();
                // Double appel avec délai pour garantir le bon calcul
                Platform.runLater(() -> {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {}
                    ajusterHauteurFlowPane();
                });
            });

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur d'affichage", "Impossible d'afficher les exercices");
        }
    }

    private List<Theme> getThemesForExercice(int exerciceID) {
        List<Theme> themes = new ArrayList<>();
        String query = """
            SELECT t.idTheme, t.nomTheme, t.couleur
            FROM Exercice_Theme et
            JOIN Theme t ON et.themeID = t.idTheme
            WHERE et.exerciceID = ?
            """;

        try (Connection conn = SqlConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, exerciceID);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                themes.add(new Theme(
                        rs.getInt("idTheme"),
                        rs.getString("nomTheme"),
                        rs.getString("couleur")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return themes;
    }
    private VBox creerCarteExercice(int id, String titre, List<Theme> themes) {
        VBox card = new VBox(6);
        card.setPadding(new Insets(10));
        card.setPrefWidth(100);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMinWidth(100);

        // ⭐ HAUTEUR FIXE pour faciliter le calcul
        card.setMinHeight(140);
        card.setPrefHeight(140);
        card.setMaxHeight(140);

        card.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-radius: 10; " +
                        "-fx-background-radius: 10; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8, 0, 0, 2);"
        );

        Label lblTitre = new Label(titre != null ? titre : "Sans titre");
        lblTitre.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #5E35B1;");
        lblTitre.setWrapText(true);
        lblTitre.setMaxWidth(Double.MAX_VALUE);
        lblTitre.setMinHeight(25);
        lblTitre.setMaxHeight(40);

        Label lblType = new Label("QCM");
        lblType.setStyle("-fx-font-size: 9px; -fx-text-fill: #999; -fx-font-style: italic;");

        FlowPane paneThemes = new FlowPane(3,3);
        paneThemes.setMaxWidth(Double.MAX_VALUE);
        paneThemes.setPrefHeight(30);
        paneThemes.setMaxHeight(30);

        if (themes.isEmpty()) {
            Label lblAucun = new Label("Aucun thème");
            lblAucun.setStyle("-fx-background-color: #CCCCCC; -fx-text-fill: white; -fx-padding: 2 6; -fx-background-radius: 8; -fx-font-size: 9px;");
            paneThemes.getChildren().add(lblAucun);
        } else {
            int maxTags = Math.min(themes.size(), 3);
            for (int i=0;i<maxTags;i++){
                Theme t = themes.get(i);
                Label tag = new Label(t.getNomTheme());
                tag.setStyle("-fx-background-color: " + t.getCouleur() + "; -fx-text-fill: white; -fx-padding: 2 6; -fx-background-radius: 8; -fx-font-size: 9px;");
                paneThemes.getChildren().add(tag);
            }
            if (themes.size() > 3){
                Label plusTag = new Label("+" + (themes.size()-3));
                plusTag.setStyle("-fx-background-color: #999; -fx-text-fill: white; -fx-padding: 2 6; -fx-background-radius: 8; -fx-font-size: 9px;");
                paneThemes.getChildren().add(plusTag);
            }
        }

        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        JFXButton btnEditer = creerBoutonEditerCompact(id);
        btnEditer.setMinHeight(25);
        btnEditer.setPrefHeight(25);

        HBox btnContainer = new HBox(btnEditer);
        btnContainer.setAlignment(Pos.CENTER_RIGHT);
        btnContainer.setPadding(new Insets(3,0,0,0));

        card.getChildren().addAll(lblTitre, lblType, paneThemes, spacer, btnContainer);

        return card;
    }

    private JFXButton creerBoutonEditerCompact(int exerciceID) {
        JFXButton btn = new JFXButton("✏️ Éditer");
        String styleBase = "-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold; -fx-background-radius: 15; -fx-padding: 6 14; -fx-cursor: hand;";
        String styleHover = styleBase.replace("#2196F3","#1976D2");
        btn.setStyle(styleBase);
        btn.setOnAction(e -> editerExercice(exerciceID));
        btn.setOnMouseEntered(e -> btn.setStyle(styleHover));
        btn.setOnMouseExited(e -> btn.setStyle(styleBase));
        return btn;
    }

    private void afficherMessageVide(String message) {
        VBox empty = new VBox(15);
        empty.setAlignment(Pos.CENTER);
        empty.setPadding(new Insets(40));
        empty.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5,0,0,2);");

        Label icon = new Label("📭");
        icon.setStyle("-fx-font-size: 48px;");

        Label msg = new Label(message);
        msg.setStyle("-fx-font-size: 14px; -fx-text-fill: #999; -fx-wrap-text: true;");

        empty.getChildren().addAll(icon,msg);
        flowPaneExercices.getChildren().add(empty);
    }

    private void editerExercice(int exerciceID){
        try {
            int controleID = getControleIDFromExercice(exerciceID);
            if (controleID == -1){ showError("Erreur","Contrôle introuvable."); return; }

            int projetID = getProjetIDFromControle(controleID);
            if (projetID == -1){ showError("Erreur","Projet introuvable."); return; }

            Projet projet = loadProjet(projetID);
            if (projet==null){ showError("Erreur","Impossible de charger le projet."); return; }

            FxmlLoader loader = new FxmlLoader();
            Parent view = loader.getPane("editer_quiz/_2_EditerProjet");

            EditerProjet controller = (EditerProjet) loader.getController();
            if(controller!=null){
                controller.setParentPane(parentPane);
                controller.setProjet(projet);
            }

            parentPane.getChildren().setAll(view);

        } catch (Exception e){
            e.printStackTrace();
            showError("Erreur","Impossible d'ouvrir l'éditeur : "+e.getMessage());
        }
    }

    private int getControleIDFromExercice(int exerciceID){
        try(Connection conn = SqlConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement("SELECT controleID FROM Exercice WHERE idExercice=?")){
            stmt.setInt(1,exerciceID);
            ResultSet rs = stmt.executeQuery();
            return rs.next()?rs.getInt("controleID"):-1;
        } catch (SQLException e){ e.printStackTrace(); return -1; }
    }

    private int getProjetIDFromControle(int controleID){
        try(Connection conn = SqlConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement("SELECT projetID FROM Controle WHERE idControle=?")){
            stmt.setInt(1,controleID);
            ResultSet rs = stmt.executeQuery();
            return rs.next()?rs.getInt("projetID"):-1;
        } catch (SQLException e){ e.printStackTrace(); return -1; }
    }

    private Projet loadProjet(int projetID){
        try(Connection conn = SqlConnection.getConnection();
            PreparedStatement stmt = conn.prepareStatement("SELECT * FROM Projet WHERE idProjet=?")){
            stmt.setInt(1,projetID);
            ResultSet rs = stmt.executeQuery();
            if(rs.next()){
                return new Projet(projetID, rs.getString("nomProjet"), rs.getString("localisationProjet"), rs.getString("typeProjet"), rs.getDate("creationDate"));
            }
        } catch (SQLException e){ e.printStackTrace(); }
        return null;
    }

    private void showError(String title,String message){
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}
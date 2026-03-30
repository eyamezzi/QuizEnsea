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
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
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

        Platform.runLater(this::ajusterLargeurCartes);
    }

    private void ajusterHauteurFlowPane() {
        Platform.runLater(() -> {
            flowPaneExercices.layout();

            int nbCartes = flowPaneExercices.getChildren().size();

            if (nbCartes == 0) {
                flowPaneExercices.setPrefHeight(200);
                return;
            }

            int cartesParLigne = 3;
            double hauteurCarte = 150;

            if (!flowPaneExercices.getChildren().isEmpty()) {
                javafx.scene.Node premiereCarte = flowPaneExercices.getChildren().get(0);
                if (premiereCarte.getBoundsInParent().getHeight() > 0) {
                    hauteurCarte = premiereCarte.getBoundsInParent().getHeight();
                }
            }

            double vgap = flowPaneExercices.getVgap();
            double padding = flowPaneExercices.getPadding().getTop() + flowPaneExercices.getPadding().getBottom();
            int nbLignes = (int) Math.ceil((double) nbCartes / cartesParLigne);
            double hauteurTotale = (nbLignes * hauteurCarte) + ((nbLignes - 1) * vgap) + padding + 40;

            flowPaneExercices.setPrefHeight(hauteurTotale);
            flowPaneExercices.setMinHeight(hauteurTotale);

            System.out.println("✅ Hauteur ajustée: " + hauteurTotale + "px | " + nbCartes + " cartes");
        });
    }

    private void configurerFlowPane() {
        flowPaneExercices.setPadding(new Insets(10));
        flowPaneExercices.setHgap(16);
        flowPaneExercices.setVgap(16);

        flowPaneExercices.getChildren().addListener((javafx.collections.ListChangeListener<? super javafx.scene.Node>) change -> {
            Platform.runLater(this::ajusterHauteurFlowPane);
        });

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
        System.out.println("✅ Cartes ajustées | Largeur: " + cardWidth + "px");
    }

    public void setParentPane(AnchorPane pane) {
        this.parentPane = pane;
    }

    private void chargerStatistiques() {
        try (Connection conn = SqlConnection.getConnection()) {

            // Compter les exercices
            PreparedStatement stmtEx = conn.prepareStatement("SELECT COUNT(*) FROM Exercice");
            ResultSet rsEx = stmtEx.executeQuery();
            if (rsEx.next()) {
                int nbEx = rsEx.getInt(1);
                lblNbExercices.setText(String.valueOf(nbEx));
                System.out.println("✅ " + nbEx + " exercice(s) dans la base");
            }

            // Compter les thèmes
            PreparedStatement stmtTh = conn.prepareStatement("SELECT COUNT(*) FROM Theme");
            ResultSet rsTh = stmtTh.executeQuery();
            if (rsTh.next()) {
                int nbTh = rsTh.getInt(1);
                lblNbThemes.setText(String.valueOf(nbTh));
                System.out.println("✅ " + nbTh + " thème(s) dans la base");
            }

            // Compter TOUTES les questions
            int totalQuestions = 0;

            try (PreparedStatement stmtQCM = conn.prepareStatement("SELECT COUNT(*) FROM QCM")) {
                ResultSet rsQCM = stmtQCM.executeQuery();
                if (rsQCM.next()) totalQuestions += rsQCM.getInt(1);
            } catch (SQLException e) {
                System.out.println("⚠️ Table QCM vide ou inexistante");
            }

            try (PreparedStatement stmtQL = conn.prepareStatement("SELECT COUNT(*) FROM QuestionLibre")) {
                ResultSet rsQL = stmtQL.executeQuery();
                if (rsQL.next()) totalQuestions += rsQL.getInt(1);
            } catch (SQLException e) {
                System.out.println("⚠️ Table QuestionLibre vide ou inexistante");
            }

            lblNbQuestions.setText(String.valueOf(totalQuestions));
            System.out.println("✅ " + totalQuestions + " question(s) au total");

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur de chargement", "Impossible de charger les statistiques");
        }
    }

    @FXML
    private void handleGererThemes(ActionEvent event) {
        try {
            FxmlLoader loader = new FxmlLoader();
            Parent root = loader.getPane("editer_quiz/EditerTheme");

            EditerTheme controller = (EditerTheme) loader.getController();
            if (controller != null) {
                controller.setOnThemeAdded(() -> {
                    chargerThemes();
                    chargerStatistiques();
                    System.out.println("✅ Thèmes rechargés après ajout");
                });
            }

            Stage stage = new Stage();
            stage.setTitle("Gérer les Thèmes");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

            chargerThemes();
            chargerStatistiques();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur", "Impossible d'ouvrir la gestion des thèmes : " + e.getMessage());
        }
    }

    @FXML
    private void handleRechercher(ActionEvent event) {
        String recherche = txtRecherche.getText().trim().toLowerCase();
        if (recherche.isEmpty()) {
            afficherExercices(null);
        } else {
            rechercherExercices(recherche);
        }
    }

    private void rechercherExercices(String motCle) {
        flowPaneExercices.getChildren().clear();

        // ✅ CORRECTION : Exercice_Theme avec underscore, exerciceID et themeID
        String query = """
            SELECT DISTINCT e.idExercice, e.numero, e.titre, e.consigne
            FROM Exercice e
            LEFT JOIN Exercice_Theme et ON e.idExercice = et.exerciceID
            LEFT JOIN Theme t ON et.themeID = t.idTheme
            WHERE LOWER(e.titre) LIKE ? 
               OR LOWER(e.consigne) LIKE ?
               OR LOWER(t.nomTheme) LIKE ?
            ORDER BY e.numero
            """;

        try (Connection conn = SqlConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            String pattern = "%" + motCle + "%";
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            stmt.setString(3, pattern);

            ResultSet rs = stmt.executeQuery();
            int count = 0;

            while (rs.next()) {
                int id = rs.getInt("idExercice");
                int numero = rs.getInt("numero");
                String titre = rs.getString("titre");
                String consigne = rs.getString("consigne");
                List<Theme> themes = getThemesForExercice(id);

                VBox carte = creerCarteExercice(id, numero, titre, consigne, themes);
                flowPaneExercices.getChildren().add(carte);
                count++;
            }

            if (count == 0) {
                afficherMessageVide("No exercises found for : \"" + motCle + "\"");
            }

            System.out.println("✅ Recherche: " + count + " exercice(s) trouvé(s)");
            Platform.runLater(this::ajusterLargeurCartes);

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur de recherche", "Impossible de rechercher les exercices: " + e.getMessage());
        }
    }

    private void chargerThemes() {
        String query = "SELECT idTheme, nomTheme, couleur FROM Theme ORDER BY nomTheme";

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

            System.out.println("✅ " + allThemes.size() + " thème(s) chargé(s)");
            afficherBoutonsThemes();

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur de chargement", "Impossible de charger les thèmes: " + e.getMessage());
        }
    }

    private void afficherBoutonsThemes() {
        flowPaneFiltres.getChildren().clear();

        // Bouton "Tous"
        JFXButton btnTous = new JFXButton("📋 All");
        btnTous.setStyle(
                "-fx-background-color: #5E35B1; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-radius: 20; " +
                        "-fx-padding: 8 15; " +
                        "-fx-cursor: hand;"
        );
        btnTous.setOnAction(e -> afficherExercices(null));
        flowPaneFiltres.getChildren().add(btnTous);

        for (Theme t : allThemes) {
            JFXButton btn = new JFXButton(t.getNomTheme());
            btn.setStyle(
                    "-fx-background-color: " + t.getCouleur() + "; " +
                            "-fx-text-fill: white; " +
                            "-fx-font-weight: bold; " +
                            "-fx-background-radius: 20; " +
                            "-fx-padding: 8 15; " +
                            "-fx-cursor: hand;"
            );
            btn.setOnAction(e -> afficherExercices(t));
            flowPaneFiltres.getChildren().add(btn);
        }
    }

    private void afficherExercices(Theme themeFiltre) {
        flowPaneExercices.getChildren().clear();

        // ✅ CORRECTION : Exercice_Theme, exerciceID, themeID, numero
        String query = themeFiltre == null ?
                "SELECT idExercice, numero, titre, consigne FROM Exercice ORDER BY numero" :
                """
                SELECT DISTINCT e.idExercice, e.numero, e.titre, e.consigne 
                FROM Exercice e 
                JOIN Exercice_Theme et ON e.idExercice = et.exerciceID 
                WHERE et.themeID = ?
                ORDER BY e.numero
                """;

        try (Connection conn = SqlConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            if (themeFiltre != null) {
                stmt.setInt(1, themeFiltre.getIdTheme());
            }

            ResultSet rs = stmt.executeQuery();
            int count = 0;

            while (rs.next()) {
                int id = rs.getInt("idExercice");
                int numero = rs.getInt("numero");
                String titre = rs.getString("titre");
                String consigne = rs.getString("consigne");
                List<Theme> themes = getThemesForExercice(id);

                VBox carte = creerCarteExercice(id, numero, titre, consigne, themes);
                flowPaneExercices.getChildren().add(carte);
                count++;
            }

            if (count == 0) {
                String message = themeFiltre == null ?
                        "No exercises available. Create one from the project editor!" :
                        "No exercises for the topic : " + themeFiltre.getNomTheme();
                afficherMessageVide(message);
            }

            System.out.println("✅ " + count + " exercice(s) affiché(s)" +
                    (themeFiltre != null ? " pour le thème " + themeFiltre.getNomTheme() : ""));

            Platform.runLater(() -> {
                ajusterLargeurCartes();
                Platform.runLater(() -> {
                    try {
                        Thread.sleep(50);
                    } catch (InterruptedException e) {}
                    ajusterHauteurFlowPane();
                });
            });

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur d'affichage", "Impossible d'afficher les exercices: " + e.getMessage());
        }
    }

    private List<Theme> getThemesForExercice(int exerciceID) {
        List<Theme> themes = new ArrayList<>();

        // ✅ CORRECTION : Exercice_Theme, exerciceID, themeID
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
            System.out.println("⚠️ Erreur lors du chargement des thèmes pour l'exercice " + exerciceID);
        }
        return themes;
    }

    private VBox creerCarteExercice(int id, int numero, String titre, String consigne, List<Theme> themes) {
        VBox card = new VBox(6); // ✅ Réduit l'espacement de 8 à 6
        card.setPadding(new Insets(12));
        card.setPrefWidth(100);
        card.setMaxWidth(Double.MAX_VALUE);
        card.setMinWidth(100);
        card.setMinHeight(150);
        card.setPrefHeight(150);
        card.setMaxHeight(150);

        card.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-radius: 10; " +
                        "-fx-background-radius: 10; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 8, 0, 0, 2); " +
                        "-fx-cursor: hand;"
        );
        card.setOnMouseEntered(e -> card.setStyle(
                card.getStyle().replace("rgba(0,0,0,0.15)", "rgba(0,0,0,0.25)")
        ));
        card.setOnMouseExited(e -> card.setStyle(
                card.getStyle().replace("rgba(0,0,0,0.25)", "rgba(0,0,0,0.15)")
        ));
        // En-tête
        HBox header = new HBox(5);
        header.setAlignment(Pos.CENTER_LEFT);

        Label lblNumero = new Label("Ex. " + numero);
        lblNumero.setStyle(
                "-fx-background-color: #E3F2FD; " +
                        "-fx-text-fill: #1976D2; " +
                        "-fx-padding: 3 8; " +
                        "-fx-background-radius: 5; " +
                        "-fx-font-size: 10px; " +
                        "-fx-font-weight: bold;"
        );

        Label lblType = new Label("QCM");
        lblType.setStyle(
                "-fx-background-color: #F3E5F5; " +
                        "-fx-text-fill: #7B1FA2; " +
                        "-fx-padding: 3 8; " +
                        "-fx-background-radius: 5; " +
                        "-fx-font-size: 10px; " +
                        "-fx-font-weight: bold;"
        );
        header.getChildren().addAll(lblNumero, lblType);

        // Titre
        String titreFinal = (titre != null && !titre.trim().isEmpty()) ? titre : "Sans titre";
        Label lblTitre = new Label(titreFinal);
        lblTitre.setStyle(
                "-fx-font-size: 13px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-text-fill: #5E35B1;"
        );
        lblTitre.setWrapText(true);
        lblTitre.setMaxWidth(Double.MAX_VALUE);
        lblTitre.setMaxHeight(32); // ✅ Réduit de 35 à 32

        // Consigne
        String consigneText = "Pas de consigne";
        if (consigne != null && !consigne.trim().isEmpty()) {
            consigneText = consigne.length() > 55 ? // ✅ Réduit de 60 à 55 caractères
                    consigne.substring(0, 55) + "..." : consigne;
        }

        Label lblConsigne = new Label(consigneText);
        lblConsigne.setStyle(
                "-fx-font-size: 10px; " +
                        "-fx-text-fill: #757575; " +
                        "-fx-font-style: italic;"
        );
        lblConsigne.setWrapText(true);
        lblConsigne.setMaxHeight(26); // ✅ Réduit de 30 à 26

        // Tags thèmes
        FlowPane paneThemes = new FlowPane(4, 4);
        paneThemes.setMaxWidth(Double.MAX_VALUE);
        paneThemes.setPrefHeight(22); // ✅ Réduit de 25 à 22
        paneThemes.setMaxHeight(22);

        if (themes.isEmpty()) {
            Label lblAucun = new Label("Aucun thème");
            lblAucun.setStyle(
                    "-fx-background-color: #BDBDBD; " +
                            "-fx-text-fill: white; " +
                            "-fx-padding: 2 6; " +
                            "-fx-background-radius: 8; " +
                            "-fx-font-size: 9px;"
            );
            paneThemes.getChildren().add(lblAucun);
        } else {
            int maxTags = Math.min(themes.size(), 2);
            for (int i = 0; i < maxTags; i++) {
                Theme t = themes.get(i);
                Label tag = new Label(t.getNomTheme());
                tag.setStyle(
                        "-fx-background-color: " + t.getCouleur() + "; " +
                                "-fx-text-fill: white; " +
                                "-fx-padding: 2 6; " +
                                "-fx-background-radius: 8; " +
                                "-fx-font-size: 9px;"
                );
                paneThemes.getChildren().add(tag);
            }

            if (themes.size() > 2) {
                Label plusTag = new Label("+" + (themes.size() - 2));
                plusTag.setStyle(
                        "-fx-background-color: #9E9E9E; " +
                                "-fx-text-fill: white; " +
                                "-fx-padding: 2 6; " +
                                "-fx-background-radius: 8; " +
                                "-fx-font-size: 9px;"
                );
                paneThemes.getChildren().add(plusTag);
            }
        }

        // ✅ Spacer plus petit pour remonter le bouton
        Region spacer = new Region();
        spacer.setPrefHeight(5); // Limite la hauteur du spacer
        VBox.setVgrow(spacer, Priority.SOMETIMES); // Change de ALWAYS à SOMETIMES

        // Bouton éditer
        JFXButton btnEditer = new JFXButton("✏️ Éditer");
        btnEditer.setStyle(
                "-fx-background-color: #2196F3; " +
                        "-fx-text-fill: white; " +
                        "-fx-font-size: 11px; " +
                        "-fx-font-weight: bold; " +
                        "-fx-background-radius: 15; " +
                        "-fx-padding: 5 12; " + // ✅ Réduit padding de 6 14 à 5 12
                        "-fx-cursor: hand;"
        );
        btnEditer.setOnAction(e -> editerExercice(id));
        btnEditer.setOnMouseEntered(e -> btnEditer.setStyle(
                btnEditer.getStyle().replace("#2196F3", "#1976D2")
        ));
        btnEditer.setOnMouseExited(e -> btnEditer.setStyle(
                btnEditer.getStyle().replace("#1976D2", "#2196F3")
        ));

        HBox btnContainer = new HBox(btnEditer);
        btnContainer.setAlignment(Pos.CENTER_RIGHT);
        btnContainer.setPadding(new Insets(0)); // ✅ Supprime le padding

        card.getChildren().addAll(
                header,
                lblTitre,
                lblConsigne,
                paneThemes,
                spacer,
                btnContainer
        );

        return card;
    }

    private void afficherMessageVide(String message) {
        VBox empty = new VBox(15);
        empty.setAlignment(Pos.CENTER);
        empty.setPadding(new Insets(40));
        empty.setPrefWidth(flowPaneExercices.getPrefWidth() - 40);
        empty.setStyle(
                "-fx-background-color: white; " +
                        "-fx-background-radius: 10; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);"
        );

        Label icon = new Label("📭");
        icon.setStyle("-fx-font-size: 48px;");

        Label msg = new Label(message);
        msg.setStyle(
                "-fx-font-size: 14px; " +
                        "-fx-text-fill: #999; " +
                        "-fx-wrap-text: true; " +
                        "-fx-text-alignment: center;"
        );

        empty.getChildren().addAll(icon, msg);
        flowPaneExercices.getChildren().add(empty);
    }

    private void editerExercice(int exerciceID) {
        try {
            int controleID = getControleIDFromExercice(exerciceID);
            if (controleID == -1) {
                showError("Erreur", "Contrôle introuvable pour cet exercice.");
                return;
            }

            int projetID = getProjetIDFromControle(controleID);
            if (projetID == -1) {
                showError("Erreur", "Projet introuvable pour ce contrôle.");
                return;
            }

            Projet projet = loadProjet(projetID);
            if (projet == null) {
                showError("Erreur", "Impossible de charger le projet.");
                return;
            }

            FxmlLoader loader = new FxmlLoader();
            Parent view = loader.getPane("editer_quiz/_2_EditerProjet");

            EditerProjet controller = (EditerProjet) loader.getController();
            if (controller != null) {
                controller.setParentPane(parentPane);
                controller.setProjet(projet);
            }

            parentPane.getChildren().setAll(view);
            System.out.println("✅ Navigation vers l'édition du projet " + projetID);

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur", "Impossible d'ouvrir l'éditeur : " + e.getMessage());
        }
    }

    private int getControleIDFromExercice(int exerciceID) {
        try (Connection conn = SqlConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT controleID FROM Exercice WHERE idExercice = ?"
             )) {
            stmt.setInt(1, exerciceID);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getInt("controleID") : -1;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private int getProjetIDFromControle(int controleID) {
        try (Connection conn = SqlConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT projetID FROM Controle WHERE idControle = ?"
             )) {
            stmt.setInt(1, controleID);
            ResultSet rs = stmt.executeQuery();
            return rs.next() ? rs.getInt("projetID") : -1;
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }

    private Projet loadProjet(int projetID) {
        try (Connection conn = SqlConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "SELECT * FROM Projet WHERE idProjet = ?"
             )) {
            stmt.setInt(1, projetID);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return new Projet(
                        projetID,
                        rs.getString("nomProjet"),
                        rs.getString("localisationProjet"),
                        rs.getString("typeProjet"),
                        rs.getDate("creationDate")
                );
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

}
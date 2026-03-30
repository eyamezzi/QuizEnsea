package com.example.project7.controller.edition;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;

import com.example.project7.FxmlLoader;
import com.example.project7.model.Projet;
import com.jfoenix.controls.JFXButton;
import com.jfoenix.controls.JFXCheckBox;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import sql_connection.SqlConnection;

import java.io.File;
import java.net.URL;
import java.sql.*;
import java.util.*;

public class ExtractViewController implements Initializable {

    @FXML private TextField txtRechercheProjet;
    @FXML private ListView<ProjetItem> listViewProjets;
    @FXML private Label lblNbProjets;
    @FXML private Label lblProjetSelectionne;
    @FXML private VBox vboxExercices;
    @FXML private Label lblNbExercicesSelectionnes;
    @FXML private JFXButton btnToutSelectionner;
    @FXML private JFXButton btnToutDeselectionner;
    @FXML private JFXButton btnExtraire;
    @FXML private Label lblStatut;
    // ✅ Ajouter cette variable
    private AnchorPane parentPane;

    // ✅ Ajouter ce setter
    public void setParentPane(AnchorPane pane) {
        this.parentPane = pane;
    }
    private List<ExerciceItem> exerciceItems = new ArrayList<>();
    private ProjetItem projetSelectionne = null;

    // ===================== CLASSES INTERNES =====================

    public static class ProjetItem {
        public int idProjet;
        public String nomProjet;
        public int nbControles;

        public ProjetItem(int idProjet, String nomProjet, int nbControles) {
            this.idProjet = idProjet;
            this.nomProjet = nomProjet;
            this.nbControles = nbControles;
        }

        @Override
        public String toString() { return nomProjet; }
    }

    public static class QuestionItem {
        public int idQuestion;
        public String enonce;
        public String type; // QCM ou QuestionLibre
        public JFXCheckBox checkbox = new JFXCheckBox();

        public QuestionItem(int idQuestion, String enonce, String type) {
            this.idQuestion = idQuestion;
            this.enonce = enonce;
            this.type = type;
        }
    }

    public static class ExerciceItem {
        public int idExercice;
        public int numero;
        public String titre;
        public String consigne;
        public JFXCheckBox checkbox = new JFXCheckBox();
        public List<QuestionItem> questions = new ArrayList<>();

        public ExerciceItem(int idExercice, int numero, String titre, String consigne) {
            this.idExercice = idExercice;
            this.numero = numero;
            this.titre = titre;
            this.consigne = consigne;
        }

        public boolean isSelected() { return checkbox.isSelected(); }

        public List<QuestionItem> getQuestionsSelectionnees() {
            return questions.stream()
                    .filter(q -> q.checkbox.isSelected())
                    .toList();
        }
    }

    // ===================== INITIALISATION =====================

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        System.out.println("✅ ExtractView initialisé");
        configurerListView();
        chargerProjets();
    }

    private void configurerListView() {
        listViewProjets.setCellFactory(lv -> new ListCell<ProjetItem>() {
            @Override
            protected void updateItem(ProjetItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    // ── Carte principale ──
                    HBox carte = new HBox(10);
                    carte.setPadding(new Insets(8, 10, 8, 10));
                    carte.setAlignment(Pos.CENTER_LEFT);
                    carte.setStyle(
                            "-fx-background-color: white; " +
                                    "-fx-border-color: #E0E0E0; " +
                                    "-fx-border-radius: 5; " +
                                    "-fx-background-radius: 5;"
                    );

                    // Infos projet (gauche)
                    VBox infos = new VBox(3);
                    HBox.setHgrow(infos, Priority.ALWAYS);

                    Label lblNom = new Label("📁 " + item.nomProjet);
                    lblNom.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #333;");



                    infos.getChildren().addAll(lblNom);

                    // ── Bouton œil (droite) ──
                    JFXButton btnOeil = new JFXButton("👁");
                    btnOeil.setStyle(
                            "-fx-background-color: transparent; " +
                                    "-fx-text-fill: #1976D2; " +
                                    "-fx-font-size: 16px; " +
                                    "-fx-cursor: hand; " +
                                    "-fx-padding: 5 8;"
                    );
                    btnOeil.setTooltip(new Tooltip("View this exam as a PDF"));

                    // Hover effect
                    btnOeil.setOnMouseEntered(e -> btnOeil.setStyle(
                            "-fx-background-color: #E3F2FD; " +
                                    "-fx-text-fill: #1976D2; " +
                                    "-fx-font-size: 16px; " +
                                    "-fx-cursor: hand; " +
                                    "-fx-background-radius: 20; " +
                                    "-fx-padding: 5 8;"
                    ));
                    btnOeil.setOnMouseExited(e -> btnOeil.setStyle(
                            "-fx-background-color: transparent; " +
                                    "-fx-text-fill: #1976D2; " +
                                    "-fx-font-size: 16px; " +
                                    "-fx-cursor: hand; " +
                                    "-fx-padding: 5 8;"
                    ));

                    // ✅ Clic sur l'œil → ouvrir PDF
                    btnOeil.setOnAction(e -> {
                        e.consume(); // Empêche la sélection du projet
                        ouvrirPDFDuProjet(item);
                    });

                    carte.getChildren().addAll(infos, btnOeil);
                    setGraphic(carte);
                }
            }
        });

        listViewProjets.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                projetSelectionne = newVal;
                chargerExercices(newVal.idProjet);
            }
        });
    }

    // ===================== CHARGEMENT DONNÉES =====================

    private void chargerProjets() {
        listViewProjets.getItems().clear();

        String query = """
            SELECT p.idProjet, p.nomProjet, COUNT(c.idControle) as nbControles
            FROM Projet p
            LEFT JOIN Controle c ON p.idProjet = c.projetID
            GROUP BY p.idProjet, p.nomProjet
            HAVING COUNT(c.idControle) > 0
            ORDER BY p.nomProjet
        """;

        try (Connection conn = SqlConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            int count = 0;
            while (rs.next()) {
                listViewProjets.getItems().add(new ProjetItem(
                        rs.getInt("idProjet"),
                        rs.getString("nomProjet"),
                        rs.getInt("nbControles")
                ));
                count++;
            }
            lblNbProjets.setText(count + " project(s)");

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur", "Impossible de charger les projets : " + e.getMessage());
        }
    }

    private void chargerExercices(int projetID) {
        vboxExercices.getChildren().clear();
        exerciceItems.clear();

        // ✅ CORRECTION : Filtrer seulement les exercices avec au moins 1 question
        String queryExercices = """
        SELECT e.idExercice, e.numero, e.titre, e.consigne
        FROM Exercice e
        JOIN Controle c ON e.controleID = c.idControle
        WHERE c.projetID = ?
        AND (
            EXISTS (
                SELECT 1 FROM Section s 
                JOIN QCM q ON s.idSection = q.sectionID 
                WHERE s.exerciceID = e.idExercice
            )
            OR EXISTS (
                SELECT 1 FROM Section s 
                JOIN QuestionLibre ql ON s.idSection = ql.sectionID 
                WHERE s.exerciceID = e.idExercice
            )
        )
        ORDER BY e.numero
    """;

        try (Connection conn = SqlConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(queryExercices)) {

            stmt.setInt(1, projetID);
            ResultSet rs = stmt.executeQuery();

            int count = 0;
            while (rs.next()) {
                ExerciceItem item = new ExerciceItem(
                        rs.getInt("idExercice"),
                        rs.getInt("numero"),
                        rs.getString("titre"),
                        rs.getString("consigne")
                );

                chargerQuestions(conn, item);
                VBox carteExercice = creerCarteExercice(item);
                vboxExercices.getChildren().add(carteExercice);
                exerciceItems.add(item);
                count++;
            }

            String nomProjet = projetSelectionne != null ? projetSelectionne.nomProjet : "";
            lblProjetSelectionne.setText(count + " exercice(s) dans " + nomProjet);

            if (count == 0) {
                Label lblVide = new Label("📭 Aucun exercice avec des questions dans ce projet.");
                lblVide.setStyle("-fx-font-size: 13px; -fx-text-fill: #999; -fx-padding: 20;");
                vboxExercices.getChildren().add(lblVide);
            }

            updateBouton();

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur", "Impossible de charger les exercices : " + e.getMessage());
        }
    }

    private void chargerQuestions(Connection conn, ExerciceItem exercice) throws SQLException {
        // Charger les QCM via les sections
        String queryQCM = """
            SELECT q.idQCM, q.question, q.isQCU
            FROM QCM q
            JOIN Section s ON q.sectionID = s.idSection
            WHERE s.exerciceID = ?
        """;

        try (PreparedStatement stmt = conn.prepareStatement(queryQCM)) {
            stmt.setInt(1, exercice.idExercice);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                boolean isQCU = rs.getBoolean("isQCU");
                String type = isQCU ? "QCU" : "QCM";
                QuestionItem q = new QuestionItem(
                        rs.getInt("idQCM"),
                        rs.getString("question"),
                        type
                );
                exercice.questions.add(q);
            }
        }

        // Charger les Questions Libres
        String queryQL = """
            SELECT ql.idQuestionLibre, ql.question
            FROM QuestionLibre ql
            JOIN Section s ON ql.sectionID = s.idSection
            WHERE s.exerciceID = ?
        """;

        try (PreparedStatement stmt = conn.prepareStatement(queryQL)) {
            stmt.setInt(1, exercice.idExercice);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                QuestionItem q = new QuestionItem(
                        rs.getInt("idQuestionLibre"),
                        rs.getString("question"),
                        "Libre"
                );
                exercice.questions.add(q);
            }
        }
    }

    // ===================== CRÉATION DES CARTES =====================

    private VBox creerCarteExercice(ExerciceItem item) {
        VBox carte = new VBox(0);
        carte.setStyle(
                "-fx-background-color: white; " +
                        "-fx-border-color: #E0E0E0; " +
                        "-fx-border-radius: 8; " +
                        "-fx-background-radius: 8; " +
                        "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.08), 5, 0, 0, 1);"
        );

        // ── En-tête de l'exercice (cliquable pour déplier) ──
        HBox header = new HBox(10);
        header.setPadding(new Insets(12, 15, 12, 15));
        header.setAlignment(Pos.CENTER_LEFT);
        header.setStyle("-fx-cursor: hand; -fx-background-radius: 8;");

        // Checkbox exercice
        item.checkbox.setOnAction(e -> {
            // Sélectionner/désélectionner toutes les questions
            boolean selected = item.checkbox.isSelected();
            item.questions.forEach(q -> q.checkbox.setSelected(selected));
            updateBouton();
        });

        // Badge numéro
        Label lblNum = new Label("Ex. " + item.numero);
        lblNum.setStyle(
                "-fx-background-color: #E8EAF6; " +
                        "-fx-text-fill: #5E35B1; " +
                        "-fx-padding: 3 8; " +
                        "-fx-background-radius: 5; " +
                        "-fx-font-size: 11px; " +
                        "-fx-font-weight: bold;"
        );

        // Badge nb questions
        Label lblNbQ = new Label(item.questions.size() + " question(s)");
        lblNbQ.setStyle(
                "-fx-background-color: #E3F2FD; " +
                        "-fx-text-fill: #1565C0; " +
                        "-fx-padding: 3 8; " +
                        "-fx-background-radius: 5; " +
                        "-fx-font-size: 11px;"
        );

        // Titre exercice
        String titreFinal = (item.titre != null && !item.titre.isBlank())
                ? item.titre : "Untitled";
        Label lblTitre = new Label(titreFinal);
        lblTitre.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #333;");
        HBox.setHgrow(lblTitre, Priority.ALWAYS);

        // Flèche déplier
        Label lblArrow = new Label("▶");
        lblArrow.setStyle("-fx-font-size: 10px; -fx-text-fill: #999;");

        header.getChildren().addAll(item.checkbox, lblNum, lblNbQ, lblTitre, lblArrow);

        // ── Zone questions (cachée par défaut) ──
        VBox zoneQuestions = new VBox(6);
        zoneQuestions.setPadding(new Insets(0, 15, 12, 40));
        zoneQuestions.setVisible(false);
        zoneQuestions.setManaged(false);

        for (QuestionItem question : item.questions) {
            HBox ligneQ = creerLigneQuestion(question, item);
            zoneQuestions.getChildren().add(ligneQ);
        }

        // Clic sur header → déplier/replier
        header.setOnMouseClicked(e -> {
            boolean isVisible = zoneQuestions.isVisible();
            zoneQuestions.setVisible(!isVisible);
            zoneQuestions.setManaged(!isVisible);
            lblArrow.setText(isVisible ? "▶" : "▼");

            // Highlight header quand ouvert
            if (!isVisible) {
                header.setStyle("-fx-background-color: #F3E5F5; -fx-cursor: hand; -fx-background-radius: 8 8 0 0;");
            } else {
                header.setStyle("-fx-cursor: hand; -fx-background-radius: 8;");
            }
        });

        carte.getChildren().addAll(header, zoneQuestions);
        return carte;
    }

    private HBox creerLigneQuestion(QuestionItem question, ExerciceItem exercice) {
        HBox ligne = new HBox(10);
        ligne.setPadding(new Insets(6, 10, 6, 10));
        ligne.setAlignment(Pos.CENTER_LEFT);
        ligne.setStyle(
                "-fx-background-color: #FAFAFA; " +
                        "-fx-border-color: #F0F0F0; " +
                        "-fx-border-radius: 5; " +
                        "-fx-background-radius: 5;"
        );

        question.checkbox.setOnAction(e -> {
            // Mettre à jour la checkbox de l'exercice parent
            boolean toutesSelectionnees = exercice.questions.stream()
                    .allMatch(q -> q.checkbox.isSelected());
            boolean aucuneSelectionnee = exercice.questions.stream()
                    .noneMatch(q -> q.checkbox.isSelected());

            if (toutesSelectionnees) {
                exercice.checkbox.setSelected(true);
                exercice.checkbox.setIndeterminate(false);
            } else if (aucuneSelectionnee) {
                exercice.checkbox.setSelected(false);
                exercice.checkbox.setIndeterminate(false);
            } else {
                exercice.checkbox.setSelected(false);
                exercice.checkbox.setIndeterminate(true);
            }
            updateBouton();
        });

        // Badge type
        String couleurType = switch (question.type) {
            case "QCM" -> "#E8F5E9; -fx-text-fill: #2E7D32";
            case "QCU" -> "#FFF3E0; -fx-text-fill: #E65100";
            default -> "#FCE4EC; -fx-text-fill: #C62828";
        };

        Label lblType = new Label(question.type);
        lblType.setStyle(
                "-fx-background-color: " + couleurType + "; " +
                        "-fx-padding: 2 6; " +
                        "-fx-background-radius: 4; " +
                        "-fx-font-size: 10px; " +
                        "-fx-font-weight: bold;"
        );

        // Texte question (tronqué)
        String enonce = question.enonce != null ? question.enonce : "Question text unavailable";
        if (enonce.length() > 70) enonce = enonce.substring(0, 70) + "...";

        Label lblQuestion = new Label(enonce);
        lblQuestion.setStyle("-fx-font-size: 12px; -fx-text-fill: #555;");
        lblQuestion.setWrapText(false);
        HBox.setHgrow(lblQuestion, Priority.ALWAYS);

        ligne.getChildren().addAll(question.checkbox, lblType, lblQuestion);
        return ligne;
    }

    // ===================== ACTIONS =====================

    @FXML
    private void handleRechercheProjet() {
        String recherche = txtRechercheProjet.getText().toLowerCase().trim();
        listViewProjets.getItems().clear();

        String query = recherche.isEmpty()
                ? """
              SELECT p.idProjet, p.nomProjet, COUNT(c.idControle) as nbControles
              FROM Projet p LEFT JOIN Controle c ON p.idProjet = c.projetID
              GROUP BY p.idProjet, p.nomProjet HAVING COUNT(c.idControle) > 0 ORDER BY p.nomProjet
              """
                : """
              SELECT p.idProjet, p.nomProjet, COUNT(c.idControle) as nbControles
              FROM Projet p LEFT JOIN Controle c ON p.idProjet = c.projetID
              WHERE LOWER(p.nomProjet) LIKE ?
              GROUP BY p.idProjet, p.nomProjet HAVING COUNT(c.idControle) > 0 ORDER BY p.nomProjet
              """;

        try (Connection conn = SqlConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            if (!recherche.isEmpty()) stmt.setString(1, "%" + recherche + "%");
            ResultSet rs = stmt.executeQuery();

            int count = 0;
            while (rs.next()) {
                listViewProjets.getItems().add(new ProjetItem(
                        rs.getInt("idProjet"),
                        rs.getString("nomProjet"),
                        rs.getInt("nbControles")
                ));
                count++;
            }
            lblNbProjets.setText(count + " projet(s)");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleToutSelectionner() {
        exerciceItems.forEach(ex -> {
            ex.checkbox.setSelected(true);
            ex.checkbox.setIndeterminate(false);
            ex.questions.forEach(q -> q.checkbox.setSelected(true));
        });
        updateBouton();
    }

    @FXML
    private void handleToutDeselectionner() {
        exerciceItems.forEach(ex -> {
            ex.checkbox.setSelected(false);
            ex.checkbox.setIndeterminate(false);
            ex.questions.forEach(q -> q.checkbox.setSelected(false));
        });
        updateBouton();
    }

    @FXML

    private void handleExtraire() {
        // ✅ Inclure les exercices avec au moins 1 question sélectionnée
        List<ExerciceItem> selectionnes = exerciceItems.stream()
                .filter(ex -> ex.isSelected() || !ex.getQuestionsSelectionnees().isEmpty())
                .toList();

        if (selectionnes.isEmpty()) {
            showAlert("Aucune sélection", "Veuillez sélectionner au moins un exercice ou une question.");
            return;
        }

        // Compter le total de questions sélectionnées
        int totalQuestions = selectionnes.stream()
                .mapToInt(ex -> ex.isSelected() ?
                        ex.questions.size() :
                        ex.getQuestionsSelectionnees().size())
                .sum();

        // Dialogue de choix
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Extraire les exercices");
        dialog.setHeaderText(
                "📊 " + selectionnes.size() + " exercice(s) | " + totalQuestions + " question(s) sélectionné(s)"
        );

        ButtonType btnNouvelExamen = new ButtonType("📄 Nouvel examen", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnExamenExistant = new ButtonType("📂 Examen existant", ButtonBar.ButtonData.APPLY);
        ButtonType btnAnnuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);

        dialog.getDialogPane().getButtonTypes().addAll(btnNouvelExamen, btnExamenExistant, btnAnnuler);
        dialog.getDialogPane().setContent(
                new Label("Voulez-vous extraire vers un nouvel examen ou un examen existant ?")
        );

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() == btnAnnuler) return;

        if (result.get() == btnNouvelExamen) {
            extraireVersNouvelExamen(selectionnes);
        } else {
            extraireVersExamenExistant(selectionnes);
        }
    }
    // ── Extraction vers NOUVEL examen ──
    private void extraireVersNouvelExamen(List<ExerciceItem> selectionnes) {
        // Étape 1 : Nom de l'examen
        TextInputDialog dialogNom = new TextInputDialog();
        dialogNom.setTitle("New Exam");
        dialogNom.setHeaderText("Create a new exam");
        dialogNom.setContentText("Exam name:");
        dialogNom.getEditor().setPromptText("Ex: Contrôle Java - Juin 2025");

        Optional<String> nomResult = dialogNom.showAndWait();
        if (nomResult.isEmpty() || nomResult.get().trim().isEmpty()) return;
        String nomExamen = nomResult.get().trim();

        // Étape 2 : Choisir le dossier de localisation
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choose save location");
        dirChooser.setInitialDirectory(new File(System.getProperty("user.home")));

        Stage stage = (Stage) btnExtraire.getScene().getWindow();
        File dossierChoisi = dirChooser.showDialog(stage);

        if (dossierChoisi == null) return;
        String localisation = dossierChoisi.getAbsolutePath();

        // Étape 3 : Créer le projet et contrôle dans la base
        try (Connection conn = SqlConnection.getConnection()) {

            // Créer le projet
            String insertProjet = "INSERT INTO Projet (nomProjet, localisationProjet, typeProjet) VALUES (?, ?, 'Examen')";
            PreparedStatement stmtP = conn.prepareStatement(insertProjet, Statement.RETURN_GENERATED_KEYS);
            stmtP.setString(1, nomExamen);
            stmtP.setString(2, localisation);
            stmtP.executeUpdate();

            ResultSet rsP = stmtP.getGeneratedKeys();
            if (!rsP.next()) { showError("Error", "Unable to create the project."); return; }
            int nouveauProjetID = rsP.getInt(1);

            // Créer le contrôle
            String insertControle = "INSERT INTO Controle (nomDevoir, typeDevoir, projetID) VALUES (?, 'Controle Continu', ?)";
            PreparedStatement stmtC = conn.prepareStatement(insertControle, Statement.RETURN_GENERATED_KEYS);
            stmtC.setString(1, nomExamen);
            stmtC.setInt(2, nouveauProjetID);
            stmtC.executeUpdate();

            ResultSet rsC = stmtC.getGeneratedKeys();
            if (!rsC.next()) { showError("Erreur", "Impossible de créer le contrôle."); return; }
            int nouveauControleID = rsC.getInt(1);

            // Copier les exercices
            int nbExtraits = copierExercices(conn, selectionnes, nouveauControleID);

            showAlert("✅ Extraction successful !",
                    String.format("Exam created : %s\nLocation : %s\n%d exercise(s) extracted",
                            nomExamen, localisation, nbExtraits));

            chargerProjets();

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur SQL", "Impossible de créer l'examen : " + e.getMessage());
        }
    }

    // ── Extraction vers EXAMEN EXISTANT ──
    private void extraireVersExamenExistant(List<ExerciceItem> selectionnes) {
        // Charger les examens existants
        List<ControleItem> controles = new ArrayList<>();

        String query = """
            SELECT c.idControle, c.nomDevoir, p.nomProjet
            FROM Controle c
            JOIN Projet p ON c.projetID = p.idProjet
            ORDER BY p.nomProjet, c.nomDevoir
        """;

        try (Connection conn = SqlConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                controles.add(new ControleItem(
                        rs.getInt("idControle"),
                        rs.getString("nomDevoir"),
                        rs.getString("nomProjet")
                ));
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur", "Impossible de charger les examens : " + e.getMessage());
            return;
        }

        if (controles.isEmpty()) {
            showAlert("Aucun examen", "Aucun examen existant trouvé.");
            return;
        }

        // Dialogue de sélection
        ChoiceDialog<ControleItem> choiceDialog = new ChoiceDialog<>(controles.get(0), controles);
        choiceDialog.setTitle("Choisir l'examen");
        choiceDialog.setHeaderText("Ajouter les exercices à un examen existant");
        choiceDialog.setContentText("Examen :");

        Optional<ControleItem> choix = choiceDialog.showAndWait();
        if (choix.isEmpty()) return;

        ControleItem controleChoisi = choix.get();

        try (Connection conn = SqlConnection.getConnection()) {
            int nbExtraits = copierExercices(conn, selectionnes, controleChoisi.idControle);

            showAlert("✅ Extraction réussie !",
                    String.format("%d exercice(s) ajouté(s) à l'examen :\n%s → %s",
                            nbExtraits, controleChoisi.nomProjet, controleChoisi.nomDevoir));

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur SQL", "Impossible d'ajouter les exercices : " + e.getMessage());
        }
    }

    // ── Copier les exercices dans un contrôle cible ──
    private int copierExercices(Connection conn, List<ExerciceItem> exercices, int controleID) throws SQLException {
        int numeroExercice = getProchainNumero(conn, controleID);
        int nbExtraits = 0;

        for (ExerciceItem ex : exercices) {

            // ✅ Récupérer UNIQUEMENT les questions sélectionnées
            List<QuestionItem> questionsSelectionnees = ex.getQuestionsSelectionnees();

            // Si aucune question sélectionnée, skip cet exercice
            if (questionsSelectionnees.isEmpty()) continue;

            // Copier l'exercice
            String insertEx = """
            INSERT INTO Exercice (numero, titre, consigne, bareme, controleID)
            SELECT ?, titre, consigne, bareme, ?
            FROM Exercice WHERE idExercice = ?
        """;
            PreparedStatement stmtEx = conn.prepareStatement(insertEx, Statement.RETURN_GENERATED_KEYS);
            stmtEx.setInt(1, numeroExercice++);
            stmtEx.setInt(2, controleID);
            stmtEx.setInt(3, ex.idExercice);
            stmtEx.executeUpdate();

            ResultSet rsEx = stmtEx.getGeneratedKeys();
            if (!rsEx.next()) continue;
            int nouvelExerciceID = rsEx.getInt(1);

            // ✅ Copier SEULEMENT les sections des questions sélectionnées
            copierSectionsSelectionnees(conn, ex, nouvelExerciceID, controleID, questionsSelectionnees);
            nbExtraits++;
        }

        return nbExtraits;
    }
    private void copierSectionsSelectionnees(
            Connection conn,
            ExerciceItem ex,
            int nouvelExerciceID,
            int controleID,
            List<QuestionItem> questionsSelectionnees) throws SQLException {

        int ordreSection = 1;

        for (QuestionItem question : questionsSelectionnees) {

            // ✅ Trouver la section qui contient cette question spécifique
            String sectionID = getSectionIDPourQuestion(conn, question);

            if (sectionID == null) {
                System.out.println("⚠️ Section introuvable pour question : " + question.enonce);
                continue;
            }

            // Créer un nouveau sectionID unique
            String nouveauSectionID = "s_" + controleID + "_" + nouvelExerciceID + "_" + ordreSection + "_" + System.currentTimeMillis();

            // Insérer la section
            String insertSection = """
            INSERT INTO Section (idSection, ordreSection, numberOfSections, controleID, exerciceID)
            VALUES (?, ?, ?, ?, ?)
        """;
            PreparedStatement stmtSection = conn.prepareStatement(insertSection);
            stmtSection.setString(1, nouveauSectionID);
            stmtSection.setInt(2, ordreSection++);
            stmtSection.setInt(3, 1);
            stmtSection.setInt(4, controleID);
            stmtSection.setInt(5, nouvelExerciceID);
            stmtSection.executeUpdate();

            // ✅ Copier selon le type de question
            if (question.type.equals("QCM") || question.type.equals("QCU")) {
                copierQCMParID(conn, question.idQuestion, nouveauSectionID);
            } else if (question.type.equals("Libre")) {
                copierQuestionLibreParID(conn, question.idQuestion, nouveauSectionID);
            }

            System.out.println("✅ Question copiée : " + question.enonce.substring(0, Math.min(30, question.enonce.length())));
        }
    }
    private String getSectionIDPourQuestion(Connection conn, QuestionItem question) throws SQLException {
        String sectionID = null;

        if (question.type.equals("QCM") || question.type.equals("QCU")) {
            String query = "SELECT sectionID FROM QCM WHERE idQCM = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, question.idQuestion);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) sectionID = rs.getString("sectionID");

        } else if (question.type.equals("Libre")) {
            String query = "SELECT sectionID FROM QuestionLibre WHERE idQuestionLibre = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, question.idQuestion);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) sectionID = rs.getString("sectionID");
        }

        return sectionID;
    }


    private void copierSections(Connection conn, int ancienExerciceID, int nouvelExerciceID, int controleID, ExerciceItem ex) throws SQLException {
        String getSections = "SELECT * FROM Section WHERE exerciceID = ? ORDER BY ordreSection";
        PreparedStatement stmtGet = conn.prepareStatement(getSections);
        stmtGet.setInt(1, ancienExerciceID);
        ResultSet rs = stmtGet.executeQuery();

        while (rs.next()) {
            String ancienSectionID = rs.getString("idSection");
            String nouveauSectionID = "s_" + controleID + "_" + nouvelExerciceID + "_" + rs.getInt("ordreSection") + "_" + System.currentTimeMillis();

            // Insérer la section
            String insertSection = "INSERT INTO Section (idSection, ordreSection, numberOfSections, controleID, exerciceID) VALUES (?, ?, ?, ?, ?)";
            PreparedStatement stmtSection = conn.prepareStatement(insertSection);
            stmtSection.setString(1, nouveauSectionID);
            stmtSection.setInt(2, rs.getInt("ordreSection"));
            stmtSection.setInt(3, rs.getInt("numberOfSections"));
            stmtSection.setInt(4, controleID);
            stmtSection.setInt(5, nouvelExerciceID);
            stmtSection.executeUpdate();

            // Copier QCM de cette section
            copierQCM(conn, ancienSectionID, nouveauSectionID);

            // Copier Questions Libres
            copierQuestionsLibres(conn, ancienSectionID, nouveauSectionID);
        }
    }

    private void copierQCM(Connection conn, String ancienSectionID, String nouveauSectionID) throws SQLException {
        // Récupérer les QCM
        String getQCM = "SELECT * FROM QCM WHERE sectionID = ?";
        PreparedStatement stmtGet = conn.prepareStatement(getQCM);
        stmtGet.setString(1, ancienSectionID);
        ResultSet rs = stmtGet.executeQuery();

        while (rs.next()) {
            int ancienQCMID = rs.getInt("idQCM");

            // Insérer le QCM
            String insertQCM = "INSERT INTO QCM (question, isQCU, sectionID) VALUES (?, ?, ?)";
            PreparedStatement stmtInsert = conn.prepareStatement(insertQCM, Statement.RETURN_GENERATED_KEYS);
            stmtInsert.setString(1, rs.getString("question"));
            stmtInsert.setBoolean(2, rs.getBoolean("isQCU"));
            stmtInsert.setString(3, nouveauSectionID);
            stmtInsert.executeUpdate();

            ResultSet rsNew = stmtInsert.getGeneratedKeys();
            if (!rsNew.next()) continue;
            int nouvelQCMID = rsNew.getInt(1);

            // Copier les réponses
            String getReponses = "SELECT * FROM QCM_Reponses WHERE qcmID = ?";
            PreparedStatement stmtRep = conn.prepareStatement(getReponses);
            stmtRep.setInt(1, ancienQCMID);
            ResultSet rsRep = stmtRep.executeQuery();

            while (rsRep.next()) {
                String insertRep = "INSERT INTO QCM_Reponses (qcmID, reponse, score, isCorrect) VALUES (?, ?, ?, ?)";
                PreparedStatement stmtInsertRep = conn.prepareStatement(insertRep);
                stmtInsertRep.setInt(1, nouvelQCMID);
                stmtInsertRep.setString(2, rsRep.getString("reponse"));
                stmtInsertRep.setInt(3, rsRep.getInt("score"));
                stmtInsertRep.setBoolean(4, rsRep.getBoolean("isCorrect"));
                stmtInsertRep.executeUpdate();
            }
        }
    }
    private void copierQCMParID(Connection conn, int ancienQCMID, String nouveauSectionID) throws SQLException {

        // Copier le QCM
        String insertQCM = """
        INSERT INTO QCM (question, isQCU, sectionID)
        SELECT question, isQCU, ?
        FROM QCM WHERE idQCM = ?
    """;
        PreparedStatement stmtInsert = conn.prepareStatement(insertQCM, Statement.RETURN_GENERATED_KEYS);
        stmtInsert.setString(1, nouveauSectionID);
        stmtInsert.setInt(2, ancienQCMID);
        stmtInsert.executeUpdate();

        ResultSet rsNew = stmtInsert.getGeneratedKeys();
        if (!rsNew.next()) return;
        int nouvelQCMID = rsNew.getInt(1);

        // Copier les réponses
        String getReponses = "SELECT * FROM QCM_Reponses WHERE qcmID = ?";
        PreparedStatement stmtRep = conn.prepareStatement(getReponses);
        stmtRep.setInt(1, ancienQCMID);
        ResultSet rsRep = stmtRep.executeQuery();

        while (rsRep.next()) {
            String insertRep = "INSERT INTO QCM_Reponses (qcmID, reponse, score, isCorrect) VALUES (?, ?, ?, ?)";
            PreparedStatement stmtInsertRep = conn.prepareStatement(insertRep);
            stmtInsertRep.setInt(1, nouvelQCMID);
            stmtInsertRep.setString(2, rsRep.getString("reponse"));
            stmtInsertRep.setInt(3, rsRep.getInt("score"));
            stmtInsertRep.setBoolean(4, rsRep.getBoolean("isCorrect"));
            stmtInsertRep.executeUpdate();
        }

        System.out.println("✅ QCM #" + ancienQCMID + " copié → section " + nouveauSectionID);
    }
    private void copierQuestionLibreParID(Connection conn, int ancienQLID, String nouveauSectionID) throws SQLException {

        String insertQL = """
        INSERT INTO QuestionLibre 
            (question, scoreTotal, nombreScore, nombreLigne, tailleLigne, rappel, sectionID)
        SELECT 
            question, scoreTotal, nombreScore, nombreLigne, tailleLigne, rappel, ?
        FROM QuestionLibre 
        WHERE idQuestionLibre = ?
    """;

        PreparedStatement stmtInsert = conn.prepareStatement(insertQL);
        stmtInsert.setString(1, nouveauSectionID);
        stmtInsert.setInt(2, ancienQLID);
        stmtInsert.executeUpdate();

        System.out.println("✅ QuestionLibre #" + ancienQLID + " copiée → section " + nouveauSectionID);
    }
    private void copierQuestionsLibres(Connection conn, String ancienSectionID, String nouveauSectionID) throws SQLException {
        String getQL = "SELECT * FROM QuestionLibre WHERE sectionID = ?";
        PreparedStatement stmtGet = conn.prepareStatement(getQL);
        stmtGet.setString(1, ancienSectionID);
        ResultSet rs = stmtGet.executeQuery();

        while (rs.next()) {
            String insertQL = """
                INSERT INTO QuestionLibre (question, scoreTotal, nombreScore, nombreLigne, tailleLigne, rappel, sectionID)
                VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
            PreparedStatement stmtInsert = conn.prepareStatement(insertQL);
            stmtInsert.setString(1, rs.getString("question"));
            stmtInsert.setFloat(2, rs.getFloat("scoreTotal"));
            stmtInsert.setInt(3, rs.getInt("nombreScore"));
            stmtInsert.setInt(4, rs.getInt("nombreLigne"));
            stmtInsert.setFloat(5, rs.getFloat("tailleLigne"));
            stmtInsert.setString(6, rs.getString("rappel"));
            stmtInsert.setString(7, nouveauSectionID);
            stmtInsert.executeUpdate();
        }
    }

    private int getProchainNumero(Connection conn, int controleID) throws SQLException {
        String query = "SELECT COALESCE(MAX(numero), 0) + 1 FROM Exercice WHERE controleID = ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setInt(1, controleID);
        ResultSet rs = stmt.executeQuery();
        return rs.next() ? rs.getInt(1) : 1;
    }

    // ===================== HELPERS =====================

    private void updateBouton() {
        long nb = exerciceItems.stream()
                .filter(ex -> ex.isSelected() || !ex.getQuestionsSelectionnees().isEmpty())
                .count();
        lblNbExercicesSelectionnes.setText(nb + " exercise(s) selected");
        btnExtraire.setDisable(nb == 0);
    }

    // Classe pour les contrôles existants
    public static class ControleItem {
        public int idControle;
        public String nomDevoir;
        public String nomProjet;

        public ControleItem(int idControle, String nomDevoir, String nomProjet) {
            this.idControle = idControle;
            this.nomDevoir = nomDevoir;
            this.nomProjet = nomProjet;
        }

        @Override
        public String toString() { return nomProjet + " → " + nomDevoir; }
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    private void ouvrirPDFDuProjet(ProjetItem projet) {

        String query = "SELECT nomProjet, localisationProjet FROM Projet WHERE idProjet = ?";

        try (Connection conn = SqlConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, projet.idProjet);
            ResultSet rs = stmt.executeQuery();

            if (!rs.next()) {
                showAlert("Introuvable", "Projet introuvable.");
                return;
            }

            String nomProjet    = rs.getString("nomProjet");
            String localisation = rs.getString("localisationProjet");

            // ✅ Cas 1 : Pas de localisation configurée
            if (localisation == null || localisation.isBlank()) {
                showAlert("PDF non disponible",
                        "⚠️ Ce projet n'a pas encore de PDF généré.\n\n" +
                                "Pour générer le PDF :\n" +
                                "1. Allez dans 'Open'\n" +
                                "2. Ouvrez le projet '" + nomProjet + "'\n" +
                                "3. Cliquez sur 'Save/Done'");
                return;
            }

            // ✅ Construire le chemin du PDF
            File pdfFile = new File(
                    localisation + File.separator + nomProjet,
                    "Preremplie-ensemble.pdf"
            );

            System.out.println("📄 Cherche PDF : " + pdfFile.getAbsolutePath());

            // ✅ Cas 2 : PDF n'existe pas encore → proposer d'ouvrir le projet
            if (!pdfFile.exists()) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("PDF non généré");
                alert.setHeaderText("⚠️ Le PDF n'existe pas encore pour : " + nomProjet);
                alert.setContentText(
                        "Le PDF de ce projet n'a pas encore été généré.\n\n" +
                                "Voulez-vous ouvrir ce projet pour générer le PDF ?"
                );

                ButtonType btnOuvrir = new ButtonType("📂 Ouvrir le projet", ButtonBar.ButtonData.OK_DONE);
                ButtonType btnAnnuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
                alert.getButtonTypes().setAll(btnOuvrir, btnAnnuler);

                alert.showAndWait().ifPresent(response -> {
                    if (response == btnOuvrir) {
                        ouvrirProjetPourEdition(projet.idProjet);
                    }
                });
                return;
            }

            // ✅ Cas 3 : PDF existe → ouvrir directement
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(pdfFile);
                System.out.println("✅ PDF ouvert : " + pdfFile.getName());
            } else {
                showError("Non supporté", "Impossible d'ouvrir le PDF sur cette plateforme.");
            }

        } catch (SQLException e) {
            e.printStackTrace();
            showError("Erreur SQL", "Impossible de récupérer les infos : " + e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            showError("Erreur", "Impossible d'ouvrir le PDF : " + e.getMessage());
        }
    }
    private void ouvrirProjetPourEdition(int projetID) {
        try {
            // Charger le projet depuis la BDD
            String query = "SELECT * FROM Projet WHERE idProjet = ?";

            try (Connection conn = SqlConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {

                stmt.setInt(1, projetID);
                ResultSet rs = stmt.executeQuery();

                if (!rs.next()) {
                    showError("Erreur", "Projet introuvable.");
                    return;
                }

                Projet projet = new Projet(
                        projetID,
                        rs.getString("nomProjet"),
                        rs.getString("localisationProjet"),
                        rs.getString("typeProjet"),
                        rs.getDate("creationDate")
                );

                // ✅ Naviguer vers EditerProjet (même logique que handleClicksOpen)
                FxmlLoader loader = new FxmlLoader();
                Parent view = loader.getPane("editer_quiz/_2_EditerProjet");

                EditerProjet controller = (EditerProjet) loader.getController();
                if (controller != null) {
                    controller.setParentPane(parentPane);
                    controller.setProjet(projet);
                }

                parentPane.getChildren().clear();
                parentPane.getChildren().add(view);

                AnchorPane.setTopAnchor(view, 0.0);
                AnchorPane.setBottomAnchor(view, 0.0);
                AnchorPane.setLeftAnchor(view, 0.0);
                AnchorPane.setRightAnchor(view, 0.0);

                System.out.println("✅ Navigation vers EditerProjet pour : " + projet.getNomProjet());
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur", "Impossible d'ouvrir le projet : " + e.getMessage());
        }
    }
    private void ouvrirAvecCommandeSystème(File pdfFile) {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;

            if (os.contains("win")) {
                // Windows
                pb = new ProcessBuilder("cmd", "/c", "start", "", pdfFile.getAbsolutePath());
            } else if (os.contains("mac")) {
                // macOS
                pb = new ProcessBuilder("open", pdfFile.getAbsolutePath());
            } else {
                // Linux
                pb = new ProcessBuilder("xdg-open", pdfFile.getAbsolutePath());
            }

            pb.start();
            System.out.println("✅ PDF ouvert via commande système");

        } catch (Exception e) {
            showError("Erreur", "Impossible d'ouvrir le PDF.\nChemin : " + pdfFile.getAbsolutePath());
        }
    }

    private File[] cherchierPDFRecursivement(File dossier) {
        List<File> pdfs = new ArrayList<>();
        cherchierPDFDans(dossier, pdfs, 0);
        return pdfs.toArray(new File[0]);
    }

    private void cherchierPDFDans(File dossier, List<File> pdfs, int profondeur) {
        if (profondeur > 3) return; // Max 3 niveaux de profondeur

        File[] fichiers = dossier.listFiles();
        if (fichiers == null) return;

        for (File f : fichiers) {
            if (f.isFile() && f.getName().toLowerCase().endsWith(".pdf")) {
                pdfs.add(f);
            } else if (f.isDirectory()) {
                cherchierPDFDans(f, pdfs, profondeur + 1);
            }
        }
    }

}

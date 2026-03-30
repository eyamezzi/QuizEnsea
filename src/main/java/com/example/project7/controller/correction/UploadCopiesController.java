package com.example.project7.controller.correction;

import com.example.project7.model.Controle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import com.jfoenix.controls.JFXButton;
import sql_connection.SqlConnection;

import java.io.File;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

public class UploadCopiesController implements Initializable {

    @FXML
    private Label lblNomExamen;

    @FXML
    private Label lblNbQuestions;

    @FXML
    private Label lblNbFichiers;

    @FXML
    private ListView<String> listViewFichiers;

    @FXML
    private JFXButton btnLancerCorrection;

    private Controle controle;
    private int nbQuestionsOuvertes;
    private ObservableList<File> fichiersCopies = FXCollections.observableArrayList();

    /**
     * Initialiser avec les données de l'examen
     */
    public void setControle(Controle controle, int nbQuestionsOuvertes) {
        this.controle = controle;
        this.nbQuestionsOuvertes = nbQuestionsOuvertes;

        lblNomExamen.setText(controle.getNomDevoir());
        lblNbQuestions.setText(String.valueOf(nbQuestionsOuvertes));
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        // Configuration de la ListView avec affichage personnalisé
        listViewFichiers.setCellFactory(param -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(item);
                    setStyle("-fx-font-size: 13px; -fx-padding: 8;");
                }
            }
        });
        listViewFichiers.setOnDragOver(event -> {
            if (event.getDragboard().hasFiles()) {
                event.acceptTransferModes(javafx.scene.input.TransferMode.COPY);
            }
            event.consume();
        });

        listViewFichiers.setOnDragDropped(event -> {

            var files = event.getDragboard().getFiles();

            if (files != null && !files.isEmpty()) {
                ajouterFichiers(files);
            }

            event.setDropCompleted(true);
            event.consume();
        });
    }

    /**
     * Ajouter des fichiers individuels
     */
    @FXML
    private void handleAjouterFichiers(ActionEvent event) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner les copies scannées");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Images et PDF", "*.pdf", "*.png", "*.jpg", "*.jpeg"),
                new FileChooser.ExtensionFilter("PDF seulement", "*.pdf"),
                new FileChooser.ExtensionFilter("Images seulement", "*.png", "*.jpg", "*.jpeg")
        );

        Stage stage = (Stage) btnLancerCorrection.getScene().getWindow();
        List<File> fichiers = fileChooser.showOpenMultipleDialog(stage);

        if (fichiers != null && !fichiers.isEmpty()) {
            ajouterFichiers(fichiers);
        }
    }

    /**
     * Ajouter tous les fichiers d'un dossier
     */
    @FXML
    private void handleAjouterDossier(ActionEvent event) {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Sélectionner le dossier contenant les copies");

        Stage stage = (Stage) btnLancerCorrection.getScene().getWindow();
        File dossier = dirChooser.showDialog(stage);

        if (dossier != null && dossier.isDirectory()) {
            File[] fichiers = dossier.listFiles((dir, name) -> {
                String lower = name.toLowerCase();
                return lower.endsWith(".pdf") || lower.endsWith(".png") ||
                        lower.endsWith(".jpg") || lower.endsWith(".jpeg");
            });

            if (fichiers != null && fichiers.length > 0) {
                ajouterFichiers(Arrays.asList(fichiers));
            } else {
                showAlert("Aucun fichier trouvé",
                        "Le dossier ne contient aucun fichier PDF ou image.",
                        Alert.AlertType.WARNING);
            }
        }
    }

    /**
     * Ajouter des fichiers à la liste
     */
    private void ajouterFichiers(List<File> nouveauxFichiers) {
        int ajouts = 0;

        for (File fichier : nouveauxFichiers) {
            if (!fichiersCopies.contains(fichier)) {
                fichiersCopies.add(fichier);
                ajouts++;
            }
        }

        mettreAJourAffichage();

        if (ajouts > 0) {
            System.out.println("✅ " + ajouts + " fichier(s) ajouté(s)");
        }
    }

    /**
     * Effacer tous les fichiers
     */
    @FXML
    private void handleEffacerTout(ActionEvent event) {
        if (fichiersCopies.isEmpty()) return;

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer");
        confirm.setHeaderText("Effacer tous les fichiers ?");
        confirm.setContentText("Cette action supprimera " + fichiersCopies.size() + " fichier(s) de la liste.");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                fichiersCopies.clear();
                mettreAJourAffichage();
            }
        });
    }

    /**
     * Mettre à jour l'affichage de la liste
     */
    private void mettreAJourAffichage() {
        ObservableList<String> nomsAffichage = FXCollections.observableArrayList();

        for (int i = 0; i < fichiersCopies.size(); i++) {
            File f = fichiersCopies.get(i);
            String type = f.getName().toLowerCase().endsWith(".pdf") ? "📄 PDF" : "🖼️ IMG";
            nomsAffichage.add((i + 1) + ". " + type + " - " + f.getName());
        }

        listViewFichiers.setItems(nomsAffichage);
        lblNbFichiers.setText(fichiersCopies.size() + " copie(s)");

        // Activer le bouton si au moins 1 fichier
        btnLancerCorrection.setDisable(fichiersCopies.isEmpty());
    }

    /**
     * Lancer la correction OCR
     */
    @FXML
    private void handleLancerCorrection(ActionEvent event) {
        if (fichiersCopies.isEmpty()) {
            showAlert("Aucune copie", "Veuillez d'abord ajouter des copies scannées.", Alert.AlertType.WARNING);
            return;
        }

        System.out.println("🚀 Lancement correction OCR sur " + fichiersCopies.size() + " copie(s)");

        try {
            // ✅ Charger les questions ouvertes depuis la BDD
            List<CorrectionOCRController.QuestionOuverteInfo> questions = chargerQuestionsOuvertes();

            if (questions.isEmpty()) {
                showAlert("Erreur", "Aucune question ouverte trouvée dans cet examen.", Alert.AlertType.ERROR);
                return;
            }

            // ✅ Ouvrir l'interface de correction
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/project7/correction/CorrectionOCRView.fxml")
            );
            Parent root = loader.load();

            CorrectionOCRController controller = loader.getController();
            controller.setData(new ArrayList<>(fichiersCopies), questions);

            Stage stage = new Stage();
            stage.setTitle("Correction OCR");
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.show();

            // Fermer le dialogue d'upload
            Stage currentStage = (Stage) btnLancerCorrection.getScene().getWindow();
            currentStage.close();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de lancer la correction : " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    /**
     * Charger les questions ouvertes depuis la BDD
     */
    private List<CorrectionOCRController.QuestionOuverteInfo> chargerQuestionsOuvertes() {
        List<CorrectionOCRController.QuestionOuverteInfo> questions = new ArrayList<>();

        String query = "SELECT ql.question, ql.scoreTotal " +
                "FROM QuestionLibre ql " +
                "JOIN Section s ON s.idSection = ql.sectionID " +
                "WHERE s.controleID = ? " +
                "ORDER BY s.ordreSection";

        try (Connection conn = SqlConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, controle.getIdControle());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                String question = rs.getString("question");
                float scoreTotal = rs.getFloat("scoreTotal");
                questions.add(new CorrectionOCRController.QuestionOuverteInfo(question, scoreTotal));
            }

            System.out.println("✅ " + questions.size() + " question(s) ouverte(s) chargée(s)");

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur SQL", "Impossible de charger les questions : " + e.getMessage(), Alert.AlertType.ERROR);
        }

        return questions;
    }

    /**
     * Annuler et fermer
     */
    @FXML
    private void handleAnnuler(ActionEvent event) {
        Stage stage = (Stage) btnLancerCorrection.getScene().getWindow();
        stage.close();
    }

    /**
     * Afficher une alerte
     */
    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
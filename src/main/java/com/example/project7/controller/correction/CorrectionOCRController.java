package com.example.project7.controller.correction;

import com.example.project7.utils.OCRExtractor;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.scene.transform.Rotate;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.ResourceBundle;

public class CorrectionOCRController implements Initializable {

    @FXML
    private Label lblProgress;

    @FXML
    private ProgressBar progressBar;

    @FXML
    private ImageView imageReponse;

    @FXML
    private Label lblQuestion;

    @FXML
    private TextArea txtTexteOCR;

    @FXML
    private Label lblConfiance;

    @FXML
    private Label lblNoteMax;

    @FXML
    private Label lblNoteSuggeree;

    @FXML
    private Spinner<Double> spinnerNoteFinale;

    @FXML
    private TextArea txtCommentaire;

    private List<File> fichiersCopies;
    private List<QuestionOuverteInfo> questionsOuvertes;
    private int indexCopieActuelle = 0;
    private int indexQuestionActuelle = 0;
    private OCRExtractor ocrExtractor;

    private double zoomLevel = 1.0;
    private double rotationAngle = 0;

    /**
     * Classe interne pour stocker les infos d'une question ouverte
     */
    public static class QuestionOuverteInfo {
        public String question;
        public float scoreMax;

        public QuestionOuverteInfo(String question, float scoreMax) {
            this.question = question;
            this.scoreMax = scoreMax;
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        ocrExtractor = new OCRExtractor();

        // Configuration du Spinner
        SpinnerValueFactory<Double> valueFactory =
                new SpinnerValueFactory.DoubleSpinnerValueFactory(0.0, 20.0, 0.0, 0.5);
        spinnerNoteFinale.setValueFactory(valueFactory);
        spinnerNoteFinale.setEditable(true);
    }

    /**
     * Initialiser avec les données
     */
    public void setData(List<File> fichiers, List<QuestionOuverteInfo> questions) {
        this.fichiersCopies = fichiers;
        this.questionsOuvertes = questions;

        chargerCopieActuelle();
    }

    /**
     * Charger la copie actuelle
     */
    private void chargerCopieActuelle() {
        if (indexCopieActuelle >= fichiersCopies.size()) {
            terminerCorrection();
            return;
        }

        File fichier = fichiersCopies.get(indexCopieActuelle);
        QuestionOuverteInfo question = questionsOuvertes.get(indexQuestionActuelle);

        // Mise à jour de la progression
        int total = fichiersCopies.size() * questionsOuvertes.size();
        int current = (indexCopieActuelle * questionsOuvertes.size()) + indexQuestionActuelle + 1;

        lblProgress.setText(current + " / " + total);
        progressBar.setProgress((double) current / total);

        // Afficher la question
        lblQuestion.setText(question.question);
        lblNoteMax.setText(String.format("%.1f", question.scoreMax));

        // Charger l'image
        try {

            if (fichier.getName().toLowerCase().endsWith(".pdf")) {
                try (PDDocument doc = PDDocument.load(fichier)) {
                    PDFRenderer renderer = new PDFRenderer(doc);
                    BufferedImage buffered = renderer.renderImageWithDPI(0, 150); // 150 DPI suffit pour affichage

                    // Convertir BufferedImage → JavaFX Image
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ImageIO.write(buffered, "png", baos);
                    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

                    imageReponse.setImage(new Image(bais));
                    imageReponse.setFitWidth(500);
                    imageReponse.setPreserveRatio(true);
                }
            } else {

                Image image = new Image(new FileInputStream(fichier));
                imageReponse.setImage(image);
                imageReponse.setFitWidth(500);
            }

            zoomLevel = 1.0;
            rotationAngle = 0;

            System.out.println("📄 Chargement : " + fichier.getName());

            lancerOCR(fichier);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger l'image : " + e.getMessage());
        }
    }

    /**
     * Lancer l'OCR sur le fichier
     */
    private void lancerOCR(File fichier) {

        txtTexteOCR.setText("⏳ Extraction du texte en cours...");
        lblConfiance.setText("...");

        new Thread(() -> {
            try {

                String texte;

                // Détecter si c'est un PDF ou une image
                if (fichier.getName().toLowerCase().endsWith(".pdf")) {
                    texte = ocrExtractor.extraireTextePDF(fichier);
                } else {
                    texte = ocrExtractor.extraireTexteImage(fichier);
                }

                // Extraire seulement la réponse
                String reponse = extraireReponse(texte);

                float noteMax = questionsOuvertes.get(indexQuestionActuelle).scoreMax;
                double noteSuggere = calculerNoteSuggeree(reponse, noteMax);

                javafx.application.Platform.runLater(() -> {

                    txtTexteOCR.setText(reponse);

                    lblNoteSuggeree.setText(String.format("%.1f", noteSuggere));
                    spinnerNoteFinale.getValueFactory().setValue(noteSuggere);

                    int confiance = Math.min(95, 50 + reponse.length() / 10);

                    lblConfiance.setText(confiance + "%");

                    lblConfiance.setStyle(confiance > 70 ?
                            "-fx-text-fill:#4CAF50; -fx-font-weight:bold;" :
                            "-fx-text-fill:#FF9800; -fx-font-weight:bold;");
                });

            } catch (Exception e) {

                e.printStackTrace();

                javafx.application.Platform.runLater(() -> {
                    txtTexteOCR.setText("❌ Erreur OCR : " + e.getMessage());
                    lblConfiance.setText("0%");
                    lblConfiance.setStyle("-fx-text-fill:#F44336; -fx-font-weight:bold;");
                });
            }

        }).start();
    }

    /**
     * Calculer une note suggérée basique
     * (À améliorer avec analyse de mots-clés plus tard)
     */
    private double calculerNoteSuggeree(String texte, float noteMax) {
        if (texte == null || texte.trim().isEmpty()) {
            return 0.0;
        }

        // Critères simples pour l'instant
        int longueur = texte.length();
        int nbMots = texte.split("\\s+").length;

        double score = 0.0;

        // Points pour la longueur
        if (longueur > 50) score += 0.3;
        if (longueur > 100) score += 0.2;

        // Points pour le nombre de mots
        if (nbMots > 10) score += 0.3;
        if (nbMots > 20) score += 0.2;

        return Math.min(noteMax, score * noteMax);
    }
    private String extraireReponse(String texte) {
        if (texte == null || texte.isBlank()) return "";

        String lower = texte.toLowerCase();

        // ✅ Stratégie 1 : chercher après le texte de la question actuelle
        String questionActuelle = questionsOuvertes.get(indexQuestionActuelle).question.toLowerCase();
        int idxQuestion = lower.lastIndexOf(questionActuelle.substring(0,
                Math.min(10, questionActuelle.length())));
        if (idxQuestion != -1) {
            String apres = texte.substring(idxQuestion + questionActuelle.length()).trim();
            if (!apres.isBlank()) {
                System.out.println("✅ Réponse trouvée après la question: [" + apres + "]");
                return apres;
            }
        }

        // ✅ Stratégie 2 : chercher après "quel est ?" ou tout autre marqueur de question
        String[] marqueurs = {"? _", "?\n", "? —", "?_"};
        for (String m : marqueurs) {
            int idx = lower.lastIndexOf(m);
            if (idx != -1) {
                String apres = texte.substring(idx + m.length()).trim();
                if (!apres.isBlank()) {
                    System.out.println("✅ Réponse trouvée après marqueur '" + m + "': [" + apres + "]");
                    return apres;
                }
            }
        }

        // ✅ Stratégie 3 : prendre les 3 derniers tokens non vides après "Question X"
        int idxQ = lower.lastIndexOf("question ");
        if (idxQ != -1) {
            // Sauter "Question 1 quel est ?" → prendre ce qui suit
            int finLigne = texte.indexOf('\n', idxQ);
            if (finLigne == -1) finLigne = texte.length();
            // La réponse est après cette ligne
            String apres = texte.substring(finLigne).trim();
            if (!apres.isBlank()) {
                System.out.println("✅ Réponse après Question: [" + apres + "]");
                return apres;
            }
        }

        System.out.println("⚠️ Fallback texte complet");
        return texte.trim();
    }
    /**
     * Relancer l'OCR
     */
    @FXML
    private void handleRelancerOCR(ActionEvent event) {
        File fichier = fichiersCopies.get(indexCopieActuelle);
        lancerOCR(fichier);
    }

    /**
     * Zoom +
     */
    @FXML
    private void handleZoomIn(ActionEvent event) {
        zoomLevel += 0.2;
        imageReponse.setScaleX(zoomLevel);
        imageReponse.setScaleY(zoomLevel);
    }

    /**
     * Zoom -
     */
    @FXML
    private void handleZoomOut(ActionEvent event) {
        zoomLevel = Math.max(0.5, zoomLevel - 0.2);
        imageReponse.setScaleX(zoomLevel);
        imageReponse.setScaleY(zoomLevel);
    }

    /**
     * Pivoter l'image
     */
    @FXML
    private void handleRotate(ActionEvent event) {
        rotationAngle += 90;
        if (rotationAngle >= 360) rotationAngle = 0;
        imageReponse.setRotate(rotationAngle);
    }

    /**
     * Passer sans noter
     */
    @FXML
    private void handlePasser(ActionEvent event) {
        System.out.println("⏭ Copie passée sans notation");
        passerALaSuivante();
    }

    /**
     * Valider la note
     */
    @FXML
    private void handleValider(ActionEvent event) {
        double noteFinale = spinnerNoteFinale.getValue();
        String commentaire = txtCommentaire.getText().trim();

        System.out.println("✅ Note validée : " + noteFinale);
        System.out.println("💬 Commentaire : " + (commentaire.isEmpty() ? "(aucun)" : commentaire));

        // TODO: Sauvegarder dans la base de données

        passerALaSuivante();
    }

    /**
     * Passer à la copie/question suivante
     */
    private void passerALaSuivante() {
        indexQuestionActuelle++;

        // Si toutes les questions de cette copie sont traitées
        if (indexQuestionActuelle >= questionsOuvertes.size()) {
            indexQuestionActuelle = 0;
            indexCopieActuelle++;
        }

        // Réinitialiser le commentaire
        txtCommentaire.clear();

        // Charger la suivante
        chargerCopieActuelle();
    }

    /**
     * Terminer la correction
     */
    private void terminerCorrection() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("✅ Correction terminée");
        alert.setHeaderText("Toutes les copies ont été corrigées !");
        alert.setContentText("Félicitations ! Vous avez terminé la correction de toutes les copies.");
        alert.showAndWait();

        // Fermer la fenêtre
        Stage stage = (Stage) lblProgress.getScene().getWindow();
        stage.close();
    }

    /**
     * Afficher une alerte
     */
    private void showAlert(String title, String content) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
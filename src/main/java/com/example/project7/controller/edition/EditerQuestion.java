package com.example.project7.controller.edition;

import com.example.project7.model.Exercice;
import com.example.project7.model.QuestionLibre;
import com.example.project7.model.Section;
import com.jfoenix.controls.JFXButton;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;
import sql_connection.SqlConnection;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;
import java.util.ResourceBundle;

public class EditerQuestion implements Initializable {

    @FXML
    private TextArea enonceQuestion;

    @FXML
    private TextField scoringTotale;

    @FXML
    private TextField nombreScore;

    @FXML
    private TextField nombreLignes;

    @FXML
    private TextField tailleLigne;

    @FXML
    private TextField rappelQuestion;

    @FXML
    private Button ajouterQuestion;

    @FXML
    private Button cancelQuestion;

    // ✅ NOUVEAUX ÉLÉMENTS POUR EXERCICES
    @FXML
    private ComboBox<Exercice> comboExercice;

    @FXML
    private JFXButton btnNouvelExercice;

    @FXML
    private JFXButton btnModifierExercice;

    private Section section;
    private String rappel;
    private QuestionLibre questionLibre;
    private ObservableList<Exercice> listeExercices;

    public void setSection(Section identifierSection) {
        this.section = identifierSection;
        loadExercicesForControle();
    }

    // ========== NOUVELLES MÉTHODES POUR EXERCICES ==========

    private void loadExercicesForControle() {
        if (section == null || section.getDevoir() == null) return;

        String query = "SELECT * FROM Exercice WHERE controleID = ? ORDER BY numero";
        listeExercices.clear();

        try (Connection connection = SqlConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setInt(1, section.getDevoir().getIdControle());
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                Exercice ex = new Exercice(
                        rs.getInt("idExercice"),
                        rs.getInt("numero"),
                        rs.getString("titre"),
                        rs.getString("consigne"),
                        rs.getDouble("bareme"),
                        rs.getInt("controleID")
                );
                listeExercices.add(ex);
            }

            if (!listeExercices.isEmpty()) {
                comboExercice.setValue(listeExercices.get(listeExercices.size() - 1));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleNouvelExercice() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/project7/editer_quiz/_8_EditerExercice.fxml")
            );
            Parent root = loader.load();
            EditerExercice controller = loader.getController();

            controller.setControleID(section.getDevoir().getIdControle());
            controller.setNumeroSuggere(listeExercices.size() + 1);

            Stage stage = new Stage();
            stage.setTitle("Nouvel Exercice");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);

            Stage parentStage = (Stage) comboExercice.getScene().getWindow();
            stage.initOwner(parentStage);

            stage.showAndWait();

            if (controller.isValidated()) {
                Exercice nouvelExercice = controller.getExercice();
                listeExercices.add(nouvelExercice);
                comboExercice.setValue(nouvelExercice);

                showInfoMessage("Exercice créé",
                        "L'exercice \"" + nouvelExercice.getTitre() + "\" a été ajouté avec succès !");
            }
        } catch (IOException e) {
            e.printStackTrace();
            showErrorMessage("Erreur", "Impossible d'ouvrir l'éditeur d'exercice : " + e.getMessage());
        }
    }

    @FXML
    private void handleModifierExercice() {
        Exercice exerciceSelectionne = comboExercice.getValue();

        if (exerciceSelectionne == null) {
            showErrorMessage("Erreur", "Veuillez d'abord sélectionner un exercice à modifier.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/example/project7/editer_quiz/_8_EditerExercice.fxml")
            );
            Parent root = loader.load();
            EditerExercice controller = loader.getController();

            controller.setExercice(exerciceSelectionne);
            controller.setControleID(section.getDevoir().getIdControle());

            Stage stage = new Stage();
            stage.setTitle("Modifier l'Exercice");
            stage.setScene(new Scene(root));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setResizable(false);

            Stage parentStage = (Stage) comboExercice.getScene().getWindow();
            stage.initOwner(parentStage);

            stage.showAndWait();

            if (controller.isValidated()) {
                comboExercice.setItems(null);
                comboExercice.setItems(listeExercices);
                comboExercice.setValue(exerciceSelectionne);

                showInfoMessage("Exercice modifié",
                        "Les modifications ont été enregistrées avec succès !");
            }
        } catch (IOException e) {
            e.printStackTrace();
            showErrorMessage("Erreur", "Impossible d'ouvrir l'éditeur d'exercice : " + e.getMessage());
        }
    }

    private void showInfoMessage(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showErrorMessage(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ========== FIN NOUVELLES MÉTHODES ==========

    @FXML
    public void cancelQuestion(ActionEvent event) {
        EditerSection.cancelSection();
        Stage stage = (Stage) cancelQuestion.getScene().getWindow();
        stage.close();
    }

    private void verifyScoringTotal() {
        String element = scoringTotale.getText().trim();

        if (element.isEmpty()) {
            if (!scoringTotale.getStyleClass().contains("text-field-danger")) {
                scoringTotale.getStyleClass().add("text-field-danger");
                this.scoringTotale.setText("1");
            }
        } else {
            scoringTotale.getStyleClass().removeAll("text-field-danger");
        }
    }

    private void verifyNombreScore() {
        String element = nombreScore.getText().trim();

        if (element.isEmpty()) {
            if (!nombreScore.getStyleClass().contains("text-field-danger")) {
                nombreScore.getStyleClass().add("text-field-danger");
                this.nombreScore.setText("2");
            }
        } else {
            nombreScore.getStyleClass().removeAll("text-field-danger");
        }
    }

    private void verifyNombreLignes() {
        String element = nombreLignes.getText().trim();

        if (element.isEmpty()) {
            if (!nombreLignes.getStyleClass().contains("text-field-danger")) {
                nombreLignes.getStyleClass().add("text-field-danger");
                this.nombreLignes.setText("3");
            }
        } else {
            nombreLignes.getStyleClass().removeAll("text-field-danger");
        }
    }

    private void verifyTailleLigne() {
        String element = tailleLigne.getText().trim();

        if (element.isEmpty()) {
            if (!tailleLigne.getStyleClass().contains("text-field-danger")) {
                tailleLigne.getStyleClass().add("text-field-danger");
                this.tailleLigne.setText("0.5");
            }
        } else {
            tailleLigne.getStyleClass().removeAll("text-field-danger");
        }
    }

    private boolean verifyQuestion() {
        String question = enonceQuestion.getText().trim();

        if (question.isEmpty()) {
            if (!enonceQuestion.getStyleClass().contains("text-field-danger")) {
                enonceQuestion.getStyleClass().add("text-field-danger");
            }
            return false;
        } else {
            enonceQuestion.getStyleClass().removeAll("text-field-danger");
            return true;
        }
    }

    private void verifyRappel() {
        String question = rappelQuestion.getText().trim();

        if (question.isEmpty()) {
            if (!rappelQuestion.getStyleClass().contains("text-field-danger")) {
                rappelQuestion.getStyleClass().add("text-field-danger");
                this.rappelQuestion.setText(this.rappel);
            }
        } else {
            rappelQuestion.getStyleClass().removeAll("text-field-danger");
        }
    }

    @FXML
    public void ajouterQuestion(ActionEvent event) {
        // ✅ VALIDATION EXERCICE EN PREMIER
        Exercice exerciceSelectionne = comboExercice.getValue();
        if (exerciceSelectionne == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("❌ Exercice obligatoire");
            alert.setHeaderText("Vous devez associer cette question à un exercice");
            alert.setContentText("Veuillez sélectionner un exercice dans la liste ou cliquer sur \"+ New Exercise\" pour en créer un.");
            comboExercice.setStyle("-fx-border-color: red; -fx-border-width: 2px;");
            alert.showAndWait();
            return;
        }

        comboExercice.setStyle("");

        verifyRappel();
        verifyQuestion();
        verifyTailleLigne();
        verifyNombreLignes();
        verifyNombreScore();
        verifyScoringTotal();

        if (verifyQuestion()) {
            if (checkSectionExists(this.section.getIdSection())) {
                Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
                confirmationAlert.setTitle("Section Exists");
                confirmationAlert.setHeaderText("This section already exists");
                confirmationAlert.setContentText("Section with the identifier " + this.section.getIdSection() + " already exists, Would you like to overwrite it?");

                ButtonType modifyButton = new ButtonType("Modify");
                ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

                confirmationAlert.getButtonTypes().setAll(modifyButton, cancelButton);

                confirmationAlert.showAndWait().ifPresent(response -> {
                    if (response == modifyButton) {
                        updateSection(exerciceSelectionne);
                    }
                });
            } else {
                questionLibre.setRappel(this.rappelQuestion.getText().trim());
                questionLibre.setQuestion(this.enonceQuestion.getText().trim());
                questionLibre.setTailleLigne(Float.parseFloat(this.tailleLigne.getText().trim()));
                questionLibre.setNombreLigne(Integer.parseInt(this.nombreLignes.getText().trim()));
                questionLibre.setNombreScore(Integer.parseInt(this.nombreScore.getText().trim()));
                questionLibre.setScoreTotal(Float.parseFloat(scoringTotale.getText().trim()));

                createSection(exerciceSelectionne);
                createQuestion();
                this.section.getDevoir().getController().fetchAndUpdateTableView();

                // Message de succès
                Alert success = new Alert(Alert.AlertType.INFORMATION);
                success.setTitle("✅ Question ajoutée");
                success.setHeaderText("La question a été ajoutée avec succès");
                success.setContentText("Question ajoutée à l'exercice \"" + exerciceSelectionne.getTitre() + "\"");
                success.show();

                Stage stage = (Stage) cancelQuestion.getScene().getWindow();
                stage.close();
            }
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Random random = new Random();
        int x = random.nextInt(999999999);
        this.rappel = "QuestionN°" + String.valueOf(x);
        this.rappelQuestion.setText(this.rappel);
        this.scoringTotale.setText("1");
        this.nombreScore.setText("2");
        this.nombreLignes.setText("3");
        this.tailleLigne.setText("0.5");
        this.questionLibre = new QuestionLibre();
        enonceQuestion.setWrapText(true);

        // ✅ INITIALISER COMBOBOX EXERCICES
        listeExercices = FXCollections.observableArrayList();
        comboExercice.setItems(listeExercices);

        comboExercice.setConverter(new StringConverter<Exercice>() {
            @Override
            public String toString(Exercice ex) {
                return ex != null ? ex.toString() : "";
            }

            @Override
            public Exercice fromString(String string) {
                return null;
            }
        });

        comboExercice.valueProperty().addListener((obs, oldVal, newVal) -> {
            btnModifierExercice.setVisible(newVal != null);
            if (newVal != null) {
                comboExercice.setStyle("");
            }
        });

        btnModifierExercice.setVisible(false);
        comboExercice.setPromptText("⚠️ OBLIGATOIRE : Sélectionner un exercice");
        comboExercice.setStyle("-fx-prompt-text-fill: #d32f2f; -fx-font-weight: bold;");
    }

    private boolean checkSectionExists(String idSection) {
        String checkQuery = "SELECT COUNT(*) FROM Section WHERE idSection = ?";

        try (Connection connection = SqlConnection.getConnection();
             PreparedStatement checkStatement = connection.prepareStatement(checkQuery)) {

            checkStatement.setString(1, idSection);
            ResultSet resultSet = checkStatement.executeQuery();

            if (resultSet.next()) {
                return resultSet.getInt(1) > 0;
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return false;
    }

    private void updateQuestion() {
        String updateQuery = "UPDATE QuestionLibre SET question = ?, scoreTotal = ?, nombreScore = ?, nombreLigne = ?, tailleLigne = ?, rappel = ? WHERE sectionID = ?";

        try (Connection connection = SqlConnection.getConnection();
             PreparedStatement updateStmt = connection.prepareStatement(updateQuery)) {

            updateStmt.setString(1, enonceQuestion.getText().trim());
            updateStmt.setFloat(2, Float.parseFloat(scoringTotale.getText().trim()));
            updateStmt.setInt(3, Integer.parseInt(nombreScore.getText().trim()));
            updateStmt.setInt(4, Integer.parseInt(nombreLignes.getText().trim()));
            updateStmt.setFloat(5, Float.parseFloat(tailleLigne.getText().trim()));
            updateStmt.setString(6, rappelQuestion.getText().trim());
            updateStmt.setString(7, section.getIdSection());

            int rowsAffected = updateStmt.executeUpdate();
            if (rowsAffected <= 0) {
                System.err.println("No update for this Section");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateSectionExerciceLink(Exercice exercice) {
        String updateQuery = "UPDATE Section SET exerciceID = ? WHERE idSection = ?";

        try (Connection connection = SqlConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(updateQuery)) {

            stmt.setInt(1, exercice.getIdExercice());
            stmt.setString(2, section.getIdSection());
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateSection(Exercice exerciceSelectionne) {
        updateSectionExerciceLink(exerciceSelectionne);
        updateQuestion();
        this.section.getDevoir().getController().fetchAndUpdateTableView();
        Stage stage = (Stage) cancelQuestion.getScene().getWindow();
        stage.close();
    }

    @FXML
    public void handleInputNumber(KeyEvent event) {
        TextField textField = (TextField) event.getSource();
        String currentText = textField.getText();

        String sanitizedText = currentText.replaceAll("[^\\d]", "");

        textField.setText(sanitizedText);
        textField.positionCaret(sanitizedText.length());
    }

    @FXML
    public void handleInputFloat(KeyEvent event) {
        TextField textField = (TextField) event.getSource();
        String currentText = textField.getText();

        String sanitizedText = currentText.replaceAll("[^\\d.]", "");

        int firstDotIndex = sanitizedText.indexOf(".");
        if (firstDotIndex != -1) {
            sanitizedText = sanitizedText.substring(0, firstDotIndex + 1)
                    + sanitizedText.substring(firstDotIndex + 1).replaceAll("\\.", "");
        }

        textField.setText(sanitizedText);
        textField.positionCaret(sanitizedText.length());
    }

    private void createSection(Exercice exerciceSelectionne) {
        String insertSectionQuery = "INSERT INTO Section (idSection, ordreSection, controleID, exerciceID) VALUES (?, ?, ?, ?)";

        try (Connection connection = SqlConnection.getConnection();
             PreparedStatement insertStatement = connection.prepareStatement(insertSectionQuery)) {

            insertStatement.setString(1, this.section.getIdSection());
            insertStatement.setInt(2, this.section.getOrdreSection());
            insertStatement.setInt(3, this.section.getDevoir().getIdControle());
            insertStatement.setInt(4, exerciceSelectionne.getIdExercice());

            insertStatement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createQuestion() {
        String insertQCUQuery = "INSERT INTO QuestionLibre (question, scoreTotal,nombreScore, nombreLigne, tailleLigne, rappel, sectionID) VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (Connection connection = SqlConnection.getConnection();
             PreparedStatement insertStatement = connection.prepareStatement(insertQCUQuery, PreparedStatement.RETURN_GENERATED_KEYS)) {

            insertStatement.setString(1, questionLibre.getQuestion());
            insertStatement.setFloat(2, questionLibre.getScoreTotal());
            insertStatement.setInt(3, questionLibre.getNombreScore());
            insertStatement.setInt(4, questionLibre.getNombreLigne());
            insertStatement.setFloat(5, questionLibre.getTailleLigne());
            insertStatement.setString(6, questionLibre.getRappel());
            insertStatement.setString(7, this.section.getIdSection());

            int rowsAffected = insertStatement.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = insertStatement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        String idQCU = String.valueOf(generatedKeys.getInt(1));
                        questionLibre.setIdSection(idQCU);
                    }
                }
            } else {
                System.err.println("Failed to insert QuestionLibre data.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void setSectionUpdating(Section section) {
        this.section = section;
        this.enonceQuestion.setText(section.getIdSection());
        loadExercicesForControle();
        loadQuestionFromSectionId(section.getIdSection());
    }

    private void loadQuestionFromSectionId(String idSection) {
        String fetchQCUQuery = "SELECT * FROM QuestionLibre WHERE sectionID = ?";

        try (Connection connection = SqlConnection.getConnection();
             PreparedStatement qcuStatement = connection.prepareStatement(fetchQCUQuery)){

            qcuStatement.setString(1, idSection);
            ResultSet qcuResultSet = qcuStatement.executeQuery();
            if (qcuResultSet.next()) {
                questionLibre = new QuestionLibre();
                questionLibre.setIdSection(qcuResultSet.getString("sectionID"));
                questionLibre.setQuestion(qcuResultSet.getString("question"));
                questionLibre.setRappel(qcuResultSet.getString("rappel"));
                questionLibre.setTailleLigne(qcuResultSet.getFloat("tailleLigne"));
                questionLibre.setNombreLigne(qcuResultSet.getInt("nombreLigne"));
                questionLibre.setNombreScore(qcuResultSet.getInt("nombreScore"));
                questionLibre.setScoreTotal(qcuResultSet.getFloat("scoreTotal"));

                enonceQuestion.setText(questionLibre.getQuestion());
                rappelQuestion.setText(questionLibre.getRappel());
                tailleLigne.setText(String.valueOf(questionLibre.getTailleLigne()));
                nombreLignes.setText(String.valueOf(questionLibre.getNombreLigne()));
                nombreScore.setText(String.valueOf(questionLibre.getNombreScore()));
                scoringTotale.setText(String.valueOf(questionLibre.getScoreTotal()));
            }

            // Charger l'exercice associé
            loadExerciceForSection(idSection);

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void loadExerciceForSection(String idSection) {
        String query = "SELECT e.* FROM Exercice e " +
                "INNER JOIN Section s ON s.exerciceID = e.idExercice " +
                "WHERE s.idSection = ?";

        try (Connection connection = SqlConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(query)) {

            stmt.setString(1, idSection);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Exercice ex = new Exercice(
                        rs.getInt("idExercice"),
                        rs.getInt("numero"),
                        rs.getString("titre"),
                        rs.getString("consigne"),
                        rs.getDouble("bareme"),
                        rs.getInt("controleID")
                );

                for (Exercice exercice : listeExercices) {
                    if (exercice.getIdExercice() == ex.getIdExercice()) {
                        comboExercice.setValue(exercice);
                        break;
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void modfiyQuestion(ActionEvent events) {
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.initStyle(StageStyle.UTILITY);

        VBox popupVBox = new VBox(10);
        popupVBox.setPadding(new Insets(20));

        TextArea responseTextArea = new TextArea(enonceQuestion.getText());
        responseTextArea.setWrapText(true);

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button saveButton = new Button("Modify");
        saveButton.setOnAction(event -> {
            enonceQuestion.setText(responseTextArea.getText());
            popupStage.close();
        });

        Button closeButton = new Button("Close");
        closeButton.setOnAction(event -> {
            popupStage.close();
        });

        buttonBox.getChildren().addAll(saveButton, closeButton);

        popupVBox.getChildren().addAll(responseTextArea, buttonBox);

        Scene popupScene = new Scene(popupVBox, 350, 250);
        popupStage.setScene(popupScene);
        popupStage.setTitle("Edit the Question");
        popupStage.show();
    }
}
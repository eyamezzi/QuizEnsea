package com.example.project7.controller.edition;

import com.example.project7.model.Exercice;
import com.example.project7.model.QCM;
import com.example.project7.model.Reponse;
import com.example.project7.model.Section;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
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
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;
import javafx.util.converter.IntegerStringConverter;
import sql_connection.SqlConnection;

import java.io.IOException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class EditerQCU implements Initializable {

    @FXML
    private TextArea enonceQuestion;

    @FXML
    private TextField baremePos;

    @FXML
    private TextArea reponseCorrect;

    @FXML
    private TextField baremeNegDefault;

    @FXML
    private TableView<Reponse> incorrectTableView;

    @FXML
    private TableColumn<Reponse, String> responseColumn;

    @FXML
    private TableColumn<Reponse, Integer> scoreColumn;

    @FXML
    private TableColumn<Reponse, Void> actionColumn;

    @FXML
    private Button cancelQcu;

    @FXML
    private CheckBox multiAnswerCheckbox;

    @FXML
    private Label iconMultiple;

    // ⭐ NOUVEAUX CHAMPS POUR LA GESTION DES EXERCICES ⭐
    @FXML
    private ComboBox<Exercice> comboExercice;

    @FXML
    private Button btnNouvelExercice;

    @FXML
    private Button btnModifierExercice;

    private Section section;
    private QCM qcu;
    private ObservableList<Exercice> listeExercices;

    public void setSection(Section identifierSection) {
        this.section = identifierSection;
        loadExercicesForControle();
    }

    @FXML
    private void handleMultipleAnswerToggle(ActionEvent event) {
        boolean multiple = multiAnswerCheckbox.isSelected();
        iconMultiple.setVisible(multiple);
    }

    private boolean verifyQuesstion() {
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

    private boolean verifyReponseCorrect() {
        String question = reponseCorrect.getText().trim();

        if (question.isEmpty()) {
            if (!reponseCorrect.getStyleClass().contains("text-field-danger")) {
                reponseCorrect.getStyleClass().add("text-field-danger");
            }
            return false;
        } else {
            reponseCorrect.getStyleClass().removeAll("text-field-danger");
            return true;
        }
    }

    @FXML
    public void handleInputNumber(KeyEvent event) {
        TextField textField = (TextField) event.getSource();
        String currentText = textField.getText();

        String sanitizedText = currentText.replaceAll("[^\\d-]", "");

        if (sanitizedText.length() > 1) {
            sanitizedText = sanitizedText.replaceAll("(?<!^)-", "");
        }

        if (sanitizedText.length() > 3) {
            sanitizedText = sanitizedText.substring(0, 3);
        }

        textField.setText(sanitizedText);
        textField.positionCaret(sanitizedText.length());
    }

    @FXML
    public void handleClicksAddWrongResponce(ActionEvent event) {
        String defaultResponse = "Wrong Answer";
        int defaultScore;
        try {
            defaultScore = Integer.parseInt(baremeNegDefault.getText());
        } catch (NumberFormatException e) {
            defaultScore = 0;
        }
        ObservableList<Reponse> items = incorrectTableView.getItems();
        items.add(new Reponse(defaultResponse, defaultScore));
    }

    @FXML
    public void handleClicksAddQCU(ActionEvent event) {
        // ⭐ VALIDATION COMPLÈTE ⭐

        // 1. Vérifier l'exercice
        Exercice exerciceSelectionne = comboExercice.getValue();
        if (exerciceSelectionne == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("❌ Exercice obligatoire");
            alert.setHeaderText("Vous devez associer cette question à un exercice");
            alert.setContentText("Veuillez sélectionner un exercice dans la liste ou cliquer sur \"+ Nouvel Exercice\" pour en créer un.");

            comboExercice.setStyle("-fx-border-color: red; -fx-border-width: 2px;");

            alert.showAndWait();
            return;
        }

        comboExercice.setStyle("");

        // 2. Vérifier la question
        if (!verifyQuesstion()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("❌ Question vide");
            alert.setHeaderText("Veuillez saisir l'énoncé de la question");
            alert.setContentText("Le champ \"The question\" ne peut pas être vide.");
            alert.showAndWait();
            return;
        }

        // 3. Vérifier la réponse correcte
        if (!verifyReponseCorrect()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("❌ Réponse correcte manquante");
            alert.setHeaderText("Veuillez saisir la réponse correcte");
            alert.setContentText("Le champ \"Correct answer\" ne peut pas être vide.");
            alert.showAndWait();
            return;
        }

        // ✅ Toutes les validations sont passées

        if (checkSectionExists(this.section.getIdSection())) {
            Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmationAlert.setTitle("Section Exists");
            confirmationAlert.setHeaderText("Section Already Exists");
            confirmationAlert.setContentText("Section with the identifier " + this.section.getIdSection() +
                    " already exists. Would you like to overwrite it?");

            ButtonType modifyButton = new ButtonType("Modify");
            ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

            confirmationAlert.getButtonTypes().setAll(modifyButton, cancelButton);

            confirmationAlert.showAndWait().ifPresent(response -> {
                if (response == modifyButton) {
                    updateSection(exerciceSelectionne);
                    rafraichirComboExercice();
                }
            });
        } else {
            createSection(exerciceSelectionne);
            createQCU();
            createQCUResponse();
            rafraichirComboExercice();
            this.section.getDevoir().getController().fetchAndUpdateTableView();
            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("✅ Question ajoutée");
            success.setHeaderText("La question a été ajoutée avec succès");
            success.setContentText("Question QCU ajoutée à l'exercice \"" + exerciceSelectionne.getTitre() + "\"");
            success.show();

            Stage stage = (Stage) cancelQcu.getScene().getWindow();
            stage.close();
        }
    }

    private void updateQCU() {
        String updateQcmQuery = "UPDATE QCM SET question = ? WHERE idQCM = ?";
        String deleteResponsesQuery = "DELETE FROM QCM_Reponses WHERE qcmID = ?";
        String insertResponseQuery = "INSERT INTO QCM_Reponses (qcmID, reponse, score, isCorrect) VALUES (?, ?, ?, ?)";

        try (Connection connection = SqlConnection.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement updateStmt = connection.prepareStatement(updateQcmQuery)) {
                updateStmt.setString(1, enonceQuestion.getText());
                updateStmt.setInt(2, Integer.parseInt(qcu.getIdSection()));
                updateStmt.executeUpdate();
            }

            try (PreparedStatement deleteStmt = connection.prepareStatement(deleteResponsesQuery)) {
                deleteStmt.setInt(1, Integer.parseInt(qcu.getIdSection()));
                deleteStmt.executeUpdate();
            }

            try (PreparedStatement insertStmt = connection.prepareStatement(insertResponseQuery)) {
                insertStmt.setInt(1, Integer.parseInt(qcu.getIdSection()));
                insertStmt.setString(2, reponseCorrect.getText());
                insertStmt.setInt(3, Integer.parseInt(baremePos.getText()));
                insertStmt.setBoolean(4, true);
                insertStmt.addBatch();

                for (Reponse response : incorrectTableView.getItems()) {
                    insertStmt.setInt(1, Integer.parseInt(qcu.getIdSection()));
                    insertStmt.setString(2, response.getResponse());
                    insertStmt.setInt(3, response.getScore());
                    insertStmt.setBoolean(4, false);
                    insertStmt.addBatch();
                }
                insertStmt.executeBatch();
            }

            connection.commit();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void updateSection(Exercice exerciceSelectionne) {
        updateSectionExerciceLink(exerciceSelectionne);
        updateQCU();
        this.section.getDevoir().getController().fetchAndUpdateTableView();
        Stage stage = (Stage) cancelQcu.getScene().getWindow();
        stage.close();
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

    private void createQCU() {
        String insertQCUQuery = "INSERT INTO QCM (question, isQCU, sectionID) VALUES (?, ?, ?)";

        try (Connection connection = SqlConnection.getConnection();
             PreparedStatement insertStatement = connection.prepareStatement(insertQCUQuery, PreparedStatement.RETURN_GENERATED_KEYS)) {

            insertStatement.setString(1, enonceQuestion.getText());
            insertStatement.setBoolean(2, true);
            insertStatement.setString(3, this.section.getIdSection());

            int rowsAffected = insertStatement.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = insertStatement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        String idQCU = String.valueOf(generatedKeys.getInt(1));
                        qcu = new QCM();
                        qcu.setQCU(true);
                        qcu.setIdSection(idQCU);
                    }
                }
            } else {
                System.err.println("Failed to insert QCU data.");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createQCUResponse() {
        createQCUCorrectResponse();
        createQCUInCorrectResponse();
    }

    private void createQCUInCorrectResponse() {
        String insertIncorrectResponseQuery = "INSERT INTO QCM_Reponses (qcmID, reponse, score, isCorrect) VALUES (?, ?, ?, ?)";

        try (Connection connection = SqlConnection.getConnection();
             PreparedStatement insertStatement = connection.prepareStatement(insertIncorrectResponseQuery)) {

            for (Reponse response : incorrectTableView.getItems()) {
                insertStatement.setInt(1, Integer.parseInt(qcu.getIdSection()));
                insertStatement.setString(2, response.getResponse());
                insertStatement.setInt(3, response.getScore());
                insertStatement.setBoolean(4, false);

                insertStatement.addBatch();
            }

            insertStatement.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void createQCUCorrectResponse() {
        String insertCorrectResponseQuery = "INSERT INTO QCM_Reponses (qcmID, reponse, score, isCorrect) VALUES (?, ?, ?, ?)";

        try (Connection connection = SqlConnection.getConnection();
             PreparedStatement insertStatement = connection.prepareStatement(insertCorrectResponseQuery)) {

            insertStatement.setInt(1, Integer.parseInt(qcu.getIdSection()));
            insertStatement.setString(2, reponseCorrect.getText());
            insertStatement.setInt(3, Integer.parseInt(baremePos.getText()));
            insertStatement.setBoolean(4, true);

            insertStatement.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    @FXML
    public void handleClicksCancelQCU(ActionEvent event) {
        EditerSection.cancelSection();
        Stage stage = (Stage) cancelQcu.getScene().getWindow();
        stage.close();
    }

    private void openEditPopup(Reponse reponse) {
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.initStyle(StageStyle.UTILITY);

        VBox popupVBox = new VBox(10);
        popupVBox.setPadding(new Insets(20));

        TextArea responseTextArea = new TextArea(reponse.getResponse());
        responseTextArea.setWrapText(true);
        responseTextArea.setPrefSize(300, 150);

        TextField scoreTextField = new TextField(String.valueOf(reponse.getScore()));
        scoreTextField.setPromptText("Enter the score");

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button saveButton = new Button("Save");
        saveButton.setOnAction(event -> {
            reponse.setResponse(responseTextArea.getText());
            reponse.setScore(Integer.parseInt(scoreTextField.getText()));

            incorrectTableView.refresh();

            popupStage.close();
        });

        Button closeButton = new Button("Close");
        closeButton.setOnAction(event -> {
            popupStage.close();
        });

        buttonBox.getChildren().addAll(saveButton, closeButton);

        popupVBox.getChildren().addAll(new Label("Modify the answer and the score :"), responseTextArea, scoreTextField, buttonBox);

        Scene popupScene = new Scene(popupVBox, 350, 250);
        popupStage.setScene(popupScene);
        popupStage.setTitle("Modify the answer");
        popupStage.setOnShown(event -> {
            responseTextArea.requestFocus();
            responseTextArea.selectAll();
        });
        popupStage.show();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        baremePos.setText("1");
        baremeNegDefault.setText("0");

        // ⭐ INITIALISATION DU COMBOBOX DES EXERCICES ⭐
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
            if (newVal != null) {
                comboExercice.setStyle("");
            }
            btnModifierExercice.setVisible(newVal != null);
        });

        btnModifierExercice.setVisible(false);
        comboExercice.setPromptText("⚠️ OBLIGATOIRE : Sélectionner un exercice");
        comboExercice.setStyle("-fx-prompt-text-fill: #d32f2f; -fx-font-weight: bold;");

        // Feedback visuel sur la question
        enonceQuestion.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty()) {
                enonceQuestion.getStyleClass().removeAll("text-field-danger");
            }
        });

        // Feedback visuel sur la réponse correcte
        reponseCorrect.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty()) {
                reponseCorrect.getStyleClass().removeAll("text-field-danger");
            }
        });

        // Configuration des colonnes
        responseColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getResponse()));
        responseColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        responseColumn.setOnEditCommit(event -> {
            Reponse reponse = event.getRowValue();
            reponse.setResponse(event.getNewValue());
        });

        scoreColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getScore()).asObject());
        scoreColumn.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        scoreColumn.setOnEditCommit(event -> {
            Reponse reponse = event.getRowValue();
            reponse.setScore(event.getNewValue());
        });

        actionColumn.setCellFactory(col -> new TableCell<>() {
            private final Button deleteButton = new Button("Delete");
            private final Button editButton = new Button("Modify");

            {
                deleteButton.setOnAction(event -> {
                    Reponse reponse = getTableView().getItems().get(getIndex());
                    getTableView().getItems().remove(reponse);
                });
                deleteButton.setStyle("-fx-background-color: red; -fx-text-fill: white;");

                editButton.setOnAction(event -> {
                    Reponse reponse = getTableView().getItems().get(getIndex());
                    openEditPopup(reponse);
                });
                editButton.setStyle("-fx-background-color: blue; -fx-text-fill: white;");
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttonBox = new HBox(10, editButton, deleteButton);
                    setGraphic(buttonBox);
                }
            }
        });

        incorrectTableView.setItems(FXCollections.observableArrayList());

        enonceQuestion.setWrapText(true);
        reponseCorrect.setWrapText(true);
    }

    // ============ MÉTHODES POUR LA GESTION DES EXERCICES ============

    /**
     * Charger les exercices du contrôle
     */
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

    /**
     * Créer un nouvel exercice
     */
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

    /**
     * Modifier un exercice existant
     */
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

    /**
     * Rafraîchir le ComboBox après modification
     */
    private void rafraichirComboExercice() {
        Exercice exerciceSelectionne = comboExercice.getValue();

        if (exerciceSelectionne != null) {
            exerciceSelectionne.invaliderCache();
            loadExercicesForControle();
            for (Exercice ex : listeExercices) {
                if (ex.getIdExercice() == exerciceSelectionne.getIdExercice()) {
                    comboExercice.setValue(ex);
                    break;
                }
            }
        }
    }
    /**
     * Afficher un message d'information
     */
    private void showInfoMessage(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    /**
     * Afficher un message d'erreur
     */
    private void showErrorMessage(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ============ CHARGEMENT DES DONNÉES ============

    public void setSectionUpdating(Section section) {
        this.section = section;
        this.enonceQuestion.setText(section.getIdSection());
        loadExercicesForControle();
        loadQCUFromSectionId(section.getIdSection());
    }

    private void loadQCUFromSectionId(String idSection) {
        String fetchQCUQuery = "SELECT * FROM QCM WHERE sectionID = ?";
        String fetchResponsesQuery = "SELECT * FROM QCM_Reponses WHERE qcmID = ?";

        try (Connection connection = SqlConnection.getConnection();
             PreparedStatement qcuStatement = connection.prepareStatement(fetchQCUQuery);
             PreparedStatement responseStatement = connection.prepareStatement(fetchResponsesQuery)) {

            qcuStatement.setString(1, idSection);
            ResultSet qcuResultSet = qcuStatement.executeQuery();
            if (qcuResultSet.next()) {
                qcu = new QCM();
                qcu.setIdSection(qcuResultSet.getString("idQCM"));
                qcu.setQuestion(qcuResultSet.getString("question"));
                qcu.setQCU(qcuResultSet.getBoolean("isQcu"));

                enonceQuestion.setText(qcu.getQuestion());
            }

            if (qcu != null) {
                ObservableList<Reponse> responses = FXCollections.observableArrayList();
                responseStatement.setInt(1, Integer.parseInt(qcu.getIdSection()));
                ResultSet responseResultSet = responseStatement.executeQuery();

                while (responseResultSet.next()) {
                    String responseText = responseResultSet.getString("reponse");
                    int score = responseResultSet.getInt("score");
                    boolean isCorrect = responseResultSet.getBoolean("isCorrect");

                    if (isCorrect) {
                        reponseCorrect.setText(responseText);
                        baremePos.setText(String.valueOf(score));
                    } else {
                        responses.add(new Reponse(responseText, score));
                    }
                }

                incorrectTableView.setItems(responses);
            }

            // ⭐ CHARGER L'EXERCICE ASSOCIÉ ⭐
            loadExerciceForSection(idSection);

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("Error loading QCU data: " + e.getMessage());
        }
    }

    /**
     * Charger l'exercice associé à une section
     */
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

                // Sélectionner l'exercice dans le ComboBox
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

    // ============ MÉTHODES D'ÉDITION ============

    public void modifyCorrectAnswer(ActionEvent events) {
        Stage popupStage = new Stage();
        popupStage.initModality(Modality.APPLICATION_MODAL);
        popupStage.initStyle(StageStyle.UTILITY);

        VBox popupVBox = new VBox(10);
        popupVBox.setPadding(new Insets(20));

        TextArea responseTextArea = new TextArea(reponseCorrect.getText());
        responseTextArea.setWrapText(true);

        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER);

        Button saveButton = new Button("Modify");
        saveButton.setOnAction(event -> {
            reponseCorrect.setText(responseTextArea.getText());
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
        popupStage.setTitle("Edit the correct response");
        popupStage.show();
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
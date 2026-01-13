package com.example.project7.controller.edition;

import com.example.project7.FxmlLoader;
import com.example.project7.model.*;
import com.jfoenix.controls.JFXButton;
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
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
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
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class EditerQCM implements Initializable {

    @FXML
    private TextArea enonceQuestion;

    @FXML
    private TextField baremePosDefault;

    @FXML
    private TextField baremeNegDefault;

    @FXML
    private TableView<Reponse> correctTableView;

    @FXML
    private TableColumn<Reponse, String> responsePosColumn;

    @FXML
    private TableColumn<Reponse, Integer> scorePosColumn;

    @FXML
    private TableColumn<Reponse, Void> actionPosColumn;

    @FXML
    private TableView<Reponse> incorrectTableView;

    @FXML
    private TableColumn<Reponse, String> responseNegColumn;

    @FXML
    private TableColumn<Reponse, Integer> scoreNegColumn;

    @FXML
    private TableColumn<Reponse, Void> actionNegColumn;

    @FXML
    private Button cancelQcm;

    // Nouveaux éléments pour la gestion des exercices
    @FXML
    private ComboBox<Exercice> comboExercice;

    @FXML
    private Button btnNouvelExercice;

    @FXML
    private Button btnModifierExercice;

    private Section section;
    private QCM qcm;
    private ObservableList<Exercice> listeExercices;

    public void setSection(Section identifierSection) {
        this.section = identifierSection;
        loadExercicesForControle();
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

    @FXML
    public void handleClicksAddGoodResponce(ActionEvent event) {
        String defaultResponse = "Correct Answer";
        int defaultScore;
        try {
            defaultScore = Integer.parseInt(baremePosDefault.getText());
        } catch (NumberFormatException e) {
            defaultScore = 1;
        }
        ObservableList<Reponse> items = correctTableView.getItems();
        items.add(new Reponse(defaultResponse, defaultScore));
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
    public void handleClicksCancelQCM(ActionEvent event) {
        EditerSection.cancelSection();
        Stage stage = (Stage) cancelQcm.getScene().getWindow();
        stage.close();
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        baremePosDefault.setText("1");
        baremeNegDefault.setText("0");

        // Initialisation du ComboBox des exercices
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
        });

        btnModifierExercice.setVisible(false);
        comboExercice.setPromptText("Sélectionner un exercice ou en créer un nouveau");

        // Initialisation des colonnes de réponses négatives
        responseNegColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getResponse()));
        responseNegColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        responseNegColumn.setOnEditCommit(event -> {
            Reponse reponse = event.getRowValue();
            reponse.setResponse(event.getNewValue());
        });

        scoreNegColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getScore()).asObject());
        scoreNegColumn.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        scoreNegColumn.setOnEditCommit(event -> {
            Reponse reponse = event.getRowValue();
            reponse.setScore(event.getNewValue());
        });

        actionNegColumn.setCellFactory(col -> new TableCell<>() {
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
                    openEditPopup(reponse, false);
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

        // Initialisation des colonnes de réponses positives
        responsePosColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getResponse()));
        responsePosColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        responsePosColumn.setOnEditCommit(event -> {
            Reponse reponse = event.getRowValue();
            reponse.setResponse(event.getNewValue());
        });

        scorePosColumn.setCellValueFactory(cellData -> new SimpleIntegerProperty(cellData.getValue().getScore()).asObject());
        scorePosColumn.setCellFactory(TextFieldTableCell.forTableColumn(new IntegerStringConverter()));
        scorePosColumn.setOnEditCommit(event -> {
            Reponse reponse = event.getRowValue();
            reponse.setScore(event.getNewValue());
        });

        actionPosColumn.setCellFactory(col -> new TableCell<>() {
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
                    openEditPopup(reponse, true);
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

        correctTableView.setItems(FXCollections.observableArrayList());
        enonceQuestion.setWrapText(true);
        comboExercice.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                // Retirer le style d'erreur si un exercice est sélectionné
                comboExercice.setStyle("");
            }
            btnModifierExercice.setVisible(newVal != null);
        });

        // Listener sur la question
        enonceQuestion.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null && !newVal.trim().isEmpty()) {
                enonceQuestion.getStyleClass().removeAll("text-field-danger");
            }
        });

        // Style CSS pour le champ obligatoire
        if (comboExercice.getValue() == null) {
            comboExercice.setPromptText("⚠️ OBLIGATOIRE : Sélectionner un exercice");
            comboExercice.setStyle("-fx-prompt-text-fill: #d32f2f; -fx-font-weight: bold;");
        }
    }

    // Méthodes pour gérer les exercices

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
            EditerExercice controller = loader.getController();  // ✓ CORRECTION ICI

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
            EditerExercice controller = loader.getController();  // ✓ CORRECTION ICI

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
    private void updateQCM() {
        String updateQcmQuery = "UPDATE QCM SET question = ? WHERE idQCM = ?";
        String deleteResponsesQuery = "DELETE FROM QCM_Reponses WHERE qcmID = ?";
        String insertResponseQuery = "INSERT INTO QCM_Reponses (qcmID, reponse, score, isCorrect) VALUES (?, ?, ?, ?)";

        try (Connection connection = SqlConnection.getConnection()) {
            connection.setAutoCommit(false);

            try (PreparedStatement updateStmt = connection.prepareStatement(updateQcmQuery)) {
                updateStmt.setString(1, enonceQuestion.getText());
                updateStmt.setInt(2, Integer.parseInt(qcm.getIdSection()));
                updateStmt.executeUpdate();
            }

            try (PreparedStatement deleteStmt = connection.prepareStatement(deleteResponsesQuery)) {
                deleteStmt.setInt(1, Integer.parseInt(qcm.getIdSection()));
                deleteStmt.executeUpdate();
            }

            try (PreparedStatement insertStmt = connection.prepareStatement(insertResponseQuery)) {
                for (Reponse response : correctTableView.getItems()) {
                    insertStmt.setInt(1, Integer.parseInt(qcm.getIdSection()));
                    insertStmt.setString(2, response.getResponse());
                    insertStmt.setInt(3, response.getScore());
                    insertStmt.setBoolean(4, true);
                    insertStmt.addBatch();
                }
                for (Reponse response : incorrectTableView.getItems()) {
                    insertStmt.setInt(1, Integer.parseInt(qcm.getIdSection()));
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
        updateQCM();



        this.section.getDevoir().getController().fetchAndUpdateTableView();
        Stage stage = (Stage) cancelQcm.getScene().getWindow();
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

    private void createQCM() {
        String insertQCUQuery = "INSERT INTO QCM (question, isQCU, sectionID) VALUES (?, ?, ?)";

        try (Connection connection = SqlConnection.getConnection();
             PreparedStatement insertStatement = connection.prepareStatement(insertQCUQuery, PreparedStatement.RETURN_GENERATED_KEYS)) {

            insertStatement.setString(1, enonceQuestion.getText());
            insertStatement.setBoolean(2, false);
            insertStatement.setString(3, this.section.getIdSection());

            int rowsAffected = insertStatement.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = insertStatement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        String idQCU = String.valueOf(generatedKeys.getInt(1));
                        qcm = new QCM();
                        qcm.setQCU(false);
                        qcm.setIdSection(idQCU);
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
                insertStatement.setInt(1, Integer.parseInt(qcm.getIdSection()));
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

            for (Reponse response : correctTableView.getItems()) {
                insertStatement.setInt(1, Integer.parseInt(qcm.getIdSection()));
                insertStatement.setString(2, response.getResponse());
                insertStatement.setInt(3, response.getScore());
                insertStatement.setBoolean(4, true);

                insertStatement.addBatch();
            }

            insertStatement.executeBatch();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void openEditPopup(Reponse reponse, boolean isCorrect) {
        TableView<Reponse> tablevieuw;
        if (isCorrect) {
            tablevieuw = correctTableView;
        } else {
            tablevieuw = incorrectTableView;
        }

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
            tablevieuw.refresh();
            popupStage.close();
        });

        Button closeButton = new Button("Close");
        closeButton.setOnAction(event -> {
            popupStage.close();
        });

        buttonBox.getChildren().addAll(saveButton, closeButton);

        popupVBox.getChildren().addAll(new Label("Modify the answer and the score:"), responseTextArea, scoreTextField, buttonBox);

        Scene popupScene = new Scene(popupVBox, 350, 250);
        popupStage.setScene(popupScene);
        popupStage.setTitle("Modify the answer");
        popupStage.setOnShown(event -> {
            responseTextArea.requestFocus();
            responseTextArea.selectAll();
        });
        popupStage.show();
    }

    public void setSectionUpdating(Section section) {
        this.section = section;
        this.enonceQuestion.setText(section.getIdSection());
        loadExercicesForControle();
        loadQCMFromSectionId(section.getIdSection());
    }

    private void loadQCMFromSectionId(String idSection) {
        String fetchQCMQuery = "SELECT * FROM QCM WHERE sectionID = ?";
        String fetchResponsesQuery = "SELECT * FROM QCM_Reponses WHERE qcmID = ?";

        try (Connection connection = SqlConnection.getConnection();
             PreparedStatement qcuStatement = connection.prepareStatement(fetchQCMQuery);
             PreparedStatement responseStatement = connection.prepareStatement(fetchResponsesQuery)) {

            qcuStatement.setString(1, idSection);
            ResultSet qcuResultSet = qcuStatement.executeQuery();
            if (qcuResultSet.next()) {
                qcm = new QCM();
                qcm.setIdSection(qcuResultSet.getString("idQCM"));
                qcm.setQuestion(qcuResultSet.getString("question"));
                qcm.setQCU(qcuResultSet.getBoolean("isQcu"));

                enonceQuestion.setText(qcm.getQuestion());
            }

            if (qcm != null) {
                ObservableList<Reponse> responsesIncorrect = FXCollections.observableArrayList();
                ObservableList<Reponse> responsesCorrect = FXCollections.observableArrayList();
                responseStatement.setInt(1, Integer.parseInt(qcm.getIdSection()));
                ResultSet responseResultSet = responseStatement.executeQuery();

                while (responseResultSet.next()) {
                    String responseText = responseResultSet.getString("reponse");
                    int score = responseResultSet.getInt("score");
                    boolean isCorrect = responseResultSet.getBoolean("isCorrect");

                    if (isCorrect) {
                        responsesCorrect.add(new Reponse(responseText, score));
                    } else {
                        responsesIncorrect.add(new Reponse(responseText, score));
                    }
                }
                incorrectTableView.setItems(responsesIncorrect);
                correctTableView.setItems(responsesCorrect);
            }

            // Charger l'exercice associé à cette section
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

    @FXML
    public void handleInputNumber(KeyEvent event) {
        TextField textField = (TextField) event.getSource();
        String currentText = textField.getText();

        // Allow only digits and an optional leading "-"
        String sanitizedText = currentText.replaceAll("[^\\d-]", "");

        // Ensure "-" is only at the start and not repeated
        if (sanitizedText.length() > 1) {
            sanitizedText = sanitizedText.replaceAll("(?<!^)-", ""); // Remove "-" if not at the start
        }

        // Limit to 3 characters (including possible "-")
        if (sanitizedText.length() > 3) {
            sanitizedText = sanitizedText.substring(0, 3);
        }
        textField.setText(sanitizedText);
        textField.positionCaret(sanitizedText.length());
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
    // ============ NOUVELLES MÉTHODES POUR NAVIGATION DEPUIS THEMEVIEW ============

    private AnchorPane parentPane;
    private int exerciceID;
    private boolean isStandaloneMode = false; // Mode édition directe sans Section

    /**
     * Définir le panneau parent pour la navigation
     */
    public void setParentPane(AnchorPane pane) {
        this.parentPane = pane;
    }

    /**
     * Définir l'ID de l'exercice à éditer
     */
    public void setExerciceID(int id) {
        this.exerciceID = id;
        this.isStandaloneMode = true;
    }

    /**
     * Charger un exercice complet depuis la base de données
     * Cette méthode permet d'éditer un exercice sans passer par une Section
     */
    public void chargerExercice(int idExercice) {
        System.out.println("📥 Chargement de l'exercice #" + idExercice);

        try (Connection conn = SqlConnection.getConnection()) {
            // 1. Charger les infos de l'exercice
            String queryExercice = "SELECT * FROM Exercice WHERE idExercice = ?";
            PreparedStatement stmtEx = conn.prepareStatement(queryExercice);
            stmtEx.setInt(1, idExercice);
            ResultSet rsEx = stmtEx.executeQuery();

            if (rsEx.next()) {
                // Remplir le ComboBox avec cet exercice
                Exercice ex = new Exercice(
                        rsEx.getInt("idExercice"),
                        rsEx.getInt("numero"),
                        rsEx.getString("titre"),
                        rsEx.getString("consigne"),
                        rsEx.getDouble("bareme"),
                        rsEx.getInt("controleID")
                );

                listeExercices.clear();
                listeExercices.add(ex);
                comboExercice.setValue(ex);
                comboExercice.setDisable(true); // Verrouiller en mode édition

                // Charger les autres exercices du même contrôle
                if (ex.getControleID() > 0) {
                    chargerAutresExercicesDuControle(ex.getControleID(), idExercice);
                }
            }

            // 2. Charger les questions QCM associées à cet exercice
            chargerQuestionsExercice(idExercice, conn);

        } catch (SQLException e) {
            e.printStackTrace();
            showErrorMessage("Erreur", "Impossible de charger l'exercice : " + e.getMessage());
        }
    }

    /**
     * Charger les autres exercices du même contrôle (pour permettre le changement)
     */
    private void chargerAutresExercicesDuControle(int controleID, int exerciceActuel) {
        String query = "SELECT * FROM Exercice WHERE controleID = ? AND idExercice != ? ORDER BY numero";

        try (Connection conn = SqlConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, controleID);
            stmt.setInt(2, exerciceActuel);
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

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Charger toutes les questions QCM d'un exercice
     */
    private void chargerQuestionsExercice(int exerciceID, Connection conn) throws SQLException {
        // Trouver les sections liées à cet exercice
        String querySections = "SELECT idSection FROM Section WHERE exerciceID = ?";
        PreparedStatement stmtSections = conn.prepareStatement(querySections);
        stmtSections.setInt(1, exerciceID);
        ResultSet rsSections = stmtSections.executeQuery();

        if (rsSections.next()) {
            String idSection = rsSections.getString("idSection");

            // Charger le QCM de cette section
            String queryQCM = "SELECT * FROM QCM WHERE sectionID = ?";
            PreparedStatement stmtQCM = conn.prepareStatement(queryQCM);
            stmtQCM.setString(1, idSection);
            ResultSet rsQCM = stmtQCM.executeQuery();

            if (rsQCM.next()) {
                // Remplir l'énoncé
                enonceQuestion.setText(rsQCM.getString("question"));

                int qcmID = rsQCM.getInt("idQCM");

                // Charger les réponses
                chargerReponsesQCM(qcmID, conn);
            }
        } else {
            // Aucune section trouvée, c'est un nouvel exercice
            System.out.println("ℹ️ Aucune question existante pour cet exercice");
            enonceQuestion.clear();
            correctTableView.getItems().clear();
            incorrectTableView.getItems().clear();
        }
    }

    /**
     * Charger les réponses d'un QCM
     */
    private void chargerReponsesQCM(int qcmID, Connection conn) throws SQLException {
        String queryReponses = "SELECT * FROM QCM_Reponses WHERE qcmID = ?";
        PreparedStatement stmtRep = conn.prepareStatement(queryReponses);
        stmtRep.setInt(1, qcmID);
        ResultSet rsRep = stmtRep.executeQuery();

        ObservableList<Reponse> reponsesCorrectes = FXCollections.observableArrayList();
        ObservableList<Reponse> reponsesIncorrectes = FXCollections.observableArrayList();

        while (rsRep.next()) {
            Reponse rep = new Reponse(
                    rsRep.getString("reponse"),
                    rsRep.getInt("score")
            );

            if (rsRep.getBoolean("isCorrect")) {
                reponsesCorrectes.add(rep);
            } else {
                reponsesIncorrectes.add(rep);
            }
        }

        correctTableView.setItems(reponsesCorrectes);
        incorrectTableView.setItems(reponsesIncorrectes);

        System.out.println("✅ " + reponsesCorrectes.size() + " réponses correctes chargées");
        System.out.println("✅ " + reponsesIncorrectes.size() + " réponses incorrectes chargées");
    }

    /**
     * Bouton "Retour" pour revenir à ThemeView
     */
    @FXML
    private void handleRetour() {
        if (parentPane != null && isStandaloneMode) {
            try {
                FxmlLoader object = new FxmlLoader();
                Parent view = object.getPane("edition/ThemeView");

                ThemeView controller = (ThemeView) object.getController();
                if (controller != null) {
                    controller.setParentPane(parentPane);
                }

                parentPane.getChildren().clear();
                parentPane.getChildren().setAll(view);

                System.out.println("✅ Retour à ThemeView");

            } catch (Exception e) {
                e.printStackTrace();
                showErrorMessage("Erreur", "Impossible de revenir à la liste des thèmes");
            }
        } else {
            // Mode normal (avec Section), fermer la fenêtre
            handleClicksCancelQCM(null);
        }
    }

    /**
     * Adapter le bouton "Add" pour le mode standalone
     */
    @FXML
    public void handleClicksAddQCM(ActionEvent event) {
        if (isStandaloneMode) {
            // Mode édition directe : sauvegarder directement
            sauvegarderQCMStandalone();
        } else {
            // Mode normal avec Section
            handleClicksAddQCMNormal(event);
        }
    }

    // Extraire la logique originale dans une méthode séparée
    private void handleClicksAddQCMNormal(ActionEvent event) {
        // ⭐ VALIDATION COMPLÈTE ⭐

        // 1. Vérifier l'exercice
        Exercice exerciceSelectionne = comboExercice.getValue();
        if (exerciceSelectionne == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR); // ⚠️ ERROR au lieu de WARNING
            alert.setTitle("❌ Exercice obligatoire");
            alert.setHeaderText("Vous devez associer cette question à un exercice");
            alert.setContentText("Veuillez sélectionner un exercice dans la liste ou cliquer sur \"+ Nouvel Exercice\" pour en créer un.");

            // Mettre en évidence le ComboBox
            comboExercice.setStyle("-fx-border-color: red; -fx-border-width: 2px;");

            alert.showAndWait();
            return;
        }

        // Réinitialiser le style du ComboBox si tout est OK
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

        // 3. Vérifier qu'il y a au moins une réponse correcte
        if (correctTableView.getItems().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("❌ Réponses manquantes");
            alert.setHeaderText("Ajoutez au moins une réponse correcte");
            alert.setContentText("Une question doit avoir au moins une réponse correcte.\n\n" +
                    "Cliquez sur \"Add a correct answer\" pour en ajouter.");
            alert.showAndWait();
            return;
        }

        // 4. Vérifier qu'il y a au moins une réponse (correcte OU incorrecte)
        if (correctTableView.getItems().isEmpty() && incorrectTableView.getItems().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("❌ Aucune réponse");
            alert.setHeaderText("Ajoutez des réponses à la question");
            alert.setContentText("Une question doit avoir au moins une réponse (correcte ou incorrecte).");
            alert.showAndWait();
            return;
        }

        // ✅ Toutes les validations sont passées, continuer

        if (checkSectionExists(this.section.getIdSection())) {
            Alert confirmationAlert = new Alert(Alert.AlertType.CONFIRMATION);
            confirmationAlert.setTitle("Section Exists");
            confirmationAlert.setHeaderText("This section already exists");
            confirmationAlert.setContentText("Section with the identifier " + this.section.getIdSection() +
                    " already exists. Would you like to overwrite it?");

            ButtonType modifyButton = new ButtonType("Modify");
            ButtonType cancelButton = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

            confirmationAlert.getButtonTypes().setAll(modifyButton, cancelButton);

            confirmationAlert.showAndWait().ifPresent(response -> {
                if (response == modifyButton) {
                    updateSection(exerciceSelectionne);
                }
            });
        } else {
            createSection(exerciceSelectionne);
            createQCM();
            createQCUResponse();

            this.section.getDevoir().getController().fetchAndUpdateTableView();

            // Message de succès
            Alert success = new Alert(Alert.AlertType.INFORMATION);
            success.setTitle("✅ Question ajoutée");
            success.setHeaderText("La question a été ajoutée avec succès");
            success.setContentText("Question ajoutée à l'exercice \"" + exerciceSelectionne.getTitre() + "\"");
            success.show();

            Stage stage = (Stage) cancelQcm.getScene().getWindow();
            stage.close();
        }
    }

    /**
     * Sauvegarder un QCM en mode standalone (sans Section existante)
     */
    private void sauvegarderQCMStandalone() {
        Exercice exerciceSelectionne = comboExercice.getValue();

        if (exerciceSelectionne == null) {
            showErrorMessage("Erreur", "Aucun exercice sélectionné");
            return;
        }

        if (!verifyQuesstion()) {
            showErrorMessage("Erreur", "Veuillez saisir une question");
            return;
        }

        if (correctTableView.getItems().isEmpty()) {
            showErrorMessage("Erreur", "Ajoutez au moins une réponse correcte");
            return;
        }

        try (Connection conn = SqlConnection.getConnection()) {
            conn.setAutoCommit(false);

            // 1. Créer ou récupérer une Section pour cet exercice
            String idSection = creerOuRecupererSection(exerciceSelectionne, conn);

            // 2. Créer ou mettre à jour le QCM
            int qcmID = creerOuMAJQCM(idSection, conn);

            // 3. Supprimer les anciennes réponses
            supprimerAnciennesReponses(qcmID, conn);

            // 4. Insérer les nouvelles réponses
            insererReponses(qcmID, conn);

            conn.commit();

            showInfoMessage("Succès", "Question enregistrée avec succès !");

            // Retourner à ThemeView
            handleRetour();

        } catch (SQLException e) {
            e.printStackTrace();
            showErrorMessage("Erreur SQL", "Impossible de sauvegarder : " + e.getMessage());
        }
    }

    /**
     * Créer ou récupérer une section pour un exercice
     */
    private String creerOuRecupererSection(Exercice exercice, Connection conn) throws SQLException {
        // Vérifier si une section existe déjà
        String queryCheck = "SELECT idSection FROM Section WHERE exerciceID = ? LIMIT 1";
        PreparedStatement stmtCheck = conn.prepareStatement(queryCheck);
        stmtCheck.setInt(1, exercice.getIdExercice());
        ResultSet rs = stmtCheck.executeQuery();

        if (rs.next()) {
            return rs.getString("idSection");
        }

        // Créer une nouvelle section
        String idSection = "QCM_" + exercice.getIdExercice() + "_" + System.currentTimeMillis();
        String insertSection = "INSERT INTO Section (idSection, ordreSection, controleID, exerciceID) VALUES (?, 1, ?, ?)";
        PreparedStatement stmtInsert = conn.prepareStatement(insertSection);
        stmtInsert.setString(1, idSection);
        stmtInsert.setInt(2, exercice.getControleID());
        stmtInsert.setInt(3, exercice.getIdExercice());
        stmtInsert.executeUpdate();

        return idSection;
    }

    /**
     * Créer ou mettre à jour un QCM
     */
    private int creerOuMAJQCM(String idSection, Connection conn) throws SQLException {
        // Vérifier si le QCM existe
        String queryCheck = "SELECT idQCM FROM QCM WHERE sectionID = ?";
        PreparedStatement stmtCheck = conn.prepareStatement(queryCheck);
        stmtCheck.setString(1, idSection);
        ResultSet rs = stmtCheck.executeQuery();

        if (rs.next()) {
            // Mettre à jour
            int qcmID = rs.getInt("idQCM");
            String update = "UPDATE QCM SET question = ?, isQCU = ? WHERE idQCM = ?";
            PreparedStatement stmtUpdate = conn.prepareStatement(update);
            stmtUpdate.setString(1, enonceQuestion.getText());
            stmtUpdate.setBoolean(2, false);
            stmtUpdate.setInt(3, qcmID);
            stmtUpdate.executeUpdate();
            return qcmID;
        } else {
            // Créer
            String insert = "INSERT INTO QCM (question, isQCU, sectionID) VALUES (?, ?, ?)";
            PreparedStatement stmtInsert = conn.prepareStatement(insert, PreparedStatement.RETURN_GENERATED_KEYS);
            stmtInsert.setString(1, enonceQuestion.getText());
            stmtInsert.setBoolean(2, false);
            stmtInsert.setString(3, idSection);
            stmtInsert.executeUpdate();

            ResultSet generatedKeys = stmtInsert.getGeneratedKeys();
            if (generatedKeys.next()) {
                return generatedKeys.getInt(1);
            }
        }

        throw new SQLException("Impossible de créer ou récupérer le QCM");
    }

    /**
     * Supprimer les anciennes réponses
     */
    private void supprimerAnciennesReponses(int qcmID, Connection conn) throws SQLException {
        String delete = "DELETE FROM QCM_Reponses WHERE qcmID = ?";
        PreparedStatement stmt = conn.prepareStatement(delete);
        stmt.setInt(1, qcmID);
        stmt.executeUpdate();
    }

    /**
     * Insérer les réponses (correctes et incorrectes)
     */
    private void insererReponses(int qcmID, Connection conn) throws SQLException {
        String insert = "INSERT INTO QCM_Reponses (qcmID, reponse, score, isCorrect) VALUES (?, ?, ?, ?)";
        PreparedStatement stmt = conn.prepareStatement(insert);

        // Réponses correctes
        for (Reponse rep : correctTableView.getItems()) {
            stmt.setInt(1, qcmID);
            stmt.setString(2, rep.getResponse());
            stmt.setInt(3, rep.getScore());
            stmt.setBoolean(4, true);
            stmt.addBatch();
        }

        // Réponses incorrectes
        for (Reponse rep : incorrectTableView.getItems()) {
            stmt.setInt(1, qcmID);
            stmt.setString(2, rep.getResponse());
            stmt.setInt(3, rep.getScore());
            stmt.setBoolean(4, false);
            stmt.addBatch();
        }

        stmt.executeBatch();
    }

// ============ FIN DES NOUVELLES MÉTHODES ============


}

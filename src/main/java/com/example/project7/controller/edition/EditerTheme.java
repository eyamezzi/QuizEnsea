package com.example.project7.controller.edition;

import com.example.project7.model.Theme;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import sql_connection.SqlConnection;

import java.net.URL;
import java.sql.*;
import java.util.ResourceBundle;

public class EditerTheme implements Initializable {
    // Interface pour le callback
    @FunctionalInterface
    public interface ThemeAddedCallback {
        void onThemeAdded();
    }

    private ThemeAddedCallback onThemeAddedCallback;

    // Méthode pour définir le callback
    public void setOnThemeAdded(ThemeAddedCallback callback) {
        this.onThemeAddedCallback = callback;
    }
    @FXML
    private TextField txtNomTheme;

    @FXML
    private ColorPicker colorPickerTheme;

    @FXML
    private Button btnAjouterTheme;

    @FXML
    private TableView<Theme> tableThemes;

    @FXML
    private TableColumn<Theme, String> colNomTheme;

    @FXML
    private TableColumn<Theme, String> colCouleurTheme;

    @FXML
    private TableColumn<Theme, Void> colActionsTheme;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        colorPickerTheme.setValue(Color.web("#A53860"));

        colNomTheme.setCellValueFactory(new PropertyValueFactory<>("nomTheme"));
        colCouleurTheme.setCellValueFactory(new PropertyValueFactory<>("couleur"));

        // Afficher la couleur visuellement
        colCouleurTheme.setCellFactory(col -> new TableCell<Theme, String>() {
            @Override
            protected void updateItem(String couleur, boolean empty) {
                super.updateItem(couleur, empty);
                if (empty || couleur == null) {
                    setGraphic(null);
                    setText(null);
                } else {
                    HBox box = new HBox();
                    box.setStyle("-fx-background-color: " + couleur + "; -fx-pref-width: 50; -fx-pref-height: 20; -fx-background-radius: 5;");
                    box.setAlignment(Pos.CENTER);
                    Label label = new Label(couleur);
                    label.setStyle("-fx-text-fill: white; -fx-font-size: 10px;");
                    setGraphic(box);
                }
            }
        });

        // Colonne Actions
        colActionsTheme.setCellFactory(col -> new TableCell<Theme, Void>() {
            private final Button btnSupprimer = new Button("✕");

            {
                btnSupprimer.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
                btnSupprimer.setOnAction(event -> {
                    Theme theme = getTableView().getItems().get(getIndex());
                    handleSupprimerTheme(theme);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(btnSupprimer);
                }
            }
        });

        chargerThemes();
    }

    private void chargerThemes() {
        ObservableList<Theme> themes = FXCollections.observableArrayList();
        String query = "SELECT * FROM Theme ORDER BY nomTheme";

        try (Connection conn = SqlConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                themes.add(new Theme(
                        rs.getInt("idTheme"),
                        rs.getString("nomTheme"),
                        rs.getString("couleur")
                ));
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger les thèmes : " + e.getMessage());
        }

        tableThemes.setItems(themes);
    }

    @FXML
    public void handleAjouterTheme(ActionEvent event) {
        String nomTheme = txtNomTheme.getText().trim();

        if (nomTheme.isEmpty()) {
            showAlert("Erreur", "Le nom du thème ne peut pas être vide !");
            return;
        }

        Color color = colorPickerTheme.getValue();
        String couleurHex = String.format("#%02X%02X%02X",
                (int)(color.getRed() * 255),
                (int)(color.getGreen() * 255),
                (int)(color.getBlue() * 255));

        String insertQuery = "INSERT INTO Theme (nomTheme, couleur) VALUES (?, ?)";

        try (Connection conn = SqlConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(insertQuery)) {

            stmt.setString(1, nomTheme);
            stmt.setString(2, couleurHex);
            stmt.executeUpdate();

            showAlert("Succès", "Thème ajouté avec succès !");
            txtNomTheme.clear();
            colorPickerTheme.setValue(Color.web("#A53860"));
            chargerThemes();

        } catch (SQLException e) {
            if (e.getMessage().contains("Unique")) {
                showAlert("Erreur", "Ce thème existe déjà !");
            } else {
                showAlert("Erreur", "Impossible d'ajouter le thème : " + e.getMessage());
            }
            e.printStackTrace();
        }
        // Appeler le callback si défini
        if (onThemeAddedCallback != null) {
            onThemeAddedCallback.onThemeAdded();
        }
    }

    private void handleSupprimerTheme(Theme theme) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation");
        alert.setHeaderText("Supprimer le thème \"" + theme.getNomTheme() + "\" ?");
        alert.setContentText("Cette action est irréversible.");

        alert.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                String deleteQuery = "DELETE FROM Theme WHERE idTheme = ?";

                try (Connection conn = SqlConnection.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(deleteQuery)) {

                    stmt.setInt(1, theme.getIdTheme());
                    stmt.executeUpdate();

                    showAlert("Succès", "Thème supprimé avec succès !");
                    chargerThemes();

                } catch (SQLException e) {
                    showAlert("Erreur", "Impossible de supprimer le thème : " + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    public void handleFermer(ActionEvent event) {
        Stage stage = (Stage) btnAjouterTheme.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

package com.example.project7.controller.edition;

import com.example.project7.FxmlLoader;
import com.example.project7.model.Exercice;
import com.example.project7.model.Theme;
import com.jfoenix.controls.JFXButton;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;
import sql_connection.SqlConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class EditerExercice {

    @FXML private Spinner<Integer> spinnerNumero;
    @FXML private TextField txtTitre;
    @FXML private TextArea txtConsigne;
    @FXML private TextField txtBareme;
    @FXML private Button btnValider;
    @FXML private Button btnAnnuler;

    // Éléments pour les thèmes
    @FXML private ComboBox<Theme> comboThemes;
    @FXML private FlowPane flowPaneThemes;
    @FXML private JFXButton btnAjouterTheme;
    @FXML private JFXButton btnGererThemes;

    private List<Theme> themesSelectionnes = new ArrayList<>();
    private Exercice exercice;
    private boolean validated = false;
    private int controleID;

    @FXML
    public void initialize() {
        SpinnerValueFactory<Integer> valueFactory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 99, 1);
        spinnerNumero.setValueFactory(valueFactory);

        // Validation du barème
        txtBareme.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*\\.?\\d*")) {
                txtBareme.setText(oldVal);
            }
        });

        // Charger les thèmes disponibles
        chargerThemesDisponibles();

        javafx.application.Platform.runLater(() -> txtTitre.requestFocus());
    }

    public void setControleID(int controleID) {
        this.controleID = controleID;
    }

    public void setNumeroSuggere(int numero) {
        spinnerNumero.getValueFactory().setValue(numero);
    }

    public void setExercice(Exercice ex) {
        this.exercice = ex;
        if (ex != null) {
            spinnerNumero.getValueFactory().setValue(ex.getNumero());
            txtTitre.setText(ex.getTitre());
            txtConsigne.setText(ex.getConsigne());
            txtBareme.setText(String.valueOf(ex.getBareme()));
            // CORRECTION : Charger les thèmes UNIQUEMENT si l'exercice a un ID valide
            if (ex.getIdExercice() > 0) {
                System.out.println("🔍 Chargement des thèmes pour l'exercice ID: " + ex.getIdExercice());
                chargerThemesExercice(ex.getIdExercice());
            } else {
                System.out.println("⚠️ Nouvel exercice - pas de thèmes à charger");
            }
        }
    }

    @FXML
    private void handleValider() {
        if (exercice == null) {
            exercice = new Exercice(
                    spinnerNumero.getValue(),
                    txtTitre.getText().trim()
            );
            exercice.setControleID(controleID);
        } else {
            exercice.setNumero(spinnerNumero.getValue());
            exercice.setTitre(txtTitre.getText().trim());
        }

        exercice.setConsigne(txtConsigne.getText().trim());

        try {
            if (txtBareme.getText() != null && !txtBareme.getText().trim().isEmpty()) {
                exercice.setBareme(Double.parseDouble(txtBareme.getText().trim()));
            }
        } catch (NumberFormatException e) {
            exercice.setBareme(0.0);
        }

        // Sauvegarder dans la base de données
        if (exercice.getIdExercice() == 0) {
            createExerciceInDB();
        } else {
            updateExerciceInDB();
        }

        // Sauvegarder les thèmes après avoir créé/mis à jour l'exercice
        if (exercice.getIdExercice() > 0) {
            sauvegarderThemesExercice(exercice.getIdExercice());
        }

        validated = true;
        closeWindow();
    }

    private void createExerciceInDB() {
        String insertQuery = "INSERT INTO Exercice (numero, titre, consigne, bareme, controleID) VALUES (?, ?, ?, ?, ?)";

        try (Connection connection = SqlConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(insertQuery, PreparedStatement.RETURN_GENERATED_KEYS)) {

            stmt.setInt(1, exercice.getNumero());
            stmt.setString(2, exercice.getTitre());
            stmt.setString(3, exercice.getConsigne());
            stmt.setDouble(4, exercice.getBareme());
            stmt.setInt(5, exercice.getControleID());

            int rowsAffected = stmt.executeUpdate();
            if (rowsAffected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        exercice.setIdExercice(generatedKeys.getInt(1));
                        associerThemeParDefautSiNecessaire(exercice.getIdExercice(), exercice.getControleID());
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de créer l'exercice : " + e.getMessage());
        }
    }
    /**
     * Associe automatiquement le thème par défaut de l'épreuve à un nouvel exercice
     * UNIQUEMENT si aucun thème n'a été manuellement sélectionné
     */
    /**
     * Associe automatiquement le thème par défaut de l'épreuve à un exercice
     * Ce thème est TOUJOURS ajouté, même si l'utilisateur sélectionne d'autres thèmes
     */
    private void associerThemeParDefautSiNecessaire(int exerciceID, int controleID) {
        String queryThemeParDefaut = "SELECT themeParDefautID FROM Controle WHERE idControle = ?";

        try (Connection conn = SqlConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(queryThemeParDefaut)) {

            stmt.setInt(1, controleID);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int themeParDefautID = rs.getInt("themeParDefautID");

                if (!rs.wasNull() && themeParDefautID > 0) {
                    // Vérifier si le thème par défaut n'est pas déjà dans la liste des thèmes sélectionnés
                    boolean dejaPresent = themesSelectionnes.stream()
                            .anyMatch(t -> t.getIdTheme() == themeParDefautID);

                    if (!dejaPresent) {
                        // Insérer le thème par défaut
                        String insertQuery = "INSERT INTO Exercice_Theme (exerciceID, themeID) VALUES (?, ?)";
                        try (PreparedStatement insertStmt = conn.prepareStatement(insertQuery)) {
                            insertStmt.setInt(1, exerciceID);
                            insertStmt.setInt(2, themeParDefautID);
                            insertStmt.executeUpdate();

                            System.out.println("✅ Thème par défaut (ID: " + themeParDefautID +
                                    ") associé automatiquement à l'exercice #" + exerciceID);

                            // Charger le thème dans l'interface pour que l'utilisateur le voie
                            chargerEtAfficherThemeParDefaut(themeParDefautID);
                        }
                    } else {
                        System.out.println("ℹ️ Thème par défaut déjà sélectionné manuellement");
                    }
                } else {
                    System.out.println("ℹ️ Aucun thème par défaut défini pour cette épreuve");
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("❌ Erreur lors de l'association du thème par défaut : " + e.getMessage());
        }
    }
    /**
     * Charge un thème depuis la base de données et l'affiche dans l'interface
     */
    /**
     * Charge un thème depuis la base de données et l'affiche dans l'interface
     */
    /**
     * Charge un thème depuis la base de données et l'affiche dans l'interface
     */
    private void chargerEtAfficherThemeParDefaut(int themeID) {
        String query = "SELECT * FROM Theme WHERE idTheme = ?";

        try (Connection conn = SqlConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, themeID);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                Theme theme = new Theme(
                        rs.getInt("idTheme"),
                        rs.getString("nomTheme"),
                        rs.getString("couleur")
                );

                // Ajouter à la liste et afficher AVEC l'indicateur "par défaut"
                themesSelectionnes.add(theme);
                ajouterThemeChip(theme, true);  // ✅ CORRECTION ICI

                System.out.println("🎨 Thème par défaut affiché : " + theme.getNomTheme());
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    private void updateExerciceInDB() {
        String updateQuery = "UPDATE Exercice SET numero = ?, titre = ?, consigne = ?, bareme = ? WHERE idExercice = ?";

        try (Connection connection = SqlConnection.getConnection();
             PreparedStatement stmt = connection.prepareStatement(updateQuery)) {

            stmt.setInt(1, exercice.getNumero());
            stmt.setString(2, exercice.getTitre());
            stmt.setString(3, exercice.getConsigne());
            stmt.setDouble(4, exercice.getBareme());
            stmt.setInt(5, exercice.getIdExercice());

            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de mettre à jour l'exercice : " + e.getMessage());
        }
    }

    @FXML
    private void handleAnnuler() {
        validated = false;
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) txtTitre.getScene().getWindow();
        stage.close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public boolean isValidated() {
        return validated;
    }

    public Exercice getExercice() {
        return exercice;
    }

    // ========== GESTION DES THÈMES ==========

    private void chargerThemesDisponibles() {
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
            System.out.println("✅ " + themes.size() + " thèmes disponibles chargés");
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("❌ Erreur lors du chargement des thèmes disponibles");
        }

        comboThemes.setItems(themes);

        comboThemes.setConverter(new StringConverter<Theme>() {
            @Override
            public String toString(Theme theme) {
                return theme != null ? theme.getNomTheme() : "";
            }

            @Override
            public Theme fromString(String string) {
                return null;
            }
        });
    }

    @FXML
    private void handleAjouterTheme(ActionEvent event) {
        Theme themeSelectionne = comboThemes.getValue();

        if (themeSelectionne == null) {
            showAlert("Attention", "Veuillez sélectionner un thème !");
            return;
        }

        if (themesSelectionnes.contains(themeSelectionne)) {
            showAlert("Attention", "Ce thème est déjà ajouté !");
            return;
        }

        themesSelectionnes.add(themeSelectionne);
        ajouterThemeChip(themeSelectionne);
        comboThemes.setValue(null);
    }

    private void ajouterThemeChip(Theme theme) {
        ajouterThemeChip(theme, false);
    }

    private void ajouterThemeChip(Theme theme, boolean estThemeParDefaut) {
        HBox chip = new HBox(6);
        chip.setAlignment(Pos.CENTER_LEFT);

        String style = "-fx-background-color: " + theme.getCouleur() + "; " +
                "-fx-background-radius: 12; " +
                "-fx-padding: 4 10 4 10; " +
                "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.15), 2, 0, 0, 1);";

        // Si c'est le thème par défaut, ajouter une bordure dorée
        if (estThemeParDefaut) {
            style += "-fx-border-color: gold; -fx-border-width: 2; -fx-border-radius: 12;";
        }

        chip.setStyle(style);

        String labelText = theme.getNomTheme();
        if (estThemeParDefaut) {
            labelText += " ★"; // Ajouter une étoile pour le thème par défaut
        }

        Label label = new Label(labelText);
        label.setStyle("-fx-text-fill: white; -fx-font-size: 11px; -fx-font-weight: bold;");

        Button btnSupprimer = new Button("×");
        btnSupprimer.setStyle("-fx-background-color: transparent; " +
                "-fx-text-fill: white; " +
                "-fx-font-size: 16px; " +
                "-fx-font-weight: bold; " +
                "-fx-cursor: hand; " +
                "-fx-padding: 0 3 0 3;");

        // Empêcher la suppression du thème par défaut
        if (estThemeParDefaut) {
            btnSupprimer.setDisable(true);
            btnSupprimer.setVisible(false);

            Tooltip tooltip = new Tooltip("Ce thème est le thème par défaut de l'épreuve et ne peut pas être supprimé");
            Tooltip.install(chip, tooltip);
        } else {
            btnSupprimer.setOnAction(e -> {
                themesSelectionnes.remove(theme);
                flowPaneThemes.getChildren().remove(chip);
            });
        }

        chip.getChildren().addAll(label, btnSupprimer);
        flowPaneThemes.getChildren().add(chip);
    }

    @FXML
    private void handleOuvrirGestionThemes(ActionEvent event) {
        try {
            FxmlLoader object = new FxmlLoader();
            Parent view = object.getPane("editer_quiz/_9_EditerTheme");

            Scene popupScene = new Scene(view);
            Stage popupStage = new Stage();

            popupStage.setTitle("Gestion des Thèmes");
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.initOwner(btnGererThemes.getScene().getWindow());
            popupStage.setScene(popupScene);
            popupStage.setResizable(false);

            popupStage.showAndWait();

            // Recharger les thèmes disponibles après modification
            chargerThemesDisponibles();

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible d'ouvrir la gestion des thèmes : " + e.getMessage());
        }
    }

    private void sauvegarderThemesExercice(int exerciceID) {
        try (Connection conn = SqlConnection.getConnection()) {
            // Récupérer le thème par défaut de l'épreuve
            Integer themeParDefautID = getThemeParDefautDeEpreuve(exerciceID, conn);

            // Supprimer les anciens thèmes SAUF le thème par défaut
            String deleteQuery;
            PreparedStatement deleteStmt;

            if (themeParDefautID != null) {
                deleteQuery = "DELETE FROM Exercice_Theme WHERE exerciceID = ? AND themeID != ?";
                deleteStmt = conn.prepareStatement(deleteQuery);
                deleteStmt.setInt(1, exerciceID);
                deleteStmt.setInt(2, themeParDefautID);
            } else {
                deleteQuery = "DELETE FROM Exercice_Theme WHERE exerciceID = ?";
                deleteStmt = conn.prepareStatement(deleteQuery);
                deleteStmt.setInt(1, exerciceID);
            }

            int deleted = deleteStmt.executeUpdate();
            deleteStmt.close();
            System.out.println("🗑️ " + deleted + " anciens thèmes supprimés (thème par défaut préservé)");

            System.out.println("🧠 Themes sélectionnés : " + themesSelectionnes.size());

            // Insérer les nouveaux thèmes
            if (!themesSelectionnes.isEmpty()) {
                String insertQuery = "INSERT IGNORE INTO Exercice_Theme (exerciceID, themeID) VALUES (?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertQuery)) {
                    for (Theme theme : themesSelectionnes) {
                        stmt.setInt(1, exerciceID);
                        stmt.setInt(2, theme.getIdTheme());
                        stmt.executeUpdate();
                        System.out.println("➕ Thème '" + theme.getNomTheme() + "' ajouté");
                    }
                }
                System.out.println("✅ " + themesSelectionnes.size() + " thèmes sauvegardés");
            }

            // Affichage de debug
            String query = "SELECT et.*, t.nomTheme FROM Exercice_Theme et " +
                    "JOIN Theme t ON et.themeID = t.idTheme " +
                    "WHERE et.exerciceID = ?";
            try (PreparedStatement stmt = conn.prepareStatement(query)) {
                stmt.setInt(1, exerciceID);
                ResultSet rs = stmt.executeQuery();

                System.out.println("\n=== Thèmes de l'exercice #" + exerciceID + " ===");
                int count = 0;
                while (rs.next()) {
                    int thID = rs.getInt("themeID");
                    String nomTheme = rs.getString("nomTheme");
                    String marqueur = (themeParDefautID != null && thID == themeParDefautID) ? " [PAR DÉFAUT]" : "";
                    System.out.println("Thème ID: " + thID + " - " + nomTheme + marqueur);
                    count++;
                }

                if (count == 0) {
                    System.out.println("⚠️ Aucun thème associé.");
                } else {
                    System.out.println("✅ Total : " + count + " thème(s)\n");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("❌ Erreur lors de la sauvegarde des thèmes : " + e.getMessage());
        }
    }
    /**
     * Récupère le thème par défaut de l'épreuve associée à un exercice
     */
    private Integer getThemeParDefautDeEpreuve(int exerciceID, Connection conn) throws SQLException {
        String query = "SELECT c.themeParDefautID FROM Controle c " +
                "JOIN Exercice e ON c.idControle = e.controleID " +
                "WHERE e.idExercice = ?";

        try (PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, exerciceID);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                int themeID = rs.getInt("themeParDefautID");
                if (!rs.wasNull()) {
                    return themeID;
                }
            }
        }

        return null;
    }

    private void chargerThemesExercice(int exerciceID) {
        themesSelectionnes.clear();
        flowPaneThemes.getChildren().clear();

        String query = "SELECT t.*, " +
                "(SELECT c.themeParDefautID FROM Controle c " +
                " JOIN Exercice e ON c.idControle = e.controleID " +
                " WHERE e.idExercice = ?) AS themeParDefaut " +
                "FROM Theme t " +
                "JOIN Exercice_Theme et ON t.idTheme = et.themeID " +
                "WHERE et.exerciceID = ? " +
                "ORDER BY (t.idTheme = (SELECT c.themeParDefautID FROM Controle c " +
                "                        JOIN Exercice e ON c.idControle = e.controleID " +
                "                        WHERE e.idExercice = ?)) DESC, t.nomTheme";

        try (Connection conn = SqlConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, exerciceID);
            stmt.setInt(2, exerciceID);
            stmt.setInt(3, exerciceID);
            ResultSet rs = stmt.executeQuery();

            int count = 0;
            while (rs.next()) {
                Theme theme = new Theme(
                        rs.getInt("idTheme"),
                        rs.getString("nomTheme"),
                        rs.getString("couleur")
                );

                themesSelectionnes.add(theme);

                // Vérifier si c'est le thème par défaut
                int themeParDefautID = rs.getInt("themeParDefaut");
                boolean estThemeParDefaut = !rs.wasNull() && theme.getIdTheme() == themeParDefautID;

                ajouterThemeChip(theme, estThemeParDefaut);
                count++;
                System.out.println("✅ Thème chargé : " + theme.getNomTheme() +
                        (estThemeParDefaut ? " [PAR DÉFAUT]" : ""));
            }

            System.out.println("📊 Total : " + count + " thèmes chargés pour l'exercice " + exerciceID);

        } catch (SQLException e) {
            e.printStackTrace();
            System.err.println("❌ Erreur lors du chargement des thèmes de l'exercice : " + e.getMessage());
        }
    }
}
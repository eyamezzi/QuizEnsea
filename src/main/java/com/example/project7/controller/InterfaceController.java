package com.example.project7.controller;

import com.example.project7.FxmlLoader;
import com.example.project7.controller.edition.OpenProjet;
import com.example.project7.controller.edition.SelectionTypeProjet;
import com.example.project7.controller.edition.ThemeView;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

import java.net.URL;
import java.util.ResourceBundle;


public class InterfaceController implements Initializable {

    @FXML
    private AnchorPane anchorpane3;

    @FXML
    public Label name;

    @FXML
    void handleClicksAccueil(ActionEvent event) {
        FxmlLoader object = new FxmlLoader();
        Parent view = object.getPane("Home");
        anchorpane3.getChildren().removeAll();
        anchorpane3.getChildren().setAll(view);
    }

    @FXML
    void handleClicksNew(ActionEvent event) {
        FxmlLoader object = new FxmlLoader();
        Parent view = object.getPane("editer_quiz/_1_SelectionTypeDeProjet");
        SelectionTypeProjet controller = (SelectionTypeProjet) object.getController();
        if (controller != null) {
            controller.setParentPane(anchorpane3);
        }
        anchorpane3.getChildren().removeAll();
        anchorpane3.getChildren().setAll(view);
    }


    @FXML
    void handleClicksOpen(ActionEvent event) {
        FxmlLoader object = new FxmlLoader();
        Parent view = object.getPane("openinterf");
        OpenProjet controller = (OpenProjet) object.getController();
        if (controller != null) {
            controller.setParentPane(anchorpane3);
        }
        anchorpane3.getChildren().removeAll();
        anchorpane3.getChildren().setAll(view);
    }


    @FXML
    void handleClicksSetting(ActionEvent event) {
        FxmlLoader object = new FxmlLoader();
        Parent view = object.getPane("SettingInterf");
        anchorpane3.getChildren().removeAll();
        anchorpane3.getChildren().setAll(view);
    }


    @FXML
    void handleClicksHelp(ActionEvent event) {
        FxmlLoader object = new FxmlLoader();
        Parent view = object.getPane("HelpInterf");
        anchorpane3.getChildren().removeAll();
        anchorpane3.getChildren().setAll(view);
    }@FXML
    void handleClicksThemes(ActionEvent event) {
        try {
            // ✅ Utilisez le chemin EXACT depuis resources
            URL fxmlUrl = getClass().getResource("/com/example/project7/editer_quiz/ThemeView.fxml");

            if (fxmlUrl == null) {
                System.err.println("❌ Fichier ThemeView.fxml introuvable !");
                return;
            }

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent view = loader.load();
            // ✅ Passer le parentPane au contrôleur
            ThemeView controller = loader.getController();
            if (controller != null) {
                controller.setParentPane(anchorpane3);
            }


            if (view == null) {
                System.err.println("❌ Le chargement a échoué !");
                return;
            }

            anchorpane3.getChildren().clear();
            anchorpane3.getChildren().setAll(view);

            System.out.println("✅ ThemeView chargé avec succès !");

        } catch (Exception e) {
            System.err.println("❌ Erreur lors du chargement de ThemeView :");
            e.printStackTrace();
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        FxmlLoader object = new FxmlLoader();
        Parent view = object.getPane("Home");
        anchorpane3.getChildren().removeAll();
        anchorpane3.getChildren().setAll(view);
    }
       /* @FXML
    private void handleClicksThemes() {
        try {
            FxmlLoader object = new FxmlLoader();
            Parent view = object.getPane("editer_quiz/ThemeView");

            if (view == null) {
                System.err.println("❌ Le fichier ThemeView.fxml n'a pas pu être chargé !");
                return;
            }

            Stage stage = new Stage();
            stage.setTitle("Bibliothèque - Filtrage par Thèmes");
            stage.setScene(new Scene(view));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Erreur : " + e.getMessage());
        }
    }*/


}

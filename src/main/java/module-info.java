module com.example.project7 {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.sql;
    requires java.desktop;
    requires com.h2database;
    requires com.jfoenix;
    requires tess4j;
    requires org.apache.pdfbox;
    requires java.net.http;

    opens com.example.project7 to javafx.fxml;
    opens com.example.project7.controller to javafx.fxml;
    opens com.example.project7.controller.edition to javafx.fxml;
    opens com.example.project7.controller.correction to javafx.fxml;
    opens com.example.project7.model to javafx.fxml;
    opens sql_connection to javafx.fxml;

    exports com.example.project7;
    exports com.example.project7.model;
    exports com.example.project7.controller.edition;
    exports sql_connection;
}
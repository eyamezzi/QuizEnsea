package sql_connection;

import com.example.project7.model.TypeDevoir;
import com.example.project7.model.TypeNumero;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.ResultSet;

import static sql_connection.SqlConnection.getConnection;

public class DataBase {

    public static void createDatabaseIfDoesNotExist() {
        try {
            initializeDatabase();
        } catch (Exception e) {
            System.err.println("Error during setup: " + e.getMessage());
        }
    }

    private static void initializeDatabase() {
        try (Connection connection = getConnection(); Statement statement = connection.createStatement()) {
            createTable(statement);
            migrateDatabase(statement); // Migration des colonnes manquantes
            insertTypeDevoirData(statement);
            insertTypeNumeroData(statement);
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("SQL error: " + e.getMessage());
        }
    }

    private static void migrateDatabase(Statement statement) throws Exception {
        System.out.println("Checking for database migrations...");

        // Vérifier si la colonne exerciceID existe dans la table Section
        boolean exerciceIDExists = false;
        try {
            ResultSet rs = statement.executeQuery(
                    "SELECT exerciceID FROM Section WHERE 1=0"
            );
            exerciceIDExists = true;
            rs.close();
        } catch (Exception e) {
            exerciceIDExists = false;
        }

        if (!exerciceIDExists) {
            System.out.println("Adding missing column exerciceID to Section table...");
            try {
                statement.executeUpdate(
                        "ALTER TABLE Section ADD COLUMN exerciceID INT"
                );
                statement.executeUpdate(
                        "ALTER TABLE Section ADD CONSTRAINT fk_section_exercice " +
                                "FOREIGN KEY (exerciceID) REFERENCES Exercice(idExercice) ON DELETE SET NULL"
                );
                System.out.println("✓ Column exerciceID added successfully!");
            } catch (Exception e) {
                System.err.println("Error adding exerciceID column: " + e.getMessage());
                throw e;
            }
        } else {
            System.out.println("✓ Column exerciceID already exists.");
        }

        // Vérifier si la colonne estDansBibliotheque existe dans la table Exercice
        boolean estDansBibliothequeExists = false;
        try {
            ResultSet rs = statement.executeQuery(
                    "SELECT estDansBibliotheque FROM Exercice WHERE 1=0"
            );
            estDansBibliothequeExists = true;
            rs.close();
        } catch (Exception e) {
            estDansBibliothequeExists = false;
        }

        if (!estDansBibliothequeExists) {
            System.out.println("Adding missing column estDansBibliotheque to Exercice table...");
            try {
                statement.executeUpdate(
                        "ALTER TABLE Exercice ADD COLUMN estDansBibliotheque BOOLEAN DEFAULT FALSE"
                );
                System.out.println("✓ Column estDansBibliotheque added successfully!");
            } catch (Exception e) {
                System.err.println("Error adding estDansBibliotheque column: " + e.getMessage());
                throw e;
            }
        } else {
            System.out.println("✓ Column estDansBibliotheque already exists.");
        }

        // Vérifier si la colonne themeParDefautID existe dans la table Controle
        boolean themeParDefautIDExists = false;
        try {
            ResultSet rs = statement.executeQuery(
                    "SELECT themeParDefautID FROM Controle WHERE 1=0"
            );
            themeParDefautIDExists = true;
            rs.close();
        } catch (Exception e) {
            themeParDefautIDExists = false;
        }

        if (!themeParDefautIDExists) {
            System.out.println("Adding missing column themeParDefautID to Controle table...");
            try {
                statement.executeUpdate(
                        "ALTER TABLE Controle ADD COLUMN themeParDefautID INT"
                );
                statement.executeUpdate(
                        "ALTER TABLE Controle ADD CONSTRAINT fk_controle_theme " +
                                "FOREIGN KEY (themeParDefautID) REFERENCES Theme(idTheme)"
                );
                System.out.println("✓ Column themeParDefautID added successfully!");
            } catch (Exception e) {
                System.err.println("Error adding themeParDefautID column: " + e.getMessage());
                throw e;
            }
        } else {
            System.out.println("✓ Column themeParDefautID already exists.");
        }

        System.out.println("Database migration completed successfully!");
    }

    private static void createTable(Statement statement) throws Exception {
        // ========== ORDRE IMPORTANT ==========
        // Les tables doivent être créées dans l'ordre de leurs dépendances

        // 1️⃣ Projet (aucune dépendance)
        String createProjetQuery = "CREATE TABLE IF NOT EXISTS Projet (" +
                "idProjet INT AUTO_INCREMENT PRIMARY KEY, " +
                "nomProjet VARCHAR(255) NOT NULL, " +
                "localisationProjet VARCHAR(255), " +
                "typeProjet VARCHAR(100), " +
                "creationDate DATE DEFAULT (CURRENT_DATE));";
        statement.executeUpdate(createProjetQuery);

        // 2️⃣ Theme (aucune dépendance) - IMPORTANT : Créer AVANT Controle
        String createThemeQuery = "CREATE TABLE IF NOT EXISTS Theme (" +
                "idTheme INT AUTO_INCREMENT PRIMARY KEY, " +
                "nomTheme VARCHAR(100) UNIQUE NOT NULL, " +
                "couleur VARCHAR(7));";
        statement.executeUpdate(createThemeQuery);

        // 3️⃣ Controle (dépend de Projet)
        // Note: themeParDefautID sera ajouté par la migration pour éviter les problèmes d'ordre
        String createControleQuery = "CREATE TABLE IF NOT EXISTS Controle (" +
                "idControle INT AUTO_INCREMENT PRIMARY KEY, " +
                "nomDevoir VARCHAR(255) NOT NULL, " +
                "typeDevoir VARCHAR(255), " +
                "nombreExemplaire INT DEFAULT 1, " +
                "randomSeed INT DEFAULT 12345678, " +
                "examHeader TEXT, " +
                "reponseHeader TEXT, " +
                "creationDate DATE DEFAULT CURRENT_DATE, " +
                "projetID INT, " +
                "FOREIGN KEY (projetID) REFERENCES Projet(idProjet) ON DELETE CASCADE);";
        statement.executeUpdate(createControleQuery);

        // 4️⃣ Exercice (dépend de Controle)
        String createExerciceQuery = "CREATE TABLE IF NOT EXISTS Exercice (" +
                "idExercice INT AUTO_INCREMENT PRIMARY KEY, " +
                "numero INT NOT NULL, " +
                "titre VARCHAR(255) NOT NULL, " +
                "consigne TEXT, " +
                "bareme DECIMAL(5,2) DEFAULT 0.00, " +
                "controleID INT NOT NULL, " +
                "FOREIGN KEY (controleID) REFERENCES Controle(idControle) ON DELETE CASCADE);";
        statement.executeUpdate(createExerciceQuery);

        // 5️⃣ Exercice_Theme (dépend de Exercice et Theme)
        String createExerciceThemeQuery = "CREATE TABLE IF NOT EXISTS Exercice_Theme (" +
                "exerciceID INT, " +
                "themeID INT, " +
                "PRIMARY KEY (exerciceID, themeID), " +
                "FOREIGN KEY (exerciceID) REFERENCES Exercice(idExercice) ON DELETE CASCADE, " +
                "FOREIGN KEY (themeID) REFERENCES Theme(idTheme) ON DELETE CASCADE);";
        statement.executeUpdate(createExerciceThemeQuery);

        // 6️⃣ Section (dépend de Controle)
        // Note: exerciceID sera ajouté par la migration
        String createSectionQuery = "CREATE TABLE IF NOT EXISTS Section (" +
                "idSection VARCHAR(255) PRIMARY KEY, " +
                "ordreSection INT, " +
                "numberOfSections INT DEFAULT 0, " +
                "controleID INT, " +
                "FOREIGN KEY (controleID) REFERENCES Controle(idControle) ON DELETE CASCADE);";
        statement.executeUpdate(createSectionQuery);

        // 7️⃣ QCM (dépend de Section)
        String createQCMQuery = "CREATE TABLE IF NOT EXISTS QCM (" +
                "idQCM INT AUTO_INCREMENT PRIMARY KEY, " +
                "question TEXT NOT NULL, " +
                "isQCU BOOLEAN, " +
                "sectionID VARCHAR(255), " +
                "FOREIGN KEY (sectionID) REFERENCES Section(idSection) ON DELETE CASCADE);";
        statement.executeUpdate(createQCMQuery);

        // 8️⃣ QCM_Reponses (dépend de QCM)
        String createQCMReponsesQuery = "CREATE TABLE IF NOT EXISTS QCM_Reponses (" +
                "idReponse INT AUTO_INCREMENT PRIMARY KEY, " +
                "qcmID INT, " +
                "reponse TEXT, " +
                "score INT, " +
                "isCorrect BOOLEAN, " +
                "FOREIGN KEY (qcmID) REFERENCES QCM(idQCM) ON DELETE CASCADE);";
        statement.executeUpdate(createQCMReponsesQuery);

        // 9️⃣ QuestionLibre (dépend de Section)
        String createQuestionLibreQuery = "CREATE TABLE IF NOT EXISTS QuestionLibre (" +
                "idQuestionLibre INT AUTO_INCREMENT PRIMARY KEY, " +
                "question TEXT NOT NULL, " +
                "scoreTotal FLOAT, " +
                "nombreScore INT, " +
                "nombreLigne INT, " +
                "tailleLigne FLOAT, " +
                "rappel TEXT, " +
                "sectionID VARCHAR(255), " +
                "FOREIGN KEY (sectionID) REFERENCES Section (idSection) ON DELETE CASCADE);";
        statement.executeUpdate(createQuestionLibreQuery);

        // 🔟 Description (dépend de Section)
        String createDescriptionQuery = "CREATE TABLE IF NOT EXISTS Description (" +
                "idDescription INT AUTO_INCREMENT PRIMARY KEY, " +
                "texte TEXT, " +
                "sectionID VARCHAR(255), " +
                "FOREIGN KEY (sectionID) REFERENCES Section(idSection) ON DELETE CASCADE);";
        statement.executeUpdate(createDescriptionQuery);

        // 1️⃣1️⃣ Description_Images (dépend de Description)
        String createDescriptionImagesQuery = "CREATE TABLE IF NOT EXISTS Description_Images (" +
                "idImage INT AUTO_INCREMENT PRIMARY KEY, " +
                "descriptionID INT, " +
                "imagePath VARCHAR(255), " +
                "legendText VARCHAR(255), " +
                "imageWidth FLOAT," +
                "FOREIGN KEY (descriptionID) REFERENCES Description(idDescription) ON DELETE CASCADE);";
        statement.executeUpdate(createDescriptionImagesQuery);

        // Tables auxiliaires sans dépendances complexes
        String createFontDevoirQuery = "CREATE TABLE IF NOT EXISTS FontDevoir (" +
                "idFontDevoir INT AUTO_INCREMENT PRIMARY KEY, " +
                "nomPolice VARCHAR(100), " +
                "sizePolice INT);";
        statement.executeUpdate(createFontDevoirQuery);

        String createFormatQuestionQuery = "CREATE TABLE IF NOT EXISTS FormatQuestion (" +
                "idFormatQuestion INT AUTO_INCREMENT PRIMARY KEY, " +
                "premierTexte TEXT, " +
                "isNumerated BOOLEAN, " +
                "numeroIncremente VARCHAR(50), " +
                "deuxiemeTexte TEXT);";
        statement.executeUpdate(createFormatQuestionQuery);

        String createReponseQuery = "CREATE TABLE IF NOT EXISTS Reponse (" +
                "idReponse INT AUTO_INCREMENT PRIMARY KEY, " +
                "reponse TEXT, " +
                "score INT);";
        statement.executeUpdate(createReponseQuery);

        String createTypeDevoirQuery = "CREATE TABLE IF NOT EXISTS TypeDevoir (" +
                "idTypeDevoir INT AUTO_INCREMENT PRIMARY KEY, " +
                "nomTypeDevoir VARCHAR(255) NOT NULL);";
        statement.executeUpdate(createTypeDevoirQuery);

        String createTypeNumeroQuery = "CREATE TABLE IF NOT EXISTS TypeNumero (" +
                "idTypeNumero INT AUTO_INCREMENT PRIMARY KEY, " +
                "nomTypeNumero VARCHAR(255) NOT NULL);";
        statement.executeUpdate(createTypeNumeroQuery);
    }

    public static void insertTypeDevoirData(Statement statement) {
        TypeDevoir[] typeDevoirs = TypeDevoir.values();

        try {
            for (TypeDevoir typeDevoir : typeDevoirs) {
                String insertQuery = "INSERT INTO TypeDevoir (nomTypeDevoir) VALUES ('" + typeDevoir.getNomDevoir() + "');";
                statement.executeUpdate(insertQuery);
            }
        } catch (Exception e) {
            System.err.println("Error inserting TypeDevoir data: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void insertTypeNumeroData(Statement statement) {
        TypeNumero[] typeNumeros = TypeNumero.values();

        try {
            for (TypeNumero typeNumero : typeNumeros) {
                String insertQuery = "INSERT INTO TypeNumero (nomTypeNumero) VALUES ('" + typeNumero.getValue() + "');";
                statement.executeUpdate(insertQuery);
            }
        } catch (Exception e) {
            System.err.println("Error inserting TypeNumero data: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

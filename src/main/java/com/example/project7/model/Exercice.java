package com.example.project7.model;

import sql_connection.SqlConnection;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

/**
 * Classe représentant un exercice contenant plusieurs questions
 */
public class Exercice {
    private int idExercice;
    private int numero;
    private String titre;
    private String consigne;
    private double bareme;
    private int controleID;
    private List<Section> sections; // Les questions (sections) de cet exercice

    // ⭐ NOUVEAU : Cache pour le nombre de questions
    private Integer nombreQuestionsCache = null;

    public Exercice() {
        this.sections = new ArrayList<>();
        this.consigne = "";
        this.bareme = 0.0;
    }

    public Exercice(int numero, String titre) {
        this();
        this.numero = numero;
        this.titre = titre;
    }

    public Exercice(int idExercice, int numero, String titre, String consigne, double bareme, int controleID) {
        this();
        this.idExercice = idExercice;
        this.numero = numero;
        this.titre = titre;
        this.consigne = consigne;
        this.bareme = bareme;
        this.controleID = controleID;
    }

    // ==================== GETTERS ET SETTERS ====================

    public int getIdExercice() {
        return idExercice;
    }

    public void setIdExercice(int idExercice) {
        this.idExercice = idExercice;
    }

    public int getNumero() {
        return numero;
    }

    public void setNumero(int numero) {
        this.numero = numero;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getConsigne() {
        return consigne;
    }

    public void setConsigne(String consigne) {
        this.consigne = consigne;
    }

    public double getBareme() {
        return bareme;
    }

    public void setBareme(double bareme) {
        this.bareme = bareme;
    }

    public int getControleID() {
        return controleID;
    }

    public void setControleID(int controleID) {
        this.controleID = controleID;
    }

    public List<Section> getSections() {
        return sections;
    }

    // ==================== GESTION DES SECTIONS ====================

    /**
     * Ajoute une section à l'exercice
     * Invalide automatiquement le cache du nombre de questions
     */
    public void addSection(Section section) {
        if (!sections.contains(section)) {
            sections.add(section);
            invaliderCache(); // ⭐ Invalider le cache après ajout
        }
    }

    /**
     * Supprime une section de l'exercice
     * Invalide automatiquement le cache du nombre de questions
     */
    public void removeSection(Section section) {
        sections.remove(section);
        invaliderCache(); // ⭐ Invalider le cache après suppression
    }

    /**
     * Retourne le nombre de sections en mémoire
     */
    public int getNombreSections() {
        return sections.size();
    }

    // ==================== COMPTAGE DES QUESTIONS ====================

    /**
     * ⭐ Calcule le nombre de questions depuis la base de données (avec cache)
     * Utilisé pour l'affichage dans les ComboBox
     *
     * Le cache améliore les performances en évitant des requêtes SQL répétées
     * lors de l'affichage des exercices dans les listes déroulantes.
     *
     * @return Le nombre de questions (sections) associées à cet exercice
     */
    public int getNombreQuestions() {
        // Si déjà calculé, retourner le cache
        if (nombreQuestionsCache != null) {
            return nombreQuestionsCache;
        }

        // Si l'exercice n'a pas d'ID, il n'est pas encore en base
        if (idExercice == 0) {
            nombreQuestionsCache = sections.size();
            return nombreQuestionsCache;
        }

        // Calculer depuis la base de données
        String query = "SELECT COUNT(*) FROM Section WHERE exerciceID = ?";

        try (Connection conn = SqlConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setInt(1, idExercice);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                nombreQuestionsCache = rs.getInt(1);
                return nombreQuestionsCache;
            }

        } catch (Exception e) {
            System.err.println("⚠️ Erreur lors du comptage des questions : " + e.getMessage());
        }

        // En cas d'erreur, utiliser la taille de la liste en mémoire
        nombreQuestionsCache = sections.size();
        return nombreQuestionsCache;
    }

    /**
     * ⭐ Force le recalcul du nombre de questions au prochain appel
     *
     * Cette méthode doit être appelée après chaque modification du nombre de questions :
     * - Après l'ajout d'une nouvelle question (createSection)
     * - Après la suppression d'une question
     * - Après le chargement de questions depuis la base de données
     *
     * Elle est automatiquement appelée par addSection() et removeSection()
     */
    public void invaliderCache() {
        nombreQuestionsCache = null;
    }

    // ==================== MÉTHODES STANDARD ====================

    /**
     * Représentation textuelle de l'exercice
     * Format : "Exercice N : Titre (X question(s))"
     *
     * @return La représentation textuelle avec le nombre de questions
     */
    @Override
    public String toString() {
        int nbQuestions = getNombreQuestions();

        String result = "Exercice " + numero;

        if (titre != null && !titre.trim().isEmpty()) {
            result += " : " + titre;
        }

        result += " (" + nbQuestions + " question" + (nbQuestions > 1 ? "s" : "") + ")";

        return result;
    }

    /**
     * Comparaison basée sur l'ID de l'exercice
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Exercice exercice = (Exercice) obj;
        return idExercice == exercice.idExercice;
    }

    /**
     * Hash code basé sur l'ID de l'exercice
     */
    @Override
    public int hashCode() {
        return Integer.hashCode(idExercice);
    }
}
package com.example.project7.model;

public class TableRow {
    private boolean isExercice;      // true = ligne exercice, false = ligne question
    private Integer exerciceNumero;
    private String exerciceTitre;
    private int nbQuestions;

    // Pour les questions
    private String idSection;
    private String type;
    private String question;
    private int ordre;

    // Constructeur pour EXERCICE
    public TableRow(int exerciceNumero, String titre, int nbQuestions) {
        this.isExercice = true;
        this.exerciceNumero = exerciceNumero;
        this.exerciceTitre = titre;
        this.nbQuestions = nbQuestions;
    }

    // Constructeur pour QUESTION
    public TableRow(String idSection, String type, String question, int ordre, int exerciceNumero) {
        this.isExercice = false;
        this.idSection = idSection;
        this.type = type;
        this.question = question;
        this.ordre = ordre;
        this.exerciceNumero = exerciceNumero;
    }

    public TableRow() {

    }

    // Getters
    public boolean isExercice() { return isExercice; }
    public Integer getExerciceNumero() { return exerciceNumero; }
    public String getExerciceTitre() { return exerciceTitre; }
    public int getNbQuestions() { return nbQuestions; }
    public String getIdSection() { return idSection; }
    public String getType() { return type; }
    public String getQuestion() { return question; }
    public int getOrdre() { return ordre; }

    // Setters
    public void setExerciceNumero(Integer exerciceNumero) { this.exerciceNumero = exerciceNumero; }
    public void setOrdre(int ordre) { this.ordre = ordre; }
}
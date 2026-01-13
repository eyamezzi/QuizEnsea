package com.example.project7.model;

public class Theme {
    private int idTheme;
    private String nomTheme;
    private String couleur;

    // Constructeurs
    public Theme() {}
    
    public Theme(int idTheme, String nomTheme, String couleur) {
        this.idTheme = idTheme;
        this.nomTheme = nomTheme;
        this.couleur = couleur;
    }

    // Getters et Setters
    public int getIdTheme() {
        return idTheme;
    }

    public void setIdTheme(int idTheme) {
        this.idTheme = idTheme;
    }

    public String getNomTheme() {
        return nomTheme;
    }

    public void setNomTheme(String nomTheme) {
        this.nomTheme = nomTheme;
    }

    public String getCouleur() {
        return couleur;
    }

    public void setCouleur(String couleur) {
        this.couleur = couleur;
    }

    @Override
    public String toString() {
        return nomTheme;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Theme theme = (Theme) obj;
        return idTheme == theme.idTheme;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(idTheme);
    }
}
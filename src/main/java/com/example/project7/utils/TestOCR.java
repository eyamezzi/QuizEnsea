package com.example.project7.utils;

import java.io.File;

public class TestOCR {
    public static void main(String[] args) {
        try {
            OCRExtractor ocr = new OCRExtractor();

            // ⚠️ CHANGE CE CHEMIN VERS UNE VRAIE IMAGE SUR TON PC
            File imageTest = new File("C:/Users/mezzi/Downloads/test.png");

            if (!imageTest.exists()) {
                System.err.println("❌ Fichier introuvable : " + imageTest.getAbsolutePath());
                System.out.println("ℹ️ Crée une image avec du texte et mets le bon chemin !");
                return;
            }

            String texteExtrait = ocr.extraireTexteImage(imageTest);

            System.out.println("\n📄 ===== RÉSULTAT OCR =====");
            System.out.println(texteExtrait);
            System.out.println("==========================\n");

            if (texteExtrait.length() < 10) {
                System.out.println("⚠️ Peu de texte extrait. Vérifie que l'image contient du texte lisible.");
            } else {
                System.out.println("✅ OCR fonctionne correctement !");
            }

        } catch (Exception e) {
            System.err.println("❌ Erreur OCR :");
            e.printStackTrace();
        }
    }
}
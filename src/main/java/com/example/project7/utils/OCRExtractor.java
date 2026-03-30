package com.example.project7.utils;

import net.sourceforge.tess4j.*;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.rendering.*;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.net.http.*;
import java.net.URI;
import java.util.Base64;
import java.nio.file.Files;

public class OCRExtractor {

    private static final String CLAUDE_API_KEY = "VOTRE_CLE_API_ICI";
    private static final boolean UTILISER_CLAUDE = false; // ← mettre true si tu as une clé API

    public OCRExtractor() {
        // Tesseract gardé pour extraireTexteImage uniquement
    }

    /**
     * Extrait le texte d'une image (fichier PNG/JPG)
     */
    public String extraireTexteImage(File imageFile) throws TesseractException {
        System.out.println("📸 OCR sur image : " + imageFile.getName());
        Tesseract tesseract = creerTesseract();
        String texte = tesseract.doOCR(imageFile);
        System.out.println("✅ Texte extrait (" + texte.length() + " caractères)");
        return nettoyerTexte(texte);
    }

    /**
     * Extrait le texte d'un PDF via Tesseract CLI
     */
    public String extraireTextePDF(File pdfFile) throws Exception {
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage fullImage = renderer.renderImageWithDPI(0, 600, ImageType.RGB);

            int h = fullImage.getHeight();
            int w = fullImage.getWidth();

            int startY = (int) (h * 0.50);
            int cropH  = (int) (h * 0.40);

            // ✅ Copier le crop dans un nouveau BufferedImage (évite le bug ImageIO)
            BufferedImage zone = new BufferedImage((int)(w * 0.90), cropH, BufferedImage.TYPE_INT_RGB);
            zone.getGraphics().drawImage(
                    fullImage.getSubimage((int)(w * 0.05), startY, (int)(w * 0.90), cropH),
                    0, 0, null
            );

            // ✅ Écrire via FileOutputStream explicite
            File tempInput = new File(System.getProperty("java.io.tmpdir"),
                    "ocr_" + System.currentTimeMillis() + ".png");

            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempInput)) {
                javax.imageio.ImageIO.write(zone, "png", fos);
            }

            System.out.println("✅ PNG écrit : " + tempInput.exists() + " / " + tempInput.length() + " bytes");

            String scriptPath = "C:/Users/mezzi/QuizEnsea/src/ocr_script.py";
            ProcessBuilder pb = new ProcessBuilder(
                    "python", scriptPath, tempInput.getAbsolutePath()
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();
            String texte = new String(process.getInputStream().readAllBytes());
            process.waitFor();

            tempInput.delete();
            System.out.println("✅ EasyOCR résultat: [" + texte + "]");
            return nettoyerTexte(texte);
        }
    }

    /**
     * Améliore le contraste pour annotations manuscrites PDF
     * (crayon PC, outil dessin → traits gris/bleus qu'on force en noir)
     */
    private BufferedImage ameliorerContraste(BufferedImage original) {
        int w = original.getWidth();
        int h = original.getHeight();
        BufferedImage result = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int rgb  = original.getRGB(x, y);
                int r    = (rgb >> 16) & 0xFF;
                int g    = (rgb >> 8)  & 0xFF;
                int b    =  rgb        & 0xFF;
                int gray = (r + g + b) / 3;

                // ✅ Seuil 240 : capture les traits légers du crayon PC
                result.setRGB(x, y, gray < 240 ? 0x000000 : 0xFFFFFF);
            }
        }
        return result;
    }

    /**
     * Détecte si Tesseract a mal lu (trop peu de mots reconnus)
     */
    private boolean estLectureManuscriteMauvaise(String texte) {
        if (texte == null || texte.trim().length() < 3) return true;
        long motsReels = java.util.Arrays.stream(texte.split("\\s+"))
                .filter(m -> m.matches("[a-zA-ZàâäéèêëïîôùûüçÀÂÄÉÈÊËÏÎÔÙÛÜÇ]{2,}"))
                .count();
        return motsReels < 2; // moins de 2 vrais mots → mauvaise lecture
    }

    /**
     * Utilise Claude Vision API pour lire l'écriture manuscrite
     * (activer avec UTILISER_CLAUDE = true + clé API valide)
     */
    private String lireManuscritAvecClaude(File pdfFile) throws Exception {
        // Recapturer la zone de réponse uniquement
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage fullImage = renderer.renderImageWithDPI(0, 300, ImageType.RGB);

            int h = fullImage.getHeight();
            int w = fullImage.getWidth();
            int startY = (int) (h * 0.72);
            int cropH  = (int) (h * 0.23);
            BufferedImage zone = fullImage.getSubimage(
                    (int)(w * 0.05), startY, (int)(w * 0.90), cropH);

            File cropFile = new File(System.getProperty("java.io.tmpdir"), "ocr_claude_zone.png");
            ImageIO.write(zone, "png", cropFile);

            String base64 = Base64.getEncoder().encodeToString(Files.readAllBytes(cropFile.toPath()));
            cropFile.delete();

            String requestBody = """
                {
                    "model": "claude-opus-4-5",
                    "max_tokens": 200,
                    "messages": [{
                        "role": "user",
                        "content": [
                            {
                                "type": "image",
                                "source": {
                                    "type": "base64",
                                    "media_type": "image/png",
                                    "data": "%s"
                                }
                            },
                            {
                                "type": "text",
                                "text": "Transcris UNIQUEMENT le texte manuscrit visible dans cette image. Réponds seulement avec le texte transcrit, sans explication. Si aucune écriture manuscrite, réponds VIDE."
                            }
                        ]
                    }]
                }
                """.formatted(base64);

            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://api.anthropic.com/v1/messages"))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", CLAUDE_API_KEY)
                    .header("anthropic-version", "2023-06-01")
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = client.send(request,
                    HttpResponse.BodyHandlers.ofString());

            System.out.println("Claude status: " + response.statusCode());

            // Parser la réponse JSON simplement
            String body = response.body();
            int start = body.indexOf("\"text\":\"") + 8;
            int end   = body.indexOf("\"", start);
            if (start > 8 && end > start) {
                String result = body.substring(start, end)
                        .replace("\\n", "\n")
                        .replace("\\\"", "\"");
                System.out.println("✅ Claude transcription: [" + result + "]");
                return result;
            }

            return nettoyerTexte(body);
        }
    }

    /**
     * Crée une instance Tesseract configurée
     */
    private Tesseract creerTesseract() {
        Tesseract t = new Tesseract();
        String[] paths = {
                "C:/Program Files/Tesseract-OCR/tessdata",
                "C:/Program Files (x86)/Tesseract-OCR/tessdata",
                System.getenv("TESSDATA_PREFIX")
        };
        for (String path : paths) {
            if (path != null && new File(path).exists()) {
                t.setDatapath(path);
                break;
            }
        }
        t.setLanguage("fra+eng");
        t.setPageSegMode(6);
        t.setOcrEngineMode(1);
        return t;
    }

    /**
     * Nettoie le texte OCR
     */
    private String nettoyerTexte(String texte) {
        return texte
                .replaceAll("\\s+", " ")
                .replaceAll("[|\\[\\]{}]", "")
                .trim();
    }
}
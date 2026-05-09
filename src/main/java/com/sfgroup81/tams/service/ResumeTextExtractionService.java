package com.sfgroup81.tams.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

public class ResumeTextExtractionService {
    public String extract(Path filePath) {
        if (filePath == null || Files.notExists(filePath)) {
            return "";
        }
        String extension = extension(filePath);
        try {
            return switch (extension) {
                case "pdf" -> extractPdf(filePath);
                case "doc" -> extractDoc(filePath);
                case "docx" -> extractDocx(filePath);
                default -> extractPlain(filePath);
            };
        } catch (Exception ex) {
            return "";
        }
    }

    private String extractPdf(Path filePath) throws IOException {
        try (PDDocument document = Loader.loadPDF(filePath.toFile())) {
            return new PDFTextStripper().getText(document);
        }
    }

    private String extractDoc(Path filePath) throws IOException {
        try (InputStream stream = Files.newInputStream(filePath);
             HWPFDocument document = new HWPFDocument(stream);
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractDocx(Path filePath) throws IOException {
        try (InputStream stream = Files.newInputStream(filePath);
             XWPFDocument document = new XWPFDocument(stream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractPlain(Path filePath) throws IOException {
        byte[] bytes = Files.readAllBytes(filePath);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private String extension(Path filePath) {
        String name = filePath.getFileName().toString();
        int dot = name.lastIndexOf('.');
        return dot < 0 ? "" : name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}

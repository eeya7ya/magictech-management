package com.magictech.modules.sales.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.magictech.modules.sales.entity.QuotationDesign;
import com.magictech.modules.sales.repository.QuotationDesignRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service for QuotationDesign operations.
 * Handles PDF storage, editing, versioning, and annotation management.
 */
@Service
@Transactional
public class QuotationDesignService {

    @Autowired
    private QuotationDesignRepository repository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ==================== CRUD Operations ====================

    /**
     * Get current version of quotation design for an entity
     */
    public Optional<QuotationDesign> getCurrentVersion(String entityType, Long entityId) {
        return repository.findCurrentVersion(entityType, entityId);
    }

    /**
     * Get all versions for an entity (version history)
     */
    public List<QuotationDesign> getVersionHistory(String entityType, Long entityId) {
        return repository.findByEntityTypeAndEntityIdAndActiveTrueOrderByVersionDesc(entityType, entityId);
    }

    /**
     * Get specific version
     */
    public Optional<QuotationDesign> getVersion(String entityType, Long entityId, Integer version) {
        return repository.findByVersion(entityType, entityId, version);
    }

    /**
     * Upload new PDF (creates new version if exists)
     */
    public QuotationDesign uploadPdf(String entityType, Long entityId, byte[] pdfData,
                                      String filename, String username, String moduleSource) {
        // Check if current version exists
        Optional<QuotationDesign> existingOpt = getCurrentVersion(entityType, entityId);

        int newVersion = 1;
        Long parentVersionId = null;

        if (existingOpt.isPresent()) {
            QuotationDesign existing = existingOpt.get();
            // Mark old version as not current
            existing.setIsCurrentVersion(false);
            existing.setUpdatedBy(username);
            repository.save(existing);

            newVersion = existing.getVersion() + 1;
            parentVersionId = existing.getId();
        }

        // Create new version
        QuotationDesign quotation = new QuotationDesign(entityType, entityId);
        quotation.setPdfData(pdfData);
        quotation.setOriginalPdfData(pdfData); // Keep backup
        quotation.setFilename(filename);
        quotation.setFileSize((long) pdfData.length);
        quotation.setMimeType("application/pdf");
        quotation.setVersion(newVersion);
        quotation.setParentVersionId(parentVersionId);
        quotation.setIsCurrentVersion(true);
        quotation.setModuleSource(moduleSource);
        quotation.setCreatedBy(username);
        quotation.setVersionNote("Uploaded new PDF");

        // Get page count
        try {
            quotation.setPageCount(getPageCount(pdfData));
        } catch (IOException e) {
            quotation.setPageCount(0);
        }

        return repository.save(quotation);
    }

    /**
     * Save annotations for current version
     */
    public QuotationDesign saveAnnotations(Long quotationId, String annotationsJson, String username) {
        QuotationDesign quotation = repository.findById(quotationId)
                .orElseThrow(() -> new RuntimeException("Quotation not found: " + quotationId));

        quotation.setPdfAnnotations(annotationsJson);
        quotation.setUpdatedBy(username);

        return repository.save(quotation);
    }

    /**
     * Create new version with updated annotations (preserves history)
     * Annotations are burned into the PDF data for persistence.
     */
    public QuotationDesign createVersionWithAnnotations(String entityType, Long entityId,
                                                         String annotationsJson, String versionNote,
                                                         String username) {
        Optional<QuotationDesign> currentOpt = getCurrentVersion(entityType, entityId);

        if (currentOpt.isEmpty()) {
            throw new RuntimeException("No existing quotation found for entity");
        }

        QuotationDesign current = currentOpt.get();

        // Mark current as not current
        current.setIsCurrentVersion(false);
        current.setUpdatedBy(username);
        repository.save(current);

        // Burn annotations into PDF data so they are permanently stored
        byte[] pdfDataWithAnnotations;
        try {
            // Always use the original PDF as base and burn all annotations into it
            byte[] basePdf = current.getOriginalPdfData() != null
                    ? current.getOriginalPdfData()
                    : current.getPdfData();
            pdfDataWithAnnotations = generatePdfWithAnnotations(basePdf, annotationsJson);
        } catch (IOException e) {
            // If burning fails, fall back to original PDF data
            pdfDataWithAnnotations = current.getPdfData();
        }

        // Create new version
        QuotationDesign newVersion = new QuotationDesign(entityType, entityId);
        newVersion.setPdfData(pdfDataWithAnnotations);  // PDF with annotations burned in
        newVersion.setOriginalPdfData(current.getOriginalPdfData());  // Keep original for reset
        newVersion.setPdfAnnotations(annotationsJson);  // Also keep JSON for editing
        newVersion.setFilename(current.getFilename());
        newVersion.setFileSize((long) pdfDataWithAnnotations.length);
        newVersion.setMimeType(current.getMimeType());
        newVersion.setPageCount(current.getPageCount());
        newVersion.setVersion(current.getVersion() + 1);
        newVersion.setParentVersionId(current.getId());
        newVersion.setIsCurrentVersion(true);
        newVersion.setModuleSource(current.getModuleSource());
        newVersion.setCreatedBy(username);
        newVersion.setVersionNote(versionNote != null ? versionNote : "Updated annotations");

        return repository.save(newVersion);
    }

    /**
     * Restore a previous version as current
     */
    public QuotationDesign restoreVersion(String entityType, Long entityId, Integer versionToRestore, String username) {
        // Get version to restore
        QuotationDesign toRestore = repository.findByVersion(entityType, entityId, versionToRestore)
                .orElseThrow(() -> new RuntimeException("Version not found: " + versionToRestore));

        // Get current version
        Optional<QuotationDesign> currentOpt = getCurrentVersion(entityType, entityId);
        int newVersionNumber = 1;
        Long parentVersionId = null;

        if (currentOpt.isPresent()) {
            QuotationDesign current = currentOpt.get();
            current.setIsCurrentVersion(false);
            current.setUpdatedBy(username);
            repository.save(current);
            newVersionNumber = current.getVersion() + 1;
            parentVersionId = current.getId();
        }

        // Create new version from restored data
        QuotationDesign restored = new QuotationDesign(entityType, entityId);
        restored.setPdfData(toRestore.getPdfData());
        restored.setOriginalPdfData(toRestore.getOriginalPdfData());
        restored.setPdfAnnotations(toRestore.getPdfAnnotations());
        restored.setFilename(toRestore.getFilename());
        restored.setFileSize(toRestore.getFileSize());
        restored.setMimeType(toRestore.getMimeType());
        restored.setPageCount(toRestore.getPageCount());
        restored.setVersion(newVersionNumber);
        restored.setParentVersionId(parentVersionId);
        restored.setIsCurrentVersion(true);
        restored.setModuleSource(toRestore.getModuleSource());
        restored.setCreatedBy(username);
        restored.setVersionNote("Restored from version " + versionToRestore);

        return repository.save(restored);
    }

    /**
     * Delete quotation (soft delete all versions)
     */
    public void deleteQuotation(String entityType, Long entityId, String username) {
        List<QuotationDesign> versions = getVersionHistory(entityType, entityId);
        for (QuotationDesign version : versions) {
            version.setActive(false);
            version.setUpdatedBy(username);
            repository.save(version);
        }
    }

    /**
     * Reset to original PDF (remove all annotations)
     */
    public QuotationDesign resetToOriginal(String entityType, Long entityId, String username) {
        Optional<QuotationDesign> currentOpt = getCurrentVersion(entityType, entityId);

        if (currentOpt.isEmpty()) {
            throw new RuntimeException("No quotation found");
        }

        QuotationDesign current = currentOpt.get();

        if (current.getOriginalPdfData() == null) {
            throw new RuntimeException("No original PDF data available");
        }

        // Mark current as not current
        current.setIsCurrentVersion(false);
        current.setUpdatedBy(username);
        repository.save(current);

        // Create new version with original PDF and no annotations
        QuotationDesign reset = new QuotationDesign(current.getEntityType(), current.getEntityId());
        reset.setPdfData(current.getOriginalPdfData());
        reset.setOriginalPdfData(current.getOriginalPdfData());
        reset.setPdfAnnotations(null);
        reset.setFilename(current.getFilename());
        reset.setFileSize((long) current.getOriginalPdfData().length);
        reset.setMimeType(current.getMimeType());
        reset.setPageCount(current.getPageCount());
        reset.setVersion(current.getVersion() + 1);
        reset.setParentVersionId(current.getId());
        reset.setIsCurrentVersion(true);
        reset.setModuleSource(current.getModuleSource());
        reset.setCreatedBy(username);
        reset.setVersionNote("Reset to original PDF");

        return repository.save(reset);
    }

    // ==================== PDF Rendering ====================

    /**
     * Render PDF page as image for preview
     */
    public BufferedImage renderPage(byte[] pdfData, int pageIndex, float dpi) throws IOException {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfData))) {
            PDFRenderer renderer = new PDFRenderer(document);
            return renderer.renderImageWithDPI(pageIndex, dpi);
        }
    }

    /**
     * Render PDF page as PNG bytes
     */
    public byte[] renderPageAsPng(byte[] pdfData, int pageIndex, float dpi) throws IOException {
        BufferedImage image = renderPage(pdfData, pageIndex, dpi);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    /**
     * Get PDF page count
     */
    public int getPageCount(byte[] pdfData) throws IOException {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfData))) {
            return document.getNumberOfPages();
        }
    }

    /**
     * Get PDF page dimensions
     */
    public PDRectangle getPageDimensions(byte[] pdfData, int pageIndex) throws IOException {
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfData))) {
            PDPage page = document.getPage(pageIndex);
            return page.getMediaBox();
        }
    }

    // ==================== PDF Export with Annotations ====================

    /**
     * Generate PDF with annotations burned in
     */
    public byte[] generatePdfWithAnnotations(Long quotationId) throws IOException {
        QuotationDesign quotation = repository.findById(quotationId)
                .orElseThrow(() -> new RuntimeException("Quotation not found: " + quotationId));

        return generatePdfWithAnnotations(quotation.getPdfData(), quotation.getPdfAnnotations());
    }

    // DPI used for rendering PDF preview (must match QuotationDesignEditorPanel.RENDER_DPI)
    private static final float RENDER_DPI = 150f;
    // PDF standard is 72 points per inch
    private static final float PDF_DPI = 72f;

    /**
     * Generate PDF with annotations burned in (from raw data)
     */
    public byte[] generatePdfWithAnnotations(byte[] pdfData, String annotationsJson) throws IOException {
        if (annotationsJson == null || annotationsJson.isEmpty() || annotationsJson.equals("[]")) {
            return pdfData; // No annotations, return original
        }

        List<Map<String, Object>> annotations;
        try {
            annotations = objectMapper.readValue(annotationsJson, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return pdfData; // Invalid JSON, return original
        }

        // Scale factor to convert from rendered image pixels to PDF points
        // Image is rendered at RENDER_DPI, PDF uses 72 points per inch
        float scaleFactor = PDF_DPI / RENDER_DPI;

        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfData))) {
            for (Map<String, Object> annotation : annotations) {
                int pageNum = ((Number) annotation.getOrDefault("page", 0)).intValue();
                float x = ((Number) annotation.getOrDefault("x", 0)).floatValue();
                float y = ((Number) annotation.getOrDefault("y", 0)).floatValue();
                String text = (String) annotation.getOrDefault("text", "");
                int fontSize = ((Number) annotation.getOrDefault("fontSize", 12)).intValue();
                String fontFamily = (String) annotation.getOrDefault("fontFamily", "Helvetica");
                String color = (String) annotation.getOrDefault("color", "#000000");
                boolean bold = (Boolean) annotation.getOrDefault("bold", false);
                boolean italic = (Boolean) annotation.getOrDefault("italic", false);
                boolean underline = (Boolean) annotation.getOrDefault("underline", false);

                // Background properties
                Boolean bgEnabled = (Boolean) annotation.getOrDefault("bgEnabled", false);
                String bgColor = (String) annotation.getOrDefault("bgColor", "#FFFFFF");
                Number bgOpacity = (Number) annotation.getOrDefault("bgOpacity", 80);

                if (pageNum >= 0 && pageNum < document.getNumberOfPages()) {
                    PDPage page = document.getPage(pageNum);
                    PDRectangle mediaBox = page.getMediaBox();

                    // Convert from image coordinates (pixels at RENDER_DPI) to PDF coordinates (points at 72 DPI)
                    // Also convert Y-axis: image origin is top-left, PDF origin is bottom-left
                    float pdfX = x * scaleFactor;
                    float pdfY = mediaBox.getHeight() - (y * scaleFactor);

                    PDFont font = getFont(fontFamily, bold, italic);

                    // Handle multi-line text - calculate total height and max width for background
                    String[] lines = text.split("\n");
                    float lineHeight = fontSize * 1.2f;
                    float totalHeight = lines.length * lineHeight;
                    float maxWidth = 0;

                    // Calculate max width for background
                    for (String line : lines) {
                        try {
                            float lineWidth = font.getStringWidth(line) / 1000 * fontSize;
                            maxWidth = Math.max(maxWidth, lineWidth);
                        } catch (Exception ignored) {
                            maxWidth = Math.max(maxWidth, line.length() * fontSize * 0.6f);
                        }
                    }

                    try (PDPageContentStream contentStream = new PDPageContentStream(
                            document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

                        // Draw background rectangle if enabled
                        if (bgEnabled != null && bgEnabled && bgColor != null) {
                            float opacity = bgOpacity != null ? bgOpacity.floatValue() / 100f : 0.8f;
                            Color bg = Color.decode(bgColor);

                            // Save graphics state for transparency
                            contentStream.saveGraphicsState();

                            // Set background color with opacity approximation
                            // Note: Full transparency support requires ExtGState which is more complex
                            int r = (int) (bg.getRed() + (255 - bg.getRed()) * (1 - opacity));
                            int g = (int) (bg.getGreen() + (255 - bg.getGreen()) * (1 - opacity));
                            int b = (int) (bg.getBlue() + (255 - bg.getBlue()) * (1 - opacity));
                            contentStream.setNonStrokingColor(new Color(r, g, b));

                            // Draw background rectangle
                            float padding = 3;
                            contentStream.addRect(
                                    pdfX - padding,
                                    pdfY - totalHeight - padding + fontSize,
                                    maxWidth + (padding * 2),
                                    totalHeight + (padding * 2)
                            );
                            contentStream.fill();

                            contentStream.restoreGraphicsState();
                        }

                        // Set font (fontSize is in points, no scaling needed)
                        contentStream.setFont(font, fontSize);

                        // Set text color
                        Color textColor = Color.decode(color);
                        contentStream.setNonStrokingColor(textColor);

                        // Draw text at scaled position
                        contentStream.beginText();
                        contentStream.newLineAtOffset(pdfX, pdfY);

                        float currentY = pdfY;
                        for (int i = 0; i < lines.length; i++) {
                            if (i > 0) {
                                contentStream.newLineAtOffset(0, -lineHeight);
                                currentY -= lineHeight;
                            }
                            contentStream.showText(lines[i]);

                            // Draw underline for this line
                            if (underline && !lines[i].isEmpty()) {
                                float lineWidth;
                                try {
                                    lineWidth = font.getStringWidth(lines[i]) / 1000 * fontSize;
                                } catch (Exception ignored) {
                                    lineWidth = lines[i].length() * fontSize * 0.6f;
                                }

                                // We need to end text to draw the line, then resume
                                contentStream.endText();

                                // Draw underline
                                contentStream.setStrokingColor(textColor);
                                contentStream.setLineWidth(fontSize * 0.05f);
                                float underlineY = currentY - fontSize * 0.15f;
                                contentStream.moveTo(pdfX, underlineY);
                                contentStream.lineTo(pdfX + lineWidth, underlineY);
                                contentStream.stroke();

                                // Resume text mode for next line
                                contentStream.beginText();
                                contentStream.setFont(font, fontSize);
                                contentStream.newLineAtOffset(pdfX, currentY);
                            }
                        }

                        contentStream.endText();
                    }
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        }
    }

    /**
     * Get appropriate font based on family, bold, italic
     * Uses PDFBox 2.x static font constants
     *
     * Maps various font names to the 14 standard PDF fonts:
     * - Helvetica family (includes Arial, Verdana, Tahoma, Trebuchet MS)
     * - Times family (includes Times New Roman, Georgia)
     * - Courier family (includes Courier New)
     * - Symbol and ZapfDingbats for special characters
     */
    private PDFont getFont(String fontFamily, boolean bold, boolean italic) {
        if (fontFamily == null) {
            fontFamily = "Helvetica";
        }

        String family = fontFamily.toLowerCase().trim();

        // Sans-serif fonts -> Helvetica
        if (family.contains("helvetica") || family.contains("arial") ||
            family.contains("verdana") || family.contains("tahoma") ||
            family.contains("trebuchet") || family.contains("impact") ||
            family.contains("comic sans")) {
            if (bold && italic) {
                return PDType1Font.HELVETICA_BOLD_OBLIQUE;
            } else if (bold) {
                return PDType1Font.HELVETICA_BOLD;
            } else if (italic) {
                return PDType1Font.HELVETICA_OBLIQUE;
            } else {
                return PDType1Font.HELVETICA;
            }
        }

        // Serif fonts -> Times
        if (family.contains("times") || family.contains("georgia") ||
            family.contains("palatino") || family.contains("garamond") ||
            family.contains("book antiqua") || family.contains("century")) {
            if (bold && italic) {
                return PDType1Font.TIMES_BOLD_ITALIC;
            } else if (bold) {
                return PDType1Font.TIMES_BOLD;
            } else if (italic) {
                return PDType1Font.TIMES_ITALIC;
            } else {
                return PDType1Font.TIMES_ROMAN;
            }
        }

        // Monospace fonts -> Courier
        if (family.contains("courier") || family.contains("consolas") ||
            family.contains("monaco") || family.contains("lucida console") ||
            family.contains("monospace") || family.contains("andale mono")) {
            if (bold && italic) {
                return PDType1Font.COURIER_BOLD_OBLIQUE;
            } else if (bold) {
                return PDType1Font.COURIER_BOLD;
            } else if (italic) {
                return PDType1Font.COURIER_OBLIQUE;
            } else {
                return PDType1Font.COURIER;
            }
        }

        // Symbol fonts
        if (family.contains("symbol")) {
            return PDType1Font.SYMBOL;
        }

        if (family.contains("zapf") || family.contains("dingbat") || family.contains("wingding")) {
            return PDType1Font.ZAPF_DINGBATS;
        }

        // Default to Helvetica for unknown fonts
        if (bold && italic) {
            return PDType1Font.HELVETICA_BOLD_OBLIQUE;
        } else if (bold) {
            return PDType1Font.HELVETICA_BOLD;
        } else if (italic) {
            return PDType1Font.HELVETICA_OBLIQUE;
        } else {
            return PDType1Font.HELVETICA;
        }
    }

    // ==================== Annotation Helpers ====================

    /**
     * Parse annotations JSON to list
     */
    public List<Map<String, Object>> parseAnnotations(String annotationsJson) {
        if (annotationsJson == null || annotationsJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(annotationsJson, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            return new ArrayList<>();
        }
    }

    /**
     * Convert annotations list to JSON
     */
    public String annotationsToJson(List<Map<String, Object>> annotations) {
        try {
            return objectMapper.writeValueAsString(annotations);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    /**
     * Add annotation to existing annotations
     */
    public String addAnnotation(String existingJson, Map<String, Object> newAnnotation) {
        List<Map<String, Object>> annotations = parseAnnotations(existingJson);
        annotations.add(newAnnotation);
        return annotationsToJson(annotations);
    }

    /**
     * Remove annotation by index
     */
    public String removeAnnotation(String existingJson, int index) {
        List<Map<String, Object>> annotations = parseAnnotations(existingJson);
        if (index >= 0 && index < annotations.size()) {
            annotations.remove(index);
        }
        return annotationsToJson(annotations);
    }

    /**
     * Update annotation at index
     */
    public String updateAnnotation(String existingJson, int index, Map<String, Object> updatedAnnotation) {
        List<Map<String, Object>> annotations = parseAnnotations(existingJson);
        if (index >= 0 && index < annotations.size()) {
            annotations.set(index, updatedAnnotation);
        }
        return annotationsToJson(annotations);
    }

    // ==================== Query Methods ====================

    public List<QuotationDesign> findByModuleSource(String moduleSource) {
        return repository.findByModuleSourceAndIsCurrentVersionTrueAndActiveTrue(moduleSource);
    }

    public List<QuotationDesign> findByCreator(String username) {
        return repository.findByCreatedByAndActiveTrue(username);
    }

    public List<QuotationDesign> findAllWithAnnotations() {
        return repository.findAllWithAnnotations();
    }

    public Long countVersions(String entityType, Long entityId) {
        return repository.countVersions(entityType, entityId);
    }
}

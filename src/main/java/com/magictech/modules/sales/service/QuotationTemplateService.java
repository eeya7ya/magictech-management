package com.magictech.modules.sales.service;

import com.magictech.modules.sales.entity.QuotationTemplate;
import com.magictech.modules.sales.repository.QuotationTemplateRepository;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
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
import java.util.List;
import java.util.Optional;

/**
 * QuotationTemplateService - Handles template-based quotation PDF generation.
 *
 * Simple approach:
 * 1. Load background template (PDF or image)
 * 2. Place text at predefined positions
 * 3. Generate final PDF
 *
 * Much simpler than the annotation-based system!
 */
@Service
@Transactional
public class QuotationTemplateService {

    @Autowired
    private QuotationTemplateRepository repository;

    // ==================== Template CRUD ====================

    public List<QuotationTemplate> findAll() {
        return repository.findAllOrderedByDefault();
    }

    public Optional<QuotationTemplate> findById(Long id) {
        return repository.findByIdAndActiveTrue(id);
    }

    public Optional<QuotationTemplate> getDefaultTemplate() {
        return repository.findByIsDefaultTrueAndActiveTrue();
    }

    public QuotationTemplate save(QuotationTemplate template) {
        return repository.save(template);
    }

    public QuotationTemplate createTemplate(String name, byte[] backgroundData, String backgroundType,
                                             String filename, String createdBy) {
        QuotationTemplate template = new QuotationTemplate(name);
        template.setBackgroundData(backgroundData);
        template.setBackgroundType(backgroundType);
        template.setFilename(filename);
        template.setCreatedBy(createdBy);

        // If no default exists, make this the default
        if (repository.findByIsDefaultTrueAndActiveTrue().isEmpty()) {
            template.setIsDefault(true);
        }

        return repository.save(template);
    }

    public void setAsDefault(Long templateId) {
        // Clear existing default
        repository.findByIsDefaultTrueAndActiveTrue().ifPresent(existing -> {
            existing.setIsDefault(false);
            repository.save(existing);
        });

        // Set new default
        repository.findById(templateId).ifPresent(template -> {
            template.setIsDefault(true);
            repository.save(template);
        });
    }

    public void deleteTemplate(Long id) {
        repository.findById(id).ifPresent(template -> {
            template.setActive(false);
            repository.save(template);
        });
    }

    // ==================== PDF Generation ====================

    /**
     * Generate quotation PDF from template with field values.
     *
     * @param templateId Template to use
     * @param toValue    Value for "To:" field
     * @param fromValue  Value for "From:" field
     * @param dateValue  Value for "Date:" field
     * @return Generated PDF as byte array
     */
    public byte[] generateQuotationPdf(Long templateId, String toValue, String fromValue, String dateValue)
            throws IOException {

        QuotationTemplate template = repository.findByIdAndActiveTrue(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));

        return generateQuotationPdf(template, toValue, fromValue, dateValue);
    }

    /**
     * Generate quotation PDF using default template.
     */
    public byte[] generateQuotationPdfWithDefault(String toValue, String fromValue, String dateValue)
            throws IOException {

        QuotationTemplate template = repository.findByIsDefaultTrueAndActiveTrue()
                .orElseThrow(() -> new RuntimeException("No default template configured"));

        return generateQuotationPdf(template, toValue, fromValue, dateValue);
    }

    /**
     * Core PDF generation logic.
     */
    public byte[] generateQuotationPdf(QuotationTemplate template, String toValue, String fromValue, String dateValue)
            throws IOException {

        if (template.getBackgroundData() == null) {
            throw new RuntimeException("Template has no background data");
        }

        PDDocument document;

        if ("PDF".equalsIgnoreCase(template.getBackgroundType())) {
            // Load existing PDF as background
            document = PDDocument.load(new ByteArrayInputStream(template.getBackgroundData()));
        } else {
            // Create new PDF with image as background
            document = createPdfFromImage(template.getBackgroundData());
        }

        try {
            PDPage page = document.getPage(0);
            PDRectangle mediaBox = page.getMediaBox();

            // Get font settings from template
            PDFont labelFont = getFont(template.getFontFamily(), template.getBoldLabels(), false);
            PDFont valueFont = getFont(template.getFontFamily(), false, false);
            int fontSize = template.getFontSize() != null ? template.getFontSize() : 14;
            Color textColor = Color.decode(template.getFontColor() != null ? template.getFontColor() : "#000000");

            try (PDPageContentStream contentStream = new PDPageContentStream(
                    document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

                contentStream.setNonStrokingColor(textColor);

                // Draw "To:" field
                if (toValue != null && !toValue.isEmpty()) {
                    drawLabelAndValue(contentStream, "To:", toValue,
                            template.getToFieldX(), template.getToFieldY(),
                            labelFont, valueFont, fontSize);
                }

                // Draw "From:" field
                if (fromValue != null && !fromValue.isEmpty()) {
                    drawLabelAndValue(contentStream, "From:", fromValue,
                            template.getFromFieldX(), template.getFromFieldY(),
                            labelFont, valueFont, fontSize);
                }

                // Draw "Date:" field
                if (dateValue != null && !dateValue.isEmpty()) {
                    drawLabelAndValue(contentStream, "Date:", dateValue,
                            template.getDateFieldX(), template.getDateFieldY(),
                            labelFont, valueFont, fontSize);
                }
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();

        } finally {
            document.close();
        }
    }

    /**
     * Draw a label and value pair at the specified position.
     */
    private void drawLabelAndValue(PDPageContentStream contentStream, String label, String value,
                                    float x, float y, PDFont labelFont, PDFont valueFont, int fontSize)
            throws IOException {

        contentStream.beginText();
        contentStream.setFont(labelFont, fontSize);
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(label + " ");

        // Calculate label width to position value
        float labelWidth = labelFont.getStringWidth(label + " ") / 1000 * fontSize;

        contentStream.setFont(valueFont, fontSize);
        contentStream.showText(value);
        contentStream.endText();
    }

    /**
     * Create a PDF document from an image.
     */
    private PDDocument createPdfFromImage(byte[] imageData) throws IOException {
        PDDocument document = new PDDocument();

        // Read image to get dimensions
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(imageData));
        if (image == null) {
            throw new IOException("Could not read image data");
        }

        // Create page with image dimensions (convert pixels to points at 72 DPI)
        // Assuming image is at 72 DPI for simplicity
        float width = image.getWidth();
        float height = image.getHeight();

        // Use A4 size if image is larger
        PDRectangle pageSize = new PDRectangle(
                Math.min(width, PDRectangle.A4.getWidth()),
                Math.min(height, PDRectangle.A4.getHeight())
        );

        PDPage page = new PDPage(pageSize);
        document.addPage(page);

        // Add image as background
        PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, imageData, "background");

        try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
            // Scale image to fit page
            float scale = Math.min(
                    pageSize.getWidth() / image.getWidth(),
                    pageSize.getHeight() / image.getHeight()
            );
            float scaledWidth = image.getWidth() * scale;
            float scaledHeight = image.getHeight() * scale;

            // Center image on page
            float x = (pageSize.getWidth() - scaledWidth) / 2;
            float y = (pageSize.getHeight() - scaledHeight) / 2;

            contentStream.drawImage(pdImage, x, y, scaledWidth, scaledHeight);
        }

        return document;
    }

    /**
     * Get appropriate PDFBox font based on family and style.
     */
    private PDFont getFont(String fontFamily, boolean bold, boolean italic) {
        if (fontFamily == null) {
            fontFamily = "Helvetica";
        }

        String family = fontFamily.toLowerCase().trim();

        // Sans-serif fonts -> Helvetica
        if (family.contains("helvetica") || family.contains("arial") ||
                family.contains("verdana") || family.contains("tahoma")) {
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
        if (family.contains("times") || family.contains("georgia")) {
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
        if (family.contains("courier") || family.contains("mono")) {
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

        // Default to Helvetica
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

    // ==================== Preview ====================

    /**
     * Render template preview as PNG.
     */
    public byte[] renderTemplatePreview(Long templateId, float dpi) throws IOException {
        QuotationTemplate template = repository.findByIdAndActiveTrue(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));

        return renderTemplatePreview(template, dpi);
    }

    public byte[] renderTemplatePreview(QuotationTemplate template, float dpi) throws IOException {
        if (template.getBackgroundData() == null) {
            throw new RuntimeException("Template has no background data");
        }

        BufferedImage image;

        if ("PDF".equalsIgnoreCase(template.getBackgroundType())) {
            // Render PDF page as image
            try (PDDocument document = PDDocument.load(new ByteArrayInputStream(template.getBackgroundData()))) {
                PDFRenderer renderer = new PDFRenderer(document);
                image = renderer.renderImageWithDPI(0, dpi);
            }
        } else {
            // Return the image directly (scaled if needed)
            image = ImageIO.read(new ByteArrayInputStream(template.getBackgroundData()));
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }

    /**
     * Generate a preview of the quotation with field values filled in.
     */
    public byte[] renderQuotationPreview(Long templateId, String toValue, String fromValue, String dateValue, float dpi)
            throws IOException {

        // Generate the PDF first
        byte[] pdfData = generateQuotationPdf(templateId, toValue, fromValue, dateValue);

        // Then render it as an image
        try (PDDocument document = PDDocument.load(new ByteArrayInputStream(pdfData))) {
            PDFRenderer renderer = new PDFRenderer(document);
            BufferedImage image = renderer.renderImageWithDPI(0, dpi);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(image, "png", out);
            return out.toByteArray();
        }
    }
}

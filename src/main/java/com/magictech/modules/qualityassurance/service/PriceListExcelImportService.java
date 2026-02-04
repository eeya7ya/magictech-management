package com.magictech.modules.qualityassurance.service;

import com.magictech.modules.qualityassurance.entity.PriceList;
import com.magictech.modules.qualityassurance.entity.PriceListItem;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFPicture;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;

/**
 * Service for importing price lists from Excel files
 * Supports multi-sheet Excel where each sheet represents a manufacturer
 * Expected columns: MODEL, DESCRIPTION, PICTURE, DPP, SI/INSTALLER, END USER
 */
@Service
@Transactional
public class PriceListExcelImportService {

    private static final Logger logger = LoggerFactory.getLogger(PriceListExcelImportService.class);

    // Column indices (0-based) - can be configured
    private static final int COL_MODEL = 0;        // Column A
    private static final int COL_DESCRIPTION = 1;  // Column B
    private static final int COL_PICTURE = 2;      // Column C (images)
    private static final int COL_DPP = 3;          // Column D
    private static final int COL_SI_INSTALLER = 4; // Column E
    private static final int COL_END_USER = 5;     // Column F

    @Autowired
    private PriceListService priceListService;

    /**
     * Import price list from Excel file
     * @param excelData Excel file as byte array
     * @param filename Original filename
     * @param uploadedBy Username of uploader
     * @return PriceList with all imported items
     */
    public PriceList importPriceList(byte[] excelData, String filename, String uploadedBy) throws IOException {
        // Create the price list record
        PriceList priceList = priceListService.createPriceList(
            filename,
            (long) excelData.length,
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            uploadedBy,
            "Imported from " + filename
        );

        // Parse the Excel file
        List<PriceListItem> items = parseExcelFile(excelData, priceList.getId());

        // Save all items
        priceListService.addItems(items);

        // Update match status for all items
        priceListService.updateMatchStatusForPriceList(priceList.getId());

        logger.info("Imported price list '{}' with {} items from {} manufacturers",
            filename, items.size(), getDistinctManufacturerCount(items));

        return priceList;
    }

    /**
     * Parse Excel file and extract price list items
     */
    public List<PriceListItem> parseExcelFile(byte[] excelData, Long priceListId) throws IOException {
        List<PriceListItem> items = new ArrayList<>();

        try (ByteArrayInputStream bis = new ByteArrayInputStream(excelData);
             XSSFWorkbook workbook = new XSSFWorkbook(bis)) {

            // Process each sheet (each sheet = one manufacturer)
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                String manufacturer = sheet.getSheetName().trim();

                logger.debug("Processing sheet '{}' with {} rows", manufacturer, sheet.getLastRowNum());

                // Build image map for this sheet (row -> image data)
                Map<Integer, ImageData> imageMap = extractImagesWithPositions(sheet);

                // Process rows (skip header row 0)
                for (int rowIndex = 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row == null) continue;

                    // Get model from column A
                    String model = getCellValueAsString(row.getCell(COL_MODEL));
                    if (model == null || model.trim().isEmpty()) {
                        continue; // Skip rows without model
                    }

                    // Create price list item
                    PriceListItem item = new PriceListItem(priceListId, manufacturer, model.trim());

                    // Set description from column B
                    item.setDescription(getCellValueAsString(row.getCell(COL_DESCRIPTION)));

                    // Set prices from columns D, E, F
                    item.setDppPrice(parsePriceFromCell(row.getCell(COL_DPP)));
                    item.setSiInstallerPrice(parsePriceFromCell(row.getCell(COL_SI_INSTALLER)));
                    item.setEndUserPrice(parsePriceFromCell(row.getCell(COL_END_USER)));

                    // Set image if available for this row
                    ImageData imageData = imageMap.get(rowIndex);
                    if (imageData != null) {
                        item.setImageData(imageData.data);
                        item.setImageType(imageData.mimeType);
                    }

                    // Set excel row number for reference
                    item.setExcelRowNumber(rowIndex + 1); // 1-based for user display

                    items.add(item);
                }
            }

        } catch (Exception e) {
            logger.error("Failed to parse Excel file: {}", e.getMessage(), e);
            throw new IOException("Failed to parse Excel file: " + e.getMessage(), e);
        }

        return items;
    }

    /**
     * Extract images from sheet and map them to row numbers
     * Images are mapped based on their anchor position
     */
    private Map<Integer, ImageData> extractImagesWithPositions(Sheet sheet) {
        Map<Integer, ImageData> imageMap = new HashMap<>();

        Drawing<?> drawing = sheet.getDrawingPatriarch();
        if (drawing == null) {
            return imageMap;
        }

        for (Shape shape : (XSSFDrawing) drawing) {
            if (shape instanceof XSSFPicture picture) {

                // Get anchor to determine position
                XSSFClientAnchor anchor = picture.getClientAnchor();
                if (anchor != null) {
                    int row1 = anchor.getRow1();
                    int col1 = anchor.getCol1();

                    // Only consider images in the PICTURE column (column C = index 2)
                    if (col1 == COL_PICTURE || (col1 >= COL_PICTURE - 1 && col1 <= COL_PICTURE + 1)) {
                        ImageData imageData = new ImageData();
                        imageData.data = picture.getPictureData().getData();
                        imageData.mimeType = picture.getPictureData().getMimeType();

                        // Map to the row where image starts
                        imageMap.put(row1, imageData);

                        logger.trace("Found image at row {} col {}, size {} bytes",
                            row1, col1, imageData.data.length);
                    }
                }
            }
        }

        logger.debug("Extracted {} images from sheet", imageMap.size());
        return imageMap;
    }

    /**
     * Get cell value as string, handling different cell types
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return null;
        }

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
                } else {
                    // Remove trailing .0 for whole numbers
                    double value = cell.getNumericCellValue();
                    if (value == Math.floor(value)) {
                        yield String.valueOf((long) value);
                    }
                    yield String.valueOf(value);
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue();
                } catch (Exception e) {
                    try {
                        yield String.valueOf(cell.getNumericCellValue());
                    } catch (Exception e2) {
                        yield "";
                    }
                }
            }
            case BLANK -> null;
            default -> null;
        };
    }

    /**
     * Parse price from cell, handling various formats
     * Removes currency symbols and text (JOD, USD, etc.)
     */
    private BigDecimal parsePriceFromCell(Cell cell) {
        if (cell == null) {
            return null;
        }

        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            }

            String value = getCellValueAsString(cell);
            if (value == null || value.trim().isEmpty()) {
                return null;
            }

            // Remove currency symbols and text
            value = value.replaceAll("[^0-9.,]", "").trim();

            // Handle comma as decimal separator (European format)
            if (value.contains(",") && !value.contains(".")) {
                value = value.replace(",", ".");
            } else if (value.contains(",") && value.contains(".")) {
                // If both, assume comma is thousands separator
                value = value.replace(",", "");
            }

            if (value.isEmpty()) {
                return null;
            }

            return new BigDecimal(value);

        } catch (NumberFormatException e) {
            logger.warn("Could not parse price from cell value: {}", cell);
            return null;
        }
    }

    /**
     * Get count of distinct manufacturers in items list
     */
    private long getDistinctManufacturerCount(List<PriceListItem> items) {
        return items.stream()
            .map(PriceListItem::getManufacturer)
            .distinct()
            .count();
    }

    /**
     * Validate Excel file format for price list
     */
    public ValidationResult validateExcelFile(byte[] excelData, String filename) {
        if (excelData == null || excelData.length == 0) {
            return new ValidationResult(false, "File is empty");
        }

        if (!filename.toLowerCase().endsWith(".xlsx")) {
            return new ValidationResult(false, "File must be an Excel file (.xlsx)");
        }

        try (ByteArrayInputStream bis = new ByteArrayInputStream(excelData);
             XSSFWorkbook workbook = new XSSFWorkbook(bis)) {

            if (workbook.getNumberOfSheets() == 0) {
                return new ValidationResult(false, "Excel file has no sheets");
            }

            // Check if first sheet has expected structure
            Sheet firstSheet = workbook.getSheetAt(0);
            Row headerRow = firstSheet.getRow(0);

            if (headerRow == null) {
                return new ValidationResult(false, "First sheet has no header row");
            }

            // Validate expected columns exist
            String modelHeader = getCellValueAsString(headerRow.getCell(COL_MODEL));
            if (modelHeader == null || !modelHeader.toUpperCase().contains("MODEL")) {
                // Try to auto-detect column layout
                logger.warn("Column A does not appear to be MODEL column (found: {})", modelHeader);
            }

            int totalRows = 0;
            int totalSheets = workbook.getNumberOfSheets();
            List<String> sheetNames = new ArrayList<>();

            for (int i = 0; i < totalSheets; i++) {
                Sheet sheet = workbook.getSheetAt(i);
                sheetNames.add(sheet.getSheetName());
                totalRows += sheet.getLastRowNum();
            }

            String message = String.format("Valid Excel file: %d sheets (%s), ~%d items",
                totalSheets, String.join(", ", sheetNames), totalRows);

            return new ValidationResult(true, message, totalSheets, totalRows, sheetNames);

        } catch (Exception e) {
            return new ValidationResult(false, "Invalid Excel file: " + e.getMessage());
        }
    }

    /**
     * Get preview of Excel file contents
     */
    public PreviewResult getPreview(byte[] excelData, int maxRowsPerSheet) throws IOException {
        PreviewResult result = new PreviewResult();

        try (ByteArrayInputStream bis = new ByteArrayInputStream(excelData);
             XSSFWorkbook workbook = new XSSFWorkbook(bis)) {

            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                SheetPreview sheetPreview = new SheetPreview();
                sheetPreview.name = sheet.getSheetName();
                sheetPreview.totalRows = sheet.getLastRowNum();

                // Get header row
                Row headerRow = sheet.getRow(0);
                if (headerRow != null) {
                    sheetPreview.headers = new ArrayList<>();
                    for (int col = 0; col <= 5; col++) {
                        String header = getCellValueAsString(headerRow.getCell(col));
                        sheetPreview.headers.add(header != null ? header : "");
                    }
                }

                // Get preview rows
                sheetPreview.previewRows = new ArrayList<>();
                int rowsToShow = Math.min(maxRowsPerSheet, sheet.getLastRowNum());
                for (int rowIndex = 1; rowIndex <= rowsToShow; rowIndex++) {
                    Row row = sheet.getRow(rowIndex);
                    if (row != null) {
                        RowPreview rowPreview = new RowPreview();
                        rowPreview.model = getCellValueAsString(row.getCell(COL_MODEL));
                        rowPreview.description = getCellValueAsString(row.getCell(COL_DESCRIPTION));
                        rowPreview.dppPrice = getCellValueAsString(row.getCell(COL_DPP));
                        rowPreview.siInstallerPrice = getCellValueAsString(row.getCell(COL_SI_INSTALLER));
                        rowPreview.endUserPrice = getCellValueAsString(row.getCell(COL_END_USER));
                        sheetPreview.previewRows.add(rowPreview);
                    }
                }

                // Count images
                Drawing<?> drawing = sheet.getDrawingPatriarch();
                if (drawing != null) {
                    sheetPreview.imageCount = 0;
                    for (Shape shape : (XSSFDrawing) drawing) {
                        if (shape instanceof XSSFPicture) {
                            sheetPreview.imageCount++;
                        }
                    }
                }

                result.sheets.add(sheetPreview);
            }

        } catch (Exception e) {
            throw new IOException("Failed to generate preview: " + e.getMessage(), e);
        }

        return result;
    }

    // Helper classes

    private static class ImageData {
        byte[] data;
        String mimeType;
    }

    public static class ValidationResult {
        public final boolean valid;
        public final String message;
        public final int sheetCount;
        public final int estimatedItems;
        public final List<String> sheetNames;

        public ValidationResult(boolean valid, String message) {
            this(valid, message, 0, 0, new ArrayList<>());
        }

        public ValidationResult(boolean valid, String message, int sheetCount, int estimatedItems, List<String> sheetNames) {
            this.valid = valid;
            this.message = message;
            this.sheetCount = sheetCount;
            this.estimatedItems = estimatedItems;
            this.sheetNames = sheetNames;
        }
    }

    public static class PreviewResult {
        public List<SheetPreview> sheets = new ArrayList<>();
    }

    public static class SheetPreview {
        public String name;
        public int totalRows;
        public int imageCount;
        public List<String> headers;
        public List<RowPreview> previewRows;
    }

    public static class RowPreview {
        public String model;
        public String description;
        public String dppPrice;
        public String siInstallerPrice;
        public String endUserPrice;
    }
}

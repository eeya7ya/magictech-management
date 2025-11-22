package com.magictech.modules.sales.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.util.*;

/**
 * Service for handling Excel file storage, parsing, and extraction
 * Supports Excel files with images/photos
 */
@Service
@Transactional
public class ExcelStorageService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Parse Excel file and extract data including images
     * Returns JSON string with parsed data
     */
    public String parseExcelFile(byte[] excelFile) throws IOException {
        Map<String, Object> parsedData = new HashMap<>();
        List<Map<String, Object>> sheets = new ArrayList<>();

        try (ByteArrayInputStream bis = new ByteArrayInputStream(excelFile);
             Workbook workbook = new XSSFWorkbook(bis)) {

            // Iterate through all sheets
            for (int sheetIndex = 0; sheetIndex < workbook.getNumberOfSheets(); sheetIndex++) {
                Sheet sheet = workbook.getSheetAt(sheetIndex);
                Map<String, Object> sheetData = new HashMap<>();
                sheetData.put("sheetName", sheet.getSheetName());
                sheetData.put("sheetIndex", sheetIndex);

                // Extract rows
                List<List<String>> rows = new ArrayList<>();
                for (Row row : sheet) {
                    List<String> rowData = new ArrayList<>();
                    for (Cell cell : row) {
                        rowData.add(getCellValueAsString(cell));
                    }
                    rows.add(rowData);
                }
                sheetData.put("rows", rows);
                sheetData.put("rowCount", sheet.getLastRowNum() + 1);

                // Extract images (if any)
                List<Map<String, Object>> images = extractImages(sheet);
                sheetData.put("images", images);
                sheetData.put("imageCount", images.size());

                sheets.add(sheetData);
            }

            parsedData.put("sheets", sheets);
            parsedData.put("sheetCount", workbook.getNumberOfSheets());
            parsedData.put("parsedAt", new Date().toString());

        } catch (Exception e) {
            throw new IOException("Failed to parse Excel file: " + e.getMessage(), e);
        }

        return objectMapper.writeValueAsString(parsedData);
    }

    /**
     * Extract images from Excel sheet
     */
    private List<Map<String, Object>> extractImages(Sheet sheet) {
        List<Map<String, Object>> images = new ArrayList<>();

        // POI drawing patriarch contains all images
        Drawing<?> drawing = sheet.getDrawingPatriarch();
        if (drawing != null) {
            int imageIndex = 0;
            for (Shape shape : drawing) {
                if (shape instanceof org.apache.poi.xssf.usermodel.XSSFPicture) {
                    org.apache.poi.xssf.usermodel.XSSFPicture picture =
                        (org.apache.poi.xssf.usermodel.XSSFPicture) shape;

                    Map<String, Object> imageData = new HashMap<>();
                    imageData.put("imageIndex", imageIndex++);
                    imageData.put("mimeType", picture.getPictureData().getMimeType());
                    imageData.put("size", picture.getPictureData().getData().length);
                    imageData.put("extension", picture.getPictureData().suggestFileExtension());

                    // Store image data as Base64 for JSON compatibility
                    String base64Image = Base64.getEncoder().encodeToString(
                        picture.getPictureData().getData()
                    );
                    imageData.put("base64Data", base64Image);

                    images.add(imageData);
                }
            }
        }

        return images;
    }

    /**
     * Get cell value as string
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
                } else {
                    yield String.valueOf(cell.getNumericCellValue());
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> cell.getCellFormula();
            case BLANK -> "";
            default -> "";
        };
    }

    /**
     * Create Excel file from data
     */
    public byte[] createExcelFile(String parsedJsonData) throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {

            Map<String, Object> parsedData = objectMapper.readValue(parsedJsonData, Map.class);
            List<Map<String, Object>> sheets = (List<Map<String, Object>>) parsedData.get("sheets");

            if (sheets != null) {
                for (Map<String, Object> sheetData : sheets) {
                    String sheetName = (String) sheetData.get("sheetName");
                    Sheet sheet = workbook.createSheet(sheetName != null ? sheetName : "Sheet");

                    List<List<String>> rows = (List<List<String>>) sheetData.get("rows");
                    if (rows != null) {
                        int rowIndex = 0;
                        for (List<String> rowData : rows) {
                            Row row = sheet.createRow(rowIndex++);
                            int cellIndex = 0;
                            for (String cellValue : rowData) {
                                Cell cell = row.createCell(cellIndex++);
                                cell.setCellValue(cellValue != null ? cellValue : "");
                            }
                        }
                    }
                }
            }

            workbook.write(bos);
            return bos.toByteArray();

        } catch (Exception e) {
            throw new IOException("Failed to create Excel file: " + e.getMessage(), e);
        }
    }

    /**
     * Validate Excel file format
     */
    public boolean isValidExcelFile(byte[] fileData, String fileName) {
        if (fileData == null || fileData.length == 0) {
            return false;
        }

        // Check file extension
        if (fileName != null) {
            String lowerFileName = fileName.toLowerCase();
            if (!lowerFileName.endsWith(".xlsx") && !lowerFileName.endsWith(".xls")) {
                return false;
            }
        }

        // Try to open as workbook
        try (ByteArrayInputStream bis = new ByteArrayInputStream(fileData);
             Workbook workbook = new XSSFWorkbook(bis)) {
            return workbook.getNumberOfSheets() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get file size in human-readable format
     */
    public String getFormattedFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp - 1) + "";
        return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
    }

    /**
     * Extract summary information from parsed data
     */
    public Map<String, Object> getSummaryFromParsedData(String parsedJsonData) {
        try {
            Map<String, Object> parsedData = objectMapper.readValue(parsedJsonData, Map.class);
            Map<String, Object> summary = new HashMap<>();

            summary.put("sheetCount", parsedData.get("sheetCount"));
            summary.put("parsedAt", parsedData.get("parsedAt"));

            List<Map<String, Object>> sheets = (List<Map<String, Object>>) parsedData.get("sheets");
            if (sheets != null && !sheets.isEmpty()) {
                int totalRows = 0;
                int totalImages = 0;

                for (Map<String, Object> sheet : sheets) {
                    Integer rowCount = (Integer) sheet.get("rowCount");
                    Integer imageCount = (Integer) sheet.get("imageCount");

                    totalRows += rowCount != null ? rowCount : 0;
                    totalImages += imageCount != null ? imageCount : 0;
                }

                summary.put("totalRows", totalRows);
                summary.put("totalImages", totalImages);
            }

            return summary;
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}

package com.magictech.modules.sales.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.*;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTMarker;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Comprehensive Excel parsing service for site survey files
 * Supports:
 * - Flexible sheet structures (any number of columns/rows)
 * - Embedded images with cell positions
 * - All data types (text, numbers, dates, formulas, booleans)
 * - Multiple sheets
 * - Merged cells
 */
@Service
public class SiteSurveyExcelService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final DecimalFormat decimalFormat = new DecimalFormat("#.##########");

    /**
     * Parse Excel file and return comprehensive JSON structure
     *
     * @param excelBytes Raw Excel file bytes
     * @param fileName Original file name
     * @return JSON string with complete Excel data including images
     */
    public String parseExcelToJson(byte[] excelBytes, String fileName) throws IOException {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(excelBytes);
             Workbook workbook = WorkbookFactory.create(bis)) {

            ObjectNode rootNode = objectMapper.createObjectNode();
            rootNode.put("fileName", fileName);
            rootNode.put("parsedAt", LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME));
            rootNode.put("numberOfSheets", workbook.getNumberOfSheets());

            ArrayNode sheetsArray = objectMapper.createArrayNode();

            // Parse each sheet
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                ObjectNode sheetNode = parseSheet(sheet, workbook);
                sheetsArray.add(sheetNode);
            }

            rootNode.set("sheets", sheetsArray);

            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
        }
    }

    /**
     * Parse a single sheet
     */
    private ObjectNode parseSheet(Sheet sheet, Workbook workbook) {
        ObjectNode sheetNode = objectMapper.createObjectNode();
        sheetNode.put("sheetName", sheet.getSheetName());
        sheetNode.put("sheetIndex", workbook.getSheetIndex(sheet));

        // Get actual data range
        int firstRowNum = sheet.getFirstRowNum();
        int lastRowNum = sheet.getLastRowNum();
        sheetNode.put("firstRow", firstRowNum);
        sheetNode.put("lastRow", lastRowNum);

        // Parse rows and cells
        ArrayNode rowsArray = objectMapper.createArrayNode();
        int maxColumns = 0;

        for (int rowNum = firstRowNum; rowNum <= lastRowNum; rowNum++) {
            Row row = sheet.getRow(rowNum);
            if (row != null) {
                ObjectNode rowNode = parseRow(row);
                rowsArray.add(rowNode);

                if (row.getLastCellNum() > maxColumns) {
                    maxColumns = row.getLastCellNum();
                }
            }
        }

        sheetNode.put("numberOfRows", lastRowNum - firstRowNum + 1);
        sheetNode.put("numberOfColumns", maxColumns);
        sheetNode.set("rows", rowsArray);

        // Parse images (if XSSFWorkbook)
        if (sheet instanceof XSSFSheet) {
            ArrayNode imagesArray = parseImages((XSSFSheet) sheet);
            sheetNode.set("images", imagesArray);
        }

        return sheetNode;
    }

    /**
     * Parse a single row
     */
    private ObjectNode parseRow(Row row) {
        ObjectNode rowNode = objectMapper.createObjectNode();
        rowNode.put("rowIndex", row.getRowNum());
        rowNode.put("height", row.getHeight());

        ArrayNode cellsArray = objectMapper.createArrayNode();

        for (int cellNum = 0; cellNum < row.getLastCellNum(); cellNum++) {
            Cell cell = row.getCell(cellNum);
            if (cell != null) {
                ObjectNode cellNode = parseCell(cell);
                cellsArray.add(cellNode);
            }
        }

        rowNode.set("cells", cellsArray);
        return rowNode;
    }

    /**
     * Parse a single cell - simplified (no column metadata)
     * Similar to storage management approach - just extract the value
     */
    private ObjectNode parseCell(Cell cell) {
        ObjectNode cellNode = objectMapper.createObjectNode();

        // Extract value based on type (no column numbers, letters, or addresses)
        CellType cellType = cell.getCellType();

        switch (cellType) {
            case STRING:
                cellNode.put("value", cell.getStringCellValue());
                break;

            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date date = cell.getDateCellValue();
                    cellNode.put("value", date.toString());
                } else {
                    double numValue = cell.getNumericCellValue();
                    // Check if it's a whole number to avoid unnecessary decimals
                    if (numValue == (long) numValue) {
                        cellNode.put("value", (long) numValue);
                    } else {
                        cellNode.put("value", numValue);
                    }
                }
                break;

            case BOOLEAN:
                cellNode.put("value", cell.getBooleanCellValue());
                break;

            case FORMULA:
                // For formulas, try to get the cached result
                try {
                    CellType cachedType = cell.getCachedFormulaResultType();

                    switch (cachedType) {
                        case NUMERIC:
                            double numResult = cell.getNumericCellValue();
                            if (numResult == (long) numResult) {
                                cellNode.put("value", (long) numResult);
                            } else {
                                cellNode.put("value", numResult);
                            }
                            break;
                        case STRING:
                            cellNode.put("value", cell.getStringCellValue());
                            break;
                        case BOOLEAN:
                            cellNode.put("value", cell.getBooleanCellValue());
                            break;
                        default:
                            cellNode.put("value", "");
                    }
                } catch (Exception e) {
                    cellNode.put("value", "");
                }
                break;

            case BLANK:
                cellNode.put("value", "");
                break;

            case ERROR:
                cellNode.put("value", "ERROR");
                break;

            default:
                cellNode.put("value", "");
        }

        return cellNode;
    }

    /**
     * Parse images from XSSF sheet - simplified
     * Similar to storage approach - just extract the image data without detailed positioning
     */
    private ArrayNode parseImages(XSSFSheet sheet) {
        ArrayNode imagesArray = objectMapper.createArrayNode();

        XSSFDrawing drawing = sheet.getDrawingPatriarch();
        if (drawing != null) {
            List<XSSFShape> shapes = drawing.getShapes();
            int imageIndex = 0;

            for (XSSFShape shape : shapes) {
                if (shape instanceof XSSFPicture) {
                    XSSFPicture picture = (XSSFPicture) shape;
                    ObjectNode imageNode = objectMapper.createObjectNode();

                    imageNode.put("imageIndex", imageIndex++);

                    // Get image data
                    XSSFPictureData pictureData = picture.getPictureData();
                    byte[] imageBytes = pictureData.getData();
                    imageNode.put("size", imageBytes.length);
                    imageNode.put("mimeType", pictureData.getMimeType());
                    imageNode.put("extension", pictureData.suggestFileExtension());

                    // Encode image as Base64 for JSON storage
                    String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                    imageNode.put("base64Data", base64Image);

                    imagesArray.add(imageNode);
                }
            }
        }

        return imagesArray;
    }

    /**
     * Extract summary information from parsed JSON
     */
    public ObjectNode extractSummary(String parsedJson) throws IOException {
        ObjectNode root = (ObjectNode) objectMapper.readTree(parsedJson);
        ObjectNode summary = objectMapper.createObjectNode();

        summary.put("fileName", root.get("fileName").asText());
        summary.put("parsedAt", root.get("parsedAt").asText());
        summary.put("numberOfSheets", root.get("numberOfSheets").asInt());

        int totalRows = 0;
        int totalCells = 0;
        int totalImages = 0;

        ArrayNode sheets = (ArrayNode) root.get("sheets");
        for (int i = 0; i < sheets.size(); i++) {
            ObjectNode sheet = (ObjectNode) sheets.get(i);
            totalRows += sheet.get("numberOfRows").asInt();

            if (sheet.has("images")) {
                totalImages += ((ArrayNode) sheet.get("images")).size();
            }

            ArrayNode rows = (ArrayNode) sheet.get("rows");
            for (int j = 0; j < rows.size(); j++) {
                ObjectNode row = (ObjectNode) rows.get(j);
                totalCells += ((ArrayNode) row.get("cells")).size();
            }
        }

        summary.put("totalRows", totalRows);
        summary.put("totalCells", totalCells);
        summary.put("totalImages", totalImages);

        return summary;
    }

    /**
     * Validate Excel file
     */
    public boolean isValidExcelFile(byte[] fileBytes, String fileName) {
        if (fileBytes == null || fileBytes.length == 0) {
            return false;
        }

        if (fileName == null) {
            return false;
        }

        String lowerFileName = fileName.toLowerCase();
        if (!lowerFileName.endsWith(".xlsx") && !lowerFileName.endsWith(".xls")) {
            return false;
        }

        try (ByteArrayInputStream bis = new ByteArrayInputStream(fileBytes);
             Workbook workbook = WorkbookFactory.create(bis)) {
            return workbook.getNumberOfSheets() > 0;
        } catch (Exception e) {
            return false;
        }
    }
}

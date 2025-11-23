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

        // Parse merged regions
        ArrayNode mergedRegions = parseMergedRegions(sheet);
        if (mergedRegions.size() > 0) {
            sheetNode.set("mergedCells", mergedRegions);
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
     * Parse a single cell - comprehensive data extraction
     */
    private ObjectNode parseCell(Cell cell) {
        ObjectNode cellNode = objectMapper.createObjectNode();
        cellNode.put("columnIndex", cell.getColumnIndex());
        cellNode.put("columnLetter", CellReference.convertNumToColString(cell.getColumnIndex()));
        cellNode.put("address", cell.getAddress().formatAsString());

        CellType cellType = cell.getCellType();
        cellNode.put("cellType", cellType.name());

        // Extract value based on type
        switch (cellType) {
            case STRING:
                cellNode.put("value", cell.getStringCellValue());
                cellNode.put("displayValue", cell.getStringCellValue());
                break;

            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    Date date = cell.getDateCellValue();
                    cellNode.put("value", date.toString());
                    cellNode.put("displayValue", date.toString());
                    cellNode.put("isDate", true);
                } else {
                    double numValue = cell.getNumericCellValue();
                    cellNode.put("value", numValue);
                    cellNode.put("displayValue", decimalFormat.format(numValue));
                }
                break;

            case BOOLEAN:
                boolean boolValue = cell.getBooleanCellValue();
                cellNode.put("value", boolValue);
                cellNode.put("displayValue", String.valueOf(boolValue));
                break;

            case FORMULA:
                cellNode.put("formula", cell.getCellFormula());
                try {
                    CellType cachedType = cell.getCachedFormulaResultType();
                    cellNode.put("cachedType", cachedType.name());

                    switch (cachedType) {
                        case NUMERIC:
                            double numResult = cell.getNumericCellValue();
                            cellNode.put("value", numResult);
                            cellNode.put("displayValue", decimalFormat.format(numResult));
                            break;
                        case STRING:
                            String strResult = cell.getStringCellValue();
                            cellNode.put("value", strResult);
                            cellNode.put("displayValue", strResult);
                            break;
                        case BOOLEAN:
                            boolean boolResult = cell.getBooleanCellValue();
                            cellNode.put("value", boolResult);
                            cellNode.put("displayValue", String.valueOf(boolResult));
                            break;
                    }
                } catch (Exception e) {
                    cellNode.put("formulaError", e.getMessage());
                }
                break;

            case BLANK:
                cellNode.put("value", "");
                cellNode.put("displayValue", "");
                break;

            case ERROR:
                cellNode.put("error", cell.getErrorCellValue());
                cellNode.put("displayValue", "ERROR");
                break;

            default:
                cellNode.put("value", "");
                cellNode.put("displayValue", "");
        }

        // Cell styling
        CellStyle style = cell.getCellStyle();
        if (style != null) {
            ObjectNode styleNode = objectMapper.createObjectNode();
            Workbook workbook = cell.getSheet().getWorkbook();
            Font font = workbook.getFontAt(style.getFontIndexAsInt());
            styleNode.put("fontBold", font.getBold());
            styleNode.put("fillForegroundColor", style.getFillForegroundColor());
            styleNode.put("alignment", style.getAlignment().name());
            cellNode.set("style", styleNode);
        }

        return cellNode;
    }

    /**
     * Parse images from XSSF sheet
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
                    imageNode.put("imageSizeBytes", imageBytes.length);
                    imageNode.put("mimeType", pictureData.getMimeType());
                    imageNode.put("imageFormat", pictureData.suggestFileExtension());

                    // Encode image as Base64 for JSON storage
                    String base64Image = Base64.getEncoder().encodeToString(imageBytes);
                    imageNode.put("imageDataBase64", base64Image);

                    // Get anchor (position) information
                    XSSFClientAnchor anchor = (XSSFClientAnchor) picture.getAnchor();
                    ObjectNode anchorNode = objectMapper.createObjectNode();
                    anchorNode.put("row1", anchor.getRow1());
                    anchorNode.put("col1", anchor.getCol1());
                    anchorNode.put("row2", anchor.getRow2());
                    anchorNode.put("col2", anchor.getCol2());
                    anchorNode.put("cellAddress", CellReference.convertNumToColString((int) anchor.getCol1()) + (anchor.getRow1() + 1));
                    imageNode.set("position", anchorNode);

                    imagesArray.add(imageNode);
                }
            }
        }

        return imagesArray;
    }

    /**
     * Parse merged cell regions
     */
    private ArrayNode parseMergedRegions(Sheet sheet) {
        ArrayNode mergedArray = objectMapper.createArrayNode();

        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            org.apache.poi.ss.util.CellRangeAddress region = sheet.getMergedRegion(i);
            ObjectNode mergedNode = objectMapper.createObjectNode();
            mergedNode.put("firstRow", region.getFirstRow());
            mergedNode.put("lastRow", region.getLastRow());
            mergedNode.put("firstColumn", region.getFirstColumn());
            mergedNode.put("lastColumn", region.getLastColumn());
            mergedNode.put("range", region.formatAsString());
            mergedArray.add(mergedNode);
        }

        return mergedArray;
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

package com.magictech.modules.storage.service;

import com.magictech.modules.storage.entity.StorageItem;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

/**
 * Excel Export Service
 * Exports storage items to Excel file
 */
@Service
public class ExcelExportService {

    /**
     * Export storage items to Excel file
     */
    public File exportToExcel(List<StorageItem> items, String fileName) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Storage Items");

        // Create header style
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 12);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] columns = {"ID", "Manufacture", "Product Name", "Code", "Serial Number", "Quantity", "Price"};

        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        // Create data rows
        int rowNum = 1;
        for (StorageItem item : items) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(item.getId());
            row.createCell(1).setCellValue(item.getManufacture() != null ? item.getManufacture() : "");
            row.createCell(2).setCellValue(item.getProductName() != null ? item.getProductName() : "");
            row.createCell(3).setCellValue(item.getCode() != null ? item.getCode() : "");
            row.createCell(4).setCellValue(item.getSerialNumber() != null ? item.getSerialNumber() : "");
            row.createCell(5).setCellValue(item.getQuantity() != null ? item.getQuantity() : 0);

            if (item.getPrice() != null) {
                row.createCell(6).setCellValue(item.getPrice().doubleValue());
            } else {
                row.createCell(6).setCellValue(0);
            }
        }

        // Auto-size columns
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }

        // Write to file
        File file = new File(fileName);
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            workbook.write(outputStream);
        }
        workbook.close();

        return file;
    }
}
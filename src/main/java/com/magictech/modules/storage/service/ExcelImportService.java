package com.magictech.modules.storage.service;

import com.magictech.modules.storage.entity.StorageItem;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel Import Service - UPDATED FOR NEW STRUCTURE
 * Expected columns: Manufacture | Product Name | Code | Serial Number | Quantity | Price
 */
@Service
public class ExcelImportService {

    /**
     * Read Excel file and convert to StorageItem list
     * Expected columns: Manufacture | Product Name | Code | Serial Number | Quantity | Price
     */
    public List<StorageItem> importFromExcel(File file) throws IOException {
        List<StorageItem> items = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(file);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0); // Read first sheet

            // Skip header row (row 0)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                try {
                    StorageItem item = new StorageItem();

                    // Column 0: Manufacture
                    Cell manufactureCell = row.getCell(0);
                    if (manufactureCell != null) {
                        item.setManufacture(getCellValueAsString(manufactureCell));
                    }

                    // Column 1: Product Name (REQUIRED)
                    Cell productNameCell = row.getCell(1);
                    if (productNameCell != null) {
                        item.setProductName(getCellValueAsString(productNameCell));
                    }

                    // Column 2: Code
                    Cell codeCell = row.getCell(2);
                    if (codeCell != null) {
                        item.setCode(getCellValueAsString(codeCell));
                    }

                    // Column 3: Serial Number
                    Cell serialCell = row.getCell(3);
                    if (serialCell != null) {
                        item.setSerialNumber(getCellValueAsString(serialCell));
                    }

                    // Column 4: Quantity
                    Cell quantityCell = row.getCell(4);
                    if (quantityCell != null) {
                        item.setQuantity((int) getNumericCellValue(quantityCell));
                    } else {
                        item.setQuantity(0);
                    }

                    // Column 5: Price
                    Cell priceCell = row.getCell(5);
                    if (priceCell != null) {
                        double priceValue = getNumericCellValue(priceCell);
                        item.setPrice(BigDecimal.valueOf(priceValue));
                    }

                    // Set defaults
                    item.setDateAdded(LocalDateTime.now());
                    item.setActive(true);

                    // Only add if product name is not empty
                    if (item.getProductName() != null && !item.getProductName().trim().isEmpty()) {
                        items.add(item);
                    }

                } catch (Exception e) {
                    System.err.println("Error reading row " + (i + 1) + ": " + e.getMessage());
                    // Continue to next row
                }
            }
        }

        return items;
    }

    /**
     * Get cell value as string regardless of cell type
     */
    private String getCellValueAsString(Cell cell) {
        if (cell == null) return "";

        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue().trim();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                // Check if it's a whole number
                double numValue = cell.getNumericCellValue();
                if (numValue == (long) numValue) {
                    return String.valueOf((long) numValue);
                }
                return String.valueOf(numValue);
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            case BLANK:
                return "";
            default:
                return "";
        }
    }

    /**
     * Get numeric value from cell
     */
    private double getNumericCellValue(Cell cell) {
        if (cell == null) return 0;

        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        } else if (cell.getCellType() == CellType.STRING) {
            try {
                return Double.parseDouble(cell.getStringCellValue().trim());
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }
}
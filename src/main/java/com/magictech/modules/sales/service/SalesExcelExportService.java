package com.magictech.modules.sales.service;

import com.magictech.modules.projects.entity.Project;
import com.magictech.modules.projects.entity.ProjectElement;
import com.magictech.modules.projects.service.ProjectElementService;
import com.magictech.modules.sales.entity.Customer;
import com.magictech.modules.sales.entity.ProjectCostBreakdown;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

/**
 * Sales Excel Export Service
 * Exports sales projects and customer data to Excel files
 */
@Service
public class SalesExcelExportService {

    @Autowired
    private ProjectElementService elementService;

    @Autowired
    private ProjectCostBreakdownService costBreakdownService;

    /**
     * Export selected projects with their elements and cost breakdown to Excel
     */
    public File exportProjectsToExcel(List<Project> projects, String fileName) throws IOException {
        Workbook workbook = new XSSFWorkbook();

        // Create styles
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);

        // Sheet 1: Project Elements
        Sheet elementsSheet = workbook.createSheet("Project Elements");
        createProjectElementsSheet(elementsSheet, projects, headerStyle, currencyStyle);

        // Sheet 2: Cost Breakdown
        Sheet breakdownSheet = workbook.createSheet("Cost Breakdown");
        createCostBreakdownSheet(breakdownSheet, projects, headerStyle, currencyStyle);

        // Write to file
        File file = new File(fileName);
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            workbook.write(outputStream);
        }
        workbook.close();

        return file;
    }

    /**
     * Export selected customers to Excel
     */
    public File exportCustomersToExcel(List<Customer> customers, String fileName) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("Customers");

        CellStyle headerStyle = createHeaderStyle(workbook);

        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] columns = {"ID", "Name", "Email", "Phone", "Address", "Company"};

        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        // Create data rows
        int rowNum = 1;
        for (Customer customer : customers) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(customer.getId());
            row.createCell(1).setCellValue(customer.getName() != null ? customer.getName() : "");
            row.createCell(2).setCellValue(customer.getEmail() != null ? customer.getEmail() : "");
            row.createCell(3).setCellValue(customer.getPhone() != null ? customer.getPhone() : "");
            row.createCell(4).setCellValue(customer.getAddress() != null ? customer.getAddress() : "");
            row.createCell(5).setCellValue(customer.getCompany() != null ? customer.getCompany() : "");
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

    // ==================== HELPER METHODS ====================

    private void createProjectElementsSheet(Sheet sheet, List<Project> projects, CellStyle headerStyle, CellStyle currencyStyle) {
        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] columns = {"Project Name", "Element", "Manufacture", "Product Name", "Code", "Quantity Needed", "Unit Price", "Total Price"};

        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        // Create data rows
        int rowNum = 1;
        for (Project project : projects) {
            List<ProjectElement> elements = elementService.getElementsByProject(project.getId());

            if (elements.isEmpty()) {
                // Add project name even if no elements
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(project.getProjectName());
                row.createCell(1).setCellValue("(No elements)");
            } else {
                for (ProjectElement element : elements) {
                    Row row = sheet.createRow(rowNum++);

                    row.createCell(0).setCellValue(project.getProjectName());
                    row.createCell(1).setCellValue("Element #" + element.getId());

                    if (element.getStorageItem() != null) {
                        row.createCell(2).setCellValue(element.getStorageItem().getManufacture() != null ?
                            element.getStorageItem().getManufacture() : "");
                        row.createCell(3).setCellValue(element.getStorageItem().getProductName() != null ?
                            element.getStorageItem().getProductName() : "");
                        row.createCell(4).setCellValue(element.getStorageItem().getCode() != null ?
                            element.getStorageItem().getCode() : "");

                        row.createCell(5).setCellValue(element.getQuantityNeeded());

                        BigDecimal unitPrice = element.getStorageItem().getPrice() != null ?
                            element.getStorageItem().getPrice() : BigDecimal.ZERO;
                        Cell priceCell = row.createCell(6);
                        priceCell.setCellValue(unitPrice.doubleValue());
                        priceCell.setCellStyle(currencyStyle);

                        BigDecimal totalPrice = unitPrice.multiply(new BigDecimal(element.getQuantityNeeded()));
                        Cell totalCell = row.createCell(7);
                        totalCell.setCellValue(totalPrice.doubleValue());
                        totalCell.setCellStyle(currencyStyle);
                    } else {
                        row.createCell(2).setCellValue("(Item not found)");
                    }
                }
            }
        }

        // Auto-size columns
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private void createCostBreakdownSheet(Sheet sheet, List<Project> projects, CellStyle headerStyle, CellStyle currencyStyle) {
        // Create header row
        Row headerRow = sheet.createRow(0);
        String[] columns = {"Project Name", "Elements Subtotal", "Tax Rate %", "Tax Amount",
                            "Discount Amount", "Installation Cost", "Additional Cost", "Total Cost"};

        for (int i = 0; i < columns.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(columns[i]);
            cell.setCellStyle(headerStyle);
        }

        // Create data rows
        int rowNum = 1;
        for (Project project : projects) {
            Row row = sheet.createRow(rowNum++);

            row.createCell(0).setCellValue(project.getProjectName());

            // Calculate elements subtotal
            List<ProjectElement> elements = elementService.getElementsByProject(project.getId());
            BigDecimal subtotal = BigDecimal.ZERO;
            for (ProjectElement element : elements) {
                if (element.getStorageItem() != null && element.getStorageItem().getPrice() != null) {
                    BigDecimal price = element.getStorageItem().getPrice();
                    BigDecimal quantity = new BigDecimal(element.getQuantityNeeded());
                    subtotal = subtotal.add(price.multiply(quantity));
                }
            }

            Cell subtotalCell = row.createCell(1);
            subtotalCell.setCellValue(subtotal.doubleValue());
            subtotalCell.setCellStyle(currencyStyle);

            // Get cost breakdown if exists
            try {
                java.util.Optional<ProjectCostBreakdown> breakdown = costBreakdownService.getBreakdownByProject(project.getId());
                if (breakdown.isPresent()) {
                    ProjectCostBreakdown cb = breakdown.get();

                    // Tax rate as percentage (convert 0.15 to 15%)
                    double taxRatePercent = cb.getTaxRate() != null ? cb.getTaxRate().multiply(new BigDecimal(100)).doubleValue() : 0;
                    row.createCell(2).setCellValue(taxRatePercent);

                    Cell taxCell = row.createCell(3);
                    taxCell.setCellValue(cb.getTaxAmount() != null ? cb.getTaxAmount().doubleValue() : 0);
                    taxCell.setCellStyle(currencyStyle);

                    Cell discountCell = row.createCell(4);
                    discountCell.setCellValue(cb.getDiscountAmount() != null ? cb.getDiscountAmount().doubleValue() : 0);
                    discountCell.setCellStyle(currencyStyle);

                    Cell installationCell = row.createCell(5);
                    installationCell.setCellValue(cb.getInstallationCost() != null ? cb.getInstallationCost().doubleValue() : 0);
                    installationCell.setCellStyle(currencyStyle);

                    Cell additionalCell = row.createCell(6);
                    additionalCell.setCellValue(cb.getAdditionalCost() != null ? cb.getAdditionalCost().doubleValue() : 0);
                    additionalCell.setCellStyle(currencyStyle);

                    Cell totalCell = row.createCell(7);
                    totalCell.setCellValue(cb.getTotalCost() != null ? cb.getTotalCost().doubleValue() : 0);
                    totalCell.setCellStyle(currencyStyle);
                } else {
                    // No breakdown, just show subtotal
                    row.createCell(2).setCellValue(0);
                    row.createCell(3).setCellValue(0);
                    row.createCell(4).setCellValue(0);
                    row.createCell(5).setCellValue(0);
                    row.createCell(6).setCellValue(0);
                    Cell totalCell = row.createCell(7);
                    totalCell.setCellValue(subtotal.doubleValue());
                    totalCell.setCellStyle(currencyStyle);
                }
            } catch (Exception e) {
                System.err.println("Error loading breakdown for project " + project.getId() + ": " + e.getMessage());
            }
        }

        // Auto-size columns
        for (int i = 0; i < columns.length; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
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
        return headerStyle;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle currencyStyle = workbook.createCellStyle();
        currencyStyle.setDataFormat(workbook.createDataFormat().getFormat("$#,##0.00"));
        return currencyStyle;
    }
}

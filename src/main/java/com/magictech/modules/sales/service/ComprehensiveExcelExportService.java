package com.magictech.modules.sales.service;

import com.magictech.modules.sales.entity.*;
import com.magictech.modules.projects.entity.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Comprehensive Excel Export Service
 * Exports complete project/customer details for quotations
 */
@Service
public class ComprehensiveExcelExportService {

    @Autowired
    private CustomerElementService customerElementService;

    @Autowired
    private CustomerTaskService customerTaskService;

    @Autowired
    private CustomerScheduleService customerScheduleService;

    @Autowired
    private CustomerDocumentService customerDocumentService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * Export comprehensive customer quotation
     */
    public void exportCustomerQuotation(Customer customer, String filePath) throws IOException {
        Workbook workbook = new XSSFWorkbook();

        // Create styles
        CellStyle headerStyle = createHeaderStyle(workbook);
        CellStyle titleStyle = createTitleStyle(workbook);
        CellStyle currencyStyle = createCurrencyStyle(workbook);

        // Sheet 1: Customer Information
        createCustomerInfoSheet(workbook, customer, titleStyle, headerStyle);

        // Sheet 2: Elements/Items with Pricing
        createElementsSheet(workbook, customer, titleStyle, headerStyle, currencyStyle);

        // Sheet 3: Cost Breakdown
        createCostBreakdownSheet(workbook, customer, titleStyle, headerStyle, currencyStyle);

        // Sheet 4: Tasks/Deliverables
        createTasksSheet(workbook, customer, titleStyle, headerStyle);

        // Sheet 5: Schedule/Timeline
        createScheduleSheet(workbook, customer, titleStyle, headerStyle);

        // Sheet 6: Documents List
        createDocumentsSheet(workbook, customer, titleStyle, headerStyle);

        // Write to file
        try (FileOutputStream fileOut = new FileOutputStream(filePath)) {
            workbook.write(fileOut);
        }

        workbook.close();
    }

    private void createCustomerInfoSheet(Workbook workbook, Customer customer, CellStyle titleStyle, CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet("Customer Information");
        sheet.setColumnWidth(0, 5000);
        sheet.setColumnWidth(1, 8000);

        int rowNum = 0;

        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("CUSTOMER QUOTATION");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));
        rowNum++;

        // Customer Details
        addRow(sheet, rowNum++, "Customer Name:", customer.getName(), headerStyle);
        addRow(sheet, rowNum++, "Email:", customer.getEmail(), null);
        addRow(sheet, rowNum++, "Phone:", customer.getPhone(), null);
        addRow(sheet, rowNum++, "Company:", customer.getCompany(), null);
        addRow(sheet, rowNum++, "Address:", customer.getAddress(), null);
        rowNum++;

        // Dates
        if (customer.getCreatedAt() != null) {
            addRow(sheet, rowNum++, "Created Date:", customer.getCreatedAt().format(DATETIME_FORMATTER), null);
        }

        // Statistics
        rowNum++;
        addRow(sheet, rowNum++, "Total Elements:", String.valueOf(customerElementService.getElementCount(customer.getId())), headerStyle);
        addRow(sheet, rowNum++, "Pending Tasks:", String.valueOf(customerTaskService.getPendingTaskCount(customer.getId())), headerStyle);
        addRow(sheet, rowNum++, "Total Documents:", String.valueOf(customerDocumentService.getDocumentCount(customer.getId())), headerStyle);
    }

    private void createElementsSheet(Workbook workbook, Customer customer, CellStyle titleStyle, CellStyle headerStyle, CellStyle currencyStyle) {
        Sheet sheet = workbook.createSheet("Elements & Pricing");

        // Set column widths
        sheet.setColumnWidth(0, 3000);  // ID
        sheet.setColumnWidth(1, 5000);  // Manufacture
        sheet.setColumnWidth(2, 8000);  // Product Name
        sheet.setColumnWidth(3, 4000);  // Code
        sheet.setColumnWidth(4, 3000);  // Qty
        sheet.setColumnWidth(5, 4000);  // Unit Price
        sheet.setColumnWidth(6, 4000);  // Total Price
        sheet.setColumnWidth(7, 4000);  // Status

        int rowNum = 0;

        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("ELEMENTS & PRICING DETAILS");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));
        rowNum++;

        // Header row
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"ID", "Manufacture", "Product Name", "Code", "Quantity", "Unit Price", "Total Price", "Status"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data rows
        List<CustomerElement> elements = customerElementService.getCustomerElements(customer.getId());
        BigDecimal grandTotal = BigDecimal.ZERO;

        for (CustomerElement element : elements) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(element.getId());
            row.createCell(1).setCellValue(element.getStorageItem().getManufacture());
            row.createCell(2).setCellValue(element.getStorageItem().getProductName());
            row.createCell(3).setCellValue(element.getStorageItem().getCode());
            row.createCell(4).setCellValue(element.getQuantityNeeded());

            Cell unitPriceCell = row.createCell(5);
            if (element.getUnitPrice() != null) {
                unitPriceCell.setCellValue(element.getUnitPrice().doubleValue());
                unitPriceCell.setCellStyle(currencyStyle);
            }

            Cell totalPriceCell = row.createCell(6);
            if (element.getTotalPrice() != null) {
                totalPriceCell.setCellValue(element.getTotalPrice().doubleValue());
                totalPriceCell.setCellStyle(currencyStyle);
                grandTotal = grandTotal.add(element.getTotalPrice());
            }

            row.createCell(7).setCellValue(element.getStatus());
        }

        // Grand Total
        rowNum++;
        Row totalRow = sheet.createRow(rowNum);
        Cell totalLabelCell = totalRow.createCell(5);
        totalLabelCell.setCellValue("GRAND TOTAL:");
        totalLabelCell.setCellStyle(headerStyle);

        Cell totalValueCell = totalRow.createCell(6);
        totalValueCell.setCellValue(grandTotal.doubleValue());
        totalValueCell.setCellStyle(currencyStyle);
    }

    private void createCostBreakdownSheet(Workbook workbook, Customer customer, CellStyle titleStyle, CellStyle headerStyle, CellStyle currencyStyle) {
        Sheet sheet = workbook.createSheet("Cost Breakdown");
        sheet.setColumnWidth(0, 8000);
        sheet.setColumnWidth(1, 5000);

        int rowNum = 0;

        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("COST BREAKDOWN SUMMARY");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));
        rowNum++;

        // Calculate costs
        BigDecimal materialsTotal = customerElementService.calculateTotalCost(customer.getId());

        // Cost breakdown
        addCostRow(sheet, rowNum++, "Materials & Equipment:", materialsTotal, headerStyle, currencyStyle);
        rowNum++;
        addCostRow(sheet, rowNum++, "SUBTOTAL:", materialsTotal, headerStyle, currencyStyle);

        // Add space for manual entries
        rowNum++;
        addRow(sheet, rowNum++, "Tax (Manual Entry):", "", headerStyle);
        addRow(sheet, rowNum++, "Discount (Manual Entry):", "", headerStyle);
        addRow(sheet, rowNum++, "Labor Cost (Manual Entry):", "", headerStyle);
        addRow(sheet, rowNum++, "Additional Materials (Manual Entry):", "", headerStyle);
        rowNum++;
        addRow(sheet, rowNum++, "GRAND TOTAL:", "[Calculate Manually]", headerStyle);
    }

    private void createTasksSheet(Workbook workbook, Customer customer, CellStyle titleStyle, CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet("Tasks & Deliverables");

        sheet.setColumnWidth(0, 3000);
        sheet.setColumnWidth(1, 8000);
        sheet.setColumnWidth(2, 10000);
        sheet.setColumnWidth(3, 3000);
        sheet.setColumnWidth(4, 4000);
        sheet.setColumnWidth(5, 4000);

        int rowNum = 0;

        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("TASKS & DELIVERABLES");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));
        rowNum++;

        // Header
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"ID", "Task Title", "Details", "Priority", "Status", "Assigned To"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data
        List<CustomerTask> tasks = customerTaskService.getCustomerTasks(customer.getId());
        for (CustomerTask task : tasks) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(task.getId());
            row.createCell(1).setCellValue(task.getTaskTitle());
            row.createCell(2).setCellValue(task.getTaskDetails() != null ? task.getTaskDetails() : "");
            row.createCell(3).setCellValue(task.getPriority() != null ? task.getPriority() : "MEDIUM");
            row.createCell(4).setCellValue(task.getIsCompleted() ? "✓ Completed" : "⏳ Pending");
            row.createCell(5).setCellValue(task.getAssignedTo() != null ? task.getAssignedTo() : "");
        }
    }

    private void createScheduleSheet(Workbook workbook, Customer customer, CellStyle titleStyle, CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet("Schedule & Timeline");

        sheet.setColumnWidth(0, 3000);
        sheet.setColumnWidth(1, 8000);
        sheet.setColumnWidth(2, 4000);
        sheet.setColumnWidth(3, 4000);
        sheet.setColumnWidth(4, 10000);
        sheet.setColumnWidth(5, 4000);

        int rowNum = 0;

        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("DELIVERY & INSTALLATION SCHEDULE");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));
        rowNum++;

        // Header
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"ID", "Task Name", "Start Date", "End Date", "Description", "Status"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data
        List<CustomerSchedule> schedules = customerScheduleService.getCustomerSchedules(customer.getId());
        for (CustomerSchedule schedule : schedules) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(schedule.getId());
            row.createCell(1).setCellValue(schedule.getTaskName());
            row.createCell(2).setCellValue(schedule.getStartDate() != null ? schedule.getStartDate().format(DATE_FORMATTER) : "");
            row.createCell(3).setCellValue(schedule.getEndDate() != null ? schedule.getEndDate().format(DATE_FORMATTER) : "");
            row.createCell(4).setCellValue(schedule.getDescription() != null ? schedule.getDescription() : "");
            row.createCell(5).setCellValue(schedule.getStatus() != null ? schedule.getStatus() : "SCHEDULED");
        }
    }

    private void createDocumentsSheet(Workbook workbook, Customer customer, CellStyle titleStyle, CellStyle headerStyle) {
        Sheet sheet = workbook.createSheet("Documents");

        sheet.setColumnWidth(0, 3000);
        sheet.setColumnWidth(1, 8000);
        sheet.setColumnWidth(2, 4000);
        sheet.setColumnWidth(3, 4000);
        sheet.setColumnWidth(4, 10000);
        sheet.setColumnWidth(5, 5000);

        int rowNum = 0;

        // Title
        Row titleRow = sheet.createRow(rowNum++);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("DOCUMENTS & CONTRACTS");
        titleCell.setCellStyle(titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 5));
        rowNum++;

        // Header
        Row headerRow = sheet.createRow(rowNum++);
        String[] headers = {"ID", "Document Name", "Type", "Category", "Description", "Date Uploaded"};
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }

        // Data
        List<CustomerDocument> documents = customerDocumentService.getCustomerDocuments(customer.getId());
        for (CustomerDocument doc : documents) {
            Row row = sheet.createRow(rowNum++);
            row.createCell(0).setCellValue(doc.getId());
            row.createCell(1).setCellValue(doc.getDocumentName());
            row.createCell(2).setCellValue(doc.getDocumentType());
            row.createCell(3).setCellValue(doc.getCategory() != null ? doc.getCategory() : "OTHER");
            row.createCell(4).setCellValue(doc.getDescription() != null ? doc.getDescription() : "");
            row.createCell(5).setCellValue(doc.getDateUploaded() != null ? doc.getDateUploaded().format(DATETIME_FORMATTER) : "");
        }
    }

    // Helper methods
    private void addRow(Sheet sheet, int rowNum, String label, String value, CellStyle labelStyle) {
        Row row = sheet.createRow(rowNum);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        if (labelStyle != null) {
            labelCell.setCellStyle(labelStyle);
        }
        row.createCell(1).setCellValue(value);
    }

    private void addCostRow(Sheet sheet, int rowNum, String label, BigDecimal value, CellStyle labelStyle, CellStyle valueStyle) {
        Row row = sheet.createRow(rowNum);
        Cell labelCell = row.createCell(0);
        labelCell.setCellValue(label);
        if (labelStyle != null) {
            labelCell.setCellStyle(labelStyle);
        }

        Cell valueCell = row.createCell(1);
        if (value != null) {
            valueCell.setCellValue(value.doubleValue());
            valueCell.setCellStyle(valueStyle);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 12);
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
        return style;
    }

    private CellStyle createTitleStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 16);
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createCurrencyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("$#,##0.00"));
        return style;
    }
}

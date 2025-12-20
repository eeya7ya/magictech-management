package com.magictech.modules.sales.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Service for extracting and parsing Excel files from ZIP archives
 * Supports ZIP files containing multiple Excel files with multiple sheets
 */
@Service
public class ZipExcelExtractorService {

    @Autowired
    private SiteSurveyExcelService siteSurveyExcelService;

    @Autowired
    private ExcelStorageService excelStorageService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Extract and parse all Excel files from a ZIP archive
     * Returns comprehensive JSON with all files and their sheets
     *
     * @param zipBytes Raw ZIP file bytes
     * @param zipFileName Original ZIP file name
     * @return JSON string with all extracted Excel data
     */
    public String extractAndParseZipFile(byte[] zipBytes, String zipFileName) throws IOException {
        ObjectNode rootNode = objectMapper.createObjectNode();
        rootNode.put("zipFileName", zipFileName);
        rootNode.put("zipSize", zipBytes.length);
        rootNode.put("extractedAt", java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_DATE_TIME));

        List<ExcelFileData> excelFiles = extractExcelFilesFromZip(zipBytes);
        rootNode.put("excelFileCount", excelFiles.size());

        ArrayNode filesArray = objectMapper.createArrayNode();

        // Parse each Excel file
        int totalSheets = 0;
        for (ExcelFileData fileData : excelFiles) {
            try {
                // Parse the Excel file using SiteSurveyExcelService (reads all sheets)
                String parsedJson = siteSurveyExcelService.parseExcelToJson(fileData.fileBytes, fileData.fileName);

                // Add to the files array
                ObjectNode fileNode = (ObjectNode) objectMapper.readTree(parsedJson);
                fileNode.put("originalFileName", fileData.fileName);
                fileNode.put("fileSize", fileData.fileBytes.length);

                filesArray.add(fileNode);

                // Count total sheets
                if (fileNode.has("numberOfSheets")) {
                    totalSheets += fileNode.get("numberOfSheets").asInt();
                }

            } catch (Exception e) {
                // If parsing fails for a file, add error info
                ObjectNode errorNode = objectMapper.createObjectNode();
                errorNode.put("fileName", fileData.fileName);
                errorNode.put("fileSize", fileData.fileBytes.length);
                errorNode.put("error", "Failed to parse: " + e.getMessage());
                filesArray.add(errorNode);
            }
        }

        rootNode.set("excelFiles", filesArray);
        rootNode.put("totalSheets", totalSheets);

        return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(rootNode);
    }

    /**
     * Extract all Excel files from ZIP archive
     *
     * @param zipBytes ZIP file bytes
     * @return List of Excel file data
     */
    private List<ExcelFileData> extractExcelFilesFromZip(byte[] zipBytes) throws IOException {
        List<ExcelFileData> excelFiles = new ArrayList<>();

        try (ByteArrayInputStream bis = new ByteArrayInputStream(zipBytes);
             ZipInputStream zis = new ZipInputStream(bis)) {

            ZipEntry entry;
            while ((entry = zis.getNextEntry()) != null) {
                // Skip directories
                if (entry.isDirectory()) {
                    continue;
                }

                String fileName = entry.getName();

                // Only process Excel files (.xlsx, .xls)
                if (isExcelFile(fileName)) {
                    // Read the Excel file bytes
                    byte[] fileBytes = readEntryBytes(zis);

                    ExcelFileData fileData = new ExcelFileData();
                    fileData.fileName = getFileNameWithoutPath(fileName);
                    fileData.fileBytes = fileBytes;

                    excelFiles.add(fileData);

                    System.out.println("📄 Extracted Excel file from ZIP: " + fileData.fileName +
                            " (Size: " + formatFileSize(fileBytes.length) + ")");
                }

                zis.closeEntry();
            }
        }

        return excelFiles;
    }

    /**
     * Read all bytes from a ZIP entry
     */
    private byte[] readEntryBytes(ZipInputStream zis) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int len;
        while ((len = zis.read(buffer)) > 0) {
            bos.write(buffer, 0, len);
        }
        return bos.toByteArray();
    }

    /**
     * Check if file is an Excel file by extension
     */
    private boolean isExcelFile(String fileName) {
        if (fileName == null) {
            return false;
        }
        String lowerFileName = fileName.toLowerCase();
        return lowerFileName.endsWith(".xlsx") || lowerFileName.endsWith(".xls");
    }

    /**
     * Extract file name without directory path
     */
    private String getFileNameWithoutPath(String fullPath) {
        if (fullPath == null) {
            return null;
        }

        // Handle both Windows and Unix path separators
        int lastSlash = Math.max(fullPath.lastIndexOf('/'), fullPath.lastIndexOf('\\'));
        if (lastSlash >= 0) {
            return fullPath.substring(lastSlash + 1);
        }
        return fullPath;
    }

    /**
     * Format file size to human-readable format
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
    }

    /**
     * Validate ZIP file
     */
    public boolean isValidZipFile(byte[] fileBytes, String fileName) {
        if (fileBytes == null || fileBytes.length == 0) {
            return false;
        }

        if (fileName != null && !fileName.toLowerCase().endsWith(".zip")) {
            return false;
        }

        try (ByteArrayInputStream bis = new ByteArrayInputStream(fileBytes);
             ZipInputStream zis = new ZipInputStream(bis)) {

            // Try to read at least one entry
            return zis.getNextEntry() != null;

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get summary of ZIP contents without full parsing
     */
    public String getZipSummary(byte[] zipBytes) {
        try {
            List<ExcelFileData> excelFiles = extractExcelFilesFromZip(zipBytes);

            ObjectNode summary = objectMapper.createObjectNode();
            summary.put("excelFileCount", excelFiles.size());

            ArrayNode fileNames = objectMapper.createArrayNode();
            long totalSize = 0;
            for (ExcelFileData file : excelFiles) {
                fileNames.add(file.fileName);
                totalSize += file.fileBytes.length;
            }

            summary.set("excelFileNames", fileNames);
            summary.put("totalExcelSize", formatFileSize(totalSize));

            return objectMapper.writeValueAsString(summary);

        } catch (Exception e) {
            return "{\"error\": \"Failed to read ZIP: " + e.getMessage() + "\"}";
        }
    }

    /**
     * Internal class to hold Excel file data
     */
    private static class ExcelFileData {
        String fileName;
        byte[] fileBytes;
    }
}

package com.magictech.modules.projects.service;

import com.magictech.modules.projects.entity.Project;
import com.magictech.modules.projects.entity.ProjectDocument;
import com.magictech.modules.projects.repository.ProjectDocumentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.file.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

/**
 * Project Document Service
 * Handles file upload, storage, retrieval, and deletion for project documents
 */
@Service
@Transactional
public class ProjectDocumentService {

    @Autowired
    private ProjectDocumentRepository documentRepository;

    @Value("${app.document.storage.path:./data/documents/projects}")
    private String documentStoragePath;

    /**
     * Save a document (file) for a project
     */
    public ProjectDocument saveDocument(Project project, File sourceFile, String category, String description, String uploadedBy) throws IOException {
        // Create storage directory if not exists
        Path storagePath = Paths.get(documentStoragePath, String.valueOf(project.getId()));
        if (!Files.exists(storagePath)) {
            Files.createDirectories(storagePath);
        }

        // Generate unique filename
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String originalFilename = sourceFile.getName();
        String fileExtension = getFileExtension(originalFilename);
        String newFilename = timestamp + "_" + sanitizeFilename(originalFilename);

        // Copy file to storage
        Path targetPath = storagePath.resolve(newFilename);
        Files.copy(sourceFile.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);

        // Create database record
        ProjectDocument document = new ProjectDocument();
        document.setProject(project);
        document.setDocumentName(originalFilename);
        document.setDocumentType(fileExtension);
        document.setFilePath(targetPath.toString());
        document.setFileSize(Files.size(targetPath));
        document.setCategory(category != null ? category : "OTHER");
        document.setDescription(description);
        document.setUploadedBy(uploadedBy);
        document.setDateUploaded(LocalDateTime.now());

        return documentRepository.save(document);
    }

    /**
     * Get all documents for a project
     */
    public List<ProjectDocument> getProjectDocuments(Long projectId) {
        return documentRepository.findByProjectIdAndActiveTrue(projectId);
    }

    /**
     * Get documents by category
     */
    public List<ProjectDocument> getProjectDocumentsByCategory(Long projectId, String category) {
        return documentRepository.findByProjectIdAndCategoryAndActiveTrue(projectId, category);
    }

    /**
     * Get document by ID
     */
    public Optional<ProjectDocument> getDocumentById(Long documentId) {
        return documentRepository.findById(documentId);
    }

    /**
     * Download document (returns File object)
     */
    public File downloadDocument(Long documentId) throws IOException {
        Optional<ProjectDocument> docOpt = documentRepository.findById(documentId);
        if (docOpt.isEmpty()) {
            throw new FileNotFoundException("Document not found: " + documentId);
        }

        ProjectDocument document = docOpt.get();
        File file = new File(document.getFilePath());

        if (!file.exists()) {
            throw new FileNotFoundException("Physical file not found: " + document.getFilePath());
        }

        // Update last accessed time
        document.setLastAccessed(LocalDateTime.now());
        documentRepository.save(document);

        return file;
    }

    /**
     * Delete document (soft delete)
     */
    public void deleteDocument(Long documentId) {
        Optional<ProjectDocument> docOpt = documentRepository.findById(documentId);
        if (docOpt.isPresent()) {
            ProjectDocument document = docOpt.get();
            document.setActive(false);
            documentRepository.save(document);
        }
    }

    /**
     * Delete document permanently (removes file and database record)
     */
    public void deleteDocumentPermanently(Long documentId) throws IOException {
        Optional<ProjectDocument> docOpt = documentRepository.findById(documentId);
        if (docOpt.isPresent()) {
            ProjectDocument document = docOpt.get();

            // Delete physical file
            File file = new File(document.getFilePath());
            if (file.exists()) {
                Files.delete(file.toPath());
            }

            // Delete database record
            documentRepository.deleteById(documentId);
        }
    }

    /**
     * Get document count for project
     */
    public long getDocumentCount(Long projectId) {
        return documentRepository.countByProjectIdAndActiveTrue(projectId);
    }

    /**
     * Get file extension
     */
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1).toUpperCase();
        }
        return "UNKNOWN";
    }

    /**
     * Sanitize filename (remove special characters)
     */
    private String sanitizeFilename(String filename) {
        return filename.replaceAll("[^a-zA-Z0-9.-]", "_");
    }

    /**
     * Get storage path
     */
    public String getStoragePath() {
        return documentStoragePath;
    }
}

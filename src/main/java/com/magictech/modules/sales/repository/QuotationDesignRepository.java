package com.magictech.modules.sales.repository;

import com.magictech.modules.sales.entity.QuotationDesign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for QuotationDesign entity.
 * Provides data access methods for quotation PDF documents.
 */
@Repository
public interface QuotationDesignRepository extends JpaRepository<QuotationDesign, Long> {

    // Find current version for an entity
    Optional<QuotationDesign> findByEntityTypeAndEntityIdAndIsCurrentVersionTrueAndActiveTrue(
            String entityType, Long entityId);

    // Find all versions for an entity (for version history)
    List<QuotationDesign> findByEntityTypeAndEntityIdAndActiveTrueOrderByVersionDesc(
            String entityType, Long entityId);

    // Find by entity type and ID (current version only)
    @Query("SELECT q FROM QuotationDesign q WHERE q.entityType = :entityType " +
           "AND q.entityId = :entityId AND q.isCurrentVersion = true AND q.active = true")
    Optional<QuotationDesign> findCurrentVersion(
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId);

    // Find all active quotation designs
    List<QuotationDesign> findByActiveTrue();

    // Find by module source
    List<QuotationDesign> findByModuleSourceAndActiveTrue(String moduleSource);

    // Find by module source and current version only
    List<QuotationDesign> findByModuleSourceAndIsCurrentVersionTrueAndActiveTrue(String moduleSource);

    // Count versions for an entity
    @Query("SELECT COUNT(q) FROM QuotationDesign q WHERE q.entityType = :entityType " +
           "AND q.entityId = :entityId AND q.active = true")
    Long countVersions(@Param("entityType") String entityType, @Param("entityId") Long entityId);

    // Find specific version
    @Query("SELECT q FROM QuotationDesign q WHERE q.entityType = :entityType " +
           "AND q.entityId = :entityId AND q.version = :version AND q.active = true")
    Optional<QuotationDesign> findByVersion(
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId,
            @Param("version") Integer version);

    // Find by created user
    List<QuotationDesign> findByCreatedByAndActiveTrue(String createdBy);

    // Find quotations with annotations
    @Query("SELECT q FROM QuotationDesign q WHERE q.pdfAnnotations IS NOT NULL " +
           "AND q.pdfAnnotations != '' AND q.pdfAnnotations != '[]' " +
           "AND q.isCurrentVersion = true AND q.active = true")
    List<QuotationDesign> findAllWithAnnotations();

    // Delete all versions for an entity (soft delete)
    @Query("UPDATE QuotationDesign q SET q.active = false WHERE q.entityType = :entityType AND q.entityId = :entityId")
    void softDeleteAllVersions(@Param("entityType") String entityType, @Param("entityId") Long entityId);
}

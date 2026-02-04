package com.magictech.modules.qualityassurance.repository;

import com.magictech.modules.qualityassurance.entity.PriceList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for PriceList entity
 */
@Repository
public interface PriceListRepository extends JpaRepository<PriceList, Long> {

    /**
     * Find all active price lists
     */
    List<PriceList> findByActiveTrue();

    /**
     * Find all active price lists ordered by upload date (newest first)
     */
    List<PriceList> findByActiveTrueOrderByUploadedAtDesc();

    /**
     * Find the latest active price list
     */
    Optional<PriceList> findFirstByActiveTrueOrderByUploadedAtDesc();

    /**
     * Find price lists by uploader
     */
    List<PriceList> findByUploadedByAndActiveTrue(String uploadedBy);

    /**
     * Find price list by filename
     */
    Optional<PriceList> findByFilenameAndActiveTrue(String filename);

    /**
     * Get the current version number for a filename (for versioning)
     */
    @Query("SELECT COALESCE(MAX(p.version), 0) FROM PriceList p WHERE p.filename = :filename")
    Integer getMaxVersionForFilename(String filename);

    /**
     * Count active price lists
     */
    long countByActiveTrue();
}

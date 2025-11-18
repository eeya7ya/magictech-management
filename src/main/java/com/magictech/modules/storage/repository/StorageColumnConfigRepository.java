package com.magictech.modules.storage.repository;

import com.magictech.modules.storage.entity.StorageColumnConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Storage Column Configuration
 */
@Repository
public interface StorageColumnConfigRepository extends JpaRepository<StorageColumnConfig, Long> {

    /**
     * Find all visible columns ordered by display order
     */
    List<StorageColumnConfig> findByVisibleTrueOrderByDisplayOrderAsc();

    /**
     * Find all columns ordered by display order
     */
    List<StorageColumnConfig> findAllByOrderByDisplayOrderAsc();

    /**
     * Find column by name
     */
    Optional<StorageColumnConfig> findByColumnName(String columnName);

    /**
     * Check if column name exists
     */
    boolean existsByColumnName(String columnName);

    /**
     * Find all default (built-in) columns
     */
    List<StorageColumnConfig> findByIsDefaultTrue();

    /**
     * Find all custom (user-added) columns
     */
    List<StorageColumnConfig> findByIsDefaultFalse();
}
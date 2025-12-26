package com.magictech.modules.storage.repository;

import com.magictech.modules.storage.entity.StorageLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for StorageLocation entity
 */
@Repository
public interface StorageLocationRepository extends JpaRepository<StorageLocation, Long> {

    // Find all active storage locations
    List<StorageLocation> findByActiveTrueOrderByDisplayOrderAsc();

    // Find by code
    Optional<StorageLocation> findByCodeAndActiveTrue(String code);

    // Find by name (case insensitive)
    List<StorageLocation> findByNameContainingIgnoreCaseAndActiveTrue(String name);

    // Find by city
    List<StorageLocation> findByCityAndActiveTrue(String city);

    // Find all cities (distinct)
    @Query("SELECT DISTINCT sl.city FROM StorageLocation sl WHERE sl.active = true AND sl.city IS NOT NULL ORDER BY sl.city")
    List<String> findAllCities();

    // Search by name, code, or city
    @Query("SELECT sl FROM StorageLocation sl WHERE sl.active = true AND " +
           "(LOWER(sl.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(sl.code) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(sl.city) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<StorageLocation> searchLocations(@Param("searchTerm") String searchTerm);

    // Count active locations
    long countByActiveTrue();

    // Find locations with map coordinates
    @Query("SELECT sl FROM StorageLocation sl WHERE sl.active = true AND sl.mapX IS NOT NULL AND sl.mapY IS NOT NULL ORDER BY sl.displayOrder")
    List<StorageLocation> findLocationsWithMapCoordinates();
}

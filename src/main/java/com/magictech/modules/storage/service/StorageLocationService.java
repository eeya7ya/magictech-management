package com.magictech.modules.storage.service;

import com.magictech.modules.storage.entity.StorageLocation;
import com.magictech.modules.storage.repository.StorageLocationRepository;
import com.magictech.modules.storage.repository.StorageItemLocationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Service for managing storage locations
 */
@Service
@Transactional
public class StorageLocationService {

    @Autowired
    private StorageLocationRepository locationRepository;

    @Autowired
    private StorageItemLocationRepository itemLocationRepository;

    // ==================== CRUD Operations ====================

    public StorageLocation createLocation(StorageLocation location) {
        return locationRepository.save(location);
    }

    public StorageLocation updateLocation(Long id, StorageLocation updatedLocation) {
        StorageLocation existing = locationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Storage location not found: " + id));

        existing.setName(updatedLocation.getName());
        existing.setCode(updatedLocation.getCode());
        existing.setCity(updatedLocation.getCity());
        existing.setAddress(updatedLocation.getAddress());
        existing.setDescription(updatedLocation.getDescription());
        existing.setLatitude(updatedLocation.getLatitude());
        existing.setLongitude(updatedLocation.getLongitude());
        existing.setMapX(updatedLocation.getMapX());
        existing.setMapY(updatedLocation.getMapY());
        existing.setManagerName(updatedLocation.getManagerName());
        existing.setPhone(updatedLocation.getPhone());
        existing.setEmail(updatedLocation.getEmail());
        existing.setColor(updatedLocation.getColor());
        existing.setIcon(updatedLocation.getIcon());
        existing.setMaxCapacity(updatedLocation.getMaxCapacity());
        existing.setDisplayOrder(updatedLocation.getDisplayOrder());

        return locationRepository.save(existing);
    }

    public void deleteLocation(Long id) {
        StorageLocation location = locationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Storage location not found: " + id));
        location.setActive(false);
        locationRepository.save(location);
    }

    public void hardDeleteLocation(Long id) {
        locationRepository.deleteById(id);
    }

    // ==================== Read Operations ====================

    public List<StorageLocation> getAllActiveLocations() {
        return locationRepository.findByActiveTrueOrderByDisplayOrderAsc();
    }

    public Optional<StorageLocation> findById(Long id) {
        return locationRepository.findById(id);
    }

    public Optional<StorageLocation> findByCode(String code) {
        return locationRepository.findByCodeAndActiveTrue(code);
    }

    public List<StorageLocation> searchLocations(String searchTerm) {
        return locationRepository.searchLocations(searchTerm);
    }

    public List<StorageLocation> findByCity(String city) {
        return locationRepository.findByCityAndActiveTrue(city);
    }

    public List<String> getAllCities() {
        return locationRepository.findAllCities();
    }

    public List<StorageLocation> getLocationsWithMapCoordinates() {
        return locationRepository.findLocationsWithMapCoordinates();
    }

    // ==================== Statistics ====================

    public long getLocationCount() {
        return locationRepository.countByActiveTrue();
    }

    public int getItemCountInLocation(Long locationId) {
        return (int) itemLocationRepository.countItemsInLocation(locationId);
    }

    public int getTotalQuantityInLocation(Long locationId) {
        return itemLocationRepository.sumQuantityInLocation(locationId);
    }

    /**
     * DTO for location summary with item counts
     */
    public static class LocationSummary {
        private Long locationId;
        private String locationName;
        private String locationCode;
        private String city;
        private Double mapX;
        private Double mapY;
        private String color;
        private String icon;
        private int itemCount;
        private int totalQuantity;

        public LocationSummary(StorageLocation location, int itemCount, int totalQuantity) {
            this.locationId = location.getId();
            this.locationName = location.getName();
            this.locationCode = location.getCode();
            this.city = location.getCity();
            this.mapX = location.getMapX();
            this.mapY = location.getMapY();
            this.color = location.getColor();
            this.icon = location.getIcon();
            this.itemCount = itemCount;
            this.totalQuantity = totalQuantity;
        }

        // Getters
        public Long getLocationId() { return locationId; }
        public String getLocationName() { return locationName; }
        public String getLocationCode() { return locationCode; }
        public String getCity() { return city; }
        public Double getMapX() { return mapX; }
        public Double getMapY() { return mapY; }
        public String getColor() { return color; }
        public String getIcon() { return icon; }
        public int getItemCount() { return itemCount; }
        public int getTotalQuantity() { return totalQuantity; }
    }

    public List<LocationSummary> getAllLocationSummaries() {
        List<StorageLocation> locations = getAllActiveLocations();
        return locations.stream()
                .map(loc -> new LocationSummary(
                        loc,
                        getItemCountInLocation(loc.getId()),
                        getTotalQuantityInLocation(loc.getId())
                ))
                .toList();
    }

    // ==================== Initialization ====================

    /**
     * Initialize default Jordan storage locations if none exist
     * Most locations are in Amman (industrial areas) with a few in other cities
     * Coordinates are percentage-based (0-100) mapped to Jordan's actual geography
     *
     * Map coordinate system (600x700 pixels):
     * - Amman area: roughly X: 26-42%, Y: 28-44%
     * - Irbid (north): X: ~27%, Y: ~18%
     * - Mafraq (northeast): X: ~60%, Y: ~22%
     * - Aqaba (south): X: ~9%, Y: ~73%
     */
    public void initializeDefaultLocations() {
        if (locationRepository.countByActiveTrue() == 0) {
            System.out.println("Initializing default storage locations...");

            // === AMMAN LOCATIONS (8 locations) - spread across Greater Amman area ===
            // Coordinates carefully positioned to avoid overlap and match real geography

            // Sahab Industrial - southeast Amman industrial zone
            createDefaultLocation("Sahab Industrial", "SAH", "Amman", 36.0, 42.0, "#3b82f6", "🏭", 1);

            // Marka Warehouse - northeast Amman
            createDefaultLocation("Marka Warehouse", "MRK", "Amman", 33.0, 32.0, "#22c55e", "📦", 2);

            // Airport Free Zone - Queen Alia Airport area (south of Amman)
            createDefaultLocation("Airport Free Zone", "AFZ", "Amman", 34.0, 48.0, "#f59e0b", "✈️", 3);

            // Abu Alanda - east Amman
            createDefaultLocation("Abu Alanda", "ABA", "Amman", 38.0, 36.0, "#8b5cf6", "🏢", 4);

            // Khalda Center - west Amman (upscale area)
            createDefaultLocation("Khalda Center", "KHL", "Amman", 27.0, 30.0, "#ec4899", "🏬", 5);

            // Al-Qastal - south of Amman on Desert Highway
            createDefaultLocation("Al-Qastal", "QST", "Amman", 32.0, 52.0, "#14b8a6", "🏗️", 6);

            // Tabarbour - north Amman
            createDefaultLocation("Tabarbour", "TBR", "Amman", 30.0, 28.0, "#6366f1", "📦", 7);

            // Al-Juwaideh - south central Amman
            createDefaultLocation("Al-Juwaideh", "JWD", "Amman", 31.0, 38.0, "#84cc16", "🏭", 8);

            // === OTHER CITIES (4 locations) ===

            // Zarqa Industrial - east of Amman, Jordan's industrial hub
            createDefaultLocation("Zarqa Industrial", "ZRQ", "Zarqa", 42.0, 34.0, "#ef4444", "🏭", 9);

            // Irbid Branch - northern Jordan, second largest city
            createDefaultLocation("Irbid Branch", "IRB", "Irbid", 27.0, 18.0, "#06b6d4", "📦", 10);

            // Aqaba Port - southern tip, Red Sea port city
            createDefaultLocation("Aqaba Port", "AQB", "Aqaba", 9.0, 73.0, "#0ea5e9", "⚓", 11);

            // Mafraq Depot - northeastern Jordan, near Syrian border
            createDefaultLocation("Mafraq Depot", "MFQ", "Mafraq", 58.0, 22.0, "#64748b", "🌾", 12);

            System.out.println("✓ Default storage locations initialized (8 in Amman, 4 in other cities)");
        }
    }

    private void createDefaultLocation(String name, String code, String city,
                                        Double mapX, Double mapY, String color, String icon, int order) {
        StorageLocation location = new StorageLocation();
        location.setName(name + " Storage");
        location.setCode(code);
        location.setCity(city);
        location.setMapX(mapX);
        location.setMapY(mapY);
        location.setColor(color);
        location.setIcon(icon);
        location.setDisplayOrder(order);
        location.setCreatedBy("system");
        locationRepository.save(location);
    }
}

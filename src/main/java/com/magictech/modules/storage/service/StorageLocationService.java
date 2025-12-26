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
     */
    public void initializeDefaultLocations() {
        if (locationRepository.countByActiveTrue() == 0) {
            System.out.println("Initializing default storage locations...");

            // Jordan main cities with approximate map positions (0-100 scale)
            createDefaultLocation("Amman", "AMN", "Amman", 60.0, 45.0, "#3b82f6", "🏢", 1);
            createDefaultLocation("Irbid", "IRB", "Irbid", 55.0, 20.0, "#22c55e", "🏭", 2);
            createDefaultLocation("Zarqa", "ZRQ", "Zarqa", 65.0, 42.0, "#f59e0b", "📦", 3);
            createDefaultLocation("Ajloun", "AJL", "Ajloun", 52.0, 25.0, "#8b5cf6", "🏔️", 4);
            createDefaultLocation("Jerash", "JRS", "Jerash", 56.0, 28.0, "#ef4444", "🏛️", 5);
            createDefaultLocation("Mafraq", "MFQ", "Mafraq", 70.0, 22.0, "#06b6d4", "🌾", 6);
            createDefaultLocation("Balqa", "BLQ", "Balqa", 52.0, 43.0, "#ec4899", "⛰️", 7);
            createDefaultLocation("Madaba", "MDB", "Madaba", 57.0, 52.0, "#14b8a6", "🏺", 8);
            createDefaultLocation("Karak", "KRK", "Karak", 55.0, 68.0, "#f97316", "🏰", 9);
            createDefaultLocation("Tafilah", "TFL", "Tafilah", 52.0, 75.0, "#a855f7", "🌄", 10);
            createDefaultLocation("Ma'an", "MAN", "Ma'an", 55.0, 85.0, "#64748b", "🏜️", 11);
            createDefaultLocation("Aqaba", "AQB", "Aqaba", 50.0, 95.0, "#0ea5e9", "⚓", 12);

            System.out.println("✓ Default storage locations initialized");
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

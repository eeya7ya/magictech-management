package com.magictech.modules.storage.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * StorageLocation Entity - Represents a physical storage location
 * Used for the Road Map view with coordinates for interactive map pins
 */
@Entity
@Table(name = "storage_locations")
public class StorageLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "code", length = 20, unique = true)
    private String code;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Map coordinates for Jordan map visualization
    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;

    // Relative position on SVG map (0-100 percentage)
    @Column(name = "map_x")
    private Double mapX;

    @Column(name = "map_y")
    private Double mapY;

    // Contact information
    @Column(name = "manager_name", length = 100)
    private String managerName;

    @Column(name = "phone", length = 50)
    private String phone;

    @Column(name = "email", length = 100)
    private String email;

    // Visual customization
    @Column(name = "color", length = 20)
    private String color = "#3b82f6";

    @Column(name = "icon", length = 10)
    private String icon = "📦";

    // Capacity tracking
    @Column(name = "max_capacity")
    private Integer maxCapacity;

    // Display order on the map
    @Column(name = "display_order")
    private Integer displayOrder = 0;

    // Metadata fields
    @Column(name = "date_added", nullable = false)
    private LocalDateTime dateAdded;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(nullable = false)
    private Boolean active = true;

    // Constructors
    public StorageLocation() {
        this.dateAdded = LocalDateTime.now();
        this.active = true;
    }

    public StorageLocation(String name, String code, String city, Double mapX, Double mapY) {
        this();
        this.name = name;
        this.code = code;
        this.city = city;
        this.mapX = mapX;
        this.mapY = mapY;
    }

    // Lifecycle callbacks
    @PrePersist
    protected void onCreate() {
        if (dateAdded == null) {
            dateAdded = LocalDateTime.now();
        }
        if (active == null) {
            active = true;
        }
        if (color == null) {
            color = "#3b82f6";
        }
        if (icon == null) {
            icon = "📦";
        }
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdated = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Double getMapX() {
        return mapX;
    }

    public void setMapX(Double mapX) {
        this.mapX = mapX;
    }

    public Double getMapY() {
        return mapY;
    }

    public void setMapY(Double mapY) {
        this.mapY = mapY;
    }

    public String getManagerName() {
        return managerName;
    }

    public void setManagerName(String managerName) {
        this.managerName = managerName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(Integer maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public Integer getDisplayOrder() {
        return displayOrder;
    }

    public void setDisplayOrder(Integer displayOrder) {
        this.displayOrder = displayOrder;
    }

    public LocalDateTime getDateAdded() {
        return dateAdded;
    }

    public void setDateAdded(LocalDateTime dateAdded) {
        this.dateAdded = dateAdded;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    @Override
    public String toString() {
        return "StorageLocation{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", code='" + code + '\'' +
                ", city='" + city + '\'' +
                ", mapX=" + mapX +
                ", mapY=" + mapY +
                ", active=" + active +
                '}';
    }
}

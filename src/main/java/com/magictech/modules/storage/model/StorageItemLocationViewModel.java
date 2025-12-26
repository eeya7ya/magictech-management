package com.magictech.modules.storage.model;

import com.magictech.modules.storage.entity.StorageItemLocation;
import javafx.beans.property.*;

import java.math.BigDecimal;

/**
 * ViewModel for StorageItemLocation - for display in TableView
 * Used in both individual location sheets and total sheet
 */
public class StorageItemLocationViewModel {

    private final LongProperty itemLocationId = new SimpleLongProperty();
    private final LongProperty itemId = new SimpleLongProperty();
    private final StringProperty manufacture = new SimpleStringProperty();
    private final StringProperty productName = new SimpleStringProperty();
    private final StringProperty code = new SimpleStringProperty();
    private final StringProperty serialNumber = new SimpleStringProperty();
    private final IntegerProperty quantity = new SimpleIntegerProperty();
    private final ObjectProperty<BigDecimal> price = new SimpleObjectProperty<>(BigDecimal.ZERO);
    private final StringProperty binLocation = new SimpleStringProperty();
    private final StringProperty locationName = new SimpleStringProperty();
    private final LongProperty locationId = new SimpleLongProperty();
    private final StringProperty locationCode = new SimpleStringProperty();
    private final BooleanProperty lowStock = new SimpleBooleanProperty();
    private final StringProperty availabilityStatus = new SimpleStringProperty();

    public StorageItemLocationViewModel() {
    }

    public StorageItemLocationViewModel(StorageItemLocation entity) {
        if (entity != null) {
            this.itemLocationId.set(entity.getId());
            this.itemId.set(entity.getStorageItem().getId());
            this.manufacture.set(entity.getStorageItem().getManufacture() != null ?
                    entity.getStorageItem().getManufacture() : "");
            this.productName.set(entity.getStorageItem().getProductName());
            this.code.set(entity.getStorageItem().getCode() != null ?
                    entity.getStorageItem().getCode() : "");
            this.serialNumber.set(entity.getStorageItem().getSerialNumber() != null ?
                    entity.getStorageItem().getSerialNumber() : "");
            this.quantity.set(entity.getQuantity() != null ? entity.getQuantity() : 0);
            this.price.set(entity.getStorageItem().getPrice() != null ?
                    entity.getStorageItem().getPrice() : BigDecimal.ZERO);
            this.binLocation.set(entity.getFullBinLocation());
            this.locationName.set(entity.getStorageLocation().getName());
            this.locationId.set(entity.getStorageLocation().getId());
            this.locationCode.set(entity.getStorageLocation().getCode() != null ?
                    entity.getStorageLocation().getCode() : "");
            this.lowStock.set(entity.isLowStock());
            updateAvailabilityStatus();
        }
    }

    public void updateAvailabilityStatus() {
        if (quantity.get() > 0) {
            availabilityStatus.set("✅ Available (" + quantity.get() + ")");
        } else {
            availabilityStatus.set("❌ Out of Stock");
        }
    }

    // Property accessors for JavaFX binding
    public LongProperty itemLocationIdProperty() { return itemLocationId; }
    public LongProperty itemIdProperty() { return itemId; }
    public StringProperty manufactureProperty() { return manufacture; }
    public StringProperty productNameProperty() { return productName; }
    public StringProperty codeProperty() { return code; }
    public StringProperty serialNumberProperty() { return serialNumber; }
    public IntegerProperty quantityProperty() { return quantity; }
    public ObjectProperty<BigDecimal> priceProperty() { return price; }
    public StringProperty binLocationProperty() { return binLocation; }
    public StringProperty locationNameProperty() { return locationName; }
    public LongProperty locationIdProperty() { return locationId; }
    public StringProperty locationCodeProperty() { return locationCode; }
    public BooleanProperty lowStockProperty() { return lowStock; }
    public StringProperty availabilityStatusProperty() { return availabilityStatus; }

    // Standard getters and setters
    public Long getItemLocationId() { return itemLocationId.get(); }
    public void setItemLocationId(Long value) { itemLocationId.set(value); }

    public Long getItemId() { return itemId.get(); }
    public void setItemId(Long value) { itemId.set(value); }

    public String getManufacture() { return manufacture.get(); }
    public void setManufacture(String value) { manufacture.set(value); }

    public String getProductName() { return productName.get(); }
    public void setProductName(String value) { productName.set(value); }

    public String getCode() { return code.get(); }
    public void setCode(String value) { code.set(value); }

    public String getSerialNumber() { return serialNumber.get(); }
    public void setSerialNumber(String value) { serialNumber.set(value); }

    public Integer getQuantity() { return quantity.get(); }
    public void setQuantity(Integer value) {
        quantity.set(value != null ? value : 0);
        updateAvailabilityStatus();
    }

    public BigDecimal getPrice() { return price.get(); }
    public void setPrice(BigDecimal value) { price.set(value != null ? value : BigDecimal.ZERO); }

    public String getBinLocation() { return binLocation.get(); }
    public void setBinLocation(String value) { binLocation.set(value); }

    public String getLocationName() { return locationName.get(); }
    public void setLocationName(String value) { locationName.set(value); }

    public Long getLocationId() { return locationId.get(); }
    public void setLocationId(Long value) { locationId.set(value); }

    public String getLocationCode() { return locationCode.get(); }
    public void setLocationCode(String value) { locationCode.set(value); }

    public boolean isLowStock() { return lowStock.get(); }
    public void setLowStock(boolean value) { lowStock.set(value); }

    public String getAvailabilityStatus() { return availabilityStatus.get(); }

    @Override
    public String toString() {
        return "StorageItemLocationViewModel{" +
                "productName=" + productName.get() +
                ", quantity=" + quantity.get() +
                ", location=" + locationName.get() +
                '}';
    }
}

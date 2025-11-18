package com.magictech.modules.storage.model;

import javafx.beans.property.*;

import java.math.BigDecimal;

/**
 * Storage Item View Model - UPDATED WITH AVAILABILITY STATUS
 * Supports both quantity display and availability status for different modules
 */
public class StorageItemViewModel {

    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty manufacture = new SimpleStringProperty();
    private final StringProperty productName = new SimpleStringProperty();
    private final StringProperty code = new SimpleStringProperty();
    private final StringProperty serialNumber = new SimpleStringProperty();
    private final IntegerProperty quantity = new SimpleIntegerProperty();
    private final ObjectProperty<BigDecimal> price = new SimpleObjectProperty<>();
    private final StringProperty dateAdded = new SimpleStringProperty();

    // NEW: Availability status for modules that don't show numbers
    private final StringProperty availabilityStatus = new SimpleStringProperty();
    private final BooleanProperty isAvailable = new SimpleBooleanProperty();

    public StorageItemViewModel() {}

    public StorageItemViewModel(long id, String manufacture, String productName, String code,
                                String serialNumber, int quantity, BigDecimal price, String dateAdded) {
        setId(id);
        setManufacture(manufacture);
        setProductName(productName);
        setCode(code);
        setSerialNumber(serialNumber);
        setQuantity(quantity);
        setPrice(price);
        setDateAdded(dateAdded);
        updateAvailabilityStatus();
    }

    // ID Property
    public long getId() { return id.get(); }
    public void setId(long value) { id.set(value); }
    public LongProperty idProperty() { return id; }

    // Manufacture Property
    public String getManufacture() { return manufacture.get(); }
    public void setManufacture(String value) { manufacture.set(value); }
    public StringProperty manufactureProperty() { return manufacture; }

    // Product Name Property
    public String getProductName() { return productName.get(); }
    public void setProductName(String value) { productName.set(value); }
    public StringProperty productNameProperty() { return productName; }

    // Code Property
    public String getCode() { return code.get(); }
    public void setCode(String value) { code.set(value); }
    public StringProperty codeProperty() { return code; }

    // Serial Number Property
    public String getSerialNumber() { return serialNumber.get(); }
    public void setSerialNumber(String value) { serialNumber.set(value); }
    public StringProperty serialNumberProperty() { return serialNumber; }

    // Quantity Property
    public int getQuantity() { return quantity.get(); }
    public void setQuantity(int value) {
        quantity.set(value);
        updateAvailabilityStatus();
    }
    public IntegerProperty quantityProperty() { return quantity; }

    // Price Property
    public BigDecimal getPrice() { return price.get(); }
    public void setPrice(BigDecimal value) { price.set(value); }
    public ObjectProperty<BigDecimal> priceProperty() { return price; }

    // Date Added Property
    public String getDateAdded() { return dateAdded.get(); }
    public void setDateAdded(String value) { dateAdded.set(value); }
    public StringProperty dateAddedProperty() { return dateAdded; }

    // NEW: Availability Status Property
    public String getAvailabilityStatus() { return availabilityStatus.get(); }
    public void setAvailabilityStatus(String value) { availabilityStatus.set(value); }
    public StringProperty availabilityStatusProperty() { return availabilityStatus; }

    // NEW: Is Available Property
    public boolean isAvailable() { return isAvailable.get(); }
    public void setIsAvailable(boolean value) { isAvailable.set(value); }
    public BooleanProperty isAvailableProperty() { return isAvailable; }

    /**
     * Update availability status based on quantity
     * Can be customized per module with different thresholds
     */
    public void updateAvailabilityStatus() {
        int qty = getQuantity();
        if (qty > 0) {
            setAvailabilityStatus("✅ Available");
            setIsAvailable(true);
        } else {
            setAvailabilityStatus("❌ Not Available");
            setIsAvailable(false);
        }
    }

    /**
     * Update availability with custom threshold
     */
    public void updateAvailabilityStatus(int threshold) {
        int qty = getQuantity();
        if (qty >= threshold) {
            setAvailabilityStatus("✅ Available");
            setIsAvailable(true);
        } else {
            setAvailabilityStatus("❌ Not Available");
            setIsAvailable(false);
        }
    }

    @Override
    public String toString() {
        return "StorageItemViewModel{" +
                "id=" + getId() +
                ", manufacture='" + getManufacture() + '\'' +
                ", productName='" + getProductName() + '\'' +
                ", code='" + getCode() + '\'' +
                ", serialNumber='" + getSerialNumber() + '\'' +
                ", quantity=" + getQuantity() +
                ", availabilityStatus='" + getAvailabilityStatus() + '\'' +
                ", price=" + getPrice() +
                '}';
    }
}
package com.magictech.modules.sales.model;

import javafx.beans.property.*;

public class CustomerElementViewModel {

    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty storageItemName = new SimpleStringProperty();
    private final IntegerProperty quantityNeeded = new SimpleIntegerProperty();
    private final IntegerProperty quantityAllocated = new SimpleIntegerProperty();
    private final StringProperty status = new SimpleStringProperty();
    private final StringProperty notes = new SimpleStringProperty();

    public CustomerElementViewModel() {}

    // ID
    public long getId() { return id.get(); }
    public void setId(long value) { id.set(value); }
    public LongProperty idProperty() { return id; }

    // Storage Item Name
    public String getStorageItemName() { return storageItemName.get(); }
    public void setStorageItemName(String value) { storageItemName.set(value); }
    public StringProperty storageItemNameProperty() { return storageItemName; }

    // Quantity Needed
    public int getQuantityNeeded() { return quantityNeeded.get(); }
    public void setQuantityNeeded(int value) { quantityNeeded.set(value); }
    public IntegerProperty quantityNeededProperty() { return quantityNeeded; }

    // Quantity Allocated
    public int getQuantityAllocated() { return quantityAllocated.get(); }
    public void setQuantityAllocated(int value) { quantityAllocated.set(value); }
    public IntegerProperty quantityAllocatedProperty() { return quantityAllocated; }

    // Status
    public String getStatus() { return status.get(); }
    public void setStatus(String value) { status.set(value); }
    public StringProperty statusProperty() { return status; }

    // Notes
    public String getNotes() { return notes.get(); }
    public void setNotes(String value) { notes.set(value); }
    public StringProperty notesProperty() { return notes; }
}

package com.magictech.modules.sales.model;

import javafx.beans.property.*;

public class CustomerNoteViewModel {

    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty noteContent = new SimpleStringProperty();
    private final StringProperty createdBy = new SimpleStringProperty();
    private final StringProperty createdAt = new SimpleStringProperty();

    public CustomerNoteViewModel() {}

    // ID
    public long getId() { return id.get(); }
    public void setId(long value) { id.set(value); }
    public LongProperty idProperty() { return id; }

    // Note Content
    public String getNoteContent() { return noteContent.get(); }
    public void setNoteContent(String value) { noteContent.set(value); }
    public StringProperty noteContentProperty() { return noteContent; }

    // Created By
    public String getCreatedBy() { return createdBy.get(); }
    public void setCreatedBy(String value) { createdBy.set(value); }
    public StringProperty createdByProperty() { return createdBy; }

    // Created At
    public String getCreatedAt() { return createdAt.get(); }
    public void setCreatedAt(String value) { createdAt.set(value); }
    public StringProperty createdAtProperty() { return createdAt; }
}

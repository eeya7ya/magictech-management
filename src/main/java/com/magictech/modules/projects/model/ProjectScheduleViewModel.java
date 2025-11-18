package com.magictech.modules.projects.model;

import javafx.beans.property.*;

public class ProjectScheduleViewModel {

    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty taskName = new SimpleStringProperty();
    private final StringProperty startDate = new SimpleStringProperty();
    private final StringProperty endDate = new SimpleStringProperty();
    private final StringProperty status = new SimpleStringProperty();
    private final IntegerProperty progress = new SimpleIntegerProperty();
    private final StringProperty assignedTo = new SimpleStringProperty();

    public ProjectScheduleViewModel() {}

    // ID
    public long getId() { return id.get(); }
    public void setId(long value) { id.set(value); }
    public LongProperty idProperty() { return id; }

    // Task Name
    public String getTaskName() { return taskName.get(); }
    public void setTaskName(String value) { taskName.set(value); }
    public StringProperty taskNameProperty() { return taskName; }

    // Start Date
    public String getStartDate() { return startDate.get(); }
    public void setStartDate(String value) { startDate.set(value); }
    public StringProperty startDateProperty() { return startDate; }

    // End Date
    public String getEndDate() { return endDate.get(); }
    public void setEndDate(String value) { endDate.set(value); }
    public StringProperty endDateProperty() { return endDate; }

    // Status
    public String getStatus() { return status.get(); }
    public void setStatus(String value) { status.set(value); }
    public StringProperty statusProperty() { return status; }

    // Progress
    public int getProgress() { return progress.get(); }
    public void setProgress(int value) { progress.set(value); }
    public IntegerProperty progressProperty() { return progress; }

    // Assigned To
    public String getAssignedTo() { return assignedTo.get(); }
    public void setAssignedTo(String value) { assignedTo.set(value); }
    public StringProperty assignedToProperty() { return assignedTo; }
}
package com.magictech.modules.sales.model;

import com.magictech.modules.sales.entity.CustomerSchedule;
import javafx.beans.property.*;

public class CustomerScheduleViewModel {

    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty taskName = new SimpleStringProperty();
    private final StringProperty startDate = new SimpleStringProperty();
    private final StringProperty endDate = new SimpleStringProperty();
    private final StringProperty status = new SimpleStringProperty();
    private final IntegerProperty progress = new SimpleIntegerProperty();
    private final StringProperty assignedTo = new SimpleStringProperty();

    public CustomerScheduleViewModel() {}

    public CustomerScheduleViewModel(CustomerSchedule schedule) {
        this.id.set(schedule.getId() != null ? schedule.getId() : 0L);
        this.taskName.set(schedule.getTaskName() != null ? schedule.getTaskName() : "");
        this.startDate.set(schedule.getStartDate() != null ? schedule.getStartDate().toString() : "");
        this.endDate.set(schedule.getEndDate() != null ? schedule.getEndDate().toString() : "");
        this.status.set(schedule.getStatus() != null ? schedule.getStatus() : "");
        this.progress.set(0); // Default value as entity doesn't have this field
        this.assignedTo.set(""); // Default value as entity doesn't have this field
    }

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

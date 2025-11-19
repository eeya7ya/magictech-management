package com.magictech.modules.sales.model;

import javafx.beans.property.*;
import java.time.LocalDateTime;

public class CustomerTaskViewModel {

    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty taskTitle = new SimpleStringProperty();
    private final StringProperty taskDetails = new SimpleStringProperty();
    private final StringProperty priority = new SimpleStringProperty();
    private final BooleanProperty isCompleted = new SimpleBooleanProperty();
    private final StringProperty scheduleTaskName = new SimpleStringProperty();
    private LocalDateTime dueDate;

    public CustomerTaskViewModel() {}

    // ID
    public long getId() { return id.get(); }
    public void setId(long value) { id.set(value); }
    public LongProperty idProperty() { return id; }

    // Task Title
    public String getTaskTitle() { return taskTitle.get(); }
    public void setTaskTitle(String value) { taskTitle.set(value); }
    public StringProperty taskTitleProperty() { return taskTitle; }

    // Task Details
    public String getTaskDetails() { return taskDetails.get(); }
    public void setTaskDetails(String value) { taskDetails.set(value); }
    public StringProperty taskDetailsProperty() { return taskDetails; }

    // Priority
    public String getPriority() { return priority.get(); }
    public void setPriority(String value) { priority.set(value); }
    public StringProperty priorityProperty() { return priority; }

    // Is Completed
    public boolean getIsCompleted() { return isCompleted.get(); }
    public void setIsCompleted(boolean value) { isCompleted.set(value); }
    public BooleanProperty isCompletedProperty() { return isCompleted; }

    // Schedule Task Name
    public String getScheduleTaskName() { return scheduleTaskName.get(); }
    public void setScheduleTaskName(String value) { scheduleTaskName.set(value); }
    public StringProperty scheduleTaskNameProperty() { return scheduleTaskName; }

    // Due Date
    public LocalDateTime getDueDate() { return dueDate; }
    public void setDueDate(LocalDateTime value) { dueDate = value; }
}

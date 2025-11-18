package com.magictech.modules.projects.model;

import javafx.beans.property.*;

/**
 * Project View Model - JavaFX Property Bindings
 * Used for TableView display and data binding
 */
public class ProjectViewModel {

    private final LongProperty id = new SimpleLongProperty();
    private final StringProperty projectName = new SimpleStringProperty();
    private final StringProperty projectLocation = new SimpleStringProperty();
    private final StringProperty dateOfIssue = new SimpleStringProperty();
    private final StringProperty dateOfCompletion = new SimpleStringProperty();
    private final StringProperty status = new SimpleStringProperty();
    private final StringProperty dateAdded = new SimpleStringProperty();

    public ProjectViewModel() {}

    public ProjectViewModel(long id, String projectName, String projectLocation,
                            String dateOfIssue, String dateOfCompletion,
                            String status, String dateAdded) {
        setId(id);
        setProjectName(projectName);
        setProjectLocation(projectLocation);
        setDateOfIssue(dateOfIssue);
        setDateOfCompletion(dateOfCompletion);
        setStatus(status);
        setDateAdded(dateAdded);
    }

    // ID Property
    public long getId() { return id.get(); }
    public void setId(long value) { id.set(value); }
    public LongProperty idProperty() { return id; }

    // Project Name Property
    public String getProjectName() { return projectName.get(); }
    public void setProjectName(String value) { projectName.set(value); }
    public StringProperty projectNameProperty() { return projectName; }

    // Project Location Property
    public String getProjectLocation() { return projectLocation.get(); }
    public void setProjectLocation(String value) { projectLocation.set(value); }
    public StringProperty projectLocationProperty() { return projectLocation; }

    // Date of Issue Property
    public String getDateOfIssue() { return dateOfIssue.get(); }
    public void setDateOfIssue(String value) { dateOfIssue.set(value); }
    public StringProperty dateOfIssueProperty() { return dateOfIssue; }

    // Date of Completion Property
    public String getDateOfCompletion() { return dateOfCompletion.get(); }
    public void setDateOfCompletion(String value) { dateOfCompletion.set(value); }
    public StringProperty dateOfCompletionProperty() { return dateOfCompletion; }

    // Status Property
    public String getStatus() { return status.get(); }
    public void setStatus(String value) { status.set(value); }
    public StringProperty statusProperty() { return status; }

    // Date Added Property
    public String getDateAdded() { return dateAdded.get(); }
    public void setDateAdded(String value) { dateAdded.set(value); }
    public StringProperty dateAddedProperty() { return dateAdded; }

    @Override
    public String toString() {
        return "ProjectViewModel{" +
                "id=" + getId() +
                ", projectName='" + getProjectName() + '\'' +
                ", location='" + getProjectLocation() + '\'' +
                ", status='" + getStatus() + '\'' +
                ", dateOfIssue='" + getDateOfIssue() + '\'' +
                ", dateOfCompletion='" + getDateOfCompletion() + '\'' +
                '}';
    }
}
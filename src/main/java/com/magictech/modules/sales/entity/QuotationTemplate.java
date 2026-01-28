package com.magictech.modules.sales.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * QuotationTemplate - Stores background templates for quotation generation.
 *
 * The template system works by:
 * 1. Storing a background PDF/image as the template
 * 2. Defining fixed text positions (To, From, Date fields)
 * 3. Generating final PDF by overlaying text on the background
 *
 * This is much simpler than the annotation-based editing approach.
 */
@Entity
@Table(name = "quotation_templates")
public class QuotationTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_name", nullable = false, length = 200)
    private String templateName;

    @Column(name = "description", length = 500)
    private String description;

    // Background template data (PDF or image)
    @Lob
    @Column(name = "background_data", columnDefinition = "BYTEA")
    private byte[] backgroundData;

    @Column(name = "background_type", length = 20)
    private String backgroundType; // "PDF" or "IMAGE"

    @Column(name = "filename", length = 255)
    private String filename;

    // Text field positions (in PDF points, origin at bottom-left)
    // "To:" field position
    @Column(name = "to_field_x")
    private Float toFieldX = 370f;

    @Column(name = "to_field_y")
    private Float toFieldY = 555f;

    // "From:" field position
    @Column(name = "from_field_x")
    private Float fromFieldX = 370f;

    @Column(name = "from_field_y")
    private Float fromFieldY = 525f;

    // "Date:" field position
    @Column(name = "date_field_x")
    private Float dateFieldX = 370f;

    @Column(name = "date_field_y")
    private Float dateFieldY = 495f;

    // Font settings
    @Column(name = "font_family", length = 50)
    private String fontFamily = "Helvetica";

    @Column(name = "font_size")
    private Integer fontSize = 14;

    @Column(name = "font_color", length = 20)
    private String fontColor = "#000000";

    @Column(name = "bold_labels")
    private Boolean boldLabels = true;

    // Whether this is the default template
    @Column(name = "is_default")
    private Boolean isDefault = false;

    // Standard metadata
    @Column(name = "date_added", nullable = false)
    private LocalDateTime dateAdded;

    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(nullable = false)
    private Boolean active = true;

    // Constructors
    public QuotationTemplate() {
    }

    public QuotationTemplate(String templateName) {
        this.templateName = templateName;
    }

    @PrePersist
    protected void onCreate() {
        this.dateAdded = LocalDateTime.now();
        if (this.active == null) this.active = true;
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastUpdated = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public byte[] getBackgroundData() {
        return backgroundData;
    }

    public void setBackgroundData(byte[] backgroundData) {
        this.backgroundData = backgroundData;
    }

    public String getBackgroundType() {
        return backgroundType;
    }

    public void setBackgroundType(String backgroundType) {
        this.backgroundType = backgroundType;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public Float getToFieldX() {
        return toFieldX;
    }

    public void setToFieldX(Float toFieldX) {
        this.toFieldX = toFieldX;
    }

    public Float getToFieldY() {
        return toFieldY;
    }

    public void setToFieldY(Float toFieldY) {
        this.toFieldY = toFieldY;
    }

    public Float getFromFieldX() {
        return fromFieldX;
    }

    public void setFromFieldX(Float fromFieldX) {
        this.fromFieldX = fromFieldX;
    }

    public Float getFromFieldY() {
        return fromFieldY;
    }

    public void setFromFieldY(Float fromFieldY) {
        this.fromFieldY = fromFieldY;
    }

    public Float getDateFieldX() {
        return dateFieldX;
    }

    public void setDateFieldX(Float dateFieldX) {
        this.dateFieldX = dateFieldX;
    }

    public Float getDateFieldY() {
        return dateFieldY;
    }

    public void setDateFieldY(Float dateFieldY) {
        this.dateFieldY = dateFieldY;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }

    public Integer getFontSize() {
        return fontSize;
    }

    public void setFontSize(Integer fontSize) {
        this.fontSize = fontSize;
    }

    public String getFontColor() {
        return fontColor;
    }

    public void setFontColor(String fontColor) {
        this.fontColor = fontColor;
    }

    public Boolean getBoldLabels() {
        return boldLabels;
    }

    public void setBoldLabels(Boolean boldLabels) {
        this.boldLabels = boldLabels;
    }

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
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
}

package com.seismiq.common.model;

import java.time.LocalDateTime;

/**
 * Represents a landmark or point of interest in the disaster response system.
 * Landmarks are created based on reports and provide essential location information
 * for emergency services and affected people.
 *
 * @author Sıla Bozkurt
 */
public class Landmark {
    private String landmarkId;
    private String name;
    private String location;
    private Category category;
    private String description;
    private Report associatedReport;
    private String reportId;
    private String geohash;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;
    private boolean isActive;
    private String status;
    private String createdBy;
    private double latitude;
    private double longitude;
    
    // Default constructor for JSON deserialization
    public Landmark() {
        this.createdAt = LocalDateTime.now();
        this.lastUpdated = this.createdAt;
        this.isActive = true;
        this.status = "ACTIVE";
    }

    public Landmark(String landmarkId, String name, String location, 
                   Category category, Report associatedReport, String createdBy) {
        this.landmarkId = landmarkId;
        this.name = name;
        this.location = location;
        this.category = category;
        this.associatedReport = associatedReport;
        if (associatedReport != null) {
            this.reportId = associatedReport.getReportId();
        }
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        this.lastUpdated = this.createdAt;
        this.isActive = true;
        this.status = "ACTIVE";
    }

    public Landmark(String landmarkId, String name, String location, 
                   Category category, Report associatedReport, String createdBy,
                   double latitude, double longitude) {
        this(landmarkId, name, location, category, associatedReport, createdBy);
        this.latitude = latitude;
        this.longitude = longitude;
    }
    
    // Constructor matching the frontend model for new landmark creation
    public Landmark(String name, String categoryType, String description, double latitude, double longitude) {
        this.name = name;
        this.category = Category.valueOf(categoryType);
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isActive = true;
        this.status = "ACTIVE";
        this.createdAt = LocalDateTime.now();
        this.lastUpdated = this.createdAt;
    }

    // Getters and setters
    public String getLandmarkId() { return landmarkId; }
    public void setLandmarkId(String landmarkId) { this.landmarkId = landmarkId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public Category getCategory() { return category; }
    public void setCategory(Category category) { this.category = category; }
    
    // Convenience methods for frontend compatibility
    public String getCategoryType() { 
        return category != null ? category.getCategoryType() : null; 
    }
    public void setCategoryType(String categoryType) { 
        this.category = Category.valueOf(categoryType); 
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Report getAssociatedReport() { return associatedReport; }
    public void setAssociatedReport(Report associatedReport) { 
        this.associatedReport = associatedReport;
        if (associatedReport != null) {
            this.reportId = associatedReport.getReportId();
        } else {
            this.reportId = null;
        }
    }

    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }

    public String getGeohash() { return geohash; }
    public void setGeohash(String geohash) { this.geohash = geohash; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { 
        this.isActive = active;
        this.status = active ? "ACTIVE" : "INACTIVE";
        this.lastUpdated = LocalDateTime.now();
    }
    
    public String getStatus() { return status; }
    public void setStatus(String status) { 
        this.status = status;
        this.isActive = "ACTIVE".equals(status);
        this.lastUpdated = LocalDateTime.now();
    }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}
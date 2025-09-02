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
    private LandmarkCategory category;
    private String description;
    private Report associatedReport;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;
    private boolean isActive;
    private String createdBy;
    private double latitude;
    private double longitude;

    public Landmark(String landmarkId, String name, String location, 
                   LandmarkCategory category, Report associatedReport, String createdBy) {
        this.landmarkId = landmarkId;
        this.name = name;
        this.location = location;
        this.category = category;
        this.associatedReport = associatedReport;
        this.createdBy = createdBy;
        this.createdAt = LocalDateTime.now();
        this.lastUpdated = this.createdAt;
        this.isActive = true;
    }

    public Landmark(String landmarkId, String name, String location, 
                   LandmarkCategory category, Report associatedReport, String createdBy,
                   double latitude, double longitude) {
        this(landmarkId, name, location, category, associatedReport, createdBy);
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters and setters
    public String getLandmarkId() { return landmarkId; }
    public void setLandmarkId(String landmarkId) { this.landmarkId = landmarkId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public LandmarkCategory getCategory() { return category; }
    public void setCategory(LandmarkCategory category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Report getAssociatedReport() { return associatedReport; }
    public void setAssociatedReport(Report associatedReport) { this.associatedReport = associatedReport; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { 
        this.isActive = active;
        this.lastUpdated = LocalDateTime.now();
    }

    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}
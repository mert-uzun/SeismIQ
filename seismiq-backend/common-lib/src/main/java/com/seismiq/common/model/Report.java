package com.seismiq.common.model;

import java.time.LocalDateTime;

/**
 * Represents an emergency report in the SeismIQ system.
 * Contains information about various types of emergency needs
 * and requests during earthquake response operations.
 *
 * @author Sıla Bozkurt
 */
public class Report {
    public enum ReportCategory {
        MEDICAL_HELP,
        FOOD_WATER,
        SHELTER,
        RESCUE,
        SUPPLIES,
        VOLUNTEER,
        OTHER
    }

    public enum ReportStatus {
        PENDING,
        IN_PROGRESS,
        COMPLETED,
        CANCELLED
    }

    private String reportId;
    private User user;
    private ReportCategory category;
    private String description;
    private String location; // Combined location string
    private boolean currentLocation; // Added boolean for current location
    private double latitude;
    private double longitude;
    private String locationDescription;
    private ReportStatus status;
    private LocalDateTime timestamp;
    private LocalDateTime lastUpdated;
    private String additionalNotes;

    // Constructors
    public Report() {}

    /**
     * Copy constructor to create a deep copy of a Report object.
     *
     * @param other The Report object to copy
     */
    public Report(Report other) {
        this.reportId = other.reportId;
        this.user = other.user; // Note: User object should be immutable or also implement deep copy if needed
        this.category = other.category;
        this.description = other.description;
        this.location = other.location;
        this.currentLocation = other.currentLocation;
        this.latitude = other.latitude;
        this.longitude = other.longitude;
        this.locationDescription = other.locationDescription;
        this.status = other.status;
        this.timestamp = other.timestamp;
        this.lastUpdated = other.lastUpdated;
        this.additionalNotes = other.additionalNotes;
    }

    public Report(String reportId, User user, ReportCategory category, String description, 
                 String location, boolean currentLocation, ReportStatus status, LocalDateTime timestamp) {
        this.reportId = reportId;
        this.user = user;
        this.category = category;
        this.description = description;
        this.location = location;
        this.currentLocation = currentLocation;
        this.status = status;
        this.timestamp = timestamp;
        this.lastUpdated = timestamp;
    }

    /**
     * Constructor with all fields including location coordinates.
     *
     * @param reportId           The unique identifier of the report
     * @param user              The user who created the report
     * @param category          The category of the report (e.g., MEDICAL_HELP, SHELTER)
     * @param description       The detailed description of the report
     * @param location          The general location string
     * @param currentLocation   Whether this is the user's current location
     * @param latitude          The latitude coordinate
     * @param longitude         The longitude coordinate
     * @param locationDescription Additional details about the location
     * @param status            The current status of the report
     * @param timestamp         The creation time of the report
     */
    public Report(String reportId, User user, ReportCategory category, String description, 
                 String location, boolean currentLocation, double latitude, double longitude,
                 String locationDescription, ReportStatus status, LocalDateTime timestamp) {
        this.reportId = reportId;
        this.user = user;
        this.category = category;
        this.description = description;
        this.location = location;
        this.currentLocation = currentLocation;
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationDescription = locationDescription;
        this.status = status;
        this.timestamp = timestamp;
        this.lastUpdated = timestamp;
    }

    // Getters and Setters
    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public ReportCategory getCategory() { return category; }
    public void setCategory(ReportCategory category) { this.category = category; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public boolean isCurrentLocation() { return currentLocation; }
    public void setCurrentLocation(boolean currentLocation) { this.currentLocation = currentLocation; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getLocationDescription() { return locationDescription; }
    public void setLocationDescription(String locationDescription) { this.locationDescription = locationDescription; }

    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { 
        this.status = status;
        this.lastUpdated = LocalDateTime.now();
    }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public LocalDateTime getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(LocalDateTime lastUpdated) { this.lastUpdated = lastUpdated; }

    public String getAdditionalNotes() { return additionalNotes; }
    public void setAdditionalNotes(String additionalNotes) { this.additionalNotes = additionalNotes; }
}
package com.seismiq.common.model;

import java.time.LocalDateTime;

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
    private ReportStatus status;
    private LocalDateTime timestamp;
    private LocalDateTime lastUpdated;
    private String additionalNotes;

    // Constructors
    public Report() {}

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
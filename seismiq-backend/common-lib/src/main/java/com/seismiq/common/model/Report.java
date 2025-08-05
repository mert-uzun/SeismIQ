package com.seismiq.common.model;

import java.time.LocalDateTime;

public class Report {
    private String reportId;
    private String userId;
    private String description;
    private String location;
    private String status;
    private LocalDateTime timestamp;

    // Constructors
    public Report() {}

    public Report(String reportId, String userId, String description, String location, String status, LocalDateTime timestamp) {
        this.reportId = reportId;
        this.userId = userId;
        this.description = description;
        this.location = location;
        this.status = status;
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public String getReportId() { return reportId; }
    public void setReportId(String reportId) { this.reportId = reportId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
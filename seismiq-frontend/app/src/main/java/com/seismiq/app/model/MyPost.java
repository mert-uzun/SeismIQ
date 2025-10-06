package com.seismiq.app.model;

import java.util.Date;

public class MyPost {
    public static final int TYPE_REPORT = 0;
    public static final int TYPE_LANDMARK = 1;

    private final int type;
    private final String id;
    private final String description;
    private final String categoryType;
    private final String location;
    private final double latitude;
    private final double longitude;
    private final Date createdAt;

    // For Reports
    public MyPost(Report report) {
        this.type = TYPE_REPORT;
        this.id = report.getReportId();
        this.description = report.getDescription();
        this.categoryType = report.getCategoryType();
        this.location = report.getLocation();
        this.latitude = report.getLatitude();
        this.longitude = report.getLongitude();
        this.createdAt = report.getCreatedAt();
    }

    // For Landmarks
    public MyPost(Landmark landmark) {
        this.type = TYPE_LANDMARK;
        this.id = landmark.getLandmarkId();
        this.description = landmark.getDescription();
        this.categoryType = landmark.getCategory() != null ? landmark.getCategory().getCategoryType() : null;
        this.location = landmark.getLocation();
        this.latitude = landmark.getLatitude();
        this.longitude = landmark.getLongitude();
        this.createdAt = landmark.getCreatedAt();
    }

    public int getType() {
        return type;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public String getCategoryType() {
        return categoryType;
    }

    public String getLocation() {
        return location;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public String getTypeString() {
        return type == TYPE_REPORT ? "Report" : "Landmark";
    }
}


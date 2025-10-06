package com.seismiq.app.model;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class Landmark {
    private String landmarkId;
    private String name;
    private Category category;
    private String description;
    private String location;
    private double latitude;
    private double longitude;
    private String reportId;
    private String geohash;
    private String status;
    private String createdBy;
    
    @SerializedName("createdAt")
    private Date createdAt;
    
    @SerializedName("lastUpdated")
    private Date lastUpdated;

    // Default constructor for Gson
    public Landmark() {
    }

    public Landmark(String landmarkId, String name, Category category, String description, double latitude, 
                    double longitude, String reportId, String geohash, String status) {
        this.landmarkId = landmarkId;
        this.name = name;
        this.category = category;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.reportId = reportId;
        this.geohash = geohash;
        this.status = status;
    }

    // Constructor for creating new landmark (without ID)
    public Landmark(String name, String categoryType, String description, double latitude, double longitude) {
        this.name = name;
        this.category = Category.valueOf(categoryType);
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.status = "ACTIVE";
    }

    // Getters and Setters
    public String getLandmarkId() {
        return landmarkId;
    }

    public void setLandmarkId(String landmarkId) {
        this.landmarkId = landmarkId;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }
    
    // Convenience methods for dealing with string category types
    public String getCategoryType() {
        return category != null ? category.getCategoryType() : null;
    }
    
    public void setCategoryType(String categoryType) {
        this.category = Category.valueOf(categoryType);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getGeohash() {
        return geohash;
    }

    public void setGeohash(String geohash) {
        this.geohash = geohash;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(Date lastUpdated) {
        this.lastUpdated = lastUpdated;
    }
}

package com.seismiq.app.model;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class Report {
    private String reportId;
    private String userId;
    
    // For receiving: backend sends nested category object
    @SerializedName("category")
    private Category categoryObject;
    
    // For sending: backend expects flat categoryType string
    private String categoryType;
    
    private User user;  // Backend expects user object
    private String status;
    private String description;
    private String location;
    private double latitude;
    private double longitude;
    
    @SerializedName("timestamp")
    private Date createdAt;  // Backend sends "timestamp", we map it to createdAt
    
    @SerializedName("lastUpdated")
    private Date updatedAt;

    // Default constructor for Gson
    public Report() {
    }

    public Report(String reportId, String userId, Category category, String status, 
                String description, double latitude, double longitude, 
                Date createdAt, Date updatedAt) {
        this.reportId = reportId;
        this.userId = userId;
        this.categoryObject = category;
        if (category != null) {
            this.categoryType = category.getCategoryType();
        }
        this.status = status;
        this.description = description;
        this.latitude = latitude;
        this.longitude = longitude;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    // Getters and Setters
    public String getReportId() {
        return reportId;
    }

    public void setReportId(String reportId) {
        this.reportId = reportId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public Category getCategory() {
        return categoryObject;
    }

    public void setCategory(Category category) {
        this.categoryObject = category;
        if (category != null) {
            this.categoryType = category.getCategoryType();
        }
    }
    
    // Get category type - check both fields for compatibility
    public String getCategoryType() {
        if (categoryType != null) {
            return categoryType;
        }
        return categoryObject != null ? categoryObject.getCategoryType() : null;
    }
    
    // Set category from string - sets the flat field for API submission
    public void setCategoryType(String categoryType) {
        this.categoryType = categoryType;
        // Also create category object for internal use
        if (categoryType != null) {
            this.categoryObject = Category.valueOf(categoryType);
        }
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
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

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}

package com.seismiq.app.model;

import java.util.Date;

public class Earthquake {
    private String earthquakeId;
    private double magnitude;
    private double latitude;
    private double longitude;
    private Date timestamp;
    private String location;
    private int depth;
    private String description;

    // Default constructor for Gson
    public Earthquake() {
    }

    public Earthquake(String earthquakeId, double magnitude, double latitude, double longitude, 
                     Date timestamp, String location, int depth, String description) {
        this.earthquakeId = earthquakeId;
        this.magnitude = magnitude;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.location = location;
        this.depth = depth;
        this.description = description;
    }

    // Getters and Setters
    public String getEarthquakeId() {
        return earthquakeId;
    }

    public void setEarthquakeId(String earthquakeId) {
        this.earthquakeId = earthquakeId;
    }

    public double getMagnitude() {
        return magnitude;
    }

    public void setMagnitude(double magnitude) {
        this.magnitude = magnitude;
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

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public int getDepth() {
        return depth;
    }

    public void setDepth(int depth) {
        this.depth = depth;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}

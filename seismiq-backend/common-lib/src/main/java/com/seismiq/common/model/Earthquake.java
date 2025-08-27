package com.seismiq.common.model;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Represents an earthquake event in the SeismIQ system.
 * Contains information about the earthquake's location, magnitude,
 * depth, and other relevant seismic data.
 *
 * @author Sıla Bozkurt
 */
public class Earthquake {
    private String earthquakeId;
    private double magnitude;
    private double latitude;
    private double longitude;
    private double depth;
    private String location;
    private LocalDateTime timestamp;
    private String source;  // Source of the earthquake data (e.g., USGS, AFAD)
    private List<Report> associatedReports;
    private boolean isActive;  // Will be set to false after 6 months

    public Earthquake() {}

    public Earthquake(String earthquakeId, double magnitude, double latitude, double longitude,
                     double depth, String location, LocalDateTime timestamp, String source) {
        this.earthquakeId = earthquakeId;
        this.magnitude = magnitude;
        this.latitude = latitude;
        this.longitude = longitude;
        this.depth = depth;
        this.location = location;
        this.timestamp = timestamp;
        this.source = source;
        this.isActive = true;
    }

    // Getters and Setters
    public String getEarthquakeId() { return earthquakeId; }
    public void setEarthquakeId(String earthquakeId) { this.earthquakeId = earthquakeId; }

    public double getMagnitude() { return magnitude; }
    public void setMagnitude(double magnitude) { this.magnitude = magnitude; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public double getDepth() { return depth; }
    public void setDepth(double depth) { this.depth = depth; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public List<Report> getAssociatedReports() { return associatedReports; }
    public void setAssociatedReports(List<Report> associatedReports) { this.associatedReports = associatedReports; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }
}

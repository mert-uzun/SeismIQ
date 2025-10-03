package com.seismiq.common.model;

/**
 * Represents a priority zone for earthquake monitoring and response.
 * Priority zones are geographic areas with specific monitoring requirements
 * based on population density, critical infrastructure, or historical seismic activity.
 *
 * @author Sıla Bozkurt
 */
public class PriorityZone {
    private String zoneId;
    private double latitude;
    private double longitude;
    private int priority;  // 1 to 5, where 5 is highest priority
    private String description;
    private double radius;  // Radius in kilometers

    public PriorityZone() {}

    public PriorityZone(String zoneId, double latitude, double longitude, int priority) {
        this.zoneId = zoneId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.priority = priority;
    }

    // Getters and Setters
    public String getZoneId() { return zoneId; }
    public void setZoneId(String zoneId) { this.zoneId = zoneId; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public int getPriority() { return priority; }
    public void setPriority(int priority) { 
        if (priority < 1 || priority > 5) {
            throw new IllegalArgumentException("Priority must be between 1 and 5");
        }
        this.priority = priority; 
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public double getRadius() { return radius; }
    public void setRadius(double radius) { this.radius = radius; }
}

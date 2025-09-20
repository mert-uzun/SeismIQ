package com.seismiq.app.model;

public class User {
    private String userId;
    private String name;
    private String address;
    private boolean isVolunteer;
    private boolean isSocialWorker;

    // Default constructor for Gson
    public User() {
    }

    public User(String userId, String name, String address, boolean isVolunteer, boolean isSocialWorker) {
        this.userId = userId;
        this.name = name;
        this.address = address;
        this.isVolunteer = isVolunteer;
        this.isSocialWorker = isSocialWorker;
    }

    // Getters and Setters
    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public boolean isVolunteer() {
        return isVolunteer;
    }

    public void setVolunteer(boolean volunteer) {
        isVolunteer = volunteer;
    }

    public boolean isSocialWorker() {
        return isSocialWorker;
    }

    public void setSocialWorker(boolean socialWorker) {
        isSocialWorker = socialWorker;
    }
}

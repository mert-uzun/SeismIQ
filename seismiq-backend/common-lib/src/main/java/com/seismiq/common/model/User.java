package com.seismiq.common.model;

/**
 * Represents a user in the SeismIQ system.
 * Users can be regular citizens, volunteers, or social workers
 * who interact with the system for earthquake response and assistance.
 *
 * @author Sıla Bozkurt
 */
public class User {
    private String userId;
    private String name;
    private String address;
    private boolean isVolunteer;
    private boolean isSocialWorker;
    private String email;
    private String passwordHash;

    public User() {}

    public User(String userId, String name, String address, boolean isVolunteer, boolean isSocialWorker,String email, String passwordHash) {
        this.userId = userId;
        this.name = name;
        this.address = address;
        this.isVolunteer = isVolunteer;
        this.isSocialWorker = isSocialWorker;
        this.email = email;
        this.passwordHash = passwordHash;
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public boolean isVolunteer() { return isVolunteer; }
    public void setVolunteer(boolean volunteer) { isVolunteer = volunteer; }

    public boolean isSocialWorker() { return isSocialWorker; }
    public void setSocialWorker(boolean socialWorker) { isSocialWorker = socialWorker; }

    public String getEmail(){
        return email;
    }
    public void setEmail(String email){
        this.email = email;
    }

    public String getPasswordHash(){
        return passwordHash;
    }
    public void setPasswordHash(String passwordHash){
        this.passwordHash = passwordHash;
    }
}

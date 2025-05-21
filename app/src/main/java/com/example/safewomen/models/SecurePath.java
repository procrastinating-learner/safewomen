package com.example.safewomen.models;

public class SecurePath {
    private int id;
    private String userId;
    private double latitude;
    private double longitude;
    private String description;
    private String dateMarked;
    private boolean confirmed;

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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDateMarked() {
        return dateMarked;
    }
    public void setDateMarked(String dateMarked) {
        this.dateMarked = dateMarked;
    }
    public boolean isConfirmed() {
        return confirmed;
    }
    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getUserId() {
        return userId;
    }
    public void setUserId(String userId) {
        this.userId = userId;
    }
}
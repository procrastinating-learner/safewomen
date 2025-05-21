package com.example.safewomen.models;

import java.util.Date;

public class Alert {
    private String id;
    private Date timestamp;
    private double latitude;
    private double longitude;
    private String address;
    private String status; // "active", "resolved", "cancelled"

    // Constructors
    public Alert() {}

    public Alert(String id, Date timestamp, double latitude, double longitude, String address, String status) {
        this.id = id;
        this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.status = status;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Date getTimestamp() { return timestamp; }
    public void setTimestamp(Date timestamp) { this.timestamp = timestamp; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}

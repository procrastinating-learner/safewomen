package com.example.safewomen.models.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "alerts")
public class AlertEntity {
    @PrimaryKey
    @NonNull
    private String id;

    private long timestamp;
    private double latitude;
    private double longitude;
    private String address;
    private String status;
    // Constructors
    public AlertEntity() {}
    public AlertEntity(String id, long timestamp, double latitude, double longitude, String address, String status) {
        this.id = id;
        this.timestamp = timestamp;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.status = status;
        //
    }
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.
            longitude = longitude; }
   public  String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }


}
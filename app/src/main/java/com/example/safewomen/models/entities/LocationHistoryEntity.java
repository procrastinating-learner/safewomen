package com.example.safewomen.models.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "location_history")
public class LocationHistoryEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private double latitude;
    private double longitude;
    private String address;
    private long timestamp;
    private float accuracy;

    public LocationHistoryEntity() {}

    public LocationHistoryEntity(@NonNull String id, double latitude, double longitude,
                                 String address, long timestamp, float accuracy) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.timestamp = timestamp;
        this.accuracy = accuracy;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public float getAccuracy() { return accuracy; }
    public void setAccuracy(float accuracy) { this.accuracy = accuracy; }
}
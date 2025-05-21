package com.example.safewomen.models.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "contacts")
public class EmergencyContactEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String name;
    private String phone;
    private String relationship;
    private boolean isPrimary;
    private String syncStatus; // "synced", "pending", "failed"

    // Constructors
    public EmergencyContactEntity() {}

    public EmergencyContactEntity(String id, String name, String phone, String relationship, boolean isPrimary, String syncStatus) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.relationship = relationship;
        this.isPrimary = isPrimary;
        this.syncStatus = syncStatus;
    }

    // Getters and Setters
    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }

    public boolean isPrimary() { return isPrimary; }
    public void setPrimary(boolean primary) { isPrimary = primary; }

    public String getSyncStatus() { return syncStatus; }
    public void setSyncStatus(String syncStatus) { this.syncStatus = syncStatus; }
}
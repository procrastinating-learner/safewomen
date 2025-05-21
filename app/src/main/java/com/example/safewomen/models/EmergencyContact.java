package com.example.safewomen.models;

public class EmergencyContact {
    private String id;
    private String name;
    private String phone;
    private String relationship;
    private boolean isPrimary;

    // Constructors
    public EmergencyContact() {}

    public EmergencyContact(String id, String name, String phone, String relationship, boolean isPrimary) {
        this.id = id;
        this.name = name;
        this.phone = phone;
        this.relationship = relationship;
        this.isPrimary = isPrimary;
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getRelationship() { return relationship; }
    public void setRelationship(String relationship) { this.relationship = relationship; }

    public boolean isPrimary() { return isPrimary; }
    public void setPrimary(boolean primary) { isPrimary = primary; }
}
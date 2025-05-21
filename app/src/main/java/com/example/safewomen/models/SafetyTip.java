package com.example.safewomen.models;

public class SafetyTip {
    private String title;
    private String description;

    public SafetyTip(String title, String description) {
        this.title = title;
        this.description = description;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }
}

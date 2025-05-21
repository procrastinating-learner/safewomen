package com.example.safewomen;

import android.app.Application;

import com.example.safewomen.repositories.AlertRepository;
import com.example.safewomen.repositories.ContactRepository;
import com.example.safewomen.repositories.LocationHistoryRepository;
import com.example.safewomen.utils.PreferenceManager;

public class SafeWomenApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Initialize PreferenceManager
        PreferenceManager.init(this);
        
        // Initialize repositories
        LocationHistoryRepository.init(this);
        AlertRepository.init(this);
        ContactRepository.init(this);
    }
} 
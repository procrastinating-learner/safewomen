package com.example.safewomen.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.safewomen.models.entities.LocationHistoryEntity;
import com.example.safewomen.repositories.LocationHistoryRepository;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MapViewModel extends AndroidViewModel {
    private static final String TAG = "MapViewModel";

    private final LocationHistoryRepository locationRepository;
    private final MutableLiveData<List<LocationHistoryEntity>> locationHistory = new MutableLiveData<>();
    private final MutableLiveData<LocationHistoryEntity> selectedLocation = new MutableLiveData<>();
    private final MutableLiveData<List<SafetyZone>> safetyZones = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // Original unfiltered location history
    private List<LocationHistoryEntity> allLocations = new ArrayList<>();

    private final Application application;

    public MapViewModel(Application application) {
        super(application);
        this.application = application;
        locationRepository = LocationHistoryRepository.getInstance();

        // Load location history
        loadLocationHistory();
    }

    // Getters for LiveData
    public LiveData<List<LocationHistoryEntity>> getLocationHistory() {
        return locationHistory;
    }

    public LiveData<LocationHistoryEntity> getSelectedLocation() {
        return selectedLocation;
    }

    public LiveData<List<SafetyZone>> getSafetyZones() {
        return safetyZones;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    // Load location history
    public void loadLocationHistory() {
        isLoading.setValue(true);

        locationRepository.getLocationHistory().observeForever(locations -> {
            if (locations != null) {
                allLocations = locations;
                locationHistory.setValue(locations);
                isLoading.setValue(false);

                // Generate safety zones based on location history
                generateSafetyZones();
            }
        });
    }

    // Filter locations by today
    public void filterLocationsByToday() {
        if (allLocations == null || allLocations.isEmpty()) return;

        List<LocationHistoryEntity> filtered = new ArrayList<>();

        // Get today's start time
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long todayStart = calendar.getTimeInMillis();

        // Filter locations
        for (LocationHistoryEntity location : allLocations) {
            if (location.getTimestamp() >= todayStart) {
                filtered.add(location);
            }
        }

        locationHistory.setValue(filtered);
    }

    // Filter locations by this week
    public void filterLocationsByThisWeek() {
        if (allLocations == null || allLocations.isEmpty()) return;

        List<LocationHistoryEntity> filtered = new ArrayList<>();

        // Get start of week
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long weekStart = calendar.getTimeInMillis();

        // Filter locations
        for (LocationHistoryEntity location : allLocations) {
            if (location.getTimestamp() >= weekStart) {
                filtered.add(location);
            }
        }

        locationHistory.setValue(filtered);
    }

    // Filter locations by this month
    public void filterLocationsByThisMonth() {
        if (allLocations == null || allLocations.isEmpty()) return;

        List<LocationHistoryEntity> filtered = new ArrayList<>();

        // Get start of month
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long monthStart = calendar.getTimeInMillis();

        // Filter locations
        for (LocationHistoryEntity location : allLocations) {
            if (location.getTimestamp() >= monthStart) {
                filtered.add(location);
            }
        }

        locationHistory.setValue(filtered);
    }

    // Clear filters
    public void clearFilters() {
        locationHistory.setValue(allLocations);
    }

    // Select a location
    public void selectLocation(LocationHistoryEntity location) {
        selectedLocation.setValue(location);
    }

    // Mark location as safe
    public void markLocationAsSafe(LocationHistoryEntity location) {
        // In a real app, you would save this to a database
        // For now, we'll just add a safety zone
        if (location != null) {
            LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
            addSafetyZone(position, 300, 0.8f); // 300m radius, 0.8 safety rating
        }
    }

    // Mark location as unsafe
    public void markLocationAsUnsafe(LocationHistoryEntity location) {
        // In a real app, you would save this to a database
        // For now, we'll just add a safety zone
        if (location != null) {
            LatLng position = new LatLng(location.getLatitude(), location.getLongitude());
            addSafetyZone(position, 300, 0.2f); // 300m radius, 0.2 safety rating
        }
    }

    // Generate safety zones based on location history
    private void generateSafetyZones() {
        // This is a simplified implementation
        // In a real app, you would:
        // 1. Use a more sophisticated algorithm to identify safe/unsafe areas
        // 2. Consider time of day, crime statistics, etc.
        // 3. Load safety zones from a database or API

        List<SafetyZone> zones = new ArrayList<>();

        // For demo purposes, we'll create some random safety zones
        if (allLocations != null && !allLocations.isEmpty()) {
            // Create a safe zone around the most recent location
            LocationHistoryEntity recent = allLocations.get(allLocations.size() - 1);
            LatLng recentPos = new LatLng(recent.getLatitude(), recent.getLongitude());
            zones.add(new SafetyZone(recentPos, 500, 0.9f)); // 500m radius, 0.9 safety rating

            // Create a warning zone nearby
            LatLng warningPos = new LatLng(
                    recent.getLatitude() + 0.01,
                    recent.getLongitude() + 0.01);
            zones.add(new SafetyZone(warningPos, 400, 0.5f)); // 400m radius, 0.5 safety rating

            // Create a danger zone nearby
            LatLng dangerPos = new LatLng(
                    recent.getLatitude() - 0.01,
                    recent.getLongitude() - 0.01);
            zones.add(new SafetyZone(dangerPos, 300, 0.2f)); // 300m radius, 0.2 safety rating
        }

        safetyZones.setValue(zones);
    }

    // Add a safety zone
    private void addSafetyZone(LatLng center, double radiusMeters, float safetyRating) {
        List<SafetyZone> zones = safetyZones.getValue();
        if (zones == null) {
            zones = new ArrayList<>();
        }

        zones.add(new SafetyZone(center, radiusMeters, safetyRating));
        safetyZones.setValue(zones);
    }

    // Safety Zone class
    public static class SafetyZone {
        private final LatLng center;
        private final double radiusMeters;
        private final float safetyRating; // 0.0 to 1.0, where 1.0 is completely safe

        public SafetyZone(LatLng center, double radiusMeters, float safetyRating) {
            this.center = center;
            this.radiusMeters = radiusMeters;
            this.safetyRating = safetyRating;
        }

        public LatLng getCenter() {
            return center;
        }

        public double getRadiusMeters() {
            return radiusMeters;
        }

        public float getSafetyRating() {
            return safetyRating;
        }
    }
}

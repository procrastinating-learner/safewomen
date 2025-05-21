package com.example.safewomen.viewmodels;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.safewomen.models.User;
import com.example.safewomen.models.entities.LocationHistoryEntity;
import com.example.safewomen.repositories.AlertRepository;
import com.example.safewomen.repositories.LocationHistoryRepository;
import com.example.safewomen.repositories.UserRepository;
import com.example.safewomen.services.FallDetectionService;
import com.example.safewomen.services.LocationTrackingService;
import com.example.safewomen.services.ShakeDetectionService;
import com.example.safewomen.services.VoiceCommandService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DashboardViewModel extends ViewModel {
    private static final String TAG = "DashboardViewModel";
    private final UserRepository userRepository;
    private final LocationHistoryRepository locationRepository;
    private final AlertRepository alertRepository;

    // LiveData for dashboard information
    private final MutableLiveData<User> currentUser = new MutableLiveData<>();
    private final MutableLiveData<SafetyStatus> safetyStatus = new MutableLiveData<>(SafetyStatus.SAFE);
    private final MutableLiveData<List<Map<String, String>>> recentAlerts = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<LocationHistoryEntity> currentLocation = new MutableLiveData<>();
    private final MutableLiveData<Map<String, Boolean>> serviceStatus = new MutableLiveData<>(new HashMap<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // Service keys
    private static final String LOCATION_TRACKING = "location_tracking";
    private static final String VOICE_COMMAND = "voice_command";
    private static final String SHAKE_DETECTION = "shake_detection";
    private static final String FALL_DETECTION = "fall_detection";

    // Safety status enum
    public enum SafetyStatus {
        SAFE,       // All systems normal, no alerts
        WARNING,    // Some services not running or potential issues
        DANGER      // Active alert or emergency situation
    }

    public DashboardViewModel() {
        userRepository = UserRepository.getInstance();
        locationRepository = LocationHistoryRepository.getInstance();
        alertRepository = AlertRepository.getInstance();

        // Initialize service status map
        Map<String, Boolean> status = new HashMap<>();
        status.put(LOCATION_TRACKING, false);
        status.put(VOICE_COMMAND, false);
        status.put(SHAKE_DETECTION, false);
        status.put(FALL_DETECTION, false);
        serviceStatus.setValue(status);

        // Load initial data
        loadUserData();
        loadRecentAlerts();
        loadCurrentLocation();
        checkServiceStatus();
    }

    // Getters for LiveData
    public LiveData<User> getCurrentUser() {
        return currentUser;
    }

    public LiveData<SafetyStatus> getSafetyStatus() {
        return safetyStatus;
    }

    public LiveData<List<Map<String, String>>> getRecentAlerts() {
        return recentAlerts;
    }

    public LiveData<LocationHistoryEntity> getCurrentLocation() {
        return currentLocation;
    }

    public LiveData<Map<String, Boolean>> getServiceStatus() {
        return serviceStatus;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    // Get user safety status
    public void updateSafetyStatus() {
        // This is a simplified implementation. In a real app, you would:
        // 1. Consider active alerts
        // 2. Consider location safety (e.g., is the user in a safe area?)
        // 3. Consider time of day
        // 4. Consider user's movement patterns

        // For now, we'll base safety status on active alerts
        if (recentAlerts.getValue() != null && !recentAlerts.getValue().isEmpty()) {
            for (Map<String, String> alert : recentAlerts.getValue()) {
                if ("active".equals(alert.get("status"))) {
                    safetyStatus.setValue(SafetyStatus.DANGER);
                    return;
                }
            }
        }

        // If no active alerts, check if any services are running
        Map<String, Boolean> status = serviceStatus.getValue();
        if (status != null) {
            boolean anyServiceRunning = false;
            for (Boolean running : status.values()) {
                if (running) {
                    anyServiceRunning = true;
                    break;
                }
            }

            if (anyServiceRunning) {
                safetyStatus.setValue(SafetyStatus.SAFE);
            } else {
                safetyStatus.setValue(SafetyStatus.WARNING);
            }
        } else {
            safetyStatus.setValue(SafetyStatus.WARNING);
        }
    }

    // Get recent alerts
    public void loadRecentAlerts() {
        isLoading.setValue(true);

        alertRepository.getAlertHistory(new AlertRepository.AlertHistoryCallback() {
            @Override
            public void onSuccess(List<Map<String, String>> alerts) {
                // Sort alerts by timestamp (newest first)
                alerts.sort((a, b) -> {
                    String timestampA = a.get("timestamp");
                    String timestampB = b.get("timestamp");
                    if (timestampA == null || timestampB == null) return 0;
                    return timestampB.compareTo(timestampA);
                });

                // Limit to most recent 5 alerts
                List<Map<String, String>> recent = alerts.size() > 5
                        ? alerts.subList(0, 5)
                        : alerts;

                recentAlerts.postValue(recent);
                isLoading.postValue(false);

                // Update safety status based on alerts
                updateSafetyStatus();
            }

            @Override
            public void onError(String errorMsg) {
                isLoading.postValue(false);
                errorMessage.postValue("Failed to load alerts: " + errorMsg);
                Log.e(TAG, "Error loading alerts: " + errorMsg);
            }
        });
    }

    // Get current location
    public void loadCurrentLocation() {
        isLoading.setValue(true);

        locationRepository.getMostRecentLocation(new LocationHistoryRepository.LocationHistoryCallback() {
            @Override
            public void onLocationLoaded(LocationHistoryEntity location) {
                currentLocation.postValue(location);
                isLoading.postValue(false);
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue("Failed to load location: " + message);
                Log.e(TAG, "Error loading location: " + message);
            }
        });
    }

    // Check service status
    public void checkServiceStatus() {
        // This is a simplified implementation. In a real app, you would:
        // 1. Bind to services to check their status
        // 2. Use a more robust method to track service state

        // For now, we'll use a simple method to check if services are running
        Map<String, Boolean> status = new HashMap<>();

        // Check location tracking service
        boolean locationTrackingRunning = isServiceRunning(LocationTrackingService.class);
        status.put(LOCATION_TRACKING, locationTrackingRunning);

        // Check voice command service
        boolean voiceCommandRunning = isServiceRunning(VoiceCommandService.class);
        status.put(VOICE_COMMAND, voiceCommandRunning);

        // Check shake detection service
        boolean shakeDetectionRunning = isServiceRunning(ShakeDetectionService.class);
        status.put(SHAKE_DETECTION, shakeDetectionRunning);

        // Check fall detection service
        boolean fallDetectionRunning = isServiceRunning(FallDetectionService.class);
        status.put(FALL_DETECTION, fallDetectionRunning);

        serviceStatus.setValue(status);

        // Update safety status based on service status
        updateSafetyStatus();
    }

    // Helper method to check if a service is running
    private boolean isServiceRunning(Class<?> serviceClass) {
        // This is a simplified implementation and may not be 100% accurate
        // In a real app, you would use a more robust method

        // For now, we'll assume the service is running if it's in the preferences
        // or if we can detect it through other means

        // This is just a placeholder - in a real app, you would implement
        // proper service detection logic
        return false;
    }

    // Load user data
    private void loadUserData() {
        userRepository.getCurrentUser().observeForever(user -> {
            currentUser.setValue(user);
        });
    }

    // Refresh all dashboard data
    public void refreshDashboard() {
        loadUserData();
        loadRecentAlerts();
        loadCurrentLocation();
        checkServiceStatus();
    }
}

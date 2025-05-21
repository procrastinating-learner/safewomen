package com.example.safewomen.viewmodels;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.safewomen.services.FallDetectionService;
import com.example.safewomen.services.LocationTrackingService;
import com.example.safewomen.services.ShakeDetectionService;
import com.example.safewomen.services.VoiceCommandService;
import com.example.safewomen.utils.PreferenceManager;

public class SettingsViewModel extends ViewModel {
    private static final String TAG = "SettingsViewModel";

    // Preference keys
    private static final String KEY_VOICE_COMMAND_ENABLED = "voice_command_enabled";
    private static final String KEY_SHAKE_DETECTION_ENABLED = "shake_detection_enabled";
    private static final String KEY_FALL_DETECTION_ENABLED = "fall_detection_enabled";
    private static final String KEY_LOCATION_TRACKING_ENABLED = "location_tracking_enabled";
    private static final String KEY_EMERGENCY_CONTACT_NOTIFICATION = "emergency_contact_notification";
    private static final String KEY_AUTO_RECORD_ENABLED = "auto_record_enabled";
    private static final String KEY_DARK_MODE_ENABLED = "dark_mode_enabled";
    private static final String KEY_SOS_MESSAGE = "sos_message";

    // LiveData for settings
    private final MutableLiveData<Boolean> voiceCommandEnabled = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> shakeDetectionEnabled = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> fallDetectionEnabled = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> locationTrackingEnabled = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> emergencyContactNotification = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> autoRecordEnabled = new MutableLiveData<>(true);
    private final MutableLiveData<Boolean> darkModeEnabled = new MutableLiveData<>(false);
    private final MutableLiveData<String> sosMessage = new MutableLiveData<>("I need help! This is an emergency.");

    private final Application application;
    private final PreferenceManager preferenceManager;

    public SettingsViewModel(Application application) {
        this.application = application;
        this.preferenceManager = PreferenceManager.getInstance();

        // Load saved preferences
        loadUserPreferences();
    }

    // Getters for LiveData
    public LiveData<Boolean> getVoiceCommandEnabled() {
        return voiceCommandEnabled;
    }

    public LiveData<Boolean> getShakeDetectionEnabled() {
        return shakeDetectionEnabled;
    }

    public LiveData<Boolean> getFallDetectionEnabled() {
        return fallDetectionEnabled;
    }

    public LiveData<Boolean> getLocationTrackingEnabled() {
        return locationTrackingEnabled;
    }

    public LiveData<Boolean> getEmergencyContactNotification() {
        return emergencyContactNotification;
    }

    public LiveData<Boolean> getAutoRecordEnabled() {
        return autoRecordEnabled;
    }

    public LiveData<Boolean> getDarkModeEnabled() {
        return darkModeEnabled;
    }

    public LiveData<String> getSosMessage() {
        return sosMessage;
    }

    // Methods to toggle services

    // Toggle voice command detection
    public void toggleVoiceCommand(boolean enabled) {
        voiceCommandEnabled.setValue(enabled);
        saveUserPreferences();

        Intent intent = new Intent(application, VoiceCommandService.class);
        if (enabled) {
            intent.setAction("START_LISTENING");
            application.startService(intent);
            Log.d(TAG, "Voice command detection enabled");
        } else {
            intent.setAction("STOP_LISTENING");
            application.startService(intent);
            Log.d(TAG, "Voice command detection disabled");
        }
    }

    // Toggle shake detection
    public void toggleShakeDetection(boolean enabled) {
        shakeDetectionEnabled.setValue(enabled);
        saveUserPreferences();

        Intent intent = new Intent(application, ShakeDetectionService.class);
        if (enabled) {
            intent.setAction("START_MONITORING");
            application.startService(intent);
            Log.d(TAG, "Shake detection enabled");
        } else {
            intent.setAction("STOP_MONITORING");
            application.startService(intent);
            Log.d(TAG, "Shake detection disabled");
        }
    }

    // Toggle fall detection
    public void toggleFallDetection(boolean enabled) {
        fallDetectionEnabled.setValue(enabled);
        saveUserPreferences();

        Intent intent = new Intent(application, FallDetectionService.class);
        if (enabled) {
            intent.setAction("START_MONITORING");
            application.startService(intent);
            Log.d(TAG, "Fall detection enabled");
        } else {
            intent.setAction("STOP_MONITORING");
            application.startService(intent);
            Log.d(TAG, "Fall detection disabled");
        }
    }

    // Toggle location tracking
    public void toggleLocationTracking(boolean enabled) {
        locationTrackingEnabled.setValue(enabled);
        saveUserPreferences();

        Intent intent = new Intent(application, LocationTrackingService.class);
        if (enabled) {
            intent.setAction("START_TRACKING");
            application.startService(intent);
            Log.d(TAG, "Location tracking enabled");
        } else {
            intent.setAction("STOP_TRACKING");
            application.startService(intent);
            Log.d(TAG, "Location tracking disabled");
        }
    }

    // Toggle emergency contact notification
    public void toggleEmergencyContactNotification(boolean enabled) {
        emergencyContactNotification.setValue(enabled);
        saveUserPreferences();
        Log.d(TAG, "Emergency contact notification " + (enabled ? "enabled" : "disabled"));
    }

    // Toggle automatic recording during emergencies
    public void toggleAutoRecord(boolean enabled) {
        autoRecordEnabled.setValue(enabled);
        saveUserPreferences();
        Log.d(TAG, "Automatic recording " + (enabled ? "enabled" : "disabled"));
    }

    // Toggle dark mode
    public void toggleDarkMode(boolean enabled) {
        darkModeEnabled.setValue(enabled);
        saveUserPreferences();
        Log.d(TAG, "Dark mode " + (enabled ? "enabled" : "disabled"));
    }

    // Set custom SOS message
    public void setSosMessage(String message) {
        if (message != null && !message.trim().isEmpty()) {
            sosMessage.setValue(message);
            saveUserPreferences();
            Log.d(TAG, "SOS message updated");
        }
    }

    // Save user preferences
    private void saveUserPreferences() {
        Boolean voiceEnabled = voiceCommandEnabled.getValue();
        Boolean shakeEnabled = shakeDetectionEnabled.getValue();
        Boolean fallEnabled = fallDetectionEnabled.getValue();
        Boolean locationEnabled = locationTrackingEnabled.getValue();
        Boolean notificationEnabled = emergencyContactNotification.getValue();
        Boolean recordEnabled = autoRecordEnabled.getValue();
        Boolean darkMode = darkModeEnabled.getValue();
        String message = sosMessage.getValue();

        if (voiceEnabled != null) {
            preferenceManager.saveBoolean(KEY_VOICE_COMMAND_ENABLED, voiceEnabled);
        }

        if (shakeEnabled != null) {
            preferenceManager.saveBoolean(KEY_SHAKE_DETECTION_ENABLED, shakeEnabled);
        }

        if (fallEnabled != null) {
            preferenceManager.saveBoolean(KEY_FALL_DETECTION_ENABLED, fallEnabled);
        }

        if (locationEnabled != null) {
            preferenceManager.saveBoolean(KEY_LOCATION_TRACKING_ENABLED, locationEnabled);
        }

        if (notificationEnabled != null) {
            preferenceManager.saveBoolean(KEY_EMERGENCY_CONTACT_NOTIFICATION, notificationEnabled);
        }

        if (recordEnabled != null) {
            preferenceManager.saveBoolean(KEY_AUTO_RECORD_ENABLED, recordEnabled);
        }

        if (darkMode != null) {
            preferenceManager.saveBoolean(KEY_DARK_MODE_ENABLED, darkMode);
        }

        if (message != null) {
            preferenceManager.saveString(KEY_SOS_MESSAGE, message);
        }
    }

    // Load user preferences
    private void loadUserPreferences() {
        voiceCommandEnabled.setValue(preferenceManager.getBoolean(KEY_VOICE_COMMAND_ENABLED, false));
        shakeDetectionEnabled.setValue(preferenceManager.getBoolean(KEY_SHAKE_DETECTION_ENABLED, false));
        fallDetectionEnabled.setValue(preferenceManager.getBoolean(KEY_FALL_DETECTION_ENABLED, false));
        locationTrackingEnabled.setValue(preferenceManager.getBoolean(KEY_LOCATION_TRACKING_ENABLED, false));
        emergencyContactNotification.setValue(preferenceManager.getBoolean(KEY_EMERGENCY_CONTACT_NOTIFICATION, true));
        autoRecordEnabled.setValue(preferenceManager.getBoolean(KEY_AUTO_RECORD_ENABLED, true));
        darkModeEnabled.setValue(preferenceManager.getBoolean(KEY_DARK_MODE_ENABLED, false));

        String savedMessage = preferenceManager.getString(KEY_SOS_MESSAGE, "I need help! This is an emergency.");
        sosMessage.setValue(savedMessage);

        // Start enabled services
        if (voiceCommandEnabled.getValue()) {
            Intent intent = new Intent(application, VoiceCommandService.class);
            intent.setAction("START_LISTENING");
            application.startService(intent);
        }

        if (shakeDetectionEnabled.getValue()) {
            Intent intent = new Intent(application, ShakeDetectionService.class);
            intent.setAction("START_MONITORING");
            application.startService(intent);
        }

        if (fallDetectionEnabled.getValue()) {
            Intent intent = new Intent(application, FallDetectionService.class);
            intent.setAction("START_MONITORING");
            application.startService(intent);
        }

        if (locationTrackingEnabled.getValue()) {
            Intent intent = new Intent(application, LocationTrackingService.class);
            intent.setAction("START_TRACKING");
            application.startService(intent);
        }
    }

    // Apply all settings at once (useful when app starts)
    public void applyAllSettings() {
        // Start or stop services based on current settings
        toggleVoiceCommand(voiceCommandEnabled.getValue());
        toggleShakeDetection(shakeDetectionEnabled.getValue());
        toggleFallDetection(fallDetectionEnabled.getValue());
        toggleLocationTracking(locationTrackingEnabled.getValue());
    }

    // Reset all settings to defaults
    public void resetToDefaults() {
        toggleVoiceCommand(false);
        toggleShakeDetection(false);
        toggleFallDetection(false);
        toggleLocationTracking(false);
        toggleEmergencyContactNotification(true);
        toggleAutoRecord(true);
        toggleDarkMode(false);
        setSosMessage("I need help! This is an emergency.");
    }
}

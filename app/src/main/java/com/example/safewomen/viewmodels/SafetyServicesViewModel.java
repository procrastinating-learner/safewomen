package com.example.safewomen.viewmodels;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.safewomen.services.EmergencyRecordingService;
import com.example.safewomen.services.FallDetectionService;
import com.example.safewomen.services.LocationTrackingService;
import com.example.safewomen.services.ShakeDetectionService;
import com.example.safewomen.services.VoiceCommandService;

import java.util.HashMap;
import java.util.Map;

public class SafetyServicesViewModel extends ViewModel {
    private static final String TAG = "SafetyServicesViewModel";
    private final Application application;
    private final MutableLiveData<Map<String, Boolean>> serviceStatus = new MutableLiveData<>();

    // Service keys
    private static final String VOICE_COMMAND = "voice_command";
    private static final String SHAKE_DETECTION = "shake_detection";
    private static final String FALL_DETECTION = "fall_detection";
    private static final String LOCATION_TRACKING = "location_tracking";
    private static final String EMERGENCY_RECORDING = "emergency_recording";

    public SafetyServicesViewModel(Application application) {
        this.application = application;
        initServiceStatus();
    }

    // Initialize service status map
    private void initServiceStatus() {
        Map<String, Boolean> status = new HashMap<>();
        status.put(VOICE_COMMAND, false);
        status.put(SHAKE_DETECTION, false);
        status.put(FALL_DETECTION, false);
        status.put(LOCATION_TRACKING, false);
        status.put(EMERGENCY_RECORDING, false);
        serviceStatus.setValue(status);
    }

    // Get service status LiveData
    public LiveData<Map<String, Boolean>> getServiceStatus() {
        return serviceStatus;
    }

    // Check if a specific service is running
    public boolean isServiceRunning(String serviceKey) {
        Map<String, Boolean> status = serviceStatus.getValue();
        return status != null && status.getOrDefault(serviceKey, false);
    }

    // Update service status
    private void updateServiceStatus(String serviceKey, boolean isRunning) {
        Map<String, Boolean> status = serviceStatus.getValue();
        if (status != null) {
            status.put(serviceKey, isRunning);
            serviceStatus.setValue(status);
        }
    }

    // Start/stop voice command detection
    public void toggleVoiceCommandDetection(boolean start) {
        Intent intent = new Intent(application, VoiceCommandService.class);

        if (start) {
            intent.setAction("START_LISTENING");
            application.startService(intent);
            updateServiceStatus(VOICE_COMMAND, true);
            Log.d(TAG, "Voice command detection started");
        } else {
            intent.setAction("STOP_LISTENING");
            application.startService(intent);
            updateServiceStatus(VOICE_COMMAND, false);
            Log.d(TAG, "Voice command detection stopped");
        }
    }

    // Start voice command detection
    public void startVoiceCommandDetection() {
        toggleVoiceCommandDetection(true);
    }

    // Stop voice command detection
    public void stopVoiceCommandDetection() {
        toggleVoiceCommandDetection(false);
    }

    // Start/stop shake detection
    public void toggleShakeDetection(boolean start) {
        Intent intent = new Intent(application, ShakeDetectionService.class);

        if (start) {
            intent.setAction("START_MONITORING");
            application.startService(intent);
            updateServiceStatus(SHAKE_DETECTION, true);
            Log.d(TAG, "Shake detection started");
        } else {
            intent.setAction("STOP_MONITORING");
            application.startService(intent);
            updateServiceStatus(SHAKE_DETECTION, false);
            Log.d(TAG, "Shake detection stopped");
        }
    }

    // Start shake detection
    public void startShakeDetection() {
        toggleShakeDetection(true);
    }

    // Stop shake detection
    public void stopShakeDetection() {
        toggleShakeDetection(false);
    }

    // Start/stop fall detection
    public void toggleFallDetection(boolean start) {
        Intent intent = new Intent(application, FallDetectionService.class);

        if (start) {
            intent.setAction("START_MONITORING");
            application.startService(intent);
            updateServiceStatus(FALL_DETECTION, true);
            Log.d(TAG, "Fall detection started");
        } else {
            intent.setAction("STOP_MONITORING");
            application.startService(intent);
            updateServiceStatus(FALL_DETECTION, false);
            Log.d(TAG, "Fall detection stopped");
        }
    }

    // Start fall detection
    public void startFallDetection() {
        toggleFallDetection(true);
    }

    // Stop fall detection
    public void stopFallDetection() {
        toggleFallDetection(false);
    }

    // Start/stop location tracking
    public void toggleLocationTracking(boolean start) {
        Intent intent = new Intent(application, LocationTrackingService.class);

        if (start) {
            intent.setAction("START_TRACKING");
            application.startService(intent);
            updateServiceStatus(LOCATION_TRACKING, true);
            Log.d(TAG, "Location tracking started");
        } else {
            intent.setAction("STOP_TRACKING");
            application.startService(intent);
            updateServiceStatus(LOCATION_TRACKING, false);
            Log.d(TAG, "Location tracking stopped");
        }
    }

    // Start location tracking
    public void startLocationTracking() {
        toggleLocationTracking(true);
    }

    // Stop location tracking
    public void stopLocationTracking() {
        toggleLocationTracking(false);
    }

    // Start/stop emergency recording
    public void toggleEmergencyRecording(boolean start, boolean videoMode) {
        Intent intent = new Intent(application, EmergencyRecordingService.class);

        if (start) {
            if (videoMode) {
                intent.setAction("START_VIDEO_RECORDING");
            } else {
                intent.setAction("START_AUDIO_RECORDING");
            }
            application.startService(intent);
            updateServiceStatus(EMERGENCY_RECORDING, true);
            Log.d(TAG, "Emergency recording started (mode: " + (videoMode ? "video" : "audio") + ")");
        } else {
            intent.setAction("STOP_RECORDING");
            application.startService(intent);
            updateServiceStatus(EMERGENCY_RECORDING, false);
            Log.d(TAG, "Emergency recording stopped");
        }
    }

    // Start audio recording
    public void startAudioRecording() {
        toggleEmergencyRecording(true, false);
    }

    // Start video recording
    public void startVideoRecording() {
        toggleEmergencyRecording(true, true);
    }

    // Stop emergency recording
    public void stopEmergencyRecording() {
        toggleEmergencyRecording(false, false);
    }

    // Start all safety services
    public void startAllServices() {
        startVoiceCommandDetection();
        startShakeDetection();
        startFallDetection();
        startLocationTracking();
    }

    // Stop all safety services
    public void stopAllServices() {
        stopVoiceCommandDetection();
        stopShakeDetection();
        stopFallDetection();
        stopLocationTracking();
        stopEmergencyRecording();
    }

    // Check if any service is running
    public boolean isAnyServiceRunning() {
        Map<String, Boolean> status = serviceStatus.getValue();
        if (status == null) return false;

        for (Boolean running : status.values()) {
            if (running) return true;
        }
        return false;
    }
}

package com.example.safewomen.viewmodels;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.safewomen.models.entities.EmergencyContactEntity;
import com.example.safewomen.models.entities.LocationHistoryEntity;
import com.example.safewomen.repositories.AlertRepository;
import com.example.safewomen.repositories.ContactRepository;
import com.example.safewomen.repositories.LocationHistoryRepository;
import com.example.safewomen.services.EmergencyRecordingService;
import com.example.safewomen.services.SosAlertService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AlertViewModel extends ViewModel {
    private static final String TAG = "AlertViewModel";
    private final AlertRepository alertRepository;
    private final ContactRepository contactRepository;
    private final LocationHistoryRepository locationRepository;
    private final MutableLiveData<Boolean> isAlertActive = new MutableLiveData<>(false);
    private final MutableLiveData<String> activeAlertId = new MutableLiveData<>();
    private final MutableLiveData<List<Map<String, String>>> alertHistory = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    private final Application application;

    public AlertViewModel() {
        this.application = null;
        alertRepository = AlertRepository.getInstance();
        contactRepository = ContactRepository.getInstance();
        locationRepository = LocationHistoryRepository.getInstance();

        // Load alert history on initialization
        loadAlertHistory();
    }

    // Getters for LiveData
    public LiveData<Boolean> isAlertActive() {
        return isAlertActive;
    }

    public LiveData<String> getActiveAlertId() {
        return activeAlertId;
    }

    public LiveData<List<Map<String, String>>> getAlertHistory() {
        return alertHistory;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }

    // Trigger SOS alert
    public void triggerSosAlert(String triggerMethod) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        successMessage.setValue(null);

        // Get current location
        locationRepository.getMostRecentLocation(new LocationHistoryRepository.LocationHistoryCallback() {
            @Override
            public void onLocationLoaded(LocationHistoryEntity location) {
                if (location != null) {
                    // Create alert with location data
                    createAlert(location, triggerMethod);
                } else {
                    // No location available, create alert without location
                    createAlert(null, triggerMethod);
                }
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Error getting location: " + message);
                // Create alert without location
                createAlert(null, triggerMethod);
            }
        });

        // Start SOS alert service
        Intent sosIntent = new Intent(application, SosAlertService.class);
        sosIntent.setAction("TRIGGER_SOS");
        sosIntent.putExtra("TRIGGER_METHOD", triggerMethod);
        application.startService(sosIntent);

        // Start emergency recording
        startEmergencyRecording();
    }

    // Create alert in the repository
    private void createAlert(LocationHistoryEntity location, String triggerMethod) {
        double latitude = 0;
        double longitude = 0;
        String address = "";

        if (location != null) {
            latitude = location.getLatitude();
            longitude = location.getLongitude();
            address = location.getAddress();
        }

        alertRepository.createAlert(latitude, longitude, address, triggerMethod, new AlertRepository.AlertCallback() {
            @Override
            public void onSuccess(String alertId, String message) {
                isLoading.postValue(false);
                isAlertActive.postValue(true);
                activeAlertId.postValue(alertId);
                successMessage.postValue("SOS alert triggered: " + message);

                // Reload alert history
                loadAlertHistory();
            }

            @Override
            public void onError(String errorMsg) {
                isLoading.postValue(false);
                errorMessage.postValue("Failed to create alert: " + errorMsg);
                Log.e(TAG, "Error creating alert: " + errorMsg);
            }
        });
    }

    // Start emergency recording
    private void startEmergencyRecording() {
        Intent recordingIntent = new Intent(application, EmergencyRecordingService.class);
        recordingIntent.setAction("START_AUDIO_RECORDING");
        application.startService(recordingIntent);
    }

    // Cancel active alert
    public void cancelAlert() {
        String alertId = activeAlertId.getValue();
        if (alertId == null || alertId.isEmpty()) {
            errorMessage.setValue("No active alert to cancel");
            return;
        }

        isLoading.setValue(true);
        errorMessage.setValue(null);
        successMessage.setValue(null);

        alertRepository.updateAlertStatus(alertId, "resolved", new AlertRepository.AlertCallback() {
            @Override
            public void onSuccess(String alertId, String message) {
                isLoading.postValue(false);
                isAlertActive.postValue(false);
                activeAlertId.postValue(null);
                successMessage.postValue("Alert canceled: " + message);

                // Stop emergency recording
                stopEmergencyRecording();

                // Reload alert history
                loadAlertHistory();
            }

            @Override
            public void onError(String errorMsg) {
                isLoading.postValue(false);
                errorMessage.postValue("Failed to cancel alert: " + errorMsg);
                Log.e(TAG, "Error canceling alert: " + errorMsg);
            }
        });
    }

    // Stop emergency recording
    private void stopEmergencyRecording() {
        Intent recordingIntent = new Intent(application, EmergencyRecordingService.class);
        recordingIntent.setAction("STOP_RECORDING");
        application.startService(recordingIntent);
    }

    // Get alert history
    public void loadAlertHistory() {
        isLoading.setValue(true);

        alertRepository.getAlertHistory(new AlertRepository.AlertHistoryCallback() {
            @Override
            public void onSuccess(List<Map<String, String>> alerts) {
                alertHistory.postValue(alerts);
                isLoading.postValue(false);

                // Check if there's an active alert
                checkForActiveAlert(alerts);
            }

            @Override
            public void onError(String errorMsg) {
                isLoading.postValue(false);
                errorMessage.postValue("Failed to load alert history: " + errorMsg);
                Log.e(TAG, "Error loading alert history: " + errorMsg);
            }
        });
    }

    // Check for active alerts in the history
    private void checkForActiveAlert(List<Map<String, String>> alerts) {
        boolean hasActiveAlert = false;
        String activeId = null;

        for (Map<String, String> alert : alerts) {
            if ("active".equals(alert.get("status"))) {
                hasActiveAlert = true;
                activeId = alert.get("id");
                break;
            }
        }

        isAlertActive.postValue(hasActiveAlert);
        activeAlertId.postValue(activeId);
    }

    // Mark alert as false alarm
    public void markAsFalseAlarm() {
        String alertId = activeAlertId.getValue();
        if (alertId == null || alertId.isEmpty()) {
            errorMessage.setValue("No active alert to mark as false alarm");
            return;
        }

        isLoading.setValue(true);
        errorMessage.setValue(null);
        successMessage.setValue(null);

        alertRepository.updateAlertStatus(alertId, "false_alarm", new AlertRepository.AlertCallback() {
            @Override
            public void onSuccess(String alertId, String message) {
                isLoading.postValue(false);
                isAlertActive.postValue(false);
                activeAlertId.postValue(null);
                successMessage.postValue("Alert marked as false alarm: " + message);

                // Stop emergency recording
                stopEmergencyRecording();

                // Reload alert history
                loadAlertHistory();
            }

            @Override
            public void onError(String errorMsg) {
                isLoading.postValue(false);
                errorMessage.postValue("Failed to mark alert as false alarm: " + errorMsg);
                Log.e(TAG, "Error marking alert as false alarm: " + errorMsg);
            }
        });
    }

    // Get alert details by ID
    public void getAlertDetails(String alertId, AlertDetailsCallback callback) {
        if (alertHistory.getValue() != null) {
            for (Map<String, String> alert : alertHistory.getValue()) {
                if (alertId.equals(alert.get("id"))) {
                    callback.onAlertDetailsLoaded(alert);
                    return;
                }
            }
        }

        // If not found in cache, reload history
        alertRepository.getAlertHistory(new AlertRepository.AlertHistoryCallback() {
            @Override
            public void onSuccess(List<Map<String, String>> alerts) {
                alertHistory.postValue(alerts);

                for (Map<String, String> alert : alerts) {
                    if (alertId.equals(alert.get("id"))) {
                        callback.onAlertDetailsLoaded(alert);
                        return;
                    }
                }

                callback.onError("Alert not found");
            }

            @Override
            public void onError(String errorMsg) {
                callback.onError("Failed to load alert details: " + errorMsg);
            }
        });
    }

    // Get emergency contacts
    public LiveData<List<EmergencyContactEntity>> getEmergencyContacts() {
        return contactRepository.getContacts();
    }

    // Interface for alert details callback
    public interface AlertDetailsCallback {
        void onAlertDetailsLoaded(Map<String, String> alertDetails);
        void onError(String message);
    }
}

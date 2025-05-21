package com.example.safewomen.viewmodels;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.safewomen.models.entities.LocationHistoryEntity;
import com.example.safewomen.repositories.LocationHistoryRepository;
import com.example.safewomen.services.LocationTrackingService;
import com.example.safewomen.utils.ServiceUtils;
import com.google.android.gms.maps.model.LatLng;

public class LocationViewModel extends AndroidViewModel {
    private static final String TAG = "LocationViewModel";

    private final Application application;
    private final LocationHistoryRepository locationRepository;
    private final MutableLiveData<LatLng> currentLocation = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLocationServiceRunning = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public LocationViewModel(Application application) {
        super(application);
        this.application = application;
        locationRepository = LocationHistoryRepository.getInstance();

        // Load current location
        loadCurrentLocation();

        // Check if location service is running
        checkLocationServiceStatus();
    }

    // Getters for LiveData
    public LiveData<LatLng> getCurrentLocation() {
        return currentLocation;
    }

    public LiveData<Boolean> getIsLocationServiceRunning() {
        return isLocationServiceRunning;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    // Load current location
    public void loadCurrentLocation() {
        isLoading.setValue(true);

        locationRepository.getMostRecentLocation(new LocationHistoryRepository.LocationHistoryCallback() {
            @Override
            public void onLocationLoaded(LocationHistoryEntity location) {
                if (location != null) {
                    LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                    currentLocation.postValue(latLng);
                }
                isLoading.postValue(false);
            }

            @Override
            public void onError(String message) {
                errorMessage.postValue("Failed to load location: " + message);
                isLoading.postValue(false);
                Log.e(TAG, "Error loading location: " + message);
            }
        });
    }

    // Start location tracking
    public void startLocationTracking() {
        Intent intent = new Intent(application, LocationTrackingService.class);
        intent.setAction("START_TRACKING");
        application.startService(intent);

        isLocationServiceRunning.setValue(true);
    }

    // Stop location tracking
    public void stopLocationTracking() {
        Intent intent = new Intent(application, LocationTrackingService.class);
        intent.setAction("STOP_TRACKING");
        application.startService(intent);

        isLocationServiceRunning.setValue(false);
    }

    // Check if location service is running
    public void checkLocationServiceStatus() {
        boolean isRunning = ServiceUtils.isServiceRunning(application, LocationTrackingService.class);
        isLocationServiceRunning.setValue(isRunning);
    }

    // Clear location history
    public void clearLocationHistory() {
        locationRepository.clearLocationHistory();
    }
}

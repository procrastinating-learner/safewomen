package com.example.safewomen.repositories;

import android.app.Application;
import android.location.Location;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.safewomen.api.ApiClient;
import com.example.safewomen.api.ApiService;
import com.example.safewomen.data.LocationHistoryDao;
import com.example.safewomen.data.SafeWomenDatabase;
import com.example.safewomen.models.entities.LocationHistoryEntity;
import com.example.safewomen.utils.NetworkUtil;
import com.example.safewomen.utils.PreferenceManager;

import org.json.JSONObject;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository for managing location history with offline support
 */
public class LocationHistoryRepository {
    private static final String TAG = "LocationHistoryRepo";
    private static LocationHistoryRepository instance;
    private static Application appContext;

    private final LocationHistoryDao locationHistoryDao;
    private final ApiService apiService;
    private final Executor executor;

    // Constants
    private static final int MAX_HISTORY_ITEMS = 100; // Maximum number of location history items to keep
    private static final int DEFAULT_RETENTION_DAYS = 30; // Default number of days to keep location history

    public static synchronized void init(Application application) {
        if (instance == null) {
            appContext = application;
            instance = new LocationHistoryRepository();
        }
    }

    public static synchronized LocationHistoryRepository getInstance() {
        if (instance == null) {
            throw new IllegalStateException("LocationHistoryRepository must be initialized first");
        }
        return instance;
    }

    private LocationHistoryRepository() {
        SafeWomenDatabase db = SafeWomenDatabase.getInstance(appContext);
        locationHistoryDao = db.locationHistoryDao();
        apiService = ApiClient.getClient().create(ApiService.class);
        executor = Executors.newSingleThreadExecutor();

        // Clean up old locations on initialization
        cleanupOldLocations();
    }

    /**
     * Add a new location to history and sync with server if online
     */
    public void addLocation(Location location, String address) {
        if (location == null) return;

        executor.execute(() -> {
            // Create new location history entity
            LocationHistoryEntity locationEntity = new LocationHistoryEntity(
                    UUID.randomUUID().toString(),
                    location.getLatitude(),
                    location.getLongitude(),
                    address != null ? address : "",
                    System.currentTimeMillis(),
                    location.getAccuracy()
            );

            // Insert into database
            locationHistoryDao.insertLocation(locationEntity);

            // Check if we need to clean up old locations
            if (locationHistoryDao.getLocationCount() > MAX_HISTORY_ITEMS) {
                cleanupOldLocations();
            }

            // Sync with server if online and user is logged in
            if (NetworkUtil.isOnline(appContext) && PreferenceManager.getInstance().isLoggedIn()) {
                syncLocationWithServer(locationEntity);
            }
        });
    }

    /**
     * Get all location history as LiveData
     */
    public LiveData<List<LocationHistoryEntity>> getLocationHistory() {
        return locationHistoryDao.observeLocationHistory();
    }

    /**
     * Get recent locations with a specified limit
     */
    public void getRecentLocations(int limit, LocationHistoryCallback callback) {
        executor.execute(() -> {
            List<LocationHistoryEntity> locations = locationHistoryDao.getRecentLocations(limit);
            if (callback != null) {
                callback.onLocationsLoaded(locations);
            }
        });
    }

    /**
     * Get the most recent location
     */
    public void getMostRecentLocation(LocationHistoryCallback callback) {
        executor.execute(() -> {
            LocationHistoryEntity location = locationHistoryDao.getMostRecentLocation();
            if (location != null && callback != null) {
                callback.onLocationLoaded(location);
            } else if (callback != null) {
                callback.onError("No location history available");
            }
        });
    }

    /**
     * Get the most recent location synchronously (should be called from background thread)
     */
    public LocationHistoryEntity getMostRecentLocationSync() {
        return locationHistoryDao.getMostRecentLocation();
    }
    /**
     * Clean up old locations based on retention policy
     */
    private void cleanupOldLocations() {
        executor.execute(() -> {
            // Calculate timestamp for retention threshold (default 30 days)
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_YEAR, -DEFAULT_RETENTION_DAYS);
            long thresholdTimestamp = calendar.getTimeInMillis();

            // Delete locations older than threshold
            locationHistoryDao.deleteOldLocations(thresholdTimestamp);
        });
    }

    /**
     * Sync a location with the server
     */
    private void syncLocationWithServer(LocationHistoryEntity location) {
        String userId = PreferenceManager.getInstance().getUserId();
        if (userId == null) return;

        // Create parameters for PHP
        Map<String, String> params = new HashMap<>();
        params.put("user_id", userId);
        params.put("latitude", String.valueOf(location.getLatitude()));
        params.put("longitude", String.valueOf(location.getLongitude()));
        params.put("accuracy", String.valueOf(location.getAccuracy()));
        params.put("timestamp", String.valueOf(location.getTimestamp()));
        params.put("address", location.getAddress());

        apiService.updateLocation(params).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseString = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseString);

                        if (!jsonResponse.optBoolean("success", false)) {
                            Log.w(TAG, "Server rejected location update: " +
                                    jsonResponse.optString("message", "Unknown error"));
                        }
                    } else {
                        Log.w(TAG, "Server error when updating location: " + response.code());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing location update response", e);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Network error when updating location", t);
            }
        });
    }

    /**
     * Get locations within a specific time range
     */
    public void getLocationsInTimeRange(long startTime, long endTime, LocationHistoryCallback callback) {
        executor.execute(() -> {
            List<LocationHistoryEntity> locations = locationHistoryDao.getLocationsInTimeRange(startTime, endTime);
            if (callback != null) {
                callback.onLocationsLoaded(locations);
            }
        });
    }

    /**
     * Clear all location history
     */
    public void clearLocationHistory() {
        executor.execute(() -> {
            locationHistoryDao.clearAllLocations();
        });
    }

    /**
     * Get location count
     */
    public void getLocationCount(LocationCountCallback callback) {
        executor.execute(() -> {
            int count = locationHistoryDao.getLocationCount();
            if (callback != null) {
                callback.onLocationCountLoaded(count);
            }
        });
    }

    /**
     * Export location history as JSON string
     */
    public void exportLocationHistory(ExportCallback callback) {
        executor.execute(() -> {
            try {
                List<LocationHistoryEntity> locations = locationHistoryDao.getAllLocationsForExport();

                // Create JSON array of locations
                StringBuilder jsonBuilder = new StringBuilder("[");
                for (int i = 0; i < locations.size(); i++) {
                    LocationHistoryEntity location = locations.get(i);
                    jsonBuilder.append("{")
                            .append("\"id\":\"").append(location.getId()).append("\",")
                            .append("\"latitude\":").append(location.getLatitude()).append(",")
                            .append("\"longitude\":").append(location.getLongitude()).append(",")
                            .append("\"address\":\"").append(location.getAddress().replace("\"", "\\\"")).append("\",")
                            .append("\"timestamp\":").append(location.getTimestamp()).append(",")
                            .append("\"accuracy\":").append(location.getAccuracy())
                            .append("}");

                    if (i < locations.size() - 1) {
                        jsonBuilder.append(",");
                    }
                }
                jsonBuilder.append("]");

                if (callback != null) {
                    callback.onExportComplete(jsonBuilder.toString());
                }
            } catch (Exception e) {
                Log.e(TAG, "Error exporting location history", e);
                if (callback != null) {
                    callback.onExportError("Error exporting data: " + e.getMessage());
                }
            }
        });
    }

    /**
     * Callback interface for location history operations
     */
    public interface LocationHistoryCallback {
        default void onLocationLoaded(LocationHistoryEntity location) {}
        default void onLocationsLoaded(List<LocationHistoryEntity> locations) {}
        default void onError(String message) {}
    }

    /**
     * Callback interface for location count operations
     */
    public interface LocationCountCallback {
        void onLocationCountLoaded(int count);
    }

    /**
     * Callback interface for export operations
     */
    public interface ExportCallback {
        void onExportComplete(String jsonData);
        void onExportError(String message);
    }
}

package com.example.safewomen.utils;

import android.content.Context;
import android.location.Location;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.maps.GeoApiContext;
import com.google.maps.GeocodingApi;
import com.google.maps.model.GeocodingResult;

import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Utility class to resolve geographic coordinates to human-readable addresses
 * using Google Maps Geocoding API directly
 */
public class AddressResolver {
    private static final String TAG = "AddressResolver";

    private final Context context;
    private final GeoApiContext geoApiContext;
    private final Executor executor;
    private final Handler mainHandler;

    public AddressResolver(Context context) {
        this.context = context;
        this.geoApiContext = new GeoApiContext.Builder()
                .apiKey(Constants.MAPS_API_KEY)  // Use Constants class instead of BuildConfig
                .build();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    /**
     * Get address from location asynchronously
     * @param location The location to resolve
     * @param callback Callback to receive the address
     */
    public void getAddressFromLocation(Location location, AddressCallback callback) {
        if (location == null) {
            if (callback != null) {
                mainHandler.post(() -> callback.onAddressResolved(null));
            }
            return;
        }

        executor.execute(() -> {
            String addressText = null;

            try {
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
                GeocodingResult[] results = GeocodingApi.reverseGeocode(geoApiContext,
                                new com.google.maps.model.LatLng(latLng.latitude, latLng.longitude))
                        .await();

                if (results != null && results.length > 0) {
                    addressText = results[0].formattedAddress;
                } else {
                    // Fallback to coordinates if no results
                    addressText = formatCoordinates(location);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error getting address from location", e);
                addressText = formatCoordinates(location);
            }

            final String finalAddressText = addressText;
            if (callback != null) {
                mainHandler.post(() -> callback.onAddressResolved(finalAddressText));
            }
        });
    }

    /**
     * Format coordinates as a string
     */
    private String formatCoordinates(Location location) {
        return String.format(
                Locale.getDefault(),
                "%.6f, %.6f",
                location.getLatitude(),
                location.getLongitude()
        );
    }

    /**
     * Callback interface for address resolution
     */
    public interface AddressCallback {
        void onAddressResolved(String address);
    }
}

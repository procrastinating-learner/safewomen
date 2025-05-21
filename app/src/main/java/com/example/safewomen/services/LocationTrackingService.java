package com.example.safewomen.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.location.Location;
import android.os.Build;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.safewomen.MainActivity;
import com.example.safewomen.R;
import com.example.safewomen.models.entities.LocationHistoryEntity;
import com.example.safewomen.repositories.LocationHistoryRepository;
import com.example.safewomen.utils.AddressResolver;
import com.example.safewomen.utils.PreferenceManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

/**
 * Foreground service for real-time GPS tracking
 */
public class LocationTrackingService extends Service {
    private static final String TAG = "LocationTrackingService";
    private static final String CHANNEL_ID = "location_tracking_channel";
    private static final int NOTIFICATION_ID = 1001;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationHistoryRepository locationRepository;
    private AddressResolver addressResolver;
    private boolean isTracking = false;

    @Override
    public void onCreate() {
        super.onCreate();

        locationRepository = LocationHistoryRepository.getInstance();
        addressResolver = new AddressResolver(this);

        // Initialize location client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Create location callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;

                for (Location location : locationResult.getLocations()) {
                    // Process new location
                    processNewLocation(location);
                }
            }
        };

        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case "START_TRACKING":
                        startLocationTracking();
                        break;
                    case "STOP_TRACKING":
                        stopLocationTracking();
                        break;
                }
            } else {
                // Default action is to start tracking
                startLocationTracking();
            }
        }

        // Make this a sticky service that will be restarted if killed
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopLocationTracking();
        super.onDestroy();
    }

    private void startLocationTracking() {
        if (isTracking) return;

        // Create location request
        LocationRequest locationRequest = new LocationRequest.Builder(10000) // 10 seconds
                .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                .setMinUpdateIntervalMillis(5000) // 5 seconds
                .setMaxUpdateDelayMillis(15000) // 15 seconds
                .build();

        try {
            fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
            );

            isTracking = true;

            // Start as foreground service with notification
            startForeground(NOTIFICATION_ID, createNotification());

            Log.d(TAG, "Location tracking started");
        } catch (SecurityException e) {
            Log.e(TAG, "Error starting location tracking", e);
        }
    }

    private void stopLocationTracking() {
        if (!isTracking) return;

        fusedLocationClient.removeLocationUpdates(locationCallback);
        isTracking = false;

        // Stop foreground service
        stopForeground(true);
        stopSelf();

        Log.d(TAG, "Location tracking stopped");
    }

    private void processNewLocation(Location location) {
        if (location == null) return;

        // Resolve address asynchronously
        addressResolver.getAddressFromLocation(location, address -> {
            // Save location to repository
            locationRepository.addLocation(location, address);

            // Update notification with new location
            updateNotification(location, address);

            // Check if we need to trigger any scheduled alerts
            checkScheduledAlerts(location);
        });
    }

    private void checkScheduledAlerts(Location location) {
        // TODO: Implement scheduled alert checking
        // This would check if the user has any scheduled alerts
        // For example, "alert if I'm not home by 10pm"
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Tracking",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Used for tracking your location in the background");

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SafeWomen is protecting you")
                .setContentText("Your location is being tracked for your safety")
                .setSmallIcon(R.drawable.ic_location)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private void updateNotification(Location location, String address) {
        NotificationManager notificationManager = getSystemService(NotificationManager.class);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SafeWomen is protecting you")
                .setContentText(address != null ? "Current location: " + address : "Tracking your location")
                .setSmallIcon(R.drawable.ic_location)
                .setPriority(NotificationCompat.PRIORITY_LOW);

        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }
}

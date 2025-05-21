package com.example.safewomen.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.safewomen.MainActivity;
import com.example.safewomen.R;

import java.util.concurrent.TimeUnit;

/**
 * Service for detecting shake gestures to trigger SOS alerts
 * This service runs in the background even when the app is closed
 */
public class ShakeDetectionService extends Service implements SensorEventListener {
    private static final String TAG = "ShakeDetectionService";
    private static final String CHANNEL_ID = "shake_detection_channel";
    private static final int NOTIFICATION_ID = 1006;

    // Shake detection parameters
    private static final float SHAKE_THRESHOLD = 20.0f; // Threshold for shake detection
    private static final int SHAKE_SLOP_TIME_MS = 500; // Minimum time between shakes
    private static final int SHAKE_COUNT_RESET_TIME_MS = 3000; // Time to reset shake count
    private static final int REQUIRED_SHAKES = 3; // Number of shakes required to trigger SOS

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private PowerManager.WakeLock wakeLock;
    private boolean isMonitoring = false;

    // Shake detection variables
    private long lastShakeTime = 0;
    private int shakeCount = 0;
    private long firstShakeTime = 0;

    // Confirmation variables
    private boolean confirmationPending = false;
    private long confirmationStartTime = 0;
    private static final long CONFIRMATION_TIMEOUT_MS = TimeUnit.SECONDS.toMillis(10); // 10 seconds to confirm

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();

        // Initialize sensors
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer == null) {
                Log.e(TAG, "Accelerometer not available on this device");
                stopSelf();
            }
        } else {
            Log.e(TAG, "SensorManager not available");
            stopSelf();
        }

        // Initialize wake lock to keep CPU running for this service
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            wakeLock = powerManager.newWakeLock(
                    PowerManager.PARTIAL_WAKE_LOCK,
                    "SafeWomen:ShakeDetectionWakeLock"
            );
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case "START_MONITORING":
                        startShakeDetection();
                        break;
                    case "STOP_MONITORING":
                        stopShakeDetection();
                        break;
                    case "CANCEL_ALERT":
                        cancelShakeAlert();
                        break;
                    case "CONFIRM_SOS":
                        confirmSosAlert();
                        break;
                }
            } else {
                // Default action is to start monitoring
                startShakeDetection();
            }
        }

        // If service is killed, restart it
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopShakeDetection();
        super.onDestroy();
    }

    private void startShakeDetection() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Notification permission not granted");
                // Service can still run, but notifications won't show
            }
        }
        if (isMonitoring || sensorManager == null || accelerometer == null) return;

        // Acquire wake lock to keep CPU running
        if (wakeLock != null && !wakeLock.isHeld()) {
            wakeLock.acquire();
        }

        // Start as foreground service with notification
        startForeground(NOTIFICATION_ID, createNotification(false));

        // Register sensor listener with SENSOR_DELAY_GAME for responsive detection
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_GAME);
        isMonitoring = true;

        // Reset shake detection variables
        shakeCount = 0;
        lastShakeTime = 0;
        firstShakeTime = 0;
        confirmationPending = false;

        Log.d(TAG, "Shake detection started");
    }

    private void stopShakeDetection() {
        if (!isMonitoring || sensorManager == null) return;

        // Unregister sensor listener
        sensorManager.unregisterListener(this);
        isMonitoring = false;

        // Release wake lock
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }

        // Stop foreground service
        stopForeground(true);
        stopSelf();

        Log.d(TAG, "Shake detection stopped");
    }

    private void cancelShakeAlert() {
        // Reset shake detection variables
        shakeCount = 0;
        lastShakeTime = 0;
        firstShakeTime = 0;
        confirmationPending = false;

        // Update notification to normal state
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.notify(NOTIFICATION_ID, createNotification(false));

        Log.d(TAG, "Shake alert canceled");
    }

    private void confirmSosAlert() {
        if (confirmationPending) {
            // User confirmed SOS, trigger alert
            triggerSosAlert();

            // Reset confirmation state
            confirmationPending = false;
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

        // Get accelerometer values
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];

        // Calculate acceleration magnitude
        float acceleration = (float) Math.sqrt(x*x + y*y + z*z) - SensorManager.GRAVITY_EARTH;

        long currentTime = System.currentTimeMillis();

        // Check if we're in confirmation mode and if it has timed out
        if (confirmationPending) {
            if (currentTime - confirmationStartTime > CONFIRMATION_TIMEOUT_MS) {
                // Confirmation timed out, reset
                cancelShakeAlert();
            }
            return; // Don't process shakes while waiting for confirmation
        }

        // Check if acceleration exceeds threshold (shake detected)
        if (acceleration > SHAKE_THRESHOLD) {
            // Check if enough time has passed since last shake
            if (lastShakeTime == 0 || currentTime - lastShakeTime > SHAKE_SLOP_TIME_MS) {
                // If this is the first shake in a sequence, record the time
                if (shakeCount == 0) {
                    firstShakeTime = currentTime;
                }

                // Increment shake count
                shakeCount++;
                lastShakeTime = currentTime;

                Log.d(TAG, "Shake detected! Count: " + shakeCount);

                // Provide haptic feedback for each detected shake
                provideHapticFeedback(100);

                // Check if we've reached the required number of shakes within the time window
                if (shakeCount >= REQUIRED_SHAKES) {
                    // Check if all shakes happened within the reset time window
                    if (currentTime - firstShakeTime <= SHAKE_COUNT_RESET_TIME_MS) {
                        // Show confirmation notification
                        showConfirmationNotification();
                    } else {
                        // Reset shake count if time window expired
                        shakeCount = 1;
                        firstShakeTime = currentTime;
                    }
                }
            }
        } else if (currentTime - lastShakeTime > SHAKE_COUNT_RESET_TIME_MS) {
            // Reset shake count if no shakes for a while
            shakeCount = 0;
            firstShakeTime = 0;
        }
    }

    private void showConfirmationNotification() {
        confirmationPending = true;
        confirmationStartTime = System.currentTimeMillis();

        // Provide strong haptic feedback for confirmation
        provideHapticFeedback(500);

        // Update notification with confirmation UI
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.notify(NOTIFICATION_ID, createConfirmationNotification());

        Log.d(TAG, "Showing SOS confirmation notification");
    }

    private void triggerSosAlert() {
        // Start SOS alert process
        Intent sosIntent = new Intent(this, SosAlertService.class);
        sosIntent.setAction("TRIGGER_SOS");
        sosIntent.putExtra("TRIGGER_METHOD", "shake_detection");
        startService(sosIntent);

        // Provide strong haptic feedback for SOS activation
        provideHapticFeedback(1000);

        Log.d(TAG, "SOS alert triggered by shake detection");

        // Reset shake detection variables
        shakeCount = 0;
        lastShakeTime = 0;
        firstShakeTime = 0;
    }

    private void provideHapticFeedback(long milliseconds) {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(milliseconds, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(milliseconds);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not used
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Shake Detection",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Used for shake gesture detection to trigger SOS alerts");

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification(boolean active) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        );

        String contentText = active
                ? "Shake detection active - shake device to trigger SOS"
                : "Shake your phone 3 times quickly to trigger SOS alert";

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SOS Shake Detection Active")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_shake)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }

    private Notification createConfirmationNotification() {
        Intent cancelIntent = new Intent(this, ShakeDetectionService.class);
        cancelIntent.setAction("CANCEL_ALERT");
        PendingIntent cancelPendingIntent = PendingIntent.getService(
                this, 1, cancelIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent confirmIntent = new Intent(this, ShakeDetectionService.class);
        confirmIntent.setAction("CONFIRM_SOS");
        PendingIntent confirmPendingIntent = PendingIntent.getService(
                this, 2, confirmIntent, PendingIntent.FLAG_IMMUTABLE);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SOS Alert Confirmation")
                .setContentText("Shake detected! Send SOS alert?")
                .setSmallIcon(R.drawable.ic_warning)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .addAction(R.drawable.ic_delete, "Cancel", cancelPendingIntent)
                .addAction(R.drawable.ic_sos, "Send SOS", confirmPendingIntent)
                .setAutoCancel(false)
                .setOngoing(true)
                .build();
    }
}

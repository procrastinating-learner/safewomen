package com.example.safewomen.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.safewomen.MainActivity;
import com.example.safewomen.R;

/**
 * Service for fall detection using accelerometer
 */
public class FallDetectionService extends Service implements SensorEventListener {
    private static final String TAG = "FallDetectionService";
    private static final String CHANNEL_ID = "fall_detection_channel";
    private static final int NOTIFICATION_ID = 1004;

    private SensorManager sensorManager;
    private Sensor accelerometer;
    private boolean isMonitoring = false;

    // Fall detection parameters
    private static final float FALL_THRESHOLD = 20.0f; // Threshold for detecting a fall (in m/s²)
    private static final long IMPACT_TIME = 1000; // Time window to detect impact (in ms)
    private static final long LYING_TIME = 2000; // Time to wait after impact to check if still lying (in ms)

    private float[] gravity = new float[3];
    private float[] linear_acceleration = new float[3];
    private long lastFallTimestamp = 0;
    private boolean possibleFallDetected = false;
    private boolean confirmationPending = false;

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
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case "START_MONITORING":
                        startFallDetection();
                        break;
                    case "STOP_MONITORING":
                        stopFallDetection();
                        break;
                    case "CANCEL_ALERT":
                        cancelFallAlert();
                        break;
                }
            } else {
                // Default action is to start monitoring
                startFallDetection();
            }
        }

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopFallDetection();
        super.onDestroy();
    }

    private void startFallDetection() {
        if (isMonitoring || sensorManager == null || accelerometer == null) return;

        // Start as foreground service with notification
        startForeground(NOTIFICATION_ID, createNotification());

        // Register sensor listener
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
        isMonitoring = true;

        Log.d(TAG, "Fall detection started");
    }

    private void stopFallDetection() {
        if (!isMonitoring || sensorManager == null) return;

        // Unregister sensor listener
        sensorManager.unregisterListener(this);
        isMonitoring = false;

        // Stop foreground service
        stopForeground(true);
        stopSelf();

        Log.d(TAG, "Fall detection stopped");
    }

    private void cancelFallAlert() {
        possibleFallDetected = false;
        confirmationPending = false;

        // Update notification to normal state
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.notify(NOTIFICATION_ID, createNotification());

        Log.d(TAG, "Fall alert canceled");
    }
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;

        // Apply low-pass filter to isolate gravity
        final float alpha = 0.8f;
        gravity[0] = alpha * gravity[0] + (1 - alpha) * event.values[0];
        gravity[1] = alpha * gravity[1] + (1 - alpha) * event.values[1];
        gravity[2] = alpha * gravity[2] + (1 - alpha) * event.values[2];

        // Remove gravity from accelerometer readings to get linear acceleration
        linear_acceleration[0] = event.values[0] - gravity[0];
        linear_acceleration[1] = event.values[1] - gravity[1];
        linear_acceleration[2] = event.values[2] - gravity[2];

        // Calculate magnitude of acceleration
        float accelerationMagnitude = (float) Math.sqrt(
                linear_acceleration[0] * linear_acceleration[0] +
                        linear_acceleration[1] * linear_acceleration[1] +
                        linear_acceleration[2] * linear_acceleration[2]
        );

        long currentTime = System.currentTimeMillis();

        // Check for potential fall (sudden acceleration spike)
        if (accelerationMagnitude > FALL_THRESHOLD && !possibleFallDetected) {
            possibleFallDetected = true;
            lastFallTimestamp = currentTime;

            Log.d(TAG, "Possible fall detected! Acceleration: " + accelerationMagnitude);

            // Schedule a check after LYING_TIME to see if the person is still lying down
            new android.os.Handler().postDelayed(this::checkIfStillLying, LYING_TIME);
        }
    }

    private void checkIfStillLying() {
        if (!possibleFallDetected) return;

        // If we're still in possible fall state after LYING_TIME, consider it a confirmed fall
        if (!confirmationPending) {
            confirmationPending = true;

            // Vibrate to alert the user
            vibrate();

            // Show confirmation notification
            showFallConfirmationNotification();

            // Schedule automatic SOS if no response
            new android.os.Handler().postDelayed(this::triggerAutomaticSos, 30000); // 30 seconds

            Log.d(TAG, "Fall confirmed! Waiting for user response or timeout");
        }
    }

    private void vibrate() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(
                    new long[]{0, 1000, 500, 1000, 500, 1000}, -1));
        } else {
            vibrator.vibrate(new long[]{0, 1000, 500, 1000, 500, 1000}, -1);
        }
    }

    private void showFallConfirmationNotification() {
        Intent cancelIntent = new Intent(this, FallDetectionService.class);
        cancelIntent.setAction("CANCEL_ALERT");
        PendingIntent cancelPendingIntent = PendingIntent.getService(
                this, 1, cancelIntent, PendingIntent.FLAG_IMMUTABLE);

        Intent sosIntent = new Intent(this, SosAlertService.class);
        sosIntent.setAction("TRIGGER_SOS");
        sosIntent.putExtra("TRIGGER_METHOD", "fall_detection");
        PendingIntent sosPendingIntent = PendingIntent.getService(
                this, 2, sosIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Fall Detected!")
                .setContentText("Are you okay? Tap 'I'm OK' or SOS will be triggered in 30 seconds")
                .setSmallIcon(R.drawable.ic_fall)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .addAction(R.drawable.ic_delete, "I'm OK", cancelPendingIntent)
                .addAction(R.drawable.ic_sos, "Send SOS Now", sosPendingIntent)
                .setAutoCancel(false)
                .setOngoing(true);

        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.notify(NOTIFICATION_ID, builder.build());
    }

    private void triggerAutomaticSos() {
        if (confirmationPending) {
            // User didn't respond, trigger SOS
            Intent sosIntent = new Intent(this, SosAlertService.class);
            sosIntent.setAction("TRIGGER_SOS");
            sosIntent.putExtra("TRIGGER_METHOD", "fall_detection_automatic");
            startService(sosIntent);

            // Reset states
            possibleFallDetected = false;
            confirmationPending = false;

            Log.d(TAG, "Automatic SOS triggered after fall detection");
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
                    "Fall Detection",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Used for fall detection monitoring");

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
                .setContentTitle("Fall Detection Active")
                .setContentText("Monitoring for potential falls")
                .setSmallIcon(R.drawable.ic_fall)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
}

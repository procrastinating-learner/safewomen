package com.example.safewomen.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.telephony.SmsManager;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import android.Manifest;
import com.example.safewomen.models.entities.EmergencyContactEntity;
import com.example.safewomen.models.entities.LocationHistoryEntity;
import com.example.safewomen.MainActivity;
import com.example.safewomen.R;
import com.example.safewomen.repositories.AlertRepository;
import com.example.safewomen.repositories.ContactRepository;
import com.example.safewomen.repositories.LocationHistoryRepository;

import java.util.ArrayList;

/**
 * Service for handling SOS alerts
 */
public class SosAlertService extends Service {
    private static final String TAG = "SosAlertService";
    private static final String CHANNEL_ID = "sos_alert_channel";
    private static final int NOTIFICATION_ID = 1002;


    private ContactRepository contactRepository;
    private LocationHistoryRepository locationRepository;
    private AlertRepository alertRepository;

    private String alertId;
    private String triggerMethodGlobal;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        contactRepository = ContactRepository.getInstance();
        locationRepository = LocationHistoryRepository.getInstance();
        alertRepository = AlertRepository.getInstance();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if ("TRIGGER_SOS".equals(action)) {
                String triggerMethod = intent.getStringExtra("TRIGGER_METHOD");
                triggerSosAlert(triggerMethod);
            } else if ("CANCEL_SOS".equals(action)) {
                cancelSosAlert();
            }
        }

        return START_NOT_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void triggerSosAlert(String triggerMethod) {
        // Start as foreground service with notification
        triggerMethodGlobal = triggerMethod;
        startForeground(NOTIFICATION_ID, createNotification());

        // TODO: Implement SOS alert logic
        // 1. Send SMS to emergency contacts
        // 2. Make emergency call
        // 3. Record audio/video
        // 4. Share location
        shareLocationAndCreateAlert();

        Log.d(TAG, "SOS Alert triggered by: " + triggerMethod);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "SOS Alerts",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Used for emergency SOS alerts");

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        );

        // Create a cancel action
        Intent cancelIntent = new Intent(this, SosAlertService.class);
        cancelIntent.setAction("CANCEL_SOS");
        PendingIntent cancelPendingIntent = PendingIntent.getService(
                this, 1, cancelIntent, PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("SOS Alert Active")
                .setContentText("Emergency contacts are being notified")
                .setSmallIcon(R.drawable.ic_sos)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .addAction(R.drawable.ic_delete, "Cancel SOS", cancelPendingIntent)
                .setOngoing(true)
                .build();
    }

    private void shareLocationAndCreateAlert() {
        // Get the most recent location
        locationRepository.getMostRecentLocation(new LocationHistoryRepository.LocationHistoryCallback() {
            @Override
            public void onLocationLoaded(LocationHistoryEntity location) {
                if (location != null) {
                    // Create an alert with the location
                    alertRepository.createAlert(
                            location.getLatitude(),
                            location.getLongitude(),
                            location.getAddress(),
                            triggerMethodGlobal, // Use the trigger method as alert type
                            new AlertRepository.AlertCallback() {
                                @Override
                                public void onSuccess(String alertId, String message) {
                                    // Store the alert ID for future reference
                                    SosAlertService.this.alertId = alertId;
                                    Log.d(TAG, "Alert created with ID: " + alertId);

                                    // Now that we have the alert and location, proceed with other steps
                                    sendSmsToEmergencyContacts(location);
                                    makeEmergencyCall();
                                    startEmergencyRecording();
                                }

                                @Override
                                public void onError(String errorMessage) {
                                    Log.e(TAG, "Error creating alert: " + errorMessage);

                                    // Even if alert creation fails, proceed with other emergency actions
                                    // Get location again to ensure we have it for SMS
                                    locationRepository.getMostRecentLocation(new LocationHistoryRepository.LocationHistoryCallback() {
                                        @Override
                                        public void onLocationLoaded(LocationHistoryEntity location) {
                                            sendSmsToEmergencyContacts(location);
                                            makeEmergencyCall();
                                            startEmergencyRecording();
                                        }

                                        @Override
                                        public void onError(String message) {
                                            // If we can't get location, still proceed with other actions
                                            sendSmsToEmergencyContacts(null);
                                            makeEmergencyCall();
                                            startEmergencyRecording();
                                        }
                                    });
                                }
                            }
                    );
                } else {
                    Log.w(TAG, "No location available for alert");
                    // Even without location, proceed with other emergency actions
                    sendSmsToEmergencyContacts(null);
                    makeEmergencyCall();
                    startEmergencyRecording();
                }
            }

            @Override
            public void onError(String message) {
                Log.e(TAG, "Error getting location: " + message);
                // Even without location, proceed with other emergency actions
                sendSmsToEmergencyContacts(null);
                makeEmergencyCall();
                startEmergencyRecording();
            }
        });
    }
    private void sendSmsToEmergencyContacts(LocationHistoryEntity location) {
        // Get all emergency contacts
        contactRepository.getContacts().observeForever(contacts -> {
            if (contacts != null && !contacts.isEmpty()) {
                // Prepare the emergency message
                String message = "EMERGENCY: I need help!";

                // Add location information if available
                if (location != null) {
                    message += " My current location: ";
                    if (location.getAddress() != null && !location.getAddress().isEmpty()) {
                        message += location.getAddress();
                    } else {
                        message += "Lat: " + location.getLatitude() + ", Long: " + location.getLongitude();
                    }

                    // Add Google Maps link
                    message += " https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
                }

                // Add alert ID if available
                if (alertId != null) {
                    message += " Alert ID: " + alertId;
                }

                // Send SMS to each contact
                for (EmergencyContactEntity contact : contacts) {
                    sendSms(contact.getPhone(), message);
                    Log.d(TAG, "Emergency SMS sent to: " + contact.getName() + " (" + contact.getPhone() + ")");
                }
            } else {
                Log.w(TAG, "No emergency contacts found to send SMS");
            }
        });
    }

    private void sendSms(String phoneNumber, String message) {
        try {
            // Check for SMS permission
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                Log.e(TAG, "SMS permission not granted");
                return;
            }

            SmsManager smsManager = SmsManager.getDefault();

            // If message is too long, divide it
            if (message.length() > 160) {
                ArrayList<String> parts = smsManager.divideMessage(message);
                smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null);
            } else {
                smsManager.sendTextMessage(phoneNumber, null, message, null, null);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error sending SMS: " + e.getMessage(), e);
        }
    }
    private void makeEmergencyCall() {
        // Get emergency contacts to find primary contact
        contactRepository.getContacts().observeForever(contacts -> {
            if (contacts != null && !contacts.isEmpty()) {
                // Find primary contact
                EmergencyContactEntity primaryContact = null;
                for (EmergencyContactEntity contact : contacts) {
                    if (contact.isPrimary()) {
                        primaryContact = contact;
                        break;
                    }
                }

                // If no primary contact, use the first one
                if (primaryContact == null) {
                    primaryContact = contacts.get(0);
                }

                // Make the emergency call
                String phoneNumber = primaryContact.getPhone();
                try {
                    // Check for call phone permission
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
                        Log.e(TAG, "Call phone permission not granted");
                        return;
                    }

                    Intent callIntent = new Intent(Intent.ACTION_CALL);
                    callIntent.setData(Uri.parse("tel:" + phoneNumber));
                    callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(callIntent);

                    Log.d(TAG, "Emergency call initiated to: " + primaryContact.getName() + " (" + phoneNumber + ")");
                } catch (Exception e) {
                    Log.e(TAG, "Error making emergency call: " + e.getMessage(), e);
                }
            } else {
                Log.w(TAG, "No emergency contacts found to make call");
            }
        });
    }
    private void startEmergencyRecording() {
        try {
            // Start audio recording
            Intent audioIntent = new Intent(this, EmergencyRecordingService.class);
            audioIntent.setAction("START_AUDIO_RECORDING");
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(audioIntent);
            } else {
                startService(audioIntent);
            }

            Log.d(TAG, "Emergency audio recording started");

            // Optionally start video recording after a short delay
            new android.os.Handler().postDelayed(() -> {
                try {
                    Intent videoIntent = new Intent(this, EmergencyRecordingService.class);
                    videoIntent.setAction("START_VIDEO_RECORDING");
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        startForegroundService(videoIntent);
                    } else {
                        startService(videoIntent);
                    }
                    Log.d(TAG, "Emergency video recording started");
                } catch (Exception e) {
                    Log.e(TAG, "Error starting video recording: " + e.getMessage(), e);
                }
            }, 5000); // 5 second delay before starting video
        } catch (Exception e) {
            Log.e(TAG, "Error starting emergency recording: " + e.getMessage(), e);
        }
    }




    private void cancelSosAlert() {
        // Update alert status if we have an alert ID
        if (alertId != null) {
            alertRepository.updateAlertStatus(alertId, "cancelled", new AlertRepository.AlertCallback() {
                @Override
                public void onSuccess(String alertId, String message) {
                    Log.d(TAG, "Alert cancelled: " + alertId);
                }

                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "Error cancelling alert: " + errorMessage);
                }
            });
        }

        // Stop emergency recording
        Intent stopRecordingIntent = new Intent(this, EmergencyRecordingService.class);
        stopRecordingIntent.setAction("STOP_RECORDING");
        startService(stopRecordingIntent);

        // Stop this service
        stopForeground(true);
        stopSelf();

        Log.d(TAG, "SOS Alert cancelled");
    }

}

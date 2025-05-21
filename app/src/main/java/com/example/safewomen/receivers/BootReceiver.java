package com.example.safewomen.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import com.example.safewomen.services.ShakeDetectionService;
import com.example.safewomen.utils.PreferenceManager;

/**
 * Receiver to start services when device boots
 */
public class BootReceiver extends BroadcastReceiver {
    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Boot completed, starting services");

            // Initialize PreferenceManager
            PreferenceManager.init(context);

            // Check if shake detection was enabled before reboot
            boolean shakeDetectionEnabled = PreferenceManager.getInstance().getBoolean("shake_detection_enabled", false);

            if (shakeDetectionEnabled) {
                // Start shake detection service
                Intent serviceIntent = new Intent(context, ShakeDetectionService.class);
                serviceIntent.setAction("START_MONITORING");

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(serviceIntent);
                } else {
                    context.startService(serviceIntent);
                }

                Log.d(TAG, "Shake detection service started after boot");
            }
        }
    }
}

package com.example.safewomen.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.app.Service;

/**
 * Utility class for service-related operations
 */
public class ServiceUtils {

    /**
     * Check if a specific service is currently running
     *
     * @param context The application context
     * @param serviceClass The class of the service to check
     * @return true if the service is running, false otherwise
     */
    public static boolean isServiceRunning(Context context, Class<? extends Service> serviceClass) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);

        if (activityManager != null) {
            for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Start a service if it's not already running
     *
     * @param context The application context
     * @param serviceClass The class of the service to start
     * @param action The action to include in the intent (optional)
     */
    public static void startServiceIfNotRunning(Context context, Class<? extends Service> serviceClass, String action) {
        if (!isServiceRunning(context, serviceClass)) {
            android.content.Intent intent = new android.content.Intent(context, serviceClass);
            if (action != null && !action.isEmpty()) {
                intent.setAction(action);
            }

            // Use startForegroundService for Android O and above when needed
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent);
            } else {
                context.startService(intent);
            }
        }
    }

    /**
     * Stop a service if it's running
     *
     * @param context The application context
     * @param serviceClass The class of the service to stop
     */
    public static void stopServiceIfRunning(Context context, Class<? extends Service> serviceClass) {
        if (isServiceRunning(context, serviceClass)) {
            context.stopService(new android.content.Intent(context, serviceClass));
        }
    }
}

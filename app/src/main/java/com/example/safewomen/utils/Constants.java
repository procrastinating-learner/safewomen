package com.example.safewomen.utils;

/**
 * Class to store application-wide constants
 */
public class Constants {
    // API Keys
    public static final String MAPS_API_KEY = "AIzaSyCvOJhLdKIqB0aablZ73If3HANvUo1qRkM";

    // Notification channel IDs
    public static final String LOCATION_CHANNEL_ID = "location_tracking_channel";
    public static final String SOS_CHANNEL_ID = "sos_alert_channel";
    public static final String RECORDING_CHANNEL_ID = "emergency_recording_channel";
    public static final String VOICE_COMMAND_CHANNEL_ID = "voice_command_channel";
    public static final String FALL_DETECTION_CHANNEL_ID = "fall_detection_channel";

    // Notification IDs
    public static final int LOCATION_NOTIFICATION_ID = 1001;
    public static final int SOS_NOTIFICATION_ID = 1002;
    public static final int VOICE_COMMAND_NOTIFICATION_ID = 1003;
    public static final int FALL_DETECTION_NOTIFICATION_ID = 1004;
    public static final int RECORDING_NOTIFICATION_ID = 1005;

    // Preferences
    public static final String PREF_EMERGENCY_CONTACTS = "emergency_contacts";
    public static final String PREF_USER_INFO = "user_info";
    public static final String PREF_SAFETY_SETTINGS = "safety_settings";

    // Intent actions
    public static final String ACTION_START_LOCATION_TRACKING = "START_TRACKING";
    public static final String ACTION_STOP_LOCATION_TRACKING = "STOP_TRACKING";
    public static final String ACTION_TRIGGER_SOS = "TRIGGER_SOS";
    public static final String ACTION_CANCEL_SOS = "CANCEL_SOS";
}

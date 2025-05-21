package com.example.safewomen.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Utility class to manage SharedPreferences using singleton pattern
 */
public class PreferenceManager {
    private static final String PREF_FILE = "safe_women_prefs";
    private static final String KEY_AUTH_TOKEN = "auth_token";
    private static final String KEY_USER_ID = "user_id";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_PHONE = "user_phone";
    private static final String KEY_LOGGED_IN = "logged_in";

    private static PreferenceManager instance;
    private final SharedPreferences sharedPreferences;

    // Private constructor for singleton pattern
    private PreferenceManager(Context context) {
        this.sharedPreferences = context.getApplicationContext().getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
    }

    /**
     * Initialize the PreferenceManager singleton with context
     * @param context Application context
     */
    public static void init(Context context) {
        if (instance == null) {
            instance = new PreferenceManager(context);
        }
    }

    /**
     * Get singleton instance (must be initialized first with context)
     * @return PreferenceManager instance
     */
    public static PreferenceManager getInstance() {
        return instance;
    }

    /**
     * Check if user is logged in
     * @return true if user is logged in, false otherwise
     */
    public boolean isLoggedIn() {
        return getBoolean(KEY_LOGGED_IN, false);
    }

    /**
     * Set logged in status
     * @param loggedIn true if user is logged in, false otherwise
     */
    public void setLoggedIn(boolean loggedIn) {
        saveBoolean(KEY_LOGGED_IN, loggedIn);
    }

    /**
     * Set user ID
     * @param userId User ID to save
     */
    public void setUserId(String userId) {
        saveString(KEY_USER_ID, userId);
    }

    /**
     * Set authentication token
     * @param token Authentication token to save
     */
    public void setAuthToken(String token) {
        saveString(KEY_AUTH_TOKEN, token);
    }

    /**
     * Save authentication token (alias for setAuthToken for backward compatibility)
     * @param token Authentication token to save
     */
    public void saveAuthToken(String token) {
        setAuthToken(token);
    }

    public void saveBoolean(String key, boolean value) {
        sharedPreferences.edit().putBoolean(key, value).apply();
    }

    public boolean getBoolean(String key, boolean defaultValue) {
        return sharedPreferences.getBoolean(key, defaultValue);
    }

    public void saveString(String key, String value) {
        sharedPreferences.edit().putString(key, value).apply();
    }

    public String getString(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }

    public String getAuthToken() {
        return getString(KEY_AUTH_TOKEN, null);
    }

    public void clearAuthToken() {
        sharedPreferences.edit().remove(KEY_AUTH_TOKEN).apply();
    }

    public void saveUserProfile(String userId, String name, String email, String phone) {
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(KEY_USER_ID, userId);
        editor.putString(KEY_USER_NAME, name);
        editor.putString(KEY_USER_EMAIL, email);
        editor.putString(KEY_USER_PHONE, phone);
        editor.apply();
    }

    public String getUserId() {
        return getString(KEY_USER_ID, "");
    }

    public String getUserName() {
        return getString(KEY_USER_NAME, "");
    }

    public String getUserEmail() {
        return getString(KEY_USER_EMAIL, "");
    }

    public String getUserPhone() {
        return getString(KEY_USER_PHONE, "");
    }

    public void clearUserData() {
        sharedPreferences.edit().clear().apply();
    }
}

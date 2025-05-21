package com.example.safewomen.repositories;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.example.safewomen.api.ApiClient;
import com.example.safewomen.api.ApiService;
import com.example.safewomen.data.AuthDao;
import com.example.safewomen.data.SafeWomenDatabase;
import com.example.safewomen.data.SettingsDao;
import com.example.safewomen.models.entities.UserEntity;
import com.example.safewomen.models.entities.UserSettingsEntity;
import com.example.safewomen.utils.NetworkUtil;
import com.example.safewomen.utils.PreferenceManager;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepository {
    private static final String TAG = "AuthRepository";
    private static AuthRepository instance;
    private static Application appContext;

    private final AuthDao authDao;
    private final SettingsDao settingsDao;
    private final ApiService apiService;
    private final Executor executor;

    public static synchronized void init(Application application) {
        if (instance == null) {
            appContext = application;
            instance = new AuthRepository();
        }
    }

    public static synchronized AuthRepository getInstance() {
        if (instance == null) {
            throw new IllegalStateException("AuthRepository must be initialized with init() before use");
        }
        return instance;
    }

    private AuthRepository() {
        SafeWomenDatabase db = SafeWomenDatabase.getInstance(appContext);
        authDao = db.authDao();
        settingsDao = db.settingsDao();
        apiService = ApiClient.getClient().create(ApiService.class);
        executor = Executors.newSingleThreadExecutor();
    }

    public LiveData<UserEntity> getCurrentUser() {
        return authDao.observeCurrentUser();
    }

    public void login(String email, String password, AuthCallback callback) {
        // Create parameters for PHP
        Map<String, String> params = new HashMap<>();
        params.put("email", email);
        params.put("password", password);

        apiService.login(params).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseString = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseString);

                        if (jsonResponse.optBoolean("success", false)) {
                            // Parse user data
                            JSONObject userData = jsonResponse.getJSONObject("user");
                            String userId = userData.getString("id");
                            String name = userData.getString("name");
                            String userEmail = userData.getString("email");
                            String phone = userData.optString("phone", "");
                            String authToken = userData.getString("auth_token");

                            // Create user entity
                            UserEntity user = new UserEntity(
                                    userId,
                                    name,
                                    userEmail,
                                    phone,
                                    authToken,
                                    System.currentTimeMillis()
                            );

                            // Save user locally
                            executor.execute(() -> {
                                authDao.saveUser(user);

                                // Create default settings if not exist
                                UserSettingsEntity settings = settingsDao.getSettingsForUser(userId);
                                if (settings == null) {
                                    settings = new UserSettingsEntity(
                                            userId,
                                            true, // notifications enabled by default
                                            "button", // default SOS trigger method
                                            true, // auto location sharing enabled
                                            15, // location update frequency in minutes
                                            false, // dark mode disabled by default
                                            "I need help! This is an emergency." // default message
                                    );
                                    settingsDao.saveSettings(settings);
                                }
                            });

                            // Save to preferences
                            PreferenceManager.getInstance().setLoggedIn(true);
                            PreferenceManager.getInstance().setUserId(userId);
                            PreferenceManager.getInstance().setAuthToken(authToken);

                            if (callback != null) {
                                callback.onSuccess(user);
                            }
                        } else {
                            // Handle login error
                            String message = jsonResponse.optString("message", "Login failed");
                            if (callback != null) {
                                callback.onError(message);
                            }
                        }
                    } else {
                        // Handle HTTP error
                        if (callback != null) {
                            callback.onError("Server error: " + response.code());
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing login response", e);
                    if (callback != null) {
                        callback.onError("Error processing response: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Login network error", t);
                if (callback != null) {
                    callback.onError("Network error: " + t.getMessage());
                }
            }
        });
    }
    public void register(String name, String email, String phone, String password, AuthCallback callback) {
        // Create parameters for PHP
        Map<String, String> params = new HashMap<>();
        params.put("name", name);
        params.put("email", email);
        params.put("phone", phone);
        params.put("password", password);

        apiService.register(params).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseString = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseString);

                        if (jsonResponse.optBoolean("success", false)) {
                            // Parse user data
                            JSONObject userData = jsonResponse.getJSONObject("user");
                            String userId = userData.getString("id");
                            String userName = userData.getString("name");
                            String userEmail = userData.getString("email");
                            String userPhone = userData.optString("phone", "");
                            String authToken = userData.getString("auth_token");

                            // Create user entity
                            UserEntity user = new UserEntity(
                                    userId,
                                    userName,
                                    userEmail,
                                    userPhone,
                                    authToken,
                                    System.currentTimeMillis()
                            );

                            // Save user locally
                            executor.execute(() -> {
                                authDao.saveUser(user);

                                // Create default settings
                                UserSettingsEntity settings = new UserSettingsEntity(
                                        userId,
                                        true, // notifications enabled by default
                                        "button", // default SOS trigger method
                                        true, // auto location sharing enabled
                                        15, // location update frequency in minutes
                                        false, // dark mode disabled by default
                                        "I need help! This is an emergency." // default message
                                );
                                settingsDao.saveSettings(settings);
                            });

                            // Save to preferences
                            PreferenceManager.getInstance().setLoggedIn(true);
                            PreferenceManager.getInstance().setUserId(userId);
                            PreferenceManager.getInstance().setAuthToken(authToken);

                            if (callback != null) {
                                callback.onSuccess(user);
                            }
                        } else {
                            // Handle registration error
                            String message = jsonResponse.optString("message", "Registration failed");
                            if (callback != null) {
                                callback.onError(message);
                            }
                        }
                    } else {
                        // Handle HTTP error
                        if (callback != null) {
                            callback.onError("Server error: " + response.code());
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing registration response", e);
                    if (callback != null) {
                        callback.onError("Error processing response: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Registration network error", t);
                if (callback != null) {
                    callback.onError("Network error: " + t.getMessage());
                }
            }
        });
    }

    public void logout() {
        String userId = PreferenceManager.getInstance().getUserId();
        String authToken = PreferenceManager.getInstance().getAuthToken();

        // Only attempt server logout if we have network
        if (NetworkUtil.isOnline(appContext) && userId != null && authToken != null) {
            Map<String, String> params = new HashMap<>();
            params.put("user_id", userId);

            apiService.logout(params).enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    // Regardless of server response, clear local data
                    clearLocalUserData();
                }

                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                    // Even if server logout fails, clear local data
                    clearLocalUserData();
                }
            });
        } else {
            // No network or no credentials, just clear local data
            clearLocalUserData();
        }
    }

    private void clearLocalUserData() {
        // Clear preferences
        PreferenceManager.getInstance().setLoggedIn(false);
        PreferenceManager.getInstance().setUserId(null);
        PreferenceManager.getInstance().setAuthToken(null);

        // Clear database
        executor.execute(() -> {
            authDao.clearUserData();
            settingsDao.clearSettings();
        });
    }

    public void updateProfile(String name, String email, String phone, AuthCallback callback) {
        String userId = PreferenceManager.getInstance().getUserId();
        if (userId == null) {
            if (callback != null) {
                callback.onError("User not logged in");
            }
            return;
        }

        Map<String, String> params = new HashMap<>();
        params.put("user_id", userId);
        params.put("name", name);
        params.put("email", email);
        params.put("phone", phone);

        apiService.updateProfile(params).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseString = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseString);

                        if (jsonResponse.optBoolean("success", false)) {
                            // Update local user data
                            executor.execute(() -> {
                                UserEntity user = authDao.getUserById(userId);
                                if (user != null) {
                                    user.setName(name);
                                    user.setEmail(email);
                                    user.setPhone(phone);
                                    authDao.updateUser(user);
                                }
                            });

                            if (callback != null) {
                                callback.onSuccess(null);
                            }
                        } else {
                            String message = jsonResponse.optString("message", "Failed to update profile");
                            if (callback != null) {
                                callback.onError(message);
                            }
                        }
                    } else {
                        if (callback != null) {
                            callback.onError("Server error: " + response.code());
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing update profile response", e);
                    if (callback != null) {
                        callback.onError("Error processing response: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Update profile network error", t);
                if (callback != null) {
                    callback.onError("Network error: " + t.getMessage());
                }
            }
        });
    }
    public void requestPasswordReset(String email, AuthCallback callback) {
        // Create parameters for PHP
        Map<String, String> params = new HashMap<>();
        params.put("email", email);

        apiService.requestPasswordReset(params).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseString = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseString);

                        if (jsonResponse.optBoolean("success", false)) {
                            // Password reset request successful
                            String message = jsonResponse.optString("message", "Reset code sent to your email");
                            if (callback != null) {
                                callback.onSuccess(null); // No user entity for this operation
                            }
                        } else {
                            // Handle error
                            String message = jsonResponse.optString("message", "Failed to send reset code");
                            if (callback != null) {
                                callback.onError(message);
                            }
                        }
                    } else {
                        // Handle HTTP error
                        if (callback != null) {
                            callback.onError("Server error: " + response.code());
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing password reset request response", e);
                    if (callback != null) {
                        callback.onError("Error processing response: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Password reset request network error", t);
                if (callback != null) {
                    callback.onError("Network error: " + t.getMessage());
                }
            }
        });
    }
    public void resetPassword(String email, String resetCode, String newPassword, AuthCallback callback) {
        // Create parameters for PHP
        Map<String, String> params = new HashMap<>();
        params.put("email", email);
        params.put("reset_code", resetCode);
        params.put("new_password", newPassword);

        apiService.resetPassword(params).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseString = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseString);

                        if (jsonResponse.optBoolean("success", false)) {
                            // Password reset successful
                            String message = jsonResponse.optString("message", "Password reset successful");

                            // If the API returns user data after reset, we can update it
                            if (jsonResponse.has("user")) {
                                JSONObject userData = jsonResponse.getJSONObject("user");
                                String userId = userData.getString("id");
                                String name = userData.getString("name");
                                String userEmail = userData.getString("email");
                                String phone = userData.optString("phone", "");
                                String authToken = userData.getString("auth_token");

                                // Create user entity
                                UserEntity user = new UserEntity(
                                        userId,
                                        name,
                                        userEmail,
                                        phone,
                                        authToken,
                                        System.currentTimeMillis()
                                );

                                // Save user locally
                                executor.execute(() -> {
                                    authDao.saveUser(user);
                                });

                                // Save to preferences
                                PreferenceManager.getInstance().setLoggedIn(true);
                                PreferenceManager.getInstance().setUserId(userId);
                                PreferenceManager.getInstance().setAuthToken(authToken);

                                if (callback != null) {
                                    callback.onSuccess(user);
                                }
                            } else {
                                // No user data returned, just report success
                                if (callback != null) {
                                    callback.onSuccess(null);
                                }
                            }
                        } else {
                            // Handle reset error
                            String message = jsonResponse.optString("message", "Password reset failed");
                            if (callback != null) {
                                callback.onError(message);
                            }
                        }
                    } else {
                        // Handle HTTP error
                        if (callback != null) {
                            callback.onError("Server error: " + response.code());
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing password reset response", e);
                    if (callback != null) {
                        callback.onError("Error processing response: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Password reset network error", t);
                if (callback != null) {
                    callback.onError("Network error: " + t.getMessage());
                }
            }
        });
    }
    /**
     * Change the user's password
     * @param currentPassword User's current password for verification
     * @param newPassword New password to set
     * @param callback Callback to handle success or error
     */
    public void changePassword(String currentPassword, String newPassword, AuthCallback callback) {
        String userId = PreferenceManager.getInstance().getUserId();
        if (userId == null) {
            if (callback != null) {
                callback.onError("User not logged in");
            }
            return;
        }

        // Create parameters for PHP
        Map<String, String> params = new HashMap<>();
        params.put("user_id", userId);
        params.put("current_password", currentPassword);
        params.put("new_password", newPassword);

        apiService.changePassword(params).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseString = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseString);

                        if (jsonResponse.optBoolean("success", false)) {
                            // Password changed successfully
                            if (callback != null) {
                                callback.onSuccess(null);
                            }
                        } else {
                            // Handle error
                            String message = jsonResponse.optString("message", "Failed to change password");
                            if (callback != null) {
                                callback.onError(message);
                            }
                        }
                    } else {
                        // Handle HTTP error
                        if (callback != null) {
                            callback.onError("Server error: " + response.code());
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing change password response", e);
                    if (callback != null) {
                        callback.onError("Error processing response: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Change password network error", t);
                if (callback != null) {
                    callback.onError("Network error: " + t.getMessage());
                }
            }
        });
    }

    /**
     * Delete the user's account
     * @param password User's password for verification
     * @param callback Callback to handle success or error
     */
    public void deleteAccount(String password, AuthCallback callback) {
        String userId = PreferenceManager.getInstance().getUserId();
        if (userId == null) {
            if (callback != null) {
                callback.onError("User not logged in");
            }
            return;
        }

        // Create parameters for PHP
        Map<String, String> params = new HashMap<>();
        params.put("user_id", userId);
        params.put("password", password);

        apiService.deleteAccount(params).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseString = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseString);

                        if (jsonResponse.optBoolean("success", false)) {
                            // Account deleted successfully, clear local data
                            clearLocalUserData();

                            if (callback != null) {
                                callback.onSuccess(null);
                            }
                        } else {
                            // Handle error
                            String message = jsonResponse.optString("message", "Failed to delete account");
                            if (callback != null) {
                                callback.onError(message);
                            }
                        }
                    } else {
                        // Handle HTTP error
                        if (callback != null) {
                            callback.onError("Server error: " + response.code());
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing delete account response", e);
                    if (callback != null) {
                        callback.onError("Error processing response: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Delete account network error", t);
                if (callback != null) {
                    callback.onError("Network error: " + t.getMessage());
                }
            }
        });
    }


    public interface AuthCallback {
        void onSuccess(UserEntity user);
        void onError(String message);
    }
}

package com.example.safewomen.repositories;

import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.safewomen.models.User;
import com.example.safewomen.api.ApiClient;
import com.example.safewomen.api.ApiService;
import com.example.safewomen.utils.PreferenceManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UserRepository {
    private static final String TAG = "UserRepository";
    private static UserRepository instance;
    private MutableLiveData<User> currentUser = new MutableLiveData<>();
    private ApiService apiService;
    private PreferenceManager preferenceManager;

    private UserRepository() {
        // Initialize API service
        apiService = ApiClient.getClient().create(ApiService.class);
        preferenceManager = PreferenceManager.getInstance();

        // Check if user is already logged in
        String token = preferenceManager.getAuthToken();
        if (token != null && !token.isEmpty()) {
            fetchUserProfile();
        }
    }

    public static synchronized UserRepository getInstance() {
        if (instance == null) {
            instance = new UserRepository();
        }
        return instance;
    }

    public LiveData<User> getCurrentUser() {
        return currentUser;
    }

    public void login(String email, String password, LoginCallback callback) {
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

                        if (jsonResponse.has("success") && jsonResponse.getBoolean("success")) {
                            // Extract token
                            String token = jsonResponse.getString("token");
                            preferenceManager.saveAuthToken(token);

                            // Extract user data
                            JSONObject userData = jsonResponse.getJSONObject("user");
                            User user = new User(
                                    userData.getString("id"),
                                    userData.getString("name"),
                                    userData.getString("email"),
                                    userData.getString("phone")
                            );

                            currentUser.setValue(user);
                            callback.onSuccess();
                        } else {
                            // Handle error from PHP
                            String message = jsonResponse.has("message") ?
                                    jsonResponse.getString("message") : "Login failed";
                            callback.onError(message);
                        }
                    } else {
                        // Handle HTTP error
                        callback.onError("Login failed: " + response.code());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing login response", e);
                    callback.onError("Error processing response: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Login network error", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void register(User user, String password, RegisterCallback callback) {
        // Create parameters for PHP
        Map<String, String> params = new HashMap<>();
        params.put("name", user.getName());
        params.put("email", user.getEmail());
        params.put("phone", user.getPhone());
        params.put("password", password);

        apiService.register(params).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseString = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseString);

                        if (jsonResponse.has("success") && jsonResponse.getBoolean("success")) {
                            // Extract token
                            String token = jsonResponse.getString("token");
                            preferenceManager.saveAuthToken(token);

                            // Extract user data
                            JSONObject userData = jsonResponse.getJSONObject("user");
                            User newUser = new User(
                                    userData.getString("id"),
                                    userData.getString("name"),
                                    userData.getString("email"),
                                    userData.getString("phone")
                            );

                            currentUser.setValue(newUser);
                            callback.onSuccess();
                        } else {
                            // Handle error from PHP
                            String message = jsonResponse.has("message") ?
                                    jsonResponse.getString("message") : "Registration failed";
                            callback.onError(message);
                        }
                    } else {
                        // Handle HTTP error
                        callback.onError("Registration failed: " + response.code());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing registration response", e);
                    callback.onError("Error processing response: " + e.getMessage());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Registration network error", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    public void logout() {
        // Clear auth token
        preferenceManager.clearAuthToken();

        // Clear current user
        currentUser.setValue(null);
    }

    private void fetchUserProfile() {
        // Get the user ID from preferences
        String userId = preferenceManager.getUserId();

        // If no user ID is available, consider the user logged out
        if (userId == null || userId.isEmpty()) {
            logout();
            return;
        }

        apiService.getUserProfile(userId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseString = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseString);
                        if (jsonResponse.has("success") && jsonResponse.getBoolean("success")) {
                            // Extract user data
                            JSONObject userData = jsonResponse.getJSONObject("user");
                            User user = new User(
                                    userData.getString("id"),
                                    userData.getString("name"),
                                    userData.getString("email"),
                                    userData.getString("phone")
                            );
                            currentUser.setValue(user);
                        } else {
                            // If we can't get the profile, consider the user logged out
                            logout();
                        }
                    } else {
                        // If we can't get the profile, consider the user logged out
                        logout();
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing profile response", e);
                    // Don't logout automatically on parse error
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Profile fetch network error", t);
                // Network error, but don't log out automatically
                // The user might be offline
            }
        });
    }


    public interface LoginCallback {
        void onSuccess();
        void onError(String message);
    }

    public interface RegisterCallback {
        void onSuccess();
        void onError(String message);
    }
}

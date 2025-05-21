package com.example.safewomen.repositories;

import android.content.Context;
import android.util.Log;

import com.example.safewomen.api.ApiClient;
import com.example.safewomen.api.ApiService;
import com.example.safewomen.utils.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlertRepository {
    private static final String TAG = "AlertRepository";
    private static AlertRepository instance;
    private static Context appContext;

    private final ApiService apiService;
    private final PreferenceManager preferenceManager;

    public static synchronized void init(Context context) {
        if (instance == null) {
            appContext = context.getApplicationContext();
            instance = new AlertRepository();
        }
    }

    public static synchronized AlertRepository getInstance() {
        if (instance == null) {
            throw new IllegalStateException("AlertRepository must be initialized first");
        }
        return instance;
    }

    private AlertRepository() {
        this.apiService = ApiClient.getClient().create(ApiService.class);

        // Initialize PreferenceManager if needed, then get the instance
        if (PreferenceManager.getInstance() == null) {
            PreferenceManager.init(appContext);
        }
        this.preferenceManager = PreferenceManager.getInstance();
    }

    /**
     * Create a new alert
     */
    public void createAlert(double latitude, double longitude, String address, String type,
                            AlertCallback callback) {
        Map<String, String> params = new HashMap<>();
        params.put("user_id", preferenceManager.getUserId());
        params.put("latitude", String.valueOf(latitude));
        params.put("longitude", String.valueOf(longitude));
        params.put("address", address);
        params.put("type", type);
        params.put("status", "active");

        apiService.createAlert(params).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonString = response.body().string();
                        JSONObject jsonObject = new JSONObject(jsonString);
                        boolean success = jsonObject.getBoolean("success");
                        String message = jsonObject.getString("message");

                        if (success) {
                            JSONObject alertJson = jsonObject.getJSONObject("alert");
                            String alertId = alertJson.getString("id");
                            callback.onSuccess(alertId, message);
                        } else {
                            callback.onError(message);
                        }
                    } catch (IOException | JSONException e) {
                        Log.e(TAG, "Error parsing response", e);
                        callback.onError("Error processing response: " + e.getMessage());
                    }
                } else {
                    callback.onError("Server error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "API call failed", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    /**
     * Update alert status
     */
    public void updateAlertStatus(String alertId, String status, AlertCallback callback) {
        Map<String, String> params = new HashMap<>();
        params.put("alert_id", alertId);
        params.put("status", status);
        params.put("user_id", preferenceManager.getUserId());

        apiService.updateAlertStatus(params).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonString = response.body().string();
                        JSONObject jsonObject = new JSONObject(jsonString);
                        boolean success = jsonObject.getBoolean("success");
                        String message = jsonObject.getString("message");

                        if (success) {
                            callback.onSuccess(alertId, message);
                        } else {
                            callback.onError(message);
                        }
                    } catch (IOException | JSONException e) {
                        Log.e(TAG, "Error parsing response", e);
                        callback.onError("Error processing response: " + e.getMessage());
                    }
                } else {
                    callback.onError("Server error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "API call failed", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    /**
     * Get alert history
     */
    public void getAlertHistory(AlertHistoryCallback callback) {
        String userId = preferenceManager.getUserId();
        apiService.getAlertHistory(userId).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        String jsonString = response.body().string();
                        JSONObject jsonObject = new JSONObject(jsonString);
                        boolean success = jsonObject.getBoolean("success");

                        if (success) {
                            JSONArray alertsArray = jsonObject.getJSONArray("alerts");
                            List<Map<String, String>> alertsList = new ArrayList<>();

                            for (int i = 0; i < alertsArray.length(); i++) {
                                JSONObject alertJson = alertsArray.getJSONObject(i);
                                Map<String, String> alert = new HashMap<>();
                                alert.put("id", alertJson.getString("id"));
                                alert.put("type", alertJson.getString("type"));
                                alert.put("status", alertJson.getString("status"));
                                alert.put("latitude", alertJson.getString("latitude"));
                                alert.put("longitude", alertJson.getString("longitude"));
                                alert.put("address", alertJson.getString("address"));
                                alert.put("timestamp", alertJson.getString("timestamp"));
                                alertsList.add(alert);
                            }

                            callback.onSuccess(alertsList);
                        } else {
                            String message = jsonObject.getString("message");
                            callback.onError(message);
                        }
                    } catch (IOException | JSONException e) {
                        Log.e(TAG, "Error parsing response", e);
                        callback.onError("Error processing response: " + e.getMessage());
                    }
                } else {
                    callback.onError("Server error: " + response.code());
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "API call failed", t);
                callback.onError("Network error: " + t.getMessage());
            }
        });
    }

    /**
     * Callback interface for alert operations
     */
    public interface AlertCallback {
        void onSuccess(String alertId, String message);
        void onError(String errorMessage);
    }

    /**
     * Callback interface for alert history
     */
    public interface AlertHistoryCallback {
        void onSuccess(List<Map<String, String>> alerts);
        void onError(String errorMessage);
    }
}

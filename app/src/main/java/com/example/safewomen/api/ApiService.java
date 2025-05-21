package com.example.safewomen.api;

import java.util.Map;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FieldMap;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Query;
import retrofit2.http.QueryMap;

/**
 * Retrofit interface for API endpoints
 */
public interface ApiService {
    // Authentication endpoints
    @FormUrlEncoded
    @POST("login.php")
    Call<ResponseBody> login(@FieldMap Map<String, String> params);

    @FormUrlEncoded
    @POST("register.php")
    Call<ResponseBody> register(@FieldMap Map<String, String> params);

    @FormUrlEncoded
    @POST("logout.php")
    Call<ResponseBody> logout(@FieldMap Map<String, String> params);

    @FormUrlEncoded
    @POST("update_profile.php")
    Call<ResponseBody> updateProfile(@FieldMap Map<String, String> params);

    @FormUrlEncoded
    @POST("reset_password.php")
    Call<ResponseBody> resetPassword(@FieldMap Map<String, String> params);

    // Emergency contacts endpoints
    @GET("get_contacts.php")
    Call<ResponseBody> getContacts();

    @FormUrlEncoded
    @POST("add_contact.php")
    Call<ResponseBody> addContact(@FieldMap Map<String, String> params);

    @FormUrlEncoded
    @POST("update_contact.php")
    Call<ResponseBody> updateContact(@FieldMap Map<String, String> params);

    @FormUrlEncoded
    @POST("delete_contact.php")
    Call<ResponseBody> deleteContact(@FieldMap Map<String, String> params);

    // Alert endpoints
    @GET("get_alerts.php")
    Call<ResponseBody> getAlerts();

    @FormUrlEncoded
    @POST("send_alert.php")
    Call<ResponseBody> sendAlert(@FieldMap Map<String, String> params);

    @FormUrlEncoded
    @POST("update_alert.php")
    Call<ResponseBody> updateAlert(@FieldMap Map<String, String> params);

    @FormUrlEncoded
    @POST("cancel_alert.php")
    Call<ResponseBody> cancelAlert(@FieldMap Map<String, String> params);

    // Location endpoints
    @FormUrlEncoded
    @POST("update_location.php")
    Call<ResponseBody> updateLocation(@FieldMap Map<String, String> params);

    @GET("get_safe_zones.php")
    Call<ResponseBody> getSafeZones();

    // Settings endpoints
    @GET("get_settings.php")
    Call<ResponseBody> getSettings();

    @FormUrlEncoded
    @POST("update_settings.php")
    Call<ResponseBody> updateSettings(@FieldMap Map<String, String> params);

    // Notification endpoints
    @GET("get_notifications.php")
    Call<ResponseBody> getNotifications();

    @FormUrlEncoded
    @POST("mark_notification_read.php")
    Call<ResponseBody> markNotificationRead(@FieldMap Map<String, String> params);

    // Safety resources
    @GET("get_safety_resources.php")
    Call<ResponseBody> getSafetyResources();

    // Verification endpoints
    @FormUrlEncoded
    @POST("verify_phone.php")
    Call<ResponseBody> verifyPhone(@FieldMap Map<String, String> params);

    @FormUrlEncoded
    @POST("verify_email.php")
    Call<ResponseBody> verifyEmail(@FieldMap Map<String, String> params);

    // Additional methods that were missing
    @GET("get_user_profile.php")
    Call<ResponseBody> getUserProfile(@Query("user_id") String userId);

    @FormUrlEncoded
    @POST("create_alert.php")
    Call<ResponseBody> createAlert(@FieldMap Map<String, String> params);

    @FormUrlEncoded
    @POST("update_alert_status.php")
    Call<ResponseBody> updateAlertStatus(@FieldMap Map<String, String> params);
    @FormUrlEncoded
    @POST("request_password_reset.php")
    Call<ResponseBody> requestPasswordReset(@FieldMap Map<String, String> params);
    @GET("get_alert_history.php")
    Call<ResponseBody> getAlertHistory(@Query("user_id") String userId);
    @FormUrlEncoded
    @POST("change_password.php")
    Call<ResponseBody> changePassword(@FieldMap Map<String, String> params);

    @FormUrlEncoded
    @POST("delete_account.php")
    Call<ResponseBody> deleteAccount(@FieldMap Map<String, String> params);

}

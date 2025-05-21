package com.example.safewomen.repositories;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.work.Constraints;
import androidx.work.NetworkType;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.example.safewomen.api.ApiClient;
import com.example.safewomen.api.ApiService;
import com.example.safewomen.data.ContactDao;
import com.example.safewomen.data.SafeWomenDatabase;
import com.example.safewomen.models.entities.EmergencyContactEntity;
import com.example.safewomen.utils.NetworkLiveData;
import com.example.safewomen.utils.NetworkUtil;
import com.example.safewomen.utils.PreferenceManager;
import com.example.safewomen.workers.ContactSyncWorker;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Repository for managing emergency contacts with offline support
 */
public class ContactRepository {
    private static final String TAG = "ContactRepository";
    private static ContactRepository instance;
    private static Application appContext;

    private final ContactDao contactDao;
    private final ApiService apiService;
    private final Executor executor;
    private final WorkManager workManager;

    public static synchronized void init(Application application) {
        if (instance == null) {
            appContext = application;
            instance = new ContactRepository();
        }
    }

    public static synchronized ContactRepository getInstance() {
        if (instance == null) {
            throw new IllegalStateException("ContactRepository must be initialized first");
        }
        return instance;
    }

    private ContactRepository() {
        SafeWomenDatabase db = SafeWomenDatabase.getInstance(appContext);
        contactDao = db.contactDao();
        apiService = ApiClient.getClient().create(ApiService.class);
        executor = Executors.newSingleThreadExecutor();
        workManager = WorkManager.getInstance(appContext);

        // Load contacts if user is logged in
        if (PreferenceManager.getInstance().isLoggedIn()) {
            loadContacts();
        }

        // Observe network changes to sync pending contacts
        NetworkLiveData.getInstance(appContext).observeForever(isOnline -> {
            if (isOnline) {
                syncPendingContacts();
            }
        });
    }

    /**
     * Get all emergency contacts as LiveData
     */
    public LiveData<List<EmergencyContactEntity>> getContacts() {
        return contactDao.getAllContacts();
    }

    /**
     * Add a new emergency contact with offline support
     */
    public void addContact(String name, String phone, String relationship, boolean isPrimary, ContactCallback callback) {
        // Generate a local ID for new contact
        String localId = "local_" + UUID.randomUUID().toString();

        // Create contact entity
        EmergencyContactEntity contact = new EmergencyContactEntity(
                localId, name, phone, relationship, isPrimary, "pending"
        );

        // Save locally first
        executor.execute(() -> {
            // If this is primary, reset other contacts
            if (isPrimary) {
                contactDao.resetPrimaryExcept(localId);
            }

            // Insert the new contact
            contactDao.insert(contact);

            // Try to sync immediately if online
            if (NetworkUtil.isOnline(appContext)) {
                uploadContact(contact, callback);
            } else {
                // Schedule sync for when network is available
                scheduleContactSync();

                // Notify success for local operation
                if (callback != null) {
                    callback.onSuccess();
                }
            }
        });
    }

    /**
     * Update an existing emergency contact with offline support
     */
    public void updateContact(String contactId, String name, String phone, String relationship, boolean isPrimary, ContactCallback callback) {
        executor.execute(() -> {
            // Create updated contact entity
            EmergencyContactEntity contact = new EmergencyContactEntity(
                    contactId, name, phone, relationship, isPrimary, "pending"
            );

            // If this is primary, reset other contacts
            if (isPrimary) {
                contactDao.resetPrimaryExcept(contactId);
            }

            // Update locally
            contactDao.update(contact);

            // Try to sync immediately if online
            if (NetworkUtil.isOnline(appContext)) {
                uploadContact(contact, callback);
            } else {
                // Schedule sync for when network is available
                scheduleContactSync();

                // Notify success for local operation
                if (callback != null) {
                    callback.onSuccess();
                }
            }
        });
    }
    /**
     * Delete an emergency contact
     */
    public void deleteContact(String contactId, ContactCallback callback) {
        // For deletion, we need to be online
        if (!NetworkUtil.isOnline(appContext)) {
            if (callback != null) {
                callback.onError("Cannot delete contact while offline");
            }
            return;
        }

        // Create parameters for PHP
        Map<String, String> params = new HashMap<>();
        params.put("contact_id", contactId);

        apiService.deleteContact(params).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseString = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseString);

                        if (jsonResponse.optBoolean("success", false)) {
                            // Delete locally
                            executor.execute(() -> contactDao.deleteById(contactId));

                            if (callback != null) {
                                callback.onSuccess();
                            }
                        } else {
                            // Handle error from PHP
                            String message = jsonResponse.optString("message", "Failed to delete contact");
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
                    Log.e(TAG, "Error parsing delete contact response", e);
                    if (callback != null) {
                        callback.onError("Error processing response: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Delete contact network error", t);
                if (callback != null) {
                    callback.onError("Network error: " + t.getMessage());
                }
            }
        });
    }

    /**
     * Load contacts from server and update local database
     */
    public void loadContacts() {
        apiService.getContacts().enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseString = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseString);

                        if (jsonResponse.optBoolean("success", false)) {
                            // Parse contacts
                            JSONArray contactsArray = jsonResponse.getJSONArray("contacts");
                            List<EmergencyContactEntity> contactsList = new ArrayList<>();

                            for (int i = 0; i < contactsArray.length(); i++) {
                                JSONObject contactJson = contactsArray.getJSONObject(i);
                                EmergencyContactEntity contact = parseContactFromJson(contactJson);
                                contactsList.add(contact);
                            }

                            // Update local database
                            executor.execute(() -> {
                                // Only replace synced contacts, keep pending ones
                                for (EmergencyContactEntity contact : contactsList) {
                                    contact.setSyncStatus("synced");
                                    contactDao.insert(contact);
                                }
                            });
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error parsing contacts", e);
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Load contacts network error", t);
            }
        });
    }

    /**
     * Upload a contact to the server
     */
    private void uploadContact(EmergencyContactEntity contact, ContactCallback callback) {
        // Create parameters for PHP
        Map<String, String> params = new HashMap<>();
        params.put("name", contact.getName());
        params.put("phone", contact.getPhone());
        params.put("relationship", contact.getRelationship());
        params.put("is_primary", contact.isPrimary() ? "1" : "0");

        Call<ResponseBody> call;

        // Determine if this is a new contact or an update
        if (contact.getId().startsWith("local_")) {
            // New contact
            call = apiService.addContact(params);
        } else {
            // Existing contact
            params.put("contact_id", contact.getId());
            call = apiService.updateContact(params);
        }

        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful() && response.body() != null) {
                        String responseString = response.body().string();
                        JSONObject jsonResponse = new JSONObject(responseString);

                        if (jsonResponse.optBoolean("success", false)) {
                            // Update sync status
                            if (jsonResponse.has("contact")) {
                                JSONObject contactData = jsonResponse.getJSONObject("contact");
                                final String serverId = contactData.getString("id");

                                executor.execute(() -> {
                                    if (contact.getId().startsWith("local_")) {
                                        // For new contacts, replace local ID with server ID
                                        EmergencyContactEntity updatedContact = new EmergencyContactEntity(
                                                serverId,
                                                contact.getName(),
                                                contact.getPhone(),
                                                contact.getRelationship(),
                                                contact.isPrimary(),
                                                "synced"
                                        );

                                        contactDao.deleteById(contact.getId());
                                        contactDao.insert(updatedContact);
                                    } else {
                                        // Just update sync status
                                        contact.setSyncStatus("synced");
                                        contactDao.update(contact);
                                    }
                                });
                            } else {
                                // Just update sync status
                                executor.execute(() -> {
                                    contact.setSyncStatus("synced");
                                    contactDao.update(contact);
                                });
                            }

                            if (callback != null) {
                                callback.onSuccess();
                            }
                        } else {
                            // Handle error from PHP
                            String message = jsonResponse.optString("message", "Failed to save contact");
                            if (callback != null) {
                                callback.onError(message);
                            }

                            // Mark as failed
                            executor.execute(() -> {
                                contact.setSyncStatus("failed");
                                contactDao.update(contact);
                            });
                        }
                    } else {
                        // Handle HTTP error
                        if (callback != null) {
                            callback.onError("Server error: " + response.code());
                        }

                        // Mark as failed
                        executor.execute(() -> {
                            contact.setSyncStatus("failed");
                            contactDao.update(contact);
                        });
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error processing contact response", e);
                    if (callback != null) {
                        callback.onError("Error processing response: " + e.getMessage());
                    }

                    // Mark as failed
                    executor.execute(() -> {
                        contact.setSyncStatus("failed");
                        contactDao.update(contact);
                    });
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e(TAG, "Contact network error", t);
                if (callback != null) {
                    callback.onError("Network error: " + t.getMessage());
                }

                // Keep as pending for retry
            }
        });
    }

    /**
     * Sync all pending contacts
     */
    private void syncPendingContacts() {
        executor.execute(() -> {
            List<EmergencyContactEntity> pendingContacts = contactDao.getPendingContacts();
            for (EmergencyContactEntity contact : pendingContacts) {
                uploadContact(contact, null);
            }
        });
    }

    /**
     * Schedule background sync for contacts
     */
    private void scheduleContactSync() {
        Constraints constraints = new Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build();

        OneTimeWorkRequest syncWork = new OneTimeWorkRequest.Builder(ContactSyncWorker.class)
                .setConstraints(constraints)
                .build();

        workManager.enqueue(syncWork);
    }

    /**
     * Helper method to parse EmergencyContactEntity from JSON
     */
    private EmergencyContactEntity parseContactFromJson(JSONObject json) throws JSONException {
        String id = json.getString("id");
        String name = json.getString("name");
        String phone = json.getString("phone");
        String relationship = json.getString("relationship");
        boolean isPrimary = json.getInt("is_primary") == 1;

        return new EmergencyContactEntity(id, name, phone, relationship, isPrimary, "synced");
    }

    /**
     * Clear all data (for logout)
     */
    public void clear() {
        executor.execute(() -> contactDao.clear());
    }

    /**
     * Callback interface for contact operations
     */
    public interface ContactCallback {
        void onSuccess();
        void onError(String message);
    }
}

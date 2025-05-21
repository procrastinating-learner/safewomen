package com.example.safewomen.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.safewomen.api.ApiClient;
import com.example.safewomen.api.ApiService;
import com.example.safewomen.models.entities.EmergencyContactEntity;
import com.example.safewomen.data.ContactDao;
import com.example.safewomen.data.SafeWomenDatabase;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;

public class ContactSyncWorker extends Worker {
    private static final String TAG = "ContactSyncWorker";

    public ContactSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Context context = getApplicationContext();
            SafeWomenDatabase db = SafeWomenDatabase.getInstance(context);
            ContactDao dao = db.contactDao();
            List<EmergencyContactEntity> pending = dao.getPendingContacts();
            ApiService api = ApiClient.getClient().create(ApiService.class);

            for (EmergencyContactEntity contact : pending) {
                try {
                    Map<String, String> params = new HashMap<>();
                    params.put("name", contact.getName());
                    params.put("phone", contact.getPhone());
                    params.put("relationship", contact.getRelationship());
                    params.put("is_primary", contact.isPrimary() ? "1" : "0");

                    // If contact has a server ID (for updates), include it
                    if (!contact.getId().startsWith("local_")) {
                        params.put("contact_id", contact.getId());
                        Call<ResponseBody> call = api.updateContact(params);
                        Response<ResponseBody> response = call.execute();
                        processResponse(response, contact, dao);
                    } else {
                        // New contact
                        Call<ResponseBody> call = api.addContact(params);
                        Response<ResponseBody> response = call.execute();
                        processResponse(response, contact, dao);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error syncing contact: " + contact.getId(), e);
                    // Mark as failed but don't stop the worker
                    dao.updateSyncStatus(contact.getId(), "failed");
                }
            }
            return Result.success();
        } catch (Exception e) {
            Log.e(TAG, "Contact sync worker failed", e);
            return Result.retry();
        }
    }

    private void processResponse(Response<ResponseBody> response, EmergencyContactEntity contact, ContactDao dao) {
        try {
            if (response.isSuccessful() && response.body() != null) {
                String responseString = response.body().string();
                JSONObject jsonResponse = new JSONObject(responseString);

                if (jsonResponse.optBoolean("success", false)) {
                    // If this was a new contact, update with server ID
                    if (contact.getId().startsWith("local_") && jsonResponse.has("contact")) {
                        JSONObject contactData = jsonResponse.getJSONObject("contact");
                        String serverId = contactData.getString("id");

                        // Create updated entity with server ID
                        EmergencyContactEntity updated = new EmergencyContactEntity(
                                serverId,
                                contact.getName(),
                                contact.getPhone(),
                                contact.getRelationship(),
                                contact.isPrimary(),
                                "synced"
                        );

                        // Delete old local entry and insert updated one
                        dao.deleteById(contact.getId());
                        dao.insert(updated);
                    } else {
                        // Just update sync status for existing contacts
                        dao.updateSyncStatus(contact.getId(), "synced");
                    }
                } else {
                    dao.updateSyncStatus(contact.getId(), "failed");
                }
            } else {
                dao.updateSyncStatus(contact.getId(), "failed");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error processing contact sync response", e);
            dao.updateSyncStatus(contact.getId(), "failed");
        }
    }
}
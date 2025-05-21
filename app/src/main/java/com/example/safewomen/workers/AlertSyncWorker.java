package com.example.safewomen.workers;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.example.safewomen.api.ApiClient;
import com.example.safewomen.api.ApiService;
import com.example.safewomen.models.entities.AlertEntity;
import com.example.safewomen.data.AlertDao;
import com.example.safewomen.data.SafeWomenDatabase;

import java.util.List;

public class AlertSyncWorker extends Worker {
    public AlertSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            Context context = getApplicationContext();
            SafeWomenDatabase db = SafeWomenDatabase.getInstance(context);
            AlertDao dao = db.alertDao();

            List<AlertEntity> pending = dao.getPendingAlerts();
            ApiService api = ApiClient.getClient().create(ApiService.class);

            for (AlertEntity alert : pending) {
                // TODO: Add your upload logic here.
                // On success: dao.updateAlertStatus(alert.getId(), "active");
                // On failure: continue to next or mark for retry
            }

            return Result.success();
        } catch (Exception e) {
            e.printStackTrace();
            return Result.retry(); // optional: retry later if something fails
        }
    }
}
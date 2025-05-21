package com.example.safewomen.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.safewomen.models.entities.AlertEntity;

import java.util.List;

@Dao
public interface AlertDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(AlertEntity alert);

    @Query("SELECT * FROM alerts ORDER BY timestamp DESC")
    LiveData<List<AlertEntity>> getAllAlerts();

    @Query("SELECT * FROM alerts WHERE status = 'active' LIMIT 1")
    LiveData<AlertEntity> getActiveAlert();

    @Query("SELECT * FROM alerts WHERE status = 'pending'")
    List<AlertEntity> getPendingAlerts();

    @Update
    void update(AlertEntity alert);

    @Query("DELETE FROM alerts")
    void clear();
}
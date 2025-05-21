package com.example.safewomen.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.example.safewomen.models.entities.LocationHistoryEntity;

import java.util.List;

@Dao
public interface LocationHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertLocation(LocationHistoryEntity location);

    @Query("SELECT * FROM location_history ORDER BY timestamp DESC LIMIT :limit")
    List<LocationHistoryEntity> getRecentLocations(int limit);

    @Query("SELECT * FROM location_history ORDER BY timestamp DESC LIMIT 1")
    LocationHistoryEntity getMostRecentLocation();

    @Query("SELECT * FROM location_history ORDER BY timestamp DESC")
    LiveData<List<LocationHistoryEntity>> observeLocationHistory();

    @Query("DELETE FROM location_history WHERE timestamp < :olderThanTimestamp")
    void deleteOldLocations(long olderThanTimestamp);

    @Query("DELETE FROM location_history")
    void clearAllLocations();

    @Query("SELECT COUNT(*) FROM location_history")
    int getLocationCount();
    @Query("SELECT * FROM location_history WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    List<LocationHistoryEntity> getLocationsInTimeRange(long startTime, long endTime);

    @Query("SELECT * FROM location_history ORDER BY timestamp DESC")
    List<LocationHistoryEntity> getAllLocationsForExport();

}
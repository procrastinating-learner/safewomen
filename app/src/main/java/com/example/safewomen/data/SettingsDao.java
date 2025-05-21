package com.example.safewomen.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.safewomen.models.entities.UserSettingsEntity;

@Dao
public interface SettingsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void saveSettings(UserSettingsEntity settings);
    @Update
    void updateSettings(UserSettingsEntity settings);

    @Query("SELECT * FROM user_settings WHERE user_id = :userId LIMIT 1")
    UserSettingsEntity getSettingsForUser(String userId);

    @Query("SELECT * FROM user_settings LIMIT 1")
    UserSettingsEntity getCurrentSettings();

    @Query("SELECT * FROM user_settings LIMIT 1")
    LiveData<UserSettingsEntity> observeSettings();

    @Query("UPDATE user_settings SET notifications_enabled = :enabled WHERE user_id = :userId")
    void updateNotificationSettings(String userId, boolean enabled);

    @Query("UPDATE user_settings SET sos_trigger_method = :method WHERE user_id = :userId")
    void updateSosTriggerMethod(String userId, String method);

    @Query("UPDATE user_settings SET auto_location_sharing = :enabled WHERE user_id = :userId")
    void updateAutoLocationSharing(String userId, boolean enabled);

    @Query("DELETE FROM user_settings")
    void clearSettings();
}
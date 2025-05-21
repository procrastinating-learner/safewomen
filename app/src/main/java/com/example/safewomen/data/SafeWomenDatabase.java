package com.example.safewomen.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.example.safewomen.models.entities.AlertEntity;
import com.example.safewomen.models.entities.EmergencyContactEntity;
import com.example.safewomen.models.entities.LocationHistoryEntity;
import com.example.safewomen.models.entities.UserEntity;
import com.example.safewomen.models.entities.UserSettingsEntity;

@Database(entities = {
        AlertEntity.class,
        EmergencyContactEntity.class,
        UserEntity.class,
        LocationHistoryEntity.class,
        UserSettingsEntity.class
}, version = 1, exportSchema = false)public abstract class SafeWomenDatabase extends RoomDatabase {
    private static SafeWomenDatabase instance;

    public abstract AlertDao alertDao();
    public abstract ContactDao contactDao();
    public abstract AuthDao authDao();
    public abstract LocationHistoryDao locationHistoryDao();
    public abstract SettingsDao settingsDao();
    public static synchronized SafeWomenDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            SafeWomenDatabase.class,
                            "safewomen_db")
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return instance;
    }
}

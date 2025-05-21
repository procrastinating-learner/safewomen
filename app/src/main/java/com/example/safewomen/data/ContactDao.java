package com.example.safewomen.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.safewomen.models.entities.EmergencyContactEntity;

import java.util.List;

@Dao
public interface ContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(EmergencyContactEntity contact);

    @Update
    void update(EmergencyContactEntity contact);

    @Query("SELECT * FROM contacts ORDER BY isPrimary DESC, name ASC")
    LiveData<List<EmergencyContactEntity>> getAllContacts();

    @Query("SELECT * FROM contacts WHERE syncStatus = 'pending'")
    List<EmergencyContactEntity> getPendingContacts();

    @Query("DELETE FROM contacts WHERE id = :contactId")
    void deleteById(String contactId);

    @Query("UPDATE contacts SET isPrimary = 0 WHERE id != :contactId")
    void resetPrimaryExcept(String contactId);

    @Query("UPDATE contacts SET syncStatus = :status WHERE id = :contactId")
    void updateSyncStatus(String contactId, String status);

    @Query("DELETE FROM contacts")
    void clear();
}

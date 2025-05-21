package com.example.safewomen.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.example.safewomen.models.entities.UserEntity;

@Dao
public interface AuthDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void saveUser(UserEntity user);

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    UserEntity getUserById(String userId);

    @Query("SELECT * FROM users LIMIT 1")
    UserEntity getCurrentUser();

    @Query("SELECT * FROM users LIMIT 1")
    LiveData<UserEntity> observeCurrentUser();

    @Update
    void updateUser(UserEntity user);

    @Query("DELETE FROM users")
    void clearUserData();

    @Query("UPDATE users SET auth_token = :token WHERE id = :userId")
    void updateAuthToken(String userId, String token);

    @Query("SELECT auth_token FROM users WHERE id = :userId")
    String getAuthToken(String userId);
}

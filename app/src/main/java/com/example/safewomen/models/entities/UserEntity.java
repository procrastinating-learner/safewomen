package com.example.safewomen.models.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "users")
public class UserEntity {
    @PrimaryKey
    @NonNull
    private String id;
    private String name;
    private String email;
    private String phone;
    private String auth_token;
    private long lastLoginTimestamp;

    public UserEntity() {}

    public UserEntity(@NonNull String id, String name, String email, String phone,
                      String auth_token, long lastLoginTimestamp) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.auth_token = auth_token;
        this.lastLoginTimestamp = lastLoginTimestamp;
    }

    @NonNull
    public String getId() { return id; }
    public void setId(@NonNull String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAuth_token() { return auth_token; }
    public void setAuth_token(String auth_token) { this.auth_token = auth_token; }

    public long getLastLoginTimestamp() { return lastLoginTimestamp; }
    public void setLastLoginTimestamp(long lastLoginTimestamp) { this.lastLoginTimestamp = lastLoginTimestamp; }
}
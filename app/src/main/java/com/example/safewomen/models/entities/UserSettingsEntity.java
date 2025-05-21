package com.example.safewomen.models.entities;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_settings")
public class UserSettingsEntity {
    @PrimaryKey
    @NonNull
    private String user_id;
    private boolean notifications_enabled;
    private String sos_trigger_method; // "button", "shake", "voice", etc.
    private boolean auto_location_sharing;
    private int location_update_frequency; // in minutes
    private boolean dark_mode_enabled;
    private String emergency_message_template;

    public UserSettingsEntity() {}

    public UserSettingsEntity(@NonNull String user_id, boolean notifications_enabled,
                              String sos_trigger_method, boolean auto_location_sharing,
                              int location_update_frequency, boolean dark_mode_enabled,
                              String emergency_message_template) {
        this.user_id = user_id;
        this.notifications_enabled = notifications_enabled;
        this.sos_trigger_method = sos_trigger_method;
        this.auto_location_sharing = auto_location_sharing;
        this.location_update_frequency = location_update_frequency;
        this.dark_mode_enabled = dark_mode_enabled;
        this.emergency_message_template = emergency_message_template;
    }

    @NonNull
    public String getUser_id() { return user_id; }
    public void setUser_id(@NonNull String user_id) { this.user_id = user_id; }

    public boolean isNotifications_enabled() { return notifications_enabled; }
    public void setNotifications_enabled(boolean notifications_enabled) { this.notifications_enabled = notifications_enabled; }

    public String getSos_trigger_method() { return sos_trigger_method; }
    public void setSos_trigger_method(String sos_trigger_method) { this.sos_trigger_method = sos_trigger_method; }

    public boolean isAuto_location_sharing() { return auto_location_sharing; }
    public void setAuto_location_sharing(boolean auto_location_sharing) { this.auto_location_sharing = auto_location_sharing; }

    public int getLocation_update_frequency() { return location_update_frequency; }
    public void setLocation_update_frequency(int location_update_frequency) { this.location_update_frequency = location_update_frequency; }

    public boolean isDark_mode_enabled() { return dark_mode_enabled; }
    public void setDark_mode_enabled(boolean dark_mode_enabled) { this.dark_mode_enabled = dark_mode_enabled; }

    public String getEmergency_message_template() { return emergency_message_template; }
    public void setEmergency_message_template(String emergency_message_template) { this.emergency_message_template = emergency_message_template; }
}
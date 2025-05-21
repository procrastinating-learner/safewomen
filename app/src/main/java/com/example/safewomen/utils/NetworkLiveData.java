package com.example.safewomen.utils;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.lifecycle.LiveData;

public class NetworkLiveData extends LiveData<Boolean> {
    private static NetworkLiveData instance;
    private final ConnectivityManager cm;

    private NetworkLiveData(Context context) {
        cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        postValue(isOnline());
    }

    public static synchronized NetworkLiveData getInstance(Context context) {
        if (instance == null) {
            instance = new NetworkLiveData(context.getApplicationContext());
        }
        return instance;
    }

    private boolean isOnline() {
        NetworkInfo ni = cm.getActiveNetworkInfo();
        return ni != null && ni.isConnected();
    }
}
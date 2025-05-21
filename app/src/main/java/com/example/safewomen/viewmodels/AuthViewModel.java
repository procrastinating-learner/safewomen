package com.example.safewomen.viewmodels;

import android.app.Application;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.safewomen.models.entities.UserEntity;
import com.example.safewomen.repositories.AuthRepository;

public class AuthViewModel extends ViewModel {
    private static final String TAG = "AuthViewModel";
    private final AuthRepository authRepository;
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    public AuthViewModel() {
        authRepository = AuthRepository.getInstance();
    }

    // Getters for LiveData objects
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    // Observe current user
    public LiveData<UserEntity> getCurrentUser() {
        return authRepository.getCurrentUser();
    }

    // Login method
    public void login(String email, String password) {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        authRepository.login(email, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(UserEntity user) {
                isLoading.postValue(false);
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    // Register method
    public void register(String name, String email, String phone, String password) {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        authRepository.register(name, email, phone, password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(UserEntity user) {
                isLoading.postValue(false);
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    // Logout method
    public void logout() {
        authRepository.logout();
    }

    // Update profile method
    public void updateProfile(String name, String email, String phone) {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        authRepository.updateProfile(name, email, phone, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(UserEntity user) {
                isLoading.postValue(false);
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    // Password reset functionality
    public void requestPasswordReset(String email) {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        authRepository.requestPasswordReset(email, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(UserEntity user) {
                isLoading.postValue(false);
                // You might want to set a success message here
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    public void resetPassword(String email, String resetCode, String newPassword) {
        isLoading.setValue(true);
        errorMessage.setValue(null);

        authRepository.resetPassword(email, resetCode, newPassword, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(UserEntity user) {
                isLoading.postValue(false);
                // If user is returned, they are automatically logged in
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    // Check if user is logged in
    public boolean isLoggedIn() {
        // We can check if the current user is not null
        return authRepository.getCurrentUser().getValue() != null;
    }
}

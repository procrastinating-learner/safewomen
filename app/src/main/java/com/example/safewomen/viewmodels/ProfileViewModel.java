package com.example.safewomen.viewmodels;

import android.app.Application;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.example.safewomen.models.User;
import com.example.safewomen.models.entities.UserEntity;
import com.example.safewomen.repositories.AuthRepository;
import com.example.safewomen.repositories.UserRepository;

public class ProfileViewModel extends ViewModel {
    private final UserRepository userRepository;
    private final AuthRepository authRepository;

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();

    public ProfileViewModel(Application application) {
        userRepository = UserRepository.getInstance();
        authRepository = AuthRepository.getInstance();
    }

    // Getters for LiveData
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }

    // Get current user data
    public LiveData<UserEntity> getCurrentUser() {
        return authRepository.getCurrentUser();
    }

    // Update profile information
    public void updateProfile(String name, String email, String phone) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        successMessage.setValue(null);

        authRepository.updateProfile(name, email, phone, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(UserEntity user) {
                isLoading.postValue(false);
                successMessage.postValue("Profile updated successfully");
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    // Change password
    public void changePassword(String currentPassword, String newPassword) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        successMessage.setValue(null);

        authRepository.changePassword(currentPassword, newPassword, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(UserEntity user) {
                isLoading.postValue(false);
                successMessage.postValue("Password changed successfully");
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    // Delete account
    public void deleteAccount(String password) {
        isLoading.setValue(true);
        errorMessage.setValue(null);
        successMessage.setValue(null);

        authRepository.deleteAccount(password, new AuthRepository.AuthCallback() {
            @Override
            public void onSuccess(UserEntity user) {
                isLoading.postValue(false);
                successMessage.postValue("Account deleted successfully");
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    // Log out user
    public void logout() {
        authRepository.logout();
    }
}

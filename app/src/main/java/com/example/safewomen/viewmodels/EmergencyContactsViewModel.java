package com.example.safewomen.viewmodels;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.ContentResolver;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.util.Log;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.safewomen.models.entities.EmergencyContactEntity;
import com.example.safewomen.repositories.ContactRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class EmergencyContactsViewModel extends AndroidViewModel {
    private static final String TAG = "EmergencyContactsVM";

    private final ContactRepository contactRepository;
    private final Executor executor;

    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<String> successMessage = new MutableLiveData<>();
    private final MutableLiveData<List<PhoneContact>> phoneContacts = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isImportingContacts = new MutableLiveData<>(false);

    public EmergencyContactsViewModel(Application application) {
        super(application);
        this.contactRepository = ContactRepository.getInstance();
        this.executor = Executors.newSingleThreadExecutor();
    }

    // Getters for LiveData
    public LiveData<List<EmergencyContactEntity>> getContacts() {
        return contactRepository.getContacts();
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<String> getSuccessMessage() {
        return successMessage;
    }

    public LiveData<List<PhoneContact>> getPhoneContacts() {
        return phoneContacts;
    }

    public LiveData<Boolean> getIsImportingContacts() {
        return isImportingContacts;
    }

    // Add a new emergency contact
    public void addContact(String name, String phone, String relationship, boolean isPrimary) {
        isLoading.setValue(true);

        contactRepository.addContact(name, phone, relationship, isPrimary, new ContactRepository.ContactCallback() {
            @Override
            public void onSuccess() {
                isLoading.postValue(false);
                successMessage.postValue("Contact added successfully");
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    // Update an existing emergency contact
    public void updateContact(String contactId, String name, String phone, String relationship, boolean isPrimary) {
        isLoading.setValue(true);

        contactRepository.updateContact(contactId, name, phone, relationship, isPrimary, new ContactRepository.ContactCallback() {
            @Override
            public void onSuccess() {
                isLoading.postValue(false);
                successMessage.postValue("Contact updated successfully");
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    // Delete an emergency contact
    public void deleteContact(String contactId) {
        isLoading.setValue(true);

        contactRepository.deleteContact(contactId, new ContactRepository.ContactCallback() {
            @Override
            public void onSuccess() {
                isLoading.postValue(false);
                successMessage.postValue("Contact deleted successfully");
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    // Set a contact as primary
    public void setPrimaryContact(String contactId, String name, String phone, String relationship) {
        isLoading.setValue(true);

        contactRepository.updateContact(contactId, name, phone, relationship, true, new ContactRepository.ContactCallback() {
            @Override
            public void onSuccess() {
                isLoading.postValue(false);
                successMessage.postValue("Primary contact updated");
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue(message);
            }
        });
    }

    // Refresh contacts from server
    public void refreshContacts() {
        isLoading.setValue(true);
        contactRepository.loadContacts();
        isLoading.setValue(false);
    }

    // Load phone contacts
    public void loadPhoneContacts() {
        isImportingContacts.setValue(true);

        executor.execute(() -> {
            List<PhoneContact> contacts = new ArrayList<>();
            ContentResolver contentResolver = getApplication().getContentResolver();

            try {
                Cursor cursor = contentResolver.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        new String[]{
                                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                                ContactsContract.CommonDataKinds.Phone.NUMBER,
                                ContactsContract.CommonDataKinds.Phone.CONTACT_ID
                        },
                        null,
                        null,
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " ASC"
                );

                if (cursor != null) {
                    try {
                        while (cursor.moveToNext()) {
                            @SuppressLint("Range") String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                            @SuppressLint("Range") String phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                            @SuppressLint("Range") String contactId = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));

                            // Clean up phone number
                            phoneNumber = phoneNumber.replaceAll("[^0-9+]", "");

                            contacts.add(new PhoneContact(contactId, name, phoneNumber));
                        }
                    } finally {
                        cursor.close();
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading phone contacts", e);
                errorMessage.postValue("Error loading phone contacts: " + e.getMessage());
            }

            phoneContacts.postValue(contacts);
            isImportingContacts.postValue(false);
        });
    }

    // Add a phone contact as an emergency contact
    public void addPhoneContactAsEmergencyContact(PhoneContact phoneContact, String relationship, boolean isPrimary) {
        addContact(phoneContact.getName(), phoneContact.getPhoneNumber(), relationship, isPrimary);
    }

    // Phone Contact class
    public static class PhoneContact {
        private final String id;
        private final String name;
        private final String phoneNumber;

        public PhoneContact(String id, String name, String phoneNumber) {
            this.id = id;
            this.name = name;
            this.phoneNumber = phoneNumber;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public String getPhoneNumber() {
            return phoneNumber;
        }
    }
}

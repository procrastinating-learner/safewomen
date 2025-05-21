package com.example.safewomen.viewmodels;

import android.app.Application;
import android.content.Intent;
import android.os.Environment;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.safewomen.services.EmergencyRecordingService;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class EmergencyRecordingViewModel extends ViewModel {
    private static final String TAG = "EmergencyRecordingVM";
    private final MutableLiveData<Boolean> isRecording = new MutableLiveData<>(false);
    private final MutableLiveData<String> recordingType = new MutableLiveData<>();
    private final MutableLiveData<String> recordingFilePath = new MutableLiveData<>();
    private final MutableLiveData<List<RecordingFile>> recordingHistory = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private final Application application;
    private final Executor executor;

    public EmergencyRecordingViewModel(Application application) {
        this.application = application;
        this.executor = Executors.newSingleThreadExecutor();

        // Load recording history on initialization
        loadRecordingHistory();
    }

    // Getters for LiveData
    public LiveData<Boolean> isRecording() {
        return isRecording;
    }

    public LiveData<String> getRecordingType() {
        return recordingType;
    }

    public LiveData<String> getRecordingFilePath() {
        return recordingFilePath;
    }

    public LiveData<List<RecordingFile>> getRecordingHistory() {
        return recordingHistory;
    }

    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    // Start audio recording
    public void startAudioRecording() {
        if (isRecording.getValue() != null && isRecording.getValue()) {
            stopRecording();
        }

        Intent intent = new Intent(application, EmergencyRecordingService.class);
        intent.setAction("START_AUDIO_RECORDING");
        application.startService(intent);

        isRecording.setValue(true);
        recordingType.setValue("audio");
        Log.d(TAG, "Started audio recording");
    }

    // Start video recording
    public void startVideoRecording() {
        if (isRecording.getValue() != null && isRecording.getValue()) {
            stopRecording();
        }

        Intent intent = new Intent(application, EmergencyRecordingService.class);
        intent.setAction("START_VIDEO_RECORDING");
        application.startService(intent);

        isRecording.setValue(true);
        recordingType.setValue("video");
        Log.d(TAG, "Started video recording");
    }

    // Stop recording
    public void stopRecording() {
        Intent intent = new Intent(application, EmergencyRecordingService.class);
        intent.setAction("STOP_RECORDING");
        application.startService(intent);

        isRecording.setValue(false);
        recordingType.setValue(null);

        // Reload recording history after stopping
        loadRecordingHistory();

        Log.d(TAG, "Stopped recording");
    }

    // Get recording history
    public void loadRecordingHistory() {
        isLoading.setValue(true);

        executor.execute(() -> {
            try {
                List<RecordingFile> recordings = new ArrayList<>();

                // Get the directory where recordings are stored
                File storageDir = new File(application.getExternalFilesDir(Environment.DIRECTORY_MOVIES), "SafeWomen");

                if (storageDir.exists() && storageDir.isDirectory()) {
                    File[] files = storageDir.listFiles();

                    if (files != null) {
                        // Sort files by last modified date (newest first)
                        Arrays.sort(files, Comparator.comparing(File::lastModified).reversed());

                        for (File file : files) {
                            if (file.isFile()) {
                                String fileName = file.getName();
                                String filePath = file.getAbsolutePath();
                                long fileSize = file.length();
                                long lastModified = file.lastModified();

                                // Determine file type
                                String fileType = "unknown";
                                if (fileName.endsWith(".mp3")) {
                                    fileType = "audio";
                                } else if (fileName.endsWith(".mp4")) {
                                    fileType = "video";
                                }

                                // Add to list
                                recordings.add(new RecordingFile(
                                        fileName,
                                        filePath,
                                        fileType,
                                        fileSize,
                                        lastModified
                                ));
                            }
                        }
                    }
                }

                // Update LiveData on main thread
                recordingHistory.postValue(recordings);
                isLoading.postValue(false);

            } catch (Exception e) {
                Log.e(TAG, "Error loading recording history", e);
                errorMessage.postValue("Error loading recording history: " + e.getMessage());
                isLoading.postValue(false);
            }
        });
    }

    // Delete a recording
    public void deleteRecording(String filePath) {
        executor.execute(() -> {
            try {
                File file = new File(filePath);
                if (file.exists()) {
                    boolean deleted = file.delete();
                    if (deleted) {
                        Log.d(TAG, "Deleted recording: " + filePath);

                        // Reload recording history after deletion
                        loadRecordingHistory();
                    } else {
                        Log.e(TAG, "Failed to delete recording: " + filePath);
                        errorMessage.postValue("Failed to delete recording");
                    }
                } else {
                    Log.e(TAG, "Recording file does not exist: " + filePath);
                    errorMessage.postValue("Recording file does not exist");
                }
            } catch (Exception e) {
                Log.e(TAG, "Error deleting recording", e);
                errorMessage.postValue("Error deleting recording: " + e.getMessage());
            }
        });
    }

    // Share a recording
    public Intent createShareIntent(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            errorMessage.setValue("File does not exist");
            return null;
        }

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType(filePath.endsWith(".mp3") ? "audio/*" : "video/*");
        shareIntent.putExtra(Intent.EXTRA_STREAM, android.net.Uri.fromFile(file));
        shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        return shareIntent;
    }

    // Model class for recording files
    public static class RecordingFile {
        private final String name;
        private final String path;
        private final String type;
        private final long size;
        private final long timestamp;

        public RecordingFile(String name, String path, String type, long size, long timestamp) {
            this.name = name;
            this.path = path;
            this.type = type;
            this.size = size;
            this.timestamp = timestamp;
        }

        public String getName() {
            return name;
        }

        public String getPath() {
            return path;
        }

        public String getType() {
            return type;
        }

        public long getSize() {
            return size;
        }

        public long getTimestamp() {
            return timestamp;
        }

        // Format file size for display (KB, MB)
        public String getFormattedSize() {
            if (size < 1024) {
                return size + " B";
            } else if (size < 1024 * 1024) {
                return String.format("%.1f KB", size / 1024.0);
            } else {
                return String.format("%.1f MB", size / (1024.0 * 1024));
            }
        }
    }
}

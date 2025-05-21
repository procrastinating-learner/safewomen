package com.example.safewomen.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import com.example.safewomen.MainActivity;
import com.example.safewomen.R;
import com.example.safewomen.utils.FileUtils;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Service for automatic audio/video recording after SOS trigger
 */
public class EmergencyRecordingService extends Service {
    private static final String TAG = "EmergencyRecordingService";
    private static final String CHANNEL_ID = "emergency_recording_channel";
    private static final int NOTIFICATION_ID = 1002;

    private MediaRecorder mediaRecorder;
    private boolean isRecording = false;
    private String currentOutputFile;
    private Executor executor;
    private RecordingType currentRecordingType = RecordingType.AUDIO;

    public enum RecordingType {
        AUDIO,
        VIDEO
    }

    @Override
    public void onCreate() {
        super.onCreate();
        executor = Executors.newSingleThreadExecutor();
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case "START_AUDIO_RECORDING":
                        startRecording(RecordingType.AUDIO);
                        break;
                    case "START_VIDEO_RECORDING":
                        startRecording(RecordingType.VIDEO);
                        break;
                    case "STOP_RECORDING":
                        stopRecording();
                        break;
                }
            }
        }
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        stopRecording();
        super.onDestroy();
    }

    private void startRecording(RecordingType type) {
        if (isRecording) {
            stopRecording();
        }
        currentRecordingType = type;
        // Start as foreground service with notification
        startForeground(NOTIFICATION_ID, createNotification(type));

        executor.execute(() -> {
            try {
                if (type == RecordingType.AUDIO) {
                    startAudioRecording();
                } else {
                    startVideoRecording();
                }
            } catch (Exception e) {
                Log.e(TAG, "Error starting recording", e);
                stopSelf();
            }
        });
    }

    private void startAudioRecording() throws IOException {
        // Create output file
        File outputFile = createOutputFile(".mp3");
        currentOutputFile = outputFile.getAbsolutePath();

        // Initialize media recorder
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setAudioEncodingBitRate(128000);
        mediaRecorder.setAudioSamplingRate(44100);
        mediaRecorder.setOutputFile(currentOutputFile);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            Log.d(TAG, "Audio recording started: " + currentOutputFile);
        } catch (IOException e) {
            Log.e(TAG, "Error preparing audio recorder", e);
            releaseMediaRecorder();
            throw e;
        }
    }

    private void startVideoRecording() throws IOException {
        // Create output file
        File outputFile = createOutputFile(".mp4");
        currentOutputFile = outputFile.getAbsolutePath();

        // Get the front camera ID
        String cameraId = getFrontCameraId();
        if (cameraId == null) {
            Log.e(TAG, "No front camera available");
            throw new IOException("No front camera available");
        }

        // Initialize media recorder
        mediaRecorder = new MediaRecorder();
        mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        mediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
        mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
        mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
        mediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
        mediaRecorder.setVideoSize(1280, 720);
        mediaRecorder.setVideoFrameRate(30);
        mediaRecorder.setVideoEncodingBitRate(10000000);
        mediaRecorder.setOutputFile(currentOutputFile);

        try {
            mediaRecorder.prepare();
            mediaRecorder.start();
            isRecording = true;
            Log.d(TAG, "Video recording started: " + currentOutputFile);
        } catch (IOException e) {
            Log.e(TAG, "Error preparing video recorder", e);
            releaseMediaRecorder();
            throw e;
        }
    }

    private String getFrontCameraId() {
        CameraManager cameraManager = (CameraManager) getSystemService(CAMERA_SERVICE);
        try {
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    return cameraId;
                }
            }
        } catch (CameraAccessException e) {
            Log.e(TAG, "Error accessing camera", e);
        }
        return null;
    }

    private void stopRecording() {
        if (!isRecording) return;

        try {
            mediaRecorder.stop();
            mediaRecorder.reset();
            releaseMediaRecorder();

            // Scan file to make it visible in gallery
            if (currentOutputFile != null) {
                FileUtils.scanFile(this, currentOutputFile);
            }
            Log.d(TAG, "Recording stopped: " + currentOutputFile);
        } catch (Exception e) {
            Log.e(TAG, "Error stopping recording", e);
        } finally {
            isRecording = false;
            stopForeground(true);
            stopSelf();
        }
    }

    private void releaseMediaRecorder() {
        if (mediaRecorder != null) {
            mediaRecorder.release();
            mediaRecorder = null;
        }
    }

    private File createOutputFile(String extension) throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "EMERGENCY_" + timeStamp + extension;
        File storageDir = new File(getExternalFilesDir(Environment.DIRECTORY_MOVIES), "SafeWomen");
        if (!storageDir.exists()) {
            storageDir.mkdirs();
        }
        return new File(storageDir, fileName);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Emergency Recording",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Used for emergency recording during SOS situations");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification(RecordingType type) {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        );

        String contentText = type == RecordingType.AUDIO ?
                "Recording audio for your safety" :
                "Recording video for your safety";

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Emergency Recording Active")
                .setContentText(contentText)
                .setSmallIcon(R.drawable.ic_recording)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
    }
}

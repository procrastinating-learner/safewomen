package com.example.safewomen.services;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.safewomen.MainActivity;
import com.example.safewomen.R;
import com.example.safewomen.utils.PreferenceManager;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Service for detecting voice commands like "Help me"
 */
public class VoiceCommandService extends Service {
    private static final String TAG = "VoiceCommandService";
    private static final String CHANNEL_ID = "voice_command_channel";
    private static final int NOTIFICATION_ID = 1003;

    private SpeechRecognizer speechRecognizer;
    private Intent recognizerIntent;
    private boolean isListening = false;

    // List of trigger phrases that will activate SOS
    private final List<String> triggerPhrases = Arrays.asList(
            "help me", "help", "emergency", "sos", "danger",
            "i need help", "call for help", "call police",
            "save me", "i'm in danger", "i am in danger"
    );

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        initializeSpeechRecognizer();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case "START_LISTENING":
                        startVoiceRecognition();
                        break;
                    case "STOP_LISTENING":
                        stopVoiceRecognition();
                        break;
                }
            } else {
                // Default action is to start listening
                startVoiceRecognition();
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
        stopVoiceRecognition();
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
        }
        super.onDestroy();
    }

    private void initializeSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new SpeechRecognitionListener());

            recognizerIntent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());

            // For continuous recognition
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500);
            recognizerIntent.putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 15000);
        } else {
            Log.e(TAG, "Speech recognition is not available on this device");
            stopSelf();
        }
    }

    private void startVoiceRecognition() {
        if (isListening || speechRecognizer == null) return;

        // Start as foreground service with notification
        startForeground(NOTIFICATION_ID, createNotification());

        try {
            speechRecognizer.startListening(recognizerIntent);
            isListening = true;
            Log.d(TAG, "Voice recognition started");
        } catch (Exception e) {
            Log.e(TAG, "Error starting voice recognition", e);
            stopSelf();
        }
    }

    private void stopVoiceRecognition() {
        if (!isListening || speechRecognizer == null) return;

        try {
            speechRecognizer.stopListening();
            isListening = false;
            Log.d(TAG, "Voice recognition stopped");
        } catch (Exception e) {
            Log.e(TAG, "Error stopping voice recognition", e);
        }

        stopForeground(true);
        stopSelf();
    }
    private class SpeechRecognitionListener implements RecognitionListener {
        @Override
        public void onReadyForSpeech(Bundle bundle) {
            Log.d(TAG, "Ready for speech");
        }

        @Override
        public void onBeginningOfSpeech() {
            Log.d(TAG, "Beginning of speech");
        }

        @Override
        public void onRmsChanged(float v) {
            // Not used
        }

        @Override
        public void onBufferReceived(byte[] bytes) {
            // Not used
        }

        @Override
        public void onEndOfSpeech() {
            Log.d(TAG, "End of speech");
            // Restart listening for continuous recognition
            if (isListening && speechRecognizer != null) {
                speechRecognizer.startListening(recognizerIntent);
            }
        }

        @Override
        public void onError(int errorCode) {
            String errorMessage;
            switch (errorCode) {
                case SpeechRecognizer.ERROR_AUDIO:
                    errorMessage = "Audio recording error";
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    errorMessage = "Client side error";
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    errorMessage = "Insufficient permissions";
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                    errorMessage = "Network error";
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    errorMessage = "Network timeout";
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    errorMessage = "No match";
                    break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    errorMessage = "RecognitionService busy";
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    errorMessage = "Server error";
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    errorMessage = "No speech input";
                    break;
                default:
                    errorMessage = "Unknown error";
                    break;
            }

            Log.e(TAG, "Error in speech recognition: " + errorMessage);

            // Restart listening unless it's a critical error
            if (isListening && speechRecognizer != null &&
                    errorCode != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                speechRecognizer.startListening(recognizerIntent);
            }
        }

        @Override
        public void onResults(Bundle results) {
            Log.d(TAG, "onResults");
            processRecognitionResults(results);
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            Log.d(TAG, "onPartialResults");
            processRecognitionResults(partialResults);
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
            // Not used
        }
    }

    private void processRecognitionResults(Bundle results) {
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) {
            for (String result : matches) {
                Log.d(TAG, "Speech recognized: " + result);

                // Check if the recognized speech contains any trigger phrases
                String lowerResult = result.toLowerCase();
                for (String trigger : triggerPhrases) {
                    if (lowerResult.contains(trigger)) {
                        Log.i(TAG, "Trigger phrase detected: " + trigger);
                        triggerSosAlert(result);
                        return;
                    }
                }
            }
        }
    }

    private void triggerSosAlert(String detectedPhrase) {
        // Start SOS alert process
        Intent sosIntent = new Intent(this, SosAlertService.class);
        sosIntent.setAction("TRIGGER_SOS");
        sosIntent.putExtra("TRIGGER_METHOD", "voice");
        sosIntent.putExtra("DETECTED_PHRASE", detectedPhrase);
        startService(sosIntent);

        // Provide audio feedback that SOS has been triggered
        // TODO: Add text-to-speech feedback

        // Temporarily stop listening to avoid multiple triggers
        if (speechRecognizer != null) {
            speechRecognizer.stopListening();

            // Resume listening after a delay
            new android.os.Handler().postDelayed(() -> {
                if (isListening && speechRecognizer != null) {
                    speechRecognizer.startListening(recognizerIntent);
                }
            }, 5000); // 5 seconds delay
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Voice Command Detection",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("Used for detecting voice commands like 'Help me'");

            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE
        );

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Voice Command Detection Active")
                .setContentText("Say 'Help me' or similar phrases for emergency")
                .setSmallIcon(R.drawable.ic_mic)
                .setContentIntent(pendingIntent)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .build();
    }
}

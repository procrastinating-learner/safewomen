package com.example.safewomen.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.safewomen.R;
import com.example.safewomen.models.entities.UserEntity;
import com.example.safewomen.repositories.AuthRepository;
import com.example.safewomen.viewmodels.AuthViewModel;

public class RegisterActivity extends AppCompatActivity {
    private static final String TAG = "RegisterActivity";
    private static final int SUCCESS_SPLASH_DURATION = 2000; // 2 seconds

    // UI components
    private EditText nameEditText;
    private EditText emailEditText;
    private EditText phoneEditText;
    private EditText passwordEditText;
    private EditText confirmPasswordEditText;
    private Button registerButton;
    private TextView loginTextView;
    private ProgressBar progressBar;
    private LinearLayout successSplashLayout;
    private LinearLayout registerFormLayout;

    // ViewModels
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize AuthRepository
        AuthRepository.init(getApplication());

        // Initialize UI components
        initializeViews();

        // Initialize ViewModels
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Set up observers
        setupObservers();

        // Set up click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        nameEditText = findViewById(R.id.editTextName);
        emailEditText = findViewById(R.id.editTextEmail);
        phoneEditText = findViewById(R.id.editTextPhone);
        passwordEditText = findViewById(R.id.editTextPassword);
        confirmPasswordEditText = findViewById(R.id.editTextConfirmPassword);
        registerButton = findViewById(R.id.buttonRegister);
        loginTextView = findViewById(R.id.textViewLogin);
        progressBar = findViewById(R.id.progressBar);
        successSplashLayout = findViewById(R.id.successSplashLayout);
        registerFormLayout = findViewById(R.id.registerFormLayout);
    }

    private void setupObservers() {
        // Observe loading state
        authViewModel.getIsLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            registerButton.setEnabled(!isLoading);
        });

        // Observe error messages
        authViewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(RegisterActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });

        // Observe current user
        authViewModel.getCurrentUser().observe(this, user -> {
            if (user != null) {
                // Registration successful, show success splash
                showSuccessSplash();
            }
        });
    }

    private void setupClickListeners() {
        // Register button click listener
        registerButton.setOnClickListener(v -> attemptRegistration());

        // Login text click listener
        loginTextView.setOnClickListener(v -> {
            finish(); // Go back to login activity
        });
    }

    private void attemptRegistration() {
        String name = nameEditText.getText().toString().trim();
        String email = emailEditText.getText().toString().trim();
        String phone = phoneEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        // Validate input
        if (name.isEmpty()) {
            nameEditText.setError("Name is required");
            nameEditText.requestFocus();
            return;
        }

        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            emailEditText.requestFocus();
            return;
        }

        if (phone.isEmpty()) {
            phoneEditText.setError("Phone number is required");
            phoneEditText.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            passwordEditText.requestFocus();
            return;
        }

        if (password.length() < 6) {
            passwordEditText.setError("Password must be at least 6 characters");
            passwordEditText.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Passwords do not match");
            confirmPasswordEditText.requestFocus();
            return;
        }

        // Attempt registration
        authViewModel.register(name, email, phone, password);
    }

    private void showSuccessSplash() {
        // Hide registration form
        registerFormLayout.setVisibility(View.GONE);

        // Show success splash
        successSplashLayout.setVisibility(View.VISIBLE);

        // After a delay, navigate back to login
        new Handler().postDelayed(() -> {
            // Log out the user (since we want them to log in explicitly)
            authViewModel.logout();

            // Navigate back to login
            Intent intent = new Intent(RegisterActivity.this, LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        }, SUCCESS_SPLASH_DURATION);
    }
}

package com.example.safewomen.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.safewomen.MainActivity;
import com.example.safewomen.R;
import com.example.safewomen.models.entities.UserEntity;
import com.example.safewomen.utils.PreferenceManager;
import com.example.safewomen.viewmodels.AuthViewModel;
import com.example.safewomen.repositories.AuthRepository;

public class LoginActivity extends AppCompatActivity {
    private static final String TAG = "LoginActivity";
    private static final int SPLASH_DURATION = 2000; // 2 seconds

    // UI components
    private View splashLayout;
    private View loginLayout;
    private EditText emailEditText;
    private EditText passwordEditText;
    private Button loginButton;
    private TextView registerTextView;
    private TextView forgotPasswordTextView;
    private ProgressBar progressBar;

    // ViewModels
    private AuthViewModel authViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Initialize PreferenceManager if not already initialized
        if (PreferenceManager.getInstance() == null) {
            PreferenceManager.init(getApplicationContext());
        }

        // Initialize AuthRepository
        AuthRepository.init(getApplication());

        // Initialize UI components
        initializeViews();

        // Initialize ViewModels
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Set up observers
        setupObservers();

        // Check if user is already logged in
        if (PreferenceManager.getInstance().isLoggedIn()) {
            // Show splash screen and load user data
            showSplashScreen();
        } else {
            // Show login form
            showLoginForm();
        }

        // Set up click listeners
        setupClickListeners();
    }

    private void initializeViews() {
        splashLayout = findViewById(R.id.splashLayout);
        loginLayout = findViewById(R.id.loginLayout);
        emailEditText = findViewById(R.id.editTextEmail);
        passwordEditText = findViewById(R.id.editTextPassword);
        loginButton = findViewById(R.id.buttonLogin);
        registerTextView = findViewById(R.id.textViewRegister);
        forgotPasswordTextView = findViewById(R.id.textViewForgotPassword);
        progressBar = findViewById(R.id.progressBar);
    }

    private void setupObservers() {
        // Observe loading state
        authViewModel.getIsLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            loginButton.setEnabled(!isLoading);
        });

        // Observe error messages
        authViewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });

        // Observe current user
        authViewModel.getCurrentUser().observe(this, user -> {
            if (user != null) {
                // User is logged in, navigate to main activity
                navigateToMainActivity();
            }
        });
    }

    private void setupClickListeners() {
        // Login button click listener
        loginButton.setOnClickListener(v -> attemptLogin());

        // Register text click listener
        registerTextView.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // Forgot password text click listener
        forgotPasswordTextView.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });
    }

    private void showSplashScreen() {
        splashLayout.setVisibility(View.VISIBLE);
        loginLayout.setVisibility(View.GONE);

        // Delay to show splash screen for a minimum time
        new Handler().postDelayed(() -> {
            // Check if user data is already loaded
            if (authViewModel.isLoggedIn()) {
                navigateToMainActivity();
            } else {
                // If not loaded yet, wait for the observer to trigger
                // The observer will navigate when user data is loaded
            }
        }, SPLASH_DURATION);
    }

    private void showLoginForm() {
        splashLayout.setVisibility(View.GONE);
        loginLayout.setVisibility(View.VISIBLE);
    }

    private void attemptLogin() {
        String email = emailEditText.getText().toString().trim();
        String password = passwordEditText.getText().toString().trim();

        // Validate input
        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            emailEditText.requestFocus();
            return;
        }

        if (password.isEmpty()) {
            passwordEditText.setError("Password is required");
            passwordEditText.requestFocus();
            return;
        }

        // Attempt login
        authViewModel.login(email, password);
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

package com.example.safewomen.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

import com.example.safewomen.R;
import com.example.safewomen.repositories.AuthRepository;
import com.example.safewomen.viewmodels.AuthViewModel;

public class ForgotPasswordActivity extends AppCompatActivity {
    private static final String TAG = "ForgotPasswordActivity";

    // UI components
    private EditText emailEditText;
    private EditText resetCodeEditText;
    private EditText newPasswordEditText;
    private EditText confirmPasswordEditText;
    private Button requestResetButton;
    private Button resetPasswordButton;
    private Button backToLoginButton;
    private ProgressBar progressBar;
    private LinearLayout requestResetLayout;
    private LinearLayout resetPasswordLayout;
    private LinearLayout successLayout;
    private TextView successMessage;

    // ViewModels
    private AuthViewModel authViewModel;

    // State tracking
    private boolean isResetRequested = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_forgot_password);

        // Initialize AuthRepository
        AuthRepository.init(getApplication());

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Initialize UI components
        initializeViews();

        // Initialize ViewModels
        authViewModel = new ViewModelProvider(this).get(AuthViewModel.class);

        // Set up observers
        setupObservers();

        // Set up click listeners
        setupClickListeners();

        // Show initial layout
        showRequestResetLayout();
    }

    private void initializeViews() {
        emailEditText = findViewById(R.id.editTextEmail);
        resetCodeEditText = findViewById(R.id.editTextResetCode);
        newPasswordEditText = findViewById(R.id.editTextNewPassword);
        confirmPasswordEditText = findViewById(R.id.editTextConfirmPassword);
        requestResetButton = findViewById(R.id.buttonRequestReset);
        resetPasswordButton = findViewById(R.id.buttonResetPassword);
        backToLoginButton = findViewById(R.id.buttonBackToLogin);
        progressBar = findViewById(R.id.progressBar);
        requestResetLayout = findViewById(R.id.requestResetLayout);
        resetPasswordLayout = findViewById(R.id.resetPasswordLayout);
        successLayout = findViewById(R.id.successLayout);
        successMessage = findViewById(R.id.textViewSuccessMessage);
    }

    private void setupObservers() {
        // Observe loading state
        authViewModel.getIsLoading().observe(this, isLoading -> {
            progressBar.setVisibility(isLoading ? View.VISIBLE : View.GONE);
            requestResetButton.setEnabled(!isLoading);
            resetPasswordButton.setEnabled(!isLoading);
        });

        // Observe error messages
        authViewModel.getErrorMessage().observe(this, errorMessage -> {
            if (errorMessage != null && !errorMessage.isEmpty()) {
                Toast.makeText(ForgotPasswordActivity.this, errorMessage, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void setupClickListeners() {
        // Request reset button click listener
        requestResetButton.setOnClickListener(v -> requestPasswordReset());

        // Reset password button click listener
        resetPasswordButton.setOnClickListener(v -> resetPassword());

        // Back to login button click listener
        backToLoginButton.setOnClickListener(v -> finish());
    }

    private void requestPasswordReset() {
        String email = emailEditText.getText().toString().trim();

        // Validate input
        if (email.isEmpty()) {
            emailEditText.setError("Email is required");
            emailEditText.requestFocus();
            return;
        }

        // Request password reset
        authViewModel.requestPasswordReset(email);

        // Store email for reset step
        isResetRequested = true;

        // Show reset password layout
        showResetPasswordLayout();

        // Show toast message
        Toast.makeText(this, "Reset code sent to your email", Toast.LENGTH_LONG).show();
    }

    private void resetPassword() {
        String email = emailEditText.getText().toString().trim();
        String resetCode = resetCodeEditText.getText().toString().trim();
        String newPassword = newPasswordEditText.getText().toString().trim();
        String confirmPassword = confirmPasswordEditText.getText().toString().trim();

        // Validate input
        if (resetCode.isEmpty()) {
            resetCodeEditText.setError("Reset code is required");
            resetCodeEditText.requestFocus();
            return;
        }

        if (newPassword.isEmpty()) {
            newPasswordEditText.setError("New password is required");
            newPasswordEditText.requestFocus();
            return;
        }

        if (newPassword.length() < 6) {
            newPasswordEditText.setError("Password must be at least 6 characters");
            newPasswordEditText.requestFocus();
            return;
        }

        if (!newPassword.equals(confirmPassword)) {
            confirmPasswordEditText.setError("Passwords do not match");
            confirmPasswordEditText.requestFocus();
            return;
        }

        // Reset password
        authViewModel.resetPassword(email, resetCode, newPassword);

        // Show success layout
        showSuccessLayout();
    }

    private void showRequestResetLayout() {
        requestResetLayout.setVisibility(View.VISIBLE);
        resetPasswordLayout.setVisibility(View.GONE);
        successLayout.setVisibility(View.GONE);
    }

    private void showResetPasswordLayout() {
        requestResetLayout.setVisibility(View.GONE);
        resetPasswordLayout.setVisibility(View.VISIBLE);
        successLayout.setVisibility(View.GONE);
    }

    private void showSuccessLayout() {
        requestResetLayout.setVisibility(View.GONE);
        resetPasswordLayout.setVisibility(View.GONE);
        successLayout.setVisibility(View.VISIBLE);
        successMessage.setText("Password has been reset successfully!");
    }
}

package com.recipevault.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseUser;
import com.recipevault.MainActivity;
import com.recipevault.R;
import com.recipevault.databinding.ActivitySignUpBinding;
import com.recipevault.service.AuthService;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SignUpActivity extends AppCompatActivity {
    private static final String TAG = "SignUpActivity";
    private ActivitySignUpBinding binding;
    @Inject
    AuthService authService;

    private ActivityResultLauncher<Intent> googleSignUpLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    handleGoogleSignUpResult(result.getData());
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignUpBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authService.initializeGoogleSignIn(this);

        setupClickListeners();
    }

    private void setupClickListeners() {
        binding.btnSignUp.setOnClickListener(v -> signUpWithEmail());
        binding.btnGoogleSignUp.setOnClickListener(v -> signUpWithGoogle());
        binding.tvSignIn.setOnClickListener(v -> finish());
    }

    private void signUpWithEmail() {
        String fullName = binding.etFullName.getText().toString().trim();
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();
        String confirmPassword = binding.etConfirmPassword.getText().toString().trim();

        // Validate inputs
        if (!validateInputs(fullName, email, password, confirmPassword)) {
            return;
        }

        // Show loading
        showLoading(true);

        // Create user with email and password
        authService.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = authService.getCurrentUser();
                        if (firebaseUser != null) {
                            Log.d(TAG, "Email sign up successful: " + firebaseUser.getEmail());

                            // Create user profile in Firestore
                            createUserProfileInFirestore(firebaseUser, fullName);
                        }
                    } else {
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(this, "Registration failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void createUserProfileInFirestore(FirebaseUser firebaseUser, String fullName) {
        // Create user profile in Firestore
        authService.createUserProfile(
                firebaseUser.getUid(),
                fullName,
                firebaseUser.getEmail(),
                firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : ""
        ).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Log.d(TAG, "User profile created successfully in Firestore");
                Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show();

                // Save user info in SharedPreferences
                saveUserToSharedPreferences(firebaseUser, fullName);

                // Navigate to main activity
                navigateToMainActivity();
            } else {
                Log.e(TAG, "Failed to create user profile", task.getException());
                Toast.makeText(this, "Failed to create user profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveUserToSharedPreferences(FirebaseUser firebaseUser, String fullName) {
        getSharedPreferences("RecipeVault", MODE_PRIVATE)
                .edit()
                .putBoolean("is_logged_in", true)
                .putString("user_id", firebaseUser.getUid())
                .putString("user_email", firebaseUser.getEmail())
                .putString("user_name", fullName)
                .apply();
    }

    private void signUpWithGoogle() {
        showLoading(true);
        Intent signInIntent = authService.getGoogleSignInIntent();
        googleSignUpLauncher.launch(signInIntent);
    }

    private void handleGoogleSignUpResult(Intent data) {
        try {
            authService.firebaseAuthWithGoogle(data)
                    .addOnCompleteListener(this, task -> {
                        showLoading(false);
                        if (task.isSuccessful()) {
                            FirebaseUser firebaseUser = authService.getCurrentUser();
                            if (firebaseUser != null) {
                                Log.d(TAG, "Google sign up successful: " + firebaseUser.getEmail());

                                // Create user profile in Firestore
                                createUserProfileInFirestore(firebaseUser, firebaseUser.getDisplayName());
                            }
                        } else {
                            Log.w(TAG, "Google sign up failed", task.getException());
                            Toast.makeText(this, "Google sign up failed: " + task.getException().getMessage(),
                                    Toast.LENGTH_LONG).show();
                        }
                    });
        } catch (Exception e) {
            showLoading(false);
            Log.e(TAG, "Google sign up error", e);
            Toast.makeText(this, "Google sign up error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validateInputs(String fullName, String email, String password, String confirmPassword) {
        if (!AuthService.isValidName(fullName)) {
            binding.etFullName.setError("Please enter your full name");
            binding.etFullName.requestFocus();
            return false;
        }

        if (!AuthService.isValidEmail(email)) {
            binding.etEmail.setError("Please enter a valid email address");
            binding.etEmail.requestFocus();
            return false;
        }

        if (!AuthService.isValidPassword(password)) {
            binding.etPassword.setError("Password must be at least 6 characters");
            binding.etPassword.requestFocus();
            return false;
        }

        if (!password.equals(confirmPassword)) {
            binding.etConfirmPassword.setError("Passwords do not match");
            binding.etConfirmPassword.requestFocus();
            return false;
        }

        return true;
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            binding.btnSignUp.setEnabled(false);
            binding.btnSignUp.setText("Creating account...");
            binding.btnGoogleSignUp.setEnabled(false);
        } else {
            binding.btnSignUp.setEnabled(true);
            binding.btnSignUp.setText("Sign Up");
            binding.btnGoogleSignUp.setEnabled(true);
        }
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
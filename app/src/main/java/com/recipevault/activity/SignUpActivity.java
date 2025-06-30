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

public class SignUpActivity extends AppCompatActivity {
    private static final String TAG = "SignUpActivity";
    private ActivitySignUpBinding binding;
    private AuthService authService;

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

        authService = AuthService.getInstance();
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

        if (!validateInput(fullName, email, password, confirmPassword)) {
            return;
        }

        showLoading(true);
        authService.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "createUserWithEmail:success");
                        FirebaseUser user = authService.getCurrentUser();

                        if (user != null) {
                            // Create user profile
                            authService.createUserProfile(user.getUid(), fullName, email, null)
                                    .addOnCompleteListener(profileTask -> {
                                        showLoading(false);
                                        if (profileTask.isSuccessful()) {
                                            Log.d(TAG, "User profile created");
                                            Toast.makeText(SignUpActivity.this, "Account created successfully!",
                                                    Toast.LENGTH_SHORT).show();
                                            navigateToMain();
                                        } else {
                                            Log.w(TAG, "Failed to create user profile", profileTask.getException());
                                            Toast.makeText(SignUpActivity.this, "Failed to create profile",
                                                    Toast.LENGTH_SHORT).show();
                                        }
                                    });
                        }
                    } else {
                        showLoading(false);
                        Log.w(TAG, "createUserWithEmail:failure", task.getException());
                        Toast.makeText(SignUpActivity.this, "Registration failed: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void signUpWithGoogle() {
        Intent signUpIntent = authService.getGoogleSignInIntent();
        googleSignUpLauncher.launch(signUpIntent);
    }

    private void handleGoogleSignUpResult(Intent data) {
        showLoading(true);
        try {
            authService.firebaseAuthWithGoogle(data)
                    .addOnCompleteListener(this, task -> {
                        showLoading(false);
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signUpWithCredential:success");
                            FirebaseUser user = authService.getCurrentUser();

                            if (user != null) {
                                String displayName = user.getDisplayName();
                                String email = user.getEmail();
                                String photoUrl = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null;

                                authService.createUserProfile(user.getUid(), displayName, email, photoUrl)
                                        .addOnCompleteListener(profileTask -> {
                                            if (profileTask.isSuccessful()) {
                                                Log.d(TAG, "User profile created/updated");
                                                Toast.makeText(SignUpActivity.this, "Account created successfully!",
                                                        Toast.LENGTH_SHORT).show();
                                            }
                                            navigateToMain();
                                        });
                            } else {
                                navigateToMain();
                            }
                        } else {
                            Log.w(TAG, "signUpWithCredential:failure", task.getException());
                            Toast.makeText(SignUpActivity.this, "Google sign up failed: " +
                                    task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        } catch (Exception e) {
            showLoading(false);
            Log.w(TAG, "Google sign up exception", e);
            Toast.makeText(this, "Google sign up failed", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validateInput(String fullName, String email, String password, String confirmPassword) {
        boolean isValid = true;

        if (!AuthService.isValidName(fullName)) {
            binding.tilFullName.setError("Please enter your full name");
            isValid = false;
        } else {
            binding.tilFullName.setError(null);
        }

        if (!AuthService.isValidEmail(email)) {
            binding.tilEmail.setError(getString(R.string.error_invalid_email));
            isValid = false;
        } else {
            binding.tilEmail.setError(null);
        }

        if (!AuthService.isValidPassword(password)) {
            binding.tilPassword.setError(getString(R.string.error_weak_password));
            isValid = false;
        } else {
            binding.tilPassword.setError(null);
        }

        if (!password.equals(confirmPassword)) {
            binding.tilConfirmPassword.setError(getString(R.string.error_password_mismatch));
            isValid = false;
        } else {
            binding.tilConfirmPassword.setError(null);
        }

        return isValid;
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnSignUp.setEnabled(!show);
        binding.btnGoogleSignUp.setEnabled(!show);
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
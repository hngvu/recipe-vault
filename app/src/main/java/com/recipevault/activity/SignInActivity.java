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
import com.recipevault.databinding.ActivitySignInBinding;
import com.recipevault.service.AuthService;

public class SignInActivity extends AppCompatActivity {
    private static final String TAG = "SignInActivity";
    private ActivitySignInBinding binding;
    private AuthService authService;

    private ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    handleGoogleSignInResult(result.getData());
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        authService = AuthService.getInstance();
        authService.initializeGoogleSignIn(this);

        setupClickListeners();

        // Check if user is already signed in
        if (authService.isUserSignedIn()) {
            navigateToMain();
        }
    }

    private void setupClickListeners() {
        binding.btnSignIn.setOnClickListener(v -> signInWithEmail());
        binding.btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());
        binding.tvSignUp.setOnClickListener(v -> navigateToSignUp());
        binding.tvForgotPassword.setOnClickListener(v -> showForgotPasswordDialog());
    }

    private void signInWithEmail() {
        String email = binding.etEmail.getText().toString().trim();
        String password = binding.etPassword.getText().toString().trim();

        if (!validateInput(email, password)) {
            return;
        }

        showLoading(true);
        authService.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    showLoading(false);
                    if (task.isSuccessful()) {
                        Log.d(TAG, "signInWithEmail:success");
                        FirebaseUser user = authService.getCurrentUser();
                        navigateToMain();
                    } else {
                        Log.w(TAG, "signInWithEmail:failure", task.getException());
                        Toast.makeText(SignInActivity.this, "Authentication failed: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void signInWithGoogle() {
        Intent signInIntent = authService.getGoogleSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void handleGoogleSignInResult(Intent data) {
        showLoading(true);
        try {
            authService.firebaseAuthWithGoogle(data)
                    .addOnCompleteListener(this, task -> {
                        showLoading(false);
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInWithCredential:success");
                            FirebaseUser user = authService.getCurrentUser();

                            // Create user profile if new user
                            if (user != null) {
                                String displayName = user.getDisplayName();
                                String email = user.getEmail();
                                String photoUrl = user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : null;

                                authService.createUserProfile(user.getUid(), displayName, email, photoUrl)
                                        .addOnCompleteListener(profileTask -> {
                                            if (profileTask.isSuccessful()) {
                                                Log.d(TAG, "User profile created/updated");
                                            }
                                            navigateToMain();
                                        });
                            } else {
                                navigateToMain();
                            }
                        } else {
                            Log.w(TAG, "signInWithCredential:failure", task.getException());
                            Toast.makeText(SignInActivity.this, "Google sign in failed: " +
                                    task.getException().getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
        } catch (Exception e) {
            showLoading(false);
            Log.w(TAG, "Google sign in exception", e);
            Toast.makeText(this, "Google sign in failed", Toast.LENGTH_SHORT).show();
        }
    }

    private boolean validateInput(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.error_empty_fields), Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!AuthService.isValidEmail(email)) {
            binding.tilEmail.setError(getString(R.string.error_invalid_email));
            return false;
        } else {
            binding.tilEmail.setError(null);
        }

        if (!AuthService.isValidPassword(password)) {
            binding.tilPassword.setError(getString(R.string.error_weak_password));
            return false;
        } else {
            binding.tilPassword.setError(null);
        }

        return true;
    }

    private void showForgotPasswordDialog() {
        String email = binding.etEmail.getText().toString().trim();
        if (email.isEmpty()) {
            Toast.makeText(this, "Please enter your email first", Toast.LENGTH_SHORT).show();
            return;
        }

        authService.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(SignInActivity.this, "Password reset email sent", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(SignInActivity.this, "Failed to send reset email: " +
                                task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.btnSignIn.setEnabled(!show);
        binding.btnGoogleSignIn.setEnabled(!show);
    }

    private void navigateToSignUp() {
        Intent intent = new Intent(this, SignUpActivity.class);
        startActivity(intent);
    }

    private void navigateToMain() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
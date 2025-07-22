package com.recipevault.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.recipevault.R;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class SettingsActivity extends AppCompatActivity {

    private static final String TAG = "SettingsActivity";

    private MaterialToolbar toolbar;
    private SwitchMaterial switchNotifications;
    private SwitchMaterial switchDarkMode;
    private SwitchMaterial switchAutoBackup;
    private LinearLayout btnChangePassword;
    private LinearLayout btnPrivacyPolicy;
    private LinearLayout btnTermsOfService;
    private LinearLayout btnAbout;
    private LinearLayout btnDeleteAccount;

    @Inject
    FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Check authentication
        if (!isUserAuthenticated()) {
            redirectToSignIn();
            return;
        }

        initViews();
        setupToolbar();
        setupClickListeners();
        loadSettings();
    }

    private boolean isUserAuthenticated() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        return currentUser != null;
    }

    private void redirectToSignIn() {
        Intent intent = new Intent(this, SignInActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        switchNotifications = findViewById(R.id.switch_notifications);
        switchDarkMode = findViewById(R.id.switch_dark_mode);
        switchAutoBackup = findViewById(R.id.switch_auto_backup);
        btnChangePassword = findViewById(R.id.btn_change_password);
        btnPrivacyPolicy = findViewById(R.id.btn_privacy_policy);
        btnTermsOfService = findViewById(R.id.btn_terms_of_service);
        btnAbout = findViewById(R.id.btn_about);
        btnDeleteAccount = findViewById(R.id.btn_delete_account);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    private void setupClickListeners() {
        // Switch listeners
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveNotificationSetting(isChecked);
            if (isChecked) {
                Toast.makeText(this, "Notifications enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Notifications disabled", Toast.LENGTH_SHORT).show();
            }
        });

        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveDarkModeSetting(isChecked);
            if (isChecked) {
                Toast.makeText(this, "Dark mode enabled (restart app to apply)", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Light mode enabled (restart app to apply)", Toast.LENGTH_SHORT).show();
            }
        });

        switchAutoBackup.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveAutoBackupSetting(isChecked);
            if (isChecked) {
                Toast.makeText(this, "Auto backup enabled", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Auto backup disabled", Toast.LENGTH_SHORT).show();
            }
        });

        // Button listeners
        btnChangePassword.setOnClickListener(v -> changePassword());
        btnPrivacyPolicy.setOnClickListener(v -> showPrivacyPolicy());
        btnTermsOfService.setOnClickListener(v -> showTermsOfService());
        btnAbout.setOnClickListener(v -> showAbout());
        btnDeleteAccount.setOnClickListener(v -> showDeleteAccountDialog());
    }

    private void loadSettings() {
        SharedPreferences prefs = getSharedPreferences("RecipeVault_Settings", MODE_PRIVATE);

        boolean notificationsEnabled = prefs.getBoolean("notifications_enabled", true);
        boolean darkModeEnabled = prefs.getBoolean("dark_mode_enabled", false);
        boolean autoBackupEnabled = prefs.getBoolean("auto_backup_enabled", true);

        switchNotifications.setChecked(notificationsEnabled);
        switchDarkMode.setChecked(darkModeEnabled);
        switchAutoBackup.setChecked(autoBackupEnabled);
    }

    private void saveNotificationSetting(boolean enabled) {
        SharedPreferences prefs = getSharedPreferences("RecipeVault_Settings", MODE_PRIVATE);
        prefs.edit().putBoolean("notifications_enabled", enabled).apply();
    }

    private void saveDarkModeSetting(boolean enabled) {
        SharedPreferences prefs = getSharedPreferences("RecipeVault_Settings", MODE_PRIVATE);
        prefs.edit().putBoolean("dark_mode_enabled", enabled).apply();
    }

    private void saveAutoBackupSetting(boolean enabled) {
        SharedPreferences prefs = getSharedPreferences("RecipeVault_Settings", MODE_PRIVATE);
        prefs.edit().putBoolean("auto_backup_enabled", enabled).apply();
    }

    private void changePassword() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) return;

        String email = user.getEmail();
        if (email != null) {
            firebaseAuth.sendPasswordResetEmail(email)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Password reset email sent to " + email,
                                    Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(this, "Failed to send password reset email",
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            Toast.makeText(this, "No email associated with this account",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void showPrivacyPolicy() {
        new AlertDialog.Builder(this)
                .setTitle("Privacy Policy")
                .setMessage("Recipe Vault Privacy Policy\n\n" +
                        "Your privacy is important to us. This privacy policy explains how we collect, " +
                        "use, and protect your information when you use our app.\n\n" +
                        "Information We Collect:\n" +
                        "• Account information (email, name)\n" +
                        "• Recipes you create and save\n" +
                        "• App usage data\n\n" +
                        "How We Use Your Information:\n" +
                        "• To provide and improve our services\n" +
                        "• To sync your data across devices\n" +
                        "• To send important updates\n\n" +
                        "We do not sell your personal information to third parties.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showTermsOfService() {
        new AlertDialog.Builder(this)
                .setTitle("Terms of Service")
                .setMessage("Recipe Vault Terms of Service\n\n" +
                        "By using Recipe Vault, you agree to these terms:\n\n" +
                        "1. You are responsible for your account and content\n" +
                        "2. You must not misuse our services\n" +
                        "3. We may update these terms from time to time\n" +
                        "4. You can terminate your account at any time\n\n" +
                        "Premium Features:\n" +
                        "• Premium subscriptions are billed monthly or yearly\n" +
                        "• You can cancel your subscription at any time\n" +
                        "• Refunds are subject to our refund policy\n\n" +
                        "Contact us at support@recipevault.com for questions.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showAbout() {
        new AlertDialog.Builder(this)
                .setTitle("About Recipe Vault")
                .setMessage("Recipe Vault v1.0\n\n" +
                        "Your personal recipe organizer and meal planning companion.\n\n" +
                        "Features:\n" +
                        "• Save and organize recipes\n" +
                        "• Rate and review recipes\n" +
                        "• Set cooking reminders\n" +
                        "• Share recipes with friends\n" +
                        "• Premium meal planning tools\n\n" +
                        "Developed with ❤️ for food lovers everywhere.\n\n" +
                        "© 2025 Recipe Vault. All rights reserved.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showDeleteAccountDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account?\n\n" +
                        "This action will:\n" +
                        "• Permanently delete all your recipes\n" +
                        "• Remove all your account data\n" +
                        "• Cancel any active subscriptions\n\n" +
                        "This action cannot be undone.")
                .setPositiveButton("Delete", (dialog, which) -> deleteAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAccount() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) return;

        // Show confirmation dialog
        new AlertDialog.Builder(this)
                .setTitle("Final Confirmation")
                .setMessage("This will permanently delete your account and all data. Are you absolutely sure?")
                .setPositiveButton("Yes, Delete Forever", (dialog, which) -> {
                    // Delete the user account
                    user.delete()
                            .addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    // Clear local data
                                    clearAllLocalData();

                                    Toast.makeText(this, "Account deleted successfully",
                                            Toast.LENGTH_SHORT).show();

                                    // Redirect to sign in
                                    Intent intent = new Intent(this, SignInActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                    finish();
                                } else {
                                    Toast.makeText(this, "Failed to delete account. Please try again.",
                                            Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void clearAllLocalData() {
        // Clear all SharedPreferences
        SharedPreferences recipePrefs = getSharedPreferences("RecipeVault", MODE_PRIVATE);
        SharedPreferences settingsPrefs = getSharedPreferences("RecipeVault_Settings", MODE_PRIVATE);

        recipePrefs.edit().clear().apply();
        settingsPrefs.edit().clear().apply();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
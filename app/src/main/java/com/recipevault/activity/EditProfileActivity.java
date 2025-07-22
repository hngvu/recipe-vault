package com.recipevault.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.recipevault.R;
import com.recipevault.service.FirestoreService;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class EditProfileActivity extends AppCompatActivity {

    private static final String TAG = "EditProfileActivity";
    private static final int PICK_IMAGE_REQUEST = 1;

    private MaterialToolbar toolbar;
    private ImageView ivProfilePicture;
    private TextInputEditText etFullName, etBio, etEmail;
    private MaterialButton btnSaveChanges, btnChangePhoto;

    private Uri selectedImageUri;

    @Inject
    FirebaseAuth firebaseAuth;
    @Inject
    FirestoreService firestoreService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Check authentication
        if (!isUserAuthenticated()) {
            redirectToSignIn();
            return;
        }

        initViews();
        setupToolbar();
        setupClickListeners();
        loadUserData();
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
        ivProfilePicture = findViewById(R.id.iv_profile_picture);
        etFullName = findViewById(R.id.et_full_name);
        etBio = findViewById(R.id.et_bio);
        etEmail = findViewById(R.id.et_email);
        btnSaveChanges = findViewById(R.id.btn_save_changes);
        btnChangePhoto = findViewById(R.id.btn_change_photo);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    private void setupClickListeners() {
        btnChangePhoto.setOnClickListener(v -> openImagePicker());
        btnSaveChanges.setOnClickListener(v -> saveProfileChanges());
    }

    private void loadUserData() {
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if (firebaseUser != null) {
            etFullName.setText(firebaseUser.getDisplayName());
            etEmail.setText(firebaseUser.getEmail());

            // Load profile picture
            if (firebaseUser.getPhotoUrl() != null) {
                Glide.with(this)
                        .load(firebaseUser.getPhotoUrl())
                        .placeholder(R.drawable.ic_person)
                        .error(R.drawable.ic_person)
                        .into(ivProfilePicture);
            }

            // Load bio from Firestore
            String userId = firebaseUser.getUid();
            firestoreService.getUser(userId)
                    .addOnSuccessListener(documentSnapshot -> {
                        if (documentSnapshot.exists()) {
                            String bio = documentSnapshot.getString("bio");
                            if (bio != null) {
                                etBio.setText(bio);
                            }
                        }
                    })
                    .addOnFailureListener(e -> {
                        // Handle error silently or show message
                    });
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Profile Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            ivProfilePicture.setImageURI(selectedImageUri);
        }
    }

    private void saveProfileChanges() {
        String fullName = etFullName.getText().toString().trim();
        String bio = etBio.getText().toString().trim();

        if (fullName.isEmpty()) {
            etFullName.setError("Name is required");
            return;
        }

        // Show loading state
        btnSaveChanges.setEnabled(false);
        btnSaveChanges.setText("Saving...");

        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user == null) return;

        // Update Firebase Auth profile
        UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                .setDisplayName(fullName)
                .build();

        user.updateProfile(profileUpdates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // Update Firestore user document
                        updateFirestoreUser(user.getUid(), fullName, bio);
                    } else {
                        handleSaveError("Failed to update profile");
                    }
                });
    }

    private void updateFirestoreUser(String userId, String fullName, String bio) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("username", fullName);
        updates.put("bio", bio);

        firestoreService.updateUser(userId, updates)
                .addOnSuccessListener(aVoid -> {
                    // Update SharedPreferences
                    updateSharedPreferences(fullName);

                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    finish(); // Return to profile activity
                })
                .addOnFailureListener(e -> {
                    handleSaveError("Failed to save profile data");
                });
    }

    private void updateSharedPreferences(String fullName) {
        SharedPreferences prefs = getSharedPreferences("RecipeVault", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("user_name", fullName);
        editor.apply();
    }

    private void handleSaveError(String message) {
        btnSaveChanges.setEnabled(true);
        btnSaveChanges.setText("Save Changes");
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
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
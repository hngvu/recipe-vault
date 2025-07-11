package com.recipevault.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.recipevault.MainActivity;
import com.recipevault.R;
import com.recipevault.model.User;
import com.recipevault.service.FirestoreService;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;


@AndroidEntryPoint
public class SignInActivity extends AppCompatActivity {

    private static final String TAG = "SignInActivity";
    private GoogleSignInClient googleSignInClient;

    @Inject
    FirebaseAuth firebaseAuth;
    @Inject
    FirestoreService firestoreService;
    private Button btnSignIn;

    private final ActivityResultLauncher<Intent> signInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    Intent data = result.getData();
                    handleSignInResult(data);
                } else {
                    Log.e(TAG, "Sign in failed with result code: " + result.getResultCode());
                    Toast.makeText(this, "Sign in cancelled", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        initViews();
        configureGoogleSignIn();
        setupClickListeners();
        checkIfAlreadySignedIn();
    }

    private void initViews() {
        btnSignIn = findViewById(R.id.btnGoogleSignIn);
    }

    private void configureGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) // Add this line
                .requestEmail()
                .requestProfile()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setupClickListeners() {
        btnSignIn.setOnClickListener(v -> signIn());
    }

    private void checkIfAlreadySignedIn() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "User already signed in: " + currentUser.getEmail());

            // Set SharedPreferences to indicate user is logged in
            SharedPreferences prefs = getSharedPreferences("RecipeVault", MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putBoolean("is_logged_in", true);
            editor.putString("user_id", currentUser.getUid()); // Use Firebase UID
            editor.putString("user_email", currentUser.getEmail());
            editor.putString("user_name", currentUser.getDisplayName());
            editor.apply();

            navigateToMainActivity();
        }
    }

    private void signIn() {
        btnSignIn.setEnabled(false);
        btnSignIn.setText("Signing in...");

        Intent signInIntent = googleSignInClient.getSignInIntent();
        signInLauncher.launch(signInIntent);
    }

    private void handleSignInResult(Intent data) {
        try {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            GoogleSignInAccount account = task.getResult(ApiException.class);

            Log.d(TAG, "Google sign in successful: " + account.getEmail());

            // Sign in to Firebase with Google credentials
            signInToFirebase(account);

        } catch (ApiException e) {
            Log.e(TAG, "Google sign in failed with code: " + e.getStatusCode(), e);
            String errorMessage = getErrorMessage(e.getStatusCode());
            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
            resetSignInButton();
        }
    }

    private void signInToFirebase(GoogleSignInAccount account) {
        Log.d(TAG, "Signing in to Firebase...");

        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Firebase sign in successful");
                        FirebaseUser user = firebaseAuth.getCurrentUser();

                        if (user != null) {
                            Toast.makeText(this, "Welcome " + user.getDisplayName(), Toast.LENGTH_SHORT).show();

                            // Save user info in SharedPreferences
                            SharedPreferences prefs = getSharedPreferences("RecipeVault", MODE_PRIVATE);
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putBoolean("is_logged_in", true);
                            editor.putString("user_id", user.getUid()); // Use Firebase UID
                            editor.putString("user_email", user.getEmail());
                            editor.putString("user_name", user.getDisplayName());
                            editor.apply();

                            // Create or update user document in Firestore
                            createOrUpdateUserInFirestore(user);

                            navigateToMainActivity();
                        }
                    } else {
                        Log.e(TAG, "Firebase sign in failed", task.getException());
                        Toast.makeText(this, "Authentication failed: " + task.getException().getMessage(),
                                Toast.LENGTH_LONG).show();
                        resetSignInButton();
                    }
                });
    }

    private void createOrUpdateUserInFirestore(FirebaseUser firebaseUser) {

        // Check if user already exists in Firestore
        firestoreService.getUser(firebaseUser.getUid())
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Log.d(TAG, "User already exists in Firestore");
                        // User exists, optionally update last login time
                        updateUserLastLogin(firebaseUser.getUid());
                    } else {
                        Log.d(TAG, "Creating new user in Firestore");
                        // User doesn't exist, create new user document
                        createNewUserDocument(firebaseUser);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to check user existence", e);
                    // If check fails, try to create user anyway
                    createNewUserDocument(firebaseUser);
                });
    }

    private void createNewUserDocument(FirebaseUser firebaseUser) {
        // Create User object with Firebase user data
        User user = new User();
        user.setUserId(firebaseUser.getUid());
        user.setUsername(firebaseUser.getDisplayName() != null ? firebaseUser.getDisplayName() : "User");
        user.setEmail(firebaseUser.getEmail());
        user.setAvatarUrl(firebaseUser.getPhotoUrl() != null ? firebaseUser.getPhotoUrl().toString() : "");
        user.setBio("");
        user.setPremium(false);
        user.setCreatedAt(System.currentTimeMillis());

        // Save to Firestore
        firestoreService.createUser(user)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User document created successfully in Firestore");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create user document in Firestore", e);
                    Toast.makeText(this, "Failed to save user data", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateUserLastLogin(String userId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("lastLoginAt", System.currentTimeMillis());

        firestoreService.updateUser(userId, updates)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "User last login updated");
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update user last login", e);
                });
    }

    private String getErrorMessage(int statusCode) {
        switch (statusCode) {
            case 7: // NETWORK_ERROR
                return "Network error. Please check your connection.";
            case 8: // INTERNAL_ERROR
                return "Internal error. Please try again.";
            case 10: // DEVELOPER_ERROR
                return "Configuration error. Please contact support.";
            case 12500: // SIGN_IN_CANCELLED
                return "Sign in was cancelled.";
            case 12501: // SIGN_IN_CURRENTLY_IN_PROGRESS
                return "Sign in already in progress.";
            case 12502: // SIGN_IN_FAILED
                return "Sign in failed. Please try again.";
            default:
                return "Sign in failed. Error code: " + statusCode;
        }
    }

    private void resetSignInButton() {
        btnSignIn.setEnabled(true);
        btnSignIn.setText("Sign in with Google");
    }

    private void navigateToMainActivity() {
        Intent intent = new Intent(this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Use the same authentication check as onCreate to avoid loops
        checkIfAlreadySignedIn();
    }
}
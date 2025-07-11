package com.recipevault.service;

import android.app.Activity;
import android.content.Intent;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.recipevault.R;
import com.recipevault.model.User;

import javax.inject.Inject;
import javax.inject.Singleton;


@Singleton
public class AuthService {
    private FirebaseAuth mAuth;
    private GoogleSignInClient googleSignInClient;
    private FirestoreService firestoreService;

    public static final int RC_SIGN_IN = 9001;

    @Inject
    public AuthService(FirebaseAuth mAuth, FirestoreService firestoreService) {
        this.mAuth = mAuth;
        this.firestoreService = firestoreService;
    }


    public void initializeGoogleSignIn(Activity activity) {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(activity.getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(activity, gso);
    }

    public FirebaseUser getCurrentUser() {
        return mAuth.getCurrentUser();
    }

    public boolean isUserSignedIn() {
        return getCurrentUser() != null;
    }

    public String getCurrentUserId() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    // Email/Password Sign In
    public Task<AuthResult> signInWithEmailAndPassword(String email, String password) {
        return mAuth.signInWithEmailAndPassword(email, password);
    }

    // Email/Password Sign Up
    public Task<AuthResult> createUserWithEmailAndPassword(String email, String password) {
        return mAuth.createUserWithEmailAndPassword(email, password);
    }

    // Google Sign In Intent
    public Intent getGoogleSignInIntent() {
        return googleSignInClient.getSignInIntent();
    }

    // Handle Google Sign In Result
    public Task<AuthResult> firebaseAuthWithGoogle(Intent data) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        try {
            GoogleSignInAccount account = task.getResult(ApiException.class);
            AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
            return mAuth.signInWithCredential(credential);
        } catch (ApiException e) {
            throw new RuntimeException("Google sign in failed", e);
        }
    }

    // Create user profile after successful authentication
    public Task<Void> createUserProfile(String userId, String username, String email, String avatarUrl) {
        User user = new User(userId, username, email);
        if (avatarUrl != null) {
            user.setAvatarUrl(avatarUrl);
        }
        return firestoreService.createUser(user);
    }

    // Send password reset email
    public Task<Void> sendPasswordResetEmail(String email) {
        return mAuth.sendPasswordResetEmail(email);
    }

    // Sign out
    public void signOut() {
        mAuth.signOut();
        if (googleSignInClient != null) {
            googleSignInClient.signOut();
        }
    }

    // Input validation
    public static boolean isValidEmail(String email) {
        return email != null && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    public static boolean isValidName(String name) {
        return name != null && !name.trim().isEmpty();
    }
}
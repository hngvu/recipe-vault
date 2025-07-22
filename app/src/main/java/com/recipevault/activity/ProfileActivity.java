package com.recipevault.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.recipevault.MainActivity;
import com.recipevault.R;
import com.recipevault.service.PremiumService;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class ProfileActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private TextView tvUserName, tvUserEmail;
    private LinearLayout btnSignOut, btnEditProfile, btnMyRecipes, btnSettings, btnUpgradePremium;
    private Chip chipPremiumStatus;
    private MaterialButton btnManagePremium;
    private GoogleSignInClient googleSignInClient;

    @Inject
    FirebaseAuth firebaseAuth;
    @Inject
    PremiumService premiumService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        initViews();
        setupGoogleSignIn();
        setupBottomNavigation();
        setupClickListeners();
        loadUserData();
        checkPremiumStatus();
    }

    private void initViews() {
        bottomNavigation = findViewById(R.id.bottom_navigation);
        tvUserName = findViewById(R.id.tv_user_name);
        tvUserEmail = findViewById(R.id.tv_user_email);
        btnSignOut = findViewById(R.id.btn_sign_out);
        btnEditProfile = findViewById(R.id.btn_edit_profile);
        btnMyRecipes = findViewById(R.id.btn_my_recipes);
        btnSettings = findViewById(R.id.btn_settings);
        btnUpgradePremium = findViewById(R.id.btn_upgrade_premium);
        chipPremiumStatus = findViewById(R.id.chip_premium_status);
        btnManagePremium = findViewById(R.id.btn_manage_premium);
    }

    private void setupGoogleSignIn() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .requestProfile()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    private void setupClickListeners() {
        btnSignOut.setOnClickListener(v -> signOut());

        btnEditProfile.setOnClickListener(v -> {
            Intent intent = new Intent(this, EditProfileActivity.class);
            startActivity(intent);
        });

        btnMyRecipes.setOnClickListener(v ->
                Toast.makeText(this, "My Recipes feature coming soon", Toast.LENGTH_SHORT).show());

        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
        });

        btnUpgradePremium.setOnClickListener(v -> {
            Intent intent = new Intent(this, PremiumUpgradeActivity.class);
            startActivity(intent);
        });

        btnManagePremium.setOnClickListener(v -> {
            Intent intent = new Intent(this, PremiumUpgradeActivity.class);
            startActivity(intent);
        });
    }

    private void loadUserData() {
        // Load from Firebase Auth first, then fallback to SharedPreferences
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();

        if (firebaseUser != null) {
            tvUserName.setText(firebaseUser.getDisplayName());
            tvUserEmail.setText(firebaseUser.getEmail());
        } else {
            // Fallback to SharedPreferences
            SharedPreferences prefs = getSharedPreferences("RecipeVault", MODE_PRIVATE);
            String userName = prefs.getString("user_name", "Unknown User");
            String userEmail = prefs.getString("user_email", "unknown@email.com");

            tvUserName.setText(userName);
            tvUserEmail.setText(userEmail);
        }
    }

    private void checkPremiumStatus() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        premiumService.isPremiumUser(userId,
                isPremium -> {
                    updatePremiumUI(isPremium);
                },
                error -> {
                    // Handle error - default to non-premium
                    updatePremiumUI(false);
                }
        );
    }

    private void updatePremiumUI(boolean isPremium) {
        if (isPremium) {
            // User is premium
            chipPremiumStatus.setText("Premium Member");
            chipPremiumStatus.setChipBackgroundColorResource(R.color.primary_orange);
            chipPremiumStatus.setTextColor(getColor(R.color.white));
            chipPremiumStatus.setVisibility(android.view.View.VISIBLE);

            btnUpgradePremium.setVisibility(android.view.View.GONE);
            btnManagePremium.setVisibility(android.view.View.VISIBLE);
            btnManagePremium.setText("Manage Premium");
        } else {
            // User is not premium
            chipPremiumStatus.setText("Free User");
            chipPremiumStatus.setChipBackgroundColorResource(R.color.gray_medium);
            chipPremiumStatus.setTextColor(getColor(R.color.white));
            chipPremiumStatus.setVisibility(android.view.View.VISIBLE);

            btnUpgradePremium.setVisibility(android.view.View.VISIBLE);
            btnManagePremium.setVisibility(android.view.View.GONE);
        }
    }

    private void signOut() {
        // Sign out from Firebase
        firebaseAuth.signOut();

        // Sign out from Google
        googleSignInClient.signOut()
                .addOnCompleteListener(this, task -> {
                    // Clear SharedPreferences
                    SharedPreferences prefs = getSharedPreferences("RecipeVault", MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.clear();
                    editor.apply();

                    Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show();

                    // Navigate to SignInActivity
                    Intent intent = new Intent(this, SignInActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                });
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_profile);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_search) {
                startActivity(new Intent(this, SearchActivity.class));
                return true;
            } else if (itemId == R.id.nav_add) {
                startActivity(new Intent(this, AddRecipeActivity.class));
                return true;
            } else if (itemId == R.id.nav_favorites) {
                startActivity(new Intent(this, FavoritesActivity.class));
                return true;
            } else if (itemId == R.id.nav_profile) {
                return true; // Already on profile
            }

            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Refresh user data and premium status when returning to profile
        loadUserData();
        checkPremiumStatus();
    }
}
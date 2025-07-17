package com.recipevault.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.recipevault.R;
import com.recipevault.model.Premium;
import com.recipevault.service.FirestoreService;
import com.recipevault.service.PremiumService;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class PremiumUpgradeActivity extends AppCompatActivity {

    private static final String TAG = "PremiumUpgradeActivity";

    private MaterialToolbar toolbar;
    private MaterialCardView cardMonthlyPlan;
    private MaterialCardView cardYearlyPlan;
    private MaterialButton btnUpgradePremium;
    private MaterialButton btnRestorePurchases;
    private View progressBar;

    private String selectedPlan = "monthly"; // Default to monthly
    private boolean isProcessing = false;

    @Inject
    FirebaseAuth firebaseAuth;
    @Inject
    FirestoreService firestoreService;
    @Inject
    PremiumService premiumService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_premium_upgrade);

        // Check if user is authenticated
        if (!isUserAuthenticated()) {
            redirectToSignIn();
            return;
        }

        initViews();
        setupToolbar();
        setupPlanSelection();
        setupClickListeners();
        checkCurrentPremiumStatus();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        cardMonthlyPlan = findViewById(R.id.card_monthly_plan);
        cardYearlyPlan = findViewById(R.id.card_yearly_plan);
        btnUpgradePremium = findViewById(R.id.btn_upgrade_premium);
        btnRestorePurchases = findViewById(R.id.btn_restore_purchases);
        progressBar = findViewById(R.id.progress_bar);
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

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
    }

    private void setupPlanSelection() {
        // Set monthly as default selected
        selectPlan("monthly");
    }

    private void setupClickListeners() {
        cardMonthlyPlan.setOnClickListener(v -> selectPlan("monthly"));
        cardYearlyPlan.setOnClickListener(v -> selectPlan("yearly"));
        btnUpgradePremium.setOnClickListener(v -> processPremiumUpgrade());
        btnRestorePurchases.setOnClickListener(v -> restorePurchases());
    }

    private void selectPlan(String planType) {
        selectedPlan = planType;

        // Reset all cards to default state
        cardMonthlyPlan.setStrokeWidth(0);
        cardYearlyPlan.setStrokeWidth(0);
        cardMonthlyPlan.setCardBackgroundColor(getColor(R.color.white));
        cardYearlyPlan.setCardBackgroundColor(getColor(R.color.white));

        // Highlight selected plan
        if (planType.equals("monthly")) {
            cardMonthlyPlan.setStrokeWidth(4);
            cardMonthlyPlan.setStrokeColor(getColor(R.color.primary_orange));
            cardMonthlyPlan.setCardBackgroundColor(getColor(R.color.primary_light_green));
            btnUpgradePremium.setText("Upgrade to Monthly - $4.99/month");
        } else {
            cardYearlyPlan.setStrokeWidth(4);
            cardYearlyPlan.setStrokeColor(getColor(R.color.primary_orange));
            cardYearlyPlan.setCardBackgroundColor(getColor(R.color.primary_light_green));
            btnUpgradePremium.setText("Upgrade to Yearly - $49.99/year");
        }
    }

    private void checkCurrentPremiumStatus() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        premiumService.isPremiumUser(userId,
                isPremium -> {
                    if (isPremium) {
                        // User is already premium
                        showAlreadyPremiumState();
                    }
                },
                error -> {
                    Log.e(TAG, "Failed to check premium status", error);
                    // Continue with normal flow
                }
        );
    }

    private void showAlreadyPremiumState() {
        btnUpgradePremium.setText("Already Premium Member");
        btnUpgradePremium.setEnabled(false);
        btnUpgradePremium.setBackgroundTintList(
                getColorStateList(R.color.gray_medium)
        );

        // Show current subscription info
        Toast.makeText(this, "You are already a premium member!", Toast.LENGTH_SHORT).show();
    }

    private void processPremiumUpgrade() {
        if (isProcessing) return;

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, "Please sign in to upgrade", Toast.LENGTH_SHORT).show();
            return;
        }

        isProcessing = true;
        showLoading(true);

        // In a real app, you would integrate with a payment processor like Stripe, PayPal, or Google Play Billing
        // For demo purposes, we'll simulate the upgrade process
        simulatePaymentProcess();
    }

    private void simulatePaymentProcess() {
        // Simulate payment processing delay
        btnUpgradePremium.postDelayed(() -> {
            // Simulate successful payment
            onPaymentSuccess();
        }, 2000);
    }

    private void onPaymentSuccess() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            onPaymentError("User not authenticated");
            return;
        }

        String userId = currentUser.getUid();

        // Create premium subscription
        Premium premium = new Premium(userId, true, selectedPlan);

        // Set subscription dates
        Calendar calendar = Calendar.getInstance();
        premium.setSubscriptionStartDate(calendar.getTime());

        // Calculate end date based on plan
        if (selectedPlan.equals("monthly")) {
            calendar.add(Calendar.MONTH, 1);
            premium.setPrice(4.99);
        } else {
            calendar.add(Calendar.YEAR, 1);
            premium.setPrice(49.99);
        }
        premium.setSubscriptionEndDate(calendar.getTime());

        premium.setPaymentMethod("Credit Card"); // This would come from actual payment processor
        premium.setStatus("active");

        // Save premium subscription to Firestore
        premiumService.createPremiumSubscription(premium)
                .addOnSuccessListener(aVoid -> {
                    // Update user document to mark as premium
                    updateUserPremiumStatus(userId, true);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to create premium subscription", e);
                    onPaymentError("Failed to activate premium subscription");
                });
    }

    private void updateUserPremiumStatus(String userId, boolean isPremium) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("premium", isPremium);
        updates.put("premiumActivatedAt", System.currentTimeMillis());

        firestoreService.updateUser(userId, updates)
                .addOnSuccessListener(aVoid -> {
                    // Update local preferences
                    updateLocalPremiumStatus(isPremium);

                    showLoading(false);
                    isProcessing = false;

                    // Show success message
                    Toast.makeText(this, "Welcome to Recipe Vault Premium! 🎉",
                            Toast.LENGTH_LONG).show();

                    // Navigate back or to main activity
                    finish();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to update user premium status", e);
                    onPaymentError("Failed to update user account");
                });
    }

    private void updateLocalPremiumStatus(boolean isPremium) {
        SharedPreferences prefs = getSharedPreferences("RecipeVault", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("is_premium", isPremium);
        editor.putLong("premium_activated_at", System.currentTimeMillis());
        editor.apply();
    }

    private void onPaymentError(String error) {
        showLoading(false);
        isProcessing = false;

        Toast.makeText(this, "Payment failed: " + error, Toast.LENGTH_LONG).show();

        Log.e(TAG, "Payment error: " + error);
    }

    private void restorePurchases() {
        // In a real app, this would check with the payment processor for existing purchases
        Toast.makeText(this, "Checking for existing purchases...", Toast.LENGTH_SHORT).show();

        // For demo purposes, we'll check Firestore for existing premium status
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) return;

        String userId = currentUser.getUid();

        premiumService.getPremiumSubscription(userId)
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Premium premium = documentSnapshot.toObject(Premium.class);
                        if (premium != null && premium.isSubscriptionActive()) {
                            // Restore premium status
                            updateUserPremiumStatus(userId, true);
                            Toast.makeText(this, "Premium subscription restored!",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "No active premium subscription found",
                                    Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "No premium subscription found",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to restore purchases", e);
                    Toast.makeText(this, "Failed to check existing purchases",
                            Toast.LENGTH_SHORT).show();
                });
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            btnUpgradePremium.setText("Processing...");
            btnUpgradePremium.setEnabled(false);
            btnRestorePurchases.setEnabled(false);
            progressBar.setVisibility(View.VISIBLE);
        } else {
            progressBar.setVisibility(View.GONE);
            btnUpgradePremium.setEnabled(true);
            btnRestorePurchases.setEnabled(true);

            // Reset button text based on selected plan
            if (selectedPlan.equals("monthly")) {
                btnUpgradePremium.setText("Upgrade to Monthly - $4.99/month");
            } else {
                btnUpgradePremium.setText("Upgrade to Yearly - $49.99/year");
            }
        }
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
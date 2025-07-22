package com.recipevault.service;

import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.recipevault.model.Premium;
import com.recipevault.model.User;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class PremiumService {

    private static final String TAG = "PremiumService";
    private static final String PREMIUM_COLLECTION = "premium_subscriptions";

    private final FirebaseFirestore db;
    private final FirestoreService firestoreService;

    @Inject
    public PremiumService(FirebaseFirestore db, FirestoreService firestoreService) {
        this.db = db;
        this.firestoreService = firestoreService;
    }

    /**
     * Create a new premium subscription
     */
    public Task<Void> createPremiumSubscription(Premium premium) {
        return db.collection(PREMIUM_COLLECTION)
                .document(premium.getUserId())
                .set(premium);
    }

    /**
     * Get premium subscription for a user
     */
    public Task<DocumentSnapshot> getPremiumSubscription(String userId) {
        return db.collection(PREMIUM_COLLECTION)
                .document(userId)
                .get();
    }

    /**
     * Update premium subscription
     */
    public Task<Void> updatePremiumSubscription(String userId, Map<String, Object> updates) {
        return db.collection(PREMIUM_COLLECTION)
                .document(userId)
                .update(updates);
    }

    /**
     * Check if user is premium with callback
     */
    public void isPremiumUser(String userId, OnSuccessListener<Boolean> onSuccess, OnFailureListener onFailure) {
        // First check the user document
        firestoreService.getUser(userId)
                .addOnSuccessListener(userDoc -> {
                    if (userDoc.exists()) {
                        User user = userDoc.toObject(User.class);
                        if (user != null && user.isPremium()) {
                            // User is marked as premium, now verify subscription is active
                            verifyActiveSubscription(userId, onSuccess, onFailure);
                        } else {
                            onSuccess.onSuccess(false);
                        }
                    } else {
                        onSuccess.onSuccess(false);
                    }
                })
                .addOnFailureListener(onFailure);
    }

    /**
     * Verify if subscription is still active
     */
    private void verifyActiveSubscription(String userId, OnSuccessListener<Boolean> onSuccess, OnFailureListener onFailure) {
        getPremiumSubscription(userId)
                .addOnSuccessListener(premiumDoc -> {
                    if (premiumDoc.exists()) {
                        Premium premium = premiumDoc.toObject(Premium.class);
                        if (premium != null && premium.isSubscriptionActive()) {
                            onSuccess.onSuccess(true);
                        } else {
                            // Subscription expired, update user status
                            updateUserPremiumStatus(userId, false);
                            onSuccess.onSuccess(false);
                        }
                    } else {
                        // No premium subscription found, update user status
                        updateUserPremiumStatus(userId, false);
                        onSuccess.onSuccess(false);
                    }
                })
                .addOnFailureListener(onFailure);
    }

    /**
     * Update user's premium status
     */
    private void updateUserPremiumStatus(String userId, boolean isPremium) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("premium", isPremium);
        if (!isPremium) {
            updates.put("premiumExpiredAt", System.currentTimeMillis());
        }

        firestoreService.updateUser(userId, updates)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "User premium status updated: " + isPremium))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to update user premium status", e));
    }

    /**
     * Cancel premium subscription
     */
    public Task<Void> cancelPremiumSubscription(String userId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("autoRenew", false);
        updates.put("status", "cancelled");

        return updatePremiumSubscription(userId, updates)
                .addOnSuccessListener(aVoid -> {
                    // Update user status
                    updateUserPremiumStatus(userId, false);
                });
    }

    /**
     * Reactivate premium subscription
     */
    public Task<Void> reactivatePremiumSubscription(String userId) {
        return getPremiumSubscription(userId)
                .continueWithTask(task -> {
                    if (task.isSuccessful() && task.getResult().exists()) {
                        Premium premium = task.getResult().toObject(Premium.class);
                        if (premium != null) {
                            // Extend subscription based on type
                            java.util.Calendar calendar = java.util.Calendar.getInstance();
                            if (premium.getSubscriptionType().equals("monthly")) {
                                calendar.add(java.util.Calendar.MONTH, 1);
                            } else {
                                calendar.add(java.util.Calendar.YEAR, 1);
                            }

                            Map<String, Object> updates = new HashMap<>();
                            updates.put("autoRenew", true);
                            updates.put("status", "active");
                            updates.put("subscriptionEndDate", calendar.getTime());

                            return updatePremiumSubscription(userId, updates)
                                    .addOnSuccessListener(aVoid -> {
                                        // Update user status
                                        updateUserPremiumStatus(userId, true);
                                    });
                        }
                    }
                    throw new Exception("Premium subscription not found");
                });
    }

    /**
     * Get all premium subscriptions (admin function)
     */
    public Task<QuerySnapshot> getAllPremiumSubscriptions() {
        return db.collection(PREMIUM_COLLECTION)
                .whereEqualTo("status", "active")
                .get();
    }

    /**
     * Get premium subscriptions that are about to expire (for notifications)
     */
    public Task<QuerySnapshot> getExpiringSubscriptions(int daysFromNow) {
        // Calculate date for comparison
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        calendar.add(java.util.Calendar.DAY_OF_YEAR, daysFromNow);
        Date targetDate = calendar.getTime();

        return db.collection(PREMIUM_COLLECTION)
                .whereEqualTo("status", "active")
                .whereLessThanOrEqualTo("subscriptionEndDate", targetDate)
                .get();
    }

    /**
     * Get premium statistics
     */
    public void getPremiumStatistics(OnSuccessListener<Map<String, Object>> onSuccess, OnFailureListener onFailure) {
        db.collection(PREMIUM_COLLECTION)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    Map<String, Object> stats = new HashMap<>();
                    int totalPremium = 0;
                    int activePremium = 0;
                    int monthlySubscriptions = 0;
                    int yearlySubscriptions = 0;
                    double totalRevenue = 0.0;

                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        Premium premium = doc.toObject(Premium.class);
                        if (premium != null) {
                            totalPremium++;

                            if (premium.isSubscriptionActive()) {
                                activePremium++;
                            }

                            if (premium.getSubscriptionType().equals("monthly")) {
                                monthlySubscriptions++;
                            } else {
                                yearlySubscriptions++;
                            }

                            totalRevenue += premium.getPrice();
                        }
                    }

                    stats.put("totalPremium", totalPremium);
                    stats.put("activePremium", activePremium);
                    stats.put("monthlySubscriptions", monthlySubscriptions);
                    stats.put("yearlySubscriptions", yearlySubscriptions);
                    stats.put("totalRevenue", totalRevenue);

                    onSuccess.onSuccess(stats);
                })
                .addOnFailureListener(onFailure);
    }

    /**
     * Check if user has premium feature access
     */
    public void hasFeatureAccess(String userId, String feature, OnSuccessListener<Boolean> onSuccess, OnFailureListener onFailure) {
        isPremiumUser(userId, isPremium -> {
            if (isPremium) {
                onSuccess.onSuccess(true);
            } else {
                // Check if feature is available for free users
                boolean freeFeature = isFeatureAvailableForFree(feature);
                onSuccess.onSuccess(freeFeature);
            }
        }, onFailure);
    }

    /**
     * Helper method to check if feature is available for free users
     */
    private boolean isFeatureAvailableForFree(String feature) {
        switch (feature) {
            case "basic_recipes":
            case "basic_search":
            case "basic_favorites":
                return true;
            case "advanced_search":
            case "meal_planning":
            case "export_recipes":
            case "backup_recipes":
            case "unlimited_storage":
                return false;
            default:
                return false;
        }
    }

    /**
     * Get premium feature limits for free users
     */
    public int getFeatureLimit(String feature) {
        switch (feature) {
            case "saved_recipes":
                return 50; // Free users can save up to 50 recipes
            case "created_recipes":
                return 10; // Free users can create up to 10 recipes
            case "meal_plans":
                return 0; // Free users can't create meal plans
            default:
                return 0;
        }
    }

    /**
     * Process subscription renewal (would be called by a background service)
     */
    public void processSubscriptionRenewal(String userId, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        getPremiumSubscription(userId)
                .addOnSuccessListener(premiumDoc -> {
                    if (premiumDoc.exists()) {
                        Premium premium = premiumDoc.toObject(Premium.class);
                        if (premium != null && premium.isAutoRenew() &&
                                premium.getSubscriptionEndDate() != null &&
                                premium.getSubscriptionEndDate().before(new Date())) {
                            // Subscription is past due and has auto-renew enabled
                            // In a real app, this would charge the payment method
                            // For demo purposes, we'll just extend the subscription

                            java.util.Calendar calendar = java.util.Calendar.getInstance();
                            calendar.setTime(premium.getSubscriptionEndDate());

                            if (premium.getSubscriptionType().equals("monthly")) {
                                calendar.add(java.util.Calendar.MONTH, 1);
                            } else {
                                calendar.add(java.util.Calendar.YEAR, 1);
                            }

                            Map<String, Object> updates = new HashMap<>();
                            updates.put("subscriptionEndDate", calendar.getTime());

                            updatePremiumSubscription(userId, updates)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "Subscription renewed for user: " + userId);
                                        onSuccess.onSuccess(null);
                                    })
                                    .addOnFailureListener(onFailure);
                        } else {
                            onSuccess.onSuccess(null);
                        }
                    } else {
                        onFailure.onFailure(new Exception("Premium subscription not found"));
                    }
                })
                .addOnFailureListener(onFailure);
    }
}
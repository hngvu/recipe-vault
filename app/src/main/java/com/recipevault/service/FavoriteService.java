package com.recipevault.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FavoriteService {
    private static final String TAG = "FavoriteService";
    private static final String FAVORITES_COLLECTION = "user_favorites";
    private static final String PREFS_NAME = "RecipeVault_Favorites";
    
    private static FavoriteService instance;
    private FirebaseFirestore db;
    private FirebaseAuth firebaseAuth;
    private Context context;

    private FavoriteService(Context context) {
        this.context = context.getApplicationContext();
        this.db = FirebaseFirestore.getInstance();
        this.firebaseAuth = FirebaseAuth.getInstance();
    }

    public static synchronized FavoriteService getInstance(Context context) {
        if (instance == null) {
            instance = new FavoriteService(context);
        }
        return instance;
    }

    /**
     * Add a recipe to user's favorites
     */
    public Task<Void> addToFavorites(String recipeId) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("User must be authenticated to add favorites");
        }

        String userId = currentUser.getUid();
        
        // Also save to local preferences for offline access
        saveToLocalFavorites(recipeId, true);

        // Save to Firestore
        Map<String, Object> favoriteData = new HashMap<>();
        favoriteData.put("userId", userId);
        favoriteData.put("recipeId", recipeId);
        favoriteData.put("addedAt", FieldValue.serverTimestamp());

        return db.collection(FAVORITES_COLLECTION)
                .document(userId + "_" + recipeId)
                .set(favoriteData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Recipe added to favorites: " + recipeId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to add recipe to favorites", e);
                });
    }

    /**
     * Remove a recipe from user's favorites
     */
    public Task<Void> removeFromFavorites(String recipeId) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("User must be authenticated to remove favorites");
        }

        String userId = currentUser.getUid();
        
        // Remove from local preferences
        saveToLocalFavorites(recipeId, false);

        // Remove from Firestore
        return db.collection(FAVORITES_COLLECTION)
                .document(userId + "_" + recipeId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "Recipe removed from favorites: " + recipeId);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to remove recipe from favorites", e);
                });
    }

    /**
     * Check if a recipe is in user's favorites
     */
    public Task<DocumentSnapshot> isFavorite(String recipeId) {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("User must be authenticated to check favorites");
        }

        String userId = currentUser.getUid();
        return db.collection(FAVORITES_COLLECTION)
                .document(userId + "_" + recipeId)
                .get();
    }

    /**
     * Get all favorite recipe IDs for the current user
     */
    public Task<List<String>> getFavoriteRecipeIds() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            throw new IllegalStateException("User must be authenticated to get favorites");
        }

        String userId = currentUser.getUid();
        return db.collection(FAVORITES_COLLECTION)
                .whereEqualTo("userId", userId)
                .get()
                .continueWith(task -> {
                    List<String> favoriteIds = new ArrayList<>();
                    if (task.isSuccessful() && task.getResult() != null) {
                        task.getResult().forEach(document -> {
                            String recipeId = document.getString("recipeId");
                            if (recipeId != null) {
                                favoriteIds.add(recipeId);
                            }
                        });
                    }
                    return favoriteIds;
                });
    }

    /**
     * Toggle favorite status of a recipe
     */
    public Task<Boolean> toggleFavorite(String recipeId) {
        return isFavorite(recipeId)
                .continueWithTask(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        boolean isFavorite = task.getResult().exists();
                        if (isFavorite) {
                            return removeFromFavorites(recipeId)
                                    .continueWith(removeTask -> false);
                        } else {
                            return addToFavorites(recipeId)
                                    .continueWith(addTask -> true);
                        }
                    } else {
                        throw new RuntimeException("Failed to check favorite status");
                    }
                });
    }

    /**
     * Save favorite status to local SharedPreferences for offline access
     */
    private void saveToLocalFavorites(String recipeId, boolean isFavorite) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        
        if (isFavorite) {
            editor.putBoolean(recipeId, true);
        } else {
            editor.remove(recipeId);
        }
        
        editor.apply();
    }

    /**
     * Check if recipe is favorite from local storage (for offline access)
     */
    public boolean isLocalFavorite(String recipeId) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(recipeId, false);
    }

    /**
     * Get all local favorite recipe IDs
     */
    public List<String> getLocalFavoriteIds() {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        List<String> favoriteIds = new ArrayList<>();
        
        for (Map.Entry<String, ?> entry : prefs.getAll().entrySet()) {
            if (entry.getValue() instanceof Boolean && (Boolean) entry.getValue()) {
                favoriteIds.add(entry.getKey());
            }
        }
        
        return favoriteIds;
    }

    /**
     * Sync local favorites with Firestore (call this when user comes online)
     */
    public void syncFavorites() {
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        List<String> localFavorites = getLocalFavoriteIds();
        
        // Get remote favorites and sync
        getFavoriteRecipeIds()
                .addOnSuccessListener(remoteFavorites -> {
                    // Add local favorites that are not in remote
                    for (String localFavorite : localFavorites) {
                        if (!remoteFavorites.contains(localFavorite)) {
                            addToFavorites(localFavorite);
                        }
                    }
                    
                    // Update local storage with remote favorites
                    SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                    SharedPreferences.Editor editor = prefs.edit();
                    
                    for (String remoteFavorite : remoteFavorites) {
                        editor.putBoolean(remoteFavorite, true);
                    }
                    
                    editor.apply();
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to sync favorites", e);
                });
    }
}
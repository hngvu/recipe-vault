package com.recipevault.service;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.recipevault.model.User;
import com.recipevault.model.Recipe;
import com.recipevault.model.Comment;
import com.recipevault.model.Rating;
import com.recipevault.model.CookingReminder;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class FirestoreService {
    private FirebaseFirestore db;

    // Collection names
    public static final String USERS_COLLECTION = "users";
    public static final String RECIPES_COLLECTION = "recipes";
    public static final String COMMENTS_COLLECTION = "comments";
    public static final String RATINGS_COLLECTION = "ratings";
    public static final String REMINDERS_COLLECTION = "cooking_reminders";

    @Inject
    public FirestoreService(FirebaseFirestore db) {
        this.db = db;
    }

    // User operations
    public Task<Void> createUser(User user) {
        return db.collection(USERS_COLLECTION).document(user.getUserId()).set(user);
    }

    public Task<DocumentSnapshot> getUser(String userId) {
        return db.collection(USERS_COLLECTION).document(userId).get();
    }

    public Task<Void> updateUser(String userId, Map<String, Object> updates) {
        return db.collection(USERS_COLLECTION).document(userId).update(updates);
    }

    // Recipe operations
    public Task<DocumentReference> createRecipe(Recipe recipe) {
        return db.collection(RECIPES_COLLECTION).add(recipe);
    }

    public Task<DocumentSnapshot> getRecipe(String recipeId) {
        return db.collection(RECIPES_COLLECTION).document(recipeId).get();
    }

    public Task<QuerySnapshot> getUserRecipes(String userId) {
        return db.collection(RECIPES_COLLECTION)
                .whereEqualTo("creatorUserId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get();
    }

    public Task<QuerySnapshot> getRecipesByCategory(String category) {
        return db.collection(RECIPES_COLLECTION)
                .whereEqualTo("category", category)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get();
    }

    public Task<QuerySnapshot> searchRecipesByTags(List<String> tags) {
        return db.collection(RECIPES_COLLECTION)
                .whereArrayContainsAny("tags", tags)
                .orderBy("rating", Query.Direction.DESCENDING)
                .get();
    }

    public Task<QuerySnapshot> getRecentRecipes(int limit) {
        return db.collection(RECIPES_COLLECTION)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .limit(limit)
                .get();
    }

    public Task<Void> updateRecipe(String recipeId, Map<String, Object> updates) {
        return db.collection(RECIPES_COLLECTION).document(recipeId).update(updates);
    }

    public Task<Void> deleteRecipe(String recipeId) {
        return db.collection(RECIPES_COLLECTION).document(recipeId).delete();
    }

    // Comment operations
    public Task<DocumentReference> addComment(String recipeId, Comment comment) {
        return db.collection(RECIPES_COLLECTION)
                .document(recipeId)
                .collection(COMMENTS_COLLECTION)
                .add(comment);
    }

    public Task<QuerySnapshot> getRecipeComments(String recipeId) {
        return db.collection(RECIPES_COLLECTION)
                .document(recipeId)
                .collection(COMMENTS_COLLECTION)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get();
    }

    // Rating operations
    public Task<DocumentReference> addRating(String recipeId, Rating rating) {
        return db.collection(RECIPES_COLLECTION)
                .document(recipeId)
                .collection(RATINGS_COLLECTION)
                .add(rating);
    }

    public Task<QuerySnapshot> getRecipeRatings(String recipeId) {
        return db.collection(RECIPES_COLLECTION)
                .document(recipeId)
                .collection(RATINGS_COLLECTION)
                .whereEqualTo("isActive", true)
                .get();
    }

    // Cooking reminder operations
    public Task<DocumentReference> createReminder(CookingReminder reminder) {
        return db.collection(REMINDERS_COLLECTION).add(reminder);
    }

    public Task<QuerySnapshot> getUserReminders(String userId) {
        return db.collection(REMINDERS_COLLECTION)
                .whereEqualTo("userId", userId)
                .whereEqualTo("isActive", true)
                .orderBy("scheduledTime", Query.Direction.ASCENDING)
                .get();
    }

    public Task<Void> updateReminder(String reminderId, Map<String, Object> updates) {
        return db.collection(REMINDERS_COLLECTION).document(reminderId).update(updates);
    }
}
package com.recipevault.service;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.recipevault.enumration.EntityEnum;
import com.recipevault.model.Recipe;
import com.recipevault.model.User;
import com.recipevault.repository.FirestoreRepository;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class RecipeService {
    private static final String TAG = "FavoriteService";
    private static final String FAVORITES_COLLECTION = "user_favorites";
    private static final String PREFS_NAME = "RecipeVault_Favorites";


    private final FirestoreRepository<User> userRepository;
    private final FirestoreRepository<Recipe> recipeRepository;

    @Inject
    public RecipeService(FirebaseFirestore db) {
        this.userRepository = new FirestoreRepository<>(db, User.class, EntityEnum.USERS.name().toLowerCase());
        this.recipeRepository = new FirestoreRepository<>(db, Recipe.class, EntityEnum.RECIPES.name().toLowerCase());
    }

    public void getFavoriteRecipes(
            String userId,
            OnSuccessListener<List<Recipe>> onSuccess,
            OnFailureListener onFailure
    ) {
        userRepository.getById(userId, user -> {

            List<String> savedRecipeIds = user.getSavedRecipes();
            if (savedRecipeIds == null || savedRecipeIds.isEmpty()) {
                onSuccess.onSuccess(new ArrayList<>());
                return;
            }

            recipeRepository.getByDocumentIds(savedRecipeIds, onSuccess, onFailure);

        }, onFailure);
    }


}
package com.recipevault;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.recipevault.activity.AddRecipeActivity;
import com.recipevault.activity.ProfileActivity;
import com.recipevault.activity.RecipeDetailActivity;
import com.recipevault.activity.SignInActivity;
import com.recipevault.adapter.RecipeAdapter;
import com.recipevault.model.Recipe;
import com.recipevault.service.FirestoreService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class MainActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private RecyclerView rvRecipes;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout emptyState;
    private BottomNavigationView bottomNavigation;
    private RecipeAdapter recipeAdapter;

    @Inject
    FirestoreService firestoreService;
    @Inject
    FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        // Check if user is authenticated
        if (isUserAuthenticated()) {
            // User is logged in, show feed
            setContentView(R.layout.activity_main);
            setupFeed();
            initializeSampleData();
        } else {
            // User not logged in, redirect to login
            redirectToLogin();
        }
    }

    private boolean isUserAuthenticated() {
        // Check Firebase Auth instead of just SharedPreferences
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        SharedPreferences prefs = getSharedPreferences("RecipeVault", MODE_PRIVATE);
        boolean sharedPrefAuth = prefs.getBoolean("is_logged_in", false);

        // User is authenticated only if both Firebase and SharedPreferences say so
        if (currentUser != null && sharedPrefAuth) {
            return true;
        } else {
            // If Firebase says user is signed out, clear SharedPreferences
            if (currentUser == null) {
                clearUserSession();
            }
            return false;
        }
    }

    private void clearUserSession() {
        SharedPreferences prefs = getSharedPreferences("RecipeVault", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.clear();
        editor.apply();
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, SignInActivity.class);
        startActivity(intent);
        finish();
    }

    private void setupFeed() {
        initViews();
        setupToolbar();
        setupRecyclerView();
        setupBottomNavigation();
        loadRecipes();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        rvRecipes = findViewById(R.id.rv_recipes);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        emptyState = findViewById(R.id.empty_state);
        bottomNavigation = findViewById(R.id.bottom_navigation);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
    }

    private void setupRecyclerView() {
        rvRecipes.setLayoutManager(new LinearLayoutManager(this));
        recipeAdapter = new RecipeAdapter();
        recipeAdapter.setOnRecipeClickListener(recipe -> {
            Log.d("MainActivity", "Recipe clicked: " + recipe.getTitle() + ", ID: " + recipe.getId());

            if (recipe.getId() == null || recipe.getId().isEmpty()) {
                Log.e("MainActivity", "Recipe ID is null or empty!");
                Toast.makeText(this, "Recipe ID is missing", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, RecipeDetailActivity.class);
            intent.putExtra("recipe_id", recipe.getId());
            intent.putExtra("recipe_title", recipe.getTitle());
            startActivity(intent);
        });
        rvRecipes.setAdapter(recipeAdapter);
    }

    private void setupBottomNavigation() {
        // Set current item as home
        bottomNavigation.setSelectedItemId(R.id.nav_home);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                // Already on home
                return true;
            } else if (itemId == R.id.nav_search) {
                // TODO: Navigate to SearchActivity
                Toast.makeText(this, "Search feature coming soon", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.nav_add) {
                // Navigate to AddRecipeActivity
                Intent intent = new Intent(this, AddRecipeActivity.class);
                startActivity(intent);
                return true;
            } else if (itemId == R.id.nav_favorites) {
                // TODO: Navigate to FavoritesActivity
                Toast.makeText(this, "Favorites feature coming soon", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.nav_profile) {
                // Navigate to ProfileActivity
                Intent intent = new Intent(this, ProfileActivity.class);
                startActivity(intent);
                overridePendingTransition(0, 0);
                finish();
                return true;
            }

            return false;
        });
    }


    private void loadRecipes() {
        showLoading(true);

        firestoreService.getRecentRecipes(20)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Recipe> recipes = new ArrayList<>();
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        Recipe recipe = document.toObject(Recipe.class);
                        if (recipe != null) {
                            recipe.setId(document.getId());
                            recipes.add(recipe);
                        }
                    }
                    recipeAdapter.setRecipes(recipes);
                    showEmptyState(recipes.isEmpty());
                    showLoading(false);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load recipes: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    showLoading(false);
                    showEmptyState(true);
                });

        // Setup swipe to refresh
        swipeRefresh.setOnRefreshListener(() -> {
            loadRecipes();
            swipeRefresh.setRefreshing(false);
        });
    }

    private void initializeSampleData() {
        // Check if data already exists
        firestoreService.getRecentRecipes(1)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    if (queryDocumentSnapshots.isEmpty()) {
                        createSampleRecipes();
                    }
                });
    }

    private void createSampleRecipes() {
        Log.d("MainActivity", "Creating sample recipes...");

        // Get current user ID for proper ownership
        SharedPreferences prefs = getSharedPreferences("RecipeVault", MODE_PRIVATE);
        String currentUserId = prefs.getString("user_id", "anonymous_user");
        Log.d("MainActivity", "Current user ID: " + currentUserId);

        List<Recipe> sampleRecipes = Arrays.asList(
                createSampleRecipe("Spaghetti Carbonara",
                        "Classic Italian pasta dish with eggs, cheese, and pancetta",
                        "https://images.unsplash.com/photo-1621996346565-e3dbc353d2e5?w=500",
                        "30 minutes", "Medium", 4, "Chef Marco",
                        "pasta, eggs, parmesan, pancetta", "Italian", currentUserId),

                createSampleRecipe("Chicken Tikka Masala",
                        "Creamy tomato-based curry with tender chicken pieces",
                        "https://images.unsplash.com/photo-1565557623262-b51c2513a641?w=500",
                        "45 minutes", "Medium", 6, "Chef Priya",
                        "chicken, tomatoes, cream, spices", "Indian", currentUserId),

                createSampleRecipe("Chocolate Chip Cookies",
                        "Soft and chewy homemade chocolate chip cookies",
                        "https://images.unsplash.com/photo-1499636136210-6f4ee915583e?w=500",
                        "25 minutes", "Easy", 24, "Baker Sarah",
                        "flour, chocolate chips, butter, sugar", "Dessert", currentUserId),

                createSampleRecipe("Caesar Salad",
                        "Fresh romaine lettuce with classic Caesar dressing",
                        "https://images.unsplash.com/photo-1551248429-40975aa4de74?w=500",
                        "15 minutes", "Easy", 2, "Chef Alex",
                        "lettuce, croutons, parmesan, anchovies", "Salad", currentUserId),

                createSampleRecipe("Beef Stir Fry",
                        "Quick and healthy beef stir fry with vegetables",
                        "https://images.unsplash.com/photo-1603133872878-684f208fb84b?w=500",
                        "20 minutes", "Easy", 4, "Chef Lin",
                        "beef, vegetables, soy sauce, ginger", "Asian", currentUserId)
        );

        for (Recipe recipe : sampleRecipes) {
            Log.d("MainActivity", "Creating recipe: " + recipe.getTitle());
            firestoreService.createRecipe(recipe)
                    .addOnSuccessListener(documentReference -> {
                        Log.d("MainActivity", "Recipe created successfully: " + documentReference.getId());
                        // Reload recipes after creating sample data
                        loadRecipes();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("MainActivity", "Failed to create sample recipe: " + recipe.getTitle(), e);
                        Toast.makeText(this, "Failed to create sample recipe: " + e.getMessage(),
                                Toast.LENGTH_LONG).show();
                    });
        }
    }

    private Recipe createSampleRecipe(String title, String description, String imageUrl,
                                       String cookingTime, String difficulty, int servings,
                                       String authorName, String tags, String category, String userId) {
        Recipe recipe = new Recipe();
        recipe.setTitle(title);
        recipe.setDescription(description);
        recipe.setImageUrl(imageUrl);
        recipe.setCookingTime(cookingTime);
        recipe.setDifficulty(difficulty);
        recipe.setServings(servings);
        recipe.setAuthorName(authorName);
        recipe.setCreatedAt(new Date());
        recipe.setCreatorUserId(userId); // Use actual user ID
        recipe.setTags(Arrays.asList(tags.split(", ")));
        recipe.setCategory(category);
        recipe.setRating(4.0f + (float) (Math.random() * 1.0f)); // Random rating 4.0-5.0
        recipe.setIngredients(Arrays.asList(
                "Main ingredient 1", "Main ingredient 2", "Seasoning", "Optional garnish"
        ));
        recipe.setInstructions(Arrays.asList(
                "Prepare all ingredients",
                "Cook according to recipe",
                "Season to taste",
                "Serve and enjoy"
        ));
        return recipe;
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            rvRecipes.setVisibility(View.GONE);
            emptyState.setVisibility(View.GONE);
        } else {
            rvRecipes.setVisibility(View.VISIBLE);
        }
    }

    private void showEmptyState(boolean isEmpty) {
        if (isEmpty) {
            rvRecipes.setVisibility(View.GONE);
            emptyState.setVisibility(View.VISIBLE);
        } else {
            rvRecipes.setVisibility(View.VISIBLE);
            emptyState.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Always check authentication when returning to this activity
        if (!isUserAuthenticated()) {
            Log.d("MainActivity", "User no longer authenticated, redirecting to login");
            redirectToLogin();
            return;
        }

        // Reload recipes when returning from other activities
        if (isUserAuthenticated()) {
            loadRecipes();
        }
    }
}
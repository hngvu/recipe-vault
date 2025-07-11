package com.recipevault.activity;

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
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.recipevault.MainActivity;
import com.recipevault.R;
import com.recipevault.activity.AddRecipeActivity;
import com.recipevault.activity.ProfileActivity;
import com.recipevault.activity.RecipeDetailActivity;
import com.recipevault.activity.SearchActivity;
import com.recipevault.activity.SignInActivity;
import com.recipevault.adapter.RecipeAdapter;
import com.recipevault.model.Recipe;
import com.recipevault.service.FirestoreService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FavoritesActivity extends AppCompatActivity {

    private static final String TAG = "FavoritesActivity";
    
    private MaterialToolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvFavorites;
    private LinearLayout emptyState;
    private BottomNavigationView bottomNavigation;
    private ChipGroup chipGroupFilters;
    
    private RecipeAdapter recipeAdapter;
    private FirestoreService firestoreService;
    private FirebaseAuth firebaseAuth;
    
    private List<Recipe> allFavoriteRecipes;
    private List<Recipe> filteredRecipes;
    private String selectedFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        firestoreService = FirestoreService.getInstance();
        firebaseAuth = FirebaseAuth.getInstance();

        // Check authentication before proceeding
        if (!isUserAuthenticated()) {
            redirectToSignIn();
            return;
        }

        initViews();
        setupToolbar();
        setupFilters();
        setupRecyclerView();
        setupBottomNavigation();
        setupSwipeRefresh();
        loadFavorites();
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
        swipeRefresh = findViewById(R.id.swipe_refresh);
        rvFavorites = findViewById(R.id.rv_favorites);
        emptyState = findViewById(R.id.empty_state);
        bottomNavigation = findViewById(R.id.bottom_navigation);
        chipGroupFilters = findViewById(R.id.chip_group_filters);
        
        allFavoriteRecipes = new ArrayList<>();
        filteredRecipes = new ArrayList<>();
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupFilters() {
        String[] filters = {"All", "Easy", "Medium", "Hard", "Quick", "Italian", "Asian", "Dessert"};

        for (String filter : filters) {
            Chip chip = new Chip(this);
            chip.setText(filter);
            chip.setCheckable(true);
            chip.setChipBackgroundColorResource(R.color.chip_background_selector);
            chip.setTextColor(getResources().getColorStateList(R.color.bottom_nav_color));

            if (filter.equals("All")) {
                chip.setChecked(true);
            }

            chip.setOnClickListener(v -> {
                selectedFilter = filter;
                filterRecipes();
            });

            chipGroupFilters.addView(chip);
        }

        chipGroupFilters.setOnCheckedChangeListener((group, checkedId) -> {
            // Ensure single selection
            for (int i = 0; i < group.getChildCount(); i++) {
                Chip chip = (Chip) group.getChildAt(i);
                if (chip.getId() != checkedId) {
                    chip.setChecked(false);
                }
            }
        });
    }

    private void setupRecyclerView() {
        rvFavorites.setLayoutManager(new LinearLayoutManager(this));
        recipeAdapter = new RecipeAdapter();
        recipeAdapter.setOnRecipeClickListener(recipe -> {
            Log.d(TAG, "Favorite recipe clicked: " + recipe.getTitle() + ", ID: " + recipe.getId());

            if (recipe.getId() == null || recipe.getId().isEmpty()) {
                Log.e(TAG, "Recipe ID is null or empty!");
                Toast.makeText(this, "Recipe ID is missing", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(this, RecipeDetailActivity.class);
            intent.putExtra("recipe_id", recipe.getId());
            intent.putExtra("recipe_title", recipe.getTitle());
            startActivity(intent);
        });
        rvFavorites.setAdapter(recipeAdapter);
    }

    private void setupBottomNavigation() {
        bottomNavigation.setSelectedItemId(R.id.nav_favorites);

        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_search) {
                startActivity(new Intent(this, SearchActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_add) {
                startActivity(new Intent(this, AddRecipeActivity.class));
                return true;
            } else if (itemId == R.id.nav_favorites) {
                return true; // Already on favorites
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }

            return false;
        });
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.primary_orange);
        swipeRefresh.setOnRefreshListener(() -> {
            loadFavorites();
        });
    }

    private void loadFavorites() {
        showLoading(true);

        // Get current user's favorite recipe IDs from SharedPreferences or Firestore
        // For now, we'll simulate by loading recent recipes and marking some as favorites
        // In a real app, you'd have a favorites collection or user document with favorite recipe IDs
        
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            showLoading(false);
            showEmptyState();
            return;
        }

        // For demonstration, we'll load some recipes and simulate favorites
        // In production, you'd query user's actual favorite recipes
        firestoreService.getRecentRecipes(10)
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Recipe> recipes = new ArrayList<>();
                    
                    // Simulate some recipes being favorites (every other recipe)
                    int count = 0;
                    for (DocumentSnapshot document : queryDocumentSnapshots) {
                        if (count % 2 == 0) { // Simulate favorites
                            Recipe recipe = document.toObject(Recipe.class);
                            if (recipe != null) {
                                recipe.setId(document.getId());
                                recipes.add(recipe);
                            }
                        }
                        count++;
                    }
                    
                    allFavoriteRecipes = recipes;
                    filterRecipes();
                    showLoading(false);
                    
                    if (recipes.isEmpty()) {
                        showEmptyState();
                    } else {
                        showFavorites();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load favorite recipes", e);
                    Toast.makeText(this, "Failed to load favorites: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    showLoading(false);
                    showEmptyState();
                });
    }

    private void filterRecipes() {
        if (allFavoriteRecipes == null) {
            return;
        }

        if ("All".equals(selectedFilter)) {
            filteredRecipes = new ArrayList<>(allFavoriteRecipes);
        } else {
            filteredRecipes = allFavoriteRecipes.stream()
                    .filter(recipe -> {
                        // Filter by difficulty
                        if ("Easy".equals(selectedFilter) || "Medium".equals(selectedFilter) || "Hard".equals(selectedFilter)) {
                            return selectedFilter.equals(recipe.getDifficulty());
                        }
                        
                        // Filter by cooking time for "Quick"
                        if ("Quick".equals(selectedFilter)) {
                            String cookingTime = recipe.getCookingTime();
                            if (cookingTime != null && cookingTime.contains("min")) {
                                try {
                                    int minutes = Integer.parseInt(cookingTime.replaceAll("[^0-9]", ""));
                                    return minutes <= 30;
                                } catch (NumberFormatException e) {
                                    return false;
                                }
                            }
                            return false;
                        }
                        
                        // Filter by category/tags
                        if (recipe.getCategory() != null && recipe.getCategory().equalsIgnoreCase(selectedFilter)) {
                            return true;
                        }
                        
                        if (recipe.getTags() != null) {
                            return recipe.getTags().stream()
                                    .anyMatch(tag -> tag.equalsIgnoreCase(selectedFilter));
                        }
                        
                        return false;
                    })
                    .collect(Collectors.toList());
        }

        recipeAdapter.setRecipes(filteredRecipes);
        
        if (filteredRecipes.isEmpty() && !allFavoriteRecipes.isEmpty()) {
            showEmptyFilterState();
        } else if (filteredRecipes.isEmpty()) {
            showEmptyState();
        } else {
            showFavorites();
        }
    }

    private void showLoading(boolean isLoading) {
        swipeRefresh.setRefreshing(isLoading);
    }

    private void showEmptyState() {
        rvFavorites.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
    }

    private void showEmptyFilterState() {
        rvFavorites.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
        // You could customize the empty state message for filtered results
    }

    private void showFavorites() {
        rvFavorites.setVisibility(View.VISIBLE);
        emptyState.setVisibility(View.GONE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Check authentication when returning to this activity
        if (!isUserAuthenticated()) {
            redirectToSignIn();
            return;
        }
        
        // Reload favorites when returning from other activities
        loadFavorites();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
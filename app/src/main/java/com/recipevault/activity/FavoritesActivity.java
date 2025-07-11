package com.recipevault.activity;

import android.content.Intent;
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
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.recipevault.MainActivity;
import com.recipevault.R;
import com.recipevault.adapter.RecipeAdapter;
import com.recipevault.service.FavoriteService;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class FavoritesActivity extends AppCompatActivity {

    private static final String TAG = "FavoritesActivity";

    private MaterialToolbar toolbar;
    private SwipeRefreshLayout swipeRefresh;
    private RecyclerView rvFavorites;
    private LinearLayout emptyState;
    private BottomNavigationView bottomNavigation;
    private MaterialButton btnExploreRecipe;
    private RecipeAdapter recipeAdapter;
    @Inject
    FirebaseAuth firebaseAuth;
    @Inject
    FavoriteService favoriteService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_favorites);

        // Check authentication before proceeding
        if (!isUserAuthenticated()) {
            redirectToSignIn();
            return;
        }

        initViews();
        setupToolbar();
        setupRecyclerView();
        setupBottomNavigation();
        setupSwipeRefresh();
        setupExploreButton();
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
        btnExploreRecipe = findViewById(R.id.btn_explore_recipes);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
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
            intent.putExtra("is_favorite", true);
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
                return true;
            } else if (itemId == R.id.nav_search) {
                startActivity(new Intent(this, SearchActivity.class));
                return true;
            } else if (itemId == R.id.nav_add) {
                startActivity(new Intent(this, AddRecipeActivity.class));
                return true;
            } else if (itemId == R.id.nav_favorites) {
                return true; // Already on favorites
            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
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
        swipeRefresh.setRefreshing(true);

        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        if (currentUser == null) {
            swipeRefresh.setRefreshing(false);
            showEmptyState();
            return;
        }

        String userId = currentUser.getUid();
        favoriteService.getFavoriteRecipes(
                userId,
                recipes -> {
                    swipeRefresh.setRefreshing(false);
                    if (recipes.isEmpty()) {
                        showEmptyState();
                    } else {
                        recipeAdapter.setRecipes(recipes);
                    }

                },
                error -> {
                    swipeRefresh.setRefreshing(false);
                    Log.e(TAG, "Failed to load favorites", error);
                }
        );

    }

    private void setupExploreButton() {
        btnExploreRecipe.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
        });
    }

    private void showEmptyState() {
        rvFavorites.setVisibility(View.GONE);
        emptyState.setVisibility(View.VISIBLE);
    }


    @Override
    protected void onResume() {
        super.onResume();

        if (!isUserAuthenticated()) {
            redirectToSignIn();
            return;
        }

        loadFavorites();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return true;
    }
}
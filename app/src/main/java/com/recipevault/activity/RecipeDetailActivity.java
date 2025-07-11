package com.recipevault.activity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.recipevault.R;
import com.recipevault.adapter.IngredientsAdapter;
import com.recipevault.adapter.InstructionsAdapter;
import com.recipevault.model.Recipe;
import com.recipevault.service.FirestoreService;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class RecipeDetailActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private CollapsingToolbarLayout collapsingToolbar;
    private ImageView ivRecipeImage;
    private TextView tvDescription, tvCookTime, tvServings, tvDifficulty, tvAuthor, tvRating;
    private ChipGroup chipGroupTags;
    private RecyclerView rvIngredients, rvInstructions;
    private FloatingActionButton fabFavorite;

    private boolean isFavorite = false;
    private String recipeId;
    private String recipeTitle;
    private Recipe currentRecipe;
    private IngredientsAdapter ingredientsAdapter;
    private InstructionsAdapter instructionsAdapter;
    @Inject
    FirestoreService firestoreService;
    @Inject
    FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);


        // Check authentication before proceeding
        if (!isUserAuthenticated()) {
            Toast.makeText(this, "Please sign in to view recipe details", Toast.LENGTH_SHORT).show();
            redirectToSignIn();
            return;
        }

        initViews();
        getIntentData();
        setupToolbar();
        setupRecyclerViews();
        setupClickListeners();
        loadRecipeDetails();
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

    private void getIntentData() {
        Intent intent = getIntent();
        recipeId = intent.getStringExtra("recipe_id");
        recipeTitle = intent.getStringExtra("recipe_title");
        isFavorite = intent.getBooleanExtra("is_favorite", false);
        updateFavoriteIcon();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        collapsingToolbar = findViewById(R.id.collapsing_toolbar);
        ivRecipeImage = findViewById(R.id.iv_recipe_image);
        tvDescription = findViewById(R.id.tv_description);
        tvCookTime = findViewById(R.id.tv_cook_time);
        tvServings = findViewById(R.id.tv_servings);
        tvDifficulty = findViewById(R.id.tv_difficulty);
        tvAuthor = findViewById(R.id.tv_author);
        tvRating = findViewById(R.id.tv_rating);
        chipGroupTags = findViewById(R.id.chip_group_tags);
        rvIngredients = findViewById(R.id.rv_ingredients);
        rvInstructions = findViewById(R.id.rv_instructions);
        fabFavorite = findViewById(R.id.fab_favorite);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }

        if (recipeTitle != null) {
            collapsingToolbar.setTitle(recipeTitle);
        }
    }

    private void setupRecyclerViews() {
        // Setup ingredients RecyclerView
        rvIngredients.setLayoutManager(new LinearLayoutManager(this));
        ingredientsAdapter = new IngredientsAdapter();
        rvIngredients.setAdapter(ingredientsAdapter);

        // Setup instructions RecyclerView
        rvInstructions.setLayoutManager(new LinearLayoutManager(this));
        instructionsAdapter = new InstructionsAdapter();
        rvInstructions.setAdapter(instructionsAdapter);
    }

    private void setupClickListeners() {
        fabFavorite.setOnClickListener(v -> toggleFavorite());
    }

    private void loadRecipeDetails() {
        if (recipeId == null || recipeId.isEmpty()) {
            Log.e("RecipeDetailActivity", "Recipe ID is null or empty");
            Toast.makeText(this, "Recipe ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d("RecipeDetailActivity", "Loading recipe with ID: " + recipeId);

        // Show loading state
        showLoading(true);

        firestoreService.getRecipe(recipeId)
                .addOnSuccessListener(documentSnapshot -> {
                    Log.d("RecipeDetailActivity", "Firestore query successful");
                    showLoading(false);

                    if (documentSnapshot.exists()) {
                        Log.d("RecipeDetailActivity", "Recipe document exists");
                        try {
                            currentRecipe = documentSnapshot.toObject(Recipe.class);
                            if (currentRecipe != null) {
                                currentRecipe.setId(documentSnapshot.getId());
                                Log.d("RecipeDetailActivity", "Recipe loaded: " + currentRecipe.getTitle());
                                displayRecipeDetails();
                            } else {
                                Log.e("RecipeDetailActivity", "Failed to convert document to Recipe object");
                                Toast.makeText(this, "Failed to load recipe data", Toast.LENGTH_SHORT).show();
                                finish();
                            }
                        } catch (Exception e) {
                            Log.e("RecipeDetailActivity", "Error converting document to Recipe", e);
                            Toast.makeText(this, "Error loading recipe: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } else {
                        Log.e("RecipeDetailActivity", "Recipe document does not exist for ID: " + recipeId);
                        Toast.makeText(this, "Recipe not found", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("RecipeDetailActivity", "Firestore query failed", e);
                    showLoading(false);
                    Toast.makeText(this, "Failed to load recipe: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void showLoading(boolean isLoading) {
        if (isLoading) {
            // Hide content while loading
            tvDescription.setVisibility(View.GONE);
            rvIngredients.setVisibility(View.GONE);
            rvInstructions.setVisibility(View.GONE);
            chipGroupTags.setVisibility(View.GONE);
        } else {
            // Show content after loading
            tvDescription.setVisibility(View.VISIBLE);
            rvIngredients.setVisibility(View.VISIBLE);
            rvInstructions.setVisibility(View.VISIBLE);
            chipGroupTags.setVisibility(View.VISIBLE);
        }
    }

    private void updateFavoriteIcon() {
        Log.d("RecipeDetailActivity", "Updating favorite icon state with " + isFavorite);
        fabFavorite.setImageResource(isFavorite ?
                R.drawable.ic_favorite_filled :
                R.drawable.ic_favorite_outline);
    }

    private void displayRecipeDetails() {
        if (currentRecipe == null) {
            Log.e("RecipeDetailActivity", "currentRecipe is null in displayRecipeDetails");
            return;
        }

        Log.d("RecipeDetailActivity", "Displaying recipe details for: " + currentRecipe.getTitle());

        // Set title
        if (currentRecipe.getTitle() != null) {
            collapsingToolbar.setTitle(currentRecipe.getTitle());
        }

        // Load recipe image
        if (currentRecipe.getImageUrl() != null && !currentRecipe.getImageUrl().isEmpty()) {
            Glide.with(this)
                    .load(currentRecipe.getImageUrl())
                    .centerCrop()
                    .placeholder(R.drawable.placeholder_recipe)
                    .error(R.drawable.placeholder_recipe)
                    .into(ivRecipeImage);
        } else {
            ivRecipeImage.setImageResource(R.drawable.placeholder_recipe);
        }

        // Set basic info with null checks
        if (currentRecipe.getDescription() != null) {
            tvDescription.setText(currentRecipe.getDescription());
        } else {
            tvDescription.setText("No description available");
        }

        if (currentRecipe.getCookingTime() != null) {
            tvCookTime.setText(currentRecipe.getCookingTime());
        } else {
            tvCookTime.setText("N/A");
        }

        tvServings.setText(String.valueOf(currentRecipe.getServings()));

        if (currentRecipe.getDifficulty() != null) {
            tvDifficulty.setText(currentRecipe.getDifficulty());
        } else {
            tvDifficulty.setText("N/A");
        }

        if (currentRecipe.getAuthorName() != null) {
            tvAuthor.setText("By " + currentRecipe.getAuthorName());
        } else {
            tvAuthor.setText("By Unknown Chef");
        }

        tvRating.setText(String.format("%.1f", currentRecipe.getRating()));

        // Add tags with null check
        chipGroupTags.removeAllViews();
        if (currentRecipe.getTags() != null && !currentRecipe.getTags().isEmpty()) {
            for (String tag : currentRecipe.getTags()) {
                Chip chip = new Chip(this);
                chip.setText(tag);
                chip.setClickable(false);
                chipGroupTags.addView(chip);
            }
        } else {
            // Add a default tag if no tags exist
            Chip chip = new Chip(this);
            chip.setText("Recipe");
            chip.setClickable(false);
            chipGroupTags.addView(chip);
        }

        // Set ingredients with null check
        if (currentRecipe.getIngredients() != null && !currentRecipe.getIngredients().isEmpty()) {
            ingredientsAdapter.setIngredients(currentRecipe.getIngredients());
        } else {
            // Set placeholder ingredients
            ingredientsAdapter.setIngredients(java.util.Arrays.asList("No ingredients listed"));
        }

        // Set instructions with null check
        if (currentRecipe.getInstructions() != null && !currentRecipe.getInstructions().isEmpty()) {
            instructionsAdapter.setInstructions(currentRecipe.getInstructions());
        } else {
            // Set placeholder instructions
            instructionsAdapter.setInstructions(java.util.Arrays.asList("No instructions provided"));
        }

        Log.d("RecipeDetailActivity", "Recipe details displayed successfully");
    }

    private void toggleFavorite() {
        isFavorite = !isFavorite;
        fabFavorite.setImageResource(isFavorite ?
                R.drawable.ic_favorite_filled : R.drawable.ic_favorite_outline);

        Toast.makeText(this,
                isFavorite ? "Added to favorites" : "Removed from favorites",
                Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_recipe_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (itemId == R.id.action_share) {
            shareRecipe();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void shareRecipe() {
        if (currentRecipe != null) {
            String shareText = "Check out this recipe: " + currentRecipe.getTitle() +
                    "\n" + currentRecipe.getDescription() +
                    "\n\nShared from RecipeVault";

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);
            startActivity(Intent.createChooser(shareIntent, "Share Recipe"));
        }
    }
}
package com.recipevault.activity;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.recipevault.MainActivity;
import com.recipevault.R;

public class AddRecipeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private MaterialToolbar toolbar;
    private TextInputEditText etRecipeTitle, etRecipeDescription, etCookTime, etServings;
    private ChipGroup chipGroupDifficulty;
    private RecyclerView rvIngredients, rvInstructions;
    private MaterialButton btnSaveRecipe, btnAddIngredient, btnAddInstruction;
    private LinearLayout addImageContainer;
    private ImageView recipeImagePreview;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check authentication before proceeding
        if (!isUserAuthenticated()) {
            redirectToLogin();
            return;
        }

        setContentView(R.layout.activity_add_recipe);

        initViews();
        setupToolbar();
        setupBottomNavigation();
        setupClickListeners();
        setupRecyclerViews();
    }

    private boolean isUserAuthenticated() {
        SharedPreferences prefs = getSharedPreferences("RecipeVault", MODE_PRIVATE);
        return prefs.getBoolean("is_logged_in", false);
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, SignInActivity.class);
        startActivity(intent);
        finish();
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar);
        bottomNavigation = findViewById(R.id.bottom_navigation);
        etRecipeTitle = findViewById(R.id.et_recipe_title);
        etRecipeDescription = findViewById(R.id.et_recipe_description);
        etCookTime = findViewById(R.id.et_cook_time);
        etServings = findViewById(R.id.et_servings);
        chipGroupDifficulty = findViewById(R.id.chip_group_difficulty);
        rvIngredients = findViewById(R.id.rv_ingredients);
        rvInstructions = findViewById(R.id.rv_instructions);
        btnSaveRecipe = findViewById(R.id.btn_save_recipe);
        btnAddIngredient = findViewById(R.id.btn_add_ingredient);
        btnAddInstruction = findViewById(R.id.btn_add_instruction);
        addImageContainer = findViewById(R.id.add_image_container);
        recipeImagePreview = findViewById(R.id.recipe_image_preview);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupBottomNavigation() {
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_search) {
                // TODO: Create SearchActivity
                Toast.makeText(this, "Search feature coming soon", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.nav_favorites) {
                // TODO: Create FavoritesActivity
                Toast.makeText(this, "Favorites feature coming soon", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.nav_profile) {
                // TODO: Create ProfileActivity
                Toast.makeText(this, "Profile feature coming soon", Toast.LENGTH_SHORT).show();
                return true;
            }

            return false;
        });
    }

    private void setupClickListeners() {
        addImageContainer.setOnClickListener(v -> openImagePicker());
        btnAddIngredient.setOnClickListener(v -> addIngredientField());
        btnAddInstruction.setOnClickListener(v -> addInstructionField());
        btnSaveRecipe.setOnClickListener(v -> saveRecipe());
    }

    private void setupRecyclerViews() {
        rvIngredients.setLayoutManager(new LinearLayoutManager(this));
        rvInstructions.setLayoutManager(new LinearLayoutManager(this));
    }

    private void openImagePicker() {
        Toast.makeText(this, "Image picker functionality", Toast.LENGTH_SHORT).show();
    }

    private void addIngredientField() {
        Toast.makeText(this, "Add ingredient field", Toast.LENGTH_SHORT).show();
    }

    private void addInstructionField() {
        Toast.makeText(this, "Add instruction field", Toast.LENGTH_SHORT).show();
    }

    private void saveRecipe() {
        if (validateInputs()) {
            Toast.makeText(this, "Recipe saved successfully!", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private boolean validateInputs() {
        if (etRecipeTitle.getText().toString().trim().isEmpty()) {
            etRecipeTitle.setError("Recipe title is required");
            return false;
        }
        if (etRecipeDescription.getText().toString().trim().isEmpty()) {
            etRecipeDescription.setError("Description is required");
            return false;
        }
        return true;
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
package com.recipevault.activity;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.util.Base64;
import android.view.MenuItem;
import android.view.View;
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
import com.recipevault.adapter.InstructionsAdapter;
import com.recipevault.adapter.IngredientsAdapter;

import com.recipevault.model.IngredientInput;
import com.recipevault.model.InstructionInput;
import com.recipevault.model.Recipe;
import com.recipevault.service.FirestoreService;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.MultipartBody;
import com.google.firebase.auth.FirebaseAuth;

public class AddRecipeActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private MaterialToolbar toolbar;
    private TextInputEditText etRecipeTitle, etRecipeDescription, etCookTime, etServings;
    private ChipGroup chipGroupDifficulty;
    private RecyclerView rvIngredients, rvInstructions;
    private MaterialButton btnSaveRecipe, btnAddIngredient, btnAddInstruction;
    private LinearLayout addImageContainer;
    private ImageView recipeImagePreview;

    private IngredientsAdapter ingredientInputAdapter;
    private InstructionsAdapter instructionInputAdapter;

    @Inject
    FirestoreService firestoreService;

    @Inject
    FirebaseAuth firebaseAuth;

    private Uri selectedImageUri;
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String UPLOAD_API_URL = "https://freeimage.host/api/1/upload";
    private static final String API_KEY = "YOUR_API_KEY"; // Replace with your actual API key

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
        ingredientInputAdapter = new IngredientsAdapter();
        rvIngredients.setAdapter(ingredientInputAdapter);

        rvInstructions.setLayoutManager(new LinearLayoutManager(this));
        instructionInputAdapter = new InstructionsAdapter();
        rvInstructions.setAdapter(instructionInputAdapter);
    }

    private void openImagePicker() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            selectedImageUri = data.getData();
            recipeImagePreview.setImageURI(selectedImageUri);
            addImageContainer.setVisibility(View.GONE);
        }
    }

    private void uploadImageAndSaveRecipe() {
        if (selectedImageUri == null) {
            saveRecipeToFirestore(null);
            return;
        }

        try {
            // Convert Uri to Base64
            ContentResolver contentResolver = getContentResolver();
            InputStream inputStream = contentResolver.openInputStream(selectedImageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            String base64Image = Base64.encodeToString(byteArray, Base64.DEFAULT);

            // Create request body
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("key", API_KEY)
                    .addFormDataPart("action", "upload")
                    .addFormDataPart("source", base64Image)
                    .addFormDataPart("format", "json")
                    .build();

            // Create request
            Request request = new Request.Builder()
                    .url(UPLOAD_API_URL)
                    .post(requestBody)
                    .build();

            // Execute request
            new OkHttpClient().newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> {
                        Toast.makeText(AddRecipeActivity.this, "Failed to upload image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String jsonResponse = response.body().string();
                        try {
                            JSONObject jsonObject = new JSONObject(jsonResponse);
                            JSONObject image = jsonObject.getJSONObject("image");
                            String imageUrl = image.getString("url");
                            runOnUiThread(() -> saveRecipeToFirestore(imageUrl));
                        } catch (JSONException e) {
                            runOnUiThread(() -> {
                                Toast.makeText(AddRecipeActivity.this, "Failed to parse image response", Toast.LENGTH_SHORT).show();
                            });
                        }
                    } else {
                        runOnUiThread(() -> {
                            Toast.makeText(AddRecipeActivity.this, "Failed to upload image: " + response.message(), Toast.LENGTH_SHORT).show();
                        });
                    }
                }
            });

        } catch (IOException e) {
            Toast.makeText(this, "Error processing image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void saveRecipeToFirestore(String imageUrl) {
        if (!validateInputs()) return;

        String userId = firebaseAuth.getCurrentUser().getUid();
        String userName = firebaseAuth.getCurrentUser().getDisplayName();
        if (userName == null || userName.isEmpty()) {
            userName = "Anonymous Chef";
        }

        // Get selected difficulty
        String difficulty = "Easy";
        int selectedChipId = chipGroupDifficulty.getCheckedChipId();
        if (selectedChipId == R.id.chip_medium) {
            difficulty = "Medium";
        } else if (selectedChipId == R.id.chip_hard) {
            difficulty = "Hard";
        }

        String title = etRecipeTitle.getText().toString().trim();
        String description = etRecipeDescription.getText().toString().trim();
        String cookTime = etCookTime.getText().toString() + " minutes";
        int servings = Integer.parseInt(etServings.getText().toString());

        // Create recipe using constructor
        Recipe recipe = new Recipe(null, title, description, imageUrl,
                                 cookTime, difficulty, servings, userName);

        // Set additional fields
        recipe.setCreatorUserId(userId);
        recipe.setCreatedAt(new Date());
        recipe.setRating(0.0f);
        recipe.setTimestamp(System.currentTimeMillis());

        // Get ingredients
        List<String> ingredients = new ArrayList<>();
        for (int i = 0; i < ingredientInputAdapter.getItemCount(); i++) {
            IngredientInput ingredient = ingredientInputAdapter.getIngredientAt(i);
            if (!ingredient.getName().isEmpty()) {
                String ingredientText = ingredient.getName();
                if (!ingredient.getAmount().isEmpty()) {
                    ingredientText += ", " + ingredient.getAmount();
                }
                ingredients.add(ingredientText);
            }
        }
        recipe.setIngredients(ingredients);

        // Get instructions
        List<String> instructions = new ArrayList<>();
        for (InstructionInput instruction : instructionInputAdapter.getInstructions()) {
            if (!instruction.getText().isEmpty()) {
                instructions.add(instruction.getText());
            }
        }
        recipe.setInstructions(instructions);

        // Set some default tags based on ingredients
        Set<String> tags = new HashSet<>();
        for (String ingredient : ingredients) {
            tags.add(ingredient.split(",")[0].toLowerCase().trim());
        }
        recipe.setTags(new ArrayList<>(tags));

        // Save to Firestore
        firestoreService.createRecipe(recipe)
                .addOnSuccessListener(documentReference -> {
                    Toast.makeText(this, "Recipe saved successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to save recipe: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void saveRecipe() {
        if (validateInputs()) {
            uploadImageAndSaveRecipe();
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

    private void addIngredientField() {
        ingredientInputAdapter.addIngredient(new IngredientInput("", ""));
    }

    private void addInstructionField() {
        instructionInputAdapter.addInstruction(new InstructionInput(""));
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


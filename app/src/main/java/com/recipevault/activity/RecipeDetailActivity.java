package com.recipevault.activity;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.recipevault.R;
import com.recipevault.adapter.CommentAdapter;
import com.recipevault.adapter.IngredientsAdapter;
import com.recipevault.adapter.InstructionsAdapter;
import com.recipevault.model.Comment;
import com.recipevault.model.Recipe;
import com.recipevault.service.CommentService;
import com.recipevault.service.FirestoreService;

import javax.inject.Inject;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class RecipeDetailActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private CollapsingToolbarLayout collapsingToolbar;
    private ImageView ivRecipeImage;
    private TextView tvDescription, tvCookTime, tvServings, tvDifficulty, tvAuthor, tvRating;
    private TextView tvRatingText, tvCommentsCount, tvNoComments;
    private ChipGroup chipGroupTags;
    private RecyclerView rvIngredients, rvInstructions, rvComments;
    private FloatingActionButton fabFavorite;
    private RatingBar ratingBar;
    private Button btnSubmitRating;
    private TextInputEditText etComment;
    private Button btnSetReminder;

    private boolean isFavorite = false;
    private String recipeId;
    private String recipeTitle;
    private Recipe currentRecipe;
    private IngredientsAdapter ingredientsAdapter;
    private InstructionsAdapter instructionsAdapter;
    private CommentAdapter commentAdapter;
    @Inject
    FirestoreService firestoreService;
    @Inject
    FirebaseAuth firebaseAuth;
    @Inject
    CommentService commentService;

    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 1001;

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
        loadComments();
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
        rvComments = findViewById(R.id.rv_comments);
        fabFavorite = findViewById(R.id.fab_favorite);
        
        // Rating and commenting views
        ratingBar = findViewById(R.id.rating_bar);
        tvRatingText = findViewById(R.id.tv_rating_text);
        btnSubmitRating = findViewById(R.id.btn_submit_rating);
        etComment = findViewById(R.id.et_comment);
        tvCommentsCount = findViewById(R.id.tv_comments_count);
        tvNoComments = findViewById(R.id.tv_no_comments);
        btnSetReminder = findViewById(R.id.btn_set_reminder);
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
        rvIngredients.setLayoutManager(new LinearLayoutManager(this));
        ingredientsAdapter = new IngredientsAdapter();
        rvIngredients.setAdapter(ingredientsAdapter);

        rvInstructions.setLayoutManager(new LinearLayoutManager(this));
        instructionsAdapter = new InstructionsAdapter();
        rvInstructions.setAdapter(instructionsAdapter);

        rvComments.setLayoutManager(new LinearLayoutManager(this));
        commentAdapter = new CommentAdapter();

        String currentUserId = firebaseAuth.getCurrentUser() != null ? firebaseAuth.getCurrentUser().getUid() : null;
        commentAdapter.setCurrentUserId(currentUserId);
        commentAdapter.setOnLikeClickListener((comment, position, liked) -> handleLikeClick(comment, position, liked));
        rvComments.setAdapter(commentAdapter);
    }

    private void setupClickListeners() {
        fabFavorite.setOnClickListener(v -> toggleFavorite());
        ratingBar.setOnRatingBarChangeListener((ratingBar, rating, fromUser) -> {
            tvRatingText.setText(String.format("%.1f", rating));
        });
        btnSubmitRating.setOnClickListener(v -> submitRatingAndComment());
        btnSetReminder.setOnClickListener(v -> checkAndRequestExactAlarmPermission());
    }

    private void checkAndRequestExactAlarmPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(android.content.Context.ALARM_SERVICE);
            if (!alarmManager.canScheduleExactAlarms()) {
                // Prompt user to allow exact alarms in system settings
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
                builder.setTitle("Allow Exact Alarms")
                    .setMessage("To set reminders, please allow exact alarms for Recipe Vault in system settings.")
                    .setPositiveButton("Go to Settings", (dialog, which) -> {
                        Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
                return;
            }
        }
        checkAndRequestNotificationPermission();
    }

    private void checkAndRequestNotificationPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE_POST_NOTIFICATIONS);
            } else {
                showTimePickerDialog();
            }
        } else {
            showTimePickerDialog();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showTimePickerDialog();
            } else {
                Toast.makeText(this, "Notification permission denied. Cannot set reminder.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showTimePickerDialog() {
        java.util.Calendar calendar = java.util.Calendar.getInstance();
        int hour = calendar.get(java.util.Calendar.HOUR_OF_DAY);
        int minute = calendar.get(java.util.Calendar.MINUTE);
        android.app.TimePickerDialog timePickerDialog = new android.app.TimePickerDialog(this, (view, hourOfDay, minute1) -> {
            calendar.set(java.util.Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(java.util.Calendar.MINUTE, minute1);
            calendar.set(java.util.Calendar.SECOND, 0);
            scheduleReminder(calendar.getTimeInMillis());
        }, hour, minute, true);
        timePickerDialog.show();
    }

    private void scheduleReminder(long triggerAtMillis) {
        android.app.AlarmManager alarmManager = (android.app.AlarmManager) getSystemService(android.content.Context.ALARM_SERVICE);
        Intent intent = new Intent(this, com.recipevault.receiver.ReminderReceiver.class);
        intent.putExtra("recipeTitle", recipeTitle);
        intent.putExtra("recipeId", recipeId);
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(this, recipeId.hashCode(), intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
        alarmManager.setExact(android.app.AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
        Toast.makeText(this, "Reminder set!", Toast.LENGTH_SHORT).show();
    }

    private void loadRecipeDetails() {
        if (recipeId == null || recipeId.isEmpty()) {
            Log.e("RecipeDetailActivity", "Recipe ID is null or empty");
            Toast.makeText(this, "Recipe ID not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        Log.d("RecipeDetailActivity", "Loading recipe with ID: " + recipeId);

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

        if (currentRecipe.getTitle() != null) {
            collapsingToolbar.setTitle(currentRecipe.getTitle());
        }

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

        chipGroupTags.removeAllViews();
        if (currentRecipe.getTags() != null && !currentRecipe.getTags().isEmpty()) {
            for (String tag : currentRecipe.getTags()) {
                Chip chip = new Chip(this);
                chip.setText(tag);
                chip.setClickable(false);
                chipGroupTags.addView(chip);
            }
        } else {
            Chip chip = new Chip(this);
            chip.setText("Recipe");
            chip.setClickable(false);
            chipGroupTags.addView(chip);
        }

        if (currentRecipe.getIngredients() != null && !currentRecipe.getIngredients().isEmpty()) {
            ingredientsAdapter.setIngredients(currentRecipe.getIngredients());
        } else {
            ingredientsAdapter.setIngredients(java.util.Arrays.asList("No ingredients listed"));
        }

        if (currentRecipe.getInstructions() != null && !currentRecipe.getInstructions().isEmpty()) {
            instructionsAdapter.setInstructions(currentRecipe.getInstructions());
        } else {
            // Set placeholder instructions
            instructionsAdapter.setInstructions(java.util.Arrays.asList("No instructions provided"));
        }

        Log.d("RecipeDetailActivity", "Recipe details displayed successfully");
    }

    private void loadComments() {
        if (recipeId == null || recipeId.isEmpty()) {
            return;
        }
        commentService.loadComments(recipeId, new CommentService.CommentListCallback() {
            @Override
            public void onSuccess(List<Comment> comments) {
                commentAdapter.setComments(comments);
                updateCommentsUI(comments.size());
            }
            @Override
            public void onFailure(Exception e) {
                Log.e("RecipeDetailActivity", "Failed to load comments", e);
                Toast.makeText(RecipeDetailActivity.this, "Failed to load comments", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateCommentsUI(int commentCount) {
        tvCommentsCount.setText("(" + commentCount + ")");
        if (commentCount == 0) {
            tvNoComments.setVisibility(View.VISIBLE);
            rvComments.setVisibility(View.GONE);
        } else {
            tvNoComments.setVisibility(View.GONE);
            rvComments.setVisibility(View.VISIBLE);
        }
    }

    private void submitRatingAndComment() {
        if (!isUserAuthenticated()) {
            Toast.makeText(this, "Please sign in to rate and comment", Toast.LENGTH_SHORT).show();
            return;
        }
        float rating = ratingBar.getRating();
        String commentText = etComment.getText().toString().trim();
        if (rating == 0) {
            Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show();
            return;
        }
        if (commentText.isEmpty()) {
            Toast.makeText(this, "Please enter a comment", Toast.LENGTH_SHORT).show();
            return;
        }
        FirebaseUser currentUser = firebaseAuth.getCurrentUser();
        String userId = currentUser.getUid();
        String username = currentUser.getDisplayName();
        if (username == null || username.isEmpty()) {
            username = currentUser.getEmail();
        }
        Map<String, Object> ratingData = new java.util.HashMap<>();
        ratingData.put("userId", userId);
        ratingData.put("rating", rating);
        ratingData.put("timestamp", System.currentTimeMillis());
        Comment comment = new Comment(userId, username, commentText, recipeId, rating);
        comment.setUserAvatarUrl(currentUser.getPhotoUrl() != null ? currentUser.getPhotoUrl().toString() : null);
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        // Submit rating
        db.collection("recipes")
                .document(recipeId)
                .collection("ratings")
                .document(userId)
                .set(ratingData)
                .addOnSuccessListener(aVoid -> {
                    // Submit comment after rating is successful
                    commentService.addComment(recipeId, comment, new CommentService.CommentActionCallback() {
                        @Override
                        public void onSuccess(Comment addedComment) {
                            commentAdapter.addComment(addedComment);
                            updateCommentsUI(commentAdapter.getItemCount());
                            // Clear the form
                            ratingBar.setRating(0);
                            tvRatingText.setText("0.0");
                            etComment.setText("");
                            Toast.makeText(RecipeDetailActivity.this, "Rating and comment submitted successfully!", Toast.LENGTH_SHORT).show();
                            loadRecipeDetails();
                        }
                        @Override
                        public void onFailure(Exception e) {
                            Log.e("RecipeDetailActivity", "Failed to submit comment", e);
                            Toast.makeText(RecipeDetailActivity.this, "Rating submitted but comment failed", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("RecipeDetailActivity", "Failed to submit rating", e);
                    Toast.makeText(this, "Failed to submit rating and comment", Toast.LENGTH_SHORT).show();
                });
    }

    private void toggleFavorite() {
        isFavorite = !isFavorite;
        fabFavorite.setImageResource(isFavorite ?
                R.drawable.ic_favorite_filled : R.drawable.ic_favorite_outline);
        String userId = firebaseAuth.getUid();

        DocumentReference userRef = FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId);
        userRef.update("savedRecipes", isFavorite ?
                FieldValue.arrayUnion(recipeId) :
                FieldValue.arrayRemove(recipeId)
        ).addOnSuccessListener(unused -> {
            Log.d("Favorite", "Favorite updated");
            Toast.makeText(this,
                    isFavorite ? "Added to favorites" : "Removed from favorites",
                    Toast.LENGTH_SHORT).show();
        }).addOnFailureListener(e -> {
            Log.e("Favorite", "Failed to update favorite", e);
            Toast.makeText(this,
                    "Failed to update favorite status",
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void handleLikeClick(Comment comment, int position, boolean liked) {
        String userId = firebaseAuth.getCurrentUser() != null ? firebaseAuth.getCurrentUser().getUid() : null;
        if (userId == null) {
            Toast.makeText(this, "Please sign in to like comments", Toast.LENGTH_SHORT).show();
            return;
        }
        int newLikeCount = liked ? comment.getLikeCount() + 1 : Math.max(0, comment.getLikeCount() - 1);
        commentService.likeComment(comment.getRecipeId(), comment.getCommentId(), userId, liked, newLikeCount, new CommentService.LikeActionCallback() {
            @Override
            public void onSuccess() {
                commentAdapter.notifyItemChanged(position);
            }
            @Override
            public void onFailure(Exception e) {
                Toast.makeText(RecipeDetailActivity.this, "Failed to update like", Toast.LENGTH_SHORT).show();
            }
        });
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
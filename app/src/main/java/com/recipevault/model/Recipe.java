package com.recipevault.model;

import java.util.List;
import java.util.Map;

public class Recipe {
    private String recipeId;
    private String title;
    private String description;
    private String imageUrl;
    private List<String> imageUrls;
    private String category;
    private List<String> tags;
    private String creatorUserId;
    private String creatorUsername;
    private boolean isPremium;
    private double rating;
    private int ratingCount;
    private int likeCount;
    private int commentCount;
    private List<String> ingredients;
    private List<String> instructions;
    private int prepTime; // in minutes
    private int cookTime; // in minutes
    private int servings;
    private String difficulty; // Easy, Medium, Hard
    private Map<String, String> nutritionInfo;
    private long createdAt;
    private long updatedAt;

    // Constructors
    public Recipe() {} // Required for Firestore

    public Recipe(String title, String description, String creatorUserId) {
        this.title = title;
        this.description = description;
        this.creatorUserId = creatorUserId;
        this.isPremium = false;
        this.rating = 0.0;
        this.ratingCount = 0;
        this.likeCount = 0;
        this.commentCount = 0;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters and setters
    public String getRecipeId() { return recipeId; }
    public void setRecipeId(String recipeId) { this.recipeId = recipeId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }

    public String getCreatorUserId() { return creatorUserId; }
    public void setCreatorUserId(String creatorUserId) { this.creatorUserId = creatorUserId; }

    public String getCreatorUsername() { return creatorUsername; }
    public void setCreatorUsername(String creatorUsername) { this.creatorUsername = creatorUsername; }

    public boolean isPremium() { return isPremium; }
    public void setPremium(boolean premium) { isPremium = premium; }

    public double getRating() { return rating; }
    public void setRating(double rating) { this.rating = rating; }

    public int getRatingCount() { return ratingCount; }
    public void setRatingCount(int ratingCount) { this.ratingCount = ratingCount; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public int getCommentCount() { return commentCount; }
    public void setCommentCount(int commentCount) { this.commentCount = commentCount; }

    public List<String> getIngredients() { return ingredients; }
    public void setIngredients(List<String> ingredients) { this.ingredients = ingredients; }

    public List<String> getInstructions() { return instructions; }
    public void setInstructions(List<String> instructions) { this.instructions = instructions; }

    public int getPrepTime() { return prepTime; }
    public void setPrepTime(int prepTime) { this.prepTime = prepTime; }

    public int getCookTime() { return cookTime; }
    public void setCookTime(int cookTime) { this.cookTime = cookTime; }

    public int getServings() { return servings; }
    public void setServings(int servings) { this.servings = servings; }

    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }

    public Map<String, String> getNutritionInfo() { return nutritionInfo; }
    public void setNutritionInfo(Map<String, String> nutritionInfo) { this.nutritionInfo = nutritionInfo; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }
}
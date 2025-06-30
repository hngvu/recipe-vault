package com.recipevault.model;

public class Rating {
    private String ratingId;
    private String userId;
    private String recipeId;
    private int value; // 1-5 stars
    private boolean isActive;
    private long timestamp;

    public Rating() {} // Required for Firestore

    public Rating(String userId, String recipeId, int value) {
        this.userId = userId;
        this.recipeId = recipeId;
        this.value = value;
        this.isActive = true;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and setters
    public String getRatingId() { return ratingId; }
    public void setRatingId(String ratingId) { this.ratingId = ratingId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getRecipeId() { return recipeId; }
    public void setRecipeId(String recipeId) { this.recipeId = recipeId; }

    public int getValue() { return value; }
    public void setValue(int value) { this.value = value; }

    public boolean isActive() { return isActive; }
    public void setActive(boolean active) { isActive = active; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
}
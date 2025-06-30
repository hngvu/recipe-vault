package com.recipevault.model;

import java.util.Map;

public class CookingReminder {
    private String reminderId;
    private String userId;
    private String recipeId;
    private String recipeTitle;
    private String recipeImageUrl;
    private long scheduledTime;
    private String type; // "cook", "prep", "custom"
    private String message;
    private boolean isActive;
    private boolean isCompleted;
    private long createdAt;
    private long updatedAt;
    private Map<String, Object> metadata;

    // Default constructor required for Firestore
    public CookingReminder() {}

    // Constructor for creating new reminders
    public CookingReminder(String userId, String recipeId, String recipeTitle, long scheduledTime, String type) {
        this.userId = userId;
        this.recipeId = recipeId;
        this.recipeTitle = recipeTitle;
        this.scheduledTime = scheduledTime;
        this.type = type;
        this.isActive = true;
        this.isCompleted = false;
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Full constructor
    public CookingReminder(String userId, String recipeId, String recipeTitle, String recipeImageUrl,
                           long scheduledTime, String type, String message) {
        this(userId, recipeId, recipeTitle, scheduledTime, type);
        this.recipeImageUrl = recipeImageUrl;
        this.message = message;
    }

    // Getters and Setters
    public String getReminderId() {
        return reminderId;
    }

    public void setReminderId(String reminderId) {
        this.reminderId = reminderId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(String recipeId) {
        this.recipeId = recipeId;
    }

    public String getRecipeTitle() {
        return recipeTitle;
    }

    public void setRecipeTitle(String recipeTitle) {
        this.recipeTitle = recipeTitle;
    }

    public String getRecipeImageUrl() {
        return recipeImageUrl;
    }

    public void setRecipeImageUrl(String recipeImageUrl) {
        this.recipeImageUrl = recipeImageUrl;
    }

    public long getScheduledTime() {
        return scheduledTime;
    }

    public void setScheduledTime(long scheduledTime) {
        this.scheduledTime = scheduledTime;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
        this.updatedAt = System.currentTimeMillis();
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    // Utility methods
    public boolean isPastDue() {
        return System.currentTimeMillis() > scheduledTime;
    }

    public boolean isUpcoming() {
        return System.currentTimeMillis() < scheduledTime && isActive && !isCompleted;
    }

    public void markAsCompleted() {
        this.isCompleted = true;
        this.updatedAt = System.currentTimeMillis();
    }

    public void cancel() {
        this.isActive = false;
        this.updatedAt = System.currentTimeMillis();
    }

    // Static constants for reminder types
    public static class ReminderType {
        public static final String COOK = "cook";
        public static final String PREP = "prep";
        public static final String CUSTOM = "custom";
        public static final String SHOPPING = "shopping";
        public static final String MEAL_PREP = "meal_prep";
    }

    @Override
    public String toString() {
        return "CookingReminder{" +
                "reminderId='" + reminderId + '\'' +
                ", userId='" + userId + '\'' +
                ", recipeId='" + recipeId + '\'' +
                ", recipeTitle='" + recipeTitle + '\'' +
                ", scheduledTime=" + scheduledTime +
                ", type='" + type + '\'' +
                ", isActive=" + isActive +
                ", isCompleted=" + isCompleted +
                '}';
    }
}
package com.recipevault.model;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Comment {
    private String commentId;
    private String userId;
    private String username;
    private String userAvatarUrl;
    private String text;
    private long timestamp;
    private int likeCount;
    private String recipeId;
    private List<String> likedUserIds;
    private float rating;

    public Comment() {
    } // Required for Firestore

    public Comment(String userId, String username, String text, String recipeId, float rating) {
        this.userId = userId;
        this.username = username;
        this.text = text;
        this.recipeId = recipeId;
        this.timestamp = System.currentTimeMillis();
        this.likeCount = 0;
        this.likedUserIds = new ArrayList<>();
        this.rating = rating;
    }

    // Getters and setters
    public String getCommentId() { return commentId; }
    public void setCommentId(String commentId) { this.commentId = commentId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getUserAvatarUrl() { return userAvatarUrl; }
    public void setUserAvatarUrl(String userAvatarUrl) { this.userAvatarUrl = userAvatarUrl; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public int getLikeCount() { return likeCount; }
    public void setLikeCount(int likeCount) { this.likeCount = likeCount; }

    public String getRecipeId() { return recipeId; }
    public void setRecipeId(String recipeId) { this.recipeId = recipeId; }

    public List<String> getLikedUserIds() {
        return likedUserIds;
    }

    public void setLikedUserIds(List<String> likedUserIds) {
        this.likedUserIds = likedUserIds;
    }

    public float getRating() { return rating; }
    public void setRating(float rating) { this.rating = rating; }
}
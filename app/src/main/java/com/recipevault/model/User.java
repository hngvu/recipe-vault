package com.recipevault.model;

import java.util.List;
import java.util.Map;

public class User {
    private String userId;
    private String username;
    private String email;
    private String avatarUrl;
    private String bio;
    private List<String> savedRecipes;
    private List<String> followingUsers;
    private List<String> followers;
    private boolean isPremium;
    private long createdAt;
    private Map<String, Object> preferences;

    // Constructors, getters, and setters
    public User() {} // Required for Firestore

    public User(String userId, String username, String email) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.isPremium = false;
        this.createdAt = System.currentTimeMillis();
    }

    // Getters and setters for all fields
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }

    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }

    public List<String> getSavedRecipes() { return savedRecipes; }
    public void setSavedRecipes(List<String> savedRecipes) { this.savedRecipes = savedRecipes; }

    public List<String> getFollowingUsers() { return followingUsers; }
    public void setFollowingUsers(List<String> followingUsers) { this.followingUsers = followingUsers; }

    public List<String> getFollowers() { return followers; }
    public void setFollowers(List<String> followers) { this.followers = followers; }

    public boolean isPremium() { return isPremium; }
    public void setPremium(boolean premium) { isPremium = premium; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public Map<String, Object> getPreferences() { return preferences; }
    public void setPreferences(Map<String, Object> preferences) { this.preferences = preferences; }
}

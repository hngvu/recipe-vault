package com.recipevault.service;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.recipevault.model.Comment;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;


@Singleton
public class CommentService {

    FirebaseFirestore db;

    @Inject
    public CommentService(FirebaseFirestore db) {
        this.db = db;
    }

    public interface CommentListCallback {
        void onSuccess(List<Comment> comments);
        void onFailure(Exception e);
    }

    public interface CommentActionCallback {
        void onSuccess(Comment comment);
        void onFailure(Exception e);
    }

    public interface LikeActionCallback {
        void onSuccess();
        void onFailure(Exception e);
    }

    // Load comments for a recipe
    public void loadComments(String recipeId, final CommentListCallback callback) {
        db.collection("recipes")
                .document(recipeId)
                .collection("comments")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<Comment> comments = new ArrayList<>();
                    try {
                        for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                            Comment comment = document.toObject(Comment.class);
                            if (comment != null) {
                                comment.setCommentId(document.getId());
                                comments.add(comment);
                            }
                        }
                        callback.onSuccess(comments);
                    } catch (Exception e) {
                        callback.onFailure(e);
                    }
                })
                .addOnFailureListener(callback::onFailure);
    }

    // Add a comment
    public void addComment(String recipeId, Comment comment, final CommentActionCallback callback) {
        db.collection("recipes")
                .document(recipeId)
                .collection("comments")
                .add(comment)
                .addOnSuccessListener(documentReference -> {
                    comment.setCommentId(documentReference.getId());
                    callback.onSuccess(comment);
                })
                .addOnFailureListener(callback::onFailure);
    }

    // Like or unlike a comment
    public void likeComment(String recipeId, String commentId, String userId, boolean like, int newLikeCount, final LikeActionCallback callback) {
        DocumentReference commentRef = db.collection("recipes")
                .document(recipeId)
                .collection("comments")
                .document(commentId);
        FieldValue arrayOp = like ? FieldValue.arrayUnion(userId) : FieldValue.arrayRemove(userId);
        Map<String, Object> updates = new HashMap<>();
        updates.put("likedUserIds", arrayOp);
        updates.put("likeCount", newLikeCount);
        commentRef.update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess())
                .addOnFailureListener(callback::onFailure);
    }
} 
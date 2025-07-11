package com.recipevault.repository;


import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.*;

import java.util.ArrayList;
import java.util.List;

public class FirestoreRepository<T> {

    private final FirebaseFirestore db;
    private final Class<T> modelClass;
    private final String collectionPath;

    public FirestoreRepository(FirebaseFirestore db, Class<T> modelClass, String collectionPath) {
        this.db = db;
        this.modelClass = modelClass;
        this.collectionPath = collectionPath;
    }

    public void add(T item, OnSuccessListener<DocumentReference> onSuccess, OnFailureListener onFailure) {
        db.collection(collectionPath)
                .add(item)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void getById(String id, OnSuccessListener<T> onSuccess, OnFailureListener onFailure) {
        db.collection(collectionPath).document(id).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        T item = document.toObject(modelClass);
                        onSuccess.onSuccess(item);
                    } else {
                        onSuccess.onSuccess(null); // or handle not found case
                    }
                })
                .addOnFailureListener(onFailure);
    }

    public void getAll(OnSuccessListener<List<T>> onSuccess, OnFailureListener onFailure) {
        db.collection(collectionPath)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<T> list = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        T item = doc.toObject(modelClass);
                        list.add(item);
                    }
                    onSuccess.onSuccess(list);
                })
                .addOnFailureListener(onFailure);
    }

    public void getWithLimit(int limit, OnSuccessListener<List<T>> onSuccess, OnFailureListener onFailure) {
        db.collection(collectionPath)
                .limit(limit)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    List<T> list = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        T item = doc.toObject(modelClass);
                        list.add(item);
                    }
                    onSuccess.onSuccess(list);
                })
                .addOnFailureListener(onFailure);
    }

    public void getNextPage(DocumentSnapshot lastVisible, int limit, OnSuccessListener<List<T>> onSuccess, OnFailureListener onFailure) {
        Query query = db.collection(collectionPath).limit(limit);
        if (lastVisible != null) {
            query = query.startAfter(lastVisible);
        }

        query.get()
                .addOnSuccessListener(snapshot -> {
                    List<T> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        T item = doc.toObject(modelClass);
                        list.add(item);
                    }
                    onSuccess.onSuccess(list);
                })
                .addOnFailureListener(onFailure);
    }

    public void deleteById(String id, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        db.collection(collectionPath)
                .document(id)
                .delete()
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void update(String id, T item, OnSuccessListener<Void> onSuccess, OnFailureListener onFailure) {
        db.collection(collectionPath)
                .document(id)
                .set(item)
                .addOnSuccessListener(onSuccess)
                .addOnFailureListener(onFailure);
    }

    public void getWhereEqualTo(String field, Object value, OnSuccessListener<List<T>> onSuccess, OnFailureListener onFailure) {
        db.collection(collectionPath)
                .whereEqualTo(field, value)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<T> list = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        T item = doc.toObject(modelClass);
                        list.add(item);
                    }
                    onSuccess.onSuccess(list);
                })
                .addOnFailureListener(onFailure);
    }

    public void getByDocumentIds(List<String> ids, OnSuccessListener<List<T>> onSuccess, OnFailureListener onFailure) {
        if (ids == null || ids.isEmpty()) {
            onSuccess.onSuccess(new ArrayList<>());
            return;
        }

        // Firestore supports max 10 items in whereIn
        List<String> limitedIds = ids.subList(0, Math.min(10, ids.size()));

        db.collection(collectionPath)
                .whereIn(FieldPath.documentId(), limitedIds)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<T> results = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        T item = doc.toObject(modelClass);
                        results.add(item);
                    }
                    onSuccess.onSuccess(results);
                })
                .addOnFailureListener(onFailure);
    }

}
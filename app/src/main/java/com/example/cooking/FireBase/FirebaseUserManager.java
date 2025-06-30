package com.example.cooking.FireBase;

import android.net.Uri;

import androidx.annotation.NonNull;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

/**
 * Класс для управления данными пользователя Firebase
 */
public class FirebaseUserManager {
    private static FirebaseUserManager instance;
    private final FirebaseAuth firebaseAuth;

    private FirebaseUserManager() {
        firebaseAuth = FirebaseAuth.getInstance();
    }

    public static synchronized FirebaseUserManager getInstance() {
        if (instance == null) {
            instance = new FirebaseUserManager();
        }
        return instance;
    }

    /**
     * Получение текущего пользователя
     */
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    /**
     * Получение UID пользователя
     */
    public String getUserId() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getUid() : null;
    }
}
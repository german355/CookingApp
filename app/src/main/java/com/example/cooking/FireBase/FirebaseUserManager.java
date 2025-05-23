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
     * @return объект текущего пользователя или null, если пользователь не авторизован
     */
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    /**
     * Получение UID пользователя
     * @return уникальный идентификатор пользователя или null, если пользователь не авторизован
     */
    public String getUserId() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getUid() : null;
    }

    /**
     * Получение email пользователя
     * @return email пользователя или null, если пользователь не авторизован или email не указан
     */
    public String getUserEmail() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getEmail() : null;
    }

    /**
     * Получение номера телефона пользователя
     * @return номер телефона пользователя или null, если пользователь не авторизован или телефон не указан
     */
    public String getUserPhoneNumber() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getPhoneNumber() : null;
    }

    /**
     * Получение отображаемого имени пользователя
     * @return отображаемое имя пользователя или null, если пользователь не авторизован или имя не указано
     */
    public String getDisplayName() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getDisplayName() : null;
    }

    /**
     * Получение URL фотографии пользователя
     * @return URL фотографии пользователя или null, если пользователь не авторизован или фото не указано
     */
    public Uri getPhotoUrl() {
        FirebaseUser user = getCurrentUser();
        return user != null ? user.getPhotoUrl() : null;
    }

    /**
     * Обновление профиля пользователя
     * @param displayName отображаемое имя пользователя
     * @param photoUri URI фотографии пользователя
     * @param callback интерфейс для обработки результата операции
     */
    public void updateProfile(String displayName, Uri photoUri, final UserProfileCallback callback) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            if (callback != null) {
                callback.onError(new Exception("User not signed in"));
            }
            return;
        }

        UserProfileChangeRequest.Builder profileUpdates = new UserProfileChangeRequest.Builder();
        
        if (displayName != null) {
            profileUpdates.setDisplayName(displayName);
        }
        
        if (photoUri != null) {
            profileUpdates.setPhotoUri(photoUri);
        }

        user.updateProfile(profileUpdates.build())
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (callback != null) {
                            callback.onSuccess();
                        }
                    } else {
                        if (callback != null) {
                            callback.onError(task.getException());
                        }
                    }
                });
    }

    /**
     * Обновление email пользователя
     * @param email новый email
     * @param callback интерфейс для обработки результата операции
     */
    public void updateEmail(String email, final UserProfileCallback callback) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            if (callback != null) {
                callback.onError(new Exception("User not signed in"));
            }
            return;
        }

        user.updateEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (callback != null) {
                            callback.onSuccess();
                        }
                    } else {
                        if (callback != null) {
                            callback.onError(task.getException());
                        }
                    }
                });
    }

    /**
     * Обновление пароля пользователя
     * @param password новый пароль
     * @param callback интерфейс для обработки результата операции
     */
    public void updatePassword(String password, final UserProfileCallback callback) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            if (callback != null) {
                callback.onError(new Exception("User not signed in"));
            }
            return;
        }

        user.updatePassword(password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (callback != null) {
                            callback.onSuccess();
                        }
                    } else {
                        if (callback != null) {
                            callback.onError(task.getException());
                        }
                    }
                });
    }

    /**
     * Отправка письма для сброса пароля
     * @param email email для отправки ссылки сброса пароля
     * @param callback интерфейс для обработки результата операции
     */
    public void sendPasswordResetEmail(String email, final UserProfileCallback callback) {
        firebaseAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (callback != null) {
                            callback.onSuccess();
                        }
                    } else {
                        if (callback != null) {
                            callback.onError(task.getException());
                        }
                    }
                });
    }

    /**
     * Отправка письма для верификации email
     * @param callback интерфейс для обработки результата операции
     */
    public void sendEmailVerification(final UserProfileCallback callback) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            if (callback != null) {
                callback.onError(new Exception("User not signed in"));
            }
            return;
        }

        user.sendEmailVerification()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (callback != null) {
                            callback.onSuccess();
                        }
                    } else {
                        if (callback != null) {
                            callback.onError(task.getException());
                        }
                    }
                });
    }

    /**
     * Проверка, верифицирован ли email пользователя
     * @return true, если email верифицирован, иначе false
     */
    public boolean isEmailVerified() {
        FirebaseUser user = getCurrentUser();
        return user != null && user.isEmailVerified();
    }

    /**
     * Удаление аккаунта пользователя
     * @param callback интерфейс для обработки результата операции
     */
    public void deleteAccount(final UserProfileCallback callback) {
        FirebaseUser user = getCurrentUser();
        if (user == null) {
            if (callback != null) {
                callback.onError(new Exception("User not signed in"));
            }
            return;
        }

        user.delete()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        if (callback != null) {
                            callback.onSuccess();
                        }
                    } else {
                        if (callback != null) {
                            callback.onError(task.getException());
                        }
                    }
                });
    }
} 
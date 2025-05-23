package com.example.cooking.auth;

import com.google.gson.annotations.SerializedName;

/**
 * Класс для запроса входа пользователя
 */
public class UserLoginRequest {
    
    @SerializedName("email")
    private String email;
    
    @SerializedName("uid")
    private String firebaseId;
    
    public UserLoginRequest(String email) {
        this.email = email;
    }
    
    public UserLoginRequest(String email, String firebaseId) {
        this.email = email;
        this.firebaseId = firebaseId;
    }
    
    // Геттеры и сеттеры
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getFirebaseId() {
        return firebaseId;
    }
    
    public void setFirebaseId(String firebaseId) {
        this.firebaseId = firebaseId;
    }
} 
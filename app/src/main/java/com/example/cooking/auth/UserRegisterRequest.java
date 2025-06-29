package com.example.cooking.auth;

import com.google.gson.annotations.SerializedName;

/**
 * Класс для запроса регистрации пользователя
 */
public class UserRegisterRequest {
    
    @SerializedName("email")
    private String email;
    
    @SerializedName("name")
    private String name;
    
    @SerializedName("uid")
    private String firebaseId;
    
    public UserRegisterRequest(String email, String name) {
        this.email = email;
        this.name = name;
    }
    
    public UserRegisterRequest(String email, String name, String firebaseId) {
        this.email = email;
        this.name = name;
        this.firebaseId = firebaseId;
    }
    
    // Геттеры и сеттеры
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getFirebaseId() {
        return firebaseId;
    }
    
    public void setFirebaseId(String firebaseId) {
        this.firebaseId = firebaseId;
    }
} 
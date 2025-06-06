package com.example.cooking.utils;

/**
 * Класс для работы с данными пользователя
 */
public class User {
    private String userId;
    private String username;
    private String email;
    private int permission;
    
    public User(String userId, String username, String email, int permission) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.permission = permission;
    }
    
    public String getUserId() {
        return userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getEmail() {
        return email;
    }
    
    public int getPermission() {
        return permission;
    }
}

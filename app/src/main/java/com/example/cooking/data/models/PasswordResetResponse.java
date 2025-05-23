package com.example.cooking.data.models;

import com.google.gson.annotations.SerializedName;

public class PasswordResetResponse {
    @SerializedName("message")
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
} 
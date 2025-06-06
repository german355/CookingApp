package com.example.cooking.data.models;

import com.google.gson.annotations.SerializedName;

public class PasswordResetRequest {
    @SerializedName("email")
    private String email;

    public PasswordResetRequest(String email) {
        this.email = email;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }
} 
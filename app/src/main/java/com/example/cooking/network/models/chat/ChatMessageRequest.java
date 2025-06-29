package com.example.cooking.network.models.chat;

import com.google.gson.annotations.SerializedName;

/**
 * DTO запроса для отправки сообщения в AI-чат
 */
public class ChatMessageRequest {
    @SerializedName("message")
    private String message;

    public ChatMessageRequest(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

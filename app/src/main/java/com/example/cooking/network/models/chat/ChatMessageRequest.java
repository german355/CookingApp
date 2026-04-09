package com.example.cooking.network.models.chat;

import com.google.gson.annotations.SerializedName;

/**
 * DTO запроса для отправки сообщения в AI-чат
 */
public class ChatMessageRequest {
    @SerializedName("message")
    private String message;

    @SerializedName("recipe_id")
    private Integer recipeId;

    @SerializedName("client_request_id")
    private String clientRequestId;

    public ChatMessageRequest(String message) {
        this(message, null, null);
    }

    public ChatMessageRequest(String message, Integer recipeId) {
        this(message, recipeId, null);
    }

    public ChatMessageRequest(String message, Integer recipeId, String clientRequestId) {
        this.message = message;
        this.recipeId = recipeId;
        this.clientRequestId = clientRequestId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Integer getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(Integer recipeId) {
        this.recipeId = recipeId;
    }

    public String getClientRequestId() {
        return clientRequestId;
    }

    public void setClientRequestId(String clientRequestId) {
        this.clientRequestId = clientRequestId;
    }
}

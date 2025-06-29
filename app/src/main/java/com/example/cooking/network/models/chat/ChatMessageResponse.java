package com.example.cooking.network.models.chat;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;

/**
 * DTO для ответа при отправке сообщения в AI-чат.
 */
public class ChatMessageResponse extends com.example.cooking.network.models.BaseApiResponse {
    @SerializedName("ai_response")
    private String aiResponse;
    @SerializedName("user_message")
    private String userMessage;

    @SerializedName("has_recipes")
    private boolean hasRecipes;

    @SerializedName("recipe_ids")
    private ArrayList<Integer> recipesIds;
    @SerializedName("user_id")
    private int userId;

    public String getAiResponse() {
        return aiResponse;
    }

    public void setAiResponse(String aiResponse) {
        this.aiResponse = aiResponse;
    }
    public ArrayList<Integer> getRecipesIds() {return recipesIds;}
    public void setRecipesIds(ArrayList<Integer> recipesIds) { this.recipesIds = recipesIds;}

    public boolean isHasRecipes() {
        return hasRecipes;
    }

    public boolean getHasRecipes() {
        return hasRecipes;
    }

    public void setHasRecipes(boolean hasRecipes) {
        this.hasRecipes = hasRecipes;
    }

    public String getUserMessage() {
        return userMessage;
    }

    public void setUserMessage(String userMessage) {
        this.userMessage = userMessage;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
}

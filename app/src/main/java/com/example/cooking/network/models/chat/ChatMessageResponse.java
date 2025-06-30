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

    public ArrayList<Integer> getRecipesIds() {return recipesIds;}


    public boolean getHasRecipes() {
        return hasRecipes;
    }


    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }
}

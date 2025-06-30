package com.example.cooking.network.models.chat;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Модель сообщения для AI-чата (DTO)
 */
public class ChatMessage {

    @SerializedName("is_user")
    private boolean isUser;

    @SerializedName("message")
    private String message;

    @SerializedName("timestamp")
    private String timestamp;

    @SerializedName("has_recipes")
    private boolean hasRecipes;

    @SerializedName("recipe_ids")
    private List<Integer> recipeIds;

    public boolean isUser() {
        return isUser;
    }

    public void setUser(boolean isUser) {
        this.isUser = isUser;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }



    public List<Integer> getRecipeIds() {
        return recipeIds;
    }

}

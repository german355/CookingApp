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

    @SerializedName("created_recipe_id")
    private Integer createdRecipeId;

    @SerializedName("recipe_presentation")
    private String recipePresentation;

    @SerializedName("recipe_flow_state")
    private String recipeFlowState;

    @SerializedName("recipe_interview_stage")
    private String recipeInterviewStage;

    @SerializedName("user_id")
    private int userId;

    @SerializedName("client_request_id")
    private String clientRequestId;

    public String getAiResponse() {
        return aiResponse;
    }

    public ArrayList<Integer> getRecipesIds() {return recipesIds;}

    public Integer getCreatedRecipeId() {
        return createdRecipeId;
    }

    public String getRecipePresentation() {
        return recipePresentation;
    }

    public String getRecipeFlowState() {
        return recipeFlowState;
    }

    public String getRecipeInterviewStage() {
        return recipeInterviewStage;
    }


    public boolean getHasRecipes() {
        return hasRecipes;
    }


    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getClientRequestId() {
        return clientRequestId;
    }

    public boolean hasRecipePayload() {
        return createdRecipeId != null || (recipesIds != null && !recipesIds.isEmpty());
    }
}

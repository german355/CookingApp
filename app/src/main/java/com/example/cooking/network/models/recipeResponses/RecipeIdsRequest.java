package com.example.cooking.network.models.recipeResponses;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class RecipeIdsRequest {
    @SerializedName("recipe_ids")
    private final List<Integer> recipeIds;

    public RecipeIdsRequest(List<Integer> recipeIds) {
        this.recipeIds = recipeIds;
    }

    public List<Integer> getRecipeIds() {
        return recipeIds;
    }
}

package com.example.cooking.network.responses;

import com.google.gson.annotations.SerializedName;
import java.util.ArrayList;
import java.util.List;
import com.example.cooking.network.models.BaseApiResponse;

/**
 * Класс для представления ответа с ID лайкнутых рецептов
 */
public class LikedRecipesResponse extends BaseApiResponse {
    @SerializedName("recipe_ids")
    private List<Integer> recipeIds;

    /**
     * Получает список ID лайкнутых рецептов
     * @return список ID
     */
    public List<Integer> getRecipeIds() {
        return recipeIds != null ? recipeIds : new ArrayList<>();
    }

    /**
     * Устанавливает список ID лайкнутых рецептов
     * @param recipeIds список ID
     */
    public void setRecipeIds(List<Integer> recipeIds) {
        this.recipeIds = recipeIds;
    }
}

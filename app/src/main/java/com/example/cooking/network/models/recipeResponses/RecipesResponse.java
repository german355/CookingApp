package com.example.cooking.network.models.recipeResponses;

import com.example.cooking.domain.entities.Recipe;
import com.example.cooking.network.models.BaseApiResponse;
import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Класс для представления ответа от сервера с рецептами
 */
public class RecipesResponse extends BaseApiResponse {
    
    @SerializedName("recipes")
    private List<Recipe> recipes;
    
    @SerializedName("count")
    private int count;
    
    /**
     * Получает список рецептов из ответа
     * @return список рецептов
     */
    public List<Recipe> getRecipes() {
        return recipes;
    }
    
    /**
     * Получает количество рецептов
     * @return количество рецептов
     */
    public int getCount() {
        return count;
    }
    
    /**
     * Устанавливает список рецептов
     * @param recipes список рецептов
     */
    public void setRecipes(List<Recipe> recipes) {
        this.recipes = recipes;
    }
    
    /**
     * Устанавливает количество рецептов
     * @param count количество рецептов
     */
    public void setCount(int count) {
        this.count = count;
    }
} 
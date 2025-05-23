package com.example.cooking.data.models;

import com.example.cooking.data.database.RecipeEntity;
import com.google.gson.annotations.SerializedName;

import java.util.List;

/**
 * Data Transfer Object (DTO) для ответа API при запросе лайкнутых рецептов.
 * Соответствует JSON структуре: {'success': True, 'recipes': [...], 'count': ..., 'message': '...' }
 */
public class LikedRecipesApiResponse {

    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message; // Может быть null при успехе

    // Важно: Используем RecipeEntity, так как сервер возвращает полные рецепты.
    // Убедитесь, что поля RecipeEntity соответствуют полям в JSON-массиве 'recipes'
    // или используйте @SerializedName в RecipeEntity, если имена полей отличаются (например, photo vs photo_url).
    @SerializedName("recipes")
    private List<RecipeEntity> recipes;

    @SerializedName("count")
    private int count;

    // Геттеры
    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public List<RecipeEntity> getRecipes() {
        return recipes;
    }

    public int getCount() {
        return count;
    }

    // Сеттеры (могут понадобиться для тестов или других целей)
    public void setSuccess(boolean success) {
        this.success = success;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setRecipes(List<RecipeEntity> recipes) {
        this.recipes = recipes;
    }

    public void setCount(int count) {
        this.count = count;
    }
} 
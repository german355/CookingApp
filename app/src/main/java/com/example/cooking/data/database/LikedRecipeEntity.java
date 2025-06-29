package com.example.cooking.data.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.example.cooking.domain.entities.Recipe;
import java.util.Objects;

/**
 * Представляет сущность "лайкнутого" рецепта в базе данных.
 * Хранит только ID рецепта, так как в приложении в один момент времени
 * может быть авторизован только один пользователь.
 */
@Entity(tableName = "liked_recipes")
public class LikedRecipeEntity {
    
    @PrimaryKey
    private int recipeId;    // Идентификатор лайкнутого рецепта (связь с RecipeEntity.id)

    public LikedRecipeEntity(int recipeId) {
        this.recipeId = recipeId;
    }

    public int getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(int recipeId) {
        this.recipeId = recipeId;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LikedRecipeEntity that = (LikedRecipeEntity) o;
        return recipeId == that.recipeId;
    }

    @Override
    public int hashCode() {
        return Objects.hash(recipeId);
    }

    @NonNull
    @Override
    public String toString() {
        return "LikedRecipeEntity{" +
                "recipeId=" + recipeId +
                '}';
    }
}
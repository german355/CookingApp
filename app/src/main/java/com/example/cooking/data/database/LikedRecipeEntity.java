package com.example.cooking.data.database;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import com.example.cooking.Recipe.Recipe;
import java.util.Objects;

/**
 * Представляет сущность "лайкнутого" рецепта в базе данных.
 * Хранит связь между ID рецепта и ID пользователя, который его лайкнул.
 * ВНИМАНИЕ: Текущая структура с recipeId как единственным @PrimaryKey может не подходить,
 * если разные пользователи должны иметь возможность лайкать один и тот же рецепт.
 * Рассмотрите использование составного первичного ключа (userId, recipeId).
 */
@Entity(tableName = "liked_recipes")
public class LikedRecipeEntity {
    
    // ВНИМАНИЕ: recipeId как единственный @PrimaryKey. 
    // Это значит, что каждый recipeId может быть в этой таблице только один раз.
    // Если разные пользователи могут лайкать один и тот же рецепт, 
    // первичный ключ должен быть составным (userId, recipeId).
    @PrimaryKey
    @NonNull
    private int recipeId;    // Идентификатор лайкнутого рецепта (связь с RecipeEntity.id)
    
    @NonNull
    private String userId;   // Идентификатор пользователя, который лайкнул рецепт

    /**
     * Создает экземпляр LikedRecipeEntity.
     * @param recipeId Идентификатор рецепта.
     * @param userId Идентификатор пользователя.
     */
    public LikedRecipeEntity(int recipeId, @NonNull String userId) {
        this.recipeId = recipeId;
        this.userId = userId;
    }

    public int getRecipeId() {
        return recipeId;
    }

    public void setRecipeId(int recipeId) {
        this.recipeId = recipeId;
    }

    @NonNull
    public String getUserId() {
        return userId;
    }

    public void setUserId(@NonNull String userId) {
        this.userId = userId;
    }

    /**
     * Преобразует {@link LikedRecipeEntity} в частичный объект {@link Recipe}.
     * Заполняет ID, UserID и устанавливает isLiked в true.
     * @return Частично заполненный объект {@link Recipe}.
     */
    public Recipe toRecipe() {
        Recipe recipe = new Recipe();
        recipe.setId(this.recipeId);
        recipe.setUserId(this.userId); // Устанавливаем userId из этой сущности
        recipe.setLiked(true);         // Так как это сущность лайкнутого рецепта
        // Другие поля Recipe (title, ingredients, etc.) здесь не заполняются
        return recipe;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        LikedRecipeEntity that = (LikedRecipeEntity) o;
        return recipeId == that.recipeId &&
               Objects.equals(userId, that.userId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(recipeId, userId);
    }

    @NonNull
    @Override
    public String toString() {
        return "LikedRecipeEntity{" +
                "recipeId=" + recipeId +
                ", userId='" + userId + '\'' +
                '}';
    }
}
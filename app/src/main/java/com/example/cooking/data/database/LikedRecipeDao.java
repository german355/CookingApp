package com.example.cooking.data.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

/**
 * Data Access Object (DAO) для работы с сущностями {@link LikedRecipeEntity}.
 * Предоставляет методы для добавления, удаления и получения информации о лайкнутых рецептах.
 */
@Dao
public interface LikedRecipeDao {

    /**
     * Вставляет запись о лайкнутом рецепте. 
     * Если запись с таким PrimaryKey уже существует, она будет заменена.

     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(LikedRecipeEntity likedRecipe);

    /**
     * Удаляет запись о лайкнутом рецепте.
     */
    @Delete
    void delete(LikedRecipeEntity likedRecipe);

    /**
     * Удаляет запись о лайке по идентификатору рецепта.
     * @param recipeId Идентификатор рецепта.
     */
    @Query("DELETE FROM liked_recipes WHERE recipeId = :recipeId")
    void deleteById(int recipeId);

    /**
     * Удаляет все записи о лайках из таблицы.
     */
    @Query("DELETE FROM liked_recipes")
    void deleteAll();

    /**
     * Получает список всех лайкнутых рецептов.

     */
    @Query("SELECT * FROM liked_recipes")
    LiveData<List<LikedRecipeEntity>> getLikedRecipes();

    /**
     * Проверяет, лайкнут ли указанный рецепт.

     */
    @Query("SELECT EXISTS(SELECT 1 FROM liked_recipes WHERE recipeId = :recipeId LIMIT 1)")
    boolean isRecipeLiked(int recipeId);

    /**
     * Получает синхронно список ID всех лайкнутых рецептов.
     * @return Список ID лайкнутых рецептов.
     */
    @Query("SELECT recipeId FROM liked_recipes")
    List<Integer> getLikedRecipeIdsSync();

    /**
     * Вставляет список записей о лайкнутых рецептах.

     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<LikedRecipeEntity> likedRecipes);
}
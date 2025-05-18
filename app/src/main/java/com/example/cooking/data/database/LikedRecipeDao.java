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
 * ВНИМАНИЕ: Корректность работы некоторых методов (например, использующих userId в WHERE-условии
 * для идентификации уникальной записи) зависит от структуры первичного ключа в {@link LikedRecipeEntity}.
 * Рекомендуется использовать составной первичный ключ (userId, recipeId) в {@link LikedRecipeEntity}.
 */
@Dao
public interface LikedRecipeDao {

    /**
     * Вставляет запись о лайкнутом рецепте. 
     * Если запись с таким PrimaryKey уже существует, она будет заменена.
     * @param likedRecipe Объект {@link LikedRecipeEntity} для вставки.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(LikedRecipeEntity likedRecipe);

    /**
     * Удаляет запись о лайкнутом рецепте.
     * @param likedRecipe Объект {@link LikedRecipeEntity} для удаления.
     */
    @Delete
    void delete(LikedRecipeEntity likedRecipe);

    /**
     * Удаляет запись о лайке по идентификатору рецепта и идентификатору пользователя.
     * @param recipeId Идентификатор рецепта.
     * @param userId Идентификатор пользователя.
     */
    @Query("DELETE FROM liked_recipes WHERE recipeId = :recipeId AND userId = :userId")
    void deleteById(int recipeId, String userId);

    /**
     * Удаляет все записи о лайках для указанного пользователя.
     * @param userId Идентификатор пользователя.
     */
    @Query("DELETE FROM liked_recipes WHERE userId = :userId")
    void deleteAllForUser(String userId);

    /**
     * Получает список всех лайкнутых рецептов для указанного пользователя.
     * Возвращает результат как {@link LiveData}.
     * @param userId Идентификатор пользователя.
     * @return {@link LiveData} со списком {@link LikedRecipeEntity}.
     */
    @Query("SELECT * FROM liked_recipes WHERE userId = :userId")
    LiveData<List<LikedRecipeEntity>> getLikedRecipesForUser(String userId);

    /**
     * Проверяет, лайкнул ли указанный пользователь указанный рецепт.
     * @param recipeId Идентификатор рецепта.
     * @param userId Идентификатор пользователя.
     * @return true, если рецепт лайкнут пользователем, иначе false.
     */
    @Query("SELECT EXISTS(SELECT 1 FROM liked_recipes WHERE recipeId = :recipeId AND userId = :userId LIMIT 1)")
    boolean isRecipeLiked(int recipeId, String userId);

    /**
     * Получает синхронно список ID всех рецептов, лайкнутых указанным пользователем.
     * @param userId Идентификатор пользователя.
     * @return Список ID лайкнутых рецептов.
     */
    @Query("SELECT recipeId FROM liked_recipes WHERE userId = :userId")
    List<Integer> getLikedRecipeIdsSync(String userId);

    /**
     * Вставляет список записей о лайкнутых рецептах.
     * Если записи с такими PrimaryKey уже существуют, они будут заменены.
     * @param likedRecipes Список объектов {@link LikedRecipeEntity} для вставки.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<LikedRecipeEntity> likedRecipes);
}
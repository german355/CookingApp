package com.example.cooking.data.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;
import androidx.room.Transaction;

import java.util.List;

/**
 * Data Access Object (DAO) для работы с сущностями {@link RecipeEntity} в базе данных.
 * Предоставляет методы для выполнения CRUD-операций и других запросов к таблице рецептов.
 */
@Dao
public interface RecipeDao {
    
    /**
     * Получает все рецепты из базы данных, отсортированные по названию.
     */
    @Query("SELECT * FROM recipes ORDER BY title ASC")
    LiveData<List<RecipeEntity>> getAllRecipes();
    
    /**
     * Получает все рецепты из базы данных, отсортированные по названию.
     */
    @Query("SELECT * FROM recipes ORDER BY title ASC")
    List<RecipeEntity> getAllRecipesList();
    
    /**
     * Получает рецепт по его уникальному идентификатору (ID).
     */
    @Query("SELECT * FROM recipes WHERE id = :recipeId")
    RecipeEntity getRecipeById(int recipeId);
    
    /**
     * Получает рецепт по его уникальному идентификатору (ID) синхронно
     */
    @Query("SELECT * FROM recipes WHERE id = :recipeId")
    RecipeEntity getRecipeByIdSync(int recipeId);
    
    /**
     * Вставляет список рецептов в базу данных.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<RecipeEntity> recipes);
    
    /**
     * Вставляет один рецепт в базу данных.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(RecipeEntity recipe);
    
    /**
     * Обновляет существующий рецепт в базе данных.
     */
    @Update
    void update(RecipeEntity recipe);
    
    /**
     * Удаляет все рецепты из таблицы recipes.
     * Будьте осторожны при использовании этого метода.
     */
    @Query("DELETE FROM recipes")
    void deleteAll();
    
    /**
     * Удаляет рецепт по его уникальному идентификатору (ID).
     *
     */
    @Query("DELETE FROM recipes WHERE id = :recipeId")
    void deleteById(int recipeId);
    
    /**
     * Получает все рецепты, отмеченные как "лайкнутые" (isLiked = true).
     */
    @Query("SELECT * FROM recipes WHERE isLiked = 1 ")
    LiveData<List<RecipeEntity>> getLikedRecipes();
    
    /**
     * Выполняет поиск рецептов, у которых название содержит указанную строку запроса (без учета регистра).
     */
    @Query("SELECT * FROM recipes WHERE title LIKE '%' || :query || '%' ")
    List<RecipeEntity> searchRecipesByTitle(String query);
    
    /**
     * Обновляет статус "лайк" для указанного рецепта.
     */
    @Query("UPDATE recipes SET isLiked = :isLiked WHERE id = :recipeId")
    void updateLikeStatus(int recipeId, boolean isLiked);
    
    /**
     * Удаляет указанный рецепт из базы данных.
     */
    @Delete
    void delete(RecipeEntity recipe);

    /**
     * Получает рецепт по его ID как LiveData
     */
    @Query("SELECT * FROM recipes WHERE id = :recipeId")
    LiveData<RecipeEntity> getRecipeEntityByIdLiveData(int recipeId);

    /**
     * Получает список рецептов, отфильтрованных по типу приема пищи (mealType), отсортированных по названию.*/
    @Query("SELECT * FROM recipes WHERE mealType = :mealType ")
    List<RecipeEntity> getRecipesByMealType(String mealType);

    /**
     * Получает список рецептов, отфильтрованных по типу блюда (foodType), отсортированных по названию.
     */
    @Query("SELECT * FROM recipes WHERE foodType = :foodType ")
    List<RecipeEntity> getRecipesByFoodType(String foodType);

    /**
     * Получает все рецепты из базы данных.
     */
    @Query("SELECT * FROM recipes ORDER BY title ASC")
    List<RecipeEntity> getAllRecipesSync();

    /**
     * Сбрасывает статус лайка для всех рецептов.
     */
    @Query("UPDATE recipes SET isLiked = 0")
    void clearAllLikeStatus();

    /**
     * Транзакционный метод для атомарного удаления и вставки всех рецептов
     */
    @Transaction
    default void replaceAllRecipes(List<RecipeEntity> recipes) {
        deleteAll();
        insertAll(recipes);
    }

    /**
     * Получает все ID рецептов из базы данных.
     * Используется для определения какие рецепты нужно добавить, обновить или удалить.
     * @return список всех ID рецептов
     */
    @Query("SELECT id FROM recipes")
    List<Integer> getAllRecipeIds();

    /**
     * Вставляет новые рецепты в базу данных, игнорируя конфликты.
     * Используется для добавления рецептов, которых ещё нет в базе.
     * @param recipes список новых рецептов для вставки
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    void insertNewRecipes(List<RecipeEntity> recipes);

    /**
     * Обновляет существующие рецепты в базе данных.
     * Используется для обновления рецептов, которые уже есть в базе.
     * @param recipes список рецептов для обновления
     */
    @Update
    void updateExistingRecipes(List<RecipeEntity> recipes);

    /**
     * Удаляет рецепты по списку ID.
     * Используется для удаления рецептов, которых больше нет на сервере.
     * @param idsToDelete список ID рецептов для удаления
     */
    @Query("DELETE FROM recipes WHERE id IN (:idsToDelete)")
    void deleteRecipesByIds(List<Integer> idsToDelete);
} 
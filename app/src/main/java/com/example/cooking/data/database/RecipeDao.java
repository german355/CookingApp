package com.example.cooking.data.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;
import androidx.room.Delete;

import com.example.cooking.Recipe.Recipe;

import java.util.List;

/**
 * Data Access Object (DAO) для работы с сущностями {@link RecipeEntity} в базе данных.
 * Предоставляет методы для выполнения CRUD-операций и других запросов к таблице рецептов.
 */
@Dao
public interface RecipeDao {
    
    /**
     * Получает все рецепты из базы данных, отсортированные по названию.
     * Возвращает результат как {@link LiveData}, что позволяет UI автоматически обновляться при изменениях данных.
     * @return {@link LiveData} со списком всех {@link RecipeEntity}.
     */
    @Query("SELECT * FROM recipes ")
    LiveData<List<RecipeEntity>> getAllRecipes();
    
    /**
     * Получает все рецепты из базы данных, отсортированные по названию.
     * Синхронный вызов, возвращающий простой список.
     * @return Список всех {@link RecipeEntity}.
     */
    @Query("SELECT * FROM recipes ")
    List<RecipeEntity> getAllRecipesList();
    
    /**
     * Получает рецепт по его уникальному идентификатору (ID).
     * @param recipeId Уникальный идентификатор рецепта.
     * @return {@link RecipeEntity} или null, если рецепт с таким ID не найден.
     */
    @Query("SELECT * FROM recipes WHERE id = :recipeId")
    RecipeEntity getRecipeById(int recipeId);
    
    /**
     * Получает рецепт по его уникальному идентификатору (ID) синхронно.
     * @param recipeId Уникальный идентификатор рецепта.
     * @return {@link RecipeEntity} или null, если рецепт с таким ID не найден.
     */
    @Query("SELECT * FROM recipes WHERE id = :recipeId")
    RecipeEntity getRecipeByIdSync(int recipeId);
    
    /**
     * Вставляет список рецептов в базу данных.
     * Если рецепт с таким ID уже существует, он будет заменен ({@link OnConflictStrategy#REPLACE}).
     * @param recipes Список объектов {@link RecipeEntity} для вставки.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<RecipeEntity> recipes);
    
    /**
     * Вставляет один рецепт в базу данных.
     * Если рецепт с таким ID уже существует, он будет заменен ({@link OnConflictStrategy#REPLACE}).
     * @param recipe Объект {@link RecipeEntity} для вставки.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(RecipeEntity recipe);
    
    /**
     * Обновляет существующий рецепт в базе данных.
     * Обновление происходит на основе PrimaryKey (ID) сущности.
     * @param recipe Объект {@link RecipeEntity} для обновления.
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
     * @param recipeId Уникальный идентификатор рецепта для удаления.
     */
    @Query("DELETE FROM recipes WHERE id = :recipeId")
    void deleteById(int recipeId);
    
    /**
     * Получает все рецепты, отмеченные как "лайкнутые" (isLiked = true).
     * Возвращает результат как {@link LiveData}.
     * @return {@link LiveData} со списком лайкнутых {@link RecipeEntity}.
     */
    @Query("SELECT * FROM recipes WHERE isLiked = 1 ")
    LiveData<List<RecipeEntity>> getLikedRecipes();
    
    /**
     * Выполняет поиск рецептов, у которых название содержит указанную строку запроса (без учета регистра).
     * @param query Строка для поиска в названиях рецептов.
     * @return Список {@link RecipeEntity}, удовлетворяющих условию поиска.
     */
    @Query("SELECT * FROM recipes WHERE title LIKE '%' || :query || '%' ")
    List<RecipeEntity> searchRecipesByTitle(String query);
    
    /**
     * Обновляет статус "лайк" для указанного рецепта.
     * @param recipeId Уникальный идентификатор рецепта.
     * @param isLiked Новое состояние лайка (true - лайкнут, false - дизлайкнут).
     */
    @Query("UPDATE recipes SET isLiked = :isLiked WHERE id = :recipeId")
    void updateLikeStatus(int recipeId, boolean isLiked);
    
    /**
     * Удаляет указанный рецепт из базы данных.
     * Удаление происходит на основе PrimaryKey (ID) сущности.
     * @param recipe Объект {@link RecipeEntity} для удаления.
     */
    @Delete
    void delete(RecipeEntity recipe);

    /**
     * Получает рецепт по его ID как {@link LiveData}.
     * Позволяет наблюдать за изменениями конкретного рецепта.
     * @param recipeId Уникальный идентификатор рецепта.
     * @return {@link LiveData} с объектом {@link RecipeEntity} или null, если рецепт не найден.
     */
    @Query("SELECT * FROM recipes WHERE id = :recipeId")
    LiveData<RecipeEntity> getRecipeEntityByIdLiveData(int recipeId);

    /**
     * Получает список рецептов, отфильтрованных по типу приема пищи (mealType), отсортированных по названию.
     * @param mealType Тип приема пищи (например, "завтрак", "обед").
     * @return Список {@link RecipeEntity}, соответствующих указанному типу приема пищи.
     */
    @Query("SELECT * FROM recipes WHERE mealType = :mealType ")
    List<RecipeEntity> getRecipesByMealType(String mealType);

    /**
     * Получает список рецептов, отфильтрованных по типу блюда (foodType), отсортированных по названию.
     * @param foodType Тип блюда (например, "суп", "салат").
     * @return Список {@link RecipeEntity}, соответствующих указанному типу блюда.
     */
    @Query("SELECT * FROM recipes WHERE foodType = :foodType ")
    List<RecipeEntity> getRecipesByFoodType(String foodType);

    /**
     * Получает все рецепты из базы данных.
     * Синхронный вызов для непосредственного использования.
     * @return Список {@link RecipeEntity}.
     */
    @Query("SELECT * FROM recipes ")
    List<RecipeEntity> getAllRecipesSync();
} 
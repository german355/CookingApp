package com.example.cooking.data.repositories;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.cooking.Recipe.Recipe;
import com.example.cooking.data.database.AppDatabase;
import com.example.cooking.data.database.RecipeDao;
import com.example.cooking.data.database.RecipeEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Репозиторий для работы с локальной базой данных рецептов
 */
public class RecipeLocalRepository {
    
    private static final String TAG = "RecipeLocalRepository";
    private final RecipeDao recipeDao;
    private final ExecutorService executor;
    
    public RecipeLocalRepository(Context context) {
        AppDatabase database = AppDatabase.getInstance(context);
        recipeDao = database.recipeDao();
        executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Получить все рецепты из базы данных
     * @return LiveData список рецептов
     */
    public LiveData<List<Recipe>> getAllRecipes() {
        // Трансформация List<RecipeEntity> в List<Recipe>
        return Transformations.map(
            recipeDao.getAllRecipes(),
            entities -> {
                Log.d(TAG, "Transforming RecipeEntities to Recipes. Count: " + (entities != null ? entities.size() : 0));
                List<Recipe> recipes = new ArrayList<>();
                if (entities != null) {
                     for (RecipeEntity entity : entities) {
                         recipes.add(entity.toRecipe());
                     }
                }
                Log.d(TAG, "Transformation complete. Returning Recipes count: " + recipes.size());
                return recipes;
            }
        );
    }
    
    /**
     * Получить список всех рецептов синхронно
     * @return список рецептов
     */
    public List<Recipe> getAllRecipesSync() {
        try {
            List<RecipeEntity> entities = recipeDao.getAllRecipesSync();
            List<Recipe> recipes = new ArrayList<>();
            
            if (entities != null) {
                for (RecipeEntity entity : entities) {
                    recipes.add(entity.toRecipe());
                }
                Log.d(TAG, "Получено " + recipes.size() + " рецептов из локальной БД");
            }
            
            return recipes;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении рецептов из БД: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Вставить список рецептов в базу данных
     * @param recipes список рецептов
     */
    public void insertAll(List<Recipe> recipes) {
        List<RecipeEntity> entities = new ArrayList<>();
        for (Recipe recipe : recipes) {
            entities.add(new RecipeEntity(recipe));
        }
        executor.execute(() -> recipeDao.insertAll(entities));
    }
    
    /**
     * Вставить рецепт в базу данных
     * @param recipe рецепт для вставки
     */
    public void insert(Recipe recipe) {
        executor.execute(() -> recipeDao.insert(new RecipeEntity(recipe)));
    }
    
    /**
     * Обновить рецепт в базе данных
     * @param recipe рецепт для обновления
     */
    public void update(Recipe recipe) {
        executor.execute(() -> recipeDao.update(new RecipeEntity(recipe)));
    }
    
    /**
     * Обновить состояние лайка рецепта
     * @param recipeId идентификатор рецепта
     * @param isLiked новое состояние лайка
     */
    public void updateLikeStatus(int recipeId, boolean isLiked) {
        try {
            recipeDao.updateLikeStatus(recipeId, isLiked);
            Log.d(TAG, "Статус лайка обновлен в локальной базе: recipeId=" + recipeId + ", isLiked=" + isLiked);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка обновления статуса лайка: " + e.getMessage(), e);
        }
    }
    
    /**
     * Получить лайкнутые рецепты
     * @return LiveData список лайкнутых рецептов
     */
    public LiveData<List<Recipe>> getLikedRecipes() {
        return Transformations.map(
            recipeDao.getLikedRecipes(),
            entities -> {
                List<Recipe> recipes = new ArrayList<>();
                for (RecipeEntity entity : entities) {
                    recipes.add(entity.toRecipe());
                }
                return recipes;
            }
        );
    }
    
    /**
     * Получить рецепт по идентификатору
     * @param recipeId идентификатор рецепта
     * @return LiveData рецепта
     */
    public LiveData<Recipe> getRecipeById(int recipeId) {
        return Transformations.map(recipeDao.getRecipeEntityByIdLiveData(recipeId), entity -> {
            if (entity != null) {
                return entity.toRecipe();
            }
            return null;
        });
    }
    
    /**
     * Синхронно получить рецепт по идентификатору
     * @param recipeId идентификатор рецепта
     * @return рецепт или null, если не найден
     */
    public Recipe getRecipeByIdSync(int recipeId) {
        RecipeEntity entity = recipeDao.getRecipeByIdSync(recipeId);
        if (entity != null) {
            return entity.toRecipe();
        }
        return null;
    }
    
    /**
     * Очистить все рецепты из базы данных
     */
    public void clearAll() {
        executor.execute(recipeDao::deleteAll);
    }
    
    /**
     * Очистить все рецепты из базы данных синхронно (для использования в транзакциях)
     */
    public void clearAllSync() {
        try {
            recipeDao.deleteAll();
            Log.d(TAG, "База данных рецептов очищена синхронно");
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при очистке базы данных синхронно", e);
        }
    }
    
    /**
     * Удалить рецепт из базы данных по идентификатору
     * @param recipeId идентификатор рецепта для удаления
     */
    public void deleteRecipe(int recipeId) {
        try {
            executor.execute(() -> {
                try {
                    RecipeEntity recipe = recipeDao.getRecipeById(recipeId);
                    if (recipe != null) {
                        recipeDao.delete(recipe);
                        Log.d(TAG, "Рецепт успешно удален из базы данных: " + recipeId);
                    } else {
                        Log.w(TAG, "Попытка удалить несуществующий рецепт: " + recipeId);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при удалении рецепта: " + recipeId, e);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при запуске задачи удаления рецепта: " + recipeId, e);
        }
    }
}
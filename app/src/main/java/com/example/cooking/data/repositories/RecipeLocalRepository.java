package com.example.cooking.data.repositories;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.cooking.Recipe.Recipe;
import com.example.cooking.data.database.AppDatabase;
import com.example.cooking.data.database.RecipeDao;
import com.example.cooking.data.database.RecipeEntity;
import com.example.cooking.utils.AppExecutors;

import java.util.ArrayList;
import java.util.List;

/**
 * Репозиторий для работы с локальной базой данных рецептов
 */
public class RecipeLocalRepository extends NetworkRepository{
    
    private static final String TAG = "RecipeLocalRepository";
    private final RecipeDao recipeDao;

    
    public RecipeLocalRepository(Context context) {
        super(context);
        AppDatabase database = AppDatabase.getInstance(context);
        recipeDao = database.recipeDao();
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
                List<Recipe> recipes = new ArrayList<>();
                if (entities != null) {
                     for (RecipeEntity entity : entities) {
                         recipes.add(entity.toRecipe());
                     }
                }
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
     * Вставить рецепт в базу данных
     * @param recipe рецепт для вставки
     */
    public void insert(Recipe recipe) {
        AppExecutors.getInstance().diskIO().execute(() -> recipeDao.insert(new RecipeEntity(recipe)));
    }
    
    /**
     * Обновить рецепт в базе данных
     * @param recipe рецепт для обновления
     */
    public void update(Recipe recipe) {
        AppExecutors.getInstance().diskIO().execute(() -> recipeDao.update(new RecipeEntity(recipe)));
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
     * Удалить рецепт из базы данных по идентификатору
     * @param recipeId идентификатор рецепта для удаления
     */
    public void deleteRecipe(int recipeId) {
        try {
            AppExecutors.getInstance().diskIO().execute(() -> {
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
    
    /**
     * Заменить все рецепты в базе данных, используя транзакционный метод DAO.
     * @param recipes список новых рецептов
     */
    public void replaceAllRecipes(List<Recipe> recipes) {
        List<RecipeEntity> entities = new ArrayList<>();
        if (recipes != null) {
            for (Recipe recipe : recipes) {
                entities.add(new RecipeEntity(recipe));
            }
        }
        // NEW: выполняем замену внутри runInTransaction, чтобы гарантировать атомарность и успешный коммит
        try {
            AppDatabase.getInstance(context).runInTransaction(() -> {
                recipeDao.replaceAllRecipes(entities);
            });
            Log.d(TAG, "Все рецепты заменены, count=" + entities.size());
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при replaceAllRecipes", e);
        }
    }
}
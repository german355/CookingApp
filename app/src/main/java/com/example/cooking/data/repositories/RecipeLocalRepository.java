package com.example.cooking.data.repositories;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;

import com.example.cooking.domain.entities.Recipe;
import com.example.cooking.data.database.AppDatabase;
import com.example.cooking.data.database.RecipeDao;
import com.example.cooking.data.database.RecipeEntity;
import com.example.cooking.data.database.converters.DataConverters;
import com.example.cooking.utils.AppExecutors;
import com.example.cooking.utils.PerformanceMonitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;

/**
 * Репозиторий для работы с локальной базой данных рецептов.
 * Поддерживает ленивую сериализацию для оптимизации производительности.
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
                return PerformanceMonitor.measureOperation("transform_entities_to_recipes", () -> {
                    List<Recipe> recipes = new ArrayList<>();
                    if (entities != null) {
                        for (RecipeEntity entity : entities) {
                            recipes.add(entity.toRecipe());
                        }
                        // Предварительная загрузка кэша для часто используемых данных
                        preloadCacheForEntities(entities);
                    }
                    return recipes;
                });
            }
        );
    }
    
    /**
     * Получить список всех рецептов синхронно
     * @return список рецептов
     */
    public List<Recipe> getAllRecipesSync() {
        return PerformanceMonitor.measureOperation("get_all_recipes_sync", () -> {
            try {
                List<RecipeEntity> entities = recipeDao.getAllRecipesSync();
                List<Recipe> recipes = new ArrayList<>();
                
                if (entities != null) {
                    for (RecipeEntity entity : entities) {
                        recipes.add(entity.toRecipe());
                    }
                    Log.d(TAG, "Получено " + recipes.size() + " рецептов из локальной БД");
                    
                    // Предварительная загрузка кэша для оптимизации последующих операций
                    preloadCacheForEntities(entities);
                }
                
                return recipes;
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при получении рецептов из БД: " + e.getMessage());
                return new ArrayList<>();
            }
        });
    }

    /**
     * Предварительно загружает кэш сериализации для списка сущностей.
     * Это ускоряет последующие обращения к JSON данным.
     */
    private void preloadCacheForEntities(List<RecipeEntity> entities) {
        if (entities == null || entities.isEmpty()) return;
        
        AppExecutors.getInstance().diskIO().execute(() -> {
            PerformanceMonitor.Timer timer = PerformanceMonitor.Timer.start("preload_serialization_cache");
            
            try {
                List<String> ingredientJsons = new ArrayList<>();
                List<String> stepJsons = new ArrayList<>();
                
                // Собираем JSON данные из кэшированных сущностей
                for (RecipeEntity entity : entities) {
                    String ingredientsJson = entity.getIngredientsJson();
                    String instructionsJson = entity.getInstructionsJson();
                    
                    if (ingredientsJson != null) {
                        ingredientJsons.add(ingredientsJson);
                    }
                    if (instructionsJson != null) {
                        stepJsons.add(instructionsJson);
                    }
                }
                
                // Предварительно загружаем в кэш DataConverters
                DataConverters.preloadCache(ingredientJsons, stepJsons);
                
                Log.d(TAG, String.format("Кэш предварительно загружен для %d рецептов. %s", 
                                        entities.size(), DataConverters.getCacheStats()));
            } finally {
                timer.stop();
            }
        });
    }
    
    /**
     * Вставить рецепт в базу данных
     * @param recipe рецепт для вставки
     */
    public void insert(Recipe recipe) {
        PerformanceMonitor.measureDatabaseOperation("insert_recipe", () -> {
            AppExecutors.getInstance().diskIO().execute(() -> {
                RecipeEntity entity = new RecipeEntity(recipe);
                recipeDao.insert(entity);
            });
        });
    }
    
    /**
     * Обновить рецепт в базе данных
     * @param recipe рецепт для обновления
     */
    public void update(Recipe recipe) {
        PerformanceMonitor.measureDatabaseOperation("update_recipe", () -> {
            AppExecutors.getInstance().diskIO().execute(() -> {
                RecipeEntity entity = new RecipeEntity(recipe);
                recipeDao.update(entity);
            });
        });
    }
    
    /**
     * Обновить состояние лайка рецепта
     * @param recipeId идентификатор рецепта
     * @param isLiked новое состояние лайка
     */
    public void updateLikeStatus(int recipeId, boolean isLiked) {
        PerformanceMonitor.measureDatabaseOperation("update_like_status", () -> {
            try {
                recipeDao.updateLikeStatus(recipeId, isLiked);
                Log.d(TAG, "Статус лайка обновлен в локальной базе: recipeId=" + recipeId + ", isLiked=" + isLiked);
            } catch (Exception e) {
                Log.e(TAG, "Ошибка обновления статуса лайка: " + e.getMessage(), e);
            }
        });
    }

    
    /**
     * Синхронно получить рецепт по идентификатору
     * @param recipeId идентификатор рецепта
     * @return рецепт или null, если не найден
     */
    public Recipe getRecipeByIdSync(int recipeId) {
        return PerformanceMonitor.measureOperation("get_recipe_by_id_sync", () -> {
            RecipeEntity entity = recipeDao.getRecipeByIdSync(recipeId);
            if (entity != null) {
                // Предварительно загружаем кэш для этой сущности
                entity.refreshCache();
                return entity.toRecipe();
            }
            return null;
        });
    }
    

    
    /**
     * Удалить рецепт из базы данных по идентификатору
     * @param recipeId идентификатор рецепта для удаления
     */
    public void deleteRecipe(int recipeId) {
        PerformanceMonitor.measureDatabaseOperation("delete_recipe", () -> {
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
        });
    }
    
    /**
     * Умная замена рецептов с дифференциальными обновлениями.
     * Анализирует существующие данные и выполняет только необходимые операции:
     * - вставляет новые рецепты
     * - обновляет измененные рецепты  
     * - удаляет отсутствующие рецепты
     * @param newRecipes список новых рецептов с сервера
     */
    public void smartReplaceRecipes(List<Recipe> newRecipes) {
        PerformanceMonitor.measureDatabaseOperation("smart_replace_recipes", () -> {
            AppExecutors.getInstance().diskIO().execute(() -> {
                try {
                    PerformanceMonitor.Timer analysisTimer = PerformanceMonitor.Timer.start("analyze_recipe_differences");
                    
                    // Получаем существующие ID из БД
                    List<Integer> existingIds = recipeDao.getAllRecipeIds();
                    
                    // Подготавливаем коллекции для разных типов операций
                    List<RecipeEntity> toInsert = new ArrayList<>();
                    List<RecipeEntity> toUpdate = new ArrayList<>();
                    Set<Integer> newIds = new HashSet<>();
                    
                    // Анализируем новые рецепты и определяем операции
                    for (Recipe recipe : newRecipes) {
                        newIds.add(recipe.getId());
                        RecipeEntity entity = new RecipeEntity(recipe);
                        entity.refreshCache(); // Предварительно загружаем кэш
                        
                        if (existingIds.contains(recipe.getId())) {
                            toUpdate.add(entity);
                        } else {
                            toInsert.add(entity);
                        }
                    }
                    
                    // Находим рецепты для удаления (есть в БД, но нет в новых данных)
                    List<Integer> toDelete = existingIds.stream()
                        .filter(id -> !newIds.contains(id))
                        .collect(Collectors.toList());
                    
                    analysisTimer.stop();
                    
                    // Выполняем операции в транзакции для атомарности
                    PerformanceMonitor.Timer transactionTimer = PerformanceMonitor.Timer.start("smart_replace_transaction");
                    
                    AppDatabase.getInstance(context).runInTransaction(() -> {
                        // Удаляем отсутствующие рецепты
                        if (!toDelete.isEmpty()) {
                            recipeDao.deleteRecipesByIds(toDelete);
                            Log.d(TAG, "Удалено рецептов: " + toDelete.size());
                        }
                        
                        // Вставляем новые рецепты
                        if (!toInsert.isEmpty()) {
                            recipeDao.insertNewRecipes(toInsert);
                            Log.d(TAG, "Вставлено новых рецептов: " + toInsert.size());
                        }
                        
                        // Обновляем существующие рецепты
                        if (!toUpdate.isEmpty()) {
                            recipeDao.updateExistingRecipes(toUpdate);
                            Log.d(TAG, "Обновлено рецептов: " + toUpdate.size());
                        }
                    });
                    
                    transactionTimer.stop();
                    
                    // Предварительно загружаем кэш для новых данных
                    List<RecipeEntity> allNewEntities = new ArrayList<>();
                    allNewEntities.addAll(toInsert);
                    allNewEntities.addAll(toUpdate);
                    preloadCacheForEntities(allNewEntities);
                    
                    Log.d(TAG, String.format(
                        "Умная замена завершена: всего=%d, новых=%d, обновлено=%d, удалено=%d", 
                        newRecipes.size(), toInsert.size(), toUpdate.size(), toDelete.size()));
                        
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при умной замене рецептов: " + e.getMessage(), e);
                    // Fallback на обычную замену в случае ошибки
                    replaceAllRecipes(newRecipes);
                }
            });
        });
    }

    /**
     * Заменить все рецепты в базе данных с оптимизациями производительности.
     * Использует ленивую сериализацию и мониторинг производительности.
     * @param recipes список новых рецептов
     */
    public void replaceAllRecipes(List<Recipe> recipes) {
        PerformanceMonitor.measureDatabaseOperation("replace_all_recipes", () -> {
            PerformanceMonitor.Timer conversionTimer = PerformanceMonitor.Timer.start("convert_recipes_to_entities");
            
            List<RecipeEntity> entities = new ArrayList<>();
            if (recipes != null) {
                // Конвертируем рецепты в сущности батчами для лучшей производительности
                final int BATCH_SIZE = 25; // Уменьшено для лучшей отзывчивости
                
                for (int i = 0; i < recipes.size(); i += BATCH_SIZE) {
                    int endIndex = Math.min(i + BATCH_SIZE, recipes.size());
                    List<Recipe> batch = recipes.subList(i, endIndex);
                    
                    for (Recipe recipe : batch) {
                        RecipeEntity entity = new RecipeEntity(recipe);
                        // Предварительно загружаем кэш для новых сущностей
                        entity.refreshCache();
                        entities.add(entity);
                    }
                    
                    // Проверка прерывания потока
                    if (Thread.currentThread().isInterrupted()) {
                        Log.w(TAG, "Операция прервана при конвертации рецептов");
                        return;
                    }
                    
                    // Микропауза для предотвращения блокировки UI
                    if (endIndex < recipes.size()) {
                        try {
                            Thread.sleep(5);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
            
            conversionTimer.stop();
            
            // Выполняем замену в БД внутри транзакции
            PerformanceMonitor.Timer dbTimer = PerformanceMonitor.Timer.start("database_replace_transaction");
            
            try {
                AppDatabase.getInstance(context).runInTransaction(() -> {
                    recipeDao.replaceAllRecipes(entities);
                });
                Log.d(TAG, "Все рецепты заменены с ленивой сериализацией, count=" + entities.size());
                
                // Очищаем старый кэш и загружаем новый
                DataConverters.clearCache();
                preloadCacheForEntities(entities);
                
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при replaceAllRecipes", e);
            } finally {
                dbTimer.stop();
            }
        });
    }
    
    /**
     * Возвращает статистику производительности репозитория.
     * @return строка с отчетом о производительности
     */
    public String getPerformanceStats() {
        StringBuilder stats = new StringBuilder();
        stats.append("=== Статистика RecipeLocalRepository ===\n");
        stats.append(PerformanceMonitor.getPerformanceReport());
        stats.append("\n=== Статистика кэша DataConverters ===\n");
        stats.append(DataConverters.getCacheStats());
        return stats.toString();
    }
    
    /**
     * Очищает весь кэш для освобождения памяти.
     */
    public void clearAllCaches() {
        DataConverters.clearCache();
        PerformanceMonitor.clearStats();
        Log.d(TAG, "Все кэши очищены");
    }
}
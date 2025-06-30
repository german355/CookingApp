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
    
    // Кэш для результатов фильтрации по категориям
    private final java.util.concurrent.ConcurrentHashMap<String, List<Recipe>> categoryFilterCache = new java.util.concurrent.ConcurrentHashMap<>();
    private volatile long lastCacheUpdate = 0;
    private static final long CACHE_VALIDITY_MS = 30000; // 30 секунд

    
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
                    // Предварительная загрузка кэша для часто используемых данных
                    preloadCacheForEntities(entities);
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
                
                // Предварительная загрузка кэша для оптимизации последующих операций
                preloadCacheForEntities(entities);
            }
            
            return recipes;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении рецептов из БД: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Предварительно загружает кэш сериализации для списка сущностей.
     * Это ускоряет последующие обращения к JSON данным.
     */
    private void preloadCacheForEntities(List<RecipeEntity> entities) {
        if (entities == null || entities.isEmpty()) return;
        
        AppExecutors.getInstance().diskIO().execute(() -> {
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
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при предварительной загрузке кэша: " + e.getMessage());
            }
        });
    }
    
    /**
     * Вставить рецепт в базу данных
     * @param recipe рецепт для вставки
     */
    public void insert(Recipe recipe) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            RecipeEntity entity = new RecipeEntity(recipe);
            recipeDao.insert(entity);
            invalidateCache(); // Инвалидируем кэш
        });
    }
    
    /**
     * Обновить рецепт в базе данных
     */
    public void update(Recipe recipe) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            RecipeEntity entity = new RecipeEntity(recipe);
            recipeDao.update(entity);
            invalidateCache(); // Инвалидируем кэш
        });
    }
    
    /**
     * Обновить состояние лайка рецепта
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
     * Получает рецепты отфильтрованные по категории с оптимизацией SQL и кэшированием
     * Использует прямые SQL запросы вместо фильтрации в памяти
     */
    public List<Recipe> getRecipesByCategory(String filterKey, String filterType) {
        String cacheKey = filterType + ":" + filterKey;
        
        // Проверяем кэш
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCacheUpdate < CACHE_VALIDITY_MS) {
            List<Recipe> cachedResult = categoryFilterCache.get(cacheKey);
            if (cachedResult != null) {
                Log.d(TAG, "Возвращен результат из кэша для " + cacheKey + " (" + cachedResult.size() + " рецептов)");
                return new ArrayList<>(cachedResult); // Возвращаем копию для безопасности
            }
        }
        
        try {
            List<RecipeEntity> entities;
            
            // Используем оптимизированные SQL запросы
            switch (filterType) {
                case "meal_type":
                    entities = recipeDao.getRecipesByMealType(filterKey);
                    break;
                case "food_type":
                    entities = recipeDao.getRecipesByFoodType(filterKey);
                    break;
                default:
                    Log.w(TAG, "Неизвестный тип фильтра: " + filterType);
                    return new ArrayList<>();
            }
            
            // Конвертируем в Recipe объекты
            List<Recipe> recipes = new ArrayList<>();
            if (entities != null) {
                for (RecipeEntity entity : entities) {
                    recipes.add(entity.toRecipe());
                }
                
                // Предварительно загружаем кэш для отфильтрованных рецептов
                preloadCacheForEntities(entities);
                
                Log.d(TAG, "SQL фильтрация: найдено " + recipes.size() + " рецептов для " + filterType + "=" + filterKey);
            }
            
            // Сохраняем в кэш
            categoryFilterCache.put(cacheKey, new ArrayList<>(recipes));
            
            return recipes;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при фильтрации рецептов: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * Синхронно получить рецепт по идентификатору
     */
    public Recipe getRecipeByIdSync(int recipeId) {
        RecipeEntity entity = recipeDao.getRecipeByIdSync(recipeId);
        if (entity != null) {
            // Предварительно загружаем кэш для этой сущности
            entity.refreshCache();
            return entity.toRecipe();
        }
        return null;
    }
    

    
    /**
     * Удалить рецепт из базы данных по идентификатору
     */
    public void deleteRecipe(int recipeId) {
        try {
            AppExecutors.getInstance().diskIO().execute(() -> {
                try {
                    RecipeEntity recipe = recipeDao.getRecipeById(recipeId);
                    if (recipe != null) {
                        recipeDao.delete(recipe);
                        invalidateCache(); // Инвалидируем кэш
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
     * Инвалидирует кэш фильтрации при изменении данных
     */
    private void invalidateCache() {
        categoryFilterCache.clear();
        lastCacheUpdate = System.currentTimeMillis();
        Log.d(TAG, "Кэш фильтрации инвалидирован");
    }
    
    /**
     * Умная замена рецептов с дифференциальными обновлениями.
     * Анализирует существующие данные и выполняет только необходимые операции:
     * - вставляет новые рецепты
     * - обновляет измененные рецепты  
     * - удаляет отсутствующие рецепты
     */
    public void smartReplaceRecipes(List<Recipe> newRecipes) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
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
                
                // Выполняем операции в транзакции для атомарности
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
                
                // Предварительно загружаем кэш для новых данных
                List<RecipeEntity> allNewEntities = new ArrayList<>();
                allNewEntities.addAll(toInsert);
                allNewEntities.addAll(toUpdate);
                preloadCacheForEntities(allNewEntities);
                
                Log.d(TAG, String.format(
                    "Умная замена завершена: всего=%d, новых=%d, обновлено=%d, удалено=%d", 
                    newRecipes.size(), toInsert.size(), toUpdate.size(), toDelete.size()));
                
                // Инвалидируем кэш после изменений
                invalidateCache();
                    
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при умной замене рецептов: " + e.getMessage(), e);
                // Fallback на обычную замену в случае ошибки
                replaceAllRecipes(newRecipes);
            }
        });
    }

    /**
     * Заменить все рецепты в базе данных с оптимизациями производительности.
     * Использует ленивую сериализацию и мониторинг производительности.
     */
    public void replaceAllRecipes(List<Recipe> recipes) {
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
        
        // Выполняем замену в БД внутри транзакции
        try {
            AppDatabase.getInstance(context).runInTransaction(() -> {
                recipeDao.replaceAllRecipes(entities);
            });
            Log.d(TAG, "Все рецепты заменены с ленивой сериализацией, count=" + entities.size());
            
            // Очищаем старый кэш и загружаем новый
            DataConverters.clearCache();
            preloadCacheForEntities(entities);
            
            // Инвалидируем кэш фильтрации
            invalidateCache();
            
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при replaceAllRecipes", e);
        }
    }
    
    /**
     * Очищает весь кэш для освобождения памяти.
     */
    public void clearAllCaches() {
        DataConverters.clearCache();
        invalidateCache();
        Log.d(TAG, "Все кэши очищены");
    }
}
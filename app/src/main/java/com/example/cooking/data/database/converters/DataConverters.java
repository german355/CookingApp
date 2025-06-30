package com.example.cooking.data.database.converters;

import android.util.Log;
import androidx.room.TypeConverter;
import com.example.cooking.domain.entities.Ingredient;
import com.example.cooking.domain.entities.Step;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Type Converters для Room.
 * Преобразует списки {@link Ingredient} и {@link Step} в JSON-строку для хранения в базе данных
 * и обратно при извлечении.
 * Поддерживает ленивую сериализацию для оптимизации производительности.
 */
public class DataConverters {
    private static final String TAG = "DataConverters";
    private static final Gson GSON_INSTANCE = new Gson();
    private static final Type INGREDIENT_LIST_TYPE = new TypeToken<ArrayList<Ingredient>>() {}.getType();
    private static final Type STEP_LIST_TYPE = new TypeToken<ArrayList<Step>>() {}.getType();
    
    // Кэш для часто используемых JSON строк (опционально, для экстремальной оптимизации)
    private static final ConcurrentHashMap<String, List<Ingredient>> INGREDIENT_CACHE = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, List<Step>> STEP_CACHE = new ConcurrentHashMap<>();
    private static final int MAX_CACHE_SIZE = 100; // Ограничиваем размер кэша

    // --- Ingredient List Converters ---

    /**
     * Конвертирует список объектов {@link Ingredient} в JSON-строку.
     * Оптимизирован для работы с кэшированием.
     */
    @TypeConverter
    public static String fromIngredientList(List<Ingredient> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            return null;
        }
        
        try {
            return GSON_INSTANCE.toJson(ingredients);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при сериализации списка ингредиентов", e);
            return null;
        }
    }

    /**
     * Конвертирует JSON-строку обратно в список объектов Ingredient.
     * Использует кэширование для часто запрашиваемых данных.
     */
    @TypeConverter
    public static List<Ingredient> toIngredientList(String ingredientsString) {
        if (ingredientsString == null || ingredientsString.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Проверяем кэш
        List<Ingredient> cached = INGREDIENT_CACHE.get(ingredientsString);
        if (cached != null) {
            Log.d(TAG, "Загружен список ингредиентов из кэша");
            return new ArrayList<>(cached); // Возвращаем копию
        }
        
        try {
            List<Ingredient> list = GSON_INSTANCE.fromJson(ingredientsString, INGREDIENT_LIST_TYPE);
            if (list == null) {
                return Collections.emptyList();
            }
            
            // Добавляем в кэш если есть место
            if (INGREDIENT_CACHE.size() < MAX_CACHE_SIZE) {
                INGREDIENT_CACHE.put(ingredientsString, new ArrayList<>(list));
            }
            
            return list;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при преобразовании JSON в список ингредиентов: " + ingredientsString, e);
            return Collections.emptyList();
        }
    }

    /**
     * Конвертирует список объектов Step в JSON-строку.
     * Оптимизирован для работы с кэшированием.
     */
    @TypeConverter
    public static String fromStepList(List<Step> steps) {
        if (steps == null || steps.isEmpty()) {
            return null;
        }
        
        try {
            return GSON_INSTANCE.toJson(steps);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при сериализации списка шагов", e);
            return null;
        }
    }

    /**
     * Конвертирует JSON-строку обратно в список объектов Step.
     * Использует кэширование и валидацию данных.
     */
    @TypeConverter
    public static List<Step> toStepList(String stepsString) {
        if (stepsString == null || stepsString.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Проверяем кэш
        List<Step> cached = STEP_CACHE.get(stepsString);
        if (cached != null) {
            Log.d(TAG, "Загружен список шагов из кэша");
            return new ArrayList<>(cached); // Возвращаем копию
        }
        
        try {
            List<Step> list = GSON_INSTANCE.fromJson(stepsString, STEP_LIST_TYPE);
            if (list == null) {
                return Collections.emptyList();
            }
            
            // Валидация и исправление номеров шагов
            for (int i = 0; i < list.size(); i++) {
                Step step = list.get(i);
                if (step.getNumber() <= 0) {
                    step.setNumber(i + 1);
                }
            }
            
            // Добавляем в кэш если есть место
            if (STEP_CACHE.size() < MAX_CACHE_SIZE) {
                STEP_CACHE.put(stepsString, new ArrayList<>(list));
            }
            
            return list;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при преобразовании JSON в список шагов: " + stepsString, e);
            return Collections.emptyList();
        }
    }

    // --- Вспомогательные методы для оптимизации ---

    /**
     * Очищает кэш для освобождения памяти.
     * Рекомендуется вызывать периодически или при нехватке памяти.
     */
    public static void clearCache() {
        INGREDIENT_CACHE.clear();
        STEP_CACHE.clear();
        Log.d(TAG, "Кэш DataConverters очищен");
    }

    /**
     * Возвращает статистику использования кэша.
     */
    public static String getCacheStats() {
        return String.format("Cache stats: Ingredients=%d, Steps=%d, Max=%d", 
                           INGREDIENT_CACHE.size(), STEP_CACHE.size(), MAX_CACHE_SIZE);
    }

    /**
     * Предварительно загружает данные в кэш для часто используемых объектов.
     * @param ingredientJsons список JSON строк ингредиентов для кэширования
     * @param stepJsons список JSON строк шагов для кэширования
     */
    public static void preloadCache(List<String> ingredientJsons, List<String> stepJsons) {
        if (ingredientJsons != null) {
            for (String json : ingredientJsons) {
                if (json != null && !json.isEmpty()) {
                    toIngredientList(json); // Загружает в кэш как побочный эффект
                }
            }
        }
        
        if (stepJsons != null) {
            for (String json : stepJsons) {
                if (json != null && !json.isEmpty()) {
                    toStepList(json); // Загружает в кэш как побочный эффект
                }
            }
        }
        
        Log.d(TAG, "Кэш предварительно загружен: " + getCacheStats());
    }

    /**
     * Быстрая проверка валидности JSON без полной десериализации.
     * @param json строка для проверки
     * @return true если JSON выглядит валидным
     */
    public static boolean isValidJson(String json) {
        if (json == null || json.trim().isEmpty()) {
            return false;
        }
        String trimmed = json.trim();
        return (trimmed.startsWith("[") && trimmed.endsWith("]")) || 
               (trimmed.startsWith("{") && trimmed.endsWith("}"));
    }
} 
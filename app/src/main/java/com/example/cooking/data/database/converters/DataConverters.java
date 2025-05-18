package com.example.cooking.data.database.converters;

import android.util.Log;
import androidx.room.TypeConverter;
import com.example.cooking.Recipe.Ingredient;
import com.example.cooking.Recipe.Step;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Type Converters для Room.
 * Преобразует списки {@link Ingredient} и {@link Step} в JSON-строку для хранения в базе данных
 * и обратно при извлечении.
 */
public class DataConverters {
    private static final Gson GSON_INSTANCE = new Gson();
    private static final Type INGREDIENT_LIST_TYPE = new TypeToken<ArrayList<Ingredient>>() {}.getType();
    private static final Type STEP_LIST_TYPE = new TypeToken<ArrayList<Step>>() {}.getType();

    // --- Ingredient List Converters ---

    /**
     * Конвертирует список объектов {@link Ingredient} в JSON-строку.
     * @param ingredients Список ингредиентов.
     * @return JSON-строка или null, если список пуст или null.
     */
    @TypeConverter
    public static String fromIngredientList(List<Ingredient> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) { // Добавил проверку на isEmpty для консистентности
            return null;
        }
        return GSON_INSTANCE.toJson(ingredients);
    }

    /**
     * Конвертирует JSON-строку обратно в список объектов {@link Ingredient}.
     * Включает попытки восстановления данных, если строка не является валидным JSON-массивом.
     * @param ingredientsString JSON-строка с ингредиентами.
     * @return Список ингредиентов или пустой список в случае ошибки или отсутствия данных.
     */
    @TypeConverter
    public static List<Ingredient> toIngredientList(String ingredientsString) {
        if (ingredientsString == null || ingredientsString.isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            // Основная попытка десериализации как списка
            List<Ingredient> list = GSON_INSTANCE.fromJson(ingredientsString, INGREDIENT_LIST_TYPE);
            if (list == null) {
                return Collections.emptyList();
            }
            Log.d("DataConverters", "Ingredients: успешно преобразовано " + list.size() + " элементов из JSON: " + ingredientsString);
            return list;
        } catch (JsonSyntaxException e) { // Ловим конкретно JsonSyntaxException для проблем с форматом
            Log.w("DataConverters", "Ingredients: не удалось преобразовать как JSON-массив: " + ingredientsString + ". Ошибка: " + e.getMessage());
            // Попытка альтернативной обработки, если JSON не является массивом
            try {
                // Попытка 1: это одиночный объект Ingredient?
                Ingredient singleIngredient = GSON_INSTANCE.fromJson(ingredientsString, Ingredient.class);
                if (singleIngredient != null && singleIngredient.getName() != null) { // Добавим проверку на осмысленность объекта
                     Log.d("DataConverters", "Ingredients: строка распознана как одиночный объект Ingredient.");
                    return Collections.singletonList(singleIngredient);
                }
            } catch (JsonSyntaxException ex) {
                // Попытка 1 не удалась, игнорируем и продолжаем
                 Log.w("DataConverters", "Ingredients: строка также не является одиночным объектом Ingredient. " + ex.getMessage());
            }
             // Попытка 2 (менее вероятная, из старого кода): это просто число?
            try {
                Integer.parseInt(ingredientsString); 
                Log.w("DataConverters", "Ingredients: строка является числом ('"+ ingredientsString +"'), не список. Возвращен пустой список.");
                return Collections.emptyList(); // Если это просто число, это не список ингредиентов
            } catch (NumberFormatException nfe) {
                // Это не число, значит, формат действительно неизвестен
            }
            Log.e("DataConverters", "Ingredients: все попытки преобразования JSON провалились для строки: " + ingredientsString + ". Возвращен пустой список.");
            return Collections.emptyList(); // Возвращаем пустой список, если все попытки не увенчались успехом
        } catch (Exception e) {
            // Обработка других неожиданных ошибок
            Log.e("DataConverters", "Ingredients: непредвиденная ошибка при преобразовании JSON: " + ingredientsString, e);
            return Collections.emptyList();
        }
    }

    // --- Step List Converters ---

    /**
     * Конвертирует список объектов {@link Step} в JSON-строку.
     * @param steps Список шагов.
     * @return JSON-строка или null, если список пуст или null.
     */
    @TypeConverter
    public static String fromStepList(List<Step> steps) {
        if (steps == null || steps.isEmpty()) { // Добавил проверку на isEmpty
            return null;
        }
        return GSON_INSTANCE.toJson(steps);
    }

    /**
     * Конвертирует JSON-строку обратно в список объектов {@link Step}.
     * Включает попытки восстановления данных и коррекцию номеров шагов.
     * @param stepsString JSON-строка с шагами.
     * @return Список шагов или пустой список в случае ошибки или отсутствия данных.
     */
    @TypeConverter
    public static List<Step> toStepList(String stepsString) {
        if (stepsString == null || stepsString.isEmpty()) {
            return Collections.emptyList();
        }
        
        try {
            // Основная попытка десериализации как списка
            List<Step> list = GSON_INSTANCE.fromJson(stepsString, STEP_LIST_TYPE);
            if (list == null) {
                return Collections.emptyList();
            }
            
            // Корректируем номера шагов, если они отсутствуют или некорректны
            for (int i = 0; i < list.size(); i++) {
                Step step = list.get(i);
                if (step.getNumber() <= 0) {
                    step.setNumber(i + 1); // Нумерация с 1
                }
            }
            Log.d("DataConverters", "Steps: успешно преобразовано " + list.size() + " шагов из JSON: " + stepsString);
            return list;
        } catch (JsonSyntaxException e) { // Ловим конкретно JsonSyntaxException
            Log.w("DataConverters", "Steps: не удалось преобразовать как JSON-массив: " + stepsString + ". Ошибка: " + e.getMessage());
            // Попытка альтернативной обработки
            try {
                Step singleStep = GSON_INSTANCE.fromJson(stepsString, Step.class);
                if (singleStep != null && singleStep.getInstruction() != null) { // Проверка на осмысленность
                    if (singleStep.getNumber() <= 0) singleStep.setNumber(1); // Коррекция номера для одиночного шага
                    Log.d("DataConverters", "Steps: строка распознана как одиночный объект Step.");
                    return Collections.singletonList(singleStep);
                }
            } catch (JsonSyntaxException ex) {
                 Log.w("DataConverters", "Steps: строка также не является одиночным объектом Step. " + ex.getMessage());
            }
            try {
                Integer.parseInt(stepsString);
                Log.w("DataConverters", "Steps: строка является числом ('"+ stepsString +"'), не список. Возвращен пустой список.");
                return Collections.emptyList();
            } catch (NumberFormatException nfe) {
                // Не число
            }
            Log.e("DataConverters", "Steps: все попытки преобразования JSON провалились для строки: " + stepsString + ". Возвращен пустой список.");
            return Collections.emptyList(); 
        } catch (Exception e) {
            Log.e("DataConverters", "Steps: непредвиденная ошибка при преобразовании JSON: " + stepsString, e);
            return Collections.emptyList();
        }
    }
} 
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
     * @param ingredientsString JSON-строка с ингредиентами.
     * @return Список ингредиентов или пустой список в случае ошибки или отсутствия данных.
     */
    @TypeConverter
    public static List<Ingredient> toIngredientList(String ingredientsString) {
        if (ingredientsString == null || ingredientsString.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<Ingredient> list = GSON_INSTANCE.fromJson(ingredientsString, INGREDIENT_LIST_TYPE);
            return list != null ? list : Collections.emptyList();
        } catch (Exception e) {
            Log.e("DataConverters", "Ошибка при преобразовании JSON в список ингредиентов: " + ingredientsString, e);
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
     * @param stepsString JSON-строка с шагами.
     * @return Список шагов или пустой список в случае ошибки или отсутствия данных.
     */
    @TypeConverter
    public static List<Step> toStepList(String stepsString) {
        if (stepsString == null || stepsString.isEmpty()) {
            return Collections.emptyList();
        }
        try {
            List<Step> list = GSON_INSTANCE.fromJson(stepsString, STEP_LIST_TYPE);
            if (list == null) {
                return Collections.emptyList();
            }
            for (int i = 0; i < list.size(); i++) {
                Step step = list.get(i);
                if (step.getNumber() <= 0) {
                    step.setNumber(i + 1);
                }
            }
            return list;
        } catch (Exception e) {
            Log.e("DataConverters", "Ошибка при преобразовании JSON в список шагов: " + stepsString, e);
            return Collections.emptyList();
        }
    }
} 
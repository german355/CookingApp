package com.example.cooking.Recipe;

import android.util.Log;

import java.lang.reflect.Type;
import java.util.ArrayList;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.example.cooking.Recipe.Ingredient;
import com.example.cooking.Recipe.Step;

public class DataConverters {
    private static final String TAG = "DataConverters";

    public static ArrayList<Ingredient> fromStringToIngredientList(String value) {
        if (value == null || value.isEmpty()) {
            Log.e(TAG, "Пустая строка JSON для ингредиентов");
            return new ArrayList<>();
        }
        
        try {
            Type listType = new TypeToken<ArrayList<Ingredient>>() {}.getType();
            ArrayList<Ingredient> ingredients = new Gson().fromJson(value, listType);
            
            if (ingredients == null) {
                Log.e(TAG, "Неверный формат JSON для ингредиентов: " + value);
                return new ArrayList<>();
            }
            
            return ingredients;
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "Ошибка при десериализации JSON ингредиентов: " + e.getMessage() + "\nJSON: " + value);
            return new ArrayList<>();
        }
    }

    public static String fromIngredientListToString(ArrayList<Ingredient> ingredients) {
        if (ingredients == null) {
            Log.e(TAG, "Передан null список ингредиентов для сериализации");
            return "[]";
        }
        
        try {
            String json = new Gson().toJson(ingredients);
            return json;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при сериализации ингредиентов в JSON: " + e.getMessage());
            return "[]";
        }
    }

    public static ArrayList<Step> fromStringToStepList(String value) {
        if (value == null || value.isEmpty()) {
            Log.e(TAG, "Пустая строка JSON для шагов");
            return new ArrayList<>();
        }
        
        try {
            Type listType = new TypeToken<ArrayList<Step>>() {}.getType();
            ArrayList<Step> steps = new Gson().fromJson(value, listType);
            
            if (steps == null) {
                Log.e(TAG, "Неверный формат JSON для шагов: " + value);
                return new ArrayList<>();
            }
            
            // Проверим номера шагов
            for (int i = 0; i < steps.size(); i++) {
                Step step = steps.get(i);
                if (step != null && step.getNumber() <= 0) {
                    Log.w(TAG, "Обнаружен некорректный номер шага: " + step.getNumber() + ", установка номера " + (i+1));
                    step.setNumber(i + 1);
                }
            }
            
            return steps;
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "Ошибка при десериализации JSON шагов: " + e.getMessage() + "\nJSON: " + value);
            return new ArrayList<>();
        }
    }

    public static String fromStepListToString(ArrayList<Step> steps) {
        if (steps == null) {
            Log.e(TAG, "Передан null список шагов для сериализации");
            return "[]";
        }
        
        try {
            String json = new Gson().toJson(steps);
            return json;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при сериализации шагов в JSON: " + e.getMessage());
            return "[]";
        }
    }
} 
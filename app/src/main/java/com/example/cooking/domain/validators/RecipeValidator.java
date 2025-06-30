package com.example.cooking.domain.validators;

import android.content.Context;
import android.util.Log;

import com.example.cooking.R;
import com.example.cooking.domain.entities.Ingredient;
import com.example.cooking.domain.entities.Step;

import java.util.List;

/**
 * Класс для валидации данных рецепта.
 * Централизует всю логику проверки корректности введенных данных.
 */
public class RecipeValidator {
    
    private static final String TAG = "RecipeValidator";
    
    private final Context context;
    
    public RecipeValidator(Context context) {
        this.context = context;
    }
    
    /**
     * Результат валидации
     */
    public static class ValidationResult {
        private final boolean isValid;
        private final String errorMessage;
        
        public ValidationResult(boolean isValid, String errorMessage) {
            this.isValid = isValid;
            this.errorMessage = errorMessage;
        }
        
        public boolean isValid() {
            return isValid;
        }
        
        public String getErrorMessage() {
            return errorMessage;
        }
        
        // Статические методы для создания результатов
        public static ValidationResult success() {
            return new ValidationResult(true, null);
        }
        
        public static ValidationResult error(String message) {
            return new ValidationResult(false, message);
        }
    }
    
    /**
     * Полная валидация всех данных рецепта
     */
    public ValidationResult validateAll(String title, List<Ingredient> ingredients, List<Step> steps) {
        Log.d(TAG, "validateAll: Начинаю полную валидацию рецепта");
        
        // Валидируем название
        ValidationResult titleResult = validateTitle(title);
        if (!titleResult.isValid()) {
            return titleResult;
        }
        
        // Валидируем ингредиенты
        ValidationResult ingredientsResult = validateIngredientsList(ingredients);
        if (!ingredientsResult.isValid()) {
            return ingredientsResult;
        }
        
        // Валидируем шаги
        ValidationResult stepsResult = validateStepsList(steps);
        if (!stepsResult.isValid()) {
            return stepsResult;
        }
        
        Log.d(TAG, "validateAll: Все данные валидны");
        return ValidationResult.success();
    }
    
    /**
     * Валидация названия рецепта
     */
    public ValidationResult validateTitle(String title) {
        if (title == null || title.trim().isEmpty()) {
            String error = context.getString(R.string.error_enter_recipe_name);
            Log.w(TAG, "validateTitle: Название пустое");
            return ValidationResult.error(error);
        }
        
        if (title.trim().length() < 3) {
            String error = "Название рецепта должно содержать минимум 3 символа";
            Log.w(TAG, "validateTitle: Название слишком короткое: " + title.trim().length());
            return ValidationResult.error(error);
        }
        
        if (title.trim().length() > 100) {
            String error = "Название рецепта не должно превышать 100 символов";
            Log.w(TAG, "validateTitle: Название слишком длинное: " + title.trim().length());
            return ValidationResult.error(error);
        }
        
        Log.d(TAG, "validateTitle: Название валидно");
        return ValidationResult.success();
    }
    
    /**
     * Валидация списка ингредиентов
     */
    public ValidationResult validateIngredientsList(List<Ingredient> ingredients) {
        if (ingredients == null || ingredients.isEmpty()) {
            String error = context.getString(R.string.error_add_at_least_one_ingredient);
            Log.w(TAG, "validateIngredientsList: Список ингредиентов пуст");
            return ValidationResult.error(error);
        }
        
        // Проверяем каждый ингредиент
        for (int i = 0; i < ingredients.size(); i++) {
            Ingredient ingredient = ingredients.get(i);
            ValidationResult ingredientResult = validateSingleIngredient(ingredient, i + 1);
            if (!ingredientResult.isValid()) {
                return ingredientResult;
            }
        }
        
        Log.d(TAG, "validateIngredientsList: Все ингредиенты валидны (" + ingredients.size() + " шт.)");
        return ValidationResult.success();
    }
    
    /**
     * Валидация одного ингредиента
     * @return результат валидации
     */
    public ValidationResult validateSingleIngredient(Ingredient ingredient, int position) {
        if (ingredient == null) {
            String error = "Ингредиент #" + position + " не может быть пустым";
            return ValidationResult.error(error);
        }
        
        // Проверяем название ингредиента
        if (ingredient.getName() == null || ingredient.getName().trim().isEmpty()) {
            String error = "Укажите название для ингредиента #" + position;
            return ValidationResult.error(error);
        }
        
        // Проверяем тип/единицу измерения
        if (ingredient.getType() == null || ingredient.getType().trim().isEmpty()) {
            String error = "Укажите единицу измерения для ингредиента #" + position;
            return ValidationResult.error(error);
        }
        
        // Проверяем количество
        if (ingredient.getCount() <= 0) {
            String error = "Укажите корректное количество для ингредиента #" + position;
            return ValidationResult.error(error);
        }
        
        return ValidationResult.success();
    }
    
    /**
     * Валидация списка шагов
     */
    public ValidationResult validateStepsList(List<Step> steps) {
        if (steps == null || steps.isEmpty()) {
            String error = context.getString(R.string.error_add_at_least_one_step);
            Log.w(TAG, "validateStepsList: Список шагов пуст");
            return ValidationResult.error(error);
        }
        
        // Проверяем каждый шаг
        for (int i = 0; i < steps.size(); i++) {
            Step step = steps.get(i);
            ValidationResult stepResult = validateSingleStep(step, i + 1);
            if (!stepResult.isValid()) {
                return stepResult;
            }
        }
        
        Log.d(TAG, "validateStepsList: Все шаги валидны (" + steps.size() + " шт.)");
        return ValidationResult.success();
    }
    
    /**
     * Валидация одного шага
     */
    public ValidationResult validateSingleStep(Step step, int position) {
        if (step == null) {
            String error = "Шаг #" + position + " не может быть пустым";
            Log.w(TAG, "validateSingleStep: Шаг null на позиции " + position);
            return ValidationResult.error(error);
        }
        
        // Проверяем описание шага
        if (step.getInstruction() == null || step.getInstruction().trim().isEmpty()) {
            String error = "Опишите действие для шага #" + position;
            Log.w(TAG, "validateSingleStep: Пустое описание шага на позиции " + position);
            return ValidationResult.error(error);
        }
        
        // Проверяем минимальную длину описания
        if (step.getInstruction().trim().length() < 7) {
            String error = "Описание шага #" + position + " слишком короткое (минимум 7 символов)";
            Log.w(TAG, "validateSingleStep: Слишком короткое описание шага на позиции " + position);
            return ValidationResult.error(error);
        }
        
        return ValidationResult.success();
    }
    

    /**
     * Быстрая проверка - можно ли сохранить рецепт
     */
    public boolean canSaveRecipe(String title, List<Ingredient> ingredients, List<Step> steps) {
        return validateAll(title, ingredients, steps).isValid();
    }

} 
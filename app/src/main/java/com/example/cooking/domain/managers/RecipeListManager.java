package com.example.cooking.domain.managers;

import android.util.Log;

import com.example.cooking.domain.entities.Ingredient;
import com.example.cooking.domain.entities.Step;

import java.util.ArrayList;
import java.util.List;

/**
 * Класс для управления списками ингредиентов и шагов в рецептах.
 * Централизует всю логику работы со списками: добавление, удаление, обновление, валидация.
 */
public class RecipeListManager {
    
    private static final String TAG = "RecipeListManager";

    
    public static class ListOperationResult {
        private final boolean success;
        private final String errorMessage;
        private final List<?> updatedList;
        
        private ListOperationResult(boolean success, String errorMessage, List<?> updatedList) {
            this.success = success;
            this.errorMessage = errorMessage;
            this.updatedList = updatedList;
        }
        
        public boolean isSuccess() { return success; }
        public String getErrorMessage() { return errorMessage; }
        @SuppressWarnings("unchecked")
        public <T> List<T> getUpdatedList() { return (List<T>) updatedList; }
        
        public static ListOperationResult success(List<?> updatedList) {
            return new ListOperationResult(true, null, updatedList);
        }
        
        public static ListOperationResult error(String errorMessage) {
            return new ListOperationResult(false, errorMessage, null);
        }
    }
    
    // === УПРАВЛЕНИЕ ИНГРЕДИЕНТАМИ ===

    public ListOperationResult addEmptyIngredient(List<Ingredient> currentList) {
        if (currentList == null) {
            currentList = new ArrayList<>();
        }
        
        List<Ingredient> newList = new ArrayList<>(currentList);
        Ingredient newIngredient = new Ingredient();
        newList.add(newIngredient);
        
        Log.d(TAG, "Добавлен пустой ингредиент. Размер списка: " + newList.size());
        return ListOperationResult.success(newList);
    }
    

    public ListOperationResult updateIngredient(List<Ingredient> currentList, int position, Ingredient ingredient) {
        if (currentList == null || position < 0 || position >= currentList.size()) {
            return ListOperationResult.error("Некорректная позиция ингредиента: " + position);
        }
        
        if (ingredient == null) {
            return ListOperationResult.error("Ингредиент не может быть null");
        }
        
        List<Ingredient> newList = new ArrayList<>(currentList);
        newList.set(position, ingredient);
        
        return ListOperationResult.success(newList);
    }

    public ListOperationResult removeIngredient(List<Ingredient> currentList, int position) {
        if (currentList == null || position < 0 || position >= currentList.size()) {
            return ListOperationResult.error("Некорректная позиция ингредиента: " + position);
        }
        
        // Не удаляем, если это последний ингредиент
        if (currentList.size() <= 1) {
            return ListOperationResult.error("Нельзя удалить последний ингредиент");
        }
        
        List<Ingredient> newList = new ArrayList<>(currentList);
        Ingredient removedIngredient = newList.remove(position);
        
        Log.d(TAG, "Удален ингредиент на позиции " + position + ": " + 
              (removedIngredient != null ? removedIngredient.getName() : "null"));
        return ListOperationResult.success(newList);
    }
    

    public List<Ingredient> initializeIngredientsList() {
        List<Ingredient> initialList = new ArrayList<>();
        initialList.add(new Ingredient());
        Log.d(TAG, "Инициализирован список ингредиентов с одним пустым элементом");
        return initialList;
    }


    public boolean canRemoveIngredient(List<Ingredient> currentList, int position) {
        return currentList != null && 
               currentList.size() > 1 && 
               position >= 0 && 
               position < currentList.size();
    }
    
    // === УПРАВЛЕНИЕ ШАГАМИ ===

    public ListOperationResult addEmptyStep(List<Step> currentList) {
        if (currentList == null) {
            currentList = new ArrayList<>();
        }
        
        List<Step> newList = new ArrayList<>(currentList);
        Step newStep = new Step();
        newStep.setNumber(newList.size() + 1); // Номера начинаются с 1
        newList.add(newStep);
        
        Log.d(TAG, "Добавлен пустой шаг #" + newStep.getNumber() + ". Размер списка: " + newList.size());
        return ListOperationResult.success(newList);
    }
    

    public ListOperationResult updateStep(List<Step> currentList, int position, Step step) {
        if (currentList == null || position < 0 || position >= currentList.size()) {
            return ListOperationResult.error("Некорректная позиция шага: " + position);
        }
        
        if (step == null) {
            return ListOperationResult.error("Шаг не может быть null");
        }
        
        List<Step> newList = new ArrayList<>(currentList);
        // Устанавливаем правильный номер шага
        step.setNumber(position + 1);
        newList.set(position, step);
        
        // Убираем избыточное логирование для улучшения производительности
        // Log.d(TAG, "Обновлен шаг на позиции " + position + " (шаг #" + step.getNumber() + ")");
        return ListOperationResult.success(newList);
    }
    

    public ListOperationResult removeStep(List<Step> currentList, int position) {
        if (currentList == null || position < 0 || position >= currentList.size()) {
            return ListOperationResult.error("Некорректная позиция шага: " + position);
        }
        
        // Не удаляем, если это последний шаг
        if (currentList.size() <= 1) {
            return ListOperationResult.error("Нельзя удалить последний шаг");
        }
        
        List<Step> newList = new ArrayList<>(currentList);
        Step removedStep = newList.remove(position);
        
        // Обновляем нумерацию оставшихся шагов
        renumberSteps(newList);
        
        Log.d(TAG, "Удален шаг на позиции " + position + " (был шаг #" + 
              (removedStep != null ? removedStep.getNumber() : "null") + ")");
        return ListOperationResult.success(newList);
    }

    public List<Step> initializeStepsList() {
        List<Step> initialList = new ArrayList<>();
        Step firstStep = new Step();
        firstStep.setNumber(1);
        initialList.add(firstStep);
        Log.d(TAG, "Инициализирован список шагов с одним пустым элементом");
        return initialList;
    }
    

    public boolean canRemoveStep(List<Step> currentList, int position) {
        return currentList != null && 
               currentList.size() > 1 && 
               position >= 0 && 
               position < currentList.size();
    }
    

    public void renumberSteps(List<Step> stepsList) {
        if (stepsList == null) return;
        
        for (int i = 0; i < stepsList.size(); i++) {
            Step step = stepsList.get(i);
            if (step != null) {
                step.setNumber(i + 1);
            }
        }
        
        Log.d(TAG, "Перенумерованы шаги в списке размером " + stepsList.size());
    }
    
    // === ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ===

    public List<Step> prepareStepsForSaving(List<Step> stepsList) {
        if (stepsList == null) {
            return new ArrayList<>();
        }
        
        List<Step> preparedList = new ArrayList<>(stepsList);
        renumberSteps(preparedList);
        
        Log.d(TAG, "Подготовлен список из " + preparedList.size() + " шагов для сохранения");
        return preparedList;
    }


    

} 
package com.example.cooking.ui.viewmodels.Recipe;

import android.app.Application;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

import com.example.cooking.domain.entities.Ingredient;
import com.example.cooking.domain.entities.Step;
import com.example.cooking.domain.usecases.RecipeFormUseCase;
import com.example.cooking.domain.usecases.RecipeManagementUseCase;
import com.example.cooking.domain.validators.RecipeValidator;
import com.example.cooking.utils.MySharedPreferences;

import io.reactivex.rxjava3.disposables.CompositeDisposable;

import java.util.List;

/**
 * Базовый ViewModel для работы с формами рецептов
 * Содержит общую логику для добавления и редактирования рецептов
 */
public abstract class BaseRecipeFormViewModel extends AndroidViewModel {
    protected static final String TAG = "BaseRecipeFormViewModel";
    
    // Общие сервисы
    protected final RecipeFormUseCase recipeFormUseCase;
    protected final RecipeManagementUseCase recipeManagementUseCase;
    protected final MySharedPreferences preferences;
    protected final CompositeDisposable disposables = new CompositeDisposable();
    
    public BaseRecipeFormViewModel(@NonNull Application application) {
        super(application);
        this.recipeFormUseCase = new RecipeFormUseCase(application);
        this.recipeManagementUseCase = new RecipeManagementUseCase(application);
        this.preferences = new MySharedPreferences(application);
    }

    /**
     * Получить текущий список ингредиентов
     */
    protected abstract List<Ingredient> getCurrentIngredients();
    
    /**
     * Установить новый список ингредиентов
     */
    protected abstract void setCurrentIngredients(List<Ingredient> ingredients);
    
    /**
     * Получить текущий список шагов
     */
    protected abstract List<Step> getCurrentSteps();
    
    /**
     * Установить новый список шагов
     */
    protected abstract void setCurrentSteps(List<Step> steps);
    
    /**
     * Получить текущее название рецепта
     */
    protected abstract String getCurrentTitle();
    
    /**
     * Установить новое название рецепта
     */
    protected abstract void setCurrentTitle(String title);
    
    /**
     * Установить сообщение об ошибке
     */
    protected abstract void setErrorMessage(String errorMessage);
    
    // === ОБЩИЕ МЕТОДЫ УПРАВЛЕНИЯ ИНГРЕДИЕНТАМИ ===
    
    /**
     * Добавляет пустой ингредиент в список
     */
    public void addEmptyIngredient() {
        try {
            List<Ingredient> updatedList = recipeFormUseCase.addEmptyIngredient(getCurrentIngredients());
            setCurrentIngredients(updatedList);
        } catch (RuntimeException e) {
            Log.e(TAG, "Ошибка при добавлении ингредиента", e);
            setErrorMessage(e.getMessage());
        }
    }
    
    /**
     * Обновляет ингредиент по позиции
     */
    public void updateIngredient(int position, Ingredient ingredient) {
        try {
            List<Ingredient> updatedList = recipeFormUseCase.updateIngredient(getCurrentIngredients(), position, ingredient);
            setCurrentIngredients(updatedList);
            
            // Убираем real-time валидацию - валидируем только при сохранении
            // Это предотвращает спам Toast сообщений при вводе каждого символа
        } catch (RuntimeException e) {
            Log.e(TAG, "Ошибка при обновлении ингредиента", e);
            setErrorMessage(e.getMessage());
        }
    }
    
    /**
     * Удаляет ингредиент по позиции
     */
    public void removeIngredient(int position) {
        try {
            List<Ingredient> updatedList = recipeFormUseCase.removeIngredient(getCurrentIngredients(), position);
            setCurrentIngredients(updatedList);
            
            // Убираем real-time валидацию - валидируем только при сохранении
            // Это предотвращает спам Toast сообщений
        } catch (RuntimeException e) {
            Log.e(TAG, "Ошибка при удалении ингредиента", e);
            setErrorMessage(e.getMessage());
        }
    }
    
    // === ОБЩИЕ МЕТОДЫ УПРАВЛЕНИЯ ШАГАМИ ===
    
    /**
     * Добавляет пустой шаг в список
     */
    public void addEmptyStep() {
        try {
            List<Step> updatedList = recipeFormUseCase.addEmptyStep(getCurrentSteps());
            setCurrentSteps(updatedList);
        } catch (RuntimeException e) {
            Log.e(TAG, "Ошибка при добавлении шага", e);
            setErrorMessage(e.getMessage());
        }
    }
    
    /**
     * Обновляет шаг по позиции
     */
    public void updateStep(int position, Step step) {
        try {
            List<Step> updatedList = recipeFormUseCase.updateStep(getCurrentSteps(), position, step);
            setCurrentSteps(updatedList);
            
            // Убираем real-time валидацию - валидируем только при сохранении
            // Это предотвращает спам Toast сообщений при вводе каждого символа
        } catch (RuntimeException e) {
            Log.e(TAG, "Ошибка при обновлении шага", e);
            setErrorMessage(e.getMessage());
        }
    }
    
    /**
     * Удаляет шаг по позиции
     */
    public void removeStep(int position) {
        try {
            List<Step> updatedList = recipeFormUseCase.removeStep(getCurrentSteps(), position);
            setCurrentSteps(updatedList);
            
            // Убираем real-time валидацию - валидируем только при сохранении
            // Это предотвращает спам Toast сообщений
        } catch (RuntimeException e) {
            Log.e(TAG, "Ошибка при удалении шага", e);
            setErrorMessage(e.getMessage());
        }
    }
    
    // === ОБЩИЕ МЕТОДЫ ВАЛИДАЦИИ ===
    
    /**
     * Устанавливает название рецепта с валидацией
     */
    public void setTitle(String title) {
        setCurrentTitle(title);
        
        // Убираем real-time валидацию заголовка - валидируем только при сохранении
        // Это предотвращает показ ошибок при неполном вводе заголовка
    }
    
    /**
     * Проверяет готовность рецепта к сохранению
     */
    public boolean canSaveRecipe() {
        return recipeFormUseCase.canSaveRecipe(
            getCurrentTitle(),
            getCurrentIngredients(),
            getCurrentSteps()
        );
    }
    
    // === АБСТРАКТНЫЕ МЕТОДЫ ДЛЯ ОШИБОК ВАЛИДАЦИИ ===
    
    /**
     * Установить ошибку валидации названия
     */
    protected abstract void setTitleError(String error);
    
    /**
     * Установить ошибку валидации списка ингредиентов
     */
    protected abstract void setIngredientsError(String error);
    
    /**
     * Установить ошибку валидации списка шагов
     */
    protected abstract void setStepsError(String error);
    
    // === ОБЩИЕ МЕТОДЫ ОБРАБОТКИ ИЗОБРАЖЕНИЙ ===
    
    /**
     * Обрабатывает выбранное изображение (RxJava для асинхронности)
     */
    public void processSelectedImage(Uri imageUri) {
        disposables.add(
            recipeFormUseCase.processImage(imageUri)
                .doOnSubscribe(d -> setImageProcessing(true))
                .doFinally(() -> setImageProcessing(false))
                .subscribe(
                    this::onImageProcessed,
                    error -> {
                        onImageError(error.getMessage());
                        Log.e(TAG, "Ошибка при обработке изображения", error);
                    }
                )
        );
    }
    
    // === АБСТРАКТНЫЕ МЕТОДЫ ДЛЯ ОБРАБОТКИ ИЗОБРАЖЕНИЙ ===
    
    /**
     * Установить флаг обработки изображения
     */
    protected abstract void setImageProcessing(boolean processing);
    
    /**
     * Обработать успешно обработанное изображение
     */
    protected abstract void onImageProcessed(byte[] imageBytes);
    
    /**
     * Обработать ошибку обработки изображения
     */
    protected abstract void onImageError(String error);
    
    // === ОЧИСТКА РЕСУРСОВ ===
    
    @Override
    protected void onCleared() {
        super.onCleared();
        // Очищаем RxJava подписки
        disposables.clear();
        // Очищаем ресурсы Use Cases
        recipeFormUseCase.clearResources();
        recipeManagementUseCase.clearResources();
    }
} 
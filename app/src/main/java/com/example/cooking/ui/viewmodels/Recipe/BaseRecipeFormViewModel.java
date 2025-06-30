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
    
    public void addEmptyIngredient() {
        executeWithErrorHandling(() -> {
            List<Ingredient> updatedList = recipeFormUseCase.addEmptyIngredient(getCurrentIngredients());
            setCurrentIngredients(updatedList);
        }, "добавлении ингредиента");
    }
    
    public void updateIngredient(int position, Ingredient ingredient) {
        executeWithErrorHandling(() -> {
            List<Ingredient> updatedList = recipeFormUseCase.updateIngredient(getCurrentIngredients(), position, ingredient);
            setCurrentIngredients(updatedList);
        }, "обновлении ингредиента");
    }
    
    public void removeIngredient(int position) {
        executeWithErrorHandling(() -> {
            List<Ingredient> updatedList = recipeFormUseCase.removeIngredient(getCurrentIngredients(), position);
            setCurrentIngredients(updatedList);
        }, "удалении ингредиента");
    }
    
    // === ОБЩИЕ МЕТОДЫ УПРАВЛЕНИЯ ШАГАМИ ===
    
    public void addEmptyStep() {
        executeWithErrorHandling(() -> {
            List<Step> updatedList = recipeFormUseCase.addEmptyStep(getCurrentSteps());
            setCurrentSteps(updatedList);
        }, "добавлении шага");
    }
    
    public void updateStep(int position, Step step) {
        executeWithErrorHandling(() -> {
            List<Step> updatedList = recipeFormUseCase.updateStep(getCurrentSteps(), position, step);
            setCurrentSteps(updatedList);
        }, "обновлении шага");
    }
    
    public void removeStep(int position) {
        executeWithErrorHandling(() -> {
            List<Step> updatedList = recipeFormUseCase.removeStep(getCurrentSteps(), position);
            setCurrentSteps(updatedList);
        }, "удалении шага");
    }
    
    // === ОБЩИЙ МЕТОД ОБРАБОТКИ ОШИБОК ===
    
    private void executeWithErrorHandling(Runnable operation, String operationType) {
        try {
            operation.run();
        } catch (RuntimeException e) {
            Log.e(TAG, "Ошибка при " + operationType, e);
            setErrorMessage(e.getMessage());
        }
    }
    
    // === ОБЩИЕ МЕТОДЫ ВАЛИДАЦИИ ===
    
    public void setTitle(String title) {
        setCurrentTitle(title);
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
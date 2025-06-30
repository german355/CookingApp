package com.example.cooking.domain.usecases;

import android.app.Application;
import android.net.Uri;
import android.util.Log;

import com.example.cooking.domain.entities.Ingredient;
import com.example.cooking.domain.entities.Recipe;
import com.example.cooking.domain.entities.Step;
import com.example.cooking.domain.managers.RecipeListManager;
import com.example.cooking.domain.services.ImageProcessor;
import com.example.cooking.domain.validators.RecipeValidator;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

/**
 * UseCase для работы с формой рецепта
 * Инкапсулирует общую логику для AddRecipeViewModel и EditRecipeViewModel:
 * 
 */
public class RecipeFormUseCase {
    private static final String TAG = "RecipeFormUseCase";
    
    private final RecipeListManager listManager;
    private final RecipeValidator validator;
    private final ImageProcessor imageProcessor;
    private final Application application;
    private final CompositeDisposable disposables = new CompositeDisposable();
    
    public RecipeFormUseCase(Application application) {
        this.application = application;
        this.listManager = new RecipeListManager();
        this.validator = new RecipeValidator(application);
        this.imageProcessor = new ImageProcessor(application);
    }
    
    // === УПРАВЛЕНИЕ СПИСКАМИ ИНГРЕДИЕНТОВ (простые синхронные операции) ===
    
    /**
     * Добавляет пустой ингредиент в список
     */
    public List<Ingredient> addEmptyIngredient(List<Ingredient> currentIngredients) {
        RecipeListManager.ListOperationResult result = 
            listManager.addEmptyIngredient(currentIngredients);
        if (result.isSuccess()) {
            return result.getUpdatedList();
        } else {
            throw new RuntimeException(result.getErrorMessage());
        }
    }
    
    /**
     * Обновляет ингредиент по позиции
     */
    public List<Ingredient> updateIngredient(List<Ingredient> ingredients, 
                                           int position, Ingredient ingredient) {
        RecipeListManager.ListOperationResult result = 
            listManager.updateIngredient(ingredients, position, ingredient);
        if (result.isSuccess()) {
            return result.getUpdatedList();
        } else {
            throw new RuntimeException(result.getErrorMessage());
        }
    }
    
    /**
     * Удаляет ингредиент по позиции
     */
    public List<Ingredient> removeIngredient(List<Ingredient> ingredients, int position) {
        if (!listManager.canRemoveIngredient(ingredients, position)) {
            throw new RuntimeException("Нельзя удалить единственный ингредиент");
        }
        
        RecipeListManager.ListOperationResult result = 
            listManager.removeIngredient(ingredients, position);
        if (result.isSuccess()) {
            return result.getUpdatedList();
        } else {
            throw new RuntimeException(result.getErrorMessage());
        }
    }
    
    // === УПРАВЛЕНИЕ СПИСКАМИ ШАГОВ (простые синхронные операции) ===
    
    /**
     * Добавляет пустой шаг в список
     */
    public List<Step> addEmptyStep(List<Step> currentSteps) {
        RecipeListManager.ListOperationResult result = 
            listManager.addEmptyStep(currentSteps);
        if (result.isSuccess()) {
            return result.getUpdatedList();
        } else {
            throw new RuntimeException(result.getErrorMessage());
        }
    }
    
    /**
     * Обновляет шаг по позиции
     */
    public List<Step> updateStep(List<Step> steps, int position, Step step) {
        RecipeListManager.ListOperationResult result = 
            listManager.updateStep(steps, position, step);
        if (result.isSuccess()) {
            return result.getUpdatedList();
        } else {
            throw new RuntimeException(result.getErrorMessage());
        }
    }
    
    /**
     * Удаляет шаг по позиции
     */
    public List<Step> removeStep(List<Step> steps, int position) {
        if (!listManager.canRemoveStep(steps, position)) {
            throw new RuntimeException("Нельзя удалить единственный шаг");
        }
        
        RecipeListManager.ListOperationResult result = 
            listManager.removeStep(steps, position);
        if (result.isSuccess()) {
            return result.getUpdatedList();
        } else {
            throw new RuntimeException(result.getErrorMessage());
        }
    }
    


    /**
     * Проверка готовности рецепта к сохранению (простая операция)
     */
    public boolean canSaveRecipe(String title, List<Ingredient> ingredients, List<Step> steps) {
        return validator.canSaveRecipe(title, ingredients, steps);
    }

    
    // === ОБРАБОТКА ИЗОБРАЖЕНИЙ ===
    
    /**
     * Обрабатывает изображение из URI
     */
    public Single<byte[]> processImage(Uri imageUri) {
        return Single.<byte[]>create(emitter -> {
            if (imageUri == null) {
                emitter.onError(new Exception("URI изображения не может быть null"));
                return;
            }
            
            imageProcessor.processImageFromUri(imageUri, 800, 85, 
                new ImageProcessor.ImageProcessingCallback() {
                    @Override
                    public void onSuccess(ImageProcessor.ImageResult result) {
                        emitter.onSuccess(result.getImageBytes());
                        Log.d(TAG, "Изображение обработано (" + 
                              result.getProcessedWidth() + "x" + result.getProcessedHeight() + 
                              ", " + (result.getProcessedSize() / 1024) + " КБ)");
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        emitter.onError(new Exception(errorMessage));
                        Log.e(TAG, "Ошибка обработки изображения: " + errorMessage);
                    }
                    
                    @Override
                    public void onProgress(int progress) {

                    }
                });
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * Загружает изображение по URL
     */
    public Single<byte[]> loadImageFromUrl(String url) {
        return Single.<byte[]>create(emitter -> {
            if (url == null || url.isEmpty()) {
                emitter.onError(new Exception("URL изображения пуст"));
                return;
            }
            
            imageProcessor.processImageFromUrl(url, 800, 80, 
                new ImageProcessor.ImageProcessingCallback() {
                    @Override
                    public void onSuccess(ImageProcessor.ImageResult result) {
                        emitter.onSuccess(result.getImageBytes());
                        Log.d(TAG, "Изображение загружено с URL (" + 
                              result.getProcessedWidth() + "x" + result.getProcessedHeight() + ")");
                    }
                    
                    @Override
                    public void onError(String errorMessage) {
                        emitter.onError(new Exception(errorMessage));
                        Log.e(TAG, "Ошибка загрузки изображения: " + errorMessage);
                    }
                    
                    @Override
                    public void onProgress(int progress) {
                        // Прогресс загрузки
                    }
                });
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread());
    }
    
    // === ПОДГОТОВКА ДАННЫХ ===
    
    /**
     * Создает объект Recipe из данных формы (синхронная версия)
     */
    public Recipe buildRecipe(String title, 
                            List<Ingredient> ingredients, 
                            List<Step> steps, 
                            String userId, 
                            Integer recipeId, 
                            String photoUrl) {
        // Подготавливаем шаги для сохранения (обновляем нумерацию)
        List<Step> preparedSteps = listManager.prepareStepsForSaving(steps);
        
        Recipe recipe = new Recipe();
        if (recipeId != null && recipeId != 0) {
            recipe.setId(recipeId);
        }
        recipe.setTitle(title);
        recipe.setUserId(userId);
        recipe.setIngredients(new ArrayList<>(ingredients));
        recipe.setSteps(new ArrayList<>(preparedSteps));
        recipe.setPhoto_url(photoUrl);
        recipe.setLiked(false);
        
        Log.d(TAG, "Recipe построен: ID=" + recipe.getId() + 
                  ", Title=" + recipe.getTitle() + 
                  ", UserId=" + recipe.getUserId() + 
                  ", Ingredients=" + ingredients.size() + 
                  ", Steps=" + preparedSteps.size());
        
        return recipe;
    }
    
    /**
     * Создает объект Recipe из данных формы (асинхронная версия для обратной совместимости)
     * @deprecated Используйте синхронную версию buildRecipe()
     */
    @Deprecated
    public Single<Recipe> buildRecipeAsync(String title, 
                                         List<Ingredient> ingredients, 
                                         List<Step> steps, 
                                         String userId, 
                                         Integer recipeId, 
                                         String photoUrl) {
        return Single.fromCallable(() -> buildRecipe(title, ingredients, steps, userId, recipeId, photoUrl))
                .subscribeOn(Schedulers.computation())
                .observeOn(AndroidSchedulers.mainThread());
    }
    
    /**
     * Инициализирует списки для новой формы (простая операция)
     */
    public FormInitResult initializeForm() {
        List<Ingredient> ingredients = listManager.initializeIngredientsList();
        List<Step> steps = listManager.initializeStepsList();
        return new FormInitResult(ingredients, steps);
    }
    
    /**
     * Результат инициализации формы
     */
    public static class FormInitResult {
        private final List<Ingredient> ingredients;
        private final List<Step> steps;
        
        public FormInitResult(List<Ingredient> ingredients, List<Step> steps) {
            this.ingredients = ingredients;
            this.steps = steps;
        }
        
        public List<Ingredient> getIngredients() { return ingredients; }
        public List<Step> getSteps() { return steps; }
    }
    
    /**
     * Очищает ресурсы
     */
    public void clearResources() {
        disposables.clear();
        Log.d(TAG, "Ресурсы очищены");
    }
} 
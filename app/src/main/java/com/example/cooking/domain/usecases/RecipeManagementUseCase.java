package com.example.cooking.domain.usecases;

import android.app.Application;
import com.example.cooking.domain.entities.Recipe;
import com.example.cooking.data.repositories.UnifiedRecipeRepository;
import com.example.cooking.network.models.GeneralServerResponse;
import com.example.cooking.domain.validators.RecipeValidator;

/**
 * Use Case для управления рецептами (CRUD операции)
 * Отвечает за создание, обновление и удаление рецептов
 */
public class RecipeManagementUseCase {
    private static final String TAG = "RecipeManagementUseCase";
    
    private final UnifiedRecipeRepository repository;
    private final Application application;
    private final android.net.ConnectivityManager connectivityManager;
    private final RecipeValidator validator;
    
    public RecipeManagementUseCase(Application application) {
        this.application = application;
        this.repository = UnifiedRecipeRepository.getInstance(application);
        this.connectivityManager = (android.net.ConnectivityManager) 
            application.getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
        this.validator = new RecipeValidator(application);
    }
    
    /**
     * Интерфейс для колбэка сохранения рецепта
     */
    public interface RecipeSaveCallback {
        void onSuccess(GeneralServerResponse response, Recipe savedRecipe);
        void onFailure(String error, GeneralServerResponse errorResponse);
    }
    
    /**
     * Интерфейс для колбэка удаления рецепта
     */
    public interface DeleteRecipeCallback {
        void onDeleteSuccess();
        void onDeleteFailure(String error);
    }
    
    /**
     * Сохраняет новый рецепт
     */
    public void saveRecipe(Recipe recipe, byte[] imageBytes, RecipeSaveCallback callback) {
        // Валидация рецепта
        RecipeValidator.ValidationResult validationResult = validator.validateAll(
            recipe.getTitle(), 
            recipe.getIngredients(), 
            recipe.getSteps()
        );
        
        if (!validationResult.isValid()) {
            if (callback != null) {
                callback.onFailure(validationResult.getErrorMessage(), null);
            }
            return;
        }
        
        if (!isNetworkAvailable()) {
            if (callback != null) {
                callback.onFailure("Ошибка соединения с сервером. Проверьте подключение к интернету", null);
            }
            return;
        }
        
        repository.saveRecipe(recipe, imageBytes, new UnifiedRecipeRepository.RecipeCallback<Recipe>() {
            @Override
            public void onSuccess(Recipe savedRecipe) {
                if (callback != null) {
                    GeneralServerResponse response = new GeneralServerResponse();
                    response.setSuccess(true);
                    response.setId(savedRecipe.getId());
                    callback.onSuccess(response, savedRecipe);
                }
            }
            
            @Override
            public void onFailure(String error) {
                if (callback != null) {
                    callback.onFailure(error, null);
                }
            }
        });
    }
    
    /**
     * Обновляет существующий рецепт
     */
    public void updateRecipe(Recipe recipe, byte[] imageBytes, RecipeSaveCallback callback) {
        // Валидация рецепта
        RecipeValidator.ValidationResult validationResult = validator.validateAll(
            recipe.getTitle(), 
            recipe.getIngredients(), 
            recipe.getSteps()
        );
        
        if (!validationResult.isValid()) {
            if (callback != null) {
                callback.onFailure(validationResult.getErrorMessage(), null);
            }
            return;
        }
        
        if (!isNetworkAvailable()) {
            if (callback != null) {
                callback.onFailure("Ошибка соединения с сервером. Проверьте подключение к интернету", null);
            }
            return;
        }
        
        repository.updateRecipe(recipe, imageBytes, new UnifiedRecipeRepository.RecipeCallback<Recipe>() {
            @Override
            public void onSuccess(Recipe updatedRecipe) {
                if (callback != null) {
                    GeneralServerResponse response = new GeneralServerResponse();
                    response.setSuccess(true);
                    response.setId(updatedRecipe.getId());
                    if (updatedRecipe.getPhoto_url() != null) {
                        response.setPhotoUrl(updatedRecipe.getPhoto_url());
                    }
                    callback.onSuccess(response, updatedRecipe);
                }
            }
            
            @Override
            public void onFailure(String error) {
                 if (callback != null) {
                    callback.onFailure(error, null);
                }
            }
        });
    }
    
    
    /**
     * Удаляет рецепт
     */
    public void deleteRecipe(int recipeId, DeleteRecipeCallback callback) {
        if (!isNetworkAvailable()) {
            if (callback != null) {
                callback.onDeleteFailure("Невозможно удалить рецепт в офлайн режиме");
            }
            return;
        }
        
        repository.deleteRecipe(recipeId, new UnifiedRecipeRepository.RecipeCallback<Void>() {
            @Override
            public void onSuccess(Void data) {
                if (callback != null) {
                    callback.onDeleteSuccess();
                }
            }
            
            @Override
            public void onFailure(String error) {
                if (callback != null) {
                    callback.onDeleteFailure(error);
                }
            }
        });
    }
    
    /**
     * Проверяет доступность сети
     */
    public boolean isNetworkAvailable() {
        if (connectivityManager != null) {
            android.net.NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }
    
    /**
     * Очищает ресурсы
     */
    public void clearResources() {
        repository.clearDisposables();
    }
} 
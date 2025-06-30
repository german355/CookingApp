package com.example.cooking.ui.viewmodels.Recipe;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cooking.domain.entities.Ingredient;
import com.example.cooking.domain.entities.Recipe;
import com.example.cooking.domain.entities.Step;
import com.example.cooking.domain.usecases.RecipeManagementUseCase;
import com.example.cooking.R;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel для экрана добавления рецепта
 * Максимально использует функциональность BaseRecipeFormViewModel
 */
public class AddRecipeViewModel extends BaseRecipeFormViewModel {
    private static final String TAG = "AddRecipeViewModel";
    
    // Минимальный набор LiveData для UI состояний
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>(false);
    
    // Данные рецепта - используем простые поля вместо LiveData для упрощения
    private final MutableLiveData<String> title = new MutableLiveData<>("");
    private final MutableLiveData<List<Ingredient>> ingredients = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Step>> steps = new MutableLiveData<>(new ArrayList<>());
    private byte[] imageBytes = null;

    public AddRecipeViewModel(@NonNull Application application) {
        super(application);
        // Инициализируем форму через UseCase
        var formResult = recipeFormUseCase.initializeForm();
        ingredients.setValue(formResult.getIngredients());
        steps.setValue(formResult.getSteps());
    }
    
    /**
     * Сохраняет новый рецепт
     */
    public void saveRecipe() {
        String userId = preferences.getString("userId", "99");
        Log.d(TAG, "Saving recipe for userId: " + userId);
        
        isLoading.setValue(true);
        
        try {
            Recipe newRecipe = recipeFormUseCase.buildRecipe(
                title.getValue(),
                ingredients.getValue(),
                steps.getValue(),
                userId, null, null
            );
            
            recipeManagementUseCase.saveRecipe(newRecipe, imageBytes, 
                new RecipeManagementUseCase.RecipeSaveCallback() {
                    @Override
                    public void onSuccess(com.example.cooking.network.models.GeneralServerResponse response, Recipe savedRecipe) {
                        isLoading.postValue(false);
                        if (response.isSuccess() && savedRecipe != null) {
                            saveSuccess.postValue(true);
                        } else {
                            handleSaveError(response != null ? response.getMessage() : "Неизвестная ошибка");
                        }
                    }

                    @Override
                    public void onFailure(String error, com.example.cooking.network.models.GeneralServerResponse errorResponse) {
                        isLoading.postValue(false);

                        // Специальная обработка ошибок модерации
                        if (error != null && error.startsWith("Модерация:")) {
                            String moderationMessage = error.substring("Модерация:".length()).trim();
                            errorMessage.postValue(moderationMessage.isEmpty() ? 
                                getApplication().getString(R.string.moderation_failed_generic) :
                                getApplication().getString(R.string.moderation_failed, moderationMessage));
                            return;
                        }

                        String detailedError = error;
                        if (errorResponse != null && errorResponse.getMessage() != null) {
                            detailedError += " (Сервер: " + errorResponse.getMessage() + ")";
                        }
                        errorMessage.postValue(detailedError);
                    }
                }
            );
        } catch (Exception error) {
            isLoading.setValue(false);
            errorMessage.setValue(error.getMessage());
        }
    }
    
    private void handleSaveError(String message) {
        String errorMsg = getApplication().getString(R.string.error_saving_recipe);
        if (message != null) {
            errorMsg += ": " + message;
        }
        errorMessage.postValue(errorMsg);
    }
    
    // Простые геттеры
    public LiveData<Boolean> getIsLoading() { return isLoading; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getSaveSuccess() { return saveSuccess; }
    public LiveData<List<Ingredient>> getIngredients() { return ingredients; }
    public LiveData<List<Step>> getSteps() { return steps; }
    public LiveData<String> getTitle() { return title; }
    
    public boolean hasImage() { return imageBytes != null && imageBytes.length > 0; }
    public void clearErrorMessage() { errorMessage.setValue(null); }
    
    // === РЕАЛИЗАЦИЯ АБСТРАКТНЫХ МЕТОДОВ ===
    @Override protected List<Ingredient> getCurrentIngredients() { return ingredients.getValue(); }
    @Override protected void setCurrentIngredients(List<Ingredient> ingredients) { this.ingredients.setValue(ingredients); }
    @Override protected List<Step> getCurrentSteps() { return steps.getValue(); }
    @Override protected void setCurrentSteps(List<Step> steps) { this.steps.setValue(steps); }
    @Override protected String getCurrentTitle() { return title.getValue(); }
    @Override protected void setCurrentTitle(String title) { this.title.setValue(title); }
    @Override protected void setErrorMessage(String errorMessage) { this.errorMessage.setValue(errorMessage); }
    @Override protected void setImageProcessing(boolean processing) { this.isLoading.setValue(processing); }
    @Override protected void onImageProcessed(byte[] imageBytes) { this.imageBytes = imageBytes; }
    @Override protected void onImageError(String error) { this.imageBytes = null; this.errorMessage.setValue(error); }
}
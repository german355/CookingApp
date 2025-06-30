package com.example.cooking.ui.viewmodels.Recipe;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cooking.R;
import com.example.cooking.domain.entities.Ingredient;
import com.example.cooking.domain.entities.Recipe;
import com.example.cooking.domain.entities.Step;
import com.example.cooking.domain.usecases.RecipeFormUseCase;
import com.example.cooking.domain.usecases.RecipeManagementUseCase;
import com.example.cooking.domain.usecases.UserPermissionUseCase;
import com.example.cooking.utils.AppExecutors;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel для экрана редактирования рецепта
 * Упрощенная версия без EditRecipeState
 */
public class EditRecipeViewModel extends BaseRecipeFormViewModel {
    private static final String TAG = "EditRecipeViewModel";

    private final UserPermissionUseCase userPermissionUseCase;
    
    // Простые LiveData поля вместо EditRecipeState
    private final MutableLiveData<Boolean> isSaving = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saveResult = new MutableLiveData<>();
    
    private final MutableLiveData<String> title = new MutableLiveData<>("");
    private final MutableLiveData<List<Ingredient>> ingredients = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Step>> steps = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<String> photoUrl = new MutableLiveData<>();
    private final MutableLiveData<byte[]> imageBytes = new MutableLiveData<>();
    
    // Метаданные рецепта
    private Integer recipeId;
    private String recipeOwnerId;

    public EditRecipeViewModel(@NonNull Application application) {
        super(application);
        this.userPermissionUseCase = new UserPermissionUseCase(application);
    }

    /**
     * Устанавливает данные рецепта для редактирования
     */
    public void setRecipeData(@NonNull Recipe recipeToEdit) {
        this.recipeId = recipeToEdit.getId();
        this.recipeOwnerId = recipeToEdit.getUserId();
        
        title.setValue(recipeToEdit.getTitle() != null ? recipeToEdit.getTitle() : "");
        photoUrl.setValue(recipeToEdit.getPhoto_url());
        imageBytes.setValue(null);

        // Инициализируем списки или используем дефолтные
        List<Ingredient> initialIngredients = recipeToEdit.getIngredients();
        List<Step> initialSteps = recipeToEdit.getSteps();

        if (initialIngredients == null || initialIngredients.isEmpty() || 
            initialSteps == null || initialSteps.isEmpty()) {
            var formResult = recipeFormUseCase.initializeForm();
            if (initialIngredients == null || initialIngredients.isEmpty()) {
                initialIngredients = formResult.getIngredients();
            }
            if (initialSteps == null || initialSteps.isEmpty()) {
                initialSteps = formResult.getSteps();
            }
        }

        ingredients.setValue(new ArrayList<>(initialIngredients));
        steps.setValue(new ArrayList<>(initialSteps));

        Log.d(TAG, "Данные установлены для рецепта ID: " + recipeId);
    }

    /**
     * Загружает изображение по URL
     */
    public void loadImageFromUrl(String url) {
        if (url == null || url.isEmpty() || imageBytes.getValue() != null) {
            return;
        }
        
        disposables.add(
            recipeFormUseCase.loadImageFromUrl(url)
                .subscribe(
                    bytes -> {
                        if (imageBytes.getValue() == null) {
                            imageBytes.postValue(bytes);
                            Log.d(TAG, "Изображение загружено по URL");
                        }
                    },
                    error -> Log.e(TAG, "Ошибка загрузки изображения: " + error.getMessage())
                )
        );
    }

    /**
     * Сохраняет изменения рецепта
     */
    public void saveRecipe() {
        // Проверка прав
        if (recipeOwnerId != null) {
            var permissionResult = userPermissionUseCase.canEditRecipe(recipeOwnerId);
            if (!permissionResult.hasPermission()) {
                errorMessage.setValue(permissionResult.getReason());
                return;
            }
        }

        if (recipeId == null) {
            errorMessage.setValue(getApplication().getString(R.string.error_missing_data));
            return;
        }

        isSaving.setValue(true);

        try {
            Recipe updatedRecipe = recipeFormUseCase.buildRecipe(
                title.getValue(),
                ingredients.getValue(),
                steps.getValue(),
                recipeOwnerId,
                recipeId,
                photoUrl.getValue()
            );
            
            recipeManagementUseCase.updateRecipe(updatedRecipe, imageBytes.getValue(),
                new RecipeManagementUseCase.RecipeSaveCallback() {
                    @Override
                    public void onSuccess(com.example.cooking.network.models.GeneralServerResponse response, Recipe savedRecipe) {
                        AppExecutors.getInstance().mainThread().execute(() -> {
                            isSaving.setValue(false);
                            saveResult.setValue(true);
                        });
                    }

                    @Override
                    public void onFailure(String error, com.example.cooking.network.models.GeneralServerResponse errorResponse) {
                        AppExecutors.getInstance().mainThread().execute(() -> {
                            isSaving.setValue(false);
                            handleSaveError(error, errorResponse);
                        });
                    }
                }
            );
        } catch (Exception error) {
            isSaving.setValue(false);
            errorMessage.setValue(error.getMessage());
        }
    }
    

    
    private void handleSaveError(String error, com.example.cooking.network.models.GeneralServerResponse errorResponse) {
        if (error != null && error.startsWith("Модерация:")) {
            String moderationMessage = error.substring("Модерация:".length()).trim();
            errorMessage.setValue(moderationMessage.isEmpty() ? 
                getApplication().getString(R.string.moderation_failed_generic) :
                getApplication().getString(R.string.moderation_failed, moderationMessage));
            return;
        }
        
        String detailedError = error;
        if (errorResponse != null && errorResponse.getMessage() != null) {
            detailedError += " (Детали: " + errorResponse.getMessage() + ")";
        }
        errorMessage.setValue(detailedError);
    }
    
    // Простые геттеры
    public LiveData<String> getTitle() { return title; }
    public LiveData<List<Ingredient>> getIngredients() { return ingredients; }
    public LiveData<List<Step>> getSteps() { return steps; }
    public LiveData<String> getPhotoUrl() { return photoUrl; }
    public LiveData<byte[]> getImageBytes() { return imageBytes; }
    public LiveData<Boolean> getIsSaving() { return isSaving; }
    public LiveData<Boolean> getSaveResult() { return saveResult; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    
    public void clearSaveResult() { saveResult.setValue(null); }
    public void clearErrorMessage() { errorMessage.setValue(null); }

    // === РЕАЛИЗАЦИЯ АБСТРАКТНЫХ МЕТОДОВ ===
    @Override protected List<Ingredient> getCurrentIngredients() { return ingredients.getValue(); }
    @Override protected void setCurrentIngredients(List<Ingredient> ingredients) { this.ingredients.setValue(ingredients); }
    @Override protected List<Step> getCurrentSteps() { return steps.getValue(); }
    @Override protected void setCurrentSteps(List<Step> steps) { this.steps.setValue(steps); }
    @Override protected String getCurrentTitle() { return title.getValue(); }
    @Override protected void setCurrentTitle(String title) { this.title.setValue(title); }
    @Override protected void setErrorMessage(String errorMessage) { this.errorMessage.setValue(errorMessage); }
    @Override protected void setImageProcessing(boolean processing) { this.isSaving.setValue(processing); }
    @Override protected void onImageProcessed(byte[] imageBytes) { 
        this.imageBytes.setValue(imageBytes);
        this.photoUrl.setValue(null); // Сбрасываем старый URL
    }
    @Override protected void onImageError(String error) { 
        this.errorMessage.setValue(error);
        this.imageBytes.setValue(null);
    }
}

package com.example.cooking.ui.viewmodels.Recipe;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.cooking.domain.entities.Recipe;
import com.example.cooking.utils.MySharedPreferences;
import com.example.cooking.domain.usecases.RecipeDataUseCase;
import com.example.cooking.domain.usecases.RecipeLikeUseCase;
import com.example.cooking.domain.usecases.RecipeManagementUseCase;
import com.example.cooking.utils.AppExecutors;

/**
 * ViewModel для экрана детальной информации о рецепте
 */
public class RecipeDetailViewModel extends AndroidViewModel {
    private static final String TAG = "RecipeDetailViewModel";
    
    private final RecipeDataUseCase recipeDataUseCase;
    private final RecipeLikeUseCase recipeLikeUseCase;
    private final RecipeManagementUseCase recipeManagementUseCase;
    private final MySharedPreferences preferences;

    private final MutableLiveData<Recipe> recipe = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLiked = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> deleteSuccess = new MutableLiveData<>();
    
    private int recipeId;
    private int permissionLevel;
    
    /**
     * Конструктор
     */
    public RecipeDetailViewModel(@NonNull Application application) {
        super(application);
        this.recipeDataUseCase = new RecipeDataUseCase(application);
        this.recipeLikeUseCase = new RecipeLikeUseCase(application);
        this.recipeManagementUseCase = new RecipeManagementUseCase(application);
        this.preferences = new MySharedPreferences(application);
    }
    
    /**
     * Загружает данные о рецепте
     */
    public void init(int recipeId, int permissionLevel) {
        this.recipeId = recipeId;
        this.permissionLevel = permissionLevel;
        loadRecipe(recipeId);
    }
    
    /**
     * Загружает рецепт из SharedRecipeViewModel
     */
    private void loadRecipe(int recipeId) {
        isLoading.setValue(true);
        AppExecutors.getInstance().diskIO().execute(() -> {
            Recipe loadedRecipe = recipeDataUseCase.getRecipeByIdSync(recipeId);
            if (loadedRecipe != null) {
                recipe.postValue(loadedRecipe);
                isLiked.postValue(loadedRecipe.isLiked());
            } else {
                errorMessage.postValue("Рецепт не найден");
            }
            isLoading.postValue(false);
        });
    }
    
    /**
     * Переключает лайк для текущего рецепта
     */
    public void toggleLike() {
        Recipe currentRecipe = recipe.getValue();
        if (currentRecipe == null) return;

        boolean newLikeStatus = !isLiked.getValue();
        isLiked.setValue(newLikeStatus);
        currentRecipe.setLiked(newLikeStatus);
        
        recipeLikeUseCase.setLikeStatus(preferences.getUserId(), currentRecipe.getId(), newLikeStatus, errorMessage);
    }
    
    /**
     * Удаляет рецепт
     */
    public void deleteRecipe() {
        Recipe currentRecipe = recipe.getValue();
        if (currentRecipe == null) return;
        
        boolean hasPermission = currentRecipe.getUserId().equals(preferences.getUserId()) || permissionLevel == 2;
        if (!hasPermission) {
            errorMessage.setValue("У вас нет прав для удаления этого рецепта");
            return;
        }

        isLoading.setValue(true);
        recipeManagementUseCase.deleteRecipe(currentRecipe.getId(), new RecipeManagementUseCase.DeleteRecipeCallback() {
            @Override
            public void onDeleteSuccess() {
                isLoading.postValue(false);
                deleteSuccess.postValue(true);
            }

            @Override
            public void onDeleteFailure(String error) {
                isLoading.postValue(false);
                errorMessage.postValue(error);
            }
        });
    }
    
    // Геттеры для LiveData
    public LiveData<Recipe> getRecipe() {
        return recipe;
    }
    
    /**
     * Возвращает LiveData с информацией о том, лайкнут ли рецепт
     */
    public LiveData<Boolean> getIsLiked() {
        return isLiked;
    }
    

    
    /**
     * Возвращает LiveData с сообщениями об ошибках
     */
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * Возвращает LiveData с информацией об успешном удалении рецепта
     */
    public LiveData<Boolean> getDeleteSuccess() {
        return deleteSuccess;
    }
    
    /**
     * Очищает сообщение об ошибке после его показа
     */
    public void clearErrorMessage() {
        errorMessage.setValue(null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        recipeDataUseCase.clearResources();
        recipeLikeUseCase.clearResources();
        recipeManagementUseCase.clearResources();
    }
}
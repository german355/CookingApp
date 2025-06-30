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
import com.example.cooking.domain.usecases.UserPermissionUseCase;
import com.example.cooking.utils.AppExecutors;

/**
 * ViewModel для экрана детальной информации о рецепте
 * Использует единое UIState для управления всем состоянием экрана
 */
public class RecipeDetailViewModel extends AndroidViewModel {
    private static final String TAG = "RecipeDetailViewModel";
    
    private final RecipeDataUseCase recipeDataUseCase;
    private final RecipeLikeUseCase recipeLikeUseCase;
    private final RecipeManagementUseCase recipeManagementUseCase;
    private final UserPermissionUseCase userPermissionUseCase;
    private final MySharedPreferences preferences;

    // Единое состояние UI вместо множественных LiveData
    private final MutableLiveData<RecipeDetailUIState> uiState = new MutableLiveData<>(RecipeDetailUIState.INITIAL);
    
    private int recipeId;
    
    /**
     * Конструктор
     */
    public RecipeDetailViewModel(@NonNull Application application) {
        super(application);
        this.recipeDataUseCase = new RecipeDataUseCase(application);
        this.recipeLikeUseCase = new RecipeLikeUseCase(application);
        this.recipeManagementUseCase = new RecipeManagementUseCase(application);
        this.userPermissionUseCase = new UserPermissionUseCase(application);
        this.preferences = new MySharedPreferences(application);
    }
    
    /**
     * Загружает данные о рецепте
     */
    public void init(int recipeId, int permissionLevel) {
        this.recipeId = recipeId;
        loadRecipe(recipeId);
    }
    
    /**
     * Загружает рецепт и определяет права доступа
     */
    private void loadRecipe(int recipeId) {
        // Устанавливаем состояние загрузки
        uiState.setValue(RecipeDetailUIState.loading());
        
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                Recipe loadedRecipe = recipeDataUseCase.getRecipeByIdSync(recipeId);
                
                if (loadedRecipe != null) {
            
                    // Создаем новое состояние с рецептом и правами
                    RecipeDetailUIState newState = RecipeDetailUIState.withRecipe(loadedRecipe, userPermissionUseCase.canEditRecipe(loadedRecipe.getUserId()).hasPermission(), userPermissionUseCase.canDeleteRecipe(loadedRecipe.getUserId()).hasPermission());
                    uiState.postValue(newState);
                } else {
                    uiState.postValue(RecipeDetailUIState.error("Рецепт не найден"));
                }
            } catch (Exception e) {
                uiState.postValue(RecipeDetailUIState.error("Ошибка загрузки: " + e.getMessage()));
            }
        });
    }
    
    /**
     * Увеличивает количество порций
     */
    public void incrementPortion() {
        RecipeDetailUIState current = uiState.getValue();
        if (current != null) {
            uiState.setValue(current.incrementPortion());
        }
    }
    
    /**
     * Уменьшает количество порций (минимум 1)
     */
    public void decrementPortion() {
        RecipeDetailUIState current = uiState.getValue();
        if (current != null) {
            uiState.setValue(current.decrementPortion());
        }
    }
    
    /**
     * Переключает статус лайка для текущего рецепта
     */
    public void toggleLike() {
        RecipeDetailUIState current = uiState.getValue();
        if (current == null || !current.hasRecipe()) return;
        
        Recipe recipe = current.getRecipe();
        boolean newLikeStatus = !current.isLiked();
        
        // Сразу обновляем UI для отзывчивости
        uiState.setValue(current.withLikedStatus(newLikeStatus));
        
        // Обновляем рецепт и сохраняем на сервере
        recipe.setLiked(newLikeStatus);
        recipeLikeUseCase.setLikeStatus(
            preferences.getUserId(), 
            recipe.getId(), 
            newLikeStatus,
            new MutableLiveData<String>() {
                @Override
                public void setValue(String errorMessage) {
                    if (errorMessage != null) {
                        // В случае ошибки возвращаем предыдущее состояние
                        RecipeDetailUIState errorState = current.withLikedStatus(!newLikeStatus)
                                                                .withError(errorMessage);
                        uiState.postValue(errorState);
                    }
                }
            }
        );
    }
    
    /**
     * Удаляет рецепт
     */
    public void deleteRecipe() {
        RecipeDetailUIState current = uiState.getValue();
        if (current == null || !current.hasRecipe()) return;
        
        Recipe recipe = current.getRecipe();
        
        // Проверяем права доступа
        if (!current.canDelete()) {
            uiState.setValue(current.withError("У вас нет прав для удаления этого рецепта"));
            return;
        }
        
        // Устанавливаем состояние загрузки
        uiState.setValue(current.withLoading(true).withoutError());
        
        recipeManagementUseCase.deleteRecipe(recipe.getId(), new RecipeManagementUseCase.DeleteRecipeCallback() {
            @Override
            public void onDeleteSuccess() {
                RecipeDetailUIState successState = current.withLoading(false)
                                                          .withDeleteSuccess(true);
                uiState.postValue(successState);
            }

            @Override
            public void onDeleteFailure(String error) {
                RecipeDetailUIState errorState = current.withLoading(false)
                                                       .withError(error);
                uiState.postValue(errorState);
            }
        });
    }
    
    /**
     * Очищает сообщение об ошибке
     */
    public void clearError() {
        RecipeDetailUIState current = uiState.getValue();
        if (current != null && current.hasError()) {
            uiState.setValue(current.withoutError());
        }
    }
    
    /**
     * Перезагружает рецепт (например, после редактирования)
     */
    public void refreshRecipe() {
        if (recipeId > 0) {
            loadRecipe(recipeId);
        }
    }
    
    // Геттер для единого UI состояния
    public LiveData<RecipeDetailUIState> getUIState() {
        return uiState;
    }
    
    // Deprecated методы для обратной совместимости
    // TODO: Удалить после рефакторинга Activity
    
    /**
     * @deprecated Используйте getUIState() вместо этого
     */
    @Deprecated
    public LiveData<Recipe> getRecipe() {
        // Пустая LiveData для совместимости
        return new MutableLiveData<>(null);
    }
    
    /**
     * @deprecated Используйте getUIState() вместо этого
     */
    @Deprecated
    public LiveData<Boolean> getIsLiked() {
        return new MutableLiveData<>(false);
    }
    
    /**
     * @deprecated Используйте getUIState() вместо этого
     */
    @Deprecated
    public LiveData<String> getErrorMessage() {
        return new MutableLiveData<>(null);
    }
    
    /**
     * @deprecated Используйте getUIState() вместо этого
     */
    @Deprecated
    public LiveData<Boolean> getDeleteSuccess() {
        return new MutableLiveData<>(false);
    }
    
    /**
     * @deprecated Используйте clearError() вместо этого
     */
    @Deprecated
    public void clearErrorMessage() {
        clearError();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        recipeDataUseCase.clearResources();
        recipeLikeUseCase.clearResources();
        recipeManagementUseCase.clearResources();
    }
}
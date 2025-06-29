package com.example.cooking.ui.viewmodels.Recipe;

import android.app.Application;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;

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
 * ViewModel для экрана редактирования рецепта.
 */
public class EditRecipeViewModel extends BaseRecipeFormViewModel {
    private static final String TAG = "EditRecipeViewModel";

    private final UserPermissionUseCase userPermissionUseCase;
    private final EditRecipeState state;

    public EditRecipeViewModel(@NonNull Application application) {
        super(application);
        this.userPermissionUseCase = new UserPermissionUseCase(application);
        this.state = new EditRecipeState();
    }

    /**
     * Устанавливает начальные данные рецепта для редактирования из объекта Recipe.
     * @param recipeToEdit Объект Recipe, полученный из Intent.
     */
    public void setRecipeData(@NonNull Recipe recipeToEdit) {
        state.setRecipeId(recipeToEdit.getId());
        state.setRecipeOwnerId(recipeToEdit.getUserId());
        state.setTitle(recipeToEdit.getTitle() != null ? recipeToEdit.getTitle() : "");
        state.setPhotoUrl(recipeToEdit.getPhoto_url()); // Сохраняем исходный URL
        state.setImageBytes(null); // Сбрасываем новое/выбранное изображение
        state.setSelectedImageUri(null); // Сбрасываем Uri
        state.setImageChanged(false);

        // Получаем списки напрямую
        List<Ingredient> initialIngredients = recipeToEdit.getIngredients();
        List<Step> initialSteps = recipeToEdit.getSteps();

        // Убедимся, что списки не null и содержат хотя бы один элемент
        if (initialIngredients == null || initialIngredients.isEmpty()) {
            RecipeFormUseCase.FormInitResult formResult = recipeFormUseCase.initializeForm();
            initialIngredients = formResult.getIngredients();
            if (initialSteps == null || initialSteps.isEmpty()) {
                initialSteps = formResult.getSteps();
            }
        }
        if (initialSteps == null || initialSteps.isEmpty()) {
            RecipeFormUseCase.FormInitResult formResult = recipeFormUseCase.initializeForm();
            initialSteps = formResult.getSteps();
        }

        // Устанавливаем списки в LiveData
        state.setIngredients(new ArrayList<>(initialIngredients)); // Используем копию
        state.setSteps(new ArrayList<>(initialSteps)); // Используем копию

        Log.d(TAG, "setRecipeData: Установлены данные для рецепта ID " + state.getRecipeIdValue() + ", Ингредиентов: " + state.getIngredientsCount() + ", Шагов: " + state.getStepsCount());
    }

    // --- Геттеры для LiveData полей ---
    public LiveData<String> getTitle() {
        return state.getTitle();
    }

    public LiveData<List<Ingredient>> getIngredients() {
        return state.getIngredients();
    }

    public LiveData<List<Step>> getSteps() {
        return state.getSteps();
    }

    public LiveData<String> getPhotoUrl() {
        return state.getPhotoUrl();
    }

    public LiveData<byte[]> getImageBytes() {
        return state.getImageBytes();
    }

    public LiveData<Boolean> getIsSaving() {
        return state.getIsSaving();
    }

    public LiveData<Boolean> getSaveResult() {
        return state.getSaveResult();
    }

    public LiveData<String> getErrorMessage() {
        return state.getErrorMessage();
    }

    /**
     * Загружает изображение по URL (обычно для отображения существующего фото)
     */
    public void loadImageFromUrl(String url) {
        if (url == null || url.isEmpty() || state.getImageBytesValue() != null) {
            Log.d(TAG, "loadImageFromUrl: URL пуст или уже есть новое изображение, загрузка пропущена.");
            return;
        }
        
        disposables.add(
            recipeFormUseCase.loadImageFromUrl(url)
                .subscribe(
                    imageBytes -> {
                        if (state.getImageBytesValue() == null && state.getSelectedImageUri() == null) {
                            state.setImageBytes(imageBytes);
                            state.setImageChanged(false);
                            Log.d(TAG, "loadImageFromUrl: Исходное изображение загружено");
                        }
                    },
                    error -> {
                        Log.e(TAG, "loadImageFromUrl: " + error.getMessage());
                    }
                )
        );
    }



    @Override
    protected void onCleared() {
        super.onCleared();

    }

    
    /**
     * Очищает результат сохранения
     */
    public void clearSaveResult() {
        state.clearSaveResult();
    }
    
    /**
     * Очищает сообщение об ошибке
     */
    public void clearErrorMessage() {
        state.clearErrorMessage();
    }
    
    /**
     * Сохраняет (обновляет) рецепт
     */
    public void saveRecipe() {
        // Проверка прав пользователя: может редактировать только свои рецепты или админ
        if (state.getRecipeOwnerIdValue() != null) {
            UserPermissionUseCase.PermissionResult permissionResult = userPermissionUseCase.canEditRecipe(state.getRecipeOwnerIdValue());
            if (!permissionResult.hasPermission()) {
                state.setErrorMessage(permissionResult.getReason());
                state.setSaveResult(false);
                return;
            }
        }
        
        // Проверяем подключение к интернету
        if (!recipeManagementUseCase.isNetworkAvailable()) {
            state.setErrorMessage(getApplication().getString(R.string.error_no_internet_connection));
            return;
        }

        // Получаем текущие данные
        Integer currentRecipeId = state.getRecipeIdValue();
        String currentTitle = state.getTitleValue();
        List<Ingredient> currentIngredients = state.getIngredientsValue();
        List<Step> currentSteps = state.getStepsValue();
        String currentPhotoUrl = state.getPhotoUrlValue();
        byte[] currentImageBytes = state.getImageBytesValue();

        // Проверяем, что у нас есть все необходимые данные
        if (currentRecipeId == null || currentTitle == null || currentIngredients == null || currentSteps == null) {
            state.setErrorMessage(getApplication().getString(R.string.error_missing_data));
            return;
        }

        // Проверяем валидность данных
        if (currentTitle.trim().isEmpty()) {
            state.setErrorMessage(getApplication().getString(R.string.error_enter_recipe_name));
            return;
        }
        
        if (currentIngredients.isEmpty()) {
            state.setErrorMessage(getApplication().getString(R.string.error_add_at_least_one_ingredient));
            return;
        }
        
        if (currentSteps.isEmpty()) {
            state.setErrorMessage(getApplication().getString(R.string.error_add_at_least_one_step));
            return;
        }

        // Устанавливаем флаг сохранения
        state.setIsSaving(true);

        // Используем RecipeFormUseCase для создания объекта рецепта и сохранения
        disposables.add(
            recipeFormUseCase.buildRecipe(
                currentTitle,
                currentIngredients,
                currentSteps,
                state.getRecipeOwnerIdValue(), // userId оригинального создателя
                currentRecipeId,
                currentPhotoUrl
            )
            .subscribe(
                updatedRecipe -> {
                    Log.d(TAG, "saveRecipe: Recipe построен, userId = " + updatedRecipe.getUserId());
                    
                    // Обновляем рецепт на сервере
                    recipeManagementUseCase.updateRecipe(
                        updatedRecipe,
                        currentImageBytes,
                        new RecipeManagementUseCase.RecipeSaveCallback() {
                            @Override
                            public void onSuccess(com.example.cooking.network.models.GeneralServerResponse response, Recipe savedRecipe) {
                                AppExecutors.getInstance().mainThread().execute(() -> {
                                    // Обновляем локальное состояние данными из сохраненного рецепта
                                    if (savedRecipe != null) {
                                        // Обновляем URL фото если получен новый с сервера
                                        if (response != null && response.getPhotoUrl() != null) {
                                            state.setPhotoUrl(response.getPhotoUrl());
                                            savedRecipe.setPhoto_url(response.getPhotoUrl());
                                        }
                                        
                                        // Обновляем ингредиенты и шаги из сохраненного рецепта
                                        if (savedRecipe.getIngredients() != null) {
                                            state.setIngredients(new ArrayList<>(savedRecipe.getIngredients()));
                                        }
                                        if (savedRecipe.getSteps() != null) {
                                            state.setSteps(new ArrayList<>(savedRecipe.getSteps()));
                                        }
                                        
                                        // Обновляем заголовок если он изменился
                                        if (savedRecipe.getTitle() != null) {
                                            state.setTitle(savedRecipe.getTitle());
                                        }
                                        
                                        Log.d(TAG, "Локальное состояние обновлено: ингредиентов=" + 
                                              (savedRecipe.getIngredients() != null ? savedRecipe.getIngredients().size() : 0) + 
                                              ", шагов=" + (savedRecipe.getSteps() != null ? savedRecipe.getSteps().size() : 0));
                                    }
                                    
                                    state.setIsSaving(false); // Скрываем индикатор только после успешного сохранения
                                    state.setSaveResult(true);
                                });
                                Log.d(TAG, "Рецепт успешно обновлен");
                            }

                            @Override
                            public void onFailure(String error, com.example.cooking.network.models.GeneralServerResponse errorResponse) {
                                AppExecutors.getInstance().mainThread().execute(() -> {
                                    state.setIsSaving(false); // Скрываем индикатор при ошибке
                                    
                                    // Специальная обработка ошибок модерации
                                    if (error != null && error.startsWith("Модерация:")) {
                                        String moderationMessage = error.substring("Модерация:".length()).trim();
                                        if (moderationMessage.isEmpty()) {
                                            state.setErrorMessage(getApplication().getString(R.string.moderation_failed_generic));
                                        } else {
                                            state.setErrorMessage(getApplication().getString(R.string.moderation_failed, moderationMessage));
                                        }
                                        Log.w(TAG,  moderationMessage);
                                        return;
                                    }
                                    
                                    // Для других ошибок используем стандартную обработку
                                    String detailedError = error;
                                    if (errorResponse != null && errorResponse.getMessage() != null) {
                                        detailedError += " (Детали: " + errorResponse.getMessage() + ")";
                                    }
                                    
                                    state.setErrorMessage(detailedError);
                                });
                                Log.e(TAG, "Ошибка при обновлении рецепта: " + error);
                            }
                        }
                    );
                },
                error -> {
                    Log.e(TAG, "Ошибка при построении рецепта", error);
                    state.setIsSaving(false); // Скрываем индикатор при ошибке построения рецепта
                    state.setErrorMessage(error.getMessage());
                }
            )
        );
    }
    
    // === РЕАЛИЗАЦИЯ АБСТРАКТНЫХ МЕТОДОВ ===
    
    @Override
    protected List<Ingredient> getCurrentIngredients() {
        return state.getIngredientsValue();
    }
    
    @Override
    protected void setCurrentIngredients(List<Ingredient> ingredients) {
        state.setIngredients(ingredients);
    }
    
    @Override
    protected List<Step> getCurrentSteps() {
        return state.getStepsValue();
    }
    
    @Override
    protected void setCurrentSteps(List<Step> steps) {
        state.setSteps(steps);
    }
    
    @Override
    protected String getCurrentTitle() {
        return state.getTitleValue();
    }
    
    @Override
    protected void setCurrentTitle(String title) {
        state.setTitle(title);
    }
    
    @Override
    protected void setErrorMessage(String errorMessage) {
        state.setErrorMessage(errorMessage);
    }
    
    @Override
    protected void setTitleError(String error) {
        // В EditRecipeState нет отдельного titleError, используем общий errorMessage
        state.setErrorMessage(error);
    }
    
    @Override
    protected void setIngredientsError(String error) {
        // В EditRecipeState нет отдельных ошибок для списков, используем общий errorMessage
        state.setErrorMessage(error);
    }
    
    @Override
    protected void setStepsError(String error) {
        // В EditRecipeState нет отдельных ошибок для списков, используем общий errorMessage
        state.setErrorMessage(error);
    }
    
    @Override
    protected void setImageProcessing(boolean processing) {
        state.setIsSaving(processing);
    }
    
    @Override
    protected void onImageProcessed(byte[] imageBytes) {
        state.setImageBytes(imageBytes);
        state.setImageChanged(true);
        state.setPhotoUrl(null); // Сбрасываем старый URL
        Log.d(TAG, "Изображение обработано");
    }
    
    @Override
    protected void onImageError(String error) {
        state.setErrorMessage(error);
        state.setImageBytes(null);
        state.setImageChanged(false);
    }
}

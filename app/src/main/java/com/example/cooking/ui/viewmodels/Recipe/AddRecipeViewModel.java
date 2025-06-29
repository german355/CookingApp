package com.example.cooking.ui.viewmodels.Recipe;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cooking.domain.entities.Ingredient;
import com.example.cooking.domain.entities.Recipe;
import com.example.cooking.domain.entities.Step;
import com.example.cooking.domain.usecases.RecipeFormUseCase;
import com.example.cooking.domain.usecases.RecipeManagementUseCase;
import com.example.cooking.R;

import java.util.ArrayList;
import java.util.List;

/**
 * ViewModel для экрана добавления рецепта
 * Управляет бизнес-логикой создания нового рецепта
 */
public class AddRecipeViewModel extends BaseRecipeFormViewModel {
    private static final String TAG = "AddRecipeViewModel";
    
    // LiveData для UI состояний
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saveSuccess = new MutableLiveData<>(false);
    
    // LiveData для валидации полей
    private final MutableLiveData<String> titleError = new MutableLiveData<>();
    private final MutableLiveData<String> ingredientsListError = new MutableLiveData<>();
    private final MutableLiveData<String> stepsListError = new MutableLiveData<>();
    private final MutableLiveData<String> imageError = new MutableLiveData<>();
    
    // Данные рецепта
    private final MutableLiveData<String> title = new MutableLiveData<>("");
    private final MutableLiveData<List<Ingredient>> ingredients = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<List<Step>> steps = new MutableLiveData<>(new ArrayList<>());
    private byte[] imageBytes = null;
    
    // Поля специфичные для AddRecipeViewModel

    public AddRecipeViewModel(@NonNull Application application) {
        super(application);

        // Инициализируем списки через RecipeFormUseCase
        RecipeFormUseCase.FormInitResult formResult = recipeFormUseCase.initializeForm();
        ingredients.setValue(formResult.getIngredients());
        steps.setValue(formResult.getSteps());
    }
    
    /**
     * Сохраняет новый рецепт
     */
    public void saveRecipe() {
        // Проверяем подключение к интернету
        if (!recipeManagementUseCase.isNetworkAvailable()) {
            errorMessage.setValue(getApplication().getString(R.string.error_no_internet_connection));
            return;
        }
        
        // Получаем ID пользователя
        String userId = preferences.getString("userId", "99");
        Log.d(TAG, "Saving recipe for userId: " + userId);
        
        // Показываем индикатор загрузки
        isLoading.setValue(true);
        
        // Сначала валидация, затем построение рецепта и сохранение - красивая RxJava цепочка!
        disposables.add(
            recipeFormUseCase.validateRecipeForm(
                title.getValue(),
                ingredients.getValue(),
                steps.getValue()
            )
            .flatMap(validationResult -> {
                if (!validationResult.isValid()) {
                    return io.reactivex.rxjava3.core.Single.error(new Exception(validationResult.getErrorMessage()));
                }
                
                // Валидация прошла успешно, строим рецепт
                return recipeFormUseCase.buildRecipe(
                    title.getValue(),
                    ingredients.getValue(),
                    steps.getValue(),
                    userId,
                    null, // новый рецепт, ID нет
                    null  // новый рецепт, URL нет
                );
            })
            .doFinally(() -> isLoading.setValue(false))
            .subscribe(
                newRecipe -> {
                    Log.d(TAG, "Recipe построен: " + newRecipe.getTitle());
                    
                    // Сохраняем рецепт через RecipeManagementUseCase
                    recipeManagementUseCase.saveRecipe(
                        newRecipe,
                        imageBytes,
                        new RecipeManagementUseCase.RecipeSaveCallback() {
                            @Override
                            public void onSuccess(com.example.cooking.network.models.GeneralServerResponse response, Recipe savedRecipe) {
                                if (response.isSuccess() && savedRecipe != null) {
                                    saveSuccess.postValue(true);
                                    Log.d(TAG, "Рецепт успешно сохранен с ID: " + savedRecipe.getId());
                                } else {
                                    String errorMsg = getApplication().getString(R.string.error_saving_recipe);
                                    if (response != null && response.getMessage() != null) {
                                        errorMsg += ": " + response.getMessage();
                                    }
                                    errorMessage.postValue(errorMsg);
                                    Log.e(TAG, errorMsg);
                                }
                            }

                            @Override
                            public void onFailure(String error, com.example.cooking.network.models.GeneralServerResponse errorResponse) {
                                String detailedError = error;
                                if (errorResponse != null && errorResponse.getMessage() != null) {
                                    detailedError += " (Сервер: " + errorResponse.getMessage() + ")";
                                }

                                // Если ошибка связана с дублированием рецепта, считаем это успешным сохранением
                                if (detailedError != null && (detailedError.toLowerCase().contains("дубли") || 
                                    detailedError.toLowerCase().contains("уже существует") || 
                                    detailedError.toLowerCase().contains("повторно") || 
                                    detailedError.toLowerCase().contains("duplicate") || 
                                    detailedError.toLowerCase().contains("already exists") || 
                                    detailedError.toLowerCase().contains("already added"))) {
                                    saveSuccess.postValue(true);
                                    Log.d(TAG, "Рецепт уже существует или был отправлен повторно: " + detailedError);
                                } else {
                                    errorMessage.postValue(detailedError);
                                    Log.e(TAG, "Ошибка при сохранении рецепта: " + detailedError);
                                }
                            }
                        }
                    );
                },
                error -> {
                    Log.e(TAG, "Ошибка валидации или построения рецепта", error);
                    errorMessage.setValue(error.getMessage());
                }
            )
        );
    }
    

    

    

    



    

    
    // Геттеры для LiveData
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<Boolean> getSaveSuccess() {
        return saveSuccess;
    }
    
    public LiveData<String> getTitleError() {
        return titleError;
    }
    
    public LiveData<String> getIngredientsListError() {
        return ingredientsListError;
    }
    
    public LiveData<String> getStepsListError() {
        return stepsListError;
    }
    
    public LiveData<String> getImageError() {
        return imageError;
    }
    
    public LiveData<List<Ingredient>> getIngredients() {
        return ingredients;
    }
    
    public LiveData<List<Step>> getSteps() {
        return steps;
    }
    
    public LiveData<String> getTitle() {
        return title;
    }
    


    /**
     * Возвращает true, если изображение было выбрано и обработано.
     */
    public boolean hasImage() {
        return imageBytes != null && imageBytes.length > 0;
    }
    

    

    
    // === РЕАЛИЗАЦИЯ АБСТРАКТНЫХ МЕТОДОВ ===
    
    @Override
    protected List<Ingredient> getCurrentIngredients() {
        return ingredients.getValue();
    }
    
    @Override
    protected void setCurrentIngredients(List<Ingredient> ingredients) {
        this.ingredients.setValue(ingredients);
    }
    
    @Override
    protected List<Step> getCurrentSteps() {
        return steps.getValue();
    }
    
    @Override
    protected void setCurrentSteps(List<Step> steps) {
        this.steps.setValue(steps);
    }
    
    @Override
    protected String getCurrentTitle() {
        return title.getValue();
    }
    
    @Override
    protected void setCurrentTitle(String title) {
        this.title.setValue(title);
    }
    
    @Override
    protected void setErrorMessage(String errorMessage) {
        this.errorMessage.setValue(errorMessage);
    }
    
    @Override
    protected void setTitleError(String error) {
        this.titleError.setValue(error);
    }
    
    @Override
    protected void setIngredientsError(String error) {
        this.ingredientsListError.setValue(error);
    }
    
    @Override
    protected void setStepsError(String error) {
        this.stepsListError.setValue(error);
    }
    
    @Override
    protected void setImageProcessing(boolean processing) {
        // Используем isLoading для индикации обработки изображения
        this.isLoading.setValue(processing);
    }
    
    @Override
    protected void onImageProcessed(byte[] imageBytes) {
        this.imageBytes = imageBytes;
        this.imageError.setValue(null);
        Log.d(TAG, "Изображение обработано");
    }
    
    @Override
    protected void onImageError(String error) {
        this.imageBytes = null;
        this.imageError.setValue(error);
    }
}
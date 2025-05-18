package com.example.cooking.ui.viewmodels;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cooking.Recipe.Recipe;
import com.example.cooking.data.repositories.LikedRecipesRepository;
import com.example.cooking.data.repositories.RecipeLocalRepository;
import com.example.cooking.network.services.RecipeService;
import com.example.cooking.utils.MySharedPreferences;

/**
 * ViewModel для экрана детальной информации о рецепте
 */
public class RecipeDetailViewModel extends AndroidViewModel {
    private static final String TAG = "RecipeDetailViewModel";
    
    private final RecipeLocalRepository recipeRepository;
    private final LikedRecipesRepository likedRecipesRepository;
    private final RecipeService recipeService;
    private final SharedRecipeViewModel sharedRecipeViewModel;
    
    private final MutableLiveData<Recipe> recipe = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLiked = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> deleteSuccess = new MutableLiveData<>(false);
    
    private int recipeId;
    private int userPermission;
    
    private final java.util.concurrent.ExecutorService executor = 
            java.util.concurrent.Executors.newSingleThreadExecutor();
    
    /**
     * Конструктор
     */
    public RecipeDetailViewModel(@NonNull Application application) {
        super(application);
        recipeRepository = new RecipeLocalRepository(application);
        likedRecipesRepository = new LikedRecipesRepository(application);
        recipeService = new RecipeService(application);
        sharedRecipeViewModel = new SharedRecipeViewModel(application);
    }
    
    /**
     * Загружает данные о рецепте
     */
    public void init(int recipeId, int permission) {
        this.recipeId = recipeId;
        this.userPermission = permission;
        loadRecipe();
        checkIfLiked();
    }
    
    /**
     * Загружает рецепт из локальной базы данных
     */
    private void loadRecipe() {
        isLoading.setValue(true);
        
        // Запрашиваем рецепт и устанавливаем его в recipe LiveData после получения
        executor.execute(() -> {
            Recipe loadedRecipe = recipeRepository.getRecipeByIdSync(recipeId);
            isLoading.postValue(false);
            if (loadedRecipe != null) {
                recipe.postValue(loadedRecipe);
            } else {
                errorMessage.postValue("Не удалось загрузить рецепт");
            }
        });
    }
    
    /**
     * Проверяет, добавлен ли рецепт в избранное
     */
    private void checkIfLiked() {
        // Получаем userId текущего пользователя
        MySharedPreferences preferences = new MySharedPreferences(getApplication());
        String userId = preferences.getString("userId", "0");
        
        if (userId.equals("0")) {
            isLiked.postValue(false);
            return;
        }
        
        likedRecipesRepository.isRecipeLiked(recipeId, userId).observeForever(liked -> {
            isLiked.postValue(liked != null && liked);
        });
    }
    
    /**
     * Переключает состояние "избранное" для рецепта
     */
    public void toggleLike() {
        // Получаем userId текущего пользователя
        MySharedPreferences preferences = new MySharedPreferences(getApplication());
        String userId = preferences.getString("userId", "0");
        
        if (userId.equals("0")) {
            errorMessage.setValue("Необходимо войти в систему");
            return;
        }
        
        // Получаем текущее состояние
        Boolean currentLiked = isLiked.getValue();
        if (currentLiked == null) {
            return;
        }
        
        // Обновляем в SharedRecipeViewModel для синхронизации между фрагментами
        sharedRecipeViewModel.toggleLike(userId, recipeId);
        
        // Обновляем локальное состояние
        isLiked.setValue(!currentLiked);
    }
    
    /**
     * Удаляет рецепт
     */
    public void deleteRecipe() {
        if (!isNetworkAvailable()) {
            errorMessage.setValue("Отсутствует подключение к интернету");
            return;
        }
        
        // Получаем userId текущего пользователя
        MySharedPreferences preferences = new MySharedPreferences(getApplication());
        String userId = preferences.getString("userId", "0");
        
        isLoading.setValue(true);
        
        recipeService.deleteRecipe(
            recipeId,
            userId,
            userPermission,
            new RecipeService.DeleteRecipeCallback() {
                @Override
                public void onDeleteSuccess() {
                    isLoading.postValue(false);
                    deleteSuccess.postValue(true);
                    // Обновляем список рецептов в SharedRecipeViewModel после удаления
                    sharedRecipeViewModel.refreshRecipes();
                }
                
                @Override
                public void onDeleteFailure(String error) {
                    isLoading.postValue(false);
                    errorMessage.postValue(error);
                }
            }
        );
    }
    
    /**
     * Проверяет подключение к интернету
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) 
                getApplication().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
    
    // Геттеры для LiveData
    public LiveData<Recipe> getRecipe() {
        return recipe;
    }
    
    public LiveData<Boolean> getIsLiked() {
        return isLiked;
    }
    
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    public LiveData<Boolean> getDeleteSuccess() {
        return deleteSuccess;
    }
}
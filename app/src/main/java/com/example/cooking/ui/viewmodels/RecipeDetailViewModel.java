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
import androidx.lifecycle.LifecycleOwner;

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
    
    private final RecipeLocalRepository localRepository;
    private final LikedRecipesRepository likedRecipesRepository;
    private final RecipeService recipeService;
    private final SharedRecipeViewModel sharedRecipeViewModel;
    
    private final MutableLiveData<Recipe> recipe = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLikedLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> deleteSuccess = new MutableLiveData<>(false);
    
    private int recipeId;
    private int userPermission;
    private String userId;
    
    private final java.util.concurrent.ExecutorService executor = 
            java.util.concurrent.Executors.newSingleThreadExecutor();
    
    /**
     * Конструктор
     */
    public RecipeDetailViewModel(@NonNull Application application) {
        super(application);
        localRepository = new RecipeLocalRepository(application);
        likedRecipesRepository = new LikedRecipesRepository(application);
        recipeService = new RecipeService(application);
        sharedRecipeViewModel = new SharedRecipeViewModel(application);
        // Получаем userId текущего пользователя
        MySharedPreferences preferences = new MySharedPreferences(application);
        userId = preferences.getString("userId", "0");
    }
    
    /**
     * Загружает данные о рецепте
     */
    public void init(int recipeId, int permission) {
        this.recipeId = recipeId;
        this.userPermission = permission;
        loadRecipe();
        // Инициализируем статус лайка через observeLikeStatus при первой загрузке
    }
    
    /**
     * Загружает рецепт из локальной базы данных
     */
    private void loadRecipe() {
        isLoading.setValue(true);
        
        // Запрашиваем рецепт и устанавливаем его в recipe LiveData после получения
        executor.execute(() -> {
            Recipe loadedRecipe = localRepository.getRecipeByIdSync(recipeId);
            isLoading.postValue(false);
            if (loadedRecipe != null) {
                recipe.postValue(loadedRecipe);
            } else {
                errorMessage.postValue("Не удалось загрузить рецепт");
            }
        });
    }
    
    /**
     * Обновляет лайк для текущего рецепта
     */
    public void toggleLike() {
        if (userId.equals("0")) {
            errorMessage.setValue("Чтобы поставить лайк, необходимо войти в аккаунт");
            return;
        }
        
        // Получаем текущее состояние
        Boolean currentLiked = isLikedLiveData.getValue();
        if (currentLiked == null) {
            return;
        }
        
        // Обновляем локальное состояние
        isLikedLiveData.setValue(!currentLiked);
        
        // Обновляем в репозитории
        if (!currentLiked) {
            likedRecipesRepository.insertLikedRecipeLocal(recipeId);
        } else {
            likedRecipesRepository.deleteLikedRecipeLocal(recipeId);
        }
    }
    
    /**
     * Удаляет рецепт
     */
    public void deleteRecipe() {
        if (!isNetworkAvailable()) {
            errorMessage.setValue("Отсутствует подключение к интернету");
            return;
        }
        
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
    
    /**
     * Наблюдает за статусом лайка для текущего рецепта
     *
     * @param lifecycleOwner владелец жизненного цикла для наблюдения
     */
    public void observeLikeStatus(LifecycleOwner lifecycleOwner) {
        // Получаем текущий статус лайка
        boolean isCurrentlyLiked = likedRecipesRepository.isRecipeLikedLocalSync(recipeId);
        isLikedLiveData.setValue(isCurrentlyLiked);
        
        // Меняем механизм: просто наблюдаем за изменениями в базе данных через LiveData
        localRepository.getRecipeById(recipeId).observe(lifecycleOwner, recipe -> {
            if (recipe != null) {
                boolean isLiked = recipe.isLiked();
                isLikedLiveData.setValue(isLiked);
                Log.d(TAG, "RecipeDetailViewModel: Обновляем статус лайка. recipeId=" + recipeId + " isLiked=" + isLiked);
            }
        });
    }
    
    // Геттеры для LiveData
    public LiveData<Recipe> getRecipe() {
        return recipe;
    }
    
    public LiveData<Boolean> getIsLiked() {
        return isLikedLiveData;
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
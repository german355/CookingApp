package com.example.cooking.domain.usecases;

import android.app.Application;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.cooking.domain.entities.Recipe;
import com.example.cooking.data.repositories.UnifiedRecipeRepository;
import com.example.cooking.network.utils.Resource;

import java.util.List;

/**
 * Use Case для операций с данными рецептов
 * Отвечает за загрузку, синхронизацию и получение данных рецептов
 */
public class RecipeDataUseCase {
    private static final String TAG = "RecipeDataUseCase";
    
    private final UnifiedRecipeRepository repository;
    private final Application application;
    private final android.net.ConnectivityManager connectivityManager;
    
    public RecipeDataUseCase(Application application) {
        this.application = application;
        this.repository = UnifiedRecipeRepository.getInstance(application);
        this.connectivityManager = (android.net.ConnectivityManager) 
            application.getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
    }
    
    /**
     * Получает LiveData со всеми рецептами из локальной базы данных
     */
    public LiveData<List<Recipe>> getAllRecipesLocalLiveData() {
        return repository.getAllRecipesLocal();
    }
    
    /**
     * Получает все рецепты синхронно (для использования в фоновых потоках)
     */
    public List<Recipe> getAllRecipesSync() {
        return repository.getAllRecipesSync();
    }
    
    /**
     * Обновляет рецепты с сервера
     */
    public void refreshRecipes(MutableLiveData<Boolean> isRefreshingLiveData, 
                            MutableLiveData<String> errorMessageLiveData,
                            MutableLiveData<Resource<List<Recipe>>> recipesLiveData,
                            Runnable onComplete) {
        
        isRefreshingLiveData.postValue(true);
        
        // Выполняем синхронизацию в фоновом потоке
        repository.syncWithRemoteData(recipesLiveData, errorMessageLiveData);

        // Устанавливаем задержку для корректного отображения состояния загрузки
        android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());
        handler.postDelayed(() -> {
            isRefreshingLiveData.postValue(false);
            if (onComplete != null) {
                onComplete.run();
            }
        }, 500); // Минимальная задержка для показа индикатора загрузки
    }
    

    
    /**
     * Очищает ресурсы
     */
    public void clearResources() {
        repository.clearDisposables();
    }

    public Recipe getRecipeByIdSync(int recipeId) {
        return repository.getRecipeByIdSync(recipeId);
    }
} 
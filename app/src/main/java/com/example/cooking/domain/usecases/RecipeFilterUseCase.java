package com.example.cooking.domain.usecases;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import androidx.lifecycle.MutableLiveData;
import com.example.cooking.domain.entities.Recipe;
import com.example.cooking.data.repositories.UnifiedRecipeRepository;
import com.example.cooking.network.utils.Resource;

import java.util.List;

/**
 * Use Case для операций фильтрации рецептов по категориям
 * Отвечает за фильтрацию рецептов по meal_type и food_type
 */
public class RecipeFilterUseCase {
    private static final String TAG = "RecipeFilterUseCase";
    
    private final UnifiedRecipeRepository repository;
    private final RecipeDataUseCase recipeDataUseCase;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    
    public RecipeFilterUseCase(Application application) {
        this.repository = UnifiedRecipeRepository.getInstance(application);
        this.recipeDataUseCase = new RecipeDataUseCase(application);
    }
    
    /**
     * Выполняет фильтрацию рецептов по категории с опциональным автообновлением
     */
    public void filterRecipesByCategory(String filterKey, String filterType, 
                                      MutableLiveData<List<Recipe>> filteredResultsLiveData,
                                      MutableLiveData<String> errorMessageLiveData) {
        filterRecipesByCategory(filterKey, filterType, filteredResultsLiveData, errorMessageLiveData, null);
    }
    
    /**
     * Выполняет фильтрацию рецептов по категории с автообновлением если данных нет
     */
    public void filterRecipesByCategory(String filterKey, String filterType, 
                                      MutableLiveData<List<Recipe>> filteredResultsLiveData,
                                      MutableLiveData<String> errorMessageLiveData,
                                      MutableLiveData<Boolean> isRefreshingLiveData) {
        
        // Создаем временную MutableLiveData для получения результатов
        MutableLiveData<List<Recipe>> tempResultsLiveData = new MutableLiveData<List<Recipe>>() {
            @Override
            public void postValue(List<Recipe> recipes) {
                // Переключаемся на main thread для безопасной обработки
                mainThreadHandler.post(() -> {
                    handleFilterResults(recipes, filterKey, filterType, filteredResultsLiveData, 
                                      errorMessageLiveData, isRefreshingLiveData);
                });
            }
        };
        
        repository.filterRecipesByCategory(filterKey, filterType, tempResultsLiveData);
    }
    
    /**
     * Обрабатывает результаты фильтрации и определяет нужно ли обновление
     */
    private void handleFilterResults(List<Recipe> recipes, String filterKey, String filterType,
                                   MutableLiveData<List<Recipe>> filteredResultsLiveData,
                                   MutableLiveData<String> errorMessageLiveData,
                                   MutableLiveData<Boolean> isRefreshingLiveData) {
        
        if (recipes == null || recipes.isEmpty()) {
            if (isRefreshingLiveData != null) {
                refreshDataAndFilter(filterKey, filterType, filteredResultsLiveData,
                                   errorMessageLiveData, isRefreshingLiveData);
            } else {
                filteredResultsLiveData.postValue(recipes);
            }
        } else {
            filteredResultsLiveData.postValue(recipes);
            if (isRefreshingLiveData != null) {
                isRefreshingLiveData.postValue(false);
            }
        }
    }
    
    /**
     * Обновляет данные с сервера и повторяет фильтрацию
     */
    public void refreshDataAndFilter(String filterKey, String filterType,
                                   MutableLiveData<List<Recipe>> filteredResultsLiveData,
                                   MutableLiveData<String> errorMessageLiveData,
                                   MutableLiveData<Boolean> isRefreshingLiveData) {
        
        MutableLiveData<Resource<List<Recipe>>> dummyLiveData = new MutableLiveData<>();
        recipeDataUseCase.refreshRecipes(isRefreshingLiveData, errorMessageLiveData, dummyLiveData, () -> {
            filterRecipesByCategory(filterKey, filterType, filteredResultsLiveData, errorMessageLiveData);
        });
    }
    

    
    /**
     * Очищает ресурсы
     */
    public void clearResources() {
        repository.clearDisposables();
        recipeDataUseCase.clearResources();
    }
} 
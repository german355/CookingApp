package com.example.cooking.domain.usecases;

import android.app.Application;
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
        
        repository.filterRecipesByCategory(filterKey, filterType, new MutableLiveData<List<Recipe>>() {
            @Override
            public void setValue(List<Recipe> recipes) {
                super.setValue(recipes);
                handleFilterResults(recipes, filterKey, filterType, filteredResultsLiveData, 
                                  errorMessageLiveData, isRefreshingLiveData);
            }
            
            @Override 
            public void postValue(List<Recipe> recipes) {
                setValue(recipes);
            }
        });
    }
    
    /**
     * Обрабатывает результаты фильтрации и определяет нужно ли обновление
     */
    private void handleFilterResults(List<Recipe> recipes, String filterKey, String filterType,
                                   MutableLiveData<List<Recipe>> filteredResultsLiveData,
                                   MutableLiveData<String> errorMessageLiveData,
                                   MutableLiveData<Boolean> isRefreshingLiveData) {
        
        if (recipes == null || recipes.isEmpty()) {
            // Локальных данных нет
            if (isRefreshingLiveData != null) {
                // Есть индикатор загрузки - загружаем с сервера и повторяем фильтрацию
                refreshDataAndFilter(filterKey, filterType, filteredResultsLiveData, 
                                   errorMessageLiveData, isRefreshingLiveData);
            } else {
                // Без автообновления - просто возвращаем пустой результат
                filteredResultsLiveData.setValue(recipes);
            }
        } else {
            // Данные есть - возвращаем результат
            filteredResultsLiveData.setValue(recipes);
            if (isRefreshingLiveData != null) {
                isRefreshingLiveData.setValue(false);
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
            // После обновления - повторяем фильтрацию БЕЗ автообновления (избегаем рекурсии)
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
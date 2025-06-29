package com.example.cooking.domain.usecases;

import android.app.Application;
import android.util.Log;
import androidx.lifecycle.MutableLiveData;
import com.example.cooking.domain.entities.Recipe;
import com.example.cooking.data.repositories.UnifiedRecipeRepository;
import com.example.cooking.utils.RecipeSearchService;

import java.util.List;

/**
 * Use Case для операций поиска рецептов
 * Отвечает за все виды поиска: умный поиск и локальный поиск
 */
public class RecipeSearchUseCase {
    private static final String TAG = "RecipeSearchUseCase";
    
    private final UnifiedRecipeRepository repository;
    private final Application application;
    
    public RecipeSearchUseCase(Application application) {
        this.application = application;
        this.repository = UnifiedRecipeRepository.getInstance(application);
        Log.d(TAG, "RecipeSearchUseCase создан");
    }
    
    /**
     * Выполняет поиск рецептов с возможностью использования умного поиска
     */
    public void searchRecipes(String query, boolean smartSearchEnabled, 
                              MutableLiveData<List<Recipe>> searchResultsLiveData,
                              MutableLiveData<String> errorMessageLiveData,
                              MutableLiveData<Boolean> isRefreshingLiveData) {
        Log.d(TAG, "=== ПОИСК НАЧАТ ===");
        Log.d(TAG, "searchRecipes вызван с параметрами:");
        Log.d(TAG, "  - query: '" + query + "'");
        Log.d(TAG, "  - smartSearchEnabled: " + smartSearchEnabled);
        
        isRefreshingLiveData.setValue(true);
        
        if (smartSearchEnabled) {
            Log.d(TAG, "Выполняю умный поиск");
            performSmartSearch(query, searchResultsLiveData, errorMessageLiveData, isRefreshingLiveData);
        } else {
            Log.d(TAG, "Выполняю локальный поиск");
            performLocalSearch(query, searchResultsLiveData, errorMessageLiveData, isRefreshingLiveData);
        }
    }
    
    /**
     * Выполняет умный поиск через AI сервис
     */
    private void performSmartSearch(String query, 
                                   MutableLiveData<List<Recipe>> searchResultsLiveData,
                                   MutableLiveData<String> errorMessageLiveData,
                                   MutableLiveData<Boolean> isRefreshingLiveData) {
        Log.d(TAG, "performSmartSearch начат для запроса: '" + query + "'");
        
        RecipeSearchService searchService = new RecipeSearchService(application);
        searchService.searchRecipes(query, new RecipeSearchService.SearchCallback() {
            @Override
            public void onSearchResults(List<Recipe> recipes) {
                Log.d(TAG, "Получены результаты умного поиска: " + (recipes != null ? recipes.size() : 0) + " рецептов");
                if (recipes != null && !recipes.isEmpty()) {
                    Log.d(TAG, "Первый рецепт: " + recipes.get(0).getTitle());
                }
                
                searchResultsLiveData.postValue(recipes);
                Log.d(TAG, "Результаты умного поиска отправлены в LiveData");
                
                isRefreshingLiveData.postValue(false);
            }
            
            @Override
            public void onSearchError(String error) {
                Log.e(TAG, "Ошибка умного поиска: " + error);
                errorMessageLiveData.postValue("Ошибка умного поиска: " + error + ". Выполняется локальный поиск.");
                Log.d(TAG, "Переключаюсь на локальный поиск из-за ошибки");
                performLocalSearch(query, searchResultsLiveData, errorMessageLiveData, isRefreshingLiveData); 
            }
        });
        
        Log.d(TAG, "RecipeSearchService.searchRecipes вызван, ожидаем результат");
    }
    
    /**
     * Выполняет локальный поиск в базе данных
     */
    private void performLocalSearch(String query, 
                                   MutableLiveData<List<Recipe>> searchResultsLiveData, 
                                   MutableLiveData<String> errorMessageLiveData, 
                                   MutableLiveData<Boolean> isRefreshingLiveData) {
        Log.d(TAG, "performLocalSearch начат для запроса: '" + query + "'");
        repository.searchInLocalData(query, searchResultsLiveData);
        isRefreshingLiveData.postValue(false);
        Log.d(TAG, "Локальный поиск завершен");
    }
    
    /**
     * Очищает ресурсы
     */
    public void clearResources() {
        Log.d(TAG, "clearResources вызван");
        repository.clearDisposables();
    }
} 
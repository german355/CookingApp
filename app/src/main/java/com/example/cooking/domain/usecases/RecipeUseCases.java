package com.example.cooking.domain.usecases;

import android.app.Application;
import androidx.lifecycle.MutableLiveData;
import com.example.cooking.Recipe.Recipe;
import com.example.cooking.data.repositories.RecipeRemoteRepository;
import com.example.cooking.data.repositories.UnifiedRecipeRepository;
import com.example.cooking.network.utils.Resource;
import com.example.cooking.utils.RecipeSearchService;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

public class RecipeUseCases {
    private final UnifiedRecipeRepository repository;
    private final Application application;
    private final ExecutorService executor;
    
    public RecipeUseCases(Application application, ExecutorService executor) {
        this.application = application;
        this.executor = executor;
        this.repository = new UnifiedRecipeRepository(application, executor);
    }
    
    public interface SearchCallback {
        void onSearchComplete(List<Recipe> results);
        void onSearchError(String error);
    }
    
    public void searchRecipes(String query, boolean smartSearchEnabled, 
                              MutableLiveData<List<Recipe>> searchResultsLiveData,
                              MutableLiveData<String> errorMessageLiveData,
                              MutableLiveData<Boolean> isRefreshingLiveData) {
        isRefreshingLiveData.setValue(true);
        if (smartSearchEnabled) {
            RecipeSearchService searchService = new RecipeSearchService(application);
            searchService.searchRecipes(query, new RecipeSearchService.SearchCallback() {
                @Override
                public void onSearchResults(List<Recipe> recipes) {
                    searchResultsLiveData.postValue(recipes);
                    isRefreshingLiveData.postValue(false);
                }
                
                @Override
                public void onSearchError(String error) {
                    errorMessageLiveData.postValue("Ошибка умного поиска: " + error + ". Выполняется локальный поиск.");
                    performLocalSearch(query, searchResultsLiveData, errorMessageLiveData, isRefreshingLiveData); 
                }
            });
        } else {
            performLocalSearch(query, searchResultsLiveData, errorMessageLiveData, isRefreshingLiveData);
        }
    }
    
    private void performLocalSearch(String query, 
                                    MutableLiveData<List<Recipe>> searchResultsLiveData, 
                                    MutableLiveData<String> errorMessageLiveData, 
                                    MutableLiveData<Boolean> isRefreshingLiveData) {
        repository.searchInLocalData(query, searchResultsLiveData, errorMessageLiveData);
    }
    
    public void refreshRecipes(MutableLiveData<Boolean> isRefreshingLiveData, 
                            MutableLiveData<String> errorMessageLiveData,
                            MutableLiveData<Resource<List<Recipe>>> recipesLiveData,
                            Runnable onComplete) {
        
        isRefreshingLiveData.postValue(true);
        
        repository.loadRemoteRecipes(new RecipeRemoteRepository.RecipesCallback() {
            @Override
            public void onRecipesLoaded(List<Recipe> remoteRecipes) {
                repository.syncWithRemoteData(remoteRecipes, errorMessageLiveData, recipesLiveData);
                isRefreshingLiveData.postValue(false);
                if (onComplete != null) {
                    onComplete.run();
                }
            }
            
            @Override
            public void onDataNotAvailable(String error) {
                errorMessageLiveData.postValue(error);
                if (recipesLiveData != null) {
                    recipesLiveData.postValue(Resource.error(error, null));
                }
                isRefreshingLiveData.postValue(false);
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }
    
    public void toggleLike(String userId, int recipeId, MutableLiveData<String> errorMessageLiveData) {
        if (userId == null || userId.equals("0") || userId.isEmpty()) {
            errorMessageLiveData.setValue("Войдите, чтобы добавить рецепт в избранное");
            return;
        }
        repository.toggleLike(userId, recipeId);
    }

    public void setLikeStatus(String userId, int recipeId, boolean newLikeStatus, MutableLiveData<String> errorMessageLiveData) {
        if (userId == null || userId.equals("0") || userId.isEmpty()) {
            errorMessageLiveData.setValue("Войдите, чтобы установить статус лайка");
            return;
        }
        repository.setLikeStatus(userId, recipeId, newLikeStatus);
    }
} 
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
    private final android.net.ConnectivityManager connectivityManager;
    
    public RecipeUseCases(Application application, ExecutorService executor) {
        this.application = application;
        this.executor = executor;
        this.repository = new UnifiedRecipeRepository(application, executor);
        this.connectivityManager = (android.net.ConnectivityManager) application.getSystemService(android.content.Context.CONNECTIVITY_SERVICE);
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
                    // Полученные объекты уже полные — напрямую отдаем UI
                    android.util.Log.d("RecipeUseCases", "Получены результаты поиска в RecipeUseCases: " + (recipes != null ? recipes.size() : 0) + " рецептов");
                    if (recipes != null && !recipes.isEmpty()) {
                        android.util.Log.d("RecipeUseCases", "Первый рецепт: " + recipes.get(0).getTitle());
                    }
                    
                    // Проверяем searchResultsLiveData перед отправкой
                    android.util.Log.d("RecipeUseCases", "searchResultsLiveData перед отправкой: " + (searchResultsLiveData != null ? "не null" : "null"));
                    
                    searchResultsLiveData.postValue(recipes);
                    android.util.Log.d("RecipeUseCases", "Результаты поиска отправлены в LiveData");
                    
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
        
        // Проверяем доступность сети перед загрузкой данных
        if (!isNetworkAvailable()) {
            // Если сеть недоступна, загружаем данные из локального кэша
            executor.execute(() -> {
                try {
                    // Получаем все рецепты из локальной базы данных
                    List<Recipe> localRecipes = repository.getAllRecipesSync();
                    
                    if (localRecipes != null && !localRecipes.isEmpty()) {
                        // Если есть кэшированные рецепты, возвращаем их

                        
                        // Отображаем тост о работе в офлайн-режиме
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                            android.widget.Toast.makeText(application, "Вы работаете в офлайн режиме. Отображаются сохраненные рецепты.", 
                                android.widget.Toast.LENGTH_LONG).show();
                        });
                        
                        // Обновляем UI с данными из кэша
                        if (recipesLiveData != null) {
                            recipesLiveData.postValue(Resource.success(localRecipes));
                        }
                        errorMessageLiveData.postValue("Вы работаете в офлайн режиме. Отображаются сохраненные рецепты.");
                    } else {
                        // Если кэш пуст, сообщаем об ошибке
                        errorMessageLiveData.postValue("Нет сохраненных рецептов для отображения в офлайн режиме");
                        if (recipesLiveData != null) {
                            recipesLiveData.postValue(Resource.error("Нет сохраненных рецептов для отображения в офлайн режиме", null));
                        }
                        
                        // Отображаем тост о проблеме
                        new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                            android.widget.Toast.makeText(application, "Нет сохраненных рецептов для отображения в офлайн режиме", 
                                android.widget.Toast.LENGTH_LONG).show();
                        });
                    }
                } catch (Exception e) {
                    android.util.Log.e("RecipeUseCases", "Error loading cached recipes", e);
                    errorMessageLiveData.postValue("Ошибка при загрузке кэшированных рецептов: " + e.getMessage());
                    if (recipesLiveData != null) {
                        recipesLiveData.postValue(Resource.error("Ошибка при загрузке кэшированных рецептов", null));
                    }
                } finally {
                    isRefreshingLiveData.postValue(false);
                    if (onComplete != null) {
                        onComplete.run();
                    }
                }
            });
            return;
        }
        
        // Если сеть доступна, продолжаем обычную загрузку данных с сервера
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
                // Если при загрузке с сервера произошла ошибка, но это офлайн-режим,
                // то загружаем данные из кэша
                if (error.contains("офлайн режиме")) {
                    executor.execute(() -> {
                        List<Recipe> localRecipes = repository.getAllRecipesSync();
                        if (localRecipes != null && !localRecipes.isEmpty()) {
                            if (recipesLiveData != null) {
                                recipesLiveData.postValue(Resource.success(localRecipes));
                            }
                        } else {
                            if (recipesLiveData != null) {
                                recipesLiveData.postValue(Resource.error("Нет сохраненных рецептов для отображения в офлайн режиме", null));
                            }
                        }
                    });
                } else {
                    // Это обычная ошибка загрузки
                    errorMessageLiveData.postValue(error);
                    if (recipesLiveData != null) {
                        recipesLiveData.postValue(Resource.error(error, null));
                    }
                }
                
                isRefreshingLiveData.postValue(false);
                if (onComplete != null) {
                    onComplete.run();
                }
            }
        });
    }
    
    public void toggleLike(String userId, int recipeId, MutableLiveData<String> errorMessageLiveData) {
        // Проверяем доступность сети перед установкой лайка
        if (!isNetworkAvailable()) {
            // Отображаем уведомление о невозможности поставить лайк в офлайн-режиме
            errorMessageLiveData.setValue("Вы в офлайн режиме и не можете ставить лайки");
            // Показываем Toast
            android.widget.Toast.makeText(application, "Вы в офлайн режиме и не можете ставить лайки", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (userId == null || userId.equals("0") || userId.isEmpty()) {
            errorMessageLiveData.setValue("Войдите, чтобы добавить рецепт в избранное");
            return;
        }
        repository.toggleLike(userId, recipeId);
    }

    public void addLike(String userId, int recipeId, MutableLiveData<String> errorMessageLiveData) {
        if (userId == null || userId.equals("0") || userId.isEmpty()) {
            errorMessageLiveData.setValue("Войдите, чтобы добавить рецепт в избранное");
            return;
        }
        setLikeStatus(userId, recipeId, true, errorMessageLiveData);
    }

    public void removeLike(String userId, int recipeId, MutableLiveData<String> errorMessageLiveData) {
        if (userId == null || userId.equals("0") || userId.isEmpty()) {
            errorMessageLiveData.setValue("Войдите, чтобы убрать рецепт из избранного");
            return;
        }
        setLikeStatus(userId, recipeId, false, errorMessageLiveData);
    }
    
    /**
     * Проверяет доступность сети
     * @return true если сеть доступна, иначе false
     */
    public boolean isNetworkAvailable() {
        if (connectivityManager != null) {
            android.net.NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
        }
        return false;
    }

    public void setLikeStatus(String userId, int recipeId, boolean newLikeStatus, MutableLiveData<String> errorMessageLiveData) {
        // Проверяем доступность сети перед установкой статуса лайка
        if (!isNetworkAvailable()) {
            // Отображаем уведомление о невозможности поставить лайк в офлайн-режиме
            errorMessageLiveData.setValue("Вы в офлайн режиме и не можете ставить лайки");
            // Показываем Toast
            android.widget.Toast.makeText(application, "Вы в офлайн режиме и не можете ставить лайки", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (userId == null || userId.equals("0") || userId.isEmpty()) {
            errorMessageLiveData.setValue("Войдите, чтобы установить статус лайка");
            return;
        }
        repository.setLikeStatus(userId, recipeId, newLikeStatus);
    }
} 
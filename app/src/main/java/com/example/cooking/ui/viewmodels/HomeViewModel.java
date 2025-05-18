package com.example.cooking.ui.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import android.util.Pair;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.fragment.app.FragmentActivity;

import com.example.cooking.Recipe.Recipe;
import com.example.cooking.data.repositories.RecipeLocalRepository;
import com.example.cooking.data.repositories.RecipeRemoteRepository;
import com.example.cooking.data.repositories.LikedRecipesRepository;
import com.example.cooking.utils.MySharedPreferences;
import android.content.SharedPreferences;
import androidx.preference.PreferenceManager;
import com.example.cooking.network.services.NetworkService;
import com.example.cooking.network.responses.SearchResponse;
import com.example.cooking.network.utils.ApiCallHandler;
import retrofit2.Call;
import com.example.cooking.utils.RecipeSearchService;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.ArrayList;

/**
 * ViewModel для HomeFragment
 */
public class HomeViewModel extends AndroidViewModel {
    
    private static final String TAG = "HomeViewModel";
    private final RecipeLocalRepository localRepository;
    private final RecipeRemoteRepository remoteRepository;
    private final LikedRecipesRepository likedRecipesRepository;
    private LikeSyncViewModel likeSyncViewModel; // Убрали final, будем инициализировать позже
    private final ExecutorService executor;
    
    // LiveData для состояния загрузки и ошибок
    private final MutableLiveData<Boolean> isRefreshing = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
    // Храним ID последнего обработанного события лайка, чтобы избежать эха
    private Pair<Integer, Boolean> lastProcessedLikeEvent = null;

    // Флаг для отслеживания первичной загрузки
    private boolean isInitialLoadDone = false;
    
    // LiveData для результатов поиска
    private final MutableLiveData<List<Recipe>> searchResults = new MutableLiveData<>();

    public HomeViewModel(@NonNull Application application) {
        super(application);
        localRepository = new RecipeLocalRepository(application);
        remoteRepository = new RecipeRemoteRepository(application);
        likedRecipesRepository = new LikedRecipesRepository(application);
        executor = Executors.newFixedThreadPool(2);
        // likeSyncViewModel инициализируется в observeLikeChanges
    }
    
    // Метод для инициализации наблюдения за Shared ViewModel. Вызывается из Фрагмента.
    public void observeLikeChanges(LifecycleOwner owner, FragmentActivity activity) {
        // Инициализируем Shared ViewModel здесь, используя Activity scope
        // Сохраняем его в переменную класса
        likeSyncViewModel = new ViewModelProvider(activity).get(LikeSyncViewModel.class);

        likeSyncViewModel.getLikeChangeEvent().observe(owner, event -> {
            if (event != null && !event.equals(lastProcessedLikeEvent)) {
                Log.d(TAG, "Received like change event from LikeSyncViewModel: " + event.first + " -> " + event.second);
                // Добавляем логирование перед обновлением
                Log.i(TAG, "[LikeSync] Updating local statuses for Recipe ID: " + event.first + " to liked: " + event.second);
                // Обновляем статус лайка в ЛОКАЛЬНОЙ БД ОСНОВНЫХ рецептов
                updateLocalLikeStatus(event.first, event.second);
                // Обновляем статус лайка в ЛОКАЛЬНОЙ БД ЛАЙКНУТЫХ рецептов
                updateLikedRepositoryStatus(event.first, event.second);
                // Мы не сохраняем это событие как lastProcessed, так как обновление локальной БД
                // должно быть идемпотентным.
            } else if (event != null) {
                Log.d(TAG, "[LikeSync] Ignored duplicate/own like event: " + event.first + " -> " + event.second);
            }
        });
    }
    
    /**
     * Получить LiveData со списком рецептов из локального хранилища
     */
    public LiveData<List<Recipe>> getRecipes() {
        return localRepository.getAllRecipes();
    }
    
    /**
     * Получить LiveData с состоянием обновления данных
     */
    public LiveData<Boolean> getIsRefreshing() {
        return isRefreshing;
    }
    
    /**
     * Получить LiveData с сообщением об ошибке
     */
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * Получить LiveData с результатами поиска
     */
    public LiveData<List<Recipe>> getSearchResults() {
        return searchResults;
    }
    
    /**
     * Загружает рецепты при первом запуске, если это еще не сделано.
     */
    public void loadInitialRecipesIfNeeded() {
        if (!isInitialLoadDone) {
            Log.d(TAG, "Performing initial recipe load.");
            refreshRecipes(); // Вызываем существующий метод обновления
            isInitialLoadDone = true;
        } else {
            Log.d(TAG, "Initial load already done, skipping refresh trigger from initial load.");
        }
    }
    
    /**
     * Загрузить рецепты с сервера и обновить локальную базу данных.
     * НЕ очищает базу данных перед вставкой.
     * Использует OnConflictStrategy.REPLACE в DAO для обновления существующих записей.
     */
    public void refreshRecipes() {
        isRefreshing.setValue(true);
        Log.d(TAG, "Refreshing recipes...");
        
        remoteRepository.getRecipes(new RecipeRemoteRepository.RecipesCallback() {
            @Override
            public void onRecipesLoaded(List<Recipe> remoteRecipes) {
                 Log.d(TAG, "Recipes loaded from remote: " + (remoteRecipes != null ? remoteRecipes.size() : 0));
                // Сохраняем или обновляем рецепты в локальном хранилище
                executeIfActive(() -> {
                    try {
                        if (remoteRecipes != null) {
                             // Получаем ID текущего пользователя
                             String currentUserId = new MySharedPreferences(getApplication()).getString("userId", "0");

                             // Получаем набор ID лайкнутых рецептов из LikedRecipesRepository
                             Set<Integer> likedRecipeIds = new HashSet<>();
                             if (!currentUserId.equals("0")) {
                                 List<Integer> likedIdsList = likedRecipesRepository.getLikedRecipeIdsSync(currentUserId);
                                 if (likedIdsList != null) {
                                     likedRecipeIds.addAll(likedIdsList);
                                 }
                                 Log.d(TAG, "Loaded liked recipe IDs for user " + currentUserId + ": " + likedRecipeIds.size());
                             } else {
                                 Log.w(TAG, "User not logged in (userId=0), cannot load liked IDs.");
                             }

                             // Обновляем isLiked в полученных с сервера рецептах
                             for (Recipe remoteRecipe : remoteRecipes) {
                                 boolean isLiked = likedRecipeIds.contains(remoteRecipe.getId());
                                 remoteRecipe.setLiked(isLiked);
                                 // Лог для проверки
                                 // Log.d(TAG, "Setting like status for ID " + remoteRecipe.getId() + ": " + isLiked);
                             }

                             // Вставляем/заменяем рецепты с обновленным статусом isLiked
                             localRepository.insertAll(remoteRecipes);
                             Log.d(TAG, "Recipes inserted/updated in local storage with like status based on LikedRecipesRepository: " + remoteRecipes.size());
                        } else {
                             Log.w(TAG, "Received null recipe list from remote.");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error saving recipes to local storage", e);
                        errorMessage.postValue("Ошибка сохранения данных локально.");
                    } finally {
                        isRefreshing.postValue(false);
                         Log.d(TAG, "Recipe refresh finished.");
                    }
                });
            }
            
            @Override
            public void onDataNotAvailable(String error) {
                Log.e(TAG, "Error loading recipes from remote: " + error);
                errorMessage.postValue(error);
                isRefreshing.postValue(false);
                 Log.d(TAG, "Recipe refresh finished with error.");
            }
        });
    }
    
    /**
     * Обновить состояние лайка для рецепта.
     * Обновляет локальную БД и оповещает другие компоненты через Shared ViewModel.
     * @param recipe рецепт
     * @param isLiked новое состояние лайка
     */
    public void updateLikeStatus(Recipe recipe, boolean isLiked) {
        // 1. Обновляем статус в локальной базе RecipeEntity
        updateLocalLikeStatus(recipe.getId(), isLiked);

        // 2. Обновляем статус в локальной базе LikedRecipeEntity
        updateLikedRepositoryStatus(recipe.getId(), isLiked);

        // 3. Оповещаем Shared ViewModel об изменении (для других фрагментов)
        // Используем сохраненную переменную likeSyncViewModel
        if (likeSyncViewModel != null) {
             Log.d(TAG, "Notifying LikeSyncViewModel about change: " + recipe.getId() + " -> " + isLiked);
             // Сохраняем событие, которое мы инициировали
             lastProcessedLikeEvent = new Pair<>(recipe.getId(), isLiked);
             likeSyncViewModel.notifyLikeChanged(recipe.getId(), isLiked);
        } else {
             Log.e(TAG, "LikeSyncViewModel is null! Cannot notify.");
        }

        // 4. Отправляем обновление на сервер (асинхронно)
        remoteRepository.updateLikeStatus(recipe, isLiked);
    }
    
    /**
     * Обновляет статус лайка в локальной БД ОСНОВНЫХ рецептов (RecipeEntity).
     */
    public void updateLocalLikeStatus(int recipeId, boolean isLiked) {
        executeIfActive(() -> {
            try {
                Log.d(TAG, "Updating like status in local REPOSITORY for recipe " + recipeId + " to " + isLiked);
                localRepository.updateLikeStatus(recipeId, isLiked);
            } catch (Exception e) {
                Log.e(TAG, "Error updating like status in local repository: " + e.getMessage(), e);
            }
        });
    }

    private void updateLikedRepositoryStatus(int recipeId, boolean isLiked) {
         String currentUserId = new MySharedPreferences(getApplication()).getString("userId", "0");
        if (currentUserId.equals("0")) {
            Log.w(TAG, "Cannot update liked repository status: User ID is 0.");
            return;
        }
        executeIfActive(() -> {
            try {
                Log.d(TAG, "Updating like status in LIKED REPOSITORY for recipe " + recipeId + " to " + isLiked);
                if (isLiked) {
                    likedRecipesRepository.insertLikedRecipeLocal(recipeId, currentUserId);
                } else {
                    likedRecipesRepository.deleteLikedRecipeLocal(recipeId, currentUserId);
                }
            } catch (Exception e) {
                 Log.e(TAG, "Error updating status in liked repository: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Метод, выполняющий операцию в executor с проверкой его состояния
     * @param task задача для выполнения
     */
    private void executeIfActive(Runnable task) {
        if (executor != null && !executor.isShutdown()) {
            try {
                executor.execute(task);
            } catch (Exception e) {
                 Log.e(TAG, "Error executing task in executor", e);
            }
        } else {
            Log.w(TAG, "Executor is null or shut down, skipping task");
        }
    }
    
    /**
     * Очистить ресурсы
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
         Log.d(TAG, "HomeViewModel cleared.");
    }

    /**
     * Выполнить поиск рецептов, учитывая настройку Smart Search
     * @param query строка поиска
     */
    public void searchRecipes(String query) {
        Log.d(TAG, "Searching recipes with query: " + query);
        isRefreshing.setValue(true);
        
        // Используем RecipeSearchService для поиска
        RecipeSearchService searchService = new RecipeSearchService(getApplication());
        searchService.searchRecipes(query, new RecipeSearchService.SearchCallback() {
            @Override
            public void onSearchResults(List<Recipe> recipes) {
                searchResults.postValue(recipes);
                isRefreshing.postValue(false);
                Log.d(TAG, "Search complete, found " + (recipes != null ? recipes.size() : 0) + " results");
            }
            
            @Override
            public void onSearchError(String error) {
                Log.e(TAG, "Search error: " + error);
                errorMessage.postValue("Ошибка поиска: " + error);
                searchResults.postValue(Collections.emptyList());
                isRefreshing.postValue(false);
            }
        });
    }
} 
package com.example.cooking.ui.viewmodels;

import android.app.Application;
import android.util.Log;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cooking.Recipe.Recipe;
import com.example.cooking.data.repositories.RecipeLocalRepository;
import com.example.cooking.data.repositories.RecipeRemoteRepository;
import com.example.cooking.data.repositories.LikedRecipesRepository;
import com.example.cooking.network.utils.Resource;
import com.example.cooking.utils.MySharedPreferences;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SharedViewModel для данных о рецептах.
 * Является единым источником данных для всех фрагментов, работающих с рецептами.
 */
public class SharedRecipeViewModel extends AndroidViewModel {
    private static final String TAG = "SharedRecipeViewModel";

    // Репозитории
    private final RecipeLocalRepository localRepository;
    private final RecipeRemoteRepository remoteRepository;
    private final LikedRecipesRepository likedRecipesRepository;
    private final ExecutorService executor;

    // LiveData для рецептов с обернутым статусом
    private final MutableLiveData<Resource<List<Recipe>>> recipes = new MutableLiveData<>(Resource.loading(null));
    
    // LiveData для результатов поиска
    private final MutableLiveData<List<Recipe>> searchResults = new MutableLiveData<>();
    
    // LiveData для состояния загрузки и ошибок
    private final MutableLiveData<Boolean> isRefreshing = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // Флаг для отслеживания первичной загрузки
    private boolean isInitialLoadDone = false;

    public SharedRecipeViewModel(@NonNull Application application) {
        super(application);
        localRepository = new RecipeLocalRepository(application);
        remoteRepository = new RecipeRemoteRepository(application);
        likedRecipesRepository = new LikedRecipesRepository(application);
        executor = Executors.newFixedThreadPool(2);

        // Инициализация наблюдения за данными из локального репозитория
        initLocalDataObserver();
    }

    /**
     * Настройка наблюдения за локальными данными
     */
    private void initLocalDataObserver() {
        localRepository.getAllRecipes().observeForever(recipesList -> {
            if (isRefreshing.getValue() != Boolean.TRUE) {
                recipes.setValue(Resource.success(recipesList));
            }
        });
    }

    /**
     * Получить LiveData со списком рецептов
     */
    public LiveData<Resource<List<Recipe>>> getRecipes() {
        // Если первичная загрузка еще не выполнена, запускаем ее
        if (!isInitialLoadDone) {
            loadInitialRecipes();
        }
        return recipes;
    }

    /**
     * Получить LiveData с результатами поиска
     */
    public LiveData<List<Recipe>> getSearchResults() {
        return searchResults;
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
     * Загрузка рецептов при первом запуске
     */
    public void loadInitialRecipes() {
        if (!isInitialLoadDone) {
            Log.d(TAG, "Выполняется первичная загрузка рецептов");
            refreshRecipes();
            isInitialLoadDone = true;
        }
    }

    /**
     * Обновление списка рецептов с сервера
     */
    public void refreshRecipes() {
        isRefreshing.setValue(true);
        Log.d(TAG, "Обновление рецептов с сервера...");
        
        remoteRepository.getRecipes(new RecipeRemoteRepository.RecipesCallback() {
            @Override
            public void onRecipesLoaded(List<Recipe> remoteRecipes) {
                Log.d(TAG, "Рецептов загружено с сервера: " + (remoteRecipes != null ? remoteRecipes.size() : 0));
                
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
                                Log.d(TAG, "Загружены ID лайкнутых рецептов для пользователя " + currentUserId + ": " + likedRecipeIds.size());
                            } else {
                                Log.w(TAG, "Пользователь не авторизован (userId=0), невозможно загрузить лайкнутые рецепты.");
                            }

                            // Обновляем isLiked в полученных с сервера рецептах
                            for (Recipe remoteRecipe : remoteRecipes) {
                                boolean isLiked = likedRecipeIds.contains(remoteRecipe.getId());
                                remoteRecipe.setLiked(isLiked);
                            }

                            // Вставляем/заменяем рецепты с обновленным статусом isLiked
                            localRepository.insertAll(remoteRecipes);
                            Log.d(TAG, "Рецепты сохранены в локальное хранилище: " + remoteRecipes.size());
                            
                            // Обновляем LiveData с данными статусом SUCCESS
                            recipes.postValue(Resource.success(remoteRecipes));
                        } else {
                            Log.w(TAG, "Получен null список рецептов с сервера.");
                            errorMessage.postValue("Не удалось получить рецепты с сервера");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка при сохранении рецептов в локальное хранилище", e);
                        errorMessage.postValue("Ошибка сохранения данных локально: " + e.getMessage());
                    } finally {
                        isRefreshing.postValue(false);
                    }
                });
            }
            
            @Override
            public void onDataNotAvailable(String error) {
                Log.e(TAG, "Ошибка загрузки рецептов с сервера: " + error);
                errorMessage.postValue(error);
                isRefreshing.postValue(false);
            }
        });
    }

    /**
     * Обновление статуса лайка рецепта
     */
    public void updateLikeStatus(Recipe recipe, boolean isLiked) {
        if (recipe == null) {
            Log.e(TAG, "updateLikeStatus: рецепт равен null");
            return;
        }

        int recipeId = recipe.getId();
        String userId = new MySharedPreferences(getApplication()).getString("userId", "0");
        
        if (userId.equals("0")) {
            Log.w(TAG, "Нельзя обновить статус лайка: пользователь не авторизован.");
            errorMessage.setValue("Войдите, чтобы добавить рецепт в избранное");
            return;
        }

        // Обновляем статус в локальной БД
        updateLocalLikeStatus(recipeId, isLiked);
        
        // Обновляем в репозитории лайкнутых рецептов
        if (isLiked) {
            likedRecipesRepository.addLikedRecipe(userId, recipeId);
        } else {
            likedRecipesRepository.removeLikedRecipe(userId, recipeId);
        }
    }

    /**
     * Обновление статуса лайка в локальном хранилище
     */
    private void updateLocalLikeStatus(int recipeId, boolean isLiked) {
        executor.execute(() -> {
            localRepository.updateLikeStatus(recipeId, isLiked);
            Log.d(TAG, "Обновлен локальный статус лайка рецепта " + recipeId + " на " + isLiked);
        });
    }

    /**
     * Выполнить задачу, если ViewModel активна
     */
    private void executeIfActive(Runnable task) {
        try {
            executor.execute(task);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка выполнения задачи: " + e.getMessage(), e);
        }
    }
    
    /**
     * Выполнить поиск среди рецептов
     */
    public void searchRecipes(String query) {
        // Если запрос пустой, очищаем результаты поиска
        if (query == null || query.trim().isEmpty()) {
            searchResults.setValue(null);
            return;
        }

        // Берем текущие данные из LiveData
        Resource<List<Recipe>> resource = recipes.getValue();
        if (resource != null && resource.isSuccess() && resource.getData() != null) {
            List<Recipe> allRecipes = resource.getData();
            
            // Фильтруем рецепты по запросу (поиск в названии, ингредиентах и инструкциях)
            List<Recipe> filtered = allRecipes.stream()
                .filter(recipe -> matchesSearchQuery(recipe, query.toLowerCase()))
                .collect(java.util.stream.Collectors.toList());
                
            Log.d(TAG, "Search results for \"" + query + "\": found " + filtered.size() + " matches");
            searchResults.setValue(filtered);
        } else {
            Log.d(TAG, "Search failed: no recipes data available");
            errorMessage.setValue("Не удалось выполнить поиск: данные недоступны");
            searchResults.setValue(new ArrayList<>());
        }
    }
    
    /**
     * Проверяет, соответствует ли рецепт поисковому запросу
     */
    private boolean matchesSearchQuery(Recipe recipe, String query) {
        if (recipe.getTitle() != null && recipe.getTitle().toLowerCase().contains(query)) {
            return true;
        }

        // Поиск в ингредиентах
        if (recipe.getIngredients() != null) {
            for (com.example.cooking.Recipe.Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient.getName() != null && ingredient.getName().toLowerCase().contains(query)) {
                    return true;
                }
            }
        }

        // Поиск в шагах приготовления
        if (recipe.getSteps() != null) {
            for (com.example.cooking.Recipe.Step step : recipe.getSteps()) {
                if (step.getInstruction() != null && step.getInstruction().toLowerCase().contains(query)) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Переключает статус лайка для рецепта по его ID
     * @param userId ID пользователя
     * @param recipeId ID рецепта
     */
    public void toggleLike(String userId, int recipeId) {
        if (userId == null || userId.equals("0") || userId.isEmpty()) {
            Log.w(TAG, "Нельзя переключить лайк: невалидный userId=" + userId);
            errorMessage.setValue("Войдите, чтобы добавить рецепт в избранное");
            return;
        }
        
        // Получаем текущий статус лайка из локального репозитория
        boolean isCurrentlyLiked = likedRecipesRepository.isRecipeLikedLocalSync(recipeId, userId);
        Log.d(TAG, "toggleLike: recipeId=" + recipeId + ", userId=" + userId + ", текущий статус=" + isCurrentlyLiked);
        
        // Переключаем статус лайка
        boolean newLikeStatus = !isCurrentlyLiked;
        
        // Обновляем статус в локальной БД
        updateLocalLikeStatus(recipeId, newLikeStatus);
        
        // Обновляем в репозитории лайкнутых рецептов
        if (newLikeStatus) {
            likedRecipesRepository.addLikedRecipe(userId, recipeId);
        } else {
            likedRecipesRepository.removeLikedRecipe(userId, recipeId);
        }
    }

    /**
     * Очистка ресурсов при уничтожении ViewModel
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
} 
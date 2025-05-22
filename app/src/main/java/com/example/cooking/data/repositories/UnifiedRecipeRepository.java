package com.example.cooking.data.repositories;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import android.os.Handler;
import android.os.Looper;

import com.example.cooking.Recipe.Recipe;
import com.example.cooking.network.utils.Resource;
import com.example.cooking.utils.MySharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

public class UnifiedRecipeRepository {
    private static final String TAG = "UnifiedRecipeRepository";
    
    private final RecipeLocalRepository localRepository;
    private final RecipeRemoteRepository remoteRepository;
    private final LikedRecipesRepository likedRecipesRepository;
    private final Application application;
    private final ExecutorService executor;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    
    public UnifiedRecipeRepository(Application application, ExecutorService executor) {
        this.application = application;
        this.localRepository = new RecipeLocalRepository(application);
        this.remoteRepository = new RecipeRemoteRepository(application);
        this.likedRecipesRepository = new LikedRecipesRepository(application);
        this.executor = executor;
    }
    
    public LiveData<List<Recipe>> getAllRecipesLocal() {
        return localRepository.getAllRecipes();
    }
    
    // Этот метод должен вызываться из фонового потока
    public List<Recipe> getAllRecipesSync() {
        return localRepository.getAllRecipesSync();
    }
    
    /**
     * Синхронизирует локальные данные с данными с сервера в фоновом потоке.
     * Оповещает об ошибке через errorMessage в основном потоке.
     * Обновляет recipesLiveData в основном потоке, если он не null.
     */
    public void syncWithRemoteData(List<Recipe> remoteRecipes, MutableLiveData<String> errorMessage, MutableLiveData<Resource<List<Recipe>>> recipesLiveData) {
        executor.execute(() -> {
            try {
                if (remoteRecipes != null) {
                    String currentUserId = new MySharedPreferences(application).getString("userId", "0");
                    Set<Integer> likedRecipeIds = new HashSet<>();
                    if (!currentUserId.equals("0")) {
                        List<Integer> likedIdsList = likedRecipesRepository.getLikedRecipeIdsSync(); // Вызов к БД
                        if (likedIdsList != null) {
                            likedRecipeIds.addAll(likedIdsList);
                        }
                        Log.d(TAG, "(BG Thread) Загружены ID лайкнутых рецептов: " + likedRecipeIds.size());
                    }

                    List<Recipe> localRecipes = localRepository.getAllRecipesSync(); // Вызов к БД
                    Set<Integer> remoteRecipeIds = new HashSet<>();
                    
                    for (Recipe remoteRecipe : remoteRecipes) {
                        remoteRecipe.setLiked(likedRecipeIds.contains(remoteRecipe.getId()));
                        remoteRecipeIds.add(remoteRecipe.getId());
                    }

                    Set<Integer> deletedRecipeIds = new HashSet<>();
                    if (localRecipes != null) {
                        for (Recipe localRecipe : localRecipes) {
                            if (!remoteRecipeIds.contains(localRecipe.getId())) {
                                deletedRecipeIds.add(localRecipe.getId());
                            }
                        }
                    }
                    
                    if (!deletedRecipeIds.isEmpty()) {
                        for (Integer deletedId : deletedRecipeIds) {
                            localRepository.deleteRecipe(deletedId); // Вызов к БД
                        }
                        Log.d(TAG, "(BG Thread) Удалено " + deletedRecipeIds.size() + " рецептов");
                    }

                    localRepository.insertAll(remoteRecipes); // Вызов к БД
                    Log.d(TAG, "(BG Thread) Сохранено в локальное хранилище: " + remoteRecipes.size() + " рецептов");
                    // Обновляем LiveData в основном потоке
                    if (recipesLiveData != null) {
                        mainThreadHandler.post(() -> recipesLiveData.setValue(Resource.success(remoteRecipes)));
                    }
                } else {
                     if (recipesLiveData != null) {
                        mainThreadHandler.post(() -> recipesLiveData.setValue(Resource.error("Получен null список рецептов с сервера", null)));
                     }
                }
            } catch (Exception e) {
                Log.e(TAG, "(BG Thread) Ошибка при синхронизации: " + e.getMessage(), e);
                if (errorMessage != null) {
                    // Обновляем LiveData об ошибке в основном потоке
                    mainThreadHandler.post(() -> errorMessage.setValue("Ошибка синхронизации: " + e.getMessage()));
                }
                // Также обновляем recipesLiveData с ошибкой
                if (recipesLiveData != null) {
                    mainThreadHandler.post(() -> recipesLiveData.setValue(Resource.error("Ошибка синхронизации: " + e.getMessage(), null)));
                }
            }
        });
    }
    
    public void loadRemoteRecipes(RecipeRemoteRepository.RecipesCallback callback) {
        remoteRepository.getRecipes(callback);
    }
    
    public void updateLikeStatus(int recipeId, boolean isLiked) {
         executor.execute(() -> localRepository.updateLikeStatus(recipeId, isLiked));
    }
    
    public void toggleLike(String userId, int recipeId) {
        executor.execute(() -> {
            boolean isCurrentlyLiked = likedRecipesRepository.isRecipeLikedLocalSync(recipeId);
            boolean newLikeStatus = !isCurrentlyLiked;
            localRepository.updateLikeStatus(recipeId, newLikeStatus);
            if (newLikeStatus) {
                likedRecipesRepository.addLikedRecipe(userId, recipeId);
            } else {
                likedRecipesRepository.removeLikedRecipe(userId, recipeId);
            }
        });
    }

    public void setLikeStatus(String userId, int recipeId, boolean newLikeStatus) {
        executor.execute(() -> {
            localRepository.updateLikeStatus(recipeId, newLikeStatus);
            if (newLikeStatus) {
                likedRecipesRepository.addLikedRecipe(userId, recipeId);
            } else {
                likedRecipesRepository.removeLikedRecipe(userId, recipeId);
            }
            Log.d(TAG, "(BG Thread) Установлен статус лайка для рецепта " + recipeId + " на " + newLikeStatus + " для пользователя " + userId);
        });
    }
    
    /**
     * Выполняет поиск в локальных данных в фоновом потоке.
     * Результат возвращается через LiveData в основном потоке.
     */
    public void searchInLocalData(String query, MutableLiveData<List<Recipe>> searchResultsLiveData, MutableLiveData<String> errorMessageLiveData) {
        executor.execute(() -> {
            try {
                List<Recipe> allRecipes = getAllRecipesSync(); // Этот метод теперь вызывается в executor
                if (allRecipes == null) {
                    mainThreadHandler.post(() -> searchResultsLiveData.setValue(Collections.emptyList()));
                    return;
                }
                String lowerQuery = query.toLowerCase();
                List<Recipe> filteredResults = allRecipes.stream()
                    .filter(recipe -> matchesSearchQuery(recipe, lowerQuery))
                    .collect(Collectors.toList());
                mainThreadHandler.post(() -> searchResultsLiveData.setValue(filteredResults));
            } catch (Exception e) {
                Log.e(TAG, "(BG Thread) Ошибка при локальном поиске: " + e.getMessage(), e);
                if (errorMessageLiveData != null) {
                    mainThreadHandler.post(() -> errorMessageLiveData.setValue("Ошибка локального поиска: " + e.getMessage()));
                }
                 mainThreadHandler.post(() -> searchResultsLiveData.setValue(Collections.emptyList()));
            }
        });
    }
    
    private boolean matchesSearchQuery(Recipe recipe, String query) {
        if (recipe.getTitle() != null && recipe.getTitle().toLowerCase().contains(query)) {
            return true;
        }
        if (recipe.getIngredients() != null) {
            for (com.example.cooking.Recipe.Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient.getName() != null && ingredient.getName().toLowerCase().contains(query)) {
                    return true;
                }
            }
        }
        if (recipe.getSteps() != null) {
            for (com.example.cooking.Recipe.Step step : recipe.getSteps()) {
                if (step.getInstruction() != null && step.getInstruction().toLowerCase().contains(query)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    // Этот метод должен вызываться из фонового потока
    public Set<Integer> getLikedRecipeIds() {
        Set<Integer> likedRecipeIds = new HashSet<>();
        List<Integer> likedIdsList = likedRecipesRepository.getLikedRecipeIdsSync(); // Вызов к БД
        if (likedIdsList != null) {
            likedRecipeIds.addAll(likedIdsList);
        }
        return likedRecipeIds;
    }
} 
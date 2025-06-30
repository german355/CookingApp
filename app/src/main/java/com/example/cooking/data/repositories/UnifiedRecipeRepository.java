package com.example.cooking.data.repositories;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import android.os.Handler;
import android.os.Looper;
import com.example.cooking.domain.entities.Recipe;
import com.example.cooking.domain.entities.Ingredient;
import com.example.cooking.domain.entities.Step;
import com.example.cooking.network.models.GeneralServerResponse;
import com.example.cooking.network.utils.Resource;
import com.example.cooking.utils.MySharedPreferences;
import com.example.cooking.utils.AppExecutors;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class UnifiedRecipeRepository {
    private static final String TAG = "UnifiedRecipeRepository";
    private static volatile UnifiedRecipeRepository INSTANCE;

    private final RecipeLocalRepository localRepository;
    private final RecipeRemoteRepository remoteRepository;
    private final LikedRecipesRepository likedRecipesRepository;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    private final Context context;
    private final ConnectivityManager connectivityManager;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final MySharedPreferences prefs;

    public interface RecipeCallback<T> {
        void onSuccess(T data);
        void onFailure(String error);
    }

    private UnifiedRecipeRepository(Application application) {
        this.context = application.getApplicationContext();
        this.localRepository = new RecipeLocalRepository(application);
        this.remoteRepository = new RecipeRemoteRepository(application);
        this.likedRecipesRepository = new LikedRecipesRepository(application);
        this.prefs = new MySharedPreferences(context);
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public static UnifiedRecipeRepository getInstance(Application application) {
        if (INSTANCE == null) {
            synchronized (UnifiedRecipeRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new UnifiedRecipeRepository(application);
                }
            }
        }
        return INSTANCE;
    }

    public LiveData<List<Recipe>> getAllRecipesLocal() {
        return localRepository.getAllRecipes();
    }

    public void syncWithRemoteData(MutableLiveData<Resource<List<Recipe>>> recipesLiveData, MutableLiveData<String> errorMessage) {
        if (!isNetworkAvailable()) {
            errorMessage.postValue("Нет подключения к сети. Отображаются сохраненные данные.");
            loadLocalData(recipesLiveData);
            return;
        }

        remoteRepository.getRecipes(new RecipeRemoteRepository.RecipesCallback() {
            @Override
            public void onRecipesLoaded(List<Recipe> remoteRecipes) {
                // Обрабатываем данные в фоновом потоке батчами для лучшей производительности
                AppExecutors.getInstance().diskIO().execute(() -> {
                    try {
                        // Получаем лайки одним запросом
                        Set<Integer> likedIds = new HashSet<>(likedRecipesRepository.getLikedRecipeIdsSync());
                        
                        // Обрабатываем рецепты оптимизированными батчами (уменьшено с 50 до 25)
                        processRecipesInOptimizedBatches(remoteRecipes, likedIds);
                        
                        // Используем умную замену с дифференциальными обновлениями
                        localRepository.smartReplaceRecipes(remoteRecipes);
                        List<Recipe> updatedLocalRecipes = localRepository.getAllRecipesSync();
                        recipesLiveData.postValue(Resource.success(updatedLocalRecipes));
                        
                        Log.d(TAG, "Успешно обработано " + remoteRecipes.size() + " рецептов");
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка при обработке рецептов: " + e.getMessage());
                        errorMessage.postValue("Ошибка при сохранении рецептов");
                        loadLocalData(recipesLiveData);
                    }
                });
            }

            @Override
            public void onDataNotAvailable(String error) {
                errorMessage.postValue(error);
                loadLocalData(recipesLiveData);
            }
        });
    }

    /**
     * Оптимизированная обработка рецептов батчами с улучшенной производительностью.
     */
    private void processRecipesInOptimizedBatches(List<Recipe> recipes, Set<Integer> likedIds) {
        final int OPTIMAL_BATCH_SIZE = 25; // Уменьшено с 50 для лучшей отзывчивости
        final int PAUSE_BETWEEN_BATCHES_MS = 5; // Уменьшено с 10
        
        try {
            for (int i = 0; i < recipes.size(); i += OPTIMAL_BATCH_SIZE) {
                int endIndex = Math.min(i + OPTIMAL_BATCH_SIZE, recipes.size());
                List<Recipe> batch = recipes.subList(i, endIndex);
                
                // Параллельная обработка лайков в батче для максимальной производительности
                batch.parallelStream().forEach(recipe -> {
                    recipe.setLiked(likedIds.contains(recipe.getId()));
                });
                
                // Проверка прерывания потока
                if (Thread.currentThread().isInterrupted()) {
                    Log.w(TAG, "Обработка батчей прервана");
                    break;
                }
                
                // Микропауза между батчами для предотвращения блокировки
                if (endIndex < recipes.size()) {
                    try {
                        Thread.sleep(PAUSE_BETWEEN_BATCHES_MS);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
            
            Log.d(TAG, String.format("Обработано %d рецептов в %d батчах с ленивой сериализацией", 
                                   recipes.size(), (recipes.size() + OPTIMAL_BATCH_SIZE - 1) / OPTIMAL_BATCH_SIZE));
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при обработке батчей: " + e.getMessage());
        }
    }

    private void loadLocalData(MutableLiveData<Resource<List<Recipe>>> recipesLiveData) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            List<Recipe> localRecipes = localRepository.getAllRecipesSync();
            if (localRecipes != null && !localRecipes.isEmpty()) {
                recipesLiveData.postValue(Resource.success(localRecipes));
            } else {
                recipesLiveData.postValue(Resource.error("Нет сохраненных рецептов.", null));
            }
        });
    }

    public void searchInLocalData(String query, MutableLiveData<List<Recipe>> searchResultsLiveData) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            List<Recipe> allRecipes = localRepository.getAllRecipesSync();
            if (allRecipes == null) allRecipes = Collections.emptyList();
            String lowerQuery = query.toLowerCase();
            List<Recipe> filteredResults = allRecipes.stream()
                    .filter(recipe -> matchesSearchQuery(recipe, lowerQuery))
                    .collect(Collectors.toList());
            searchResultsLiveData.postValue(filteredResults);
        });
    }

    public void filterRecipesByCategory(String filterKey, String filterType, MutableLiveData<List<Recipe>> filteredResultsLiveData) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            List<Recipe> filteredResults = localRepository.getRecipesByCategory(filterKey, filterType);
            filteredResultsLiveData.postValue(filteredResults);
        });
    }

    private boolean matchesSearchQuery(Recipe recipe, String query) {
        if (recipe.getTitle() != null && recipe.getTitle().toLowerCase().contains(query)) return true;
        if (recipe.getIngredients() != null) {
            for (Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient.getName() != null && ingredient.getName().toLowerCase().contains(query)) return true;
            }
        }
        if (recipe.getSteps() != null) {
            for (Step step : recipe.getSteps()) {
                if (step.getInstruction() != null && step.getInstruction().toLowerCase().contains(query)) return true;
            }
        }
        return false;
    }



    public void saveRecipe(Recipe recipe, byte[] imageBytes, RecipeCallback<Recipe> callback) {
        remoteRepository.saveRecipe(recipe, imageBytes, new RecipeRemoteRepository.RecipeSaveCallback() {
            @Override
            public void onSuccess(GeneralServerResponse response, Recipe savedRecipe) {
                // После успешного сохранения на сервере, сохраняем в локальную БД
                if (response != null && response.getId() != null) {
                    savedRecipe.setId(response.getId());
                } else {
                    Log.w(TAG, "Ответ сервера не содержит ID рецепта");
                }
                
                // Устанавливаем URL изображения из ответа сервера
                if (response != null && response.getPhotoUrl() != null) {
                    savedRecipe.setPhoto_url(response.getPhotoUrl());
                    Log.d(TAG, "URL изображения получен от сервера: " + response.getPhotoUrl());
                } else {
                    Log.w(TAG, "Ответ сервера не содержит URL изображения");
                }
                
                AppExecutors.getInstance().diskIO().execute(() -> {
                    localRepository.insert(savedRecipe);
                    mainThreadHandler.post(() -> callback.onSuccess(savedRecipe));
                });
            }

            @Override
            public void onFailure(String error, GeneralServerResponse errorResponse) {
                callback.onFailure(error);
            }
        });
    }

    public void updateRecipe(Recipe recipe, byte[] imageBytes, RecipeCallback<Recipe> callback) {
        remoteRepository.updateRecipe(recipe, imageBytes, new RecipeRemoteRepository.RecipeSaveCallback() {
            @Override
            public void onSuccess(GeneralServerResponse response, Recipe updatedRecipe) {
                // Используем обновленный рецепт от сервера вместо исходного
                Recipe recipeToSave = updatedRecipe;
                
                // Обновляем URL изображения если получен новый в response
                if(response != null && response.getPhotoUrl() != null){
                    recipeToSave.setPhoto_url(response.getPhotoUrl());
                    Log.d(TAG, "URL изображения обновлен от сервера: " + response.getPhotoUrl());
                } else {
                    Log.d(TAG, "Сервер не вернул новый URL изображения при обновлении рецепта");
                }
                AppExecutors.getInstance().diskIO().execute(() -> {
                    localRepository.update(recipeToSave);
                    mainThreadHandler.post(() -> callback.onSuccess(recipeToSave));
                });
            }

            @Override
            public void onFailure(String error, GeneralServerResponse errorResponse) {
                callback.onFailure(error);
            }
        });
    }
    
    public void deleteRecipe(int recipeId, RecipeCallback<Void> callback) {
        remoteRepository.deleteRecipe(recipeId, new RecipeRemoteRepository.DeleteRecipeCallback() {
            @Override
            public void onDeleteSuccess() {
                AppExecutors.getInstance().diskIO().execute(() -> {
                    localRepository.deleteRecipe(recipeId);
                    mainThreadHandler.post(() -> callback.onSuccess(null));
                });
            }
            @Override
            public void onDeleteFailure(String error) {
                callback.onFailure(error);
            }
        });
    }

    public void setLikeStatus(int recipeId, boolean isLiked) {
        AppExecutors.getInstance().diskIO().execute(() -> {
            localRepository.updateLikeStatus(recipeId, isLiked);
            likedRecipesRepository.updateLikeStatusLocal(recipeId, isLiked);
            // Отправляем запрос на сервер, но не ждем ответа (fire and forget)
            likedRecipesRepository.toggleLikeRecipeOnServer(recipeId)
                .subscribeOn(Schedulers.io())
                .subscribe(() -> {}, throwable -> Log.e(TAG, "Failed to toggle like on server", throwable));
        });
    }

    public boolean isNetworkAvailable() {
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    public void clearDisposables() {
        disposables.clear();
    }

    public Set<Integer> getLikedRecipeIds() {
        return new HashSet<>(likedRecipesRepository.getLikedRecipeIdsSync());
    }

    public Recipe getRecipeByIdSync(int recipeId) {
        return localRepository.getRecipeByIdSync(recipeId);
    }

    public void clearAllCaches() {
        localRepository.clearAllCaches();
        Log.d(TAG, "Все кэши UnifiedRecipeRepository очищены");
    }
}
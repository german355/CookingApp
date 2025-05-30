package com.example.cooking.ui.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.fragment.app.FragmentActivity;

import com.example.cooking.Recipe.Recipe;
import com.example.cooking.data.repositories.LikedRecipesRepository;

import com.example.cooking.network.utils.Resource;
import com.example.cooking.utils.MySharedPreferences;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.lang.reflect.Field;

import com.example.cooking.BuildConfig;

/**
 * ViewModel для HomeFragment
 */
public class HomeViewModel extends AndroidViewModel {
    
    private static final String TAG = "HomeViewModel";
    
    private final SharedRecipeViewModel sharedRecipeViewModel;
    private final LikedRecipesRepository likedRecipesRepository;
    private final ExecutorService executor;
    
    // LiveData для состояния загрузки и ошибок
    private final MutableLiveData<Boolean> isRefreshing = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
    // LiveData для результатов поиска
    private final MutableLiveData<List<Recipe>> searchResults = new MutableLiveData<>();
    
    // Флаг, указывающий, что мы находимся в режиме поиска
    private boolean isInSearchMode = false;
    // Последний поисковый запрос для возможности восстановления
    private String lastSearchQuery = "";
    
    // LiveData для списка рецептов
    private final MutableLiveData<List<Recipe>> recipesLiveData = new MutableLiveData<>();

    private final Observer<Resource<List<Recipe>>> recipesObserver;
    private final Observer<Boolean> refreshingObserver;
    private final Observer<String> sharedErrorObserver;
    private final Observer<List<Recipe>> searchResultsObserver;

    public HomeViewModel(@NonNull Application application) {
        super(application);
        this.likedRecipesRepository = new LikedRecipesRepository(application);
        this.executor = Executors.newFixedThreadPool(2);
        
        // init observer fields
        this.recipesObserver = resource -> {
            if (resource.getStatus() == Resource.Status.SUCCESS && resource.getData() != null) {
                recipesLiveData.postValue(resource.getData());
            } else if (resource.getStatus() == Resource.Status.ERROR) {
                errorMessage.postValue(resource.getMessage());
            }
        };
        this.refreshingObserver = isLoading -> isRefreshing.postValue(isLoading);
        this.sharedErrorObserver = error -> {
            if (error != null && !error.isEmpty()) {
                errorMessage.postValue(error);
            }
        };
        this.searchResultsObserver = recipes -> {
            Log.d(TAG, "---> searchResultsObserver вызван с " + (recipes != null ? recipes.size() : "null") + " рецептами");
            if (recipes != null) {
                Log.d(TAG, "Получены результаты поиска в HomeViewModel: " + recipes.size() + " рецептов");
                if (!recipes.isEmpty()) {
                    Log.d(TAG, "Первый рецепт в результатах: " + recipes.get(0).getTitle() + " (ID: " + recipes.get(0).getId() + ")");
                }
                Log.d(TAG, "searchResults LiveData до обновления: " + (searchResults.getValue() != null ? searchResults.getValue().size() : "null") + " рецептов");
                searchResults.postValue(recipes);
                Log.d(TAG, "Результаты поиска отправлены в LiveData searchResults");
                
                if (BuildConfig.DEBUG) {
                    try {
                        java.lang.reflect.Field observersField = androidx.lifecycle.LiveData.class.getDeclaredField("mObservers");
                        observersField.setAccessible(true);
                        Object observers = observersField.get(searchResults);
                        if (observers != null) {
                            int size = ((java.util.Map<?, ?>) observers).size();
                            Log.d(TAG, "Количество наблюдателей для searchResults: " + size);
                        } else {
                            Log.w(TAG, "Наблюдатели для searchResults отсутствуют (observers == null)");
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка при проверке наблюдателей: " + e.getMessage());
                    }
                }
            } else {
                Log.w(TAG, "Получен нулевой список результатов поиска");
            }
        };
        
        // Инициализируем SharedRecipeViewModel
        sharedRecipeViewModel = new ViewModelProvider.AndroidViewModelFactory(application)
            .create(SharedRecipeViewModel.class);
            
        // Подписываемся на обновления рецептов
        sharedRecipeViewModel.getRecipes().observeForever(recipesObserver);
        
        // Подписываемся на состояние загрузки
        sharedRecipeViewModel.getIsRefreshing().observeForever(refreshingObserver);
        
        // Подписываемся на ошибки
        sharedRecipeViewModel.getErrorMessage().observeForever(sharedErrorObserver);
        
        // Подписываемся на результаты поиска (чтобы HomeFragment сразу получал данные)
        sharedRecipeViewModel.getSearchResults().observeForever(searchResultsObserver);
    }
    
    // Поле для хранения LikeSyncViewModel
    private LikeSyncViewModel likeSyncViewModel;
    // Поле для хранения последнего обработанного события лайка
    private LikeSyncViewModel.LikeEvent lastProcessedLikeEvent;
    
    // Метод для инициализации наблюдения за Shared ViewModel. Вызывается из Фрагмента.
    public void observeLikeChanges(LifecycleOwner owner, FragmentActivity activity) {
        if (activity != null) {
            // Инициализируем Shared ViewModel здесь, используя Activity scope
            likeSyncViewModel = new ViewModelProvider(activity).get(LikeSyncViewModel.class);

            likeSyncViewModel.getLikeChangeEvent().observe(owner, event -> {
                if (event != null && !event.equals(lastProcessedLikeEvent)) {
                    Log.d(TAG, "Received like change event from LikeSyncViewModel: " + event.getRecipeId() + " -> " + event.isLiked());
                    // Добавляем логирование перед обновлением
                    Log.i(TAG, "[LikeSync] Updating local statuses for Recipe ID: " + event.getRecipeId() + " to liked: " + event.isLiked());
                    // Обновляем статус лайка в ЛОКАЛЬНОЙ БД ОСНОВНЫХ рецептов
                    updateLocalLikeStatus(event.getRecipeId(), event.isLiked());
                    // Обновляем статус лайка в ЛОКАЛЬНОЙ БД ЛАЙКНУТЫХ рецептов
                    updateLikedRepositoryStatus(event.getRecipeId(), event.isLiked());
                    // Мы не сохраняем это событие как lastProcessed, так как обновление локальной БД
                    // должно быть идемпотентным.
                    lastProcessedLikeEvent = event;
                } else if (event != null) {
                    Log.d(TAG, "[LikeSync] Ignored duplicate/own like event: " + event.getRecipeId() + " -> " + event.isLiked());
                }
            });
        }
    }
    
    /**
     * Получить LiveData со списком рецептов из локального хранилища
     */
    public LiveData<List<Recipe>> getRecipes() {
        return recipesLiveData;
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
        sharedRecipeViewModel.loadInitialRecipesIfNeeded();
    }
    
    /**
     * Загрузить рецепты с сервера и обновить локальную базу данных.
     * НЕ очищает базу данных перед вставкой.
     * Использует OnConflictStrategy.REPLACE в DAO для обновления существующих записей.
     */
    public void refreshRecipes() {
        sharedRecipeViewModel.refreshRecipes();
    }
    
    /**
     * Обновить состояние лайка для рецепта.
     * Обновляет локальную БД и оповещает другие компоненты через Shared ViewModel.
     * @param recipe рецепт
     * @param isLiked новое состояние лайка
     */
    public void updateLikeStatus(Recipe recipe, boolean isLiked) {
        String userId = new MySharedPreferences(getApplication()).getString("userId", "0");
        if (userId.equals("0")) {
            errorMessage.postValue("Войдите, чтобы добавить рецепт в избранное");
            return;
        }
        
        // Обновляем статус через SharedRecipeViewModel
        sharedRecipeViewModel.updateLikeStatus(recipe, isLiked, userId);
        
        // Обновляем локальное состояние
        recipe.setLiked(isLiked);
        updateLocalRecipeLikeStatus(recipe.getId(), isLiked);
    }
    
    /**
     * Обновляет статус лайка в локальной БД ОСНОВНЫХ рецептов (RecipeEntity).
     */
    public void updateLocalLikeStatus(int recipeId, boolean isLiked) {
        executeIfActive(() -> {
            try {
                Log.d(TAG, "Updating like status in local REPOSITORY for recipe " + recipeId + " to " + isLiked);
                // Обновляем статус лайка через SharedRecipeViewModel
                String userId = new MySharedPreferences(getApplication()).getString("userId", "0");
                Recipe recipe = null;
                List<Recipe> currentRecipes = recipesLiveData.getValue();
                if (currentRecipes != null) {
                    for (Recipe r : currentRecipes) {
                        if (r.getId() == recipeId) {
                            recipe = r;
                            break;
                        }
                    }
                }
                if (recipe != null) {
                    sharedRecipeViewModel.updateLikeStatus(recipe, isLiked, userId);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error updating like status in local repository: " + e.getMessage(), e);
            }
        });
    }
    
    /**
     * Обновляет статус лайка в репозитории
     * @param recipeId ID рецепта
     * @param isLiked новое состояние лайка
     */
    public void updateLikedRepositoryStatus(int recipeId, boolean isLiked) {
        String userId = new MySharedPreferences(getApplication()).getString("userId", "0");
        Recipe recipe = null;
        List<Recipe> currentRecipes = recipesLiveData.getValue();
        if (currentRecipes != null) {
            for (Recipe r : currentRecipes) {
                if (r.getId() == recipeId) {
                    recipe = r;
                    break;
                }
            }
        }
        
        if (recipe != null) {
            updateLikeStatus(recipe, isLiked);
        } else {
            Log.e(TAG, "Recipe not found with id: " + recipeId);
        }
    }

    private void updateLocalRecipeLikeStatus(int recipeId, boolean isLiked) {
        // Обновляем локальное состояние рецепта в списке
        List<Recipe> currentRecipes = recipesLiveData.getValue();
        if (currentRecipes != null) {
            for (Recipe recipe : currentRecipes) {
                if (recipe.getId() == recipeId) {
                    recipe.setLiked(isLiked);
                    break;
                }
            }
            recipesLiveData.postValue(currentRecipes);
        }
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
        sharedRecipeViewModel.getRecipes().removeObserver(recipesObserver);
        sharedRecipeViewModel.getIsRefreshing().removeObserver(refreshingObserver);
        sharedRecipeViewModel.getErrorMessage().removeObserver(sharedErrorObserver);
        sharedRecipeViewModel.getSearchResults().removeObserver(searchResultsObserver);
        Log.d(TAG, "HomeViewModel cleared.");
    }





/**
 * Выполняет поиск рецептов по указанному запросу
 * @param query Строка поиска
 */
public void searchRecipes(String query) {
    Log.d(TAG, "Выполняю поиск рецептов: " + query);
    
    // Запоминаем последний запрос и устанавливаем режим поиска
    lastSearchQuery = query;
    isInSearchMode = true;
    
    // Инициируем поиск через SharedRecipeViewModel
    sharedRecipeViewModel.searchRecipes(query);
}

/**
 * Проверяет, находимся ли мы в режиме поиска
 * @return true если в режиме поиска, иначе false
 */
public boolean isInSearchMode() {
    return isInSearchMode;
}

/**
 * Устанавливает режим поиска
 * @param inSearchMode значение режима поиска
 */
public void setInSearchMode(boolean inSearchMode) {
    isInSearchMode = inSearchMode;
}

/**
 * Возвращает последний поисковый запрос
 * @return строка последнего поиска
 */
public String getLastSearchQuery() {
    return lastSearchQuery;
}

/**
 * Восстанавливает последний поиск
 */
public void restoreLastSearch() {
    if (isInSearchMode && !lastSearchQuery.isEmpty()) {
        Log.d(TAG, "Восстанавливаю последний поиск: " + lastSearchQuery);
        searchRecipes(lastSearchQuery);
    }
}
} 
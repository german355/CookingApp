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
            if (recipes != null) {
                searchResults.postValue(recipes);
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

    // Поле для хранения последнего обработанного события лайка
    private LikeSyncViewModel.LikeEvent lastProcessedLikeEvent;
    


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
     * Обновляет статус лайка в репозитории
     * @param recipeId ID рецепта
     * @param isLiked новое состояние лайка
     */
    public void updateLikedRepositoryStatus(int recipeId, boolean isLiked) {
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
    }





/**
 * Выполняет поиск рецептов по указанному запросу
 * @param query Строка поиска
 */
public void searchRecipes(String query) {
    Log.d(TAG, "HomeViewModel.searchRecipes called with query: '" + query + "', isInSearchMode set to true");
    // Запоминаем последний запрос и устанавливаем режим поиска
    lastSearchQuery = query;
    isInSearchMode = true;
    Log.d(TAG, "HomeViewModel.lastSearchQuery='" + lastSearchQuery + "'");
    
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
        searchRecipes(lastSearchQuery);
    }
}
} 
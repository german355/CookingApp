package com.example.cooking.ui.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.cooking.Recipe.Recipe;
import com.example.cooking.data.repositories.RecipeRepository;
import com.example.cooking.data.repositories.UserRepository;
import com.example.cooking.network.utils.Resource;

import java.util.List;

/**
 * ViewModel для отображения списка рецептов
 */
public class RecipeListViewModel extends AndroidViewModel {
    private static final String TAG = "RecipeListViewModel";

    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;

    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    private final MutableLiveData<Boolean> isRefreshing = new MutableLiveData<>(false);
    private final MutableLiveData<String> filterType = new MutableLiveData<>("all");

    // Медиатор для объединения источников данных
    private final MediatorLiveData<Resource<List<Recipe>>> recipes = new MediatorLiveData<>();

    // Последний загруженный источник
    private LiveData<Resource<List<Recipe>>> currentSource;

    /**
     * Конструктор ViewModel
     * @param application приложение
     */
    public RecipeListViewModel(@NonNull Application application) {
        super(application);
        recipeRepository = new RecipeRepository(application.getApplicationContext());
        userRepository = new UserRepository(application.getApplicationContext());
        
        // Загрузка рецептов при создании ViewModel
        loadRecipes();
    }

    /**
     * Загружает список рецептов
     */
    public void loadRecipes() {
        isRefreshing.setValue(true);
        
        // Если у нас есть текущий источник, удаляем его наблюдателя
        if (currentSource != null) {
            recipes.removeSource(currentSource);
        }
        
        String userId = userRepository.getCurrentUserId();
        String currentFilter = filterType.getValue();
        
        if (currentFilter != null && currentFilter.equals("liked") && userId != null) {
            // Получаем список лайкнутых рецептов
            currentSource = recipeRepository.getLikedRecipes(userId);
        } else {
            // Получаем все рецепты
            currentSource = recipeRepository.getRecipes(userId);
        }
        
        recipes.addSource(currentSource, resource -> {
            if (resource.getStatus() != Resource.Status.LOADING || resource.getData() != null) {
                isRefreshing.setValue(false);
            }
            recipes.setValue(resource);
        });
    }
    
    /**
     * Выполняет поиск рецептов
     * @param query строка поиска
     */
    public void searchRecipes(String query) {
        searchQuery.setValue(query);
        if (query == null || query.trim().isEmpty()) {
            loadRecipes();
            return;
        }
        
        isRefreshing.setValue(true);
        
        // Если у нас есть текущий источник, удаляем его наблюдателя
        if (currentSource != null) {
            recipes.removeSource(currentSource);
        }
        
        String userId = userRepository.getCurrentUserId();
        // Поиск рецептов
        currentSource = recipeRepository.searchRecipes(query, userId, 1, 20);
        
        recipes.addSource(currentSource, resource -> {
            isRefreshing.setValue(false);
            recipes.setValue(resource);
        });
    }
    
    /**
     * Устанавливает тип фильтрации рецептов
     * @param type тип фильтра ("all", "liked")
     */
    public void setFilterType(String type) {
        String currentType = filterType.getValue();
        if (currentType != null && currentType.equals(type)) {
            return; // Тот же фильтр, ничего не делаем
        }
        filterType.setValue(type);
        loadRecipes(); // Перезагружаем с новым фильтром
    }
    
    /**
     * Обновляет список рецептов
     */
    public void refreshRecipes() {
        loadRecipes();
    }
    
    /**
     * Очищает кэш рецептов
     */
    public void clearCache() {
        recipeRepository.clearRecipesCache();
    }

    // Геттеры для LiveData
    public LiveData<Resource<List<Recipe>>> getRecipes() {
        return recipes;
    }

    public LiveData<Boolean> isRefreshing() {
        return isRefreshing;
    }
    
    public LiveData<String> getFilterType() {
        return filterType;
    }
    
    public LiveData<String> getSearchQuery() {
        return searchQuery;
    }
} 
package com.example.cooking.ui.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.ViewModelProvider;

import com.example.cooking.Recipe.Ingredient;
import com.example.cooking.Recipe.Recipe;
import com.example.cooking.data.repositories.LikedRecipesRepository;
import com.example.cooking.network.utils.Resource;
import com.example.cooking.utils.MySharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * ViewModel для экрана избранных рецептов.
 * Работает с SharedRecipeViewModel для доступа к данным о рецептах.
 */
public class FavoritesViewModel extends AndroidViewModel {
    private static final String TAG = "FavoritesViewModel";
    // Ссылка на SharedRecipeViewModel
    private SharedRecipeViewModel sharedRecipeViewModel;
    
    // LiveData для состояния загрузки и ошибок
    private final MutableLiveData<Boolean> isRefreshing = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
    // LiveData для поискового запроса
    private final MutableLiveData<String> searchQuery = new MutableLiveData<>("");
    
    // LiveData для фильтрованного списка избранных рецептов
    private final MediatorLiveData<List<Recipe>> filteredLikedRecipes = new MediatorLiveData<>();
    
    // Информация о пользователе
    private final String userId;
    private final boolean isUserLoggedIn;
    
    // Флаг, что источник SharedRecipeViewModel уже добавлен
    private boolean isSharedObserverSet = false;
    
    // Флаг, что источник searchQuery уже добавлен
    private boolean isSearchQueryObserverSet = false;
    
    // Репозиторий для получение фактического списка лайков из БД
    private final LikedRecipesRepository likedRecipesRepository;
    
    // Исполнитель для фоновых запросов к БД (избегаем IllegalStateException)
    private final java.util.concurrent.ExecutorService ioExecutor = java.util.concurrent.Executors.newSingleThreadExecutor();
    
    public FavoritesViewModel(@NonNull Application application) {
        super(application);

        // Получаем ID пользователя
        MySharedPreferences preferences = new MySharedPreferences(application);
        userId = preferences.getString("userId", "0");
        isUserLoggedIn = !userId.equals("0");
        
        // Репозиторий лайков
        likedRecipesRepository = new LikedRecipesRepository(application);
        
        // Устанавливаем начальное значение
        filteredLikedRecipes.setValue(new ArrayList<>());
    }
    
    /**
     * Устанавливает SharedRecipeViewModel.
     * Вызывается из фрагмента после создания ViewModel.
     */
    public void setSharedRecipeViewModel(SharedRecipeViewModel viewModel) {
        this.sharedRecipeViewModel = viewModel;
        
        setupSharedViewModelObserver();
        
        // Настраиваем наблюдение за поисковым запросом
        setupSearchQueryObserver();
    }
    
    /**
     * Настраивает наблюдение за данными из SharedRecipeViewModel
     */
    private void setupSharedViewModelObserver() {
        // Проверяем, что источник ещё не добавлен и viewModel проинициализирована
        if (sharedRecipeViewModel == null || isSharedObserverSet) return;
        isSharedObserverSet = true;
        
        filteredLikedRecipes.addSource(sharedRecipeViewModel.getRecipes(), resource -> {
            if (resource == null || !resource.isSuccess() || resource.getData() == null) {
                Log.d(TAG, "Получены пустые данные из SharedRecipeViewModel");
                return;
            }
            
            List<Recipe> allRecipes = resource.getData();
            
            // Захватываем текущий поисковый запрос на главном потоке, чтобы не обращаться к LiveData из фоновой нити
            final String currentQuery = searchQuery.getValue();

            // Делаем тяжёлую работу в ioExecutor, чтобы не лочить главный поток
            ioExecutor.execute(() -> {
                List<Recipe> likedRecipesLocal = allRecipes.stream()
                        .filter(Recipe::isLiked)
                        .collect(Collectors.toList());

                if (currentQuery != null && !currentQuery.isEmpty()) {
                    likedRecipesLocal = filterRecipesByQuery(likedRecipesLocal, currentQuery);
                }

                Log.d(TAG, "Получено " + likedRecipesLocal.size() + " понравившихся рецептов из общего списка " + allRecipes.size());

                filteredLikedRecipes.postValue(likedRecipesLocal);
            });
        });
    }
    
    /**
     * Настраивает наблюдение за поисковым запросом
     */
    private void setupSearchQueryObserver() {
        // Проверяем, что observer ещё не настроен и viewModel инициализирована
        if (sharedRecipeViewModel == null || isSearchQueryObserverSet) return;
        isSearchQueryObserverSet = true;
        filteredLikedRecipes.addSource(searchQuery, query -> {
            if (sharedRecipeViewModel == null) return;
            
            Resource<List<Recipe>> resource = sharedRecipeViewModel.getRecipes().getValue();
            if (resource == null || !resource.isSuccess() || resource.getData() == null) return;
            
            List<Recipe> allRecipes = resource.getData();
            
            // Захватываем текущий поисковый запрос на главном потоке, чтобы не обращаться к LiveData из фоновой нити
            final String currentQuery = searchQuery.getValue();

            // Делаем тяжёлую работу в ioExecutor, чтобы не лочить главный поток
            ioExecutor.execute(() -> {
                List<Recipe> likedRecipesLocal = allRecipes.stream()
                        .filter(Recipe::isLiked)
                        .collect(Collectors.toList());

                if (currentQuery != null && !currentQuery.isEmpty()) {
                    likedRecipesLocal = filterRecipesByQuery(likedRecipesLocal, currentQuery);
                }

                filteredLikedRecipes.postValue(likedRecipesLocal);
            });
        });
    }
    
    /**
     * Фильтрует рецепты по поисковому запросу
     */
    private List<Recipe> filterRecipesByQuery(List<Recipe> recipes, String query) {
        if (query == null || query.isEmpty()) {
            return recipes;
        }
        
        String lowerQuery = query.toLowerCase();
        
        return recipes.stream()
            .filter(recipe -> {
                // Поиск в названии
                if (recipe.getTitle() != null && recipe.getTitle().toLowerCase().contains(lowerQuery)) {
                    return true;
                }
                
                // Поиск в ингредиентах
                if (recipe.getIngredients() != null) {
                    for (Ingredient ingredient : recipe.getIngredients()) {
                        if (ingredient.getName() != null && ingredient.getName().toLowerCase().contains(lowerQuery)) {
                            return true;
                        }
                    }
                }
                
                return false;
            })
            .collect(Collectors.toList());
    }
    
    /**
     * Обновляет список избранных рецептов
     */
    public void refreshLikedRecipes() {
        if (!isUserLoggedIn) {
            errorMessage.setValue("Необходимо войти в систему для просмотра избранных рецептов");
            return;
        }
        
        isRefreshing.setValue(true);
        
        // Используем SharedRecipeViewModel для обновления данных
        if (sharedRecipeViewModel != null) {
            sharedRecipeViewModel.refreshRecipes();
        }
        
        isRefreshing.setValue(false);
    }
    
    /**
     * Получает LiveData с фильтрованным списком избранных рецептов
     */
    public LiveData<List<Recipe>> getFilteredLikedRecipes() {
        return filteredLikedRecipes;
    }
    
    /**
     * Получает LiveData с состоянием загрузки
     */
    public LiveData<Boolean> getIsRefreshing() {
        return isRefreshing;
    }
    
    /**
     * Получает LiveData с сообщением об ошибке
     */
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * Проверяет, вошел ли пользователь в систему
     */
    public boolean isUserLoggedIn() {
        return isUserLoggedIn;
    }

    
    /**
     * Изменяет статус лайка рецепта
     * @param recipe Рецепт, для которого нужно изменить статус лайка
     * @param isLiked Новый статус лайка
     */
    public void toggleLikeStatus(Recipe recipe, boolean isLiked) {
        if (!isUserLoggedIn) {
            Log.w(TAG, "Попытка изменить статус лайка без авторизации");
            errorMessage.setValue("Необходимо войти в систему для изменения статуса лайка");
            return;
        }
        
        if (recipe == null) {
            Log.e(TAG, "toggleLikeStatus: рецепт равен null");
            return;
        }
        
        sharedRecipeViewModel.updateLikeStatus(recipe, isLiked);

    }
    
    /**
     * Обработка запроса обновления от UI (event-driven)
     */
    public void onRefreshRequested() {
        refreshLikedRecipes();
    }
    
    @Override
    public void onCleared() {
        super.onCleared();
        ioExecutor.shutdown();
    }
}
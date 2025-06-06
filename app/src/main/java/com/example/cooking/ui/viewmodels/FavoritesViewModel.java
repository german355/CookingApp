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
    
    // Репозиторий для работы с избранными рецептами
    private final LikedRecipesRepository repository;
    
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
    
    public FavoritesViewModel(@NonNull Application application) {
        super(application);
        repository = new LikedRecipesRepository(application);
        
        // Получаем ID пользователя
        MySharedPreferences preferences = new MySharedPreferences(application);
        userId = preferences.getString("userId", "0");
        isUserLoggedIn = !userId.equals("0");
        
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
            
            // Фильтруем только понравившиеся рецепты
            List<Recipe> likedRecipes = allRecipes.stream()
                .filter(Recipe::isLiked)
                .collect(Collectors.toList());
            
            Log.d(TAG, "Получено " + likedRecipes.size() + " понравившихся рецептов из общего списка " + allRecipes.size());
            
            // Применяем текущий поисковый запрос
            String query = searchQuery.getValue();
            if (query != null && !query.isEmpty()) {
                likedRecipes = filterRecipesByQuery(likedRecipes, query);
            }
            
            filteredLikedRecipes.setValue(likedRecipes);
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
            
            // Фильтруем только понравившиеся рецепты
            List<Recipe> likedRecipes = allRecipes.stream()
                .filter(Recipe::isLiked)
                .collect(Collectors.toList());
            
            // Применяем поисковый запрос
            if (query != null && !query.isEmpty()) {
                likedRecipes = filterRecipesByQuery(likedRecipes, query);
            }
            
            filteredLikedRecipes.setValue(likedRecipes);
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
     * Выполняет поиск среди избранных рецептов
     */
    public void performSearch(String query) {
        if (query == null || query.isEmpty()) {
            refreshLikedRecipes();
            return;
        }
        searchQuery.setValue(query);
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
     * Обновляет информацию о пользователе и перезагружает данные
     */
    public void updateUser(String newUserId) {
        Log.d(TAG, "Updating user from " + userId + " to " + newUserId);
        
        // Обновляем локальное состояние
        MySharedPreferences preferences = new MySharedPreferences(getApplication());
        preferences.putString("userId", newUserId);
        
        // Если поменялся пользователь, обновляем данные
        if (sharedRecipeViewModel != null) {
            // Используем RefreshRecipes вместо того, чтобы заново устанавливать соединение 
            // с SharedRecipeViewModel - это запустит каскад обновлений через наблюдателей
            sharedRecipeViewModel.refreshRecipes();
        }
        
        // Сообщаем об изменении
        Log.d(TAG, "User updated, refreshing data");
    }
    
    /**
     * Устанавливает наблюдение за изменениями лайков в SharedRecipeViewModel
     */
    public void observeLikeChanges(androidx.lifecycle.LifecycleOwner lifecycleOwner, androidx.fragment.app.FragmentActivity activity) {
        if (sharedRecipeViewModel == null) {
            // Инициализируем SharedRecipeViewModel, если это еще не было сделано
            sharedRecipeViewModel = new ViewModelProvider(activity).get(SharedRecipeViewModel.class);
            
            // Настраиваем наблюдение за данными из SharedRecipeViewModel
            setupSharedViewModelObserver();
            
            // Настраиваем наблюдение за поисковым запросом
            setupSearchQueryObserver();
        }
        
        // Наблюдаем за обновлениями в SharedRecipeViewModel
        sharedRecipeViewModel.getRecipes().observe(lifecycleOwner, resource -> {
            // Обновление уже происходит через наблюдателей, настроенных в setupSharedViewModelObserver()
            // Этот метод просто обеспечивает инициализацию SharedRecipeViewModel и подключение наблюдателей
            Log.d(TAG, "observeLikeChanges: SharedRecipeViewModel обновился");
        });
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
        
        // Обновляем статус лайка в SharedViewModel
        if (sharedRecipeViewModel != null) {
            sharedRecipeViewModel.updateLikeStatus(recipe, isLiked);
        } else {
            Log.e(TAG, "toggleLikeStatus: SharedRecipeViewModel равен null");
            // Если SharedViewModel не инициализирован, обновляем напрямую через репозиторий
            repository.updateLikeStatusLocal(recipe.getId(), isLiked);
        }
    }
    
    /**
     * Обработка запроса обновления от UI (event-driven)
     */
    public void onRefreshRequested() {
        refreshLikedRecipes();
    }
    
    /**
     * Обработка запроса поиска от UI (event-driven)
     */
    public void onSearchRequested(String query) {
        searchQuery.setValue(query);
    }
}
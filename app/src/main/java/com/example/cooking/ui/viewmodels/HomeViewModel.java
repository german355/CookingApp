package com.example.cooking.ui.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cooking.domain.entities.Recipe;
import com.example.cooking.auth.FirebaseAuthManager;
import com.example.cooking.domain.usecases.RecipeDataUseCase;
import com.example.cooking.domain.usecases.RecipeLikeUseCase;
import com.example.cooking.domain.usecases.RecipeSearchUseCase;
import com.example.cooking.network.utils.Resource;
import com.example.cooking.utils.MySharedPreferences;

import java.util.List;

/**
 * ViewModel для HomeFragment.
 * Отвечает за состояние главного экрана, включая список рецептов,
 * статус загрузки и обработку ошибок.
 */
public class HomeViewModel extends AndroidViewModel {
    
    private static final String TAG = "HomeViewModel";

    private final RecipeDataUseCase recipeDataUseCase;
    private final RecipeLikeUseCase recipeLikeUseCase;
    private final RecipeSearchUseCase recipeSearchUseCase;

    // Основной источник данных для UI
    private final MediatorLiveData<Resource<List<Recipe>>> _recipes = new MediatorLiveData<>();
    public final LiveData<Resource<List<Recipe>>> recipes = _recipes;

    // Состояние для Swipe-to-Refresh
    private final MutableLiveData<Boolean> _isRefreshing = new MutableLiveData<>(false);
    public final LiveData<Boolean> isRefreshing = _isRefreshing;
    
    // Сообщение об ошибке для отображения в Snackbar или Toast
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public final LiveData<String> errorMessage = _errorMessage;

    private final MutableLiveData<String> _searchQuery = new MutableLiveData<>("");
    
    // Результаты поиска
    private final MutableLiveData<List<Recipe>> _searchResults = new MutableLiveData<>();

    private boolean isInitialLoadDone = false;
    private volatile boolean isCurrentlyRefreshing = false;
    private final MutableLiveData<Boolean> _isSearchMode = new MutableLiveData<>(false);

    public HomeViewModel(@NonNull Application application) {
        super(application);
        this.recipeDataUseCase = new RecipeDataUseCase(application);
        this.recipeLikeUseCase = new RecipeLikeUseCase(application);
        this.recipeSearchUseCase = new RecipeSearchUseCase(application);

        // Устанавливаем начальное состояние загрузки
        _recipes.setValue(Resource.loading(null));

        // Подписываемся на локальные данные из БД.
        // MediatorLiveData будет автоматически получать обновления.
        LiveData<List<Recipe>> localRecipesSource = recipeDataUseCase.getAllRecipesLocalLiveData();
        
        _recipes.addSource(localRecipesSource, recipesList -> {
            Boolean isSearchMode = _isSearchMode.getValue();
            if (isSearchMode == null || !isSearchMode) {
                // Показываем обычные данные только если не в режиме поиска
                _recipes.setValue(Resource.success(recipesList));
            }
        });

        // Подписываемся на результаты поиска
        _recipes.addSource(_searchResults, searchResults -> {
            Boolean isSearchMode = _isSearchMode.getValue();
            if (isSearchMode != null && isSearchMode) {
                // Показываем результаты поиска только в режиме поиска
                _recipes.setValue(Resource.success(searchResults));
            }
        });
        
        // Подписываемся на изменение режима поиска
        _recipes.addSource(_isSearchMode, isSearchMode -> {
            if (isSearchMode != null && !isSearchMode) {
                // Выходим из режима поиска - показываем локальные данные
                List<Recipe> localRecipes = localRecipesSource.getValue();
                if (localRecipes != null) {
                    _recipes.setValue(Resource.success(localRecipes));
                }
            } else if (isSearchMode != null && isSearchMode) {
                // Входим в режим поиска - показываем результаты поиска если есть
                List<Recipe> searchResults = _searchResults.getValue();
                if (searchResults != null) {
                    _recipes.setValue(Resource.success(searchResults));
                }
            }
        });

        // Запускаем первичную загрузку данных
        loadInitialRecipes();
    }

    /**
     * Запускает первичную загрузку данных, если она еще не была выполнена.
     */
    public synchronized void loadInitialRecipes() {
        if (!isInitialLoadDone) {
            isInitialLoadDone = true;
            refreshRecipes();
        }
    }

    /**
     * Запускает принудительное обновление данных с сервера.
     * Используется для Swipe-to-Refresh.
     */
    public synchronized void refreshRecipes() {
        if (isCurrentlyRefreshing) {
            Log.d(TAG, "Обновление уже выполняется, пропускаем.");
            return;
        }
        isCurrentlyRefreshing = true;
        _isRefreshing.setValue(true);

        recipeDataUseCase.refreshRecipes(_isRefreshing, _errorMessage, _recipes, () -> {
            isCurrentlyRefreshing = false;
        });
    }

    /**
     * Обновить состояние лайка для рецепта.
     * @param recipe рецепт
     * @param isLiked новое состояние лайка
     */
    public void updateLikeStatus(Recipe recipe, boolean isLiked) {
        String userId = new MySharedPreferences(getApplication()).getString("userId", "0");
        if (!FirebaseAuthManager.getInstance().isUserSignedIn() || "0".equals(userId)) {
            _errorMessage.setValue("Необходимо войти в аккаунт, чтобы ставить лайки");
            return;
        }
        
        // Оптимистичное обновление UI не требуется,
        recipeLikeUseCase.setLikeStatus(userId, recipe.getId(), isLiked, _errorMessage);
    }

    public void performSearch(String query) {
        Log.d(TAG, "performSearch called with query: '" + query + "'");
        _searchQuery.setValue(query);
        
        if (query == null || query.trim().isEmpty()) {
            // Пустой запрос - выходим из режима поиска и показываем все рецепты
            Log.d(TAG, "Пустой запрос, выходим из режима поиска");
            _isSearchMode.setValue(false);
            _searchResults.setValue(null); // Очищаем результаты поиска
            return;
        }
        
        // Переходим в режим поиска
        _isSearchMode.setValue(true);
        Log.d(TAG, "Переходим в режим поиска");
        
        // Проверяем настройки умного поиска
        MySharedPreferences preferences = new MySharedPreferences(getApplication());
        boolean smartSearchEnabled = preferences.getBoolean("smart_search_enabled", true);
        Log.d(TAG, "Smart search enabled: " + smartSearchEnabled);
        
        // Выполняем поиск через UseCase
        recipeSearchUseCase.searchRecipes(query.trim(), smartSearchEnabled, _searchResults, _errorMessage, _isRefreshing);
    }


    
    /**
     * Очистить ресурсы
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        recipeDataUseCase.clearResources();
        recipeLikeUseCase.clearResources();
        recipeSearchUseCase.clearResources();
    }
} 
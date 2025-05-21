package com.example.cooking.data.repositories;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import com.example.cooking.Recipe.Ingredient;
import com.example.cooking.Recipe.Recipe;
import com.example.cooking.Recipe.Step;
import com.example.cooking.config.ServerConfig;
import com.example.cooking.network.api.ApiService;
import com.example.cooking.network.responses.RecipesResponse;
import com.example.cooking.network.responses.SearchResponse;
import com.example.cooking.network.services.NetworkService;
import com.example.cooking.network.utils.ApiCallHandler;
import com.example.cooking.network.utils.Resource;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import retrofit2.Call;

import com.example.cooking.utils.MySharedPreferences;
import com.example.cooking.data.repositories.RecipeLocalRepository;

/**
 * Репозиторий для управления данными рецептов
 * Использует шаблон repository для абстрагирования источников данных
 */
public class RecipeRepository extends NetworkRepository {
    private static final String TAG = "RecipeRepository";

    private final RecipeLocalRepository localRepository;
    private final MySharedPreferences preferences;

    /**
     * Интерфейс для обратного вызова загрузки рецепта
     */
    public interface RecipeCallback {
        void onRecipeLoaded(Recipe recipe);
        void onDataNotAvailable(String error);
    }

    /**
     * Конструктор репозитория
     * @param context контекст приложения
     */
    public RecipeRepository(Context context) {
        super(context);
        this.preferences = new MySharedPreferences(context);
        this.localRepository = new RecipeLocalRepository(context);
    }

    /**
     * Получает список всех рецептов
     * @param userId ID пользователя (опционально)
     * @return LiveData c Resource<List<Recipe>>
     */
    public LiveData<Resource<List<Recipe>>> getRecipes(String userId) {
        return getRecipesInternal(userId, true);
    }

    /**
     * Получает список всех рецептов без кэширования
     * @param userId ID пользователя (опционально)
     * @return LiveData c Resource<List<Recipe>>
     */
    public LiveData<Resource<List<Recipe>>> getRecipesNoCache(String userId) {
        return getRecipesInternal(userId, false);
    }

    /**
     * Внутренний метод для получения рецептов с возможностью кэширования
     */
    private LiveData<Resource<List<Recipe>>> getRecipesInternal(String userId, boolean useCache) {
        MediatorLiveData<Resource<List<Recipe>>> result = new MediatorLiveData<>();
        result.setValue(Resource.loading(null));

        // Если нужно кэширование, сначала пробуем загрузить из локальной базы
        if (useCache) {
            LiveData<List<Recipe>> dbSource = localRepository.getAllRecipes();
            result.addSource(dbSource, recipeList -> {
                result.removeSource(dbSource);
                
                if (recipeList != null && !recipeList.isEmpty()) {
                    // Отображаем данные из кэша пока загружаем с сервера
                    result.setValue(Resource.loading(recipeList));
                }
                
                // В любом случае делаем запрос к серверу
                fetchRecipesFromApi(userId, result, useCache);
            });
        } else {
            // Если кэширование не требуется, сразу запрашиваем с сервера
            fetchRecipesFromApi(userId, result, false);
        }

        return result;
    }

    /**
     * Получает рецепты с сервера
     */
    private void fetchRecipesFromApi(String userId, 
                                    MediatorLiveData<Resource<List<Recipe>>> result,
                                    boolean shouldCache) {
        LiveData<Resource<RecipesResponse>> apiResponse = 
                ApiCallHandler.asLiveData(apiService.getRecipes());
                
        result.addSource(apiResponse, resource -> {
            result.removeSource(apiResponse);
            
            if (resource.isSuccess()) {
                RecipesResponse response = resource.getData();
                if (response != null && response.getRecipes() != null) {
                    List<Recipe> recipes = response.getRecipes();
                    
                    if (shouldCache) {
                        // Сохраняем в локальную БД для оффлайн доступа
                        executeInBackground(() -> localRepository.insertAll(recipes));
                    }
                    
                    result.setValue(Resource.success(recipes));
                } else {
                    result.setValue(Resource.error(
                        "Сервер вернул пустой список рецептов", null));
                }
            } else if (resource.isError()) {
                // Если не удалось получить с сервера и у нас уже есть кэш, оставляем его
                if (result.getValue() != null && 
                    result.getValue().isLoading() && 
                    result.getValue().getData() != null) {
                    // Переводим в статус ошибки, но сохраняем данные из кэша
                    result.setValue(Resource.error(
                        "Ошибка сети: " + resource.getMessage(), 
                        result.getValue().getData()));
                } else {
                    result.setValue(Resource.error(resource.getMessage(), null));
                }
            }
        });
    }

    /**
     * Выполняет поиск рецептов
     * @param query поисковый запрос
     * @param userId ID пользователя (опционально)
     * @param page страница (начиная с 1)
     * @param perPage количество рецептов на странице
     * @return LiveData с результатом поиска
     */
    public LiveData<Resource<List<Recipe>>> searchRecipes(String query, String userId, 
                                                        int page, int perPage) {
        MutableLiveData<Resource<List<Recipe>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));
        
        LiveData<Resource<SearchResponse>> apiResponse = 
                ApiCallHandler.asLiveData(apiService.searchRecipes(query, userId, page, perPage));
                
        Observer<Resource<SearchResponse>> observer = new Observer<Resource<SearchResponse>>() {
            @Override
            public void onChanged(Resource<SearchResponse> resource) {
                apiResponse.removeObserver(this);
                
                if (resource.isSuccess()) {
                    SearchResponse response = resource.getData();
                    if (response != null) {
                        List<Recipe> recipes = response.getResults();
                        result.setValue(Resource.success(recipes));
        } else {
                        result.setValue(Resource.error(
                            "Не удалось получить результаты поиска", null));
                    }
                } else if (resource.isError()) {
                    result.setValue(Resource.error(resource.getMessage(), null));
                }
            }
        };
        
        apiResponse.observeForever(observer);
        
        return result;
    }

    /**
     * Получает список лайкнутых рецептов пользователя
     * @param userId ID пользователя
     * @return LiveData с результатом
     */
    public LiveData<Resource<List<Recipe>>> getLikedRecipes(String userId) {
        MutableLiveData<Resource<List<Recipe>>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));
        
        LiveData<Resource<RecipesResponse>> apiResponse = 
                ApiCallHandler.asLiveData(apiService.getLikedRecipes(userId));
                
        Observer<Resource<RecipesResponse>> observer = new Observer<Resource<RecipesResponse>>() {
            @Override
            public void onChanged(Resource<RecipesResponse> resource) {
                apiResponse.removeObserver(this);
                
                if (resource.isSuccess()) {
                    RecipesResponse response = resource.getData();
                    if (response != null) {
                        List<Recipe> recipes = response.getRecipes();
                        result.setValue(Resource.success(recipes));
                    } else {
                        result.setValue(Resource.error(
                            "Не удалось получить список лайкнутых рецептов", null));
                    }
                } else if (resource.isError()) {
                    result.setValue(Resource.error(resource.getMessage(), null));
                }
            }
        };
        
        apiResponse.observeForever(observer);
        
        return result;
    }

    /**
     * Очищает кэш рецептов
     */
    public void clearRecipesCache() {
        executeInBackground(() -> localRepository.clearAll());
    }

    /**
     * Загружает конкретный рецепт по его ID
     * @param recipeId идентификатор рецепта
     * @param callback интерфейс обратного вызова для результата
     */
    public void loadRecipeFromServer(int recipeId, final RecipeCallback callback) {
        getRecipes(null).observeForever(new Observer<Resource<List<Recipe>>>() {
            @Override
            public void onChanged(Resource<List<Recipe>> resource) {
                if (resource.isSuccess()) {
                    List<Recipe> recipes = resource.getData();
                if (recipes != null) {
                        for (Recipe recipe : recipes) {
                            if (recipe.getId() == recipeId) {
                                callback.onRecipeLoaded(recipe);
                                return;
                            }
                        }
                    }
                    callback.onDataNotAvailable("Рецепт с ID " + recipeId + " не найден");
                } else {
                    callback.onDataNotAvailable(resource.getMessage());
                }
            }
        });
    }

    /**
     * Проверяет доступность сети
     */
    protected boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager != null ? connectivityManager.getActiveNetworkInfo() : null;
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
package com.example.cooking.data.repositories;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.example.cooking.Recipe.Recipe;
import com.example.cooking.config.ServerConfig;
import com.example.cooking.network.api.ApiService;
import com.example.cooking.network.responses.RecipesResponse;
import com.example.cooking.network.services.NetworkService;
import com.example.cooking.network.utils.ApiCallHandler;
import com.example.cooking.utils.MySharedPreferences;

import java.util.List;

import retrofit2.Call;

/**
 * Репозиторий для работы с удаленным API рецептов
 */
public class RecipeRemoteRepository extends NetworkRepository {

    private static final String TAG = "RecipeRemoteRepository";

    private final MySharedPreferences preferences;
    
    // Флаг для предотвращения множественных одновременных запросов
    private volatile boolean isRequestInProgress = false;

    public interface RecipesCallback {
        void onRecipesLoaded(List<Recipe> recipes);
        void onDataNotAvailable(String error);
    }

    public RecipeRemoteRepository(Context context) {
        super(context);
        this.preferences = new MySharedPreferences(context);
    }

    /**
     * Получить рецепты с сервера
     * 
     * @param callback callback для возврата результата
     */
    public synchronized void getRecipes(final RecipesCallback callback) {
        // Проверяем, не выполняется ли уже запрос
        if (isRequestInProgress) {
            Log.d(TAG, "Запрос рецептов уже выполняется, пропускаем дублирующий запрос");
            callback.onDataNotAvailable("Запрос уже выполняется");
            return;
        }
        
        // Проверяем доступность сети
        if (!isNetworkAvailable()) {
            Log.d(TAG, "Сеть недоступна, отменяем запрос рецептов");
            callback.onDataNotAvailable("Нет подключения к интернету");
            return;
        }

        // Устанавливаем флаг выполнения запроса
        isRequestInProgress = true;
        Log.d(TAG, "Начинаем запрос рецептов с сервера");

        // Вызываем API с использованием ApiCallHandler
        Call<RecipesResponse> call = apiService.getRecipes();
        ApiCallHandler.execute(call, new ApiCallHandler.ApiCallback<RecipesResponse>() {
            @Override
            public void onSuccess(RecipesResponse response) {
                synchronized (RecipeRemoteRepository.this) {
                    isRequestInProgress = false; // Сбрасываем флаг при успехе
                }
                Log.d(TAG, "Запрос рецептов успешно завершен");
                if (response.getRecipes() != null) {
                    List<Recipe> recipes = response.getRecipes();
                    Log.d(TAG, "Получено " + recipes.size() + " рецептов с сервера");
                    callback.onRecipesLoaded(recipes);
                } else {
                    Log.w(TAG, "Список рецептов в ответе пуст");
                    callback.onDataNotAvailable("Список рецептов пуст");
                }
            }

            @Override
            public void onError(String errorMessage) {
                synchronized (RecipeRemoteRepository.this) {
                    isRequestInProgress = false; // Сбрасываем флаг при ошибке
                }
                Log.e(TAG, "Ошибка запроса рецептов: " + errorMessage);
                callback.onDataNotAvailable(errorMessage);
            }
        });
    }


}
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
public class RecipeRemoteRepository {

    private static final String TAG = "RecipeRemoteRepository";

    private final Context context;
    private final ApiService apiService;
    private final MySharedPreferences preferences;

    public interface RecipesCallback {
        void onRecipesLoaded(List<Recipe> recipes);
        void onDataNotAvailable(String error);
    }

    public RecipeRemoteRepository(Context context) {
        this.context = context.getApplicationContext();
        this.preferences = new MySharedPreferences(context);
        this.apiService = NetworkService.getApiService(context);
    }

    /**
     * Получить рецепты с сервера
     * 
     * @param callback callback для возврата результата
     */
    public void getRecipes(final RecipesCallback callback) {
        // Проверяем доступность сети
        if (!isNetworkAvailable()) {
            callback.onDataNotAvailable("Нет подключения к интернету");
            return;
        }

        // Больше не нужно получать userId для этого запроса
        Log.d(TAG, "Отправляем запрос getRecipes");

        // Вызываем API с использованием ApiCallHandler
        Call<RecipesResponse> call = apiService.getRecipes();
        ApiCallHandler.execute(call, new ApiCallHandler.ApiCallback<RecipesResponse>() {
            @Override
            public void onSuccess(RecipesResponse response) {
                if (response.getRecipes() != null) {
                    List<Recipe> recipes = response.getRecipes();
                        Log.d(TAG, "Загружено с сервера рецептов: " + recipes.size());
                        callback.onRecipesLoaded(recipes);
                } else {
                    Log.e(TAG, "Список рецептов в ответе пуст");
                    callback.onDataNotAvailable("Список рецептов пуст");
                }
            }

            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Ошибка при загрузке рецептов: " + errorMessage);
                callback.onDataNotAvailable(errorMessage);
            }
        });
    }

    /**
     * Обновляет статус лайка рецепта
     * 
     * @param recipe  рецепт для обновления
     * @param isLiked новый статус лайка
     */
    public void updateLikeStatus(Recipe recipe, boolean isLiked) {
        if (!isNetworkAvailable()) {
            Log.e(TAG, "Нет подключения к интернету, невозможно обновить статус лайка");
            return;
        }

        if (recipe == null) {
            Log.e(TAG, "Рецепт для обновления статуса лайка равен null");
            return;
        }

        int recipeId = recipe.getId();
        String userId = preferences.getString("userId", "0");
        if (userId.equals("0")) {
            Log.e(TAG, "ID пользователя не найден. Невозможно обновить статус лайка");
            return;
                }

        // URL формат: /recipes/{id}/like или /recipes/{id}/unlike
        String endpoint = "/recipes/" + recipeId + (isLiked ? "/like" : "/unlike");
        String url = ServerConfig.getFullUrl(endpoint);
        
        Log.d(TAG, "Отправка запроса на " + url + " для userId=" + userId);
        
        // Тут можно использовать NetworkService напрямую или реализовать API метод в ApiService
        // Для примера оставим реализацию с OkHttpClient
    }

    /**
     * Проверяет доступность сети
     * 
     * @return true, если сеть доступна, иначе false
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager != null ? connectivityManager.getActiveNetworkInfo() : null;
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
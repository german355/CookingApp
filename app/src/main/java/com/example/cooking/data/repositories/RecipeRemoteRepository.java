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
    public void getRecipes(final RecipesCallback callback) {
        // Проверяем доступность сети
        if (!isNetworkAvailable()) {
            callback.onDataNotAvailable("Нет подключения к интернету");
            return;
        }

        // Больше не нужно получать userId для этого запроса

        // Вызываем API с использованием ApiCallHandler
        Call<RecipesResponse> call = apiService.getRecipes();
        ApiCallHandler.execute(call, new ApiCallHandler.ApiCallback<RecipesResponse>() {
            @Override
            public void onSuccess(RecipesResponse response) {
                if (response.getRecipes() != null) {
                    List<Recipe> recipes = response.getRecipes();
                    callback.onRecipesLoaded(recipes);
                } else {
                    callback.onDataNotAvailable("Список рецептов пуст");
                }
            }

            @Override
            public void onError(String errorMessage) {
                callback.onDataNotAvailable(errorMessage);
            }
        });
    }


}
package com.example.cooking.utils;

import com.example.cooking.Recipe.Recipe;
import java.util.ArrayList;
import java.util.List;
import android.content.Context;
import android.widget.Toast;
import com.example.cooking.network.api.ApiService;
import com.example.cooking.network.responses.RecipesResponse;
import com.example.cooking.network.responses.SearchResponse;
import com.example.cooking.network.services.NetworkService;
import com.example.cooking.network.utils.ApiCallHandler;
import retrofit2.Call;
import java.util.Collections;
import com.example.cooking.utils.MySharedPreferences;
import android.os.Handler;
import android.os.Looper;

public class RecipeSearchService {
    
    public interface SearchCallback {
        void onSearchResults(List<Recipe> recipes);
        void onSearchError(String error);
    }
    
    private final Context context;
    private final ApiService apiService;
    
    public RecipeSearchService(Context context) {
        this.context = context.getApplicationContext();
        this.apiService = NetworkService.getApiService(context);
    }
    
    /**
     * Поиск рецептов по запросу
     */
    public void searchRecipes(String query, SearchCallback callback) {
        if (query == null || query.trim().isEmpty()) {
            callback.onSearchResults(Collections.emptyList());
            return;
        }
        
        // Показываем тост для отладки
        showToast("Начинаю поиск: " + query);
        
        MySharedPreferences preferences = new MySharedPreferences(context);
        boolean smartSearchEnabled = preferences.getBoolean("smart_search_enabled", false);
        
        android.util.Log.d("RecipeSearchService", "Smart search enabled from prefs: " + smartSearchEnabled);
        showToast("Умный поиск включен: " + smartSearchEnabled);
        
        if (smartSearchEnabled) {
            String userId = preferences.getString("userId", "0");
            int page = 1;
            int perPage = 20;
            
            android.util.Log.d("RecipeSearchService", "Выполняю умный поиск с параметрами: query=" + query + ", userId=" + userId);
            showToast("Выполняю умный поиск");
            
            try {
                // Сбрасываем закэшированные сетевые клиенты
                NetworkService.reset();
                
                // Получаем новый экземпляр ApiService
                ApiService freshApiService = NetworkService.getApiService(context);
                
                // Форматируем запрос в соответствии с ожиданиями сервера
                String formattedQuery = query.trim();
                
                // Используем правильный вызов API для умного поиска
                Call<SearchResponse> smartCall = freshApiService.searchRecipes(formattedQuery, userId, page, perPage);
                
                String fullUrl = smartCall.request().url().toString();
                android.util.Log.d("RecipeSearchService", "URL умного поиска: " + fullUrl);
                android.util.Log.d("RecipeSearchService", "Заголовки запроса: " + smartCall.request().headers().toString());
                showToast("URL поиска: " + fullUrl);
                
                // Запускаем выполнение запроса в отдельном потоке
                new Thread(() -> {
                    try {
                        // Прямой вызов выполнения запроса
                        retrofit2.Response<SearchResponse> response = smartCall.execute();
                        android.util.Log.d("RecipeSearchService", "Умный поиск выполнен напрямую, код: " + response.code());
                        
                        // Обновляем UI в главном потоке
                        new Handler(Looper.getMainLooper()).post(() -> {
                            showToast("Код ответа: " + response.code());
                            
                            if (response.isSuccessful() && response.body() != null) {
                                SearchResponse searchResponse = response.body();
                                android.util.Log.d("RecipeSearchService", "Успешный ответ от умного поиска (прямой вызов)");
                                showToast("Поиск успешен, найдено: " + 
                                         (searchResponse.getData() != null && searchResponse.getData().getResults() != null ? 
                                          searchResponse.getData().getResults().size() : 0));
                                
                                if (searchResponse.getData() != null && searchResponse.getData().getResults() != null) {
                                    callback.onSearchResults(searchResponse.getData().getResults());
                                } else {
                                    callback.onSearchResults(Collections.emptyList());
                                }
                            } else {
                                try {
                                    String errorBody = response.errorBody() != null ? response.errorBody().string() : "";
                                    android.util.Log.e("RecipeSearchService", "Ошибка умного поиска (прямой вызов): " + errorBody);
                                    showToast("Ошибка поиска: " + response.code() + " " + errorBody);
                                } catch (Exception e) {
                                    android.util.Log.e("RecipeSearchService", "Ошибка при чтении тела ошибки", e);
                                }
                                fallbackToSimpleSearch(query, callback);
                            }
                        });
                    } catch (Exception e) {
                        android.util.Log.e("RecipeSearchService", "Ошибка прямого вызова: " + e.getMessage(), e);
                        
                        // Обновляем UI в главном потоке
                        new Handler(Looper.getMainLooper()).post(() -> {
                            showToast("Исключение при прямом вызове: " + e.getMessage());
                            
                            // Используем асинхронный вызов как запасной вариант
                            useAsyncCall(smartCall, query, callback);
                        });
                    }
                }).start();
            } catch (Exception e) {
                android.util.Log.e("RecipeSearchService", "Исключение при вызове умного поиска: " + e.getMessage(), e);
                showToast("Ошибка: " + e.getMessage());
                fallbackToSimpleSearch(query, callback);
            }
        } else {
            fallbackToSimpleSearch(query, callback);
        }
    }
    
    /**
     * Запасной метод для использования простого поиска
     */
    private void fallbackToSimpleSearch(String query, SearchCallback callback) {
        android.util.Log.d("RecipeSearchService", "Использую простой поиск для запроса: " + query);
        showToast("Использую простой поиск");
        Call<RecipesResponse> call = apiService.searchRecipesSimple(query.trim());
        
        ApiCallHandler.execute(call, new ApiCallHandler.ApiCallback<RecipesResponse>() {
            @Override
            public void onSuccess(RecipesResponse response) {
                callback.onSearchResults(response.getRecipes());
            }
            
            @Override
            public void onError(String errorMessage) {
                callback.onSearchError(errorMessage);
            }
        });
    }
    
    /**
     * Показывает тост для отладки
     */
    private void showToast(String message) {
        new Handler(Looper.getMainLooper()).post(() -> {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        });
    }
    
    /**
     * Использует асинхронный вызов API
     */
    private void useAsyncCall(Call<SearchResponse> smartCall, String query, SearchCallback callback) {
        ApiCallHandler.execute(smartCall, new ApiCallHandler.ApiCallback<SearchResponse>() {
            @Override
            public void onSuccess(SearchResponse response) {
                android.util.Log.d("RecipeSearchService", "Успешный ответ от умного поиска");
                showToast("Успешный ответ от умного поиска");
                
                if (response != null) {
                    android.util.Log.d("RecipeSearchService", "Статус ответа: " + response.getStatus());
                    
                    if (!response.isSuccess()) {
                        android.util.Log.e("RecipeSearchService", "Ответ от сервера имеет статус ошибки: " + response.getMessage());
                        showToast("Ошибка: " + response.getMessage());
                        fallbackToSimpleSearch(query, callback);
                        return;
                    }
                    
                    if (response.getData() != null && response.getData().getResults() != null) {
                        List<Recipe> recipes = response.getData().getResults();
                        android.util.Log.d("RecipeSearchService", "Получено " + recipes.size() + " рецептов из " + response.getTotalResults() + " всего");
                        showToast("Найдено " + recipes.size() + " рецептов");
                        callback.onSearchResults(recipes);
                    } else {
                        android.util.Log.e("RecipeSearchService", "Ответ от сервера был успешным, но не содержал данных или результатов");
                        showToast("Нет результатов");
                        callback.onSearchResults(Collections.emptyList());
                    }
                } else {
                    android.util.Log.e("RecipeSearchService", "Ответ от сервера был null");
                    showToast("Пустой ответ");
                    callback.onSearchResults(Collections.emptyList());
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                android.util.Log.e("RecipeSearchService", "Ошибка умного поиска: " + errorMessage);
                showToast("Ошибка: " + errorMessage);
                // Если умный поиск не сработал, пробуем откатиться к простому поиску
                fallbackToSimpleSearch(query, callback);
            }
        });
    }
}
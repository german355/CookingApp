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
        boolean smartSearchEnabled = preferences.getBoolean("smart_search_enabled", true);
        
        android.util.Log.d("RecipeSearchService", "Smart search enabled from prefs: " + smartSearchEnabled);
        showToast("Умный поиск включен: " + smartSearchEnabled);
        
        if (smartSearchEnabled) {
            String userId = preferences.getString("userId", "0");
            int page = 1;
            int perPage = 20;
            
            android.util.Log.d("RecipeSearchService", "Выполняю умный поиск (асинхронно) с параметрами: query=" + query + ", userId=" + userId);
            showToast("Выполняю умный поиск");
            
            try {
                // Сбрасываем закэшированные сетевые клиенты
                NetworkService.reset();
                
                // Получаем новый экземпляр ApiService
                ApiService freshApiService = NetworkService.getApiService(context);
                
                // Форматируем запрос в соответствии с ожиданиями сервера
                String formattedQuery = query.trim();
                
                // Используем правильный вызов API для умного поиска
                final Call<SearchResponse> smartCall = freshApiService.searchRecipes(formattedQuery, userId, page, perPage);
                
                String fullUrl = smartCall.request().url().toString();
                android.util.Log.d("RecipeSearchService", "URL умного поиска: " + fullUrl);
                android.util.Log.d("RecipeSearchService", "Заголовка запроса: " + smartCall.request().headers().toString());
                // showToast("URL поиска: " + fullUrl); // Можно закомментировать, чтобы не перегружать пользователя тостами

                // Сразу используем асинхронный вызов
                useAsyncCall(smartCall, query, callback);
                
            } catch (Exception e) { // Этот блок catch теперь будет ловить ошибки, возникшие при подготовке вызова (например, в getApiService или searchRecipes)
                android.util.Log.e("RecipeSearchService", "Исключение при подготовке или запуске умного поиска: " + e.getMessage(), e);
                showToast("Ошибка умного поиска: " + e.getMessage());
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
        // Уберем тост о переключении, так как он уже отображается в умном поиске
        Call<RecipesResponse> call = apiService.searchRecipesSimple(query.trim());
        android.util.Log.d("RecipeSearchService", "URL простого поиска: " + call.request().url().toString());
        
        ApiCallHandler.execute(call, new ApiCallHandler.ApiCallback<RecipesResponse>() {
            @Override
            public void onSuccess(RecipesResponse response) {
                android.util.Log.d("RecipeSearchService", "Простой поиск успешен, найдено рецептов: " + 
                    (response != null && response.getRecipes() != null ? response.getRecipes().size() : 0));
                
                if (response != null && response.getRecipes() != null && !response.getRecipes().isEmpty()) {
                    callback.onSearchResults(response.getRecipes());
                } else {
                    android.util.Log.d("RecipeSearchService", "Простой поиск не вернул результатов");
                    callback.onSearchResults(Collections.emptyList());
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                android.util.Log.e("RecipeSearchService", "Ошибка простого поиска: " + errorMessage);
                callback.onSearchError("Ошибка поиска: " + errorMessage);
            }
        });
    }
    
    /**
     * Использует асинхронный вызов API
     */
    private void useAsyncCall(Call<SearchResponse> callToExecute, String query, SearchCallback callback) {
        ApiCallHandler.execute(callToExecute, new ApiCallHandler.ApiCallback<SearchResponse>() {
            @Override
            public void onSuccess(SearchResponse response) {
                android.util.Log.d("RecipeSearchService", "Успешный ответ от умного поиска");
                // Подробный лог содержимого ответа для отладки
                if (response != null) {
                    android.util.Log.d("RecipeSearchService", "Детали ответа: status=" + response.getStatus() 
                        + ", message=" + response.getMessage() 
                        + ", isSuccess=" + response.isSuccess());
                    
                    if (response.getData() != null) {
                        android.util.Log.d("RecipeSearchService", "Данные: data!=null, results=" 
                            + (response.getData().getResults() != null ? response.getData().getResults().size() : "null") 
                            + ", totalResults=" + response.getData().getTotalResults());
                    } else {
                        android.util.Log.d("RecipeSearchService", "Данные: data=null");
                    }
                }
                
                // Проверка на null и валидность ответа
                if (response != null) {
                    android.util.Log.d("RecipeSearchService", "Статус ответа: " + response.getStatus());
                    
                    // Даже если isSuccess() возвращает false, попробуем проверить, есть ли данные
                    if (!response.isSuccess() && (response.getData() == null || response.getData().getResults() == null)) {
                        android.util.Log.e("RecipeSearchService", "Ответ от сервера имеет статус ошибки: " + response.getMessage());
                        showToast("Переключаюсь на обычный поиск: " + response.getMessage());
                        fallbackToSimpleSearch(query, callback);
                        return;
                    }
                    
                    if (response.getData() != null && response.getData().getResults() != null) {
                        List<Recipe> recipes = response.getData().getResults();
                        if (!recipes.isEmpty()) {
                            android.util.Log.d("RecipeSearchService", "Получено " + recipes.size() + " рецептов из " + response.getTotalResults() + " всего");
                            showToast("Найдено " + recipes.size() + " рецептов");
                            callback.onSearchResults(recipes);
                            return;
                        } else {
                            android.util.Log.d("RecipeSearchService", "Список рецептов пуст");
                        }
                    } else {
                        android.util.Log.e("RecipeSearchService", "Ответ от сервера был успешным, но не содержал данных или результатов");
                        android.util.Log.d("RecipeSearchService", "response.getData()=" + response.getData());
                    }
                    
                    // Если мы дошли до этой точки, значит результатов нет или они пустые
                    showToast("Переключаюсь на обычный поиск: нет результатов");
                    fallbackToSimpleSearch(query, callback);
                } else {
                    android.util.Log.e("RecipeSearchService", "Ответ от сервера был null");
                    showToast("Переключаюсь на обычный поиск: пустой ответ");
                    fallbackToSimpleSearch(query, callback);
                }
            }
            
            @Override
            public void onError(String errorMessage) {
                android.util.Log.e("RecipeSearchService", "Ошибка умного поиска: " + errorMessage);
                showToast("Переключаюсь на обычный поиск: " + (errorMessage.length() > 30 ? errorMessage.substring(0, 30) + "..." : errorMessage));
                // Если умный поиск не сработал, пробуем откатиться к простому поиску
                fallbackToSimpleSearch(query, callback);
            }
        });
    }
    
    /**
     * Показывает тост для отладки
     */
    private void showToast(String message) {
        // Debug-toasts suppressed to reduce spam
    }
}
package com.example.cooking.utils;

import com.example.cooking.Recipe.Recipe;
import com.example.cooking.network.api.ApiService;
import com.example.cooking.network.responses.SearchResponse;
import com.example.cooking.network.services.NetworkService;
import com.example.cooking.network.utils.ApiCallHandler;
import retrofit2.Call;
import com.example.cooking.utils.MySharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.content.Context;
import android.widget.Toast;
import android.util.Log;
import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import com.example.cooking.data.repositories.RecipeLocalRepository;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecipeSearchService {
    
    public interface SearchCallback {
        void onSearchResults(List<Recipe> recipes);
        void onSearchError(String error);
    }
    
    private final Context context;
    private final ApiService apiService;
    private final ExecutorService dbExecutor = Executors.newSingleThreadExecutor();
    private final Handler uiHandler = new Handler(Looper.getMainLooper());
    
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
        
        Log.d("RecipeSearchService", "Smart search enabled from prefs: " + smartSearchEnabled);
        showToast("Умный поиск включен: " + smartSearchEnabled);
        
        if (smartSearchEnabled) {
            String userId = preferences.getString("userId", "0");
            int page = 1;
            int perPage = 20;
            
            Log.d("RecipeSearchService", "Выполняю умный поиск (асинхронно) с параметрами: query=" + query + ", userId=" + userId);
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
                Log.d("RecipeSearchService", "URL умного поиска: " + fullUrl);
                Log.d("RecipeSearchService", "Заголовка запроса: " + smartCall.request().headers().toString());
                // showToast("URL поиска: " + fullUrl); // Можно закомментировать, чтобы не перегружать пользователя тостами

                // Сразу используем асинхронный вызов
                useAsyncCall(smartCall, query, callback);
                
            } catch (Exception e) { // Этот блок catch теперь будет ловить ошибки, возникшие при подготовке вызова (например, в getApiService или searchRecipes)
                Log.e("RecipeSearchService", "Исключение при подготовке или запуске умного поиска: " + e.getMessage(), e);
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
        Log.d("RecipeSearchService", "Использую простой поиск для запроса: " + query);
        Call<SearchResponse> call = apiService.searchRecipesSimple(query.trim());
        Log.d("RecipeSearchService", "URL простого поиска: " + call.request().url());
        ApiCallHandler.execute(call, new ApiCallHandler.ApiCallback<SearchResponse>() {
            @Override
            public void onSuccess(SearchResponse response) {
                List<String> ids = response != null && response.getData() != null
                    ? response.getData().getResults() : Collections.emptyList();
                Log.d("RecipeSearchService", "Простой поиск вернул ID: " + ids.size());
                dbExecutor.execute(() -> {
                    RecipeLocalRepository localRepo = new RecipeLocalRepository(context);
                    List<Recipe> fullRecipes = new ArrayList<>();
                    for (String idStr : ids) {
                        try {
                            int id = Integer.parseInt(idStr);
                            Recipe full = localRepo.getRecipeByIdSync(id);
                            if (full != null) fullRecipes.add(full);
                        } catch (NumberFormatException ignore) {}
                    }
                    uiHandler.post(() -> callback.onSearchResults(fullRecipes));
                });
            }
            @Override
            public void onError(String errorMessage) {
                Log.e("RecipeSearchService", "Ошибка простого поиска: " + errorMessage);
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
                Log.d("RecipeSearchService", "Успешный ответ от умного поиска");
                if (response != null && response.getData() != null && response.getData().getResults() != null) {
                    List<String> ids = response.getData().getResults();
                    if (!ids.isEmpty()) {
                        Log.d("RecipeSearchService", "Получено ID: " + ids.size() + " из " + response.getTotalResults());
                        showToast("Найдено " + ids.size() + " рецептов");
                        dbExecutor.execute(() -> {
                            RecipeLocalRepository localRepo = new RecipeLocalRepository(context);
                            List<Recipe> fullRecipes = new ArrayList<>();
                            for (String idStr : ids) {
                                try {
                                    int id = Integer.parseInt(idStr);
                                    Recipe full = localRepo.getRecipeByIdSync(id);
                                    if (full != null) fullRecipes.add(full);
                                } catch (NumberFormatException ignored) {}
                            }
                            uiHandler.post(() -> callback.onSearchResults(fullRecipes));
                        });
                        return;
                    }
                    Log.d("RecipeSearchService", "Список ID пуст");
                } else {
                    Log.e("RecipeSearchService", "Нет данных или результатов в ответе");
                }
                showToast("Переключаюсь на обычный поиск: нет результатов");
                fallbackToSimpleSearch(query, callback);
            }
            @Override
            public void onError(String errorMessage) {
                Log.e("RecipeSearchService", "Ошибка умного поиска: " + errorMessage);
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
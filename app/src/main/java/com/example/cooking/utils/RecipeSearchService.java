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
    
    private static final String TAG = "RecipeSearchService";
    
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
        Log.d(TAG, "searchRecipes start for query: '" + query + "'");
        Log.d(TAG, "-------------- НАЧАЛО ПОИСКА --------------");
        Log.d(TAG, "Поисковый запрос: '" + query + "'");
        
        if (query == null || query.trim().isEmpty()) {
            Log.d(TAG, "Поисковый запрос пуст или null, возвращаем пустой список");
            callback.onSearchResults(Collections.emptyList());
            return;
        }
        
        MySharedPreferences preferences = new MySharedPreferences(context);
        boolean smartSearchEnabled = preferences.getBoolean("smart_search_enabled", true);
        Log.d(TAG, "smartSearchEnabled: " + smartSearchEnabled);
        
        Log.d("RecipeSearchService", "Smart search enabled from prefs: " + smartSearchEnabled);
        if (smartSearchEnabled) {
            Log.d(TAG, "Использую умный поиск для запроса: '" + query + "'");
            int page = 1;
            int perPage = 20;

            try {
                // Сбрасываем закэшированные сетевые клиенты
                NetworkService.reset();
                
                // Получаем новый экземпляр ApiService
                ApiService freshApiService = NetworkService.getApiService(context);
                
                // Форматируем запрос в соответствии с ожиданиями сервера
                String formattedQuery = query.trim();
                
                // Используем правильный вызов API для умного поиска
                final Call<SearchResponse> smartCall = freshApiService.searchRecipes(formattedQuery, page, perPage);
                
                String fullUrl = smartCall.request().url().toString();

                useAsyncCall(smartCall, query, callback);
                
            } catch (Exception e) { // Этот блок catch теперь будет ловить ошибки, возникшие при подготовке вызова (например, в getApiService или searchRecipes)
                Log.e(TAG, "Ошибка подготовки умного поиска, exception:", e);
                showToast("Ошибка умного поиска");
                fallbackToSimpleSearch(query, callback);
            }
        } else {
            Log.d(TAG, "smartSearchEnabled=false, переходим к простому поиску");
            fallbackToSimpleSearch(query, callback);
        }
    }
    
    /**
     * Запасной метод для использования простого поиска
     */
    private void fallbackToSimpleSearch(String query, SearchCallback callback) {
        Log.d("RecipeSearchService", "Использую простой поиск для запроса: " + query);
        Log.d(TAG, "fallbackToSimpleSearch start for query: '" + query + "'");
        Call<SearchResponse> call = apiService.searchRecipesSimple(query.trim());
        Log.d("RecipeSearchService", "URL простого поиска: " + call.request().url());
        ApiCallHandler.execute(call, new ApiCallHandler.ApiCallback<SearchResponse>() {
            @Override
            public void onSuccess(SearchResponse response) {
                List<String> ids = response != null && response.getData() != null
                    ? response.getData().getResults() : Collections.emptyList();
                Log.d(TAG, "fallbackToSimpleSearch onSuccess ids size: " + ids.size());
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
                    uiHandler.post(() -> {
                        callback.onSearchResults(fullRecipes);
                        Log.d(TAG, "fallbackToSimpleSearch posting fullRecipes size: " + fullRecipes.size());
                    });
                });
            }
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "fallbackToSimpleSearch onError: " + errorMessage);
                callback.onSearchError("Ошибка поиска");
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
                Log.d(TAG, "useAsyncCall onSuccess response: " + response);

                if (response == null) {
                    Log.d(TAG, "useAsyncCall response null, fallbackToSimpleSearch");
                    fallbackToSimpleSearch(query, callback);
                    return;
                }
                
                if (response.getData() == null) {
                    Log.d(TAG, "useAsyncCall data null, fallbackToSimpleSearch");
                    fallbackToSimpleSearch(query, callback);
                    return;
                }
                
                if (response.getData().getResults() == null) {
                    Log.d(TAG, "useAsyncCall results null, fallbackToSimpleSearch");
                    fallbackToSimpleSearch(query, callback);
                    return;
                }
                
                List<String> ids = response.getData().getResults();
                Log.d(TAG, "useAsyncCall initial ids size: " + ids.size());

                if (!ids.isEmpty()) {
                    showToast("Найдено " + ids.size() + " рецептов");
                    dbExecutor.execute(() -> {
                        RecipeLocalRepository localRepo = new RecipeLocalRepository(context);
                        List<Recipe> fullRecipes = new ArrayList<>();
                        int foundCount = 0;
                        int notFoundCount = 0;
                        
                        for (String idStr : ids) {
                            try {
                                int id = Integer.parseInt(idStr);
                                Recipe full = localRepo.getRecipeByIdSync(id);

                                if (full != null) {
                                    fullRecipes.add(full);
                                    foundCount++;
                                } else {
                                    notFoundCount++;
                                }
                            } catch (NumberFormatException e) {
                            }
                        }

                        Log.d(TAG, "useAsyncCall DB load foundCount: " + foundCount + ", notFoundCount: " + notFoundCount + ", fullRecipes size: " + fullRecipes.size());
                        
                        final List<Recipe> finalRecipes = new ArrayList<>(fullRecipes); // создаем копию для безопасной передачи
                        uiHandler.post(() -> {
                            Log.d(TAG, "useAsyncCall posting finalRecipes size: " + finalRecipes.size());
                            callback.onSearchResults(finalRecipes);
                        });
                    });
                    return;
                }
                Log.d(TAG, "useAsyncCall ids empty, fallbackToSimpleSearch");
                fallbackToSimpleSearch(query, callback);
            }
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "useAsyncCall onError: " + errorMessage);
                showToast("Извините но наш поиск решил отдохнуть");
                fallbackToSimpleSearch(query, callback);
            }
        });
    }
    
    /**
     * Показывает тост для отладки
     */
    private void showToast(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }
}
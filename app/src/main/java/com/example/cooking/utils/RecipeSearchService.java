package com.example.cooking.utils;

import com.example.cooking.Recipe.Recipe;
import java.util.ArrayList;
import java.util.List;
import android.content.Context;
import com.example.cooking.network.api.ApiService;
import com.example.cooking.network.responses.RecipesResponse;
import com.example.cooking.network.responses.SearchResponse;
import com.example.cooking.network.services.NetworkService;
import com.example.cooking.network.utils.ApiCallHandler;
import retrofit2.Call;
import java.util.Collections;
import com.example.cooking.utils.MySharedPreferences;

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
        
        MySharedPreferences preferences = new MySharedPreferences(context);
        boolean smartSearchEnabled = preferences.getBoolean("smart_search_enabled", false);
        android.util.Log.d("RecipeSearchService", "Smart search enabled from prefs: " + smartSearchEnabled);
        
        if (smartSearchEnabled) {
            String userId = preferences.getString("userId", "0");
            int page = 1;
            int perPage = 20;
            Call<SearchResponse> smartCall = apiService.searchRecipes(query.trim(), userId, page, perPage);
            
            ApiCallHandler.execute(smartCall, new ApiCallHandler.ApiCallback<SearchResponse>() {
                @Override
                public void onSuccess(SearchResponse response) {
                    if (response.getData() != null && response.getData().getResults() != null) {
                        callback.onSearchResults(response.getData().getResults());
                    } else {
                        callback.onSearchResults(Collections.emptyList());
                    }
                }
                
                @Override
                public void onError(String errorMessage) {
                    callback.onSearchError(errorMessage);
                }
            });
        } else {
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
    }
}
package com.example.cooking.utils;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import com.example.cooking.R;
import com.example.cooking.domain.entities.Recipe;
import com.example.cooking.network.api.ApiService;
import com.example.cooking.network.models.recipeResponses.SearchResponse;
import com.example.cooking.network.services.NetworkService;
import com.example.cooking.data.repositories.RecipeLocalRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class RecipeSearchService {

    private static final String TAG = "RecipeSearchService";

    public interface SearchCallback {
        void onSearchResults(List<Recipe> recipes);
        void onSearchError(String error);
    }

    private final Context context;
    private final ApiService apiService;
    private final RecipeLocalRepository localRepository;
    private final CompositeDisposable disposables = new CompositeDisposable();

    public RecipeSearchService(Context context) {
        this.context = context.getApplicationContext();
        this.apiService = NetworkService.getApiService(context);
        this.localRepository = new RecipeLocalRepository(context);
    }

    public void searchRecipes(String query, SearchCallback callback) {
        Log.d(TAG, "searchRecipes start for query: '" + query + "'");

        if (query == null || query.trim().isEmpty()) {
            Log.d(TAG, "Поисковый запрос пуст, возвращаем пустой список.");
            callback.onSearchResults(Collections.emptyList());
            return;
        }

        MySharedPreferences preferences = new MySharedPreferences(context);
        boolean smartSearchEnabled = preferences.getBoolean("smart_search_enabled", true);
        Log.d(TAG, "Smart search enabled: " + smartSearchEnabled);

        Single<List<Recipe>> searchSingle = smartSearchEnabled
                ? performSmartSearch(query)
                : performSimpleSearch(query);

        disposables.add(
            searchSingle
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    recipes -> {
                        Log.d(TAG, "Search successful, found recipes: " + recipes.size());
                        callback.onSearchResults(recipes);
                    },
                    throwable -> {
                        Log.e(TAG, "Search failed", throwable);
                        callback.onSearchError(context.getString(R.string.search_service_generic_error));
                    }
                )
        );
    }

    private Single<List<Recipe>> performSmartSearch(String query) {
        int page = 1;
        int perPage = 20;
        String formattedQuery = "\"" + query.trim() + "\"";
        Log.d(TAG, "Performing smart search with query: " + formattedQuery);

        return apiService.searchRecipes(formattedQuery, page, perPage)
                .flatMap(this::processSearchResponse)
                .doOnSuccess(recipes -> {
                    if (!recipes.isEmpty()) {
                        String foundMessage = context.getResources().getQuantityString(R.plurals.search_service_recipes_found, recipes.size(), recipes.size());
                        showToastOnMainThread(foundMessage);
                    }
                })
                .onErrorResumeNext(throwable -> {
                    Log.w(TAG, "Smart search failed, falling back to simple search.", throwable);
                    showToastOnMainThread(context.getString(R.string.search_service_smart_search_error));
                    return performSimpleSearch(query);
                });
    }

    private Single<List<Recipe>> performSimpleSearch(String query) {
        Log.d(TAG, "Performing simple search with query: " + query);
        return apiService.searchRecipesSimple(query.trim())
                .flatMap(this::processSearchResponse);
    }

    private Single<List<Recipe>> processSearchResponse(SearchResponse response) {
        if (response == null || response.getData() == null || response.getData().getResults() == null) {
            Log.d(TAG, "Response or data is null, returning empty list.");
            return Single.just(Collections.emptyList());
        }

        List<String> ids = response.getData().getResults();
        if (ids.isEmpty()) {
            Log.d(TAG, "No recipe IDs found in response.");
            return Single.just(Collections.emptyList());
        }

        Log.d(TAG, "Processing " + ids.size() + " recipe IDs from search response.");
        return Single.fromCallable(() -> {
            List<Recipe> fullRecipes = new ArrayList<>();
            int foundCount = 0;
            int notFoundCount = 0;
            for (String idStr : ids) {
                try {
                    int id = Integer.parseInt(idStr);
                    Recipe full = localRepository.getRecipeByIdSync(id);
                    if (full != null) {
                        fullRecipes.add(full);
                        foundCount++;
                    } else {
                        notFoundCount++;
                    }
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Invalid recipe ID format: " + idStr, e);
                }
            }
            Log.d(TAG, "DB lookup complete. Found: " + foundCount + ", Not Found: " + notFoundCount);
            return fullRecipes;
        });
    }

    public void cancel() {
        disposables.clear();
    }

    private void showToastOnMainThread(String message) {
        AndroidSchedulers.mainThread().scheduleDirect(() -> {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
        });
    }
}
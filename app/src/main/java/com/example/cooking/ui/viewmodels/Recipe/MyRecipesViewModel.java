package com.example.cooking.ui.viewmodels.Recipe;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cooking.data.repositories.RecipeLocalRepository;
import com.example.cooking.data.repositories.RecipeRemoteRepository;
import com.example.cooking.domain.entities.Recipe;
import com.example.cooking.domain.usecases.RecipeLikeUseCase;
import com.example.cooking.utils.AppExecutors;
import com.example.cooking.utils.MySharedPreferences;

import java.util.ArrayList;
import java.util.List;

public class MyRecipesViewModel extends AndroidViewModel {
    private final MutableLiveData<List<Recipe>> recipes = new MutableLiveData<>(new ArrayList<>());
    private final MutableLiveData<Boolean> isRefreshing = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    private final RecipeRemoteRepository recipeRemoteRepository;
    private final RecipeLocalRepository recipeLocalRepository;
    private final RecipeLikeUseCase recipeLikeUseCase;

    public MyRecipesViewModel(@NonNull Application application) {
        super(application);
        this.recipeRemoteRepository = new RecipeRemoteRepository(application);
        this.recipeLocalRepository = new RecipeLocalRepository(application);
        this.recipeLikeUseCase = new RecipeLikeUseCase(application);
    }

    public LiveData<List<Recipe>> getRecipes() {
        return recipes;
    }

    public LiveData<Boolean> getIsRefreshing() {
        return isRefreshing;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public void loadMyRecipes() {
        isRefreshing.setValue(true);
        recipeRemoteRepository.getMyRecipes(new RecipeRemoteRepository.RecipesCallback() {
            @Override
            public void onRecipesLoaded(List<Recipe> loadedRecipes) {
                AppExecutors.getInstance().diskIO().execute(() -> {
                    for (Recipe recipe : loadedRecipes) {
                        Recipe existing = recipeLocalRepository.getRecipeByIdSync(recipe.getId());
                        if (existing != null) {
                            recipe.setLiked(existing.isLiked());
                            recipeLocalRepository.updateSync(recipe);
                        } else {
                            recipeLocalRepository.insertSync(recipe);
                        }
                    }
                    isRefreshing.postValue(false);
                    recipes.postValue(loadedRecipes);
                });
            }

            @Override
            public void onDataNotAvailable(String error) {
                isRefreshing.postValue(false);
                errorMessage.postValue(error);
            }
        });
    }

    public void toggleLikeStatus(Recipe recipe, boolean isLiked) {
        MySharedPreferences preferences = new MySharedPreferences(getApplication());
        recipeLikeUseCase.setLikeStatus(preferences.getUserId(), recipe.getId(), isLiked, errorMessage);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        recipeRemoteRepository.clearDisposables();
        recipeLikeUseCase.clearResources();
    }
}

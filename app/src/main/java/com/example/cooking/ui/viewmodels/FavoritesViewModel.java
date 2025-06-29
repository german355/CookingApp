package com.example.cooking.ui.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cooking.domain.entities.Recipe;
import com.example.cooking.utils.MySharedPreferences;
import com.example.cooking.domain.usecases.RecipeDataUseCase;
import com.example.cooking.domain.usecases.RecipeLikeUseCase;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ViewModel для экрана избранных рецептов.
 */
public class FavoritesViewModel extends AndroidViewModel {
    private static final String TAG = "FavoritesViewModel";

    private final RecipeDataUseCase recipeDataUseCase;
    private final RecipeLikeUseCase recipeLikeUseCase;
    
    private final MediatorLiveData<List<Recipe>> _favoriteRecipes = new MediatorLiveData<>();
    public final LiveData<List<Recipe>> favoriteRecipes = _favoriteRecipes;

    private final MutableLiveData<Boolean> _isRefreshing = new MutableLiveData<>(false);
    public final LiveData<Boolean> isRefreshing = _isRefreshing;
    
    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public final LiveData<String> errorMessage = _errorMessage;

    private final MySharedPreferences preferences;
    
    public FavoritesViewModel(@NonNull Application application) {
        super(application);
        preferences = new MySharedPreferences(application);
        recipeDataUseCase = new RecipeDataUseCase(application);
        recipeLikeUseCase = new RecipeLikeUseCase(application);

        // Подписываемся на основной источник всех рецептов
        LiveData<List<Recipe>> allRecipesSource = recipeDataUseCase.getAllRecipesLocalLiveData();

        _favoriteRecipes.addSource(allRecipesSource, allRecipes -> {
            if (allRecipes != null) {
                // Фильтруем только лайкнутые
                List<Recipe> likedRecipes = allRecipes.stream()
                        .filter(Recipe::isLiked)
                        .collect(Collectors.toList());
                _favoriteRecipes.setValue(likedRecipes);
            }
        });

        // Принудительно обновляем данные с сервера при инициализации
        refreshRecipes();
    }
    
    public void refreshRecipes() {
        _isRefreshing.setValue(true);
        // Используем пустой MediatorLiveData, так как данные обновятся через основной источник
        recipeDataUseCase.refreshRecipes(_isRefreshing, _errorMessage, new MediatorLiveData<>(), () -> {
            _isRefreshing.setValue(false);
        });
    }

    public void toggleLikeStatus(Recipe recipe) {
        if (recipe == null) return;
        String userId = preferences.getUserId();
        // Дизлайк в избранном всегда приводит к удалению из этого списка
        recipeLikeUseCase.setLikeStatus(userId, recipe.getId(), false, _errorMessage);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        recipeDataUseCase.clearResources();
        recipeLikeUseCase.clearResources();
    }
}
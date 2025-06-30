package com.example.cooking.ui.viewmodels.Recipe;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cooking.domain.entities.Recipe;
import com.example.cooking.ui.fragments.FilteredRecipesFragment;
import com.example.cooking.domain.usecases.RecipeDataUseCase;
import com.example.cooking.domain.usecases.RecipeFilterUseCase;
import com.example.cooking.domain.usecases.RecipeLikeUseCase;
import com.example.cooking.utils.MySharedPreferences;

import java.util.Collections;
import java.util.List;

/**
 * ViewModel для {@link FilteredRecipesFragment}.
 * Отвечает за фильтрацию рецептов на основе выбранной категории,
 * используя единый источник данных из SharedRecipeViewModel.
 */
public class FilteredRecipesViewModel extends AndroidViewModel {
    private final MutableLiveData<List<Recipe>> _filteredRecipes = new MutableLiveData<>(Collections.emptyList());
    public final LiveData<List<Recipe>> filteredRecipes = _filteredRecipes;

    private final MutableLiveData<String> _errorMessage = new MutableLiveData<>();
    public final LiveData<String> errorMessage = _errorMessage;

    private final MutableLiveData<Boolean> _isRefreshing = new MutableLiveData<>(false);
    public final LiveData<Boolean> isRefreshing = _isRefreshing;

    private final RecipeDataUseCase recipeDataUseCase;
    private final RecipeFilterUseCase recipeFilterUseCase;
    private final RecipeLikeUseCase recipeLikeUseCase;

    public FilteredRecipesViewModel(@NonNull Application application) {
        super(application);
        recipeDataUseCase = new RecipeDataUseCase(application);
        recipeFilterUseCase = new RecipeFilterUseCase(application);
        recipeLikeUseCase = new RecipeLikeUseCase(application);
    }

    public void loadFilteredRecipes(String filterKey, String filterType) {
        _isRefreshing.setValue(true);
        
        // Используем единый метод фильтрации с автообновлением
        recipeFilterUseCase.filterRecipesByCategory(filterKey, filterType, _filteredRecipes, _errorMessage, _isRefreshing);
    }

    public void toggleLikeStatus(Recipe recipe, boolean isLiked) {
        MySharedPreferences preferences = new MySharedPreferences(getApplication());
        recipeLikeUseCase.setLikeStatus(preferences.getUserId(), recipe.getId(), isLiked, _errorMessage);
    }
    
    public void refreshData() {
        // Получаем текущие параметры фильтрации (можно сохранить в полях класса)
        // Пока используем общее обновление через RecipeDataUseCase
        MutableLiveData<com.example.cooking.network.utils.Resource<List<Recipe>>> dummyLiveData = new MutableLiveData<>();
        recipeDataUseCase.refreshRecipes(_isRefreshing, _errorMessage, dummyLiveData, null);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        recipeDataUseCase.clearResources();
        recipeFilterUseCase.clearResources();
        recipeLikeUseCase.clearResources();
    }
} 
package com.example.cooking.ui.viewmodels.Recipe;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cooking.domain.entities.Recipe;
import com.example.cooking.ui.fragments.FilteredRecipesFragment;
import com.example.cooking.domain.usecases.RecipeDataUseCase;
import com.example.cooking.domain.usecases.RecipeLikeUseCase;
import com.example.cooking.utils.AppExecutors;
import com.example.cooking.utils.MySharedPreferences;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
    private final RecipeLikeUseCase recipeLikeUseCase;

    public FilteredRecipesViewModel(@NonNull Application application) {
        super(application);
        recipeDataUseCase = new RecipeDataUseCase(application);
        recipeLikeUseCase = new RecipeLikeUseCase(application);
    }

    public void loadFilteredRecipes(String filterKey, String filterType) {
        _isRefreshing.setValue(true);
        
        AppExecutors.getInstance().diskIO().execute(() -> {
            List<Recipe> allRecipes = recipeDataUseCase.getAllRecipesSync();
            if (allRecipes == null) {
                _errorMessage.postValue("Не удалось загрузить рецепты.");
                _filteredRecipes.postValue(Collections.emptyList());
                _isRefreshing.postValue(false);
                return;
            }

            List<Recipe> filtered = allRecipes.stream()
                    .filter(recipe -> {
                        if ("meal_type".equals(filterType)) {
                            return filterKey.equalsIgnoreCase(recipe.getMealType());
                        } else if ("food_type".equals(filterType)) {
                            return filterKey.equalsIgnoreCase(recipe.getFoodType());
                        }
                        return false;
                    })
                    .collect(Collectors.toList());
            
            _filteredRecipes.postValue(filtered);
            if (filtered.isEmpty()) {
                _errorMessage.postValue("Рецепты в этой категории не найдены.");
            }
            _isRefreshing.postValue(false);
        });
    }

    public void toggleLikeStatus(Recipe recipe, boolean isLiked) {
        MySharedPreferences preferences = new MySharedPreferences(getApplication());
        recipeLikeUseCase.setLikeStatus(preferences.getUserId(), recipe.getId(), isLiked, _errorMessage);
    }
    
    public void refreshData() {
        // В этой реализации обновление данных происходит через HomeFragment,
        // а этот ViewModel просто получает отфильтрованный результат из общей базы.
        // При необходимости можно добавить вызов recipeDataUseCase.refreshRecipes(...)
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        recipeDataUseCase.clearResources();
        recipeLikeUseCase.clearResources();
    }
} 
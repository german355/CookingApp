package com.example.cooking.ui.viewmodels;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cooking.Recipe.Recipe;
import com.example.cooking.network.utils.Resource;
import com.example.cooking.ui.fragments.FilteredRecipesFragment;
import com.example.cooking.R;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.core.Completable;

/**
 * ViewModel для {@link FilteredRecipesFragment}.
 * Отвечает за фильтрацию рецептов на основе выбранной категории,
 * используя единый источник данных из SharedRecipeViewModel.
 */
public class FilteredRecipesViewModel extends AndroidViewModel {
    // LiveData для отфильтрованных рецептов
    private final MutableLiveData<List<Recipe>> filteredRecipes = new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    
    // Сохраняем последние параметры фильтрации для обновления данных
    private String lastFilterKey;
    private String lastFilterType;
    
    // Ссылка на общую модель
    private SharedRecipeViewModel sharedViewModel;
    private final androidx.lifecycle.Observer<Resource<List<Recipe>>> recipesObserver;

    public FilteredRecipesViewModel(@NonNull Application application) {
        super(application);
        // НЕ создаем новый экземпляр, так как это приводит к дублированию запросов!
        // SharedRecipeViewModel должен передаваться извне или использоваться общий экземпляр
        sharedViewModel = null; // Временно устанавливаем null
        recipesObserver = this::handleRecipesUpdate;
    }
    
    /**
     * Устанавливает SharedRecipeViewModel (должен вызываться из Activity/Fragment)
     */
    public void setSharedRecipeViewModel(SharedRecipeViewModel sharedRecipeViewModel) {
        if (this.sharedViewModel != null) {
            // Удаляем старый наблюдатель если был
            this.sharedViewModel.getRecipes().removeObserver(recipesObserver);
        }
        
        this.sharedViewModel = sharedRecipeViewModel;
        
        if (sharedRecipeViewModel != null) {
            // Отслеживаем изменения в общем списке рецептов для автоматической перефильтрации
            sharedRecipeViewModel.getRecipes().observeForever(recipesObserver);
        }
    }
    
    /**
     * Обработчик обновлений рецептов
     */
    private void handleRecipesUpdate(Resource<List<Recipe>> resource) {
        if (resource != null && resource.isSuccess() && lastFilterKey != null && lastFilterType != null) {
            // Если основной список рецептов обновился, повторно применяем фильтры
            applyFilters(lastFilterKey, lastFilterType, resource);
        }
    }

    /**
     * Возвращает LiveData со списком отфильтрованных рецептов.
     */
    public LiveData<List<Recipe>> getFilteredRecipes() {
        return filteredRecipes;
    }

    /**
     * Возвращает LiveData с сообщениями об ошибках.
     */
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * Возвращает LiveData со статусом загрузки из SharedViewModel.
     */
    public LiveData<Boolean> getIsRefreshing() {
        if (sharedViewModel == null) {
            MutableLiveData<Boolean> dummy = new MutableLiveData<>(false);
            return dummy;
        }
        return sharedViewModel.getIsRefreshing();
    }

    /**
     * Загружает список рецептов на основе ключа и типа фильтра.
     * Использует данные из SharedRecipeViewModel.
     * 
     * @param filterKey Ключ для фильтрации (например, "завтрак", "суп").
     * @param filterType Тип фильтра (например, "meal_type", "food_type").
     */
    public void loadFilteredRecipes(String filterKey, String filterType) {
        if (sharedViewModel == null) {
            errorMessage.setValue(getApplication().getString(R.string.shared_recipe_view_model_not_set));
            return;
        }
        
        // Сохраняем параметры для возможного повторного применения фильтров
        this.lastFilterKey = filterKey;
        this.lastFilterType = filterType;
        
        // Получаем текущие данные из SharedViewModel
        Resource<List<Recipe>> resource = sharedViewModel.getRecipes().getValue();
        
        // Если данные уже загружены, фильтруем их
        if (resource != null && resource.isSuccess()) {
            applyFilters(filterKey, filterType, resource);
        } else {

        }
    }
    
    /**
     * Применяет фильтры к списку рецептов.
     */
    private void applyFilters(String filterKey, String filterType, Resource<List<Recipe>> resource) {
        if (resource == null || !resource.isSuccess() || resource.getData() == null) {
            filteredRecipes.postValue(Collections.emptyList());
            errorMessage.postValue(getApplication().getString(R.string.filtered_recipes_data_unavailable));
            return;
        }

        List<Recipe> allRecipes = resource.getData();

        io.reactivex.rxjava3.core.Completable.fromAction(() -> {
            try {
                // Фильтруем рецепты в зависимости от типа фильтра
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

                filteredRecipes.postValue(filtered);

                if (filtered.isEmpty()) {
                    errorMessage.postValue(getApplication().getString(R.string.filtered_recipes_not_found_for_category, filterKey));
                }
            } catch (Exception e) {
                errorMessage.postValue(getApplication().getString(R.string.filtered_recipes_filter_error, e.getMessage()));
                android.util.Log.e("FilteredViewModel", "Ошибка при фильтрации рецептов", e);
            }
        })
        .subscribeOn(io.reactivex.rxjava3.schedulers.Schedulers.io())
        .subscribe();
    }

    /**
     * Обновляет статус "лайк" для указанного рецепта.
     * Делегирует обновление в SharedRecipeViewModel.
     * 
     * @param recipe Рецепт, статус которого нужно изменить.
     * @param isLiked Новое состояние лайка (true - лайкнут, false - дизлайкнут).
     */
    public void toggleLikeStatus(Recipe recipe, boolean isLiked) {
        if (sharedViewModel == null) {
            errorMessage.setValue(getApplication().getString(R.string.shared_recipe_view_model_not_set));
            return;
        }
        sharedViewModel.updateLikeStatus(recipe, isLiked);
    }
    
    /**
     * Выполняет принудительное обновление данных с сервера
     */
    public void refreshData() {
        if (sharedViewModel == null) {
            errorMessage.setValue(getApplication().getString(R.string.shared_recipe_view_model_not_set));
            return;
        }
        sharedViewModel.refreshRecipes();
    }

    /**
     * Обработка запроса фильтрации от UI
     */
    public void onFilterRequested(String filterKey, String filterType) {
        loadFilteredRecipes(filterKey, filterType);
    }

    /**
     * Обработка запроса на обновление от UI
     */
    public void onRefreshRequested() {
        refreshData();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Удаляем наблюдателя, чтобы избежать утечек памяти
        if (sharedViewModel != null) {
            sharedViewModel.getRecipes().removeObserver(recipesObserver);
        }
    }
} 
package com.example.cooking.ui.Catalog;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;

import com.example.cooking.Recipe.Recipe;
import com.example.cooking.network.utils.Resource;
import com.example.cooking.ui.viewmodels.SharedRecipeViewModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
    private final SharedRecipeViewModel sharedViewModel;

    public FilteredRecipesViewModel(@NonNull Application application) {
        super(application);
        sharedViewModel = new SharedRecipeViewModel(application);
        
        // Отслеживаем изменения в общем списке рецептов для автоматической перефильтрации
        sharedViewModel.getRecipes().observeForever(resource -> {
            if (resource != null && resource.isSuccess() && lastFilterKey != null && lastFilterType != null) {
                // Если основной список рецептов обновился, повторно применяем фильтры
                applyFilters(lastFilterKey, lastFilterType, resource);
            }
        });
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
        // Сохраняем параметры для возможного повторного применения фильтров
        this.lastFilterKey = filterKey;
        this.lastFilterType = filterType;
        
        // Получаем текущие данные из SharedViewModel
        Resource<List<Recipe>> resource = sharedViewModel.getRecipes().getValue();
        
        // Если данные уже загружены, фильтруем их
        if (resource != null && resource.isSuccess()) {
            applyFilters(filterKey, filterType, resource);
        } else {
            // Если данных еще нет, запускаем загрузку, а фильтрация произойдет 
            // автоматически через observeForever при получении данных
            sharedViewModel.refreshIfNeeded();
        }
    }
    
    /**
     * Применяет фильтры к списку рецептов.
     */
    private void applyFilters(String filterKey, String filterType, Resource<List<Recipe>> resource) {
        if (resource == null || !resource.isSuccess() || resource.getData() == null) {
            filteredRecipes.setValue(Collections.emptyList());
            errorMessage.setValue("Данные недоступны для фильтрации");
            return;
        }
        
        List<Recipe> allRecipes = resource.getData();
        
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
            
            filteredRecipes.setValue(filtered);
            
            if (filtered.isEmpty()) {
                errorMessage.setValue("Не найдено рецептов для категории '" + filterKey + "'");
            }
        } catch (Exception e) {
            errorMessage.setValue("Ошибка при фильтрации рецептов: " + e.getMessage());
            android.util.Log.e("FilteredViewModel", "Ошибка при фильтрации рецептов", e);
        }
    }

    /**
     * Обновляет статус "лайк" для указанного рецепта.
     * Делегирует обновление в SharedRecipeViewModel.
     * 
     * @param recipe Рецепт, статус которого нужно изменить.
     * @param isLiked Новое состояние лайка (true - лайкнут, false - дизлайкнут).
     */
    public void toggleLikeStatus(Recipe recipe, boolean isLiked) {
        sharedViewModel.updateLikeStatus(recipe, isLiked);
    }
    
    /**
     * Выполняет принудительное обновление данных с сервера
     */
    public void refreshData() {
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
        sharedViewModel.getRecipes().removeObserver(resource -> {});
    }
} 
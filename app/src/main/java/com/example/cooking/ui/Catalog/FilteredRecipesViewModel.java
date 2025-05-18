package com.example.cooking.ui.Catalog;

import android.app.Application;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.cooking.Recipe.Recipe;
import com.example.cooking.data.database.AppDatabase;
import com.example.cooking.data.database.RecipeDao;
import com.example.cooking.data.database.RecipeEntity;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel для {@link FilteredRecipesFragment}.
 * Отвечает за загрузку и управление списком отфильтрованных рецептов,
 * а также за обработку изменения статуса "лайк" для рецептов.
 */
public class FilteredRecipesViewModel extends AndroidViewModel {
    private final RecipeDao recipeDao;
    private final MutableLiveData<List<Recipe>> filteredRecipes = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final ExecutorService executorService;

    public FilteredRecipesViewModel(@NonNull Application application) {
        super(application);
        recipeDao = AppDatabase.getInstance(application).recipeDao();
        executorService = Executors.newSingleThreadExecutor();
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
     * Загружает список рецептов на основе ключа и типа фильтра.
     * Операция выполняется асинхронно.
     * @param filterKey Ключ для фильтрации (например, "завтрак", "суп").
     * @param filterType Тип фильтра (например, "meal_type", "food_type").
     */
    public void loadFilteredRecipes(String filterKey, String filterType) {
        executorService.execute(() -> {
            try {
                List<RecipeEntity> entities;
                if ("meal_type".equals(filterType)) {
                    entities = recipeDao.getRecipesByMealType(filterKey);
                } else if ("food_type".equals(filterType)) {
                    entities = recipeDao.getRecipesByFoodType(filterKey);
                } else {
                    entities = new ArrayList<>();
                    errorMessage.postValue("Неизвестный тип фильтра: " + filterType);
                }

                List<Recipe> recipes = new ArrayList<>();
                for (RecipeEntity entity : entities) {
                    recipes.add(entity.toRecipe());
                }
                filteredRecipes.postValue(recipes);
            } catch (Exception e) {
                errorMessage.postValue("Ошибка загрузки рецептов: " + e.getMessage());
                android.util.Log.e("FilteredViewModel", "Ошибка загрузки рецептов", e);
            }
        });
    }

    /**
     * Изменяет статус "лайк" для указанного рецепта.
     * Обновляет данные в базе данных и соответствующим образом обновляет LiveData {@link #filteredRecipes}.
     * Операция выполняется асинхронно.
     * @param recipe Рецепт, статус которого нужно изменить.
     * @param isLiked Новое состояние лайка (true - лайкнут, false - дизлайкнут).
     */
    public void toggleLikeStatus(Recipe recipe, boolean isLiked) {
        executorService.execute(() -> {
            try {
                // Обновление сущности в базе данных
                RecipeEntity entity = recipeDao.getRecipeById(recipe.getId());
                if (entity != null) {
                    entity.setLiked(isLiked);
                    recipeDao.update(entity);

                    // Обновление LiveData для немедленного отражения изменений в UI
                    List<Recipe> currentRecipes = filteredRecipes.getValue();
                    if (currentRecipes != null) {
                        List<Recipe> updatedRecipes = new ArrayList<>(currentRecipes);
                        for (int i = 0; i < updatedRecipes.size(); i++) {
                            if (updatedRecipes.get(i).getId() == recipe.getId()) {
                                // Для корректной работы DiffUtil важно, чтобы объект в списке
                                // был обновлен. Здесь мы обновляем существующий объект в новой копии списка.
                                Recipe recipeToUpdate = updatedRecipes.get(i);
                                recipeToUpdate.setLiked(isLiked); 
                                updatedRecipes.set(i, recipeToUpdate); 
                                break;
                            }
                        }
                        filteredRecipes.postValue(updatedRecipes);
                    }
                } else {
                    errorMessage.postValue("Рецепт (ID: " + recipe.getId() + ") не найден в БД для обновления лайка.");
                    android.util.Log.e("FilteredViewModel", "Рецепт не найден в БД для обновления лайка: " + recipe.getId());
                }
            } catch (Exception e) {
                errorMessage.postValue("Ошибка обновления статуса лайка: " + e.getMessage());
                android.util.Log.e("FilteredViewModel", "Ошибка обновления статуса лайка", e);
            }
        });
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        // Остановка ExecutorService при уничтожении ViewModel
        executorService.shutdown();
    }
} 
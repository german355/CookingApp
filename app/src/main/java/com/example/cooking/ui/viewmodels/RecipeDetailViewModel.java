package com.example.cooking.ui.viewmodels;

import android.app.Application;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.cooking.Recipe.Recipe;
import com.example.cooking.data.repositories.LikedRecipesRepository;
import com.example.cooking.network.utils.Resource;
import com.example.cooking.utils.MySharedPreferences;
import java.util.List;


/**
 * ViewModel для экрана детальной информации о рецепте
 */
public class RecipeDetailViewModel extends AndroidViewModel {
    private static final String TAG = "RecipeDetailViewModel";
    
    private final SharedRecipeViewModel sharedRecipeViewModel;
    private final LikedRecipesRepository likedRecipesRepository;
    
    private final MutableLiveData<Recipe> recipe = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLikedLiveData = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> deleteSuccess = new MutableLiveData<>(false);
    
    private int recipeId;
    private int userPermission;
    private String userId;
    
    private final java.util.concurrent.ExecutorService executor = 
            java.util.concurrent.Executors.newSingleThreadExecutor();
    
    /**
     * Конструктор
     */
    public RecipeDetailViewModel(@NonNull Application application) {
        super(application);
        this.likedRecipesRepository = new LikedRecipesRepository(application);
        this.sharedRecipeViewModel = new SharedRecipeViewModel(application);
        
        // Получаем userId текущего пользователя
        MySharedPreferences preferences = new MySharedPreferences(application);
        this.userId = preferences.getString("userId", "0");
    }
    
    /**
     * Загружает данные о рецепте
     */
    public void init(int recipeId, int permission) {
        this.recipeId = recipeId;
        this.userPermission = permission;
        loadRecipe();
        // Инициализируем статус лайка через observeLikeStatus при первой загрузке
    }
    
    /**
     * Загружает рецепт из SharedRecipeViewModel
     */
    private void loadRecipe() {
        isLoading.setValue(true);
        
        // Подписываемся на обновления рецептов
        sharedRecipeViewModel.getRecipes().observeForever(new androidx.lifecycle.Observer<Resource<List<Recipe>>>() {
            @Override
            public void onChanged(Resource<List<Recipe>> resource) {
                if (resource.getStatus() == Resource.Status.SUCCESS && resource.getData() != null) {
                    // Ищем нужный рецепт в списке
                    for (Recipe r : resource.getData()) {
                        if (r.getId() == recipeId) {
                            recipe.postValue(r);
                            updateLikeStatus(r);
                            break;
                        }
                    }
                } else if (resource.getStatus() == Resource.Status.ERROR) {
                    errorMessage.postValue(resource.getMessage());
                }
                isLoading.postValue(false);
                // Удаляем наблюдателя после получения данных
                sharedRecipeViewModel.getRecipes().removeObserver(this);
            }
        });
        
        // Запрашиваем обновление данных, если нужно
        sharedRecipeViewModel.loadInitialRecipesIfNeeded();
    }
    
    /**
     * Обновляет статус лайка для текущего рецепта
     */
    private void updateLikeStatus(Recipe recipe) {
        if (userId.equals("0")) {
            return;
        }
        
        // Проверяем, лайкнут ли рецепт
        boolean isLiked = recipe != null && recipe.isLiked();
        isLikedLiveData.postValue(isLiked);
    }
    
    /**
     * Переключает лайк для текущего рецепта
     */
    public void toggleLike() {
        if (userId.equals("0")) {
            errorMessage.setValue("Чтобы поставить лайк, необходимо войти в аккаунт");
            return;
        }
        
        Recipe currentRecipe = recipe.getValue();
        if (currentRecipe == null) {
            return;
        }
        
        // Получаем текущее состояние
        Boolean currentLiked = isLikedLiveData.getValue();
        if (currentLiked == null) {
            return;
        }
        
        // Обновляем через SharedRecipeViewModel
        sharedRecipeViewModel.updateLikeStatus(currentRecipe, !currentLiked, userId);
        
        // Обновляем локальное состояние
        currentRecipe.setLiked(!currentLiked);
        recipe.postValue(currentRecipe);
        isLikedLiveData.setValue(!currentLiked);
    }
    
    /**
     * Удаляет рецепт
     */
    /**
     * Удаляет рецепт
     */
    public void deleteRecipe() {
        if (!sharedRecipeViewModel.isNetworkAvailable()) {
            errorMessage.setValue("Отсутствует подключение к интернету");
            return;
        }
        
        isLoading.setValue(true);
        
        // Используем SharedRecipeViewModel для удаления рецепта
        sharedRecipeViewModel.deleteRecipe(recipeId, userId, userPermission, new SharedRecipeViewModel.DeleteRecipeCallback() {
            @Override
            public void onDeleteSuccess() {
                isLoading.postValue(false);
                deleteSuccess.postValue(true);
            }
            
            @Override
            public void onDeleteFailure(String error) {
                isLoading.postValue(false);
                errorMessage.postValue(error);
            }
        });
    }
    
    /**
     * Проверяет подключение к интернету
     */
    private boolean isNetworkAvailable() {
        return sharedRecipeViewModel.isNetworkAvailable();
    }
    
    /**
     * Наблюдает за статусом лайка для текущего рецепта
     *
     * @param lifecycleOwner владелец жизненного цикла для наблюдения
     */
    /**
     * Наблюдает за статусом лайка для текущего рецепта
     *
     * @param lifecycleOwner владелец жизненного цикла для наблюдения
     */
    public void observeLikeStatus(androidx.lifecycle.LifecycleOwner lifecycleOwner) {
        // Обновляем статус лайка при инициализации
        Recipe currentRecipe = recipe.getValue();
        if (currentRecipe != null) {
            updateLikeStatus(currentRecipe);
        }
        
        // Подписываемся на обновления рецепта
        recipe.observe(lifecycleOwner, updatedRecipe -> {
            if (updatedRecipe != null) {
                updateLikeStatus(updatedRecipe);
                Log.d(TAG, "RecipeDetailViewModel: Обновлен статус лайка. recipeId=" + 
                      recipeId + " isLiked=" + updatedRecipe.isLiked());
            }
        });
    }
    
    // Геттеры для LiveData
    public LiveData<Recipe> getRecipe() {
        return recipe;
    }
    
    /**
     * Возвращает LiveData с информацией о загрузке
     */
    
    /**
     * Возвращает LiveData с информацией о том, лайкнут ли рецепт
     */
    public LiveData<Boolean> getIsLiked() {
        return isLikedLiveData;
    }
    
    /**
     * Возвращает LiveData с информацией о процессе загрузки
     */
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }
    
    /**
     * Возвращает LiveData с сообщениями об ошибках
     */
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }
    
    /**
     * Возвращает LiveData с информацией об успешном удалении рецепта
     */
    public LiveData<Boolean> getDeleteSuccess() {
        return deleteSuccess;
    }
}
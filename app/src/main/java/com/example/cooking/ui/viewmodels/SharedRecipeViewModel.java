package com.example.cooking.ui.viewmodels;

import android.app.Application;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cooking.Recipe.Recipe;
import com.example.cooking.data.repositories.UnifiedRecipeRepository;
import com.example.cooking.domain.usecases.RecipeUseCases;
import com.example.cooking.network.utils.Resource;
import com.example.cooking.utils.MySharedPreferences;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * SharedViewModel для данных о рецептах.
 * Является единым источником данных для всех фрагментов, работающих с рецептами.
 */
public class SharedRecipeViewModel extends AndroidViewModel {
    private static final String TAG = "SharedRecipeViewModel";

    // Минимальный интервал между обновлениями данных с сервера (5 минут)
    private static final long MIN_REFRESH_INTERVAL = 2 * 60 * 1000;

    // Use Cases и репозиторий
    private final RecipeUseCases recipeUseCases;
    private final UnifiedRecipeRepository repository;
    private final ExecutorService executor;

    // LiveData для рецептов с обернутым статусом
    private final MutableLiveData<Resource<List<Recipe>>> recipes = new MutableLiveData<>(Resource.loading(null));
    
    // LiveData для результатов поиска
    private final MutableLiveData<List<Recipe>> searchResults = new MutableLiveData<>();
    
    // LiveData для состояния загрузки и ошибок
    private final MutableLiveData<Boolean> isRefreshing = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // Флаг для отслеживания первичной загрузки
    private boolean isInitialLoadDone = false;

    // Время последнего обновления данных с сервера
    private long lastRefreshTime = 0;

    public SharedRecipeViewModel(@NonNull Application application) {
        super(application);
        this.executor = Executors.newFixedThreadPool(2);
        this.recipeUseCases = new RecipeUseCases(application, executor);
        this.repository = new UnifiedRecipeRepository(application, executor);

        // Инициализация наблюдения за данными из локального репозитория
        initLocalDataObserver();
    }

    /**
     * Настройка наблюдения за локальными данными
     */
    private void initLocalDataObserver() {
        repository.getAllRecipesLocal().observeForever(recipesList -> {
            if (recipesList != null && !recipesList.isEmpty()) {
                recipes.setValue(Resource.success(recipesList));
                Log.d(TAG, "Локальные данные обновлены: " + recipesList.size() + " рецептов");
            }
        });
    }

    /**
     * Получить LiveData со списком рецептов
     */
    public LiveData<Resource<List<Recipe>>> getRecipes() {
        if (!isInitialLoadDone) {
            loadInitialRecipes();
        }
        return recipes;
    }

    /**
     * Получить LiveData с результатами поиска
     */
    public LiveData<List<Recipe>> getSearchResults() {
        return searchResults;
    }

    /**
     * Получить LiveData с состоянием обновления данных
     */
    public LiveData<Boolean> getIsRefreshing() {
        return isRefreshing;
    }

    /**
     * Получить LiveData с сообщением об ошибке
     */
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Загрузка рецептов при первом запуске
     */
    public void loadInitialRecipes() {
        if (!isInitialLoadDone) {
            Log.d(TAG, "Выполняется первичная загрузка рецептов");
            
            // ИЗМЕНЕНО: Сначала загружаем локальные данные
            loadLocalRecipesAndThenRefresh();
            isInitialLoadDone = true;
        } else {
            refreshIfNeeded();
        }
    }

    /**
     * Загружает локальные данные, а затем запускает обновление с сервера
     */
    private void loadLocalRecipesAndThenRefresh() {
        // Показываем, что данные загружаются (краткосрочно)
        isRefreshing.setValue(true);
        
        // Запускаем загрузку локальных данных в фоновом потоке
        executor.execute(() -> {
            List<Recipe> localRecipesList = repository.getAllRecipesSync();
            
            if (localRecipesList != null && !localRecipesList.isEmpty()) {
                Log.d(TAG, "Локально доступно " + localRecipesList.size() + " рецептов. Показываем их перед обновлением с сервера.");
                
                // Применяем статус лайков
                Set<Integer> likedRecipeIds = repository.getLikedRecipeIds();
                for (Recipe recipe : localRecipesList) {
                    recipe.setLiked(likedRecipeIds.contains(recipe.getId()));
                }
                
                // Обновляем UI с локальными данными
                recipes.postValue(Resource.success(localRecipesList));
                
                // Скрываем индикатор загрузки, так как локальные данные уже доступны
                isRefreshing.postValue(false);
                
                // Запускаем фоновое обновление с сервера асинхронно
                updateFromServerInBackground();
            } else {
                Log.d(TAG, "Локальные данные отсутствуют. Загружаем с сервера.");
                // Если локальных данных нет, загружаем данные с сервера обычным образом
                // и оставляем индикатор загрузки активным
                new Handler(Looper.getMainLooper()).post(this::refreshRecipes);
            }
        });
    }
    
    /**
     * Запускает фоновое обновление данных с сервера без блокирования UI
     */
    private void updateFromServerInBackground() {
        Log.d(TAG, "Запущено фоновое обновление данных с сервера");
        
        // Обновляем время последнего обновления
        lastRefreshTime = System.currentTimeMillis();
        
        // Создаем отдельную LiveData для отслеживания ошибок и результата обновления
        final MutableLiveData<Boolean> bgRefreshingStatus = new MutableLiveData<>();
        final MutableLiveData<String> bgErrorMessage = new MutableLiveData<>();
        
        // Настраиваем наблюдение за ошибками в главном потоке (это безопасно, так как
        // выполняется перед запуском фонового процесса)
        new Handler(Looper.getMainLooper()).post(() -> {
            // Устанавливаем начальное значение в главном потоке
            bgRefreshingStatus.setValue(true);
            
            // Настраиваем наблюдателя за ошибками
            androidx.lifecycle.Observer<String> errorObserver = new androidx.lifecycle.Observer<String>() {
                @Override
                public void onChanged(String error) {
                    if (error != null && !error.isEmpty()) {
                        Log.e(TAG, "Ошибка фонового обновления: " + error);
                    }
                    // После получения ошибки удаляем наблюдателя, чтобы избежать утечек памяти
                    bgErrorMessage.removeObserver(this);
                }
            };
            
            // Безопасно добавляем наблюдателя в главном потоке
            bgErrorMessage.observeForever(errorObserver);
        });
        
        // Используем Use Case для обновления рецептов с колбэком по завершении
        recipeUseCases.refreshRecipes(bgRefreshingStatus, bgErrorMessage, null, () -> {
            Log.d(TAG, "Фоновое обновление с сервера завершено");
            // Очищаем статус в главном потоке
            new Handler(Looper.getMainLooper()).post(() -> {
                bgRefreshingStatus.setValue(false);
                Log.d(TAG, "Фоновое обновление завершено, индикаторы сброшены");
            });
        });
    }

    /**
     * Обновление списка рецептов с сервера если прошло достаточно времени
     */
    public void refreshIfNeeded() {
        long currentTime = System.currentTimeMillis();
        
        // Если данных нет, или прошло больше минимального интервала - обновляем с сервера
        if (recipes.getValue() == null || recipes.getValue().getData() == null || 
                recipes.getValue().getData().isEmpty() || 
                (currentTime - lastRefreshTime) > MIN_REFRESH_INTERVAL) {
            Log.d(TAG, "Обновление данных с сервера (прошло " + ((currentTime - lastRefreshTime) / 1000) + " секунд)");
            refreshRecipes();
        } else {
            // Иначе просто загружаем из локального хранилища
            Log.d(TAG, "Обновление не требуется, загружаем локальные данные");
            loadLocalRecipes();
        }
    }

    /**
     * Обновление списка рецептов с сервера
     */
    public void refreshRecipes() {
        isRefreshing.setValue(true);
        Log.d(TAG, "Обновление рецептов с сервера...");
        
        // Устанавливаем время последнего обновления
        lastRefreshTime = System.currentTimeMillis();
        
        // Используем Use Case для обновления рецептов
        recipeUseCases.refreshRecipes(isRefreshing, errorMessage, recipes, null);
    }

    /**
     * Обновление статуса лайка рецепта
     */
    public void updateLikeStatus(Recipe recipe, boolean isLiked) {
        if (recipe == null) {
            Log.e(TAG, "updateLikeStatus: рецепт равен null");
            return;
        }

        int recipeId = recipe.getId();
        String userId = new MySharedPreferences(getApplication()).getString("userId", "0");
        
        // Используем UseCase для установки статуса лайка
        recipeUseCases.setLikeStatus(userId, recipeId, isLiked, errorMessage);
        
        // Логирование можно оставить здесь или перенести в UseCase/Repository, если нужно
        Log.d(TAG, "Запрошено обновление статуса лайка рецепта " + recipeId + " на " + isLiked);
    }

    /**
     * Выполнить задачу, если ViewModel активна
     */
    private void executeIfActive(Runnable task) {
        try {
            executor.execute(task);
        } catch (Exception e) {
            Log.e(TAG, "Ошибка выполнения задачи: " + e.getMessage(), e);
        }
    }
    
    /**
     * Выполнить поиск среди рецептов
     */
    public void searchRecipes(String query) {
        if (query == null || query.trim().isEmpty()) {
            // Если запрос пустой, загружаем и показываем все локальные рецепты
            Log.d(TAG, "Поисковый запрос пуст, показываем все рецепты");
            // Останавливаем индикатор загрузки
            isRefreshing.setValue(false);
            
            // Загружаем все рецепты из локальной БД в фоновом потоке
            executor.execute(() -> {
                try {
                    List<Recipe> allRecipes = repository.getAllRecipesSync();
                    if (allRecipes != null && !allRecipes.isEmpty()) {
                        Log.d(TAG, "Загружено " + allRecipes.size() + " рецептов для отображения вместо пустого поиска");
                        // Применяем статус лайков
                        Set<Integer> likedRecipeIds = repository.getLikedRecipeIds();
                        for (Recipe recipe : allRecipes) {
                            recipe.setLiked(likedRecipeIds.contains(recipe.getId()));
                        }
                        // Загружаем все рецепты в результаты поиска
                        searchResults.postValue(allRecipes);
                    } else {
                        Log.d(TAG, "Нет локальных рецептов для отображения");
                        // Если локальных рецептов нет, устанавливаем пустой список
                        searchResults.postValue(new ArrayList<>());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при загрузке рецептов для пустого поиска: " + e.getMessage(), e);
                    searchResults.postValue(new ArrayList<>());
                }
            });
            return;
        }
        
        MySharedPreferences preferences = new MySharedPreferences(getApplication());
        boolean smartSearchEnabled = preferences.getBoolean("smart_search_enabled", true);
        

        
        recipeUseCases.searchRecipes(query, smartSearchEnabled, searchResults, errorMessage, isRefreshing);

    }

    /**
     * Загрузка рецептов из локального хранилища без обращения к серверу
     */
    public void loadLocalRecipes() {
        isRefreshing.setValue(true); // Показываем индикатор загрузки

        executor.execute(() -> {
            List<Recipe> localRecipesList = repository.getAllRecipesSync(); 
            if (localRecipesList != null && !localRecipesList.isEmpty()) {
                Log.d(TAG, "(BG) Загружено " + localRecipesList.size() + " рецептов из локального хранилища");
                // Применяем статус лайков (этот блок тоже должен быть в фоновом потоке, если getLikedRecipeIds делает запрос к БД)
                Set<Integer> likedRecipeIds = repository.getLikedRecipeIds(); // getLikedRecipeIds уже должен быть потокобезопасным или вызываться в фоне
                for (Recipe recipe : localRecipesList) {
                    recipe.setLiked(likedRecipeIds.contains(recipe.getId()));
                }
                recipes.postValue(Resource.success(localRecipesList));
            } else {
                Log.d(TAG, "(BG) Локальные данные отсутствуют, возможно, стоит загрузить с сервера");

                new Handler(Looper.getMainLooper()).post(this::refreshRecipes); 
            }
            isRefreshing.postValue(false); // Скрываем индикатор загрузки
        });
    }

    /**
     * Переключает статус лайка для рецепта по его ID
     * @param userId ID пользователя
     * @param recipeId ID рецепта
     */
    public void toggleLike(String userId, int recipeId) {
        if (userId == null || userId.equals("0") || userId.isEmpty()) {
            Log.w(TAG, "Нельзя переключить лайк: невалидный userId=" + userId);
            errorMessage.setValue("Войдите, чтобы добавить рецепт в избранное");
            return;
        }
        
        // Используем UseCase
        recipeUseCases.toggleLike(userId, recipeId, errorMessage);
    }
    
    /**
     * Интерфейс для колбэка удаления рецепта
     */
    public interface DeleteRecipeCallback {
        void onDeleteSuccess();
        void onDeleteFailure(String error);
    }
    
    /**
     * Удаляет рецепт
     * @param recipeId ID рецепта
     * @param userId ID пользователя
     * @param userPermission уровень прав пользователя
     * @param callback колбэк с результатом операции
     */
    public void deleteRecipe(int recipeId, String userId, int userPermission, DeleteRecipeCallback callback) {
        if (!isNetworkAvailable()) {
            if (callback != null) {
                callback.onDeleteFailure("Отсутствует подключение к интернету");
            }
            return;
        }
        
        isRefreshing.setValue(true);
        
        executor.execute(() -> {
            try {
                // Удаляем рецепт через репозиторий
                // Используем метод удаления из репозитория
                repository.deleteRecipe(recipeId, new UnifiedRecipeRepository.DeleteRecipeCallback() {
                    @Override
                    public void onDeleteSuccess() {
                        // Обновляем список рецептов после удаления
                        refreshRecipes();
                        if (callback != null) {
                            new Handler(Looper.getMainLooper()).post(callback::onDeleteSuccess);
                        }
                    }

                    @Override
                    public void onDeleteFailure(String error) {
                        if (callback != null) {
                            new Handler(Looper.getMainLooper()).post(() -> 
                                callback.onDeleteFailure(error)
                            );
                        }
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при удалении рецепта", e);
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> 
                        callback.onDeleteFailure("Ошибка при удалении рецепта: " + e.getMessage())
                    );
                }
            } finally {
                isRefreshing.postValue(false);
            }
        });
    }
    
    /**
     * Проверяет доступность сети
     */
    public boolean isNetworkAvailable() {
        return recipeUseCases.isNetworkAvailable();
    }
    
    /**
     * Загружает рецепты при первом запуске, если они еще не загружены
     */
    public void loadInitialRecipesIfNeeded() {
        if (!isInitialLoadDone) {
            loadInitialRecipes();
        }
    }
    
    /**
     * Обновляет статус лайка для рецепта
     * @param recipe рецепт для обновления
     * @param isLiked новый статус лайка
     * @param userId ID пользователя
     */
    /**
     * Обновляет статус лайка для рецепта
     * @param recipe рецепт для обновления
     * @param isLiked новый статус лайка
     * @param userId ID пользователя
     */
    public void updateLikeStatus(Recipe recipe, boolean isLiked, String userId) {
        if (recipe == null) {
            Log.e(TAG, "updateLikeStatus: рецепт равен null");
            return;
        }
        
        if (userId == null || userId.equals("0") || userId.isEmpty()) {
            Log.w(TAG, "updateLikeStatus: невалидный userId");
            errorMessage.setValue("Войдите, чтобы изменить статус лайка");
            return;
        }
        
        // Обновляем локальное состояние
        recipe.setLiked(isLiked);
        
        // Обновляем в репозитории, используя setLikeStatus для явного указания статуса
        recipeUseCases.setLikeStatus(userId, recipe.getId(), isLiked, errorMessage);
        
        // Не вызываем refreshRecipes, так как данные обновляются через LiveData
        Log.d(TAG, "Статус лайка для рецепта " + recipe.getId() + " обновлен на " + isLiked);
    }
    
    /**
     * Очистка ресурсов при уничтожении ViewModel
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }
}
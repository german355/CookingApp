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


import java.util.List;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.rxjava3.disposables.CompositeDisposable;


/**
 * SharedViewModel для данных о рецептах.
 * Является единым источником данных для всех фрагментов, работающих с рецептами.
 */
public class SharedRecipeViewModel extends AndroidViewModel {
    private static final String TAG = "SharedRecipeViewModel";

    // Минимальный интервал между обновлениями данных с сервера (3 минуты)
    private static final long MIN_REFRESH_INTERVAL = 3* 60 * 1000;

    // Use Cases и репозиторий
    private final RecipeUseCases recipeUseCases;
    private final UnifiedRecipeRepository repository;
    private final ExecutorService executor;
    private final CompositeDisposable disposables = new CompositeDisposable();

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

    // Handler для периодического обновления
    private final Handler periodicHandler = new Handler(Looper.getMainLooper());
    // Запускаемый Runnable для обновления с сервера
    private final Runnable periodicRunnable = new Runnable() {
        @Override
        public void run() {
            updateFromServerInBackground();
            periodicHandler.postDelayed(this, MIN_REFRESH_INTERVAL);
        }
    };

    public SharedRecipeViewModel(@NonNull Application application) {
        super(application);
        this.executor = Executors.newFixedThreadPool(2);
        this.recipeUseCases = new RecipeUseCases(application, executor);
        this.repository = new UnifiedRecipeRepository(application);

        // Инициализация наблюдения за данными из локального репозитория
        initLocalDataObserver();
        // Запускаем периодический таск обновления
        periodicHandler.postDelayed(periodicRunnable, MIN_REFRESH_INTERVAL);
    }

    /**
     * Настройка наблюдения за локальными данными
     */
    private void initLocalDataObserver() {
        recipeUseCases.getAllRecipesLocalLiveData().observeForever(recipesList -> {
            // Обновляем UI любыми изменениями локальной БД, включая удаление всех элементов
            if (recipesList != null) {
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
            updateFromServerInBackground();
        }
    }

    /**
     * Загружает локальные данные, а затем запускает обновление с сервера
     */
    private void loadLocalRecipesAndThenRefresh() {
        // Показ локальных данных через initLocalDataObserver, затем синхронизация через UseCase
        isRefreshing.setValue(true);
        recipeUseCases.refreshRecipes(isRefreshing, errorMessage, recipes,
            () -> isRefreshing.setValue(false)
        );
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
        Log.d(TAG, "SharedRecipeViewModel.updateLikeStatus: recipeId=" + (recipe != null ? recipe.getId() : null) + ", isLiked=" + isLiked);
        if (recipe == null) {
            Log.e(TAG, "updateLikeStatus: рецепт равен null");
            return;
        }

        int recipeId = recipe.getId();
        String userId = new MySharedPreferences(getApplication()).getString("userId", "0");

        Log.d(TAG, "SharedRecipeViewModel: invoking recipeUseCases.setLikeStatus");
        // Используем UseCase для установки статуса лайка
        recipeUseCases.setLikeStatus(userId, recipeId, isLiked, errorMessage);
        Log.d(TAG, "SharedRecipeViewModel: recipeUseCases.setLikeStatus invoked");

        // Логирование можно оставить здесь или перенести в UseCase/Repository, если нужно
        Log.d(TAG, "Запрошено обновление статуса лайка рецепта " + recipeId + " на " + isLiked);
    }


    /**
     * Выполнить поиск среди рецептов
     */
    public void searchRecipes(String query) {
        // Используем UseCase для поиска, он управляет isRefreshing и LiveData
        isRefreshing.setValue(true);
        // Получаем настройку умного поиска из SharedPreferences
        boolean smartSearchEnabled = new MySharedPreferences(getApplication()).getBoolean("smart_search_enabled", true);
        // Запускаем поиск через UseCase с учетом smartSearchEnabled
        recipeUseCases.searchRecipes(
            query,
            smartSearchEnabled,
            searchResults,
            errorMessage,
            isRefreshing
        );
    }

    /**
     * Загрузка рецептов из локального хранилища без обращения к серверу
     */
    public void loadLocalRecipes() {
        // UI обновится через LiveData observer, индикатор сбрасываем
        isRefreshing.setValue(false);
        // Получаем все рецепты из локального хранилища и обновляем результаты поиска
        List<Recipe> all = repository.getAllRecipesSync();
        searchResults.setValue(all);
        recipes.setValue(Resource.success(all));
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

        // Удаляем рецепт через UseCase: удаление на сервере и из локальной БД, LiveData обновит UI
        isRefreshing.setValue(true);
        recipeUseCases.deleteRecipe(recipeId, new RecipeUseCases.DeleteRecipeCallback() {
            @Override
            public void onDeleteSuccess() {
                isRefreshing.setValue(false);
                if (callback != null) callback.onDeleteSuccess();
            }

            @Override
            public void onDeleteFailure(String error) {
                isRefreshing.setValue(false);
                if (callback != null) callback.onDeleteFailure(error);
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
    public void updateLikeStatus(Recipe recipe, boolean isLiked, String userId) {
        Log.d(TAG, "updateLikeStatus called: id=" + recipe.getId() + " liked=" + isLiked);
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
        disposables.clear();
        periodicHandler.removeCallbacks(periodicRunnable);
    }
}
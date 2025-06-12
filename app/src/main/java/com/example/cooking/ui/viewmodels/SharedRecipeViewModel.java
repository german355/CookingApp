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

    // Флаг для предотвращения множественных одновременных запросов
    private volatile boolean isCurrentlyRefreshing = false;

    // Флаг для отслеживания режима поиска
    private final MutableLiveData<Boolean> isInSearchMode = new MutableLiveData<>(false);

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
     * Получить LiveData с состоянием режима поиска
     */
    public LiveData<Boolean> getIsInSearchMode() {
        return isInSearchMode;
    }

    /**
     * Загрузка рецептов при первом запуске
     */
    public synchronized void loadInitialRecipes() {
        if (!isInitialLoadDone) {
            Log.d(TAG, "Выполняется первичная загрузка рецептов");

            // ИЗМЕНЕНО: Сначала загружаем локальные данные
            loadLocalRecipesAndThenRefresh();
            isInitialLoadDone = true;
        } else {
            Log.d(TAG, "Первичная загрузка уже выполнена, пропускаем");
        }
    }

    /**
     * Загружает локальные данные, а затем запускает обновление с сервера
     */
    private synchronized void loadLocalRecipesAndThenRefresh() {
        // Проверяем, не выполняется ли уже обновление
        if (isCurrentlyRefreshing) {
            Log.d(TAG, "Обновление уже выполняется, пропускаем дублирующий запрос");
            return;
        }
        
        isCurrentlyRefreshing = true;
        Log.d(TAG, "Начинаем загрузку локальных данных и обновление с сервера");
        
        // СНАЧАЛА читаем локальную БД в отдельном потоке, чтобы не блокировать главный
        executor.execute(() -> {
            List<Recipe> cachedRecipes = repository.getAllRecipesSync();
            int count = cachedRecipes != null ? cachedRecipes.size() : -1;
            Log.d(TAG, "Получено из БД " + count + " рецептов (до сетевого запроса)");

            // Обновляем UI только в главном потоке
            new Handler(Looper.getMainLooper()).post(() -> {
                if (cachedRecipes != null && !cachedRecipes.isEmpty()) {
                    Log.d(TAG, "Отображаю " + cachedRecipes.size() + " кэшированных рецептов до сетевого запроса");
                    recipes.setValue(Resource.success(cachedRecipes));
                } else {
                    Log.d(TAG, "Локальная БД пуста — отображать пока нечего");
                }
            });
        });

        // Показ локальных данных через initLocalDataObserver, затем синхронизация через UseCase
        isRefreshing.setValue(true);
        recipeUseCases.refreshRecipes(isRefreshing, errorMessage, recipes,
            () -> {
                isRefreshing.setValue(false);
                synchronized (this) {
                    isCurrentlyRefreshing = false; // Сбрасываем флаг после завершения
                    Log.d(TAG, "Загрузка локальных данных и обновление с сервера завершены");
                }
            }
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
    public synchronized void refreshRecipes() {
        // Проверяем, не выполняется ли уже обновление
        if (isCurrentlyRefreshing) {
            Log.d(TAG, "Обновление уже выполняется, пропускаем запрос");
            return;
        }
        
        isCurrentlyRefreshing = true;
        isRefreshing.setValue(true);
        Log.d(TAG, "Обновление рецептов с сервера...");

        // Устанавливаем время последнего обновления
        lastRefreshTime = System.currentTimeMillis();

        // Используем Use Case для обновления рецептов
        recipeUseCases.refreshRecipes(isRefreshing, errorMessage, recipes, () -> {
            synchronized (this) {
                isCurrentlyRefreshing = false; // Сбрасываем флаг после завершения
            }
        });
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

        // Обновляем локальное состояние
        recipe.setLiked(isLiked);

        // Явно уведомляем наблюдателей о том, что список рецептов изменился, чтобы UI обновился
        Resource<List<Recipe>> currentResource = recipes.getValue();
        if (currentResource != null && currentResource.getData() != null) {
            // Создаём новый объект списка, чтобы LiveData заметила изменение
            java.util.List<Recipe> updatedList = new java.util.ArrayList<>(currentResource.getData());
            recipes.setValue(Resource.success(updatedList));
        }
    }


    /**
     * Выполнить поиск среди рецептов
     */
    public void searchRecipes(String query) {
        // Устанавливаем режим поиска
        isInSearchMode.setValue(true);
        
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
        // Получаем все рецепты из локального хранилища и обновляем основной список
        List<Recipe> all = repository.getAllRecipesSync();
        recipes.setValue(Resource.success(all));
    }

    /**
     * Выход из режима поиска - показываем все рецепты
     */
    public void exitSearchMode() {
        // Выходим из режима поиска
        isInSearchMode.setValue(false);
        // Очищаем результаты поиска
        searchResults.setValue(null);
        // Отключаем индикатор загрузки
        isRefreshing.setValue(false);
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

        // Явно уведомляем наблюдателей о том, что список рецептов изменился, чтобы UI обновился
        Resource<List<Recipe>> currentResource = recipes.getValue();
        if (currentResource != null && currentResource.getData() != null) {
            // Создаём новый объект списка, чтобы LiveData заметила изменение
            java.util.List<Recipe> updatedList = new java.util.ArrayList<>(currentResource.getData());
            recipes.setValue(Resource.success(updatedList));
        }
    }

    /**
     * Очистка ресурсов при уничтожении ViewModel
     */
    @Override
    protected void onCleared() {
        super.onCleared();
        Log.d(TAG, "onCleared() вызван - начинаем очистку ресурсов");
        
        // Останавливаем периодические задачи
        periodicHandler.removeCallbacks(periodicRunnable);
        
        // Очищаем disposables
        disposables.clear();
        
        // Безопасно закрываем executor
        if (!executor.isShutdown()) {
            Log.d(TAG, "Закрываем ExecutorService");
            executor.shutdown();
            try {
                // Ждем завершения выполняющихся задач максимум 2 секунды
                if (!executor.awaitTermination(2, java.util.concurrent.TimeUnit.SECONDS)) {
                    Log.w(TAG, "ExecutorService не завершился за 2 секунды, принудительно останавливаем");
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                Log.w(TAG, "Прерывание при ожидании завершения ExecutorService", e);
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        } else {
            Log.d(TAG, "ExecutorService уже закрыт");
        }
    }
}
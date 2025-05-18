package com.example.cooking.data.repositories;

import android.content.Context;
import android.util.Log;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cooking.Recipe.Recipe;
import com.example.cooking.config.ServerConfig;
import com.example.cooking.data.database.AppDatabase;
import com.example.cooking.data.database.LikedRecipeDao;
import com.example.cooking.data.database.LikedRecipeEntity;
import com.example.cooking.data.database.RecipeDao;
import com.example.cooking.data.database.RecipeEntity;
import com.example.cooking.network.api.ApiService;
import com.example.cooking.network.responses.RecipesResponse;
import com.example.cooking.network.services.NetworkService;

// Импортируем RecipeLocalRepository
import com.example.cooking.data.repositories.RecipeLocalRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LikedRecipesRepository {
    private static final String TAG = "LikedRecipesRepository";
    private static final String API_URL = ServerConfig.BASE_API_URL;
    private final Context context; // Добавляем Context для проверки сети
    private final LikedRecipeDao likedRecipeDao;
    private final RecipeDao recipeDao;
    private final ApiService apiService;
    private final ExecutorService executor;
    private final RecipeLocalRepository recipeLocalRepository; // Добавляем зависимость

    public interface LikedRecipesCallback {
        void onRecipesLoaded(List<Recipe> recipes);

        void onDataNotAvailable(String error);
    }

    public LikedRecipesRepository(Context context) {
        this.context = context.getApplicationContext(); // Сохраняем Application Context
        AppDatabase db = AppDatabase.getInstance(this.context);
        likedRecipeDao = db.likedRecipeDao();
        recipeDao = db.recipeDao();
        executor = Executors.newSingleThreadExecutor();
        // Создаем экземпляр RecipeLocalRepository
        recipeLocalRepository = new RecipeLocalRepository(this.context);

        // Получаем ApiService из NetworkService
        apiService = NetworkService.getApiService(this.context);
    }

    /**
     * Получить LiveData ПОЛНЫХ лайкнутых рецептов из локальной базы данных (Room).
     * Загружает ID лайков, затем по этим ID получает полные данные из
     * RecipeLocalRepository.
     * Это основной метод для получения данных в UI.
     */
    public LiveData<List<Recipe>> getLikedRecipes(String userId) {
        // Проверяем чтение internalUserId из SharedPreferences
        com.example.cooking.utils.MySharedPreferences prefs = new com.example.cooking.utils.MySharedPreferences(
                context);
        String internalUserId = prefs.getString("userId", "0");
        Log.d(TAG, "getLikedRecipes: parameter userId=" + userId + ", SharedPreferences userId=" + internalUserId);
        syncLikedRecipesFromServerIfNeeded(internalUserId); // Запускаем фоновую синхронизацию лайков

        // Получаем LiveData списка ID лайкнутых рецептов (LikedRecipeEntity) для
        // internalUserId
        LiveData<List<LikedRecipeEntity>> likedEntitiesLiveData = likedRecipeDao.getLikedRecipesForUser(internalUserId);

        // Используем Transformations.switchMap для загрузки полных данных по ID
        return Transformations.switchMap(likedEntitiesLiveData, entities -> {
            MediatorLiveData<List<Recipe>> fullRecipesLiveData = new MediatorLiveData<>();
            if (entities == null || entities.isEmpty()) {
                fullRecipesLiveData.setValue(new ArrayList<>()); // Если лайков нет, возвращаем пустой список
                return fullRecipesLiveData;
            }

            // Получаем LiveData всех рецептов из локальной базы
            LiveData<List<Recipe>> allLocalRecipesLiveData = recipeLocalRepository.getAllRecipes();

            // Используем MediatorLiveData для объединения данных
            fullRecipesLiveData.addSource(allLocalRecipesLiveData, allLocalRecipes -> {
                List<Recipe> likedFullRecipes = new ArrayList<>();
                if (allLocalRecipes != null && !entities.isEmpty()) {
                    // Создаем карту всех локальных рецептов для быстрого доступа по ID
                    java.util.Map<Integer, Recipe> allRecipesMap = new java.util.HashMap<>();
                    for (Recipe r : allLocalRecipes) {
                        allRecipesMap.put(r.getId(), r);
                    }

                    // Формируем список лайкнутых рецептов с полными данными
                    for (LikedRecipeEntity entity : entities) {
                        Recipe fullRecipe = allRecipesMap.get(entity.getRecipeId());
                        if (fullRecipe != null) {
                            // Важно: Устанавливаем флаг isLiked вручную, т.к. он берется из общей таблицы
                            fullRecipe.setLiked(true);
                            likedFullRecipes.add(fullRecipe);
                        } else {
                            // Если полного рецепта нет в локальной базе (маловероятно при правильной
                            // синхронизации)
                            // Можно добавить заглушку или пропустить
                            Log.w(TAG, "Полный рецепт для liked recipeId " + entity.getRecipeId()
                                    + " не найден в RecipeLocalRepository");
                        }
                    }
                }
                // Сортируем по ID или дате, если нужно
                // Collections.sort(likedFullRecipes, ...);
                fullRecipesLiveData.setValue(likedFullRecipes);
                Log.d(TAG, "Сформирован полный список лайкнутых рецептов: " + likedFullRecipes.size());
            });

            return fullRecipesLiveData;
        });
    }

    /**
     * Запускает синхронизацию с сервером, если необходимо (например, есть сеть).
     * Можно добавить логику проверки времени последнего обновления.
     */
    public void syncLikedRecipesFromServerIfNeeded(final String userId) {
        if (userId == null || userId.equals("0") || userId.isEmpty()) {
            Log.w(TAG, "Пропуск синхронизации лайков: неверный userId=" + userId);
            return;
        }
        if (!isNetworkAvailable()) {
            Log.d(TAG, "Нет сети, синхронизация лайкнутых рецептов не выполняется.");
            return;
        }
        Log.d(TAG, "Запуск синхронизации лайкнутых рецептов с сервером для userId: " + userId);
        executor.execute(() -> fetchAndStoreLikedRecipes(userId));
    }

    /**
     * Выполняет сетевой запрос и сохраняет результат в БД.
     * Исправлено: использует общий apiService и обрабатывает RecipesResponse.
     */
    private void fetchAndStoreLikedRecipes(String userId) {
        Log.d(TAG, "[fetchAndStore] Выполнение запроса к apiService.getLikedRecipes для userId: " + userId);
        // Используем общий ApiService
        retrofit2.Call<RecipesResponse> call = apiService.getLikedRecipes(userId);
        try {
            // Используем execute() для синхронного выполнения в фоновом потоке Executor'a
            retrofit2.Response<RecipesResponse> response = call.execute();
            Log.d(TAG, "[fetchAndStore] Получен ответ от сервера: Code=" + response.code() + ", isSuccessful="
                    + response.isSuccessful());

            if (response.isSuccessful() && response.body() != null) {
                RecipesResponse recipesResponse = response.body();
                if (recipesResponse.isSuccess()) {
                    List<Recipe> recipes = recipesResponse.getRecipes();
                    if (recipes != null) {
                        Log.i(TAG, "[fetchAndStore] Успешно загружено " + recipes.size()
                                + " лайкнутых рецептов с сервера для userId: " + userId);
                        // Конвертируем и сохраняем
                        storeServerLikedRecipes(userId, recipes);
                    } else {
                        Log.w(TAG,
                                "[fetchAndStore] Сервер вернул success=true, но null список лайкнутых рецептов для userId: "
                                        + userId);
                        // Очищаем локальные лайки, раз сервер говорит, что их нет
                        storeServerLikedRecipes(userId, new ArrayList<>());
                    }
                } else {
                    // Сервер вернул success=false
                    Log.e(TAG, "[fetchAndStore] Ошибка при синхронизации лайкнутых рецептов (success=false): " +
                            "Code: " + response.code() + ", Message: " + recipesResponse.getMessage());
                }
            } else {
                // Неуспешный HTTP ответ
                Log.e(TAG, "[fetchAndStore] Ошибка при синхронизации лайкнутых рецептов (HTTP неудача): " +
                        "Code: " + response.code() + ", Message: " + response.message()); // Логируем код и стандартное
                                                                                          // сообщение
            }
        } catch (Exception e) {
            // Исключение при выполнении запроса или обработке ответа
            Log.e(TAG, "[fetchAndStore] Исключение при синхронизации лайкнутых рецептов для userId: " + userId,
                    e); // Логируем полный стектрейс
        }
    }

    /**
     * Конвертирует и сохраняет лайкнутые рецепты, полученные с сервера.
     * Перезаписывает старые данные для пользователя.
     */
    private void storeServerLikedRecipes(String userId, List<Recipe> serverRecipes) {
        executor.execute(() -> {
            List<LikedRecipeEntity> likedEntitiesToInsert = new ArrayList<>();
            List<RecipeEntity> recipeEntitiesToInsert = new ArrayList<>();

            for (Recipe recipe : serverRecipes) {
                if (recipe != null) { // Проверка на null
                    // Создаем сущность для таблицы лайков
                    likedEntitiesToInsert.add(new LikedRecipeEntity(recipe.getId(), userId));
                    // Создаем сущность для основной таблицы рецептов (для обновления/вставки)
                    recipeEntitiesToInsert.add(new RecipeEntity(recipe));
                }
            }

            // Выполняем операции в транзакции для атомарности
            try {
                Log.d(TAG, "[DB Sync] Запуск транзакции для обновления лайков userId: " + userId);
                AppDatabase.getInstance(context).runInTransaction(() -> {
                    // 1. Очистить старые лайки для этого пользователя
                    Log.d(TAG, "[DB Sync] Удаление старых записей из liked_recipes для userId: " + userId);
                    likedRecipeDao.deleteAllForUser(userId);
                    // 2. Вставить новые лайки, если они есть
                    if (!likedEntitiesToInsert.isEmpty()) {
                        Log.d(TAG, "[DB Sync] Вставка " + likedEntitiesToInsert.size()
                                + " новых записей в liked_recipes для userId: " + userId);
                        likedRecipeDao.insertAll(likedEntitiesToInsert);
                    }
                    // 3. Вставить/Обновить полные данные рецептов в основную таблицу recipes
                    if (!recipeEntitiesToInsert.isEmpty()) {
                        Log.d(TAG, "[DB Sync] Вставка/Обновление " + recipeEntitiesToInsert.size()
                                + " записей в recipes.");
                        recipeDao.insertAll(recipeEntitiesToInsert); // Используем RecipeDao
                    }
                });
                Log.i(TAG, "[DB Sync] Транзакция обновления лайков для userId " + userId + " успешно завершена.");
            } catch (Exception e) {
                Log.e(TAG, "[DB Sync] Ошибка во время транзакции обновления лайков для userId: " + userId, e);
            }
        });
    }

    /**
     * Добавить лайкнутый рецепт в локальную базу.
     * Используется при действии пользователя "лайкнуть".
     */
    public void insertLikedRecipeLocal(int recipeId, String userId) {
        executor.execute(() -> {
            Log.d(TAG, "Добавление лайка в локальную базу: recipeId=" + recipeId + ", userId=" + userId);
            likedRecipeDao.insert(new LikedRecipeEntity(recipeId, userId));
        });
        // TODO: Добавить вызов API для синхронизации лайка с сервером
    }

    /**
     * Удалить лайкнутый рецепт из локальной базы.
     * Используется при действии пользователя "снять лайк" или при удалении рецепта.
     */
    public void deleteLikedRecipeLocal(int recipeId, String userId) {
        executor.execute(() -> {
            Log.d(TAG, "Удаление лайка из локальной базы: recipeId=" + recipeId + ", userId=" + userId);
            likedRecipeDao.deleteById(recipeId, userId);
        });
        // TODO: Добавить вызов API для синхронизации снятия лайка с сервером
    }

    /**
     * Проверить, лайкнут ли рецепт локально (синхронно).
     * ВНИМАНИЕ: Выполняет запрос к БД в вызывающем потоке. Не использовать в UI
     * потоке!
     * Лучше использовать LiveData<List<Recipe>> и проверять наличие в списке.
     * Оставляем для возможного использования в фоновых задачах.
     */
    public boolean isRecipeLikedLocalSync(int recipeId, String userId) {
        // Этот метод не рекомендуется использовать из UI потока.
        // Room потребует @AllowMainThreadQueries или вызов из фонового потока.
        // Лучше получать LiveData и проверять список.
        // Если все же нужен синхронный вызов, его надо делать в executor'е или через
        // suspend функцию в Kotlin.
        Log.w(TAG, "Синхронная проверка лайка isRecipeLikedLocalSync - не рекомендуется для UI потока!");
        // Возвращаем false или выбрасываем исключение, чтобы предотвратить неправильное
        // использование
        // return likedRecipeDao.isRecipeLiked(recipeId, userId); // Раскомментировать с
        // осторожностью
        return false; // Безопасное значение по умолчанию
    }

    /**
     * Обновить статус лайка (локально).
     * Сетевой вызов для синхронизации должен быть в отдельном методе или сервисе.
     */
    public void updateLikeStatusLocal(int recipeId, String userId, boolean isLiked) {
        executor.execute(() -> {
            if (isLiked) {
                Log.d(TAG,
                        "Обновление статуса лайка (локально): добавление recipeId=" + recipeId + ", userId=" + userId);
                likedRecipeDao.insert(new LikedRecipeEntity(recipeId, userId));
            } else {
                Log.d(TAG, "Обновление статуса лайка (локально): удаление recipeId=" + recipeId + ", userId=" + userId);
                likedRecipeDao.deleteById(recipeId, userId);
            }
        });
        // Сетевой вызов должен быть инициирован отдельно, например, из ViewModel
        // updateLikeStatusOnServer(recipeId, userId, isLiked);
    }

    // --- Удаленные методы SharedPreferences ---
    /*
     * public List<Recipe> loadLikedRecipesFromCache(Context context, String userId)
     * { ... }
     * private void saveLikedRecipesToCache(Context context, String userId,
     * List<Recipe> recipes) { ... }
     * public void syncLikedRecipesFromServer(String userId, final
     * LikedRecipesCallback callback, Context context) { ... } // Старый метод
     * синхронизации
     */

    /**
     * Проверяет доступность сети.
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = cm.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    /**
     * Получить список ID лайкнутых рецептов синхронно.
     * ВНИМАНИЕ: Выполняет запрос к БД в вызывающем потоке. Не использовать в UI
     * потоке!
     */
    public List<Integer> getLikedRecipeIdsSync(String userId) {
        try {
            // Проверка на UI поток не нужна, т.к. ViewModel вызывает это из фонового потока
            if (userId == null || userId.equals("0") || userId.isEmpty()) {
                Log.w(TAG, "getLikedRecipeIdsSync: Invalid userId=" + userId + ", returning empty list.");
                return new ArrayList<>();
            }
            return likedRecipeDao.getLikedRecipeIdsSync(userId);
        } catch (Exception e) {
            Log.e(TAG, "Error getting liked recipe IDs sync for userId=" + userId, e);
            return new ArrayList<>(); // Возвращаем пустой список в случае ошибки
        }
    }

    /**
     * Добавить рецепт в избранное и синхронизировать с сервером
     */
    public void addLikedRecipe(String userId, int recipeId) {
        if (userId == null || userId.equals("0") || userId.isEmpty()) {
            Log.w(TAG, "addLikedRecipe: Invalid userId=" + userId + ", operation skipped.");
            return;
        }
        
        // Добавляем в локальную базу
        insertLikedRecipeLocal(recipeId, userId);
        
        // Синхронизируем с сервером если есть сеть
        if (isNetworkAvailable()) {
            executor.execute(() -> {
                try {
                    // Здесь должен быть вызов API для синхронизации лайка с сервером
                    // Например: apiService.addLikedRecipe(userId, recipeId);
                    Log.d(TAG, "Рецепт " + recipeId + " добавлен в избранное для пользователя " + userId);
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при синхронизации добавления в избранное с сервером", e);
                }
            });
        }
    }

    /**
     * Удалить рецепт из избранного и синхронизировать с сервером
     */
    public void removeLikedRecipe(String userId, int recipeId) {
        if (userId == null || userId.equals("0") || userId.isEmpty()) {
            Log.w(TAG, "removeLikedRecipe: Invalid userId=" + userId + ", operation skipped.");
            return;
        }
        
        // Удаляем из локальной базы
        deleteLikedRecipeLocal(recipeId, userId);
        
        // Синхронизируем с сервером если есть сеть
        if (isNetworkAvailable()) {
            executor.execute(() -> {
                try {
                    // Здесь должен быть вызов API для синхронизации удаления лайка с сервером
                    // Например: apiService.removeLikedRecipe(userId, recipeId);
                    Log.d(TAG, "Рецепт " + recipeId + " удален из избранного для пользователя " + userId);
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при синхронизации удаления из избранного с сервером", e);
                }
            });
        }
    }

    /**
     * Проверяет, лайкнут ли рецепт пользователем (возвращает LiveData)
     */
    public LiveData<Boolean> isRecipeLiked(int recipeId, String userId) {
        Log.d(TAG, "isRecipeLiked запрос LiveData: recipeId=" + recipeId + ", userId=" + userId);
        MutableLiveData<Boolean> result = new MutableLiveData<>();
        
        if (userId.equals("0") || userId.isEmpty()) {
            result.setValue(false);
            return result;
        }
        
        // Выполняем запрос асинхронно
        executor.execute(() -> {
            boolean isLiked = isRecipeLikedLocalSync(recipeId, userId);
            Log.d(TAG, "isRecipeLiked результат: " + isLiked + " для recipeId=" + recipeId + ", userId=" + userId);
            result.postValue(isLiked);
        });
        
        return result;
    }
}
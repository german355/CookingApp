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
import com.example.cooking.network.models.GeneralServerResponse;
import com.example.cooking.network.responses.RecipesResponse;
import com.example.cooking.network.services.NetworkService;

// Импортируем RecipeLocalRepository
import com.example.cooking.data.repositories.RecipeLocalRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.HashMap;
import java.util.Map;

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
    public LiveData<List<Recipe>> getLikedRecipes() {
        // Получаем userId из SharedPreferences (для запроса к серверу)
        com.example.cooking.utils.MySharedPreferences prefs = new com.example.cooking.utils.MySharedPreferences(context);
        String userId = prefs.getString("userId", "0");
        
        // Запускаем фоновую синхронизацию лайков
        syncLikedRecipesFromServer(userId);

        // Получаем LiveData списка ID лайкнутых рецептов
        LiveData<List<LikedRecipeEntity>> likedEntitiesLiveData = likedRecipeDao.getLikedRecipes();

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
     * Запускает синхронизацию с сервером если есть сеть
     */
    public void syncLikedRecipesFromServer(final String userId) {
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
                        storeServerLikedRecipes(recipes);
                    } else {
                        Log.w(TAG,
                                "[fetchAndStore] Сервер вернул success=true, но null список лайкнутых рецептов для userId: "
                                        + userId);
                        // Очищаем локальные лайки, раз сервер говорит, что их нет
                        storeServerLikedRecipes(new ArrayList<>());
                    }
                } else {
                    // Сервер вернул success=false
                    Log.e(TAG, "[fetchAndStore] Ошибка при синхронизации лайкнутых рецептов (success=false): " +
                            "Code: " + response.code() + ", Message: " + recipesResponse.getMessage());
                }
            } else {
                // Неуспешный HTTP ответ
                Log.e(TAG, "[fetchAndStore] Ошибка при синхронизации лайкнутых рецептов (HTTP неудача): " +
                        "Code: " + response.code() + ", Message: " + response.message());
            }
        } catch (Exception e) {
            // Исключение при выполнении запроса или обработке ответа
            Log.e(TAG, "[fetchAndStore] Исключение при синхронизации лайкнутых рецептов для userId: " + userId, e);
        }
    }

    /**
     * Конвертирует и сохраняет лайкнутые рецепты, полученные с сервера.
     * Перезаписывает старые данные.
     */
    private void storeServerLikedRecipes(List<Recipe> serverRecipes) {
        executor.execute(() -> {
            List<LikedRecipeEntity> likedEntitiesToInsert = new ArrayList<>();
            List<RecipeEntity> recipeEntitiesToInsert = new ArrayList<>();

            for (Recipe recipe : serverRecipes) {
                if (recipe != null) { // Проверка на null
                    // Создаем сущность для таблицы лайков (только recipeId)
                    likedEntitiesToInsert.add(new LikedRecipeEntity(recipe.getId()));
                    // Создаем сущность для основной таблицы рецептов (для обновления/вставки)
                    recipeEntitiesToInsert.add(new RecipeEntity(recipe));
                }
            }

            // Выполняем операции в транзакции для атомарности
            try {
                Log.d(TAG, "[DB Sync] Запуск транзакции для обновления лайков");
                AppDatabase.getInstance(context).runInTransaction(() -> {
                    // 1. Очистить старые лайки
                    Log.d(TAG, "[DB Sync] Удаление старых записей из liked_recipes");
                    likedRecipeDao.deleteAll();
                    // 2. Вставить новые лайки, если они есть
                    if (!likedEntitiesToInsert.isEmpty()) {
                        Log.d(TAG, "[DB Sync] Вставка " + likedEntitiesToInsert.size()
                                + " новых записей в liked_recipes");
                        likedRecipeDao.insertAll(likedEntitiesToInsert);
                    }
                    // 3. Вставить/Обновить полные данные рецептов в основную таблицу recipes
                    if (!recipeEntitiesToInsert.isEmpty()) {
                        Log.d(TAG, "[DB Sync] Вставка/Обновление " + recipeEntitiesToInsert.size()
                                + " записей в recipes.");
                        recipeDao.insertAll(recipeEntitiesToInsert); // Используем RecipeDao
                    }
                });
                Log.i(TAG, "[DB Sync] Транзакция обновления лайков успешно завершена.");
            } catch (Exception e) {
                Log.e(TAG, "[DB Sync] Ошибка во время транзакции обновления лайков", e);
            }
        });
    }

    /**
     * Добавить лайкнутый рецепт в локальную базу.
     * Используется при действии пользователя "лайкнуть".
     */
    public void insertLikedRecipeLocal(int recipeId) {
        executor.execute(() -> {
            Log.d(TAG, "Добавление лайка в локальную базу: recipeId=" + recipeId);
            likedRecipeDao.insert(new LikedRecipeEntity(recipeId));
        });
    }

    /**
     * Удалить лайкнутый рецепт из локальной базы.
     * Используется при действии пользователя "снять лайк" или при удалении рецепта.
     */
    public void deleteLikedRecipeLocal(int recipeId) {
        executor.execute(() -> {
            Log.d(TAG, "Удаление лайка из локальной базы: recipeId=" + recipeId);
            likedRecipeDao.deleteById(recipeId);
        });
    }

    /**
     * Проверить, лайкнут ли рецепт локально (синхронно).
     * ВНИМАНИЕ: Выполняет запрос к БД в вызывающем потоке. Не использовать в UI
     * потоке!
     */
    public boolean isRecipeLikedLocalSync(int recipeId) {
        return likedRecipeDao.isRecipeLiked(recipeId);
    }

    /**
     * Обновляет статус лайка локально.
     * Если isLiked=true, добавляет лайк; если false, удаляет его.
     */
    public void updateLikeStatusLocal(int recipeId, boolean isLiked) {
        if (isLiked) {
            insertLikedRecipeLocal(recipeId);
        } else {
            deleteLikedRecipeLocal(recipeId);
        }
        
        // Обновляем статус лайка в таблице recipes
        executor.execute(() -> {
            recipeLocalRepository.updateLikeStatus(recipeId, isLiked);
        });
    }

    /**
     * Проверка наличия сети.
     */
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }

    /**
     * Получает синхронно список ID лайкнутых рецептов.
     */
    public List<Integer> getLikedRecipeIdsSync() {
        return likedRecipeDao.getLikedRecipeIdsSync();
    }
    
    /**
     * Отправляет запрос на сервер для добавления лайка и добавляет запись в локальную БД
     */
    public void addLikedRecipe(String userId, int recipeId) {
        executor.execute(() -> {
            // Добавляем запись в локальную БД напрямую
            Log.d(TAG, "Добавление лайка напрямую в БД: recipeId=" + recipeId);
            likedRecipeDao.insert(new LikedRecipeEntity(recipeId));
        });
        
        // Код для отправки запроса на сервер
        if (isNetworkAvailable()) {
            executor.execute(() -> {
                try {
                    Log.d(TAG, "Отправка запроса на лайк рецепта: userId=" + userId + ", recipeId=" + recipeId);
                    
                    // Создаем тело запроса с userId
                    Map<String, String> userData = new HashMap<>();
                    userData.put("userId", userId);
                    
                    // Отправляем запрос на сервер
                    retrofit2.Call<GeneralServerResponse> call = apiService.toggleLikeRecipe(recipeId, userData);
                    retrofit2.Response<GeneralServerResponse> response = call.execute();
                    
                    // Логируем результат
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Запрос на лайк успешно отправлен на сервер. Ответ: " + response.body());
                    } else {
                        Log.e(TAG, "Ошибка при отправке лайка на сервер. Код: " + response.code() + ", Сообщение: " + response.message());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при отправке лайка на сервер", e);
                }
            });
        } else {
            Log.w(TAG, "Нет подключения к сети, лайк сохранен только локально");
        }
    }
    
    /**
     * Отправляет запрос на сервер для удаления лайка и удаляет запись из локальной БД
     */
    public void removeLikedRecipe(String userId, int recipeId) {
        executor.execute(() -> {
            // Удаляем запись из локальной БД напрямую
            Log.d(TAG, "Удаление лайка напрямую из БД: recipeId=" + recipeId);
            likedRecipeDao.deleteById(recipeId);
        });
        
        // Код для отправки запроса на сервер
        if (isNetworkAvailable()) {
            executor.execute(() -> {
                try {
                    Log.d(TAG, "Отправка запроса на снятие лайка: userId=" + userId + ", recipeId=" + recipeId);
                    
                    // API использует тот же эндпоинт для лайка и снятия лайка
                    // поэтому отправляем тот же запрос - сервер должен определить действие по наличию/отсутствию записи
                    Map<String, String> userData = new HashMap<>();
                    userData.put("userId", userId);
                    
                    // Отправляем запрос на сервер 
                    retrofit2.Call<GeneralServerResponse> call = apiService.toggleLikeRecipe(recipeId, userData);
                    retrofit2.Response<GeneralServerResponse> response = call.execute();
                    
                    // Логируем результат
                    if (response.isSuccessful()) {
                        Log.d(TAG, "Запрос на снятие лайка успешно отправлен на сервер. Ответ: " + response.body());
                    } else {
                        Log.e(TAG, "Ошибка при отправке запроса на снятие лайка. Код: " + response.code() + ", Сообщение: " + response.message());
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при отправке запроса на снятие лайка", e);
                }
            });
        } else {
            Log.w(TAG, "Нет подключения к сети, лайк удален только локально");
        }
    }
    
    /**
     * Очищает таблицу лайков при выходе пользователя
     */
    public void clearLikedRecipes() {
        executor.execute(() -> {
            likedRecipeDao.deleteAll();
        });
    }
    
    /**
     * Запускает синхронизацию лайкнутых рецептов с сервера, если это необходимо.
     * Используется для инициализации данных при логине пользователя.
     */
    public void syncLikedRecipesFromServerIfNeeded() {
        // Получаем userId из SharedPreferences
        com.example.cooking.utils.MySharedPreferences prefs = new com.example.cooking.utils.MySharedPreferences(context);
        String userId = prefs.getString("userId", "0");
        
        // Запускаем синхронизацию, если пользователь авторизован
        if (!userId.equals("0")) {
            syncLikedRecipesFromServer(userId);
        } else {
            Log.d(TAG, "Пропуск синхронизации: пользователь не авторизован");
        }
    }
}
package com.example.cooking.data.repositories;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Transformations;
import androidx.lifecycle.MediatorLiveData;

import com.example.cooking.Recipe.Recipe;
import com.example.cooking.config.ServerConfig;
import com.example.cooking.data.database.AppDatabase;
import com.example.cooking.data.database.LikedRecipeDao;
import com.example.cooking.data.database.LikedRecipeEntity;
import com.example.cooking.data.database.RecipeDao;
import com.example.cooking.network.responses.LikedRecipesResponse;

import java.util.ArrayList;
import java.util.List;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

public class LikedRecipesRepository extends NetworkRepository {
    private static final String TAG = "LikedRecipesRepository_DEBUG"; // Используем другой тег для отладочных логов
    private static final String API_URL = ServerConfig.BASE_API_URL;
    private final LikedRecipeDao likedRecipeDao;
    private final RecipeDao recipeDao;
    private final RecipeLocalRepository recipeLocalRepository; // Добавляем зависимость
    private final CompositeDisposable disposables = new CompositeDisposable();

    public interface LikedRecipesCallback {
        void onRecipesLoaded(List<Recipe> recipes);

        void onDataNotAvailable(String error);
    }

    public LikedRecipesRepository(Context context) {
        super(context);
        Log.d(TAG, "Constructor: START");
        AppDatabase db = AppDatabase.getInstance(this.context);
        likedRecipeDao = db.likedRecipeDao();
        recipeDao = db.recipeDao();
        recipeLocalRepository = new RecipeLocalRepository(this.context);
        Log.d(TAG, "Constructor: END");
    }

    /**
     * Получить LiveData ПОЛНЫХ лайкнутых рецептов из локальной базы данных (Room).
     * Загружает ID лайков, затем по этим ID получает полные данные из
     * RecipeLocalRepository.
     * Это основной метод для получения данных в UI.
     */
    public LiveData<List<Recipe>> getLikedRecipes() {
        Log.d(TAG, "getLikedRecipes: START"); // <--- Новый лог
        // Запускаем фоновую синхронизацию лайков
        syncLikedRecipesFromServer();
        Log.d(TAG, "getLikedRecipes: Called syncLikedRecipesFromServer"); // <--- Новый лог

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
    public void syncLikedRecipesFromServer() {
        Log.d(TAG, "syncLikedRecipesFromServer: START"); // Новый лог
        if (!isNetworkAvailable()) {
            Log.d(TAG, "syncLikedRecipesFromServer: Network is NOT available. Skipping sync."); // Измененный лог
            return;
        }
        Log.d(TAG, "syncLikedRecipesFromServer: Network IS available. Proceeding with sync."); // Новый лог
        Disposable d = Completable.fromAction(this::fetchAndStoreLikedRecipes)
                .subscribeOn(Schedulers.io())
                .subscribe(
                        () -> {},
                        throwable -> Log.e(TAG, "Ошибка syncLikedRecipesFromServer", throwable)
                );
        disposables.add(d);
    }

    /**
     * Выполняет сетевой запрос и сохраняет результат в БД.
     */
    private void fetchAndStoreLikedRecipes() {
        Log.d(TAG, "fetchAndStoreLikedRecipes: START"); // <--- Новый лог (вместо старого [fetchAndStore]...)
        // Используем общий ApiService
        retrofit2.Call<LikedRecipesResponse> call = apiService.getLikedRecipes();
        try {
            // Используем execute() для синхронного выполнения в фоновом потоке Executor'a
            retrofit2.Response<LikedRecipesResponse> response = call.execute();
            Log.d(TAG, "[fetchAndStore] Получен ответ от сервера: Code=" + response.code() + ", isSuccessful="
                    + response.isSuccessful());

            if (response.isSuccessful() && response.body() != null) {
                LikedRecipesResponse likedResponse = response.body();
                if (likedResponse.isSuccess()) {
                    List<Integer> recipeIds = likedResponse.getRecipeIds();
                    storeServerLikedRecipes(recipeIds);
                } else {
                    // Сервер вернул success=false
                    Log.e(TAG, "[fetchAndStore] Ошибка при синхронизации лайкнутых рецептов (success=false): " +
                            "Code: " + response.code() + ", Message: " + likedResponse.getMessage());
                }
            } else {
                // Неуспешный HTTP ответ
                Log.e(TAG, "[fetchAndStore] Ошибка при синхронизации лайкнутых рецептов (HTTP неудача): " +
                        "Code: " + response.code() + ", Message: " + response.message());
            }
        } catch (Exception e) {
            // Исключение при выполнении запроса или обработке ответа
            Log.e(TAG, "[fetchAndStore] Исключение при синхронизации лайкнутых рецептов", e);
        }
    }

    /**
     * Конвертирует и сохраняет лайкнутые рецепты, полученные с сервера.
     * Перезаписывает старые данные.
     */
    private void storeServerLikedRecipes(List<Integer> serverRecipeIds) {
        Completable.fromAction(() -> {
            List<LikedRecipeEntity> likedEntitiesToInsert = new ArrayList<>();
            // формируем список из ID
            for (Integer id : serverRecipeIds) {
                likedEntitiesToInsert.add(new LikedRecipeEntity(id));
            }

            // Выполняем операции в транзакции для атомарности
            try {
                Log.d(TAG, "[DB Sync] Запуск транзакции для обновления лайков");
                AppDatabase.getInstance(context).runInTransaction(() -> {
                    // очистить все
                    likedRecipeDao.deleteAll();
                    recipeDao.clearAllLikeStatus();
                    // вставить новые лайки и обновить флаг
                    if (!likedEntitiesToInsert.isEmpty()) {
                        likedRecipeDao.insertAll(likedEntitiesToInsert);
                        for (LikedRecipeEntity e : likedEntitiesToInsert) {
                            recipeDao.updateLikeStatus(e.getRecipeId(), true);
                        }
                    }
                });
                Log.i(TAG, "[DB Sync] Транзакция обновления лайков успешно завершена.");
            } catch (Exception e) {
                Log.e(TAG, "[DB Sync] Ошибка во время транзакции обновления лайков", e);    
            }
        })
        .subscribeOn(Schedulers.io())
        .subscribe(
                () -> {},
                throwable -> Log.e(TAG, "Ошибка storeServerLikedRecipes", throwable)
        );
    }

    /**
     * Добавить лайкнутый рецепт в локальную базу.
     * Используется при действии пользователя "лайкнуть".
     */
    public void insertLikedRecipeLocal(int recipeId) {
        Completable.fromAction(() -> likedRecipeDao.insert(new LikedRecipeEntity(recipeId)))
                .subscribeOn(Schedulers.io())
                .subscribe(
                        () -> {},
                        throwable -> Log.e(TAG, "Ошибка insertLikedRecipeLocal", throwable)
                );
    }

    /**
     * Удалить лайкнутый рецепт из локальной базы.
     * Используется при действии пользователя "снять лайк" или при удалении рецепта.
     */
    public void deleteLikedRecipeLocal(int recipeId) {
        Completable.fromAction(() -> likedRecipeDao.deleteById(recipeId))
                .subscribeOn(Schedulers.io())
                .subscribe(
                        () -> {},
                        throwable -> Log.e(TAG, "Ошибка deleteLikedRecipeLocal", throwable)
                );
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
        Completable.fromAction(() -> {
            if (isLiked) likedRecipeDao.insert(new LikedRecipeEntity(recipeId));
            else likedRecipeDao.deleteById(recipeId);
        }).subscribeOn(Schedulers.io()).subscribe(
                () -> {},
                throwable -> Log.e(TAG, "Ошибка добавления лайка локально", throwable)
        );
        
        // Обновляем статус лайка в таблице recipes
        Completable.fromAction(() -> {
            recipeLocalRepository.updateLikeStatus(recipeId, isLiked);
        }).subscribeOn(Schedulers.io()).subscribe();
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
    public void addLikedRecipe(int recipeId) {
        Log.d(TAG, "addLikedRecipe(): выполняем только локальное обновление");
        Disposable d1 = Completable.fromAction(() -> {
            Log.d(TAG, "Добавление лайка в локальную БД: recipeId=" + recipeId);
            likedRecipeDao.insert(new LikedRecipeEntity(recipeId));
        }).subscribeOn(Schedulers.io()).subscribe(
                () -> Log.d(TAG, "Лайк добавлен локально: recipeId=" + recipeId),
                throwable -> Log.e(TAG, "Ошибка добавления лайка локально", throwable)
        );
        disposables.add(d1);
    }
    
    /**
     * Отправляет запрос на сервер для удаления лайка и удаляет запись из локальной БД
     */
    public void removeLikedRecipe(int recipeId) {
        Log.d(TAG, "removeLikedRecipe(): выполняем только локальное обновление");
        Disposable d2 = Completable.fromAction(() -> {
            Log.d(TAG, "Удаление лайка в локальной БД: recipeId=" + recipeId);
            likedRecipeDao.deleteById(recipeId);
        }).subscribeOn(Schedulers.io()).subscribe(
                () -> Log.d(TAG, "Лайк удалён локально: recipeId=" + recipeId),
                throwable -> Log.e(TAG, "Ошибка удаления лайка локально", throwable)
        );
        disposables.add(d2);
    }

    /**
     * Очищает все подписки RxJava
     */
    public void clearDisposables() {
        disposables.clear();
    }
}
package com.example.cooking.data.repositories;

import android.content.Context;
import android.util.Log;

import com.example.cooking.domain.entities.Recipe;
import com.example.cooking.config.ServerConfig;
import com.example.cooking.data.database.AppDatabase;
import com.example.cooking.data.database.LikedRecipeDao;
import com.example.cooking.data.database.LikedRecipeEntity;
import com.example.cooking.data.database.RecipeDao;
import com.example.cooking.network.models.recipeResponses.LikedRecipesResponse;

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
     * Запускает синхронизацию с сервером если есть сеть
     */
    public void syncLikedRecipesFromServer() {
        Log.d(TAG, "syncLikedRecipesFromServer: START");
        if (!isNetworkAvailable()) {
            Log.d(TAG, "syncLikedRecipesFromServer: Network is NOT available. Skipping sync.");
            return;
        }
        Log.d(TAG, "syncLikedRecipesFromServer: Network IS available. Proceeding with sync.");

        disposables.add(
            apiService.getLikedRecipes()
                .subscribeOn(Schedulers.io())
                .doOnSuccess(response -> {
                    if (response.isSuccess() && response.getRecipeIds() != null) {
                        Log.d(TAG, "[DB Sync] Получен успешный ответ с " + response.getRecipeIds().size() + " ID лайков.");
                        storeServerLikedRecipes(response.getRecipeIds());
                    } else {
                        String errorMessage = response.getMessage() != null ? response.getMessage() : "Ответ не содержит данных.";
                        Log.e(TAG, "[DB Sync] Ошибка при синхронизации лайков (success=false): " + errorMessage);
                    }
                })
                .doOnError(throwable -> {
                     Log.e(TAG, "[DB Sync] Исключение при выполнении запроса синхронизации лайков", throwable);
                })
                .subscribe(
                    response -> Log.i(TAG, "[DB Sync] Синхронизация лайков успешно завершена."),
                    throwable -> Log.e(TAG, "Критическая ошибка в цепочке syncLikedRecipesFromServer", throwable)
                )
        );
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
     * Отправляет запрос на сервер для переключения статуса лайка (без ожидания ответа)
     * @param recipeId ID рецепта
     * @return Completable для подписки
     */
    public Completable toggleLikeRecipeOnServer(int recipeId) {
        return apiService.toggleLikeRecipeCompletable(recipeId);
    }

    /**
     * Очищает все подписки RxJava
     */
    public void clearDisposables() {
        disposables.clear();
    }
    
    /**
     * Полностью очищает все лайки пользователя.
     * Используется при выходе из аккаунта.
     */
    public void clearAllLikes() {
        Completable.fromAction(() -> {
            // Очищаем таблицу лайков
            likedRecipeDao.deleteAll();
            // Сбрасываем флаги isLiked у всех рецептов
            recipeDao.clearAllLikeStatus();
            Log.d(TAG, "Все лайки успешно очищены");
        })
        .subscribeOn(Schedulers.io())
        .subscribe(
            () -> {},
            throwable -> Log.e(TAG, "Ошибка при очистке всех лайков", throwable)
        );
    }
}
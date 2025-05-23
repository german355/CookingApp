package com.example.cooking.data.repositories;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import android.os.Handler;
import android.os.Looper;

import com.example.cooking.Recipe.Ingredient;
import com.example.cooking.Recipe.Recipe;
import com.example.cooking.Recipe.Step;
import com.example.cooking.config.ServerConfig;
import com.example.cooking.network.api.ApiService;
import com.example.cooking.network.models.GeneralServerResponse;
import com.example.cooking.network.services.NetworkService;
import com.example.cooking.network.utils.Resource;
import com.example.cooking.utils.MySharedPreferences;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.google.gson.Gson;

public class UnifiedRecipeRepository {
    private static final String TAG = "UnifiedRecipeRepository";

    private final RecipeLocalRepository localRepository;
    private final RecipeRemoteRepository remoteRepository;
    private final LikedRecipesRepository likedRecipesRepository;
    private final Application application;
    private final ExecutorService executor;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    private final ApiService apiService;
    private final Context context;
    private static final Gson gson = new Gson();
    private final ConnectivityManager connectivityManager;

    // Объект Recipe, который был отправлен на обновление, чтобы использовать его для обновления в локальной БД
    private Recipe recipeBeingUpdated;

    /**
     * Интерфейс для обратного вызова результата добавления/редактирования рецепта
     */
    public interface RecipeSaveCallback {
        void onSuccess(GeneralServerResponse response, Recipe updatedRecipe);

        void onFailure(String error, GeneralServerResponse errorResponse);
    }

    /**
     * Интерфейс для обратного вызова результата удаления рецепта
     */
    public interface DeleteRecipeCallback {
        void onDeleteSuccess();

        void onDeleteFailure(String error);
    }

    public UnifiedRecipeRepository(Application application, ExecutorService executor) {
        this.application = application;
        this.context = application.getApplicationContext();
        this.localRepository = new RecipeLocalRepository(application);
        this.remoteRepository = new RecipeRemoteRepository(application);
        this.likedRecipesRepository = new LikedRecipesRepository(application);
        this.executor = executor;
        this.apiService = NetworkService.getApiService(application);
        this.connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    public LiveData<List<Recipe>> getAllRecipesLocal() {
        return localRepository.getAllRecipes();
    }

    // Этот метод должен вызываться из фонового потока
    public List<Recipe> getAllRecipesSync() {
        return localRepository.getAllRecipesSync();
    }

    /**
     * Синхронизирует локальные данные с данными с сервера в фоновом потоке.
     * Оповещает об ошибке через errorMessage в основном потоке.
     * Обновляет recipesLiveData в основном потоке, если он не null.
     * Если remoteRecipes равен null и сеть недоступна, загружает данные из локального хранилища.
     */
    public void syncWithRemoteData(List<Recipe> remoteRecipes, MutableLiveData<String> errorMessage, MutableLiveData<Resource<List<Recipe>>> recipesLiveData) {
        executor.execute(() -> {
            try {
                if (remoteRecipes != null) {
                    String currentUserId = new MySharedPreferences(application).getString("userId", "0");
                    Set<Integer> likedRecipeIds = new HashSet<>();
                    if (!currentUserId.equals("0")) {
                        List<Integer> likedIdsList = likedRecipesRepository.getLikedRecipeIdsSync(); // Вызов к БД
                        if (likedIdsList != null) {
                            likedRecipeIds.addAll(likedIdsList);
                        }
                        Log.d(TAG, "(BG Thread) Загружены ID лайкнутых рецептов: " + likedRecipeIds.size());
                    }

                    List<Recipe> localRecipes = localRepository.getAllRecipesSync(); // Вызов к БД
                    Set<Integer> remoteRecipeIds = new HashSet<>();

                    for (Recipe remoteRecipe : remoteRecipes) {
                        remoteRecipe.setLiked(likedRecipeIds.contains(remoteRecipe.getId()));
                        remoteRecipeIds.add(remoteRecipe.getId());
                    }

                    Set<Integer> deletedRecipeIds = new HashSet<>();
                    if (localRecipes != null) {
                        for (Recipe localRecipe : localRecipes) {
                            if (!remoteRecipeIds.contains(localRecipe.getId())) {
                                deletedRecipeIds.add(localRecipe.getId());
                            }
                        }
                    }

                    if (!deletedRecipeIds.isEmpty()) {
                        for (Integer deletedId : deletedRecipeIds) {
                            localRepository.deleteRecipe(deletedId); // Вызов к БД
                        }
                        Log.d(TAG, "(BG Thread) Удалено " + deletedRecipeIds.size() + " рецептов");
                    }

                    localRepository.insertAll(remoteRecipes); // Вызов к БД
                    Log.d(TAG, "(BG Thread) Сохранено в локальное хранилище: " + remoteRecipes.size() + " рецептов");
                    // Обновляем LiveData в основном потоке
                    if (recipesLiveData != null) {
                        mainThreadHandler.post(() -> recipesLiveData.setValue(Resource.success(remoteRecipes)));
                    }
                } else {
                    // Если нет данных с сервера, проверяем доступность сети
                    if (!isNetworkAvailable()) {
                        // Загружаем данные из локального хранилища в офлайн режиме
                        List<Recipe> localRecipes = localRepository.getAllRecipesSync();
                        if (localRecipes != null && !localRecipes.isEmpty()) {
                            Log.d(TAG, "(BG Thread) Загружено " + localRecipes.size() + " рецептов из локального хранилища (офлайн режим)");
                            if (recipesLiveData != null) {
                                mainThreadHandler.post(() -> recipesLiveData.setValue(Resource.success(localRecipes)));
                            }
                            if (errorMessage != null) {
                                mainThreadHandler.post(() -> errorMessage.setValue("Работа в офлайн режиме. Отображаются сохраненные рецепты."));
                            }
                        } else {
                            if (recipesLiveData != null) {
                                mainThreadHandler.post(() -> recipesLiveData.setValue(Resource.error("Нет сохраненных рецептов для отображения в офлайн режиме", null)));
                            }
                            if (errorMessage != null) {
                                mainThreadHandler.post(() -> errorMessage.setValue("Нет сохраненных рецептов для отображения в офлайн режиме"));
                            }
                        }
                    } else {
                        // Ошибка при работе онлайн
                        if (recipesLiveData != null) {
                            mainThreadHandler.post(() -> recipesLiveData.setValue(Resource.error("Получен null список рецептов с сервера", null)));
                        }
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "(BG Thread) Ошибка при синхронизации: " + e.getMessage(), e);
                if (errorMessage != null) {
                    // Обновляем LiveData об ошибке в основном потоке
                    mainThreadHandler.post(() -> errorMessage.setValue("Ошибка синхронизации: " + e.getMessage()));
                }
                // Также обновляем recipesLiveData с ошибкой
                if (recipesLiveData != null) {
                    mainThreadHandler.post(() -> recipesLiveData.setValue(Resource.error("Ошибка синхронизации: " + e.getMessage(), null)));
                }
            }
        });
    }

    /**
     * Проверяет доступность сети
     *
     * @return true, если сеть доступна, иначе false
     */
    public boolean isNetworkAvailable() {
        NetworkInfo activeNetworkInfo = connectivityManager != null ? connectivityManager.getActiveNetworkInfo() : null;
        boolean networkAvailable = activeNetworkInfo != null && activeNetworkInfo.isConnected();

        if (!networkAvailable) {
            Log.d(TAG, "Сеть недоступна. Приложение работает в офлайн режиме.");
        }

        return networkAvailable;
    }

    /**
     * Загружает рецепты с сервера, если сеть доступна.
     * В противном случае, уведомляет о работе в офлайн режиме.
     *
     * @param callback обратный вызов для получения результата
     */
    public void loadRemoteRecipes(RecipeRemoteRepository.RecipesCallback callback) {
        if (isNetworkAvailable()) {
            remoteRepository.getRecipes(callback);
        } else {
            // Если сеть недоступна, возвращаем ошибку но с кодом для офлайн режима
            callback.onDataNotAvailable("Работа в офлайн режиме. Отображаются сохраненные рецепты.");
        }
    }

    public void updateLikeStatus(int recipeId, boolean isLiked) {
        executor.execute(() -> localRepository.updateLikeStatus(recipeId, isLiked));
    }

    public void toggleLike(String userId, int recipeId) {
        executor.execute(() -> {
            // Проверяем наличие сети перед установкой лайка
            if (!isNetworkAvailable()) {
                Log.d(TAG, "Офлайн режим: установка лайков невозможна для recipeId=" + recipeId);
                // Уведомление для UI о невозможности поставить лайк в офлайн режиме будет
                // обрабатываться на уровне ViewModel или Fragment
                return;
            }
            
            boolean isCurrentlyLiked = likedRecipesRepository.isRecipeLikedLocalSync(recipeId);
            boolean newLikeStatus = !isCurrentlyLiked;
            
            // Обновляем локальную базу данных только если есть сеть
            localRepository.updateLikeStatus(recipeId, newLikeStatus);
            
            if (newLikeStatus) {
                likedRecipesRepository.addLikedRecipe(userId, recipeId);
            } else {
                likedRecipesRepository.removeLikedRecipe(userId, recipeId);
            }
        });
    }

    public void setLikeStatus(String userId, int recipeId, boolean newLikeStatus) {
        executor.execute(() -> {
            // Проверяем наличие сети перед установкой лайка
            if (!isNetworkAvailable()) {
                Log.d(TAG, "Офлайн режим: установка статуса лайка невозможна для recipeId=" + recipeId);
                // В офлайн режиме не разрешаем изменять статус лайка
                return;
            }
            
            // Обновляем данные только если есть сеть
            localRepository.updateLikeStatus(recipeId, newLikeStatus);
            if (newLikeStatus) {
                likedRecipesRepository.addLikedRecipe(userId, recipeId);
            } else {
                likedRecipesRepository.removeLikedRecipe(userId, recipeId);
            }

            // TODO: Вызвать API для обновления статуса лайка на сервере
        });
    }

    /**
     * Выполняет поиск в локальных данных в фоновом потоке.
     * Результат возвращается через LiveData в основном потоке.
     */
    /**
     * Выполняет поиск в локальных данных в фоновом потоке.
     * Результат возвращается через LiveData в основном потоке.
     * В офлайн режиме добавляет соответствующее уведомление.
     */
    public void searchInLocalData(String query, MutableLiveData<List<Recipe>> searchResultsLiveData, MutableLiveData<String> errorMessageLiveData) {
        executor.execute(() -> {
            try {
                List<Recipe> allRecipes = getAllRecipesSync(); // Этот метод теперь вызывается в executor
                if (allRecipes == null) {
                    mainThreadHandler.post(() -> searchResultsLiveData.setValue(Collections.emptyList()));
                    return;
                }
                String lowerQuery = query.toLowerCase();
                List<Recipe> filteredResults = allRecipes.stream()
                        .filter(recipe -> matchesSearchQuery(recipe, lowerQuery))
                        .collect(Collectors.toList());
                mainThreadHandler.post(() -> {
                    searchResultsLiveData.setValue(filteredResults);

                    // Если приложение в офлайн режиме, добавляем уведомление
                    if (!isNetworkAvailable() && errorMessageLiveData != null) {
                        errorMessageLiveData.setValue("Поиск выполнен в офлайн режиме. Результаты могут быть неполными.");
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "(BG Thread) Ошибка при локальном поиске: " + e.getMessage(), e);
                if (errorMessageLiveData != null) {
                    mainThreadHandler.post(() -> errorMessageLiveData.setValue("Ошибка локального поиска: " + e.getMessage()));
                }
                mainThreadHandler.post(() -> searchResultsLiveData.setValue(Collections.emptyList()));
            }
        });
    }

    private boolean matchesSearchQuery(Recipe recipe, String query) {
        if (recipe.getTitle() != null && recipe.getTitle().toLowerCase().contains(query)) {
            return true;
        }
        if (recipe.getIngredients() != null) {
            for (com.example.cooking.Recipe.Ingredient ingredient : recipe.getIngredients()) {
                if (ingredient.getName() != null && ingredient.getName().toLowerCase().contains(query)) {
                    return true;
                }
            }
        }
        if (recipe.getSteps() != null) {
            for (com.example.cooking.Recipe.Step step : recipe.getSteps()) {
                if (step.getInstruction() != null && step.getInstruction().toLowerCase().contains(query)) {
                    return true;
                }
            }
        }
        return false;
    }

    // Этот метод должен вызываться из фонового потока
    public Set<Integer> getLikedRecipeIds() {
        Set<Integer> likedRecipeIds = new HashSet<>();
        List<Integer> likedIdsList = likedRecipesRepository.getLikedRecipeIdsSync(); // Вызов к БД
        if (likedIdsList != null) {
            likedRecipeIds.addAll(likedIdsList);
        }
        return likedRecipeIds;
    }

    /**
     * Сохраняет новый рецепт
     */
    public void saveRecipe(Recipe recipe, byte[] imageBytes, RecipeSaveCallback callback) {
        // Проверяем доступность сети перед сохранением рецепта
        if (!isNetworkAvailable()) {
            if (callback != null) {
                callback.onFailure("Невозможно сохранить рецепт в офлайн режиме", null);
            }
            return;
        }

        // Создаем RequestBody для каждого поля
        RequestBody title = RequestBody.create(MediaType.parse("text/plain"), recipe.getTitle());
        RequestBody ingredients = RequestBody.create(MediaType.parse("text/plain"), new Gson().toJson(recipe.getIngredients()));
        RequestBody instructions = RequestBody.create(MediaType.parse("text/plain"), new Gson().toJson(recipe.getSteps()));
        RequestBody userId = RequestBody.create(MediaType.parse("text/plain"), recipe.getUserId());

        // Создаем часть для загрузки изображения, если оно есть
        MultipartBody.Part imagePart = null;
        if (imageBytes != null && imageBytes.length > 0) {
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), imageBytes);
            imagePart = MultipartBody.Part.createFormData("photo", "recipe_image.jpg", requestFile);
        }


        // Вызываем соответствующий метод API в зависимости от наличия изображения
        Call<GeneralServerResponse> call;
        if (imagePart != null) {
            call = apiService.addRecipe(title, ingredients, instructions, userId, imagePart);
        } else {
            call = apiService.addRecipeWithoutPhoto(title, ingredients, instructions, userId);
        }

        call.enqueue(new Callback<GeneralServerResponse>() {
            @Override
            public void onResponse(Call<GeneralServerResponse> call, Response<GeneralServerResponse> response) {
                if (response.isSuccessful() && response.body() != null) {

                    Recipe savedRecipe = new Recipe();

                    // Безопасное получение ID с проверкой на null
                    Integer responseId = response.body() != null && response.body().getId() != null ? response.body().getId() : 0;
                    savedRecipe.setId(responseId);
                    savedRecipe.setTitle(recipe.getTitle());
                    savedRecipe.setIngredients(recipe.getIngredients());
                    savedRecipe.setSteps(recipe.getSteps());
                    savedRecipe.setUserId(recipe.getUserId());

                    // Сохраняем рецепт локально
                    Completable.fromAction(() -> localRepository.insert(savedRecipe))
                            .subscribeOn(Schedulers.io())
                            .subscribe();

                    if (callback != null) {
                        callback.onSuccess(response.body(), savedRecipe);
                    }
                } else {
                    String error = "Ошибка при сохранении рецепта";
                    if (response.errorBody() != null) {
                        try {
                            error = response.errorBody().string();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (callback != null) {
                        callback.onFailure(error, response.body());
                    }
                }
            }

            @Override
            public void onFailure(Call<GeneralServerResponse> call, Throwable t) {
                if (callback != null) {
                    callback.onFailure(t.getMessage(), null);
                }
            }
        });
    }

    /**
     * Обновляет существующий рецепт
     */
    public void updateRecipe(Recipe recipe, byte[] imageBytes, RecipeSaveCallback callback) {
        if (recipe == null) {
            if (callback != null) {
                callback.onFailure("Рецепт не может быть null", null);
            }
            return;
        }

        // Создаем RequestBody для каждого поля
        RequestBody title = RequestBody.create(MediaType.parse("text/plain"), recipe.getTitle());
        RequestBody ingredients = RequestBody.create(MediaType.parse("text/plain"), new Gson().toJson(recipe.getIngredients()));
        RequestBody instructions = RequestBody.create(MediaType.parse("text/plain"), new Gson().toJson(recipe.getSteps()));

        // Получаем ID текущего пользователя (заглушка, нужно заменить на реальный ID)
        String currentUserId = "current_user_id";

        // Создаем часть для загрузки изображения, если оно есть
        MultipartBody.Part imagePart = null;
        if (imageBytes != null && imageBytes.length > 0) {
            RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), imageBytes);
            imagePart = MultipartBody.Part.createFormData("photo", "recipe_image.jpg", requestFile);
        }

        // Вызываем метод API для обновления рецепта
        Call<GeneralServerResponse> call = apiService.updateRecipe(
                recipe.getId(),
                currentUserId, // X-User-ID заголовок
                "edit",        // X-User-Permission заголовок (заглушка)
                title,
                ingredients,
                instructions,
                imagePart
        );

        call.enqueue(new Callback<GeneralServerResponse>() {
            @Override
            public void onResponse(Call<GeneralServerResponse> call, Response<GeneralServerResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Создаем обновленный рецепт на основе переданных данных
                    Recipe updatedRecipe = new Recipe();
                    updatedRecipe.setId(recipe.getId());
                    updatedRecipe.setTitle(recipe.getTitle());
                    updatedRecipe.setIngredients(recipe.getIngredients());
                    updatedRecipe.setSteps(recipe.getSteps());
                    updatedRecipe.setUserId(recipe.getUserId());

                    // Обновляем рецепт локально
                    Completable.fromAction(() -> localRepository.update(updatedRecipe))
                            .subscribeOn(Schedulers.io())
                            .subscribe();

                    if (callback != null) {
                        callback.onSuccess(response.body(), updatedRecipe);
                    }
                } else {
                    String error = "Ошибка при обновлении рецепта";
                    if (response.errorBody() != null) {
                        try {
                            error = response.errorBody().string();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    if (callback != null) {
                        callback.onFailure(error, response.body());
                    }
                }
            }

            @Override
            public void onFailure(Call<GeneralServerResponse> call, Throwable t) {
                if (callback != null) {
                    callback.onFailure(t.getMessage(), null);
                }
            }
        });
    }

    /**
     * Удаляет рецепт
     */
    /**
     * Вставляет рецепт в локальное хранилище
     *
     * @param recipe   рецепт для вставки
     * @param callback колбек для обработки результата
     */
    public void insert(Recipe recipe, RecipeSaveCallback callback) {
        if (recipe == null) {
            if (callback != null) {
                callback.onFailure("Рецепт не может быть null", null);
            }
            return;
        }

        // Вставляем рецепт в локальное хранилище
        Completable.fromAction(() -> localRepository.insert(recipe))
                .subscribeOn(Schedulers.io())
                .subscribe(
                        () -> {
                            if (callback != null) {
                                GeneralServerResponse response = new GeneralServerResponse();
                                response.setSuccess(true);
                                response.setMessage("Рецепт успешно сохранен");
                                response.setId(recipe.getId());
                                callback.onSuccess(response, recipe);
                            }
                        },
                        throwable -> {
                            if (callback != null) {
                                callback.onFailure("Ошибка при сохранении рецепта: " + throwable.getMessage(), null);
                            }
                        }
                );
    }

    /**
     * Обновляет рецепт в локальном хранилище
     *
     * @param recipe   обновленный рецепт
     * @param callback колбек для обработки результата
     */
    public void update(Recipe recipe, RecipeSaveCallback callback) {
        if (recipe == null) {
            if (callback != null) {
                callback.onFailure("Рецепт не может быть null", null);
            }
            return;
        }

        // Обновляем рецепт в локальном хранилище
        Completable.fromAction(() -> localRepository.update(recipe))
                .subscribeOn(Schedulers.io())
                .subscribe(
                        () -> {
                            if (callback != null) {
                                GeneralServerResponse response = new GeneralServerResponse();
                                response.setSuccess(true);
                                
                                // Добавляем информацию о режиме работы в сообщение
                                if (!isNetworkAvailable()) {
                                    response.setMessage("Рецепт успешно обновлен локально (офлайн режим)");
                                    Log.d(TAG, "Рецепт с ID " + recipe.getId() + " обновлен только локально в офлайн режиме");
                                } else {
                                    response.setMessage("Рецепт успешно обновлен");
                                }
                                
                                response.setId(recipe.getId());
                                callback.onSuccess(response, recipe);
                            }
                        },
                        throwable -> {
                            if (callback != null) {
                                callback.onFailure("Ошибка при обновлении рецепта: " + throwable.getMessage(), null);
                            }
                        }
                );
    }

    public void deleteRecipe(int recipeId, DeleteRecipeCallback callback) {
        // Проверяем доступность сети
        if (!isNetworkAvailable()) {
            if (callback != null) {
                callback.onDeleteFailure("Невозможно удалить рецепт в офлайн режиме");
            }
            return;
        }
        
        // Получаем ID текущего пользователя (заглушка, нужно заменить на реальный ID)
        String currentUserId = "current_user_id"; // TODO: Заменить на реальный ID пользователя

        // Удаляем рецепт с сервера
        Call<GeneralServerResponse> call = apiService.deleteRecipe(
                recipeId,
                currentUserId, // X-User-ID заголовок
                "delete"      // X-User-Permission заголовок (заглушка)
        );

        call.enqueue(new Callback<GeneralServerResponse>() {
            @Override
            public void onResponse(Call<GeneralServerResponse> call, Response<GeneralServerResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Удаляем рецепт из локальной базы данных
                    Completable.fromAction(() -> localRepository.deleteRecipe(recipeId))
                            .subscribeOn(Schedulers.io())
                            .subscribe();
                    callback.onDeleteSuccess();
                } else {
                    String error = "Ошибка при удалении рецепта";
                    if (response.errorBody() != null) {
                        try {
                            error = response.errorBody().string();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                    callback.onDeleteFailure(error);
                }
            }

            @Override
            public void onFailure(Call<GeneralServerResponse> call, Throwable t) {
                if (callback != null) {
                    callback.onDeleteFailure(t.getMessage());
                }
            }
        });
    }
}
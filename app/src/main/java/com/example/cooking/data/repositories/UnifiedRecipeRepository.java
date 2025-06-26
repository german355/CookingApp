package com.example.cooking.data.repositories;

import android.app.Application;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import android.os.Handler;
import android.os.Looper;
import com.example.cooking.Recipe.Recipe;
import com.example.cooking.network.models.GeneralServerResponse;
import com.example.cooking.network.utils.Resource;
import com.example.cooking.utils.MySharedPreferences;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import java.io.IOException;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import com.google.gson.Gson;

public class UnifiedRecipeRepository extends NetworkRepository{
    private static final String TAG = "UnifiedRecipeRepository";

    private final RecipeLocalRepository localRepository;
    private final RecipeRemoteRepository remoteRepository;
    private final LikedRecipesRepository likedRecipesRepository;
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());
    private final Context context;
    private static final Gson gson = new Gson();
    private final ConnectivityManager connectivityManager;
    private final CompositeDisposable disposables = new CompositeDisposable();
    private final Set<Integer> pendingLikeRequests = new HashSet<>();
    private final MySharedPreferences prefs;

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

    public UnifiedRecipeRepository(Application application) {
        super(application);
        this.context = application.getApplicationContext();
        this.localRepository = new RecipeLocalRepository(application);
        this.remoteRepository = new RecipeRemoteRepository(application);
        this.likedRecipesRepository = new LikedRecipesRepository(application);
        this.prefs = new MySharedPreferences(context);
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
        Disposable d = Completable.fromAction(() -> {
            try {
                if (remoteRecipes != null) {
                    Set<Integer> likedRecipeIds = new HashSet<>();
                    // Проверяем авторизацию через MySharedPreferences
                    if (!prefs.getUserId().equals("0")) {
                        List<Integer> likedIdsList = likedRecipesRepository.getLikedRecipeIdsSync(); // Вызов к БД
                        if (likedIdsList != null) {
                            likedRecipeIds.addAll(likedIdsList);
                        }
                    }
                    Set<Integer> remoteRecipeIds = new HashSet<>();

                    for (Recipe remoteRecipe : remoteRecipes) {
                        remoteRecipe.setLiked(likedRecipeIds.contains(remoteRecipe.getId()));
                        remoteRecipeIds.add(remoteRecipe.getId());
                    }

                    // Атомарная замена всех записей для стабильного порядка
                    localRepository.replaceAllRecipes(remoteRecipes);
                    
                    // NEW: после успешной записи сразу читаем обратно, убеждаемся что данные реально в БД,
                    // и уведомляем observers — это гарантирует, что данные будут сохранены и отобразятся
                    List<Recipe> inserted = localRepository.getAllRecipesSync();
                    Log.d(TAG, "syncWithRemoteData: сохранено в локальную БД " + inserted.size() + " рецептов");
                    if (recipesLiveData != null) {
                        mainThreadHandler.post(() -> recipesLiveData.setValue(Resource.success(inserted)));
                    }

                    Log.d(TAG, "syncWithRemoteData: получено с сервера " + remoteRecipes.size() + " рецептов");

                } else {
                    // Если нет данных с сервера, проверяем доступность сети
                    if (!isNetworkAvailable()) {
                        // Загружаем данные из локального хранилища в офлайн режиме
                        List<Recipe> localRecipes = localRepository.getAllRecipesSync();
                        if (localRecipes != null && !localRecipes.isEmpty()) {
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
                if (errorMessage != null) {
                    // Обновляем LiveData об ошибке в основном потоке
                    mainThreadHandler.post(() -> errorMessage.setValue("Ошибка синхронизации" ));
                }
                // Также обновляем recipesLiveData с ошибкой
                if (recipesLiveData != null) {
                    mainThreadHandler.post(() -> recipesLiveData.setValue(Resource.error("Ошибка синхронизации", null)));
                }
            }
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(
            () -> {},
            throwable -> Log.e(TAG, "Ошибка syncWithRemoteData", throwable)
        );
        disposables.add(d);
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

    public void toggleLike(int recipeId) {
        Disposable d = Completable.fromAction(() -> {
            boolean isCurrentlyLiked = likedRecipesRepository.isRecipeLikedLocalSync(recipeId);
            boolean newLikeStatus = !isCurrentlyLiked;
            localRepository.updateLikeStatus(recipeId, newLikeStatus);
            if (newLikeStatus) {
                likedRecipesRepository.addLikedRecipe(recipeId);
            } else {
                likedRecipesRepository.removeLikedRecipe(recipeId);
            }
        })
        .subscribeOn(Schedulers.io())
        .subscribe(
            () -> {},
            throwable -> Log.e(TAG, "Ошибка toggleLike", throwable)
        );
        disposables.add(d);
    }

    public void setLikeStatus(int recipeId, boolean newLikeStatus) {
        if (!pendingLikeRequests.add(recipeId)) {
            Log.d(TAG, "Like request already in progress for recipeId=" + recipeId);
            return;
        }
        Disposable d = Completable.fromAction(() -> {
            // Обновляем локально статус лайка в основной таблице рецептов
            localRepository.updateLikeStatus(recipeId, newLikeStatus);
            // Также синхронно обновляем вспомогательную таблицу liked_recipes,
            // чтобы данные были консистентны для всех частей приложения
            likedRecipesRepository.updateLikeStatusLocal(recipeId, newLikeStatus);
        })
        .subscribeOn(Schedulers.io())
        .subscribe(
            () -> {
                Log.d(TAG, "Local like status updated, отправляем запрос на сервер");
                // Единичный серверный запрос для установки статуса лайка
                Call<GeneralServerResponse> apiCall = apiService.toggleLikeRecipe(recipeId);
                apiCall.enqueue(new Callback<GeneralServerResponse>() {
                    @Override
                    public void onResponse(Call<GeneralServerResponse> call, Response<GeneralServerResponse> response) {
                        if (!response.isSuccessful()) {
                            Log.e(TAG, "Ошибка при отправке статуса лайка: " + (response.errorBody() != null ? response.errorBody().toString() : response.message()));
                        }
                        pendingLikeRequests.remove(recipeId);
                    }
                    @Override
                    public void onFailure(Call<GeneralServerResponse> call, Throwable t) {
                        Log.e(TAG, "Сбой сети при установке статуса лайка", t);
                        pendingLikeRequests.remove(recipeId);
                    }
                });
            },
            throwable -> {
                Log.e(TAG, "Ошибка setLikeStatus", throwable);
                pendingLikeRequests.remove(recipeId);
            }
        );
        disposables.add(d);
    }

    /**
     * Выполняет поиск в локальных данных в фоновом потоке.
     * Результат возвращается через LiveData в основном потоке.
     * В офлайн режиме добавляет соответствующее уведомление.
     */
    public void searchInLocalData(String query, MutableLiveData<List<Recipe>> searchResultsLiveData, MutableLiveData<String> errorMessageLiveData) {
        Disposable d = Completable.fromAction(() -> {
            List<Recipe> allRecipes = getAllRecipesSync();
            if (allRecipes == null) allRecipes = Collections.emptyList();
            String lowerQuery = query.toLowerCase();
            List<Recipe> filteredResults = allRecipes.stream()
                    .filter(recipe -> matchesSearchQuery(recipe, lowerQuery))
                    .collect(Collectors.toList());
            searchResultsLiveData.postValue(filteredResults);
            if (!isNetworkAvailable() && errorMessageLiveData != null) {
                errorMessageLiveData.postValue("Поиск выполнен в офлайн режиме. Результаты могут быть неполными.");
            }
        })
        .subscribeOn(Schedulers.io())
        .subscribe(
            () -> {},
            throwable -> {
                Log.e(TAG, "Ошибка при выполнении локального поиска", throwable);
                if (errorMessageLiveData != null) {
                    errorMessageLiveData.postValue("Ошибка при поиске");
                }
            }
        );
        disposables.add(d);
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
        // Отправляем на сервер
        Disposable d = Single.defer(() -> {
            // Подготовка данных запроса
            RequestBody title = RequestBody.create(MediaType.parse("text/plain"), recipe.getTitle());
            RequestBody ingredients = RequestBody.create(MediaType.parse("text/plain"), gson.toJson(recipe.getIngredients()));
            RequestBody instructions = RequestBody.create(MediaType.parse("text/plain"), gson.toJson(recipe.getSteps()));
            MultipartBody.Part imagePart = null;
            if (imageBytes != null && imageBytes.length > 0) {
                RequestBody file = RequestBody.create(MediaType.parse("image/*"), imageBytes);
                imagePart = MultipartBody.Part.createFormData("photo", "image.jpg", file);
            }
            // Вызываем API
            return imagePart != null
                ? apiService.addRecipe(title, ingredients, instructions, imagePart)
                : apiService.addRecipeWithoutPhoto(title, ingredients, instructions);
        })
        .subscribeOn(Schedulers.io())
        // После получения ответа сохраняем локально
        .flatMap(response -> {
            Recipe saved = new Recipe();
            Integer id = response.getId() != null ? response.getId() : 0;
            saved.setId(id);
            saved.setTitle(recipe.getTitle());
            saved.setIngredients(recipe.getIngredients());
            saved.setSteps(recipe.getSteps());
            saved.setUserId(recipe.getUserId());
            return Completable.fromAction(() -> localRepository.insert(saved))
                .andThen(Single.just(new Object[]{response, saved}));
        })
        .observeOn(Schedulers.io())
        .subscribe(arr -> {
            Object[] r = (Object[]) arr;
            if (callback != null) callback.onSuccess((GeneralServerResponse) r[0], (Recipe) r[1]);
        }, err -> {
            String errorMessage;
            if (err instanceof retrofit2.HttpException) {
                retrofit2.HttpException httpException = (retrofit2.HttpException) err;
                errorMessage = getHumanReadableErrorMessage(httpException.code(), "Не удалось создать рецепт");
            } else if (err instanceof java.net.UnknownHostException || err instanceof java.net.ConnectException) {
                errorMessage = "Ошибка соединения с сервером. Проверьте подключение к интернету";
            } else {
                errorMessage = "Произошла ошибка при создании рецепта";
            }
            if (callback != null) callback.onFailure(errorMessage, null);
        });
        disposables.add(d);
    }

    /**
     * Обновляет существующий рецепт
     */
    public void updateRecipe(Recipe recipe, byte[] imageBytes, RecipeSaveCallback callback) {
        Disposable d = Single.defer(() -> {
            recipeBeingUpdated = localRepository.getRecipeByIdSync(recipe.getId());
            if (recipe == null) {
                return Single.<GeneralServerResponse>error(new IllegalArgumentException("Рецепт не может быть null"));
            }
            RequestBody title = RequestBody.create(MediaType.parse("text/plain"), recipe.getTitle());
            RequestBody ingredients = RequestBody.create(MediaType.parse("text/plain"), gson.toJson(recipe.getIngredients()));
            RequestBody instructions = RequestBody.create(MediaType.parse("text/plain"), gson.toJson(recipe.getSteps()));
            MultipartBody.Part imagePart = null;
            if (imageBytes != null && imageBytes.length > 0) {
                RequestBody reqFile = RequestBody.create(MediaType.parse("image/*"), imageBytes);
                imagePart = MultipartBody.Part.createFormData("photo", "recipe_image.jpg", reqFile);
            }
            int userPermission = prefs.getUserPermission();
            return apiService.updateRecipe(recipe.getId(), String.valueOf(userPermission), title, ingredients, instructions, imagePart);
        })
        .subscribeOn(Schedulers.io())
        .observeOn(Schedulers.io())
        .flatMap(response -> {
            Recipe toUpdate = recipeBeingUpdated != null ? recipeBeingUpdated : recipe;
            toUpdate.setTitle(recipe.getTitle());
            toUpdate.setIngredients(recipe.getIngredients());
            toUpdate.setSteps(recipe.getSteps());
            if (imageBytes != null && response.getPhotoUrl() != null) {
                toUpdate.setPhoto_url(response.getPhotoUrl());
            }
            return Completable.fromAction(() -> localRepository.update(toUpdate))
                .andThen(Single.just(new Object[]{response, toUpdate}));
        })
        .subscribe(arr -> {
            Object[] r = (Object[]) arr;
            callback.onSuccess((GeneralServerResponse) r[0], (Recipe) r[1]);
        }, err -> {
            String errorMessage;
            if (err instanceof retrofit2.HttpException) {
                retrofit2.HttpException httpException = (retrofit2.HttpException) err;
                errorMessage = getHumanReadableErrorMessage(httpException.code(), "Не удалось обновить рецепт");
            } else if (err instanceof java.net.UnknownHostException || err instanceof java.net.ConnectException) {
                errorMessage = "Ошибка соединения с сервером. Проверьте подключение к интернету";
            } else {
                errorMessage = "Произошла ошибка при обновлении рецепта";
            }
            callback.onFailure(errorMessage, null);
        });
        disposables.add(d);
    }

    /**
     * Вставляет рецепт в локальное хранилище
     *
     * @param recipe   рецепт для вставки
     * @param callback колбэк для обработки результата
     */
    public void insert(Recipe recipe, RecipeSaveCallback callback) {
        if (recipe == null) {
            if (callback != null) {
                callback.onFailure("Рецепт не может быть null", null);
            }
            return;
        }

        // Вставляем рецепт в локальное хранилище
        Disposable d = Completable.fromAction(() -> localRepository.insert(recipe))
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
                        throwable -> Log.e(TAG, "Ошибка saveRecipe local insert", throwable)
                );
        disposables.add(d);
    }

    /**
     * Очищает все подписки
     */
    public void clearDisposables() {
        disposables.clear();
    }

    /**
     * Преобразует код ошибки HTTP в понятное для пользователя сообщение
     */
    private String getHumanReadableErrorMessage(int statusCode, String defaultMessage) {
        switch (statusCode) {
            case 403:
                return "У вас нет прав для выполнения этой операции";
            case 401:
                return "Необходима авторизация. Пожалуйста, войдите в аккаунт";
            case 404:
                return "Рецепт не найден";
            case 400:
                return "Некорректные данные запроса";
            case 500:
            case 502:
            case 503:
                return "Ошибка сервера. Попробуйте позже";
            default:
                return defaultMessage;
        }
    }

    public void deleteRecipe(int recipeId, DeleteRecipeCallback callback) {
        // Проверяем доступность сети
        if (!isNetworkAvailable()) {
            if (callback != null) {
                callback.onDeleteFailure("Невозможно удалить рецепт в офлайн режиме");
            }
            return;
        }
        
        // Удаляем рецепт с сервера
        Call<GeneralServerResponse> call = apiService.deleteRecipe(
                recipeId,
                "delete"      // X-User-Permission заголовок (заглушка)
        );

        call.enqueue(new Callback<GeneralServerResponse>() {
            @Override
            public void onResponse(Call<GeneralServerResponse> call, Response<GeneralServerResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    // Удаляем рецепт из локальной базы данных
                    Disposable d = Completable.fromAction(() -> localRepository.deleteRecipe(recipeId))
                            .subscribeOn(Schedulers.io())
                            .subscribe(
                                () -> callback.onDeleteSuccess(),
                                throwable -> Log.e(TAG, "Ошибка удаления локального рецепта", throwable)
                            );
                    disposables.add(d);
                } else {
                    // Получаем понятное сообщение об ошибке на основе кода статуса
                    String error = getHumanReadableErrorMessage(response.code(), "Не удалось удалить рецепт");
                    callback.onDeleteFailure(error);
                }
            }

            @Override
            public void onFailure(Call<GeneralServerResponse> call, Throwable t) {
                if (callback != null) {
                    callback.onDeleteFailure("Ошибка соединения с сервером. Проверьте подключение к интернету");
                }
            }
        });
    }
}
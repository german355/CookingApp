package com.example.cooking.data.repositories;

import android.content.Context;
import android.util.Log;

import com.example.cooking.domain.entities.Recipe;
import com.example.cooking.network.models.recipeResponses.RecipesResponse;
import com.example.cooking.network.utils.ApiCallHandler;
import com.example.cooking.utils.MySharedPreferences;

import java.util.List;

import retrofit2.Call;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import com.google.gson.Gson;
import com.example.cooking.network.models.GeneralServerResponse;
import com.example.cooking.network.models.BaseApiResponse;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import retrofit2.HttpException;
import java.io.IOException;

/**
 * Репозиторий для работы с удаленным API рецептов
 */
public class RecipeRemoteRepository extends NetworkRepository {

    private static final String TAG = "RecipeRemoteRepository";
    private static final Gson gson = new Gson();
    private final MySharedPreferences preferences;
    private final CompositeDisposable disposables = new CompositeDisposable();
    
    // Флаг для предотвращения множественных одновременных запросов
    private volatile boolean isRequestInProgress = false;

    public interface RecipesCallback {
        void onRecipesLoaded(List<Recipe> recipes);
        void onDataNotAvailable(String error);
    }

    public interface RecipeSaveCallback {
        void onSuccess(GeneralServerResponse response, Recipe recipe);
        void onFailure(String error, GeneralServerResponse errorResponse);
    }

    public interface DeleteRecipeCallback {
        void onDeleteSuccess();
        void onDeleteFailure(String error);
    }

    public RecipeRemoteRepository(Context context) {
        super(context);
        this.preferences = new MySharedPreferences(context);
    }

    /**
     * Парсит HTTP ошибку для извлечения сообщения о модерации
     * @param throwable исключение от Retrofit/OkHttp
     * @return детальное сообщение об ошибке
     */
    private String parseHttpError(Throwable throwable) {
        if (throwable instanceof HttpException) {
            HttpException httpException = (HttpException) throwable;
            try {
                if (httpException.response() != null && httpException.response().errorBody() != null) {
                    String errorBody = httpException.response().errorBody().string();
                    Log.d(TAG, "HTTP error body: " + errorBody);
                    
                    // Пытаемся распарсить как BaseApiResponse
                    try {
                        BaseApiResponse errorResponse = gson.fromJson(errorBody, BaseApiResponse.class);
                        if (errorResponse != null && errorResponse.getMessage() != null && !errorResponse.getMessage().isEmpty()) {
                            String message = errorResponse.getMessage();
                            Log.d(TAG, "Extracted error message: " + message);
                            
                            // Специальная обработка сообщений о модерации
                            if (httpException.code() == 400) {
                                Log.i(TAG, "Ошибка модерации (400): " + message);
                                return "Модерация: " + message;
                            }
                            
                            return message;
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Не удалось распарсить ошибку как BaseApiResponse", e);
                    }
                    
                    // Если не удалось распарсить как JSON, возвращаем сырое сообщение
                    if (!errorBody.trim().isEmpty()) {
                        return "Ошибка сервера (" + httpException.code() + "): " + errorBody;
                    }
                }
                
                return "Ошибка сервера: " + httpException.code() + " " + httpException.message();
            } catch (IOException e) {
                Log.e(TAG, "Ошибка при чтении тела HTTP ошибки", e);
                return "Ошибка сервера: " + httpException.code() + " (не удалось прочитать детали)";
            }
        }
        
        // Для других типов ошибок возвращаем стандартное сообщение
        return throwable.getMessage() != null ? throwable.getMessage() : "Неизвестная ошибка сети";
    }

    /**
     * Получить рецепты с сервера
     * 
     * @param callback callback для возврата результата
     */
    public synchronized void getRecipes(final RecipesCallback callback) {
        // Проверяем, не выполняется ли уже запрос
        if (isRequestInProgress) {
            Log.d(TAG, "Запрос рецептов уже выполняется, пропускаем дублирующий запрос");
            callback.onDataNotAvailable("Запрос уже выполняется");
            return;
        }
        
        // Проверяем доступность сети
        if (!isNetworkAvailable()) {
            Log.d(TAG, "Сеть недоступна, отменяем запрос рецептов");
            callback.onDataNotAvailable("Нет подключения к интернету");
            return;
        }

        // Устанавливаем флаг выполнения запроса
        isRequestInProgress = true;
        Log.d(TAG, "Начинаем запрос рецептов с сервера");

        // Используем RxJava для выполнения запроса в фоновом потоке
        disposables.add(
            apiService.getRecipesRx()
            .subscribeOn(Schedulers.io()) // Выполняем в фоновом потоке
            .observeOn(AndroidSchedulers.mainThread()) // Результат в главном потоке
            .subscribe(
                response -> {
                    synchronized (RecipeRemoteRepository.this) {
                        isRequestInProgress = false; // Сбрасываем флаг при успехе
                    }
                    Log.d(TAG, "Запрос рецептов успешно завершен");
                    if (response.getRecipes() != null) {
                        List<Recipe> recipes = response.getRecipes();
                        Log.d(TAG, "Получено " + recipes.size() + " рецептов с сервера");
                        callback.onRecipesLoaded(recipes);
                    } else {
                        Log.w(TAG, "Список рецептов в ответе пуст");
                        callback.onDataNotAvailable("Список рецептов пуст");
                    }
                },
                throwable -> {
                    synchronized (RecipeRemoteRepository.this) {
                        isRequestInProgress = false; // Сбрасываем флаг при ошибке
                    }
                    Log.e(TAG, "Ошибка запроса рецептов: " + throwable.getMessage());
                    callback.onDataNotAvailable(throwable.getMessage());
                }
            )
        );
    }

    public void saveRecipe(Recipe recipe, byte[] imageBytes, RecipeSaveCallback callback) {
        // Подготовка данных запроса
        RequestBody title = RequestBody.create(MediaType.parse("text/plain"), recipe.getTitle());
        RequestBody ingredients = RequestBody.create(MediaType.parse("text/plain"), gson.toJson(recipe.getIngredients()));
        RequestBody instructions = RequestBody.create(MediaType.parse("text/plain"), gson.toJson(recipe.getSteps()));
        MultipartBody.Part imagePart = null;
        if (imageBytes != null && imageBytes.length > 0) {
            RequestBody file = RequestBody.create(MediaType.parse("image/*"), imageBytes);
            imagePart = MultipartBody.Part.createFormData("photo", "image.jpg", file);
        }
        
        disposables.add(
            apiService.addRecipe(title, ingredients, instructions, imagePart)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                response -> { if (callback != null) callback.onSuccess(response, recipe); },
                throwable -> { 
                    if (callback != null) {
                        String detailedError = parseHttpError(throwable);
                        Log.e(TAG, "Ошибка сохранения рецепта: " + detailedError);
                        
                        // Попытаемся извлечь GeneralServerResponse из HttpException
                        GeneralServerResponse errorResponse = null;
                        if (throwable instanceof HttpException) {
                            errorResponse = extractErrorResponse((HttpException) throwable);
                        }
                        
                        callback.onFailure(detailedError, errorResponse);
                    }
                }
            )
        );
    }

    public void updateRecipe(Recipe recipe, byte[] imageBytes, RecipeSaveCallback callback) {
        RequestBody title = RequestBody.create(MediaType.parse("text/plain"), recipe.getTitle());
        RequestBody ingredients = RequestBody.create(MediaType.parse("text/plain"), gson.toJson(recipe.getIngredients()));
        RequestBody instructions = RequestBody.create(MediaType.parse("text/plain"), gson.toJson(recipe.getSteps()));
        MultipartBody.Part imagePart = null;
        if (imageBytes != null && imageBytes.length > 0) {
            RequestBody reqFile = RequestBody.create(MediaType.parse("image/*"), imageBytes);
            imagePart = MultipartBody.Part.createFormData("photo", "recipe_image.jpg", reqFile);
        }
        
        disposables.add(
            apiService.updateRecipe(recipe.getId(), title, ingredients, instructions, imagePart)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                response -> { if (callback != null) callback.onSuccess(response, recipe); },
                throwable -> { 
                    if (callback != null) {
                        String detailedError = parseHttpError(throwable);
                        Log.e(TAG, "Ошибка обновления рецепта: " + detailedError);
                        
                        // Попытаемся извлечь GeneralServerResponse из HttpException
                        GeneralServerResponse errorResponse = null;
                        if (throwable instanceof HttpException) {
                            errorResponse = extractErrorResponse((HttpException) throwable);
                        }
                        
                        callback.onFailure(detailedError, errorResponse);
                    }
                }
            )
        );
    }

    /**
     * Извлекает GeneralServerResponse из HttpException для передачи в callback
     */
    private GeneralServerResponse extractErrorResponse(HttpException httpException) {
        try {
            if (httpException.response() != null && httpException.response().errorBody() != null) {
                String errorBody = httpException.response().errorBody().string();
                return gson.fromJson(errorBody, GeneralServerResponse.class);
            }
        } catch (Exception e) {
            Log.w(TAG, "Не удалось извлечь GeneralServerResponse из ошибки", e);
        }
        return null;
    }

    public void deleteRecipe(int recipeId, DeleteRecipeCallback callback) {
        disposables.add(
            apiService.deleteRecipe(recipeId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                () -> { if (callback != null) callback.onDeleteSuccess(); },
                throwable -> { if (callback != null) callback.onDeleteFailure(throwable.getMessage()); }
            )
        );
    }

    public void clearDisposables() {
        disposables.clear();
    }
}
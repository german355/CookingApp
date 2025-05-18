package com.example.cooking.network.services;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.cooking.Recipe.Ingredient;
import com.example.cooking.Recipe.Recipe;
import com.example.cooking.Recipe.Step;
import com.example.cooking.config.ServerConfig;
import com.example.cooking.data.repositories.LikedRecipesRepository;
import com.example.cooking.data.repositories.RecipeLocalRepository;
import com.example.cooking.network.api.ApiService;
import com.example.cooking.network.models.GeneralServerResponse;
import com.example.cooking.utils.MySharedPreferences;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Унифицированный сервис для всех операций с рецептами: создание, обновление, удаление
 */
public class RecipeService {
    private static final String TAG = "RecipeService";
    
    private final ApiService apiService;
    private final Context context;
    private final RecipeLocalRepository localRepository;
    private final Handler mainThreadHandler;
    private static final Gson gson = new Gson();

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
    
    /**
     * Конструктор
     * @param context контекст приложения
     */
    public RecipeService(Context context) {
        this.context = context.getApplicationContext();
        this.localRepository = new RecipeLocalRepository(this.context);
        this.apiService = NetworkService.getApiService(context);
        this.mainThreadHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Проверяет наличие интернет-соединения
     * @return true, если есть подключение к интернету
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
     * Добавляет новый рецепт
     * @param title Название рецепта
     * @param ingredients Список ингредиентов
     * @param steps Список шагов
     * @param userId ID пользователя
     * @param imageBytes Изображение рецепта (может быть null)
     * @param callback Обратный вызов результата
     */
    public void saveRecipe(String title, List<Ingredient> ingredients, List<Step> steps,
                          String userId, byte[] imageBytes,
                          final RecipeSaveCallback callback) {
        if (!isNetworkAvailable()) {
            Log.w(TAG, "saveRecipe: Сеть недоступна");
            callback.onFailure("Отсутствует подключение к интернету.", null);
            return;
        }

        String ingredientsJson = gson.toJson(ingredients);
        String stepsJson = gson.toJson(steps);
        
        RequestBody titleBody = RequestBody.create(title, MediaType.parse("text/plain"));
        RequestBody ingredientsBody = RequestBody.create(ingredientsJson, MediaType.parse("application/json"));
        RequestBody stepsBody = RequestBody.create(stepsJson, MediaType.parse("application/json"));
        RequestBody userIdBody = RequestBody.create(userId, MediaType.parse("text/plain"));
        
        Call<GeneralServerResponse> call;
        if (imageBytes != null && imageBytes.length > 0) {
            String fileName = "recipe_" + System.currentTimeMillis() + ".jpg";
            RequestBody requestFile = RequestBody.create(imageBytes, MediaType.parse("image/jpeg"));
            MultipartBody.Part photoPart = MultipartBody.Part.createFormData("photo", fileName, requestFile);
            call = apiService.addRecipe(titleBody, ingredientsBody, stepsBody, userIdBody, photoPart);
        } else {
            call = apiService.addRecipeWithoutPhoto(titleBody, ingredientsBody, stepsBody, userIdBody);
        }
        
        this.recipeBeingUpdated = null; 
        enqueueCall(call, callback, false);
    }

    /**
     * Обновляет существующий рецепт
     * @param title Название рецепта
     * @param ingredients Список ингредиентов
     * @param steps Список шагов
     * @param currentUserId ID текущего пользователя
     * @param recipeId ID рецепта
     * @param imageBytes Изображение рецепта (может быть null)
     * @param permission уровень прав доступа
     * @param callback Обратный вызов результата
     */
    public void updateRecipe(String title, List<Ingredient> ingredients, List<Step> steps,
                             String currentUserId, @NonNull Integer recipeId, byte[] imageBytes,
                             int permission, final RecipeSaveCallback callback) {
        if (!isNetworkAvailable()) {
            Log.w(TAG, "updateRecipe: Сеть недоступна");
            callback.onFailure("Отсутствует подключение к интернету.", null);
            return;
        }

        // Получаем оригинальный рецепт из БД, чтобы сохранить userId автора
        Recipe originalRecipe = localRepository.getRecipeByIdSync(recipeId);
        String authorUserId;

        if (originalRecipe != null) {
            authorUserId = originalRecipe.getUserId();
            if (authorUserId == null || authorUserId.isEmpty()) {
                Log.w(TAG, "updateRecipe: userId автора в локальной БД пуст для рецепта ID: " + recipeId + ". Используем currentUserId.");
                authorUserId = currentUserId; // Крайний случай, если в БД нет userId
            }
        } else {
            Log.e(TAG, "updateRecipe: Не удалось найти оригинальный рецепт в локальной БД для ID: " + recipeId + ". Обновление userId автора может быть некорректным.");
            authorUserId = currentUserId; // Временное решение, если рецепт не найден
        }

        String ingredientsJson = gson.toJson(ingredients);
        String stepsJson = gson.toJson(steps);
        
        RequestBody titleBody = RequestBody.create(title, MediaType.parse("text/plain"));
        RequestBody ingredientsBody = RequestBody.create(ingredientsJson, MediaType.parse("application/json"));
        RequestBody stepsBody = RequestBody.create(stepsJson, MediaType.parse("application/json"));
        
        MultipartBody.Part photoPart = null;

        if (imageBytes != null && imageBytes.length > 0) {
            String fileName = "recipe_" + System.currentTimeMillis() + ".jpg";
            RequestBody requestFile = RequestBody.create(imageBytes, MediaType.parse("image/jpeg"));
            photoPart = MultipartBody.Part.createFormData("photo", fileName, requestFile);
        }

        this.recipeBeingUpdated = new Recipe();
        this.recipeBeingUpdated.setId(recipeId);
        this.recipeBeingUpdated.setTitle(title);
        this.recipeBeingUpdated.setIngredients(ingredients != null ? new ArrayList<>(ingredients) : new ArrayList<>());
        this.recipeBeingUpdated.setSteps(steps != null ? new ArrayList<>(steps) : new ArrayList<>());
        // Используем userId ОРИГИНАЛЬНОГО АВТОРА для сохранения в БД
        this.recipeBeingUpdated.setUserId(authorUserId); 

        Call<GeneralServerResponse> call = apiService.updateRecipe(
                recipeId,
                currentUserId, // ID текущего пользователя для проверки прав на сервере
                String.valueOf(permission),
                titleBody,
                ingredientsBody,
                stepsBody,
                photoPart
        );
        enqueueCall(call, callback, true);
    }
    
    /**
     * Удаляет рецепт
     * @param recipeId ID рецепта
     * @param userId ID пользователя
     * @param permission уровень прав доступа
     * @param callback колбэк для обработки результата
     */
    public void deleteRecipe(final int recipeId, final String userId, final int permission, final DeleteRecipeCallback callback) {
        if (!isNetworkAvailable()) {
            Log.w(TAG, "deleteRecipe: Сеть недоступна");
            callback.onDeleteFailure("Отсутствует подключение к интернету.");
            return;
        }
        
        Call<GeneralServerResponse> call = apiService.deleteRecipe(recipeId, userId, String.valueOf(permission));
        
        call.enqueue(new Callback<GeneralServerResponse>() {
            @Override
            public void onResponse(@NonNull Call<GeneralServerResponse> call, @NonNull Response<GeneralServerResponse> response) {
                boolean success = response.isSuccessful() && response.body() != null && response.body().isSuccess();
                
                if (success) {
                    // Локальные операции после успешного удаления на сервере
                    try {
                        // Удаляем запись об этом рецепте из локальной базы лайкнутых, если она там была
                        MySharedPreferences prefs = new MySharedPreferences(context);
                        String currentUserId = prefs.getString("userId", "0");

                        if (!currentUserId.equals("0")) {
                            LikedRecipesRepository likedRepo = new LikedRecipesRepository(context);
                            likedRepo.deleteLikedRecipeLocal(recipeId, currentUserId);
                            Log.d(TAG, "Запись о лайке для удаленного рецепта (ID: " + recipeId + ") удалена из локальной базы лайков.");
                        } else {
                            Log.w(TAG, "Не удалось получить currentUserId, удаление лайка пропущено.");
                        }

                        // Удаляем сам рецепт из основной локальной базы данных (Room)
                        localRepository.deleteRecipe(recipeId);
                        Log.d(TAG, "Рецепт (ID: " + recipeId + ") удален из локальной БД после успешного удаления с сервера.");

                    } catch (Exception e) {
                        Log.e(TAG, "Ошибка при локальных операциях после удаления рецепта с сервера", e);
                        // Успех на сервере есть, но локальные операции провалились.
                    }
                    
                    mainThreadHandler.post(() -> {
                        callback.onDeleteSuccess();
                        Log.d(TAG, "Рецепт (ID: " + recipeId + ") был успешно удален с сервера и локальные данные обновлены.");
                    });
                } else {
                    String errorMessage;
                    if (response.code() == 403) {
                        errorMessage = "У вас нет прав на удаление этого рецепта. Только автор рецепта или администратор могут удалять рецепты.";
                    } else {
                        if (response.body() != null && response.body().getMessage() != null) {
                            errorMessage = response.body().getMessage();
                        } else {
                            errorMessage = "Ошибка сервера: " + response.code();
                        }
                    }
                    
                    final String finalErrorMessage = errorMessage;
                    mainThreadHandler.post(() -> {
                        callback.onDeleteFailure(finalErrorMessage);
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call<GeneralServerResponse> call, @NonNull Throwable t) {
                final String errorMessage = "Сетевая ошибка: " + t.getMessage();
                Log.e(TAG, "Ошибка при удалении рецепта", t);
                
                mainThreadHandler.post(() -> {
                    callback.onDeleteFailure(errorMessage);
                });
            }
        });
    }
    
    /**
     * Вспомогательный метод для обработки запросов добавления/обновления рецептов
     */
    private void enqueueCall(Call<GeneralServerResponse> call, final RecipeSaveCallback callback, final boolean isUpdate) {
        call.enqueue(new Callback<GeneralServerResponse>() {
            @Override
            public void onResponse(@NonNull Call<GeneralServerResponse> call, @NonNull Response<GeneralServerResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    GeneralServerResponse serverResponse = response.body();
                    Recipe recipeToUpdateLocally = null;

                    if (serverResponse.isSuccess()) {
                        if (isUpdate && recipeBeingUpdated != null) {
                            Log.d(TAG, "Успешное обновление на сервере. Обновляем локально рецепт ID: " + recipeBeingUpdated.getId());
                            localRepository.update(recipeBeingUpdated);
                            recipeToUpdateLocally = recipeBeingUpdated;
                        } else if (!isUpdate && serverResponse.getId() != null) {
                            Log.d(TAG, "Рецепт успешно создан на сервере с ID: " + serverResponse.getId());
                        }
                        callback.onSuccess(serverResponse, recipeToUpdateLocally);
                    } else {
                        String errorMessage = serverResponse.getMessage() != null ? serverResponse.getMessage() : "Неизвестная ошибка от сервера (success:false).";
                        Log.w(TAG, "Server returned success:false. Message: " + errorMessage);
                        callback.onFailure(errorMessage, serverResponse);
                    }
                } else {
                    GeneralServerResponse errorResponse = null;
                    String errorMessage = "Ошибка сервера (код: " + response.code() + ")";
                    if (response.errorBody() != null) {
                        try {
                            String errorBodyString = response.errorBody().string();
                            Log.e(TAG, "Error body: " + errorBodyString);
                            errorResponse = gson.fromJson(errorBodyString, GeneralServerResponse.class);
                            if (errorResponse != null && errorResponse.getMessage() != null) {
                                errorMessage = errorResponse.getMessage();
                            } else if (errorResponse != null && !errorResponse.isSuccess()){
                                errorMessage = "Ошибка от сервера: " + errorBodyString;
                            } else {
                                errorMessage = "Ошибка чтения ответа сервера (код: " + response.code() + ")";
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Ошибка при чтении errorBody", e);
                            errorMessage = "Ошибка при разборе ответа сервера: " + e.getMessage();
                        }
                    }
                    Log.e(TAG, errorMessage);
                    callback.onFailure(errorMessage, errorResponse);
                }
            }

            @Override
            public void onFailure(@NonNull Call<GeneralServerResponse> call, @NonNull Throwable t) {
                Log.e(TAG, "Ошибка сети при сохранении рецепта", t);
                callback.onFailure("Ошибка сети: " + t.getMessage(), null);
            }
        });
    }
} 
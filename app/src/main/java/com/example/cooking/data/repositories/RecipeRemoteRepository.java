package com.example.cooking.data.repositories;

import android.content.Context;
import android.util.Log;

import com.example.cooking.R;
import com.example.cooking.domain.entities.Recipe;
import com.example.cooking.domain.entities.Ingredient;
import com.example.cooking.domain.entities.Step;

import com.example.cooking.utils.MySharedPreferences;

import java.util.List;

import retrofit2.Call;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import com.google.gson.Gson;
import com.example.cooking.network.models.GeneralServerResponse;
import com.example.cooking.network.models.BaseApiResponse;
import com.example.cooking.network.models.recipeResponses.BulkRecipesResponse;
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
                                return context.getString(R.string.moderation_prefix) + " " + message;
                            }
                            
                            return message;
                        }
                    } catch (Exception e) {
                        Log.w(TAG, "Не удалось распарсить ошибку как BaseApiResponse", e);
                    }
                    
                    if (!errorBody.trim().isEmpty()) {
                        return context.getString(R.string.error_server_with_body, httpException.code(), errorBody);
                    }
                }
                
                return context.getString(R.string.error_server_with_message, httpException.code(), httpException.message());
            } catch (IOException e) {
                Log.e(TAG, "Ошибка при чтении тела HTTP ошибки", e);
                return context.getString(R.string.error_server_body_unreadable, httpException.code());
            }
        }
        
        return throwable.getMessage() != null
                ? throwable.getMessage()
                : context.getString(R.string.error_unknown_network);
    }

    /**
     * Получить рецепты с сервера
     */
    public synchronized void getRecipes(final RecipesCallback callback) {
        if (isRequestInProgress) {
            callback.onDataNotAvailable(context.getString(R.string.error_request_in_progress));
            return;
        }
        
        if (!isNetworkAvailable()) {
            Log.d(TAG, "Сеть недоступна, отменяем запрос рецептов");
            callback.onDataNotAvailable(context.getString(R.string.error_no_internet_connection));
            return;
        }

        isRequestInProgress = true;
        Log.d(TAG, "Начинаем запрос рецептов с сервера");

        // Используем RxJava для выполнения запроса в фоновом потоке
        disposables.add(
            apiService.getRecipesRx()
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
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
                        callback.onDataNotAvailable(context.getString(R.string.error_recipes_list_empty));
                    }
                },
                throwable -> {
                    synchronized (RecipeRemoteRepository.this) {
                        isRequestInProgress = false; // Сбрасываем флаг при ошибке
                    }
                    String detailedError = parseHttpError(throwable);
                    Log.e(TAG, "Ошибка запроса рецептов: " + detailedError);
                    callback.onDataNotAvailable(detailedError);
                }
            )
        );
    }

    public synchronized void getMyRecipes(final RecipesCallback callback) {
        if (isRequestInProgress) {
            callback.onDataNotAvailable(context.getString(R.string.error_request_in_progress));
            return;
        }

        if (!isNetworkAvailable()) {
            callback.onDataNotAvailable(context.getString(R.string.error_no_internet_connection));
            return;
        }

        isRequestInProgress = true;
        disposables.add(
            apiService.getMyRecipes()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    response -> {
                        synchronized (RecipeRemoteRepository.this) {
                            isRequestInProgress = false;
                        }
                        if (response.getRecipes() != null) {
                            callback.onRecipesLoaded(response.getRecipes());
                        } else {
                            callback.onDataNotAvailable(context.getString(R.string.error_recipes_list_empty));
                        }
                    },
                    throwable -> {
                        synchronized (RecipeRemoteRepository.this) {
                            isRequestInProgress = false;
                        }
                        callback.onDataNotAvailable(parseHttpError(throwable));
                    }
                )
        );
    }

    public List<Recipe> getRecipesBulkSync(List<Integer> recipeIds) {
        if (recipeIds == null || recipeIds.isEmpty()) {
            return java.util.Collections.emptyList();
        }
        BulkRecipesResponse response = apiService.getRecipesBulk(
                new com.example.cooking.network.models.recipeResponses.RecipeIdsRequest(recipeIds)
        ).blockingGet();
        return response != null && response.getData() != null
                ? response.getData()
                : java.util.Collections.emptyList();
    }

    public void saveRecipe(Recipe recipe, byte[] imageBytes, RecipeSaveCallback callback) {
        RequestBody title = RequestBody.create(MediaType.parse("text/plain"), recipe.getTitle());
        RequestBody ingredients = RequestBody.create(MediaType.parse("text/plain"), serializeIngredients(recipe.getIngredients()));
        RequestBody instructions = RequestBody.create(MediaType.parse("text/plain"), serializeInstructions(recipe.getSteps()));
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
        RequestBody ingredients = RequestBody.create(MediaType.parse("text/plain"), serializeIngredients(recipe.getIngredients()));
        RequestBody instructions = RequestBody.create(MediaType.parse("text/plain"), serializeInstructions(recipe.getSteps()));
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

    private String serializeIngredients(List<Ingredient> ingredients) {
        if (ingredients == null) {
            return "[]";
        }
        List<IngredientPayload> payloads = new java.util.ArrayList<>(ingredients.size());
        for (Ingredient ingredient : ingredients) {
            String name = ingredient != null ? ingredient.getName() : null;
            String count = ingredient != null ? String.valueOf(ingredient.getCount()) : null;
            String type = ingredient != null ? ingredient.getNormalizedType() : null;
            payloads.add(new IngredientPayload(name, count, type));
        }
        return gson.toJson(payloads);
    }

    private String serializeInstructions(List<Step> steps) {
        if (steps == null) {
            return "[]";
        }
        List<InstructionPayload> payloads = new java.util.ArrayList<>(steps.size());
        for (Step step : steps) {
            int number = step != null ? step.getNumber() : 0;
            String instruction = step != null ? step.getInstruction() : null;
            payloads.add(new InstructionPayload(number, instruction));
        }
        return gson.toJson(payloads);
    }

    private static final class IngredientPayload {
        private final String name;
        private final String count;
        private final String type;

        private IngredientPayload(String name, String count, String type) {
            this.name = name;
            this.count = count;
            this.type = type;
        }
    }

    private static final class InstructionPayload {
        private final int number;
        private final String instruction;

        private InstructionPayload(int number, String instruction) {
            this.number = number;
            this.instruction = instruction;
        }
    }

    public void deleteRecipe(int recipeId, DeleteRecipeCallback callback) {
        disposables.add(
            apiService.deleteRecipe(recipeId)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(
                () -> { if (callback != null) callback.onDeleteSuccess(); },
                throwable -> {
                    if (callback != null) {
                        callback.onDeleteFailure(parseHttpError(throwable));
                    }
                }
            )
        );
    }

    public void clearDisposables() {
        disposables.clear();
    }
}

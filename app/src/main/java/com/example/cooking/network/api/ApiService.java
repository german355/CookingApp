package com.example.cooking.network.api;

import com.example.cooking.Recipe.Recipe;
import com.example.cooking.auth.UserLoginRequest;
import com.example.cooking.auth.UserRegisterRequest;
import com.example.cooking.data.models.ApiResponse;
import com.example.cooking.data.models.PasswordResetRequest;
import com.example.cooking.data.models.PasswordResetResponse;
import com.example.cooking.network.models.GeneralServerResponse;
import com.example.cooking.network.responses.RecipesResponse;
import com.example.cooking.network.responses.SearchResponse;

import java.util.List;
import java.util.Map;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

/**
 * Единый интерфейс API для взаимодействия с сервером
 */
public interface ApiService {
    
    // =============== Аутентификация и пользователи ===============
    
    /**
     * Регистрация пользователя
     * @param request запрос с данными регистрации
     * @return ответ сервера
     */
    @POST("auth/register")
    Call<ApiResponse> registerUser(@Body UserRegisterRequest request);
    
    /**
     * Вход пользователя
     * @param request запрос с данными для входа
     * @return ответ сервера
     */
    @POST("auth/login")
    Call<ApiResponse> loginUser(@Body UserLoginRequest request);
    
    /**
     * Запрос на сброс пароля
     * @param request запрос на сброс пароля
     * @return ответ сервера
     */
    @POST("auth/password-reset-request")
    Call<ApiResponse> requestPasswordReset(@Body PasswordResetRequest request);
    
    // =============== Рецепты - получение и поиск ===============
    
    /**
     * Получает список всех рецептов
     * @param userId ID пользователя (опционально)
     * @return Call объект с ответом типа RecipesResponse
     */
    @GET("recipes")
    Call<RecipesResponse> getRecipes();
    
    /**
     * Альтернативный метод для получения рецептов в виде строки
     * Используется как запасной вариант, когда возникают проблемы с десериализацией JSON
     * @return Call объект с ответом в виде строки
     */
    @GET("recipes")
    Call<String> getRecipesAsString();
    
    /**
     * Получает список лайкнутых рецептов пользователя.
     * @param userId ID пользователя
     * @return Call объект с ответом типа RecipesResponse
     */
    @GET("recipes/liked")
    Call<RecipesResponse> getLikedRecipes(@Query("userId") String userId);
    
    /**
     * Простой поиск рецептов по строке.
     * @param query строка поиска
     * @return Call объект с ответом типа RecipesResponse
     */
    @GET("recipes/search-simple")
    Call<RecipesResponse> searchRecipesSimple(@Query("q") String query);
    
    /**
     * Расширенный поиск рецептов с пагинацией
     * @param query строка поиска
     * @param userId ID пользователя
     * @param page номер страницы
     * @param perPage количество рецептов на странице
     * @return Call объект с ответом типа SearchResponse
     */
    @GET("search")
    Call<SearchResponse> searchRecipes(
        @Query("q")        String query,
        @Query("user_id")  String userId,
        @Query("page")     int page,
        @Query("per_page") int perPage
    );
    
    // =============== Рецепты - создание, обновление, удаление ===============
    
    /**
     * Метод для ДОБАВЛЕНИЯ нового рецепта с фото
     */
    @Multipart
    @POST("recipes/add")
    Call<GeneralServerResponse> addRecipe(
            @Part("title") RequestBody title,
            @Part("ingredients") RequestBody ingredients,
            @Part("instructions") RequestBody instructions,
            @Part("userId") RequestBody userId,
            @Part MultipartBody.Part photo
    );
    
    /**
     * Метод для ДОБАВЛЕНИЯ нового рецепта без фото
     */
    @Multipart
    @POST("recipes/add")
    Call<GeneralServerResponse> addRecipeWithoutPhoto(
            @Part("title") RequestBody title,
            @Part("ingredients") RequestBody ingredients,
            @Part("instructions") RequestBody instructions,
            @Part("userId") RequestBody userId
    );
    
    /**
     * Метод для ОБНОВЛЕНИЯ существующего рецепта
     */
    @Multipart
    @PUT("recipes/update/{id}")
    Call<GeneralServerResponse> updateRecipe(
            @Path("id") int recipeId,
            @Header("X-User-ID") String userIdHeader,
            @Header("X-User-Permission") String permissionHeader,
            @Part("title") RequestBody title,
            @Part("ingredients") RequestBody ingredients,
            @Part("instructions") RequestBody instructions,
            @Part MultipartBody.Part photo
    );
    
    /**
     * Метод для УДАЛЕНИЯ рецепта
     */
    @DELETE("recipes/{id}")
    Call<GeneralServerResponse> deleteRecipe(
            @Path("id") int recipeId,
            @Header("X-User-ID") String userIdHeader,
            @Header("X-User-Permission") String permissionHeader
    );

    // =============== Лайки рецептов ===============

    /**
     * Поставить/снять лайк рецепту
     * @param recipeId ID рецепта
     * @param userData тело запроса с userId
     * @return ответ сервера
     */
    @POST("recipes/{recipeId}/like")
    Call<GeneralServerResponse> toggleLikeRecipe(
        @Path("recipeId") int recipeId, 
        @Body Map<String, String> userData
    );
} 
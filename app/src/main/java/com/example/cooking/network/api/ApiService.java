package com.example.cooking.network.api;

import com.example.cooking.Recipe.Recipe;
import com.example.cooking.auth.UserLoginRequest;
import com.example.cooking.auth.UserRegisterRequest;
import com.example.cooking.data.models.ApiResponse;
import com.example.cooking.data.models.PasswordResetRequest;
import com.example.cooking.data.models.PasswordResetResponse;
import com.example.cooking.network.models.GeneralServerResponse;
import com.example.cooking.network.models.chat.ChatHistoryResponse;
import com.example.cooking.network.models.chat.ChatMessageRequest;
import com.example.cooking.network.models.chat.ChatMessageResponse;
import com.example.cooking.network.models.chat.ChatSessionResponse;
import com.example.cooking.network.responses.LikedRecipesResponse;
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

import io.reactivex.rxjava3.core.Single;

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
    Single<ApiResponse> registerUser(@Body UserRegisterRequest request);
    
    /**
     * Вход пользователя
     * @return ответ сервера
     */
    @POST("auth/login")
    Single<ApiResponse> loginUser();
    
    /**
     * Запрос на сброс пароля
     * @param request запрос на сброс пароля
     * @return ответ сервера
     */
    @POST("auth/password-reset-request")
    Single<ApiResponse> requestPasswordReset(@Body PasswordResetRequest request);
    
    // =============== Рецепты - получение и поиск ===============
    
    /**
     * Получает список всех рецептов.
     * Идентификация пользователя происходит через Firebase токен в заголовках.
     * @return Call объект с ответом типа RecipesResponse
     */
    @GET("recipes")
    Call<RecipesResponse> getRecipes();
    
    /**
     * Получает список ID лайкнутых рецептов пользователя.
     * Идентификация пользователя происходит через Firebase токен в заголовках.
     * @return Call объект с ответом типа LikedRecipesResponse
     */
    @GET("recipes/liked")
    Call<LikedRecipesResponse> getLikedRecipes();
    
    /**
     * Простой поиск рецептов по строке.
     * @param query строка поиска
     * @return Call объект с ответом типа SearchResponse
     */
    @GET("recipes/search-simple")
    Call<SearchResponse> searchRecipesSimple(@Query("q") String query);
    
    // =============== Рецепты - создание, обновление, удаление ===============
    
    /**
     * Метод для ДОБАВЛЕНИЯ нового рецепта с фото
     */
    @Multipart
    @POST("recipes/add")
    Single<GeneralServerResponse> addRecipe(
            @Part("title") RequestBody title,
            @Part("ingredients") RequestBody ingredients,
            @Part("instructions") RequestBody instructions,
            @Part MultipartBody.Part photo
    );
    
    /**
     * Метод для ДОБАВЛЕНИЯ нового рецепта без фото
     */
    @Multipart
    @POST("recipes/add")
    Single<GeneralServerResponse> addRecipeWithoutPhoto(
            @Part("title") RequestBody title,
            @Part("ingredients") RequestBody ingredients,
            @Part("instructions") RequestBody instructions
    );
    
    /**
     * Метод для ОБНОВЛЕНИЯ существующего рецепта
     */
    @Multipart
    @PUT("recipes/update/{id}")
    Single<GeneralServerResponse> updateRecipe(
            @Path("id") int recipeId,
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
            @Header("X-User-Permission") String permissionHeader
    );

    // =============== Лайки рецептов ===============

    /**
     * Поставить/снять лайк рецепту
     * @param recipeId ID рецепта
     * @return ответ сервера
     */
    @POST("recipes/{recipeId}/like")
    Call<GeneralServerResponse> toggleLikeRecipe(
        @Path("recipeId") int recipeId
    );


    /**
     * Расширенный поиск рецептов с пагинацией.
     * Идентификация пользователя происходит через Firebase токен в заголовках.
     * @param query строка поиска
     * @param page номер страницы
     * @param perPage количество рецептов на странице
     * @return Call объект с ответом типа SearchResponse
     */
    @GET("search")
    Call<SearchResponse> searchRecipes(
            @Query("q")        String query,
            @Query("page")     int page,
            @Query("per_page") int perPage
    );


    // =============== AI Chat ===============

    /**
     * Запуск новой сессии AI-чата
     */
    @POST("chatbot/start-session")
    Call<ChatSessionResponse> startChatSession();

    /**
     * Отправка сообщения в AI-чат
     * @param request тело запроса с текстом сообщения
     */
    @POST("chatbot/send-message")
    Call<ChatMessageResponse> sendChatMessage(@Body ChatMessageRequest request);

    /**
     * Получение истории сообщений текущей сессии AI-чатa
     */
    @GET("chatbot/get-history")
    Call<ChatHistoryResponse> getChatHistory();
} 
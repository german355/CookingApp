package com.example.cooking.network.api;

import com.example.cooking.auth.UserRegisterRequest;
import com.example.cooking.data.models.ApiResponse;
import com.example.cooking.data.models.PasswordResetRequest;
import com.example.cooking.network.models.GeneralServerResponse;
import com.example.cooking.network.models.chat.ChatHistoryResponse;
import com.example.cooking.network.models.chat.ChatMessageRequest;
import com.example.cooking.network.models.chat.ChatMessageResponse;
import com.example.cooking.network.models.chat.ChatSessionResponse;
import com.example.cooking.network.models.recipeResponses.LikedRecipesResponse;
import com.example.cooking.network.models.recipeResponses.RecipesResponse;
import com.example.cooking.network.models.recipeResponses.SearchResponse;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;

/**
 * Единый интерфейс API для взаимодействия с сервером
 */
public interface ApiService {
    
    // =============== Аутентификация и пользователи ===============
    
    /**
     * Регистрация пользователя
     */
    @POST("auth/register")
    Single<ApiResponse> registerUser(@Body UserRegisterRequest request);
    
    /**
     * Вход пользователя
     */
    @POST("auth/login")
    Single<ApiResponse> loginUser();
    
    /**
     * Запрос на сброс пароля
     */
    @POST("auth/password-reset-request")
    Single<ApiResponse> requestPasswordReset(@Body PasswordResetRequest request);
    
    // =============== Рецепты - получение и поиск ===============
    
    /**
     * Получает список всех рецептов (RxJava версия).
     */
    @GET("recipes")
    Single<RecipesResponse> getRecipesRx();
    
    /**
     * Получает список ID лайкнутых рецептов пользователя.
     * Идентификация пользователя происходит через Firebase токен в заголовках.
     */
    @GET("recipes/liked")
    Single<LikedRecipesResponse> getLikedRecipes();
    
    /**
     * Простой поиск рецептов по строке.
     */
    @GET("recipes/search/simple")
    Single<SearchResponse> searchRecipesSimple(@Query("q") String query);
    
    // =============== Рецепты - создание, обновление, удаление ===============
    
    /**
     * Метод для ДОБАВЛЕНИЯ нового рецепта 
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
     * Метод для ОБНОВЛЕНИЯ существующего рецепта
     */
    @Multipart
    @PUT("recipes/update/{id}")
    Single<GeneralServerResponse> updateRecipe(
            @Path("id") int recipeId,
            @Part("title") RequestBody title,
            @Part("ingredients") RequestBody ingredients,
            @Part("instructions") RequestBody instructions,
            @Part MultipartBody.Part photo
    );
    
    /**
     * Метод для УДАЛЕНИЯ рецепта
     */
    @DELETE("recipes/{id}")
    Completable deleteRecipe(@Path("id") int recipeId);

    // =============== Лайки рецептов ===============

    @POST("recipes/{recipeId}/like")
    Completable toggleLikeRecipeCompletable(@Path("recipeId") int recipeId);

    /**
     * Расширенный поиск рецептов с пагинацией.(пагинация пока что условная)
     */
    @GET("search")
    Single<SearchResponse> searchRecipes(
            @Query("q")        String query,
            @Query("page")     int page,
            @Query("per_page") int perPage
    );


    // =============== AI Chat ===============

    /**
     * Запуск новой сессии AI-чата
     */
    @POST("chatbot/start-session")
    Single<ChatSessionResponse> startChatSession();

    /**
     * Отправка сообщения в AI-чат
     * @param request тело запроса с текстом сообщения
     */
    @POST("chatbot/send-message")
    Single<ChatMessageResponse> sendChatMessage(@Body ChatMessageRequest request);

    /**
     * Получение истории сообщений текущей сессии AI-чатa
     */
    @GET("chatbot/get-history")
    Single<ChatHistoryResponse> getChatHistory();
} 
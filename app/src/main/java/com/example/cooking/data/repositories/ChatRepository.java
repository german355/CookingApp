package com.example.cooking.data.repositories;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;

import com.example.cooking.domain.entities.Recipe;
import com.example.cooking.network.models.BaseApiResponse;
import com.example.cooking.network.models.chat.ChatSessionResponse;
import com.example.cooking.network.models.chat.ChatMessageRequest;
import com.example.cooking.network.models.chat.ChatMessageResponse;
import com.example.cooking.network.models.chat.ChatHistoryResponse;
import com.example.cooking.network.models.recipeResponses.BulkRecipesResponse;
import com.example.cooking.network.models.recipeResponses.RecipeIdsRequest;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import io.reactivex.rxjava3.schedulers.Schedulers;
import retrofit2.HttpException;

/**
 * Репозиторий для AI-чата.
 * Инкапсулирует сетевые вызовы к ApiService для чата.
 */
public class ChatRepository extends NetworkRepository {
    private static final String TAG = "ChatRepository";
    private static final Gson gson = new Gson();

    public ChatRepository(Context context) {
        super(context);
    }

    public LiveData<ChatSessionResponse> startChatSession() {
        return LiveDataReactiveStreams.fromPublisher(
            apiService.startChatSession()
                .toFlowable()
                .subscribeOn(Schedulers.io())
                .onErrorReturnItem(new ChatSessionResponse())
        );
    }

    public LiveData<ChatMessageResponse> sendChatMessage(String message, Integer recipeId, String clientRequestId) {
        ChatMessageRequest request = new ChatMessageRequest(message, recipeId, clientRequestId);
        return LiveDataReactiveStreams.fromPublisher(
            apiService.sendChatMessage(request)
                .toFlowable()
                .subscribeOn(Schedulers.io())
                .onErrorReturn(this::buildChatErrorResponse)
        );
    }

    public LiveData<ChatHistoryResponse> getChatHistory() {
        return LiveDataReactiveStreams.fromPublisher(
            apiService.getChatHistory()
                .toFlowable()
                .subscribeOn(Schedulers.io())
                .onErrorReturnItem(new ChatHistoryResponse())
        );
    }

    public List<Recipe> fetchRecipesByIdsSync(List<Integer> recipeIds) {
        if (recipeIds == null || recipeIds.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            BulkRecipesResponse response = apiService.getRecipesBulk(new RecipeIdsRequest(recipeIds)).blockingGet();
            return response != null ? response.getData() : Collections.emptyList();
        } catch (Exception e) {
            Log.e(TAG, "Не удалось получить рецепты по ID для чата", e);
            return Collections.emptyList();
        }
    }

    private ChatMessageResponse buildChatErrorResponse(Throwable throwable) {
        ChatMessageResponse response = new ChatMessageResponse();
        response.setSuccess(false);
        response.setStatus("error");

        if (throwable instanceof HttpException) {
            HttpException httpException = (HttpException) throwable;
            try {
                if (httpException.response() != null) {
                    if (httpException.response().headers() != null) {
                        String retryAfterHeader = httpException.response().headers().get("Retry-After");
                        if (retryAfterHeader != null) {
                            try {
                                response.setRetryAfterSeconds(Integer.parseInt(retryAfterHeader.trim()));
                            } catch (NumberFormatException ignored) {
                                Log.w(TAG, "Не удалось распарсить Retry-After: " + retryAfterHeader);
                            }
                        }
                    }

                    if (httpException.response().errorBody() != null) {
                        String errorBody = httpException.response().errorBody().string();
                        BaseApiResponse parsed = gson.fromJson(errorBody, BaseApiResponse.class);
                        if (parsed != null) {
                            response.setMessage(parsed.getMessage());
                            response.setErrorCode(parsed.getErrorCode());
                            if (response.getRetryAfterSeconds() == null) {
                                response.setRetryAfterSeconds(parsed.getRetryAfterSeconds());
                            }
                        }
                    }
                }
            } catch (IOException ioException) {
                Log.e(TAG, "Ошибка чтения тела HTTP-ошибки чата", ioException);
            }

            if (response.getMessage() == null || response.getMessage().isEmpty()) {
                response.setMessage(httpException.message());
            }
            return response;
        }

        response.setMessage(throwable.getMessage());
        return response;
    }
}

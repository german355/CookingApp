package com.example.cooking.data.repositories;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import com.example.cooking.network.models.chat.ChatSessionResponse;
import com.example.cooking.network.models.chat.ChatMessageRequest;
import com.example.cooking.network.models.chat.ChatMessageResponse;
import com.example.cooking.network.models.chat.ChatHistoryResponse;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Репозиторий для AI-чата.
 * Инкапсулирует сетевые вызовы к ApiService для чата.
 */
public class ChatRepository extends NetworkRepository {
    public ChatRepository(Context context) {
        super(context);
    }

    public LiveData<ChatSessionResponse> startChatSession() {
        MutableLiveData<ChatSessionResponse> result = new MutableLiveData<>();
        apiService.startChatSession().enqueue(new Callback<ChatSessionResponse>() {
            @Override public void onResponse(Call<ChatSessionResponse> call, Response<ChatSessionResponse> response) { result.setValue(response.body()); }
            @Override public void onFailure(Call<ChatSessionResponse> call, Throwable t) { result.setValue(null); }
        });
        return result;
    }

    public LiveData<ChatMessageResponse> sendChatMessage(String message) {
        MutableLiveData<ChatMessageResponse> result = new MutableLiveData<>();
        ChatMessageRequest request = new ChatMessageRequest(message);
        apiService.sendChatMessage(request).enqueue(new Callback<ChatMessageResponse>() {
            @Override public void onResponse(Call<ChatMessageResponse> call, Response<ChatMessageResponse> response) { result.setValue(response.body()); }
            @Override public void onFailure(Call<ChatMessageResponse> call, Throwable t) { result.setValue(null); }
        });
        return result;
    }

    public LiveData<ChatHistoryResponse> getChatHistory() {
        MutableLiveData<ChatHistoryResponse> result = new MutableLiveData<>();
        apiService.getChatHistory().enqueue(new Callback<ChatHistoryResponse>() {
            @Override public void onResponse(Call<ChatHistoryResponse> call, Response<ChatHistoryResponse> response) { result.setValue(response.body()); }
            @Override public void onFailure(Call<ChatHistoryResponse> call, Throwable t) { result.setValue(null); }
        });
        return result;
    }
}

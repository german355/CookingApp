package com.example.cooking.data.repositories;

import android.content.Context;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.LiveDataReactiveStreams;
import com.example.cooking.network.models.chat.ChatSessionResponse;
import com.example.cooking.network.models.chat.ChatMessageRequest;
import com.example.cooking.network.models.chat.ChatMessageResponse;
import com.example.cooking.network.models.chat.ChatHistoryResponse;

import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * Репозиторий для AI-чата.
 * Инкапсулирует сетевые вызовы к ApiService для чата.
 */
public class ChatRepository extends NetworkRepository {
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

    public LiveData<ChatMessageResponse> sendChatMessage(String message) {
        ChatMessageRequest request = new ChatMessageRequest(message);
        return LiveDataReactiveStreams.fromPublisher(
            apiService.sendChatMessage(request)
                .toFlowable()
                .subscribeOn(Schedulers.io())
                .onErrorReturnItem(new ChatMessageResponse())
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
}

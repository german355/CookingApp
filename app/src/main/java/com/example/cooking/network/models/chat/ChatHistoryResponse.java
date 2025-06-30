package com.example.cooking.network.models.chat;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * DTO для получения истории сообщений AI-чата
 */
public class ChatHistoryResponse {

    @SerializedName("messages")
    private List<ChatMessage> messages;

    @SerializedName("has_more")
    private boolean hasMore;

    @SerializedName("message_count")
    private int messageCount;

    public List<ChatMessage> getMessages() {
        return messages;
    }


    public int getMessageCount() {
        return messageCount;
    }
}

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

    public void setMessages(List<ChatMessage> messages) {
        this.messages = messages;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    public int getMessageCount() {
        return messageCount;
    }

    public void setMessageCount(int messageCount) {
        this.messageCount = messageCount;
    }
}

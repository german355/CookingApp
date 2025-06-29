package com.example.cooking.network.models.chat;

import com.example.cooking.network.models.BaseApiResponse;
import com.google.gson.annotations.SerializedName;

/**
 * DTO для ответа при запуске сессии AI-чата.
 */
public class ChatSessionResponse extends BaseApiResponse {
    @SerializedName("sessionId")
    private String sessionId;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }
}

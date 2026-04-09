package com.example.cooking.network.models;

import com.google.gson.annotations.SerializedName;

/**
 * Базовый класс для всех API-ответов
 * Содержит общие поля, присутствующие в большинстве ответов от сервера
 */
public class BaseApiResponse {
    
    @SerializedName("success")
    private boolean success;

    @SerializedName("message")
    private String message;

    @SerializedName("status")
    private String status;

    @SerializedName("error_code")
    private String errorCode;

    @SerializedName("retry_after_seconds")
    private Integer retryAfterSeconds;

    /**
     * Проверяет успешность выполнения запроса
     */
    public boolean isSuccess() {
        if (status != null) {
            return "success".equalsIgnoreCase(status) || "ok".equalsIgnoreCase(status);
        }
        return success;
    }

    /**
     * Получает сообщение от сервера
     */
    public String getMessage() {
        return message;
    }

    /**
     * Получает статус ответа
     * @return статус ответа
     */
    public String getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Integer getRetryAfterSeconds() {
        return retryAfterSeconds;
    }

    /**
     * Устанавливает флаг успешности запроса
     * @param success флаг успешности
     */
    public void setSuccess(boolean success) {
        this.success = success;
    }

    /**
     * Устанавливает сообщение ответа
     * @param message сообщение
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * Устанавливает статус ответа
     * @param status статус
     */
    public void setStatus(String status) {
        this.status = status;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public void setRetryAfterSeconds(Integer retryAfterSeconds) {
        this.retryAfterSeconds = retryAfterSeconds;
    }
} 

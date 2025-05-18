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

    /**
     * Проверяет успешность выполнения запроса
     * @return true если запрос выполнен успешно
     */
    public boolean isSuccess() {
        // Если поле status определено, используем его, иначе используем поле success
        if (status != null) {
            return "success".equalsIgnoreCase(status) || "ok".equalsIgnoreCase(status);
        }
        return success;
    }

    /**
     * Получает сообщение от сервера
     * @return сообщение с описанием результата запроса
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
} 
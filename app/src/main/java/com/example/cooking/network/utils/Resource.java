package com.example.cooking.network.utils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Обобщенный класс-обёртка для представления состояний ответов API
 * Используется для передачи данных между слоями приложения с учетом их статуса
 * @param <T> тип данных внутри ресурса
 */
public class Resource<T> {
    
    /**
     * Перечисление для статусов обработки сетевых запросов
     */
    public enum Status {
        /**
         * Статус "Загрузка" - запрос выполняется
         */
        LOADING,
        
        /**
         * Статус "Успех" - запрос выполнен успешно
         */
        SUCCESS,
        
        /**
         * Статус "Ошибка" - произошла ошибка при выполнении запроса
         */
        ERROR
    }
    
    @NonNull
    private final Status status;
    
    @Nullable
    private final T data;
    
    @Nullable
    private final String message;

    private Resource(@NonNull Status status, @Nullable T data, @Nullable String message) {
        this.status = status;
        this.data = data;
        this.message = message;
    }

    /**
     * Создает ресурс в состоянии SUCCESS (Успех)
     * @param data данные для передачи
     * @param <T> тип данных
     * @return ресурс с данными в успешном состоянии
     */
    public static <T> Resource<T> success(@Nullable T data) {
        return new Resource<>(Status.SUCCESS, data, null);
    }

    /**
     * Создает ресурс в состоянии ERROR (Ошибка)
     * @param message сообщение об ошибке
     * @param data данные (могут быть null)
     * @param <T> тип данных
     * @return ресурс с сообщением об ошибке
     */
    public static <T> Resource<T> error(String message, @Nullable T data) {
        return new Resource<>(Status.ERROR, data, message);
    }

    /**
     * Создает ресурс в состоянии LOADING (Загрузка)
     * @param data данные (могут быть null, если загрузка только началась)
     * @param <T> тип данных
     * @return ресурс в состоянии загрузки
     */
    public static <T> Resource<T> loading(@Nullable T data) {
        return new Resource<>(Status.LOADING, data, null);
    }

    /**
     * Получает статус ресурса
     * @return статус ресурса
     */
    @NonNull
    public Status getStatus() {
        return status;
    }

    /**
     * Получает данные ресурса
     * @return данные или null
     */
    @Nullable
    public T getData() {
        return data;
    }

    /**
     * Получает сообщение (обычно используется для ошибок)
     * @return сообщение или null
     */
    @Nullable
    public String getMessage() {
        return message;
    }
    
    /**
     * Проверяет, находится ли ресурс в состоянии загрузки
     * @return true, если ресурс в состоянии загрузки
     */
    public boolean isLoading() {
        return status == Status.LOADING;
    }
    
    /**
     * Проверяет, находится ли ресурс в состоянии успешного выполнения
     * @return true, если ресурс в состоянии успеха
     */
    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }
    
    /**
     * Проверяет, находится ли ресурс в состоянии ошибки
     * @return true, если ресурс в состоянии ошибки
     */
    public boolean isError() {
        return status == Status.ERROR;
    }
} 
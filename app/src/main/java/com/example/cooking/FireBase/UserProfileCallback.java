package com.example.cooking.FireBase;

/**
 * Интерфейс обратного вызова для операций с профилем пользователя
 */
public interface UserProfileCallback {
    /**
     * Вызывается при успешном выполнении операции
     */
    void onSuccess();

    /**
     * Вызывается при ошибке выполнения операции
     * @param exception исключение, содержащее информацию об ошибке
     */
    void onError(Exception exception);
} 
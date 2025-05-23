package com.example.cooking.FireBase;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.PhoneAuthProvider;

/**
 * Интерфейс обратного вызова для операций аутентификации по телефону
 */
public interface PhoneAuthCallback {
    /**
     * Вызывается при успешной верификации и авторизации
     * @param user авторизованный пользователь
     */
    void onVerificationComplete(FirebaseUser user);

    /**
     * Вызывается при ошибке верификации
     * @param exception исключение с информацией об ошибке
     */
    void onVerificationFailed(Exception exception);

    /**
     * Вызывается, когда SMS-код отправлен на указанный номер
     * @param verificationId идентификатор верификации для последующей проверки кода
     * @param token токен для повторной отправки кода, если потребуется
     */
    void onCodeSent(String verificationId, PhoneAuthProvider.ForceResendingToken token);
} 
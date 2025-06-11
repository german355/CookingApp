package com.example.cooking.ui.viewmodels;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cooking.domain.usecases.ProfileUseCase;

/**
 * ViewModel для экрана профиля пользователя
 */
public class ProfileViewModel extends AndroidViewModel {
    private final ProfileUseCase profileUseCase;

    // LiveData для состояний UI
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isAuthenticated = new MutableLiveData<>(false);
    private final MutableLiveData<Boolean> operationSuccess = new MutableLiveData<>();

    // LiveData для данных пользователя
    private final MutableLiveData<String> displayName = new MutableLiveData<>();
    private final MutableLiveData<String> email = new MutableLiveData<>();

    public ProfileViewModel(@NonNull Application application) {
        super(application);
        profileUseCase = new ProfileUseCase(application);

        // Проверяем текущее состояние аутентификации
        profileUseCase.checkAuthenticationState(isAuthenticated, displayName, email);
    }

    /**
     * Обновляет отображаемое имя пользователя
     * 
     * @param newName новое имя пользователя
     */
    public void updateDisplayName(String newName) {
        profileUseCase.updateDisplayName(newName,
            isLoading, errorMessage, operationSuccess, displayName
        );
    }

    /**
     * Обновляет пароль пользователя
     * 
     * @param currentPassword текущий пароль
     * @param newPassword     новый пароль
     */
    public void updatePassword(String currentPassword, String newPassword) {
        profileUseCase.updatePassword(currentPassword, newPassword,
            isLoading, errorMessage, operationSuccess
        );
    }

    /**
     * Удаляет аккаунт пользователя
     * 
     * @param password текущий пароль для подтверждения
     */
    public void deleteAccount(String password) {
        profileUseCase.deleteAccount(password,
            isLoading, errorMessage, isAuthenticated,
            displayName, email, operationSuccess
        );
    }

    /**
     * Выход пользователя из системы
     */
    public void signOut() {
        profileUseCase.signOut(
            isLoading, isAuthenticated, displayName, email, operationSuccess
        );
    }

    /**
     * Обработка результата входа через Google
     */
    public void handleGoogleSignInResult(Intent data) {
        profileUseCase.handleGoogleSignInResult(
            data, isLoading, isAuthenticated, displayName, email,
            operationSuccess, errorMessage
        );
    }

    /**
     * Очищает сообщение об ошибке
     */
    public void clearErrorMessage() {
        errorMessage.setValue("");
    }

    /**
     * Очищает статус успешной операции
     */
    public void clearOperationSuccess() {
        operationSuccess.setValue(false);
    }

    /**
     * Геттер для LiveData состояния загрузки
     */
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    /**
     * Геттер для LiveData сообщения об ошибке
     */
    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    /**
     * Геттер для LiveData состояния аутентификации
     */
    public LiveData<Boolean> getIsAuthenticated() {
        return isAuthenticated;
    }

    /**
     * Геттер для LiveData успешного выполнения операции
     */
    public LiveData<Boolean> getOperationSuccess() {
        return operationSuccess;
    }

    /**
     * Геттер для LiveData отображаемого имени пользователя
     */
    public LiveData<String> getDisplayName() {
        return displayName;
    }

    /**
     * Геттер для LiveData email пользователя
     */
    public LiveData<String> getEmail() {
        return email;
    }

    /**
     * Запуск процесса входа через Google
     */
    public void signInWithGoogle(Activity activity) {
        profileUseCase.signInWithGoogle(activity);
    }

    @Override
    protected void onCleared() {
        profileUseCase.clear();
        super.onCleared();
    }
}
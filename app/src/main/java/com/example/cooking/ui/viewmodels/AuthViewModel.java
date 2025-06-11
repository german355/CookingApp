package com.example.cooking.ui.viewmodels;

import android.app.Application;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;
import android.util.Pair;
import android.util.Patterns;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cooking.domain.usecases.AuthUseCase;
import com.example.cooking.data.models.ApiResponse;

/**
 * ViewModel для управления аутентификацией пользователя
 */
public class AuthViewModel extends AndroidViewModel {
    private static final String TAG = "AuthViewModel";

    private final AuthUseCase authUseCase;

    // LiveData для состояний UI
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isAuthenticated = new MutableLiveData<>(false);

    // LiveData для данных пользователя
    private final MutableLiveData<String> displayName = new MutableLiveData<>();
    private final MutableLiveData<String> email = new MutableLiveData<>();
    private final MutableLiveData<Integer> permission = new MutableLiveData<>(1);

    // Валидация форм
    private boolean isEmailValid = false;
    private boolean isPasswordValid = false;
    private boolean isNameValid = false;
    private boolean isPasswordConfirmValid = false;

    // Временные данные регистрации
    private String pendingUsername;
    private String pendingEmail;
    private int pendingPermissionLevel = 1;

    public AuthViewModel(@NonNull Application application) {
        super(application);
        authUseCase = new AuthUseCase(application);
        checkAuthenticationState();
    }

    /**
     * Проверяет текущее состояние аутентификации пользователя
     */
    private void checkAuthenticationState() {
        authUseCase.checkAuthenticationState(isAuthenticated, displayName, email, permission);
    }

    /**
     * Выполняет вход с использованием email и пароля
     */
    public void signInWithEmailPassword(String email, String password) {
        if (!validateEmail(email) || !validatePassword(password)) {
            return;
        }

        authUseCase.signInWithEmailPassword(
            email, password,
            isLoading, isAuthenticated, displayName, this.email, permission,
            errorMessage
        );
    }

    /**
     * Регистрирует нового пользователя
     */
    public void registerUser(String email, String password, String username) {
        if (!validateEmail(email) || !validatePassword(password) || !validateName(username)) {
            return;
        }

        authUseCase.registerUser(
            email, password, username, pendingPermissionLevel,
            isLoading, errorMessage, isAuthenticated
        );
    }

    /**
     * Выполняет выход пользователя
     */
    public void signOut() {
        authUseCase.signOut(isLoading, isAuthenticated);
    }

    /**
     * Обрабатывает результат входа через Google
     * 
     * @param requestCode код запроса
     * @param resultCode  код результата
     * @param data        данные Intent
     */
    public void handleGoogleSignInResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != AuthUseCase.RC_SIGN_IN) {
            isLoading.setValue(false);
            return;
        }

        authUseCase.handleGoogleSignInResult(
            data,
            isLoading, isAuthenticated, displayName, this.email, permission,
            errorMessage
        );
    }

    /**
     * Валидация email
     */
    public boolean validateEmail(String emailValue) {
        if (TextUtils.isEmpty(emailValue)) {
            errorMessage.setValue("Email не может быть пустым");
            isEmailValid = false;
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(emailValue).matches()) {
            errorMessage.setValue("Неверный формат email");
            isEmailValid = false;
            return false;
        }

        isEmailValid = true;
        return true;
    }

    /**
     * Валидация пароля
     */
    public boolean validatePassword(String passwordValue) {
        if (TextUtils.isEmpty(passwordValue)) {
            errorMessage.setValue("Пароль не может быть пустым");
            isPasswordValid = false;
            return false;
        }

        if (passwordValue.length() < 6) {
            errorMessage.setValue("Пароль должен содержать не менее 6 символов");
            isPasswordValid = false;
            return false;
        }

        isPasswordValid = true;
        return true;
    }

    /**
     * Валидация имени пользователя
     */
    public boolean validateName(String nameValue) {
        if (TextUtils.isEmpty(nameValue)) {
            errorMessage.setValue("Имя не может быть пустым");
            isNameValid = false;
            return false;
        }

        if (nameValue.length() < 2) {
            errorMessage.setValue("Имя должно содержать не менее 2 символов");
            isNameValid = false;
            return false;
        }

        isNameValid = true;
        return true;
    }

    /**
     * Валидация подтверждения пароля
     */
    public boolean validatePasswordConfirmation(String password, String confirmPassword) {
        if (TextUtils.isEmpty(confirmPassword)) {
            errorMessage.setValue("Подтверждение пароля не может быть пустым");
            isPasswordConfirmValid = false;
            return false;
        }

        if (!password.equals(confirmPassword)) {
            errorMessage.setValue("Пароли не совпадают");
            isPasswordConfirmValid = false;
            return false;
        }

        isPasswordConfirmValid = true;
        return true;
    }

    /**
     * Проверяет все входные данные формы регистрации
     */
    public boolean validateAllRegistrationInputs(String name, String email, String password, String confirmPassword) {
        boolean nameValid = validateName(name);
        boolean emailValid = validateEmail(email);
        boolean passwordValid = validatePassword(password);
        boolean confirmValid = validatePasswordConfirmation(password, confirmPassword);

        return nameValid && emailValid && passwordValid && confirmValid;
    }

    /**
     * Проверяет все поля для входа на валидность
     */
    public boolean validateAllLoginInputs(String email, String password) {
        return validateEmail(email) && validatePassword(password);
    }

    /**
     * Проверяет совпадение паролей
     * 
     * @param password        пароль
     * @param confirmPassword подтверждение пароля
     * @return true, если пароли совпадают
     */
    public boolean doPasswordsMatch(String password, String confirmPassword) {
        if (confirmPassword == null || confirmPassword.isEmpty()) {
            errorMessage.setValue("Подтверждение пароля не может быть пустым");
            isPasswordConfirmValid = false;
            return false;
        }

        boolean match = password.equals(confirmPassword);

        if (!match) {
            errorMessage.setValue("Пароли не совпадают");
            isPasswordConfirmValid = false;
        } else {
            isPasswordConfirmValid = true;
        }

        return match;
    }

    /**
     * Геттеры для LiveData
     */
    public LiveData<Boolean> getIsLoading() {
        return isLoading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getIsAuthenticated() {
        return isAuthenticated;
    }

    public LiveData<String> getDisplayName() {
        return displayName;
    }

    public LiveData<String> getEmail() {
        return email;
    }

    public LiveData<Integer> getPermission() {
        return permission;
    }

    /**
     * Очищает сообщение об ошибке
     */
    public void clearErrorMessage() {
        errorMessage.setValue("");
    }

    /**
     * Проверяет, авторизован ли пользователь
     */
    public boolean isUserLoggedIn() {
        return authUseCase.isUserLoggedIn();
    }

    /**
     * Инициализирует Google Sign-In
     * 
     * @param webClientId идентификатор из google-services.json
     */
    public void initGoogleSignIn(String webClientId) {
        authUseCase.initGoogleSignIn(getApplication().getApplicationContext(), webClientId);
    }

    /**
     * Запускает процесс входа через Google
     * 
     * @param activity активность для запуска Intent
     */
    public void signInWithGoogle(android.app.Activity activity) {
        isLoading.setValue(true);
        try {
            authUseCase.signInWithGoogle(activity);
        } catch (Exception e) {
            errorMessage.setValue(e.getMessage());
            isLoading.setValue(false);
        }
    }

    @Override
    protected void onCleared() {
        authUseCase.clear();
        super.onCleared();
    }
}
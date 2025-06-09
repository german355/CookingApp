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

import com.example.cooking.auth.FirebaseAuthManager;
import com.example.cooking.data.repositories.LikedRecipesRepository;
import com.example.cooking.utils.MySharedPreferences;
import com.example.cooking.data.models.ApiResponse;
import com.example.cooking.network.services.UserService;
import com.google.firebase.auth.FirebaseUser;

import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * ViewModel для управления аутентификацией пользователя
 */
public class AuthViewModel extends AndroidViewModel {
    private static final String TAG = "AuthViewModel";

    private final FirebaseAuthManager authManager;
    private final MySharedPreferences preferences;
    private final UserService userService;
    private final LikedRecipesRepository likedRecipesRepository;
    private final CompositeDisposable disposables = new CompositeDisposable();

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
        authManager = new FirebaseAuthManager(application);
        preferences = new MySharedPreferences(application);
        userService = new UserService(application);
        likedRecipesRepository = new LikedRecipesRepository(application);

        checkAuthenticationState();
    }

    /**
     * Проверяет текущее состояние аутентификации пользователя
     */
    private void checkAuthenticationState() {
        FirebaseUser currentUser = authManager.getCurrentUser();
        if (currentUser != null) {
            isAuthenticated.setValue(true);

            // Загружаем данные пользователя из SharedPreferences
            displayName.setValue(preferences.getString("username", ""));
            email.setValue(preferences.getString("email", ""));
            permission.setValue(preferences.getInt("permission", 1));
        } else {
            isAuthenticated.setValue(false);
        }
    }

    /**
     * Выполняет вход с использованием email и пароля
     */
    public void signInWithEmailPassword(String email, String password) {
        if (!validateEmail(email) || !validatePassword(password)) {
            return;
        }

        isLoading.setValue(true);

        disposables.add(
            authManager.signInWithEmailAndPasswordSingle(email, password)
                .flatMap(user -> {
                    likedRecipesRepository.syncLikedRecipesFromServer();
                    return userService.loginFirebaseUserSingle()
                        .map(resp -> new Pair<>(user, resp));
                })
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    pair -> {
                        FirebaseUser user = pair.first;
                        ApiResponse resp = pair.second;
                        String internal = resp.getUserId();
                        int perm = resp.getPermission();
                        if (internal == null || internal.isEmpty()) {
                            internal = user.getUid();
                        }
                        if (perm == 0) {
                            perm = 1;
                        }
                        saveUserData(user, internal, perm);
                        isLoading.postValue(false);
                        isAuthenticated.postValue(true);
                    },
                    err -> {
                        errorMessage.setValue(err.getMessage());
                        isLoading.setValue(false);
                    }
                )
        );
    }

    /**
     * Регистрирует нового пользователя
     */
    public void registerUser(String email, String password, String username) {
        if (!validateEmail(email) || !validatePassword(password) || !validateName(username)) {
            return;
        }

        isLoading.setValue(true);
        pendingEmail = email;
        pendingUsername = username;
        pendingPermissionLevel = 1;

        disposables.add(
            authManager.registerWithEmailAndPasswordSingle(email, password)
                .flatMap(user -> authManager.updateUserDisplayNameCompletable(user, username).andThen(Single.just(user)))
                .flatMap(user -> userService.saveUserSingle(user.getUid(), username, pendingEmail, pendingPermissionLevel)
                    .map(resp -> new Pair<>(user, resp)))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    pair -> {
                        FirebaseUser user = pair.first;
                        ApiResponse resp = pair.second;
                        String internal = resp.getUserId();
                        int perm = resp.getPermission();
                        if (internal == null || internal.isEmpty()) {
                            internal = user.getUid();
                        }
                        if (perm == 0) {
                            perm = pendingPermissionLevel;
                        }
                        saveUserData(user, internal, perm);
                        isLoading.postValue(false);
                        isAuthenticated.postValue(true);
                    },
                    err -> {
                        isLoading.setValue(false);
                        errorMessage.setValue(err.getMessage());
                        Log.e(TAG, "registerUser Rx error: " + err.getMessage());
                    }
                )
        );
    }

    /**
     * Выполняет выход пользователя
     */
    public void signOut() {
        authManager.signOut();

        // Очищаем данные пользователя из SharedPreferences
        preferences.putString("userId", "0");
        preferences.putString("username", "");
        preferences.putString("email", "");
        preferences.putInt("permission", 1);

        // Обновляем LiveData
        isAuthenticated.setValue(false);
        displayName.setValue("");
        email.setValue("");
        permission.setValue(1);
    }

    /**
     * Сохраняет данные пользователя в SharedPreferences
     * 
     * @param user            Firebase пользователь
     * @param internalUserId  Внутренний ID пользователя, полученный от нашего
     *                        сервера
     * @param permissionLevel Уровень прав пользователя, полученный от нашего
     *                        сервера
     */
    private void saveUserData(FirebaseUser user, String internalUserId, int permissionLevel) {
        String userName = user.getDisplayName() != null ? user.getDisplayName() : "";
        String userEmail = user.getEmail() != null ? user.getEmail() : "";

        // Сохраняем ВНУТРЕННИЙ ID в SharedPreferences
        Log.d(TAG, "Сохранение userId в SharedPreferences: " + internalUserId);
        preferences.putString("userId", internalUserId);
        preferences.putString("username", userName);
        preferences.putString("email", userEmail);
        // Сохраняем УРОВЕНЬ ПРАВ
        Log.d(TAG, "Сохранение permission в SharedPreferences: " + permissionLevel);
        preferences.putInt("permission", permissionLevel);

        // Обновляем LiveData
        displayName.postValue(userName);
        email.postValue(userEmail);
        permission.postValue(permissionLevel);
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
        return authManager.getCurrentUser() != null;
    }

    /**
     * Инициализирует Google Sign-In
     * 
     * @param webClientId идентификатор из google-services.json
     */
    public void initGoogleSignIn(String webClientId) {
        authManager.initGoogleSignIn(getApplication().getApplicationContext(), webClientId);
    }

    /**
     * Запускает процесс входа через Google
     * 
     * @param activity активность для запуска Intent
     */
    public void signInWithGoogle(android.app.Activity activity) {
        isLoading.setValue(true);
        try {
            authManager.signInWithGoogle(activity);
        } catch (Exception e) {
            errorMessage.setValue(e.getMessage());
            isLoading.setValue(false);
        }
    }

    /**
     * Обрабатывает результат входа через Google
     * 
     * @param requestCode код запроса
     * @param resultCode  код результата
     * @param data        данные Intent
     */
    public void handleGoogleSignInResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != FirebaseAuthManager.RC_SIGN_IN) {
            isLoading.setValue(false);
            return;
        }
        disposables.add(
            // Получаем FirebaseUser через Rx
            authManager.handleGoogleSignInResultSingle(data)
                .subscribeOn(Schedulers.io())
                .flatMap(user ->
                    // Пытаемся логиниться на сервере
                    userService.loginFirebaseUserSingle()
                        .onErrorResumeNext(err ->
                            // При провале регистрация пробуем регистрировать
                            userService.registerFirebaseUserSingle(user.getEmail(), user.getDisplayName(), user.getUid())
                                .flatMap(reg -> userService.loginFirebaseUserSingle())
                        )
                        // Возвращаем пару (user, serverResponse)
                        .map(resp -> new android.util.Pair<>(user, resp))
                )
                .observeOn(AndroidSchedulers.mainThread())
                .doOnSubscribe(d -> isLoading.setValue(true))
                .doFinally(() -> isLoading.setValue(false))
                .subscribe(
                    pair -> {
                        FirebaseUser user = pair.first;
                        ApiResponse resp = pair.second;
                        String internalId = resp.getUserId();
                        int perm = resp.getPermission();
                        if (internalId == null || internalId.isEmpty()) internalId = user.getUid();
                        if (perm == 0) perm = 1;
                        saveUserData(user, internalId, perm);
                        isAuthenticated.setValue(true);
                        likedRecipesRepository.syncLikedRecipesFromServer();
                        Log.d(TAG, "Google Sign-In: успешный вход, ID=" + internalId + ", Права=" + perm);
                    },
                    t -> {
                        // Не удалось ни логин, ни регистрация
                        FirebaseUser cur = authManager.getCurrentUser();
                        if (cur != null) {
                            saveUserData(cur, cur.getUid(), 1);
                            isAuthenticated.setValue(true);
                        }
                        errorMessage.setValue("Ошибка авторизации: " + t.getMessage());
                    }
                )
        );
    }

    @Override
    protected void onCleared() {
        disposables.clear();
        super.onCleared();
    }
}
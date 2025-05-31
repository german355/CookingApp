package com.example.cooking.ui.viewmodels;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cooking.data.database.AppDatabase;
import com.example.cooking.data.database.LikedRecipeDao;
import com.example.cooking.data.models.ApiResponse;
import com.example.cooking.network.services.UserService;
import com.example.cooking.auth.FirebaseAuthManager;
import com.example.cooking.utils.MySharedPreferences;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * ViewModel для экрана профиля пользователя
 */
public class ProfileViewModel extends AndroidViewModel {
    private static final String TAG = "ProfileViewModel";
    private final LikedRecipeDao likedRecipeDao;
    private final FirebaseAuthManager authManager;
    private final MySharedPreferences preferences;
    private final UserService userService;
    private final ExecutorService databaseExecutor;

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
        authManager = new FirebaseAuthManager(application);
        preferences = new MySharedPreferences(application);
        userService = new UserService(application);
        likedRecipeDao = AppDatabase.getInstance(application).likedRecipeDao();
        databaseExecutor = Executors.newSingleThreadExecutor();

        // Проверяем текущее состояние аутентификации
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
            String name = currentUser.getDisplayName();
            String userEmail = currentUser.getEmail();

            // Если данные отсутствуют в Firebase, пробуем загрузить из SharedPreferences
            if (name == null || name.isEmpty()) {
                name = preferences.getString("username", "");
            }

            if (userEmail == null || userEmail.isEmpty()) {
                userEmail = preferences.getString("email", "");
            }

            displayName.setValue(name);
            email.setValue(userEmail);
        } else {
            isAuthenticated.setValue(false);
        }
    }

    /**
     * Обновляет отображаемое имя пользователя
     * 
     * @param newName новое имя пользователя
     */
    public void updateDisplayName(String newName) {
        if (newName == null || newName.trim().isEmpty()) {
            errorMessage.setValue("Имя не может быть пустым");
            return;
        }

        isLoading.setValue(true);

        FirebaseUser user = authManager.getCurrentUser();
        if (user == null) {
            isLoading.setValue(false);
            errorMessage.setValue("Пользователь не авторизован");
            return;
        }

        // Обновляем имя в профиле Firebase
        authManager.updateUserDisplayName(user, newName, new FirebaseAuthManager.UpdateProfileCallback() {
            @Override
            public void onSuccess() {
                // Обновляем данные в SharedPreferences
                preferences.putString("username", newName);

                // Обновляем LiveData
                displayName.postValue(newName);
                isLoading.postValue(false);
                operationSuccess.postValue(true);

                // Обновляем имя пользователя на сервере
                userService.updateUserName(user.getUid(), newName, new UserService.UserCallback() {
                    @Override
                    public void onSuccess(ApiResponse response) {
                        Log.d(TAG, "Имя пользователя успешно обновлено на сервере");
                    }

                    @Override
                    public void onFailure(String errorMsg) {
                        Log.e(TAG, "Ошибка при обновлении имени пользователя");
                    }
                });
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue("Ошибка при обновлении имени");
            }
        });
    }

    /**
     * Обновляет пароль пользователя
     * 
     * @param currentPassword текущий пароль
     * @param newPassword     новый пароль
     */
    public void updatePassword(String currentPassword, String newPassword) {
        if (currentPassword == null || currentPassword.isEmpty()) {
            errorMessage.setValue("Текущий пароль не может быть пустым");
            return;
        }

        if (newPassword == null || newPassword.isEmpty()) {
            errorMessage.setValue("Новый пароль не может быть пустым");
            return;
        }

        if (newPassword.length() < 6) {
            errorMessage.setValue("Новый пароль должен содержать не менее 6 символов");
            return;
        }

        isLoading.setValue(true);

        FirebaseUser user = authManager.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            isLoading.setValue(false);
            errorMessage.setValue("Пользователь не авторизован");
            return;
        }

        // Сначала повторно аутентифицируем пользователя
        authManager.reauthenticate(user, user.getEmail(), currentPassword, new FirebaseAuthManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                // После успешной повторной аутентификации обновляем пароль
                authManager.updateUserPassword(user, newPassword, new FirebaseAuthManager.UpdateProfileCallback() {
                    @Override
                    public void onSuccess() {
                        isLoading.postValue(false);
                        operationSuccess.postValue(true);
                    }

                    @Override
                    public void onError(String message) {
                        isLoading.postValue(false);
                        errorMessage.postValue("Ошибка при обновлении пароля");
                    }
                });
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue("Ошибка аутентификации");
            }
        });
    }

    /**
     * Удаляет аккаунт пользователя
     * 
     * @param password текущий пароль для подтверждения
     */
    public void deleteAccount(String password) {
        if (password == null || password.isEmpty()) {
            errorMessage.setValue("Пароль не может быть пустым");
            return;
        }

        isLoading.setValue(true);

        FirebaseUser user = authManager.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            isLoading.setValue(false);
            errorMessage.setValue("Пользователь не авторизован");
            return;
        }

        // Сначала повторно аутентифицируем пользователя
        authManager.reauthenticate(user, user.getEmail(), password, new FirebaseAuthManager.AuthCallback() {
            @Override
            public void onSuccess(FirebaseUser user) {
                // После успешной повторной аутентификации удаляем аккаунт
                user.delete()
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful()) {
                                // Очищаем данные в SharedPreferences
                                preferences.putString("userId", "");
                                preferences.putString("username", "");
                                preferences.putString("email", "");

                                // Обновляем LiveData
                                isAuthenticated.postValue(false);
                                displayName.postValue("");
                                email.postValue("");
                                isLoading.postValue(false);
                                operationSuccess.postValue(true);

                                // Удаляем данные пользователя с сервера
                                userService.deleteUser(user.getUid(), new UserService.UserCallback() {
                                    @Override
                                    public void onSuccess(ApiResponse response) {
                                        Log.d(TAG, "Данные пользователя успешно удалены с сервера");
                                    }

                                    @Override
                                    public void onFailure(String errorMsg) {
                                        Log.e(TAG, "Ошибка при удалении данных пользователя с сервера: " + errorMsg);
                                    }
                                });
                            } else {
                                isLoading.postValue(false);
                                errorMessage.postValue("Ошибка при удалении аккаунта: " +
                                        (task.getException() != null ? task.getException().getMessage()
                                                : "Неизвестная ошибка"));
                            }
                        });
            }

            @Override
            public void onError(String message) {
                isLoading.postValue(false);
                errorMessage.postValue("Ошибка аутентификации: " + message);
            }
        });
    }

    /**
     * Выход пользователя из системы
     */
    public void signOut() {
        try {
            Log.d(TAG, "Выполнение выхода пользователя");
            authManager.signOut();
            final String userId = preferences.getString("userId", "0");

            // Выполняем удаление в фоновом потоке
            databaseExecutor.execute(() -> {
                try {
                    Log.d(TAG, "Удаление лайков пользователя из БД в фоновом потоке userId: " + userId);
                    if (userId != null && !userId.equals("0")) {
                        // Очищаем лайки пользователя в локальной базе данных
                        likedRecipeDao.deleteAll();
                        Log.d(TAG, "Лайки успешно удалены для пользователя " + userId);
                    } else {
                         Log.w(TAG, "userId недействителен (" + userId + "), удаление лайков пропущено");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Ошибка при удалении лайков пользователя из БД", e);
                }
            });

            // Остальные операции выполняются в основном потоке сразу
            preferences.putString("userId", "0");
            preferences.putString("username", "");
            preferences.putString("email", "");
            preferences.putInt("permission", 1);
            Log.d(TAG, "Данные пользователя в SharedPreferences сброшены после выхода");

            isAuthenticated.postValue(false);
            Log.d(TAG, "Установлен статус isAuthenticated = false");

            displayName.postValue("");
            email.postValue("");

            Log.d(TAG, "Выставляем флаг успеха операции выхода");
            operationSuccess.postValue(true);

        } catch (Exception e) {
            Log.e(TAG, "Ошибка при выходе из системы (основной поток)", e);
            errorMessage.postValue("Произошла ошибка при выходе");
            operationSuccess.postValue(false);
        }
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

    @Override
    protected void onCleared() {
        super.onCleared();
        databaseExecutor.shutdown();
        Log.d(TAG, "ExecutorService остановлен в onCleared");
    }
}
package com.example.cooking.ui.viewmodels;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cooking.data.database.AppDatabase;
import com.example.cooking.data.database.LikedRecipeDao;
import com.example.cooking.data.database.RecipeDao;
import com.example.cooking.data.models.ApiResponse;
import com.example.cooking.network.services.UserService;
import com.example.cooking.auth.FirebaseAuthManager;
import com.example.cooking.utils.MySharedPreferences;
import com.google.firebase.auth.FirebaseUser;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;
import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;

/**
 * ViewModel для экрана профиля пользователя
 */
public class ProfileViewModel extends AndroidViewModel {
    private static final String TAG = "ProfileViewModel";
    private final LikedRecipeDao likedRecipeDao;
    private final RecipeDao recipeDao;
    private final FirebaseAuthManager authManager;
    private final MySharedPreferences preferences;
    private final UserService userService;
    private final ExecutorService databaseExecutor;
    private final CompositeDisposable disposables;

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
        recipeDao = AppDatabase.getInstance(application).recipeDao();
        databaseExecutor = Executors.newSingleThreadExecutor();
        disposables = new CompositeDisposable();

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
        FirebaseUser user = authManager.getCurrentUser();
        if (user == null) {
            errorMessage.setValue("Пользователь не авторизован");
            return;
        }
        disposables.add(
            authManager.updateUserDisplayNameCompletable(user, newName)
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(d -> isLoading.postValue(true))
                .observeOn(AndroidSchedulers.mainThread())
                .andThen(Completable.fromAction(() -> {
                    preferences.putString("username", newName);
                    displayName.postValue(newName);
                }))
                .andThen(userService.updateUserNameSingle(user.getUid(), newName).ignoreElement())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(() -> {
                    isLoading.postValue(false);
                    operationSuccess.postValue(true);
                })
                .subscribe(
                    () -> {},
                    t -> errorMessage.postValue("Ошибка при обновлении имени: " + t.getMessage())
                )
        );
    }

    /**
     * Обновляет пароль пользователя
     * 
     * @param currentPassword текущий пароль
     * @param newPassword     новый пароль
     */
    public void updatePassword(String currentPassword, String newPassword) {
        if (currentPassword == null || newPassword == null) {
            errorMessage.setValue("Пароли не могут быть пустыми");
            return;
        }
        FirebaseUser user = authManager.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            errorMessage.setValue("Пользователь не авторизован");
            return;
        }
        disposables.add(
            authManager.reauthenticateSingle(user, user.getEmail(), currentPassword)
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(d -> isLoading.postValue(true))
                .flatMapCompletable(u -> authManager.updateUserPasswordCompletable(u, newPassword))
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(() -> isLoading.postValue(false))
                .subscribe(
                    () -> operationSuccess.postValue(true),
                    t -> errorMessage.postValue("Ошибка при обновлении пароля: " + t.getMessage())
                )
        );
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
        FirebaseUser user = authManager.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            errorMessage.setValue("Пользователь не авторизован");
            return;
        }
        disposables.add(
            authManager.reauthenticateSingle(user, user.getEmail(), password)
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(d -> isLoading.postValue(true))
                .flatMapCompletable(u -> Completable.create(emitter -> {
                    u.delete().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) emitter.onComplete();
                        else emitter.onError(task.getException());
                    });
                }))
                .observeOn(AndroidSchedulers.mainThread())
                .andThen(Completable.fromAction(() -> {
                    preferences.putString("userId", "");
                    preferences.putString("username", "");
                    preferences.putString("email", "");
                    isAuthenticated.postValue(false);
                    displayName.postValue("");
                    email.postValue("");
                }))
                .andThen(userService.deleteUserSingle(user.getUid()).ignoreElement())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(() -> {
                    isLoading.postValue(false);
                    operationSuccess.postValue(true);
                })
                .subscribe(
                    () -> {},
                    t -> errorMessage.postValue("Ошибка при удалении аккаунта: " + t.getMessage())
                )
        );
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
                        recipeDao.clearAllLikeStatus();
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

    /**
     * Запуск процесса входа через Google
     */
    public void signInWithGoogle(Activity activity) {
        authManager.signInWithGoogle(activity);
    }

    /**
     * Обработка результата входа через Google через RxJava
     */
    public void handleGoogleSignInResult(Intent data) {
        isLoading.setValue(true);
        disposables.add(
            authManager.handleGoogleSignInResultSingle(data)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(
                    user -> {
                        isLoading.postValue(false);
                        isAuthenticated.postValue(true);
                        displayName.postValue(user.getDisplayName());
                        email.postValue(user.getEmail());
                        operationSuccess.postValue(true);
                    },
                    throwable -> {
                        isLoading.postValue(false);
                        errorMessage.postValue("Ошибка входа через Google: " + throwable.getMessage());
                    }
                )
        );
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        databaseExecutor.shutdown();
        disposables.clear();
        Log.d(TAG, "ExecutorService остановлен в onCleared");
    }
}
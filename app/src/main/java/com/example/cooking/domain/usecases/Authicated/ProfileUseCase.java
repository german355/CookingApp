package com.example.cooking.domain.usecases.Authicated;

import android.app.Application;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import androidx.lifecycle.MutableLiveData;

import com.example.cooking.data.database.AppDatabase;
import com.example.cooking.data.database.LikedRecipeDao;
import com.example.cooking.data.database.RecipeDao;
import com.example.cooking.data.repositories.LikedRecipesRepository;
import com.example.cooking.network.services.UserService;
import com.example.cooking.utils.MySharedPreferences;
import com.example.cooking.auth.FirebaseAuthManager;
import com.google.firebase.auth.FirebaseUser;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

/**
 * UseCase для профиля пользователя
 */
public class ProfileUseCase {
    private static final String TAG = "ProfileUseCase";
    private final Application application;
    private final FirebaseAuthManager authManager;
    private final MySharedPreferences preferences;
    private final UserService userService;
    private final LikedRecipeDao likedDao;
    private final RecipeDao recipeDao;
    private final CompositeDisposable disposables;

    public ProfileUseCase(Application application) {
        this.application = application;
        Context ctx = application.getApplicationContext();
        authManager = new FirebaseAuthManager(application);
        preferences = new MySharedPreferences(application);
        userService = new UserService(application);
        likedDao = AppDatabase.getInstance(ctx).likedRecipeDao();
        recipeDao = AppDatabase.getInstance(ctx).recipeDao();
        disposables = new CompositeDisposable();
    }

    public void checkAuthenticationState(
        MutableLiveData<Boolean> isAuthenticated,
        MutableLiveData<String> displayName,
        MutableLiveData<String> email
    ) {
        FirebaseUser user = authManager.getCurrentUser();
        if (user != null) {
            isAuthenticated.postValue(true);
            String name = user.getDisplayName();
            String mail = user.getEmail();
            if (name == null || name.isEmpty()) name = preferences.getString("username", "");
            if (mail == null || mail.isEmpty()) mail = preferences.getString("email", "");
            displayName.postValue(name);
            email.postValue(mail);
        } else {
            isAuthenticated.postValue(false);
        }
    }

    public void updateDisplayName(
        String newName,
        MutableLiveData<Boolean> isLoading,
        MutableLiveData<String> errorMessage,
        MutableLiveData<Boolean> operationSuccess,
        MutableLiveData<String> displayName
    ) {
        if (newName == null || newName.trim().isEmpty()) {
            errorMessage.postValue("Имя не может быть пустым");
            return;
        }
        FirebaseUser user = authManager.getCurrentUser();
        if (user == null) {
            errorMessage.postValue("Пользователь не авторизован");
            return;
        }
        disposables.add(
            authManager.updateUserDisplayNameCompletable(user, newName)
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(d -> isLoading.postValue(true))
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

    public void updatePassword(
        String currentPassword,
        String newPassword,
        MutableLiveData<Boolean> isLoading,
        MutableLiveData<String> errorMessage,
        MutableLiveData<Boolean> operationSuccess
    ) {
        if (currentPassword == null || newPassword == null) {
            errorMessage.postValue("Пароли не могут быть пустыми");
            return;
        }
        FirebaseUser user = authManager.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            errorMessage.postValue("Пользователь не авторизован");
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

    public void deleteAccount(
        String password,
        MutableLiveData<Boolean> isLoading,
        MutableLiveData<String> errorMessage,
        MutableLiveData<Boolean> isAuthenticated,
        MutableLiveData<String> displayName,
        MutableLiveData<String> email,
        MutableLiveData<Boolean> operationSuccess
    ) {
        if (password == null || password.isEmpty()) {
            errorMessage.postValue("Пароль не может быть пустым");
            return;
        }
        FirebaseUser user = authManager.getCurrentUser();
        if (user == null || user.getEmail() == null) {
            errorMessage.postValue("Пользователь не авторизован");
            return;
        }
        disposables.add(
            authManager.reauthenticateSingle(user, user.getEmail(), password)
                .subscribeOn(Schedulers.io())
                .doOnSubscribe(d -> isLoading.postValue(true))
                .flatMapCompletable(u -> Completable.create(emitter -> {
                    u.delete().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) emitter.onComplete(); else emitter.onError(task.getException());
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

    public void signOut(
        MutableLiveData<Boolean> isLoading,
        MutableLiveData<Boolean> isAuthenticated,
        MutableLiveData<String> displayName,
        MutableLiveData<String> email,
        MutableLiveData<Boolean> operationSuccess
    ) {
        isLoading.postValue(true);
        authManager.signOut();
        
        // Получаем userId ДО очистки настроек
        String userId = preferences.getString("userId", "0");
        
        if (userId != null && !userId.equals("0")) {
            // Создаем репозиторий лайков для очистки данных
            LikedRecipesRepository likedRepo = new LikedRecipesRepository(application);
            likedRepo.clearAllLikes();
        }
        
        // Очищаем настройки пользователя
        preferences.putString("userId", "0");
        preferences.putString("username", "");
        preferences.putString("email", "");
        preferences.putInt("permission", 1);
        
        isAuthenticated.postValue(false);
        displayName.postValue("");
        email.postValue("");
        operationSuccess.postValue(true);
        isLoading.postValue(false);
    }

    public void signInWithGoogle(Activity activity) {
        authManager.signInWithGoogle(activity);
    }

    public void handleGoogleSignInResult(
        Intent data,
        MutableLiveData<Boolean> isLoading,
        MutableLiveData<Boolean> isAuthenticated,
        MutableLiveData<String> displayName,
        MutableLiveData<String> email,
        MutableLiveData<Boolean> operationSuccess,
        MutableLiveData<String> errorMessage
    ) {
        isLoading.postValue(true);
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

    public void clear() {
        disposables.clear();
    }
}

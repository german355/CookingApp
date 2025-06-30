package com.example.cooking.domain.usecases.Authicated;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import androidx.lifecycle.MutableLiveData;
import android.util.Log;
import android.util.Pair;

import com.example.cooking.auth.FirebaseAuthManager;
import com.example.cooking.data.models.ApiResponse;
import com.example.cooking.data.repositories.LikedRecipesRepository;
import com.example.cooking.network.services.UserService;
import com.example.cooking.utils.MySharedPreferences;
import com.google.firebase.auth.FirebaseUser;

import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.disposables.CompositeDisposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers;

/**
 * UseCase для аутентификации и управления пользователем
 */
public class AuthUseCase {
    private static final String TAG = "AuthUseCase";
    private final FirebaseAuthManager authManager;
    private final UserService userService;
    private final LikedRecipesRepository likedRepo;
    private final MySharedPreferences preferences;
    private final CompositeDisposable disposables = new CompositeDisposable();

    public static final int RC_SIGN_IN = FirebaseAuthManager.RC_SIGN_IN;

    public AuthUseCase(Application application) {
        authManager = new FirebaseAuthManager(application);
        userService = new UserService(application);
        likedRepo = new LikedRecipesRepository(application);
        preferences = new MySharedPreferences(application);
    }

    public void signInWithEmailPassword(String email, String password,
            MutableLiveData<Boolean> isLoading,
            MutableLiveData<Boolean> isAuthenticated,
            MutableLiveData<String> displayName,
            MutableLiveData<String> emailLive,
            MutableLiveData<Integer> permission,
            MutableLiveData<String> errorMessage) {
        isLoading.postValue(true);
        disposables.add(
            authManager.signInWithEmailAndPasswordSingle(email, password)
                .flatMap(user -> userService.loginFirebaseUserSingle()
                    .map(resp -> new Pair<>(user, resp)))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(() -> isLoading.postValue(false))
                .subscribe(pair -> {
                    FirebaseUser user = pair.first;
                    ApiResponse resp = pair.second;
                    String internalId = resp.getUserId();
                    preferences.putString("userId", internalId != null && !internalId.isEmpty() ? internalId : user.getUid());
                    int perm = resp.getPermission();
                    if (internalId == null || internalId.isEmpty()) internalId = user.getUid();
                    preferences.putString("username", user.getDisplayName());
                    preferences.putString("email", user.getEmail());
                    preferences.putInt("permission", perm);
                    displayName.postValue(user.getDisplayName());
                    emailLive.postValue(user.getEmail());
                    permission.postValue(perm);
                    isAuthenticated.postValue(true);
                    likedRepo.syncLikedRecipesFromServer();
                }, t -> {
                    Log.e(TAG, "signIn error", t);
                    errorMessage.postValue("Ошибка авторизации: " + t.getMessage());
                })
        );
    }

    public void registerUser(String email, String password, String username, int permissionLevel,
            MutableLiveData<Boolean> isLoading,
            MutableLiveData<String> errorMessage,
            MutableLiveData<Boolean> isAuthenticated) {
        isLoading.postValue(true);
        disposables.add(
            authManager.registerWithEmailAndPasswordSingle(email, password)
                .flatMap(user -> authManager.updateUserDisplayNameCompletable(user, username).andThen(Single.just(user)))
                .flatMap(user -> userService.registerFirebaseUserSingle(user.getEmail(), username, user.getUid()))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(() -> isLoading.postValue(false))
                .subscribe(resp -> {
                    if (resp.isSuccess()) {
                        isAuthenticated.postValue(true);
                    } else {
                        errorMessage.postValue(resp.getMessage());
                    }
                }, t -> errorMessage.postValue(t.getMessage()))
        );
    }

    public void handleGoogleSignInResult(
            Intent data,
            MutableLiveData<Boolean> isLoading,
            MutableLiveData<Boolean> isAuthenticated,
            MutableLiveData<String> displayName,
            MutableLiveData<String> emailLive,
            MutableLiveData<Integer> permission,
            MutableLiveData<String> errorMessage) {
        isLoading.postValue(true);
        disposables.add(
            authManager.handleGoogleSignInResultSingle(data)
                .flatMap(user -> userService.loginFirebaseUserSingle()
                    .map(resp -> new Pair<>(user, resp)))
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .doFinally(() -> isLoading.postValue(false))
                .subscribe(pair -> {
                    FirebaseUser user = pair.first;
                    ApiResponse resp = pair.second;
                    String internalId = resp.getUserId();
                    preferences.putString("userId", internalId != null && !internalId.isEmpty() ? internalId : user.getUid());
                    int perm = resp.getPermission();
                    if (internalId == null || internalId.isEmpty()) internalId = user.getUid();
                    if (perm == 0) perm = 1;
                    preferences.putString("username", user.getDisplayName());
                    preferences.putString("email", user.getEmail());
                    preferences.putInt("permission", perm);
                    displayName.postValue(user.getDisplayName());
                    emailLive.postValue(user.getEmail());
                    permission.postValue(perm);
                    isAuthenticated.postValue(true);
                    likedRepo.syncLikedRecipesFromServer();
                }, t -> {
                    Log.e(TAG, "Google signIn error", t);
                    errorMessage.postValue("Ошибка авторизации: " + t.getMessage());
                })
        );
    }

    public void signOut(
            MutableLiveData<Boolean> isLoading,
            MutableLiveData<Boolean> isAuthenticated) {
        isLoading.postValue(true);
        authManager.signOut();
        
        // Очищаем лайки и настройки пользователя
        String userId = preferences.getString("userId", "0");
        if (userId != null && !userId.equals("0")) {
            likedRepo.clearAllLikes();
        }
        
        // Очищаем настройки пользователя
        preferences.putString("userId", "0");
        preferences.putString("username", "");
        preferences.putString("email", "");
        preferences.putInt("permission", 1);
        
        isLoading.postValue(false);
        isAuthenticated.postValue(false);
    }

    public void clear() {
        disposables.clear();
    }

    public void checkAuthenticationState(
            MutableLiveData<Boolean> isAuthenticated,
            MutableLiveData<String> displayName,
            MutableLiveData<String> emailLive,
            MutableLiveData<Integer> permission) {
        FirebaseUser user = authManager.getCurrentUser();
        if (user != null) {
            isAuthenticated.postValue(true);
            String name = user.getDisplayName();
            String email = user.getEmail();
            int perm = preferences.getInt("permission", 1);
            displayName.postValue(name);
            emailLive.postValue(email);
            permission.postValue(perm);
        } else {
            isAuthenticated.postValue(false);
        }
    }

    public void initGoogleSignIn(Context ctx, String webClientId) {
        authManager.initGoogleSignIn(ctx, webClientId);
    }

    public void signInWithGoogle(Activity activity) {
        authManager.signInWithGoogle(activity);
    }

    public boolean isUserLoggedIn() {
        return authManager.getCurrentUser() != null;
    }

    public String getCurrentUserId() {
        return preferences.getString("userId", "99");
    }
}

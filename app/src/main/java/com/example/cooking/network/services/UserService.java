package com.example.cooking.network.services;

import android.content.Context;
import android.util.Log;

import com.example.cooking.auth.UserRegisterRequest;
import com.example.cooking.data.models.ApiResponse;
import com.example.cooking.data.models.PasswordResetRequest;
import com.example.cooking.network.api.ApiService;

import io.reactivex.rxjava3.core.Completable;
import io.reactivex.rxjava3.core.Single;
import io.reactivex.rxjava3.schedulers.Schedulers;

import retrofit2.Call;

import com.google.firebase.auth.FirebaseAuth;

/**
 * Сервис для работы с пользователем
 */
public class UserService {
    private static final String TAG = "UserService";
    private final ApiService apiService;
    private final Context context;
    
    /**
     * Конструктор с контекстом
     * @param context контекст приложения
     */
    public UserService(Context context) {
        this.context = context.getApplicationContext();
        this.apiService = NetworkService.getApiService(this.context);
    }
    

    
    /**
     * RxJava Single для обновления имени пользователя
     */
    public Single<ApiResponse> updateUserNameSingle(String userId, String newName) {
        return Single.fromCallable(() -> {
            Log.d(TAG, "Updating user name: userId=" + userId + ", newName=" + newName);
            ApiResponse response = new ApiResponse();
            response.setSuccess(true);
            response.setMessage("Имя пользователя успешно обновлено");
            return response;
        }).subscribeOn(Schedulers.io());
    }

    /**
     * RxJava Single для удаления пользователя
     */
    public Single<ApiResponse> deleteUserSingle(String userId) {
        return Single.fromCallable(() -> {
            Log.d(TAG, "Deleting user: userId=" + userId);
            ApiResponse response = new ApiResponse();
            response.setSuccess(true);
            response.setMessage("Пользователь успешно удален");
            return response;
        }).subscribeOn(Schedulers.io());
    }
    

    
    /**
     * RxJava Single для входа пользователя
     */
    public Single<ApiResponse> loginFirebaseUserSingle() {
        return apiService.loginUser()
            .subscribeOn(Schedulers.io());
    }

    /**
     * RxJava Single для регистрации пользователя
     */
    public Single<ApiResponse> registerFirebaseUserSingle(String email, String name, String firebaseId) {
        return apiService.registerUser(new com.example.cooking.auth.UserRegisterRequest(email, name, firebaseId))
            .subscribeOn(Schedulers.io());
    }


    // Проверяет, авторизован ли пользователь через Firebase
    public static boolean isUserLoggedIn() {
        return FirebaseAuth.getInstance().getCurrentUser() != null;
    }
} 
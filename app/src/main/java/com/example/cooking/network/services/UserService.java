package com.example.cooking.network.services;

import android.content.Context;
import android.util.Log;

import com.example.cooking.auth.UserRegisterRequest;
import com.example.cooking.data.models.ApiResponse;
import com.example.cooking.data.models.PasswordResetRequest;
import com.example.cooking.network.api.ApiService;
import com.example.cooking.network.utils.ApiCallHandler;

import retrofit2.Call;

/**
 * Сервис для работы с пользователем
 */
public class UserService {
    private static final String TAG = "UserService";
    private final ApiService apiService;
    private final Context context;
    
    /**
     * Интерфейс для обратного вызова операций с пользователем
     */
    public interface UserCallback {
        void onSuccess(ApiResponse response);
        void onFailure(String errorMessage);
    }
    
    /**
     * Конструктор с контекстом
     * @param context контекст приложения
     */
    public UserService(Context context) {
        this.context = context.getApplicationContext();
        this.apiService = NetworkService.getApiService(this.context);
    }
    
    /**
     * Сохраняет данные пользователя в базе данных
     * @param userId идентификатор пользователя
     * @param username имя пользователя
     * @param email email пользователя
     * @param permission уровень прав доступа
     * @param callback колбэк для обработки результата
     */
    public void saveUser(String userId, String username, String email, int permission, UserCallback callback) {
        Log.d(TAG, "Saving user data: userId=" + userId + ", username=" + username + ", email=" + email);
        
        // Вызываем метод registerFirebaseUser для сохранения данных
        registerFirebaseUser(email, username, userId, callback);
    }
    
    /**
     * Обновляет имя пользователя в базе данных
     * @param userId идентификатор пользователя
     * @param newName новое имя пользователя
     * @param callback колбэк для обработки результата
     */
    public void updateUserName(String userId, String newName, UserCallback callback) {
        Log.d(TAG, "Updating user name: userId=" + userId + ", newName=" + newName);
        
        // Заглушка для обновления имени пользователя
        // В реальном приложении здесь будет обращение к API
        ApiResponse mockResponse = new ApiResponse();
        mockResponse.setSuccess(true);
        mockResponse.setMessage("Имя пользователя успешно обновлено");
        callback.onSuccess(mockResponse);
    }
    
    /**
     * Удаляет пользователя из базы данных
     * @param userId идентификатор пользователя
     * @param callback колбэк для обработки результата
     */
    public void deleteUser(String userId, UserCallback callback) {
        Log.d(TAG, "Deleting user: userId=" + userId);
        
        // Заглушка для удаления пользователя
        // В реальном приложении здесь будет обращение к API
        ApiResponse mockResponse = new ApiResponse();
        mockResponse.setSuccess(true);
        mockResponse.setMessage("Пользователь успешно удален");
        callback.onSuccess(mockResponse);
    }
    
    /**
     * Регистрация пользователя после успешной регистрации в Firebase
     * @param email Email пользователя
     * @param name Имя пользователя
     * @param firebaseId ID пользователя в Firebase
     * @param callback Callback для обработки результата
     */
    public void registerFirebaseUser(String email, String name, String firebaseId, UserCallback callback) {
        Log.d(TAG, "Registering Firebase user: email=" + email + ", name=" + name + ", firebaseId=" + firebaseId);
        
        UserRegisterRequest request = new UserRegisterRequest(email, name, firebaseId);
        Call<ApiResponse> call = apiService.registerUser(request);
        
        ApiCallHandler.execute(call, new ApiCallHandler.ApiCallback<ApiResponse>() {
            @Override
            public void onSuccess(ApiResponse response) {
                Log.d(TAG, "Register success: " + response.isSuccess());
                callback.onSuccess(response);
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Register error: " + errorMessage);
                callback.onFailure(errorMessage);
            }
        });
    }
    
    /**
     * Вход пользователя после успешного входа в Firebase
     * @param callback Callback для обработки результата
     */
    public void loginFirebaseUser(UserCallback callback) {
        Log.d(TAG, "Login Firebase user");
        Call<ApiResponse> call = apiService.loginUser();
        
        ApiCallHandler.execute(call, new ApiCallHandler.ApiCallback<ApiResponse>() {
            @Override
            public void onSuccess(ApiResponse response) {
                Log.d(TAG, "Login success: " + response.isSuccess());
                callback.onSuccess(response);
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Login error: " + errorMessage);
                callback.onFailure(errorMessage);
            }
        });
    }
    
    /**
     * Запрос на сброс пароля
     * @param email Email пользователя
     * @param callback Callback для обработки результата
     */
    public void requestPasswordReset(String email, UserCallback callback) {
        Log.d(TAG, "Request password reset for email: " + email);
        
        PasswordResetRequest request = new PasswordResetRequest(email);
        Call<ApiResponse> call = apiService.requestPasswordReset(request);
        
        ApiCallHandler.execute(call, new ApiCallHandler.ApiCallback<ApiResponse>() {
            @Override
            public void onSuccess(ApiResponse response) {
                Log.d(TAG, "Password reset request success: " + response.isSuccess());
                callback.onSuccess(response);
            }
            
            @Override
            public void onError(String errorMessage) {
                Log.e(TAG, "Password reset request error: " + errorMessage);
                callback.onFailure(errorMessage);
            }
        });
    }
} 
package com.example.cooking.data.repositories;

import android.content.Context;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cooking.auth.UserLoginRequest;
import com.example.cooking.auth.UserRegisterRequest;
import com.example.cooking.data.models.ApiResponse;
import com.example.cooking.data.models.PasswordResetRequest;
import com.example.cooking.data.models.PasswordResetResponse;
import com.example.cooking.network.utils.ApiCallHandler;
import com.example.cooking.network.utils.Resource;
import com.example.cooking.utils.MySharedPreferences;

/**
 * Репозиторий для управления данными пользователей
 * Обеспечивает доступ к API аутентификации и управления пользователями
 */
public class UserRepository extends NetworkRepository {
    private static final String TAG = "UserRepository";
    
    private final MySharedPreferences preferences;

    /**
     * Конструктор репозитория
     * @param context контекст приложения
     */
    public UserRepository(Context context) {
        super(context);
        this.preferences = new MySharedPreferences(context);
    }

    /**
     * Регистрирует нового пользователя
     * @param name имя пользователя
     * @param email электронная почта
     * @param password пароль
     * @return LiveData с результатом регистрации
     */
    public LiveData<Resource<ApiResponse>> registerUser(String name, String email, String password) {
        UserRegisterRequest request = new UserRegisterRequest(name, email, password);
        return ApiCallHandler.asLiveData(apiService.registerUser(request));
    }

    /**
     * Выполняет вход пользователя
     * @param email электронная почта
     * @param password пароль
     * @return LiveData с результатом входа
     */
    public LiveData<Resource<ApiResponse>> loginUser(String email, String password) {
        UserLoginRequest request = new UserLoginRequest(email, password);
        return ApiCallHandler.asLiveData(apiService.loginUser(request));
    }
    
    /**
     * Отправляет запрос на сброс пароля
     * @param email электронная почта
     * @return LiveData с результатом запроса
     */
    public LiveData<Resource<ApiResponse>> requestPasswordReset(String email) {
        PasswordResetRequest request = new PasswordResetRequest(email);
        MutableLiveData<Resource<ApiResponse>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));
        
        // Проверка соединения с интернетом
        if (!isNetworkAvailable()) {
            result.setValue(Resource.error("Отсутствует подключение к интернету", null));
            return result;
        }
        
        // Вызов API
        ApiCallHandler.execute(apiService.requestPasswordReset(request), 
            new ApiCallHandler.ApiCallback<ApiResponse>() {
                @Override
                public void onSuccess(ApiResponse response) {
                    Log.d(TAG, "Запрос на сброс пароля успешно отправлен");
                    result.postValue(Resource.success(response));
                }
                
                @Override
                public void onError(String errorMessage) {
                    Log.e(TAG, "Ошибка при запросе сброса пароля: " + errorMessage);
                    result.postValue(Resource.error(errorMessage, null));
                }
            }
        );
        
        return result;
    }
    
    /**
     * Сохраняет данные пользователя после успешного входа
     * @param response ответ от сервера с данными пользователя
     */
    public void saveUserData(ApiResponse response) {
        if (response != null && response.isSuccess()) {
            preferences.putString("userId", response.getUserId());
            preferences.putString("userName", response.getName());
            preferences.putInt("userPermission", response.getPermission());
            Log.d(TAG, "Данные пользователя сохранены: " + response.getUserId() + ", " + response.getName());
        } else {
            Log.e(TAG, "Попытка сохранить некорректные данные пользователя");
        }
    }
    
    /**
     * Получает ID текущего пользователя
     * @return ID пользователя или null, если пользователь не авторизован
     */
    public String getCurrentUserId() {
        String userId = preferences.getString("userId", null);
        return userId != null && !userId.isEmpty() && !userId.equals("0") ? userId : null;
    }
    
    /**
     * Очищает данные пользователя при выходе
     */
    public void logout() {
        preferences.putString("userId", null);
        preferences.putString("userName", null);
        preferences.putInt("userPermission", 0);
        Log.d(TAG, "Данные пользователя очищены при выходе");
    }
} 
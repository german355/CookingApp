package com.example.cooking.network.utils;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.cooking.network.models.BaseApiResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

/**
 * Утилитарный класс для обработки API-запросов
 */
public class ApiCallHandler {
    private static final String TAG = "ApiCallHandler";
    private static final Gson gson = new GsonBuilder().create();
    
    /**
     * Интерфейс для обратного вызова результата API-запроса
     * @param <T> тип ответа
     */
    public interface ApiCallback<T> {
        void onSuccess(T response);
        void onError(String errorMessage);
    }
    
    /**
     * Выполняет API-запрос с обработкой ответа
     * @param call объект Call для выполнения запроса
     * @param callback обратный вызов для обработки результата
     * @param <T> тип ответа
     */
    public static <T> void execute(Call<T> call, final ApiCallback<T> callback) {
        call.enqueue(new Callback<T>() {
            @Override
            public void onResponse(@NonNull Call<T> call, @NonNull Response<T> response) {
                Resource<T> resource = handleResponse(response);
                if (resource.isSuccess()) {
                    callback.onSuccess(resource.getData());
                } else {
                    callback.onError(resource.getMessage());
                }
            }
            
            @Override
            public void onFailure(@NonNull Call<T> call, @NonNull Throwable t) {
                String errorMessage = parseThrowable(t);
                Log.e(TAG, "Ошибка сети: " + errorMessage, t);
                callback.onError(errorMessage);
            }
        });
    }
    
    /**
     * Выполняет API-запрос и возвращает LiveData с Resource
     * @param call объект Call для выполнения запроса
     * @param <T> тип ответа
     * @return LiveData с Resource
     */
    public static <T> LiveData<Resource<T>> asLiveData(Call<T> call) {
        MutableLiveData<Resource<T>> result = new MutableLiveData<>();
        result.setValue(Resource.loading(null));
        
        execute(call, new ApiCallback<T>() {
            @Override
            public void onSuccess(T response) {
                result.setValue(Resource.success(response));
            }
            
            @Override
            public void onError(String errorMessage) {
                result.setValue(Resource.error(errorMessage, null));
            }
        });
        
        return result;
    }
    
    /**
     * Выполняет API-запрос синхронно и возвращает Resource
     * @param call объект Call для выполнения запроса
     * @param <T> тип ответа
     * @return Resource с результатом запроса
     */
    public static <T> Resource<T> executeSync(Call<T> call) {
        try {
            Response<T> response = call.execute();
            return handleResponse(response);
        } catch (IOException e) {
            String errorMessage = parseThrowable(e);
            Log.e(TAG, "Ошибка сети: " + errorMessage, e);
            return Resource.error(errorMessage, null);
        }
    }
    
    private static <T> Resource<T> handleResponse(Response<T> response) {
        if (response.isSuccessful() && response.body() != null) {
            T body = response.body();
            
            if (body instanceof BaseApiResponse) {
                BaseApiResponse apiResponse = (BaseApiResponse) body;
                if (apiResponse.isSuccess()) {
                    return Resource.success(body);
                } else {
                    String errorMessage = apiResponse.getMessage();
                    if (errorMessage == null || errorMessage.isEmpty()) {
                        errorMessage = "Неизвестная ошибка от сервера";
                    }
                    Log.w(TAG, "API вернул ошибку: " + errorMessage);
                    return Resource.error(errorMessage, null);
                }
            } else {
                return Resource.success(body);
            }
        } else {
            String errorMessage = parseErrorResponse(response);
            Log.e(TAG, "Ошибка API: " + errorMessage);
            return Resource.error(errorMessage, null);
        }
    }
    
    /**
     * Парсит ответ с ошибкой
     * @param response ответ с ошибкой
     * @return сообщение об ошибке
     */
    private static String parseErrorResponse(Response<?> response) {
        if (response.errorBody() == null) {
            return "Ошибка сервера: " + response.code();
        }
        
        try {
            String errorBody = response.errorBody().string();
            try {
                BaseApiResponse errorResponse = gson.fromJson(errorBody, BaseApiResponse.class);
                if (errorResponse != null && errorResponse.getMessage() != null && !errorResponse.getMessage().isEmpty()) {
                    return errorResponse.getMessage();
                }
            } catch (Exception ignored) {
                // Если не удалось распарсить как BaseApiResponse, возвращаем тело ошибки как есть
            }
            
            return "Ошибка сервера: " + response.code() + " - " + errorBody;
        } catch (IOException e) {
            return "Ошибка сервера: " + response.code() + " (не удалось прочитать тело ответа)";
        }
    }
    
    /**
     * Парсит исключение
     * @param t исключение
     * @return сообщение об ошибке
     */
    private static String parseThrowable(Throwable t) {
        if (t instanceof IOException) {
            if (t.getMessage() != null && (
                t.getMessage().contains("timeout") || 
                t.getMessage().contains("timed out"))) {
                return "Превышено время ожидания ответа от сервера";
            } else if (t.getMessage() != null && 
                      (t.getMessage().contains("Unable to resolve host") || 
                       t.getMessage().contains("Failed to connect"))) {
                return "Не удалось подключиться к серверу. Проверьте подключение к интернету";
            } else {
                return "Ошибка сети: " + t.getMessage();
            }
        } else {
            return "Непредвиденная ошибка: " + t.getMessage();
        }
    }
} 
package com.example.cooking.network.services;

import android.content.Context;
import android.util.Log;

import com.example.cooking.config.ServerConfig;
import com.example.cooking.network.api.ApiService;
import com.example.cooking.network.interceptors.AuthInterceptor;
import com.example.cooking.network.interceptors.RetryInterceptor;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava3.RxJava3CallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

/**
 * Единый класс для управления сетевыми запросами и API-клиентами.
 * Реализует паттерн Singleton с двойной проверкой для потокобезопасности.
 */
public class NetworkService {
    private static final String TAG = "NetworkService";
    
    // Параметры настройки клиента
    private static final int CONNECT_TIMEOUT = 30; // 30 секунд
    private static final int READ_TIMEOUT = 60;    // 60 секунд (увеличено)
    private static final int WRITE_TIMEOUT = 30;   // 30 секунд
    private static final int MAX_RETRY_ATTEMPTS = 3;
    private static final long RETRY_DELAY_MILLIS = 1000; // 1 секунда
    
    // Хранение экземпляров для singleton паттерна
    private static volatile OkHttpClient httpClient;
    private static volatile Retrofit retrofit;
    private static volatile ApiService apiService;
    
    // Закрытый конструктор для предотвращения создания экземпляров
    private NetworkService() {
        throw new AssertionError("Нельзя создавать экземпляры NetworkService");
    }
    
    /**
     * Получает настроенный OkHttpClient с использованием двойной проверки блокировки
     * для потокобезопасности.
     */
    public static OkHttpClient getHttpClient(Context context) {
        if (httpClient == null) {
            synchronized (NetworkService.class) {
                if (httpClient == null) {
                    // Создаем логгер для HTTP-запросов
                    HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor(message -> 
                        Log.d(TAG, "OkHttp: " + message));
                    
                    // Устанавливаем максимальный уровень логирования
                    loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BASIC);
                    
                    // Создаем и настраиваем клиент
                    OkHttpClient.Builder builder = new OkHttpClient.Builder()
                            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                            // Отключаем кэш, чтобы избежать ошибок "unexpected end of stream" на больших ответах
                            .cache(null)
                            .retryOnConnectionFailure(true)
                            // Принудительно используем HTTP/1.1 для стабильности соединения
                            .protocols(java.util.Arrays.asList(okhttp3.Protocol.HTTP_1_1))
                            .addInterceptor(loggingInterceptor)
                            .addInterceptor(new AuthInterceptor(context))
                            .addInterceptor(new RetryInterceptor(MAX_RETRY_ATTEMPTS, RETRY_DELAY_MILLIS));
                    
                    httpClient = builder.build();
                    
                    Log.d(TAG, "OkHttpClient создан с настроенными интерцепторами и таймаутами");
                }
            }
        }
        return httpClient;
    }
    
    /**
     * Получает настроенный Retrofit клиент с использованием двойной проверки блокировки
     * для потокобезопасности.

     */
    public static Retrofit getRetrofit(Context context) {
        if (retrofit == null) {
            synchronized (NetworkService.class) {
                if (retrofit == null) {
                    Log.d(TAG, "Создаю Retrofit с базовым URL: " + ServerConfig.BASE_API_URL);
                    
                    retrofit = new Retrofit.Builder()
                            .baseUrl(ServerConfig.BASE_API_URL)
                            .client(getHttpClient(context))
                            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                            .addConverterFactory(ScalarsConverterFactory.create()) // Для строковых ответов
                            .addConverterFactory(GsonConverterFactory.create())    // Для JSON ответов
                            .build();
                    
                    Log.d(TAG, "Retrofit создан с базовым URL " + ServerConfig.BASE_API_URL);
                }
            }
        }
        return retrofit;
    }
    
    /**
     * Получает сервис для общего API с использованием двойной проверки блокировки
     * для потокобезопасности.
     */
    public static ApiService getApiService(Context context) {
        if (apiService == null) {
            synchronized (NetworkService.class) {
                if (apiService == null) {
                    Log.d(TAG, "Создаю ApiService");
                    apiService = getRetrofit(context).create(ApiService.class);
                    Log.d(TAG, "ApiService создан");
                }
            }
        }
        return apiService;
    }
    

    
} 
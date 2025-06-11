package com.example.cooking.network.services;

import android.content.Context;
import android.util.Log;

import com.example.cooking.network.api.ApiService;
import com.example.cooking.network.interceptors.AuthInterceptor;
import com.example.cooking.network.interceptors.RetryInterceptor;

import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.OkHttpClient;
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
    
    // Базовый URL для API
    // Лучше хранить в BuildConfig или аналоге для разных окружений
    private static final String BASE_API_URL = "http://89.35.130.107/";
    
    // Параметры настройки клиента
    private static final int CONNECT_TIMEOUT = 30; // 30 секунд
    private static final int READ_TIMEOUT = 30;    // 30 секунд
    private static final int WRITE_TIMEOUT = 30;   // 30 секунд
    private static final int CACHE_SIZE = 10 * 1024 * 1024; // 10 MB
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
     *
     * @param context контекст приложения для доступа к кэшу
     * @return экземпляр OkHttpClient
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
                    
                    // Создаем кэш для запросов
                    Cache cache = new Cache(context.getCacheDir(), CACHE_SIZE);
                    
                    // Создаем и настраиваем клиент
                    OkHttpClient.Builder builder = new OkHttpClient.Builder()
                            .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                            .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                            .writeTimeout(WRITE_TIMEOUT, TimeUnit.SECONDS)
                            .cache(cache)
                            .retryOnConnectionFailure(true)
                            .addInterceptor(loggingInterceptor)
                            .addInterceptor(new AuthInterceptor(context))
                            .addInterceptor(new RetryInterceptor(MAX_RETRY_ATTEMPTS, RETRY_DELAY_MILLIS));
                    
                    // Добавляем кастомный интерцептор для модификации URL запросов
                    builder.addInterceptor(chain -> {
                        Request original = chain.request();
                        String url = original.url().toString();
                        
                        Log.d(TAG, "Интерцептор: проверяю URL: " + url);
                        
                        // Проверяем, является ли это запросом к endpoint search
                        if (url.contains("/search") && url.contains("q=")) {
                            Log.d(TAG, "Обнаружен запрос к /search. Исходный URL: " + url);
                            
                            // Пробуем быстрое решение: заменяем весь URL на нужный формат
                            // Используем хардкод для умного поиска для быстрого исправления
                            String query = "";
                            
                            // Получаем значение параметра q
                            String[] parts = url.split("q=");
                            if (parts.length >= 2) {
                                query = parts[1];
                                // Отрезаем все, что идет после параметра q, если есть другие параметры
                                if (query.contains("&")) {
                                    query = query.substring(0, query.indexOf("&"));
                                }
                                
                                Log.d(TAG, "Оригинальный запрос: " + query);
                                
                                try {
                                    // Кодируем запрос, если нужно
                                    query = java.net.URLDecoder.decode(query, "UTF-8");
                                    Log.d(TAG, "Декодированный запрос: " + query);
                                } catch (Exception e) {
                                    Log.e(TAG, "Ошибка при декодировании запроса", e);
                                }
                                
                                // Формируем новый URL с правильным форматированием
                                String baseUrl = "http://89.35.130.107/search?q=\"" + query.replace("\"", "").trim() + "\"";
                                
                                // Добавляем остальные параметры
                                if (url.contains("&")) {
                                    baseUrl += url.substring(url.indexOf("&"));
                                }
                                
                                Log.d(TAG, "Новый прямой URL: " + baseUrl);
                                
                                // Создаем новый запрос
                                Request newRequest = original.newBuilder()
                                        .url(baseUrl)
                                        .build();
                                
                                // Логируем заголовки для отладки
                                Log.d(TAG, "Заголовки запроса: " + newRequest.headers());
                                
                                return chain.proceed(newRequest);
                            }
                        }
                        
                        // Для остальных запросов не меняем URL
                        return chain.proceed(original);
                    });
                    
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
     *
     * @param context контекст приложения
     * @return экземпляр Retrofit
     */
    public static Retrofit getRetrofit(Context context) {
        if (retrofit == null) {
            synchronized (NetworkService.class) {
                if (retrofit == null) {
                    Log.d(TAG, "Создаю Retrofit с базовым URL: " + BASE_API_URL);
                    
                    // Удаляем все кэшированные данные
                    reset();
                    
                    retrofit = new Retrofit.Builder()
                            .baseUrl(BASE_API_URL)
                            .client(getHttpClient(context))
                            .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                            .addConverterFactory(ScalarsConverterFactory.create()) // Для строковых ответов
                            .addConverterFactory(GsonConverterFactory.create())    // Для JSON ответов
                            .build();
                    
                    Log.d(TAG, "Retrofit создан с базовым URL " + BASE_API_URL);
                }
            }
        }
        return retrofit;
    }
    
    /**
     * Получает Retrofit клиент с указанным базовым URL
     *
     * @param context контекст приложения
     * @param baseUrl базовый URL для API
     * @return экземпляр Retrofit с указанным URL
     */
    public static Retrofit getRetrofit(Context context, String baseUrl) {
        // Нормализуем URL, добавляя слеш в конце если его нет
        if (!baseUrl.endsWith("/")) {
            baseUrl += "/";
        }
        
        return new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(getHttpClient(context))
                .addCallAdapterFactory(RxJava3CallAdapterFactory.create())
                .addConverterFactory(ScalarsConverterFactory.create())
                .addConverterFactory(GsonConverterFactory.create())
                .build();
    }
    
    /**
     * Получает сервис для общего API с использованием двойной проверки блокировки
     * для потокобезопасности.
     *
     * @param context контекст приложения
     * @return экземпляр ApiService
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
    
    /**
     * Сбрасывает все закэшированные экземпляры клиентов и сервисов.
     * Полезно при смене авторизации или сетевых настроек.
     */
    public static synchronized void reset() {
        synchronized (NetworkService.class) {
            httpClient = null;
            retrofit = null;
            apiService = null;
            Log.d(TAG, "Все сетевые клиенты и сервисы сброшены");
        }
    }
    
    /**
     * Очищает кэш HTTP-запросов.
     * Полезно при проблемах с устаревшими данными.
     * 
     * @param context контекст приложения
     * @return true если кэш успешно очищен, false в случае ошибки
     */
    public static boolean clearCache(Context context) {
        try {
            Cache cache = getHttpClient(context).cache();
            if (cache != null) {
                cache.evictAll();
                Log.d(TAG, "Кэш сетевых запросов очищен");
                return true;
            }
            return false;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при очистке кэша", e);
            return false;
        }
    }
    
    /**
     * Возвращает текущий размер кэша.
     * 
     * @param context контекст приложения
     * @return размер кэша в байтах или -1 в случае ошибки
     */
    public static long getCacheSize(Context context) {
        try {
            Cache cache = getHttpClient(context).cache();
            return cache != null ? cache.size() : 0;
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при получении размера кэша", e);
            return -1;
        }
    }
} 
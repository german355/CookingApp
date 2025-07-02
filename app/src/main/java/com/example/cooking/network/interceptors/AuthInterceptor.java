package com.example.cooking.network.interceptors;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * OkHttp Interceptor для добавления Firebase ID токена в заголовок Authorization.
 */
public class AuthInterceptor implements Interceptor {

    private static final String TAG = "AuthInterceptor";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final long TOKEN_TIMEOUT_SECONDS = 15; // Увеличен timeout
    private static final long FALLBACK_TIMEOUT_SECONDS = 5; // Fallback timeout для повторной попытки
    
    private final Context context;
    private volatile String cachedToken = null;
    private volatile long tokenCacheTime = 0;
    private static final long TOKEN_CACHE_DURATION = 30 * 60 * 1000; // 30 минут кэширования
    
    /**
     * Конструктор
     * @param context контекст приложения
     */
    public AuthInterceptor(Context context) {
        this.context = context.getApplicationContext();
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request originalRequest = chain.request();
        Request.Builder requestBuilder = originalRequest.newBuilder();
        
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Log.d(TAG, "Пользователь не авторизован, пропускаем добавление токена.");
            return chain.proceed(requestBuilder.build());
        }

        String token = getTokenWithCaching(currentUser);
        if (token != null) {
            Log.d(TAG, "Добавляем Firebase ID токен в заголовок Authorization.");
            requestBuilder.header(AUTHORIZATION_HEADER, BEARER_PREFIX + token);
        } else {
            Log.w(TAG, "Не удалось получить Firebase ID токен, продолжаем без токена.");
        }
        
        return chain.proceed(requestBuilder.build());
    }

    /**
     * Получение токена с кэшированием для улучшения производительности
     */
    private String getTokenWithCaching(FirebaseUser user) throws IOException {
        long currentTime = System.currentTimeMillis();
        
        // Проверяем кэш
        if (cachedToken != null && (currentTime - tokenCacheTime) < TOKEN_CACHE_DURATION) {
            Log.d(TAG, "Используем кэшированный токен");
            return cachedToken;
        }

        // Получаем новый токен
        try {
            GetTokenResult tokenResult = Tasks.await(
                user.getIdToken(false), 
                TOKEN_TIMEOUT_SECONDS, 
                TimeUnit.SECONDS
            );
            String token = tokenResult.getToken();
            
            if (token != null) {
                // Кэшируем токен
                cachedToken = token;
                tokenCacheTime = currentTime;
                Log.d(TAG, "Токен получен и кэширован");
                return token;
            }
        } catch (TimeoutException e) {
            Log.w(TAG, "Timeout при получении токена, пробуем с force refresh");
            // Fallback: пробуем с принудительным обновлением и меньшим timeout
            try {
                GetTokenResult tokenResult = Tasks.await(
                    user.getIdToken(true), 
                    FALLBACK_TIMEOUT_SECONDS, 
                    TimeUnit.SECONDS
                );
                String token = tokenResult.getToken();
                if (token != null) {
                    cachedToken = token;
                    tokenCacheTime = currentTime;
                    return token;
                }
            } catch (Exception fallbackException) {
                Log.e(TAG, "Fallback также не удался: " + fallbackException.getMessage());
                throw new IOException("Таймаут получения токена аутентификации", e);
            }
        } catch (ExecutionException | InterruptedException e) {
            Log.e(TAG, "Ошибка при получении Firebase ID токена: " + e.getMessage(), e);
            Thread.currentThread().interrupt();
            throw new IOException("Ошибка получения токена аутентификации", e);
        }
        
        return null;
    }

    /**
     * Очистка кэшированного токена (вызывать при logout)
     */
    public void clearTokenCache() {
        cachedToken = null;
        tokenCacheTime = 0;
        Log.d(TAG, "Кэш токена очищен");
    }
}
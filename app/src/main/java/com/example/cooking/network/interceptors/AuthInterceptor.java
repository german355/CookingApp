package com.example.cooking.network.interceptors;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import com.example.cooking.utils.MySharedPreferences;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GetTokenResult;
import com.google.android.gms.tasks.Tasks; // Импорт для Tasks

import java.io.IOException;
import java.util.concurrent.ExecutionException; // Импорт для ExecutionException
import java.util.concurrent.TimeUnit; // Импорт для TimeUnit
import java.util.concurrent.TimeoutException; // Импорт для TimeoutException

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * OkHttp Interceptor для добавления Firebase ID токена в заголовок
 * Authorization и userId в заголовок X-User-ID.
 */
public class AuthInterceptor implements Interceptor {

    private static final String TAG = "AuthInterceptor";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String USER_ID_HEADER = "X-User-ID";
    private static final String BEARER_PREFIX = "Bearer ";
    private static final long TOKEN_TIMEOUT_SECONDS = 10; // Таймаут для ожидания токена
    
    private final Context context;
    
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
        
        // Добавляем userId в заголовок, если он есть
        MySharedPreferences preferences = new MySharedPreferences(context);
        String userId = preferences.getString("userId", null);
        if (userId != null && !userId.isEmpty() && !userId.equals("0")) {
            Log.d(TAG, "Добавляем userId в заголовок: " + userId);
            requestBuilder.header(USER_ID_HEADER, userId);
        }
        
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        // Если пользователь не авторизован, просто пропускаем запрос дальше
        if (currentUser == null) {
            Log.d(TAG, "Пользователь не авторизован, пропускаем добавление токена.");
            return chain.proceed(requestBuilder.build());
        }

        try {
            // Получаем ID токен синхронно (с таймаутом)
            // В реальном приложении лучше делать это асинхронно или кэшировать токен
            // Но для простоты примера используем синхронный вызов с таймаутом
            GetTokenResult tokenResult = Tasks.await(currentUser.getIdToken(false), TOKEN_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS);
            String idToken = tokenResult.getToken();

            if (idToken != null) {
                Log.d(TAG, "Добавляем Firebase ID токен в заголовок Authorization.");
                requestBuilder.header(AUTHORIZATION_HEADER, BEARER_PREFIX + idToken);
                return chain.proceed(requestBuilder.build());
            } else {
                Log.w(TAG, "Не удалось получить Firebase ID токен (null).");
                // Если токен null, отправляем оригинальный запрос
                return chain.proceed(requestBuilder.build());
            }

        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            Log.e(TAG, "Ошибка при получении Firebase ID токена: " + e.getMessage(), e);
            // В случае ошибки получения токена, отправляем оригинальный запрос
            // Можно добавить обработку ошибок, например, выбросить исключение или вернуть
            // ошибку
            Thread.currentThread().interrupt(); // Восстанавливаем флаг прерывания, если это было InterruptedException
            // Пробрасываем IOException, чтобы соответствовать сигнатуре метода
            throw new IOException("Ошибка получения токена аутентификации", e);
        }
    }
}
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
    private static final long TOKEN_TIMEOUT_SECONDS = 10;
    
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
        
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();

        if (currentUser == null) {
            Log.d(TAG, "Пользователь не авторизован, пропускаем добавление токена.");
            return chain.proceed(requestBuilder.build());
        }

        try {
            GetTokenResult tokenResult = Tasks.await(currentUser.getIdToken(false), TOKEN_TIMEOUT_SECONDS,
                    TimeUnit.SECONDS);
            String idToken = tokenResult.getToken();

            if (idToken != null) {
                Log.d(TAG, "Добавляем Firebase ID токен в заголовок Authorization.");
                requestBuilder.header(AUTHORIZATION_HEADER, BEARER_PREFIX + idToken);
                return chain.proceed(requestBuilder.build());
            } else {
                Log.w(TAG, "Не удалось получить Firebase ID токен (null).");
                return chain.proceed(requestBuilder.build());
            }

        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            Log.e(TAG, "Ошибка при получении Firebase ID токена: " + e.getMessage(), e);
            Thread.currentThread().interrupt();
            throw new IOException("Ошибка получения токена аутентификации", e);
        }
    }
}
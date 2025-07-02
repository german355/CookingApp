package com.example.cooking.network.interceptors;

import android.util.Log;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Интерсептор для повторных попыток запросов при сетевых ошибках или серверных ошибках (5xx)
 */
public class RetryInterceptor implements Interceptor {
    private static final String TAG = "RetryInterceptor";
    private final int maxRetries;
    private final long retryDelayMillis;

    /**
     * Создает интерсептор для повторных попыток запросов
     * @param maxRetries максимальное количество повторных попыток
     * @param retryDelayMillis задержка между повторными попытками в миллисекундах
     */
    public RetryInterceptor(int maxRetries, long retryDelayMillis) {
        this.maxRetries = maxRetries;
        this.retryDelayMillis = retryDelayMillis;
    }

    /**
     * Создает интерсептор с настройками по умолчанию (3 попытки, 1000 мс задержка)
     */
    public RetryInterceptor() {
        this(3, 1000);
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = null;
        IOException exception = null;

        int tryCount = 0;
        boolean shouldRetry;
        
        do {
            shouldRetry = false;
            try {
                if (tryCount > 0) {
                    Log.d(TAG, "Повторная попытка #" + tryCount + " для " + request.url());
                }
                
                if (response != null && response.body() != null) {
                    response.close();
                }
                
                response = chain.proceed(request);
                
                // Повторяем только при серверных ошибках (5xx) или ошибке 429 (Too Many Requests)
                if (response.isSuccessful() || (response.code() < 500 && response.code() != 429)) {
                    return response; // Успешный ответ или клиентская ошибка (кроме 429), не повторяем
                }
                
                // Если получили ошибку сервера (5xx) или 429, пробуем еще раз
                shouldRetry = tryCount < maxRetries;
                Log.w(TAG, "Запрос не успешен, код: " + response.code() + " URL: " + request.url() + 
                          (shouldRetry ? " Будет повторная попытка." : " Достигнут лимит повторных попыток."));
                
            } catch (IOException e) {
                exception = e;
                shouldRetry = tryCount < maxRetries;
                Log.e(TAG, "IOException для " + request.url() + 
                          (shouldRetry ? " Будет повторная попытка." : " Достигнут лимит повторных попыток."), e);
            }
            
            if (shouldRetry) {
                tryCount++;
                try {
                    long sleepTime = retryDelayMillis * (long)Math.pow(1.5, tryCount - 1); // Экспоненциальная задержка
                    Log.d(TAG, "Ожидание " + sleepTime + " мс перед следующей попыткой");
                    Thread.sleep(sleepTime);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Повторная попытка прервана", e);
                }
            }
        } while (shouldRetry);

        // Если все попытки неудачны, возвращаем последний ответ или выбрасываем исключение
        if (response != null) {
            return response; // Возвращаем последний неудачный ответ
        }
        
        if (exception != null) {
            throw exception; // Если был IOException, кидаем его
        }
        
        // Если по какой-то причине и response и exception null (маловероятно)
        throw new IOException("Не удалось выполнить запрос после " + maxRetries + " попыток.");
    }
} 
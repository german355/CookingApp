package com.example.cooking.data.repositories;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.example.cooking.network.api.ApiService;
import com.example.cooking.network.services.NetworkService;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Базовый класс для репозиториев, работающих с сетью
 * Предоставляет общую функциональность для всех сетевых репозиториев
 */
public abstract class NetworkRepository {
    protected final Context context;
    protected final ApiService apiService;
    protected final Executor executor;
    
    /**
     * Конструктор базового репозитория
     * @param context контекст приложения
     */
    protected NetworkRepository(Context context) {
        this.context = context.getApplicationContext();
        this.apiService = NetworkService.getApiService(context);
        this.executor = Executors.newFixedThreadPool(3);
    }
    
    /**
     * Проверяет наличие интернет-соединения
     * @return true, если есть подключение к интернету
     */
    protected boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = 
                (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager != null) {
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }
        return false;
    }
    
    /**
     * Выполняет операцию в фоновом потоке
     * @param runnable операция для выполнения
     */
    protected void executeInBackground(Runnable runnable) {
        executor.execute(runnable);
    }
} 
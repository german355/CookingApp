package com.example.cooking.utils;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Глобальный пул исполнителей для приложения
 * Используется для операций в фоновых потоках, на главном потоке и для операций ввода-вывода
 */
public class AppExecutors {

    private static final int THREAD_COUNT = 3;

    private final Executor diskIO;
    private final Executor networkIO;
    private final Executor mainThread;

    // Синглтон для получения экземпляра
    private static AppExecutors instance;

    public static AppExecutors getInstance() {
        if (instance == null) {
            synchronized (AppExecutors.class) {
                if (instance == null) {
                    instance = new AppExecutors();
                }
            }
        }
        return instance;
    }

    private AppExecutors() {
        this.diskIO = Executors.newSingleThreadExecutor();
        this.networkIO = Executors.newFixedThreadPool(THREAD_COUNT);
        this.mainThread = new MainThreadExecutor();
    }

    /**
     * Получает исполнителя для дисковых операций
     * @return исполнитель для операций ввода-вывода
     */
    public Executor diskIO() {
        return diskIO;
    }

    /**
     * Получает исполнителя для сетевых операций
     * @return исполнитель для сетевых операций
     */
    public Executor networkIO() {
        return networkIO;
    }

    /**
     * Получает исполнителя для главного потока
     * @return исполнитель для операций в UI потоке
     */
    public Executor mainThread() {
        return mainThread;
    }

    /**
     * Исполнитель для главного (UI) потока
     */
    private static class MainThreadExecutor implements Executor {
        private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(@NonNull Runnable command) {
            mainThreadHandler.post(command);
        }
    }
} 
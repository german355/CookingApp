package com.example.cooking.utils;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Глобальный пул исполнителей для приложения
 * Используется для операций в фоновых потоках, на главном потоке и для операций ввода-вывода
 */
public class AppExecutors {

    private static final int THREAD_COUNT = 3;

    private final ExecutorService diskIO;
    private final ExecutorService networkIO;
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
        this.diskIO = Executors.newFixedThreadPool(2); // Вместо single thread
        this.networkIO = Executors.newFixedThreadPool(THREAD_COUNT);
        this.mainThread = new MainThreadExecutor();
    }

    /**
     * Получает исполнителя для дисковых операций
     * Использует пул из 2 потоков для параллельной обработки дисковых операций
     */
    public Executor diskIO() {
        return diskIO;
    }

    /**
     * Получает исполнителя для сетевых операций
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
     * Корректное завершение работы всех executor'ов для предотвращения утечек памяти
     */
    public static void shutdown() {
        if (instance != null) {
            synchronized (AppExecutors.class) {
                if (instance != null) {
                    try {
                        instance.diskIO.shutdown();
                        instance.networkIO.shutdown();
                        
                        // Ждем завершения текущих задач
                        if (!instance.diskIO.awaitTermination(5, TimeUnit.SECONDS)) {
                            instance.diskIO.shutdownNow();
                        }
                        if (!instance.networkIO.awaitTermination(5, TimeUnit.SECONDS)) {
                            instance.networkIO.shutdownNow();
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        instance.diskIO.shutdownNow();
                        instance.networkIO.shutdownNow();
                    } finally {
                        instance = null;
                    }
                }
            }
        }
    }

    /**
     * Проверяет, были ли executor'ы завершены
     */
    public boolean isShutdown() {
        return diskIO.isShutdown() && networkIO.isShutdown();
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
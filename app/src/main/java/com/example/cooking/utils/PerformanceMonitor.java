package com.example.cooking.utils;

import android.util.Log;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Класс для мониторинга производительности критических операций.
 * Отслеживает время выполнения сериализации, операций БД и привязки RecyclerView.
 */
public class PerformanceMonitor {
    private static final String TAG = "PerformanceMonitor";
    
    // Пороги для логирования медленных операций
    private static final long DATABASE_OPERATION_THRESHOLD_MS = 100;
    private static final long SERIALIZATION_THRESHOLD_MS = 5;
    private static final long RECYCLERVIEW_BIND_THRESHOLD_MS = 16; // 60fps = 16ms per frame
    
    // Статистика операций
    private static final ConcurrentHashMap<String, OperationStats> operationStats = new ConcurrentHashMap<>();
    
    /**
     * Измеряет время выполнения операции с базой данных.
     * @param operation название операции
     * @param task задача для выполнения
     */
    public static void measureDatabaseOperation(String operation, Runnable task) {
        long startTime = System.currentTimeMillis();
        try {
            task.run();
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            recordOperation("DB_" + operation, duration);
            
            if (duration > DATABASE_OPERATION_THRESHOLD_MS) {
                Log.w(TAG, "Медленная операция БД: " + operation + " заняла " + duration + "ms");
            } else {
                Log.d(TAG, "БД операция: " + operation + " = " + duration + "ms");
            }
        }
    }
    
    /**
     * Измеряет время выполнения операции сериализации.
     * @param operation название операции
     * @param task задача для выполнения
     * @param itemCount количество обрабатываемых элементов
     */
    public static void measureSerializationOperation(String operation, Runnable task, int itemCount) {
        long startTime = System.nanoTime();
        try {
            task.run();
        } finally {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            recordOperation("SER_" + operation, durationMs);
            
            if (durationMs > SERIALIZATION_THRESHOLD_MS) {
                double avgPerItem = itemCount > 0 ? (double) durationMs / itemCount : 0;
                Log.w(TAG, String.format("Медленная сериализация: %s заняла %dms для %d элементов (%.2f ms/элемент)", 
                                        operation, durationMs, itemCount, avgPerItem));
            }
        }
    }
    
    /**
     * Измеряет время привязки данных в RecyclerView.
     * @param bindTask задача привязки
     */
    public static void measureRecyclerViewBind(Runnable bindTask) {
        long startTime = System.nanoTime();
        try {
            bindTask.run();
        } finally {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            recordOperation("RV_BIND", durationMs);
            
            if (durationMs > RECYCLERVIEW_BIND_THRESHOLD_MS) {
                Log.w(TAG, "Медленная привязка RecyclerView: " + durationMs + "ms (цель: <16ms для 60fps)");
            }
        }
    }
    
    /**
     * Измеряет время выполнения произвольной операции.
     * @param operationName название операции
     * @param task задача для выполнения
     * @return результат выполнения задачи
     */
    public static <T> T measureOperation(String operationName, java.util.function.Supplier<T> task) {
        long startTime = System.nanoTime();
        try {
            return task.get();
        } finally {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            recordOperation(operationName, durationMs);
            Log.d(TAG, operationName + " = " + durationMs + "ms");
        }
    }
    
    /**
     * Записывает статистику выполнения операции.
     */
    private static void recordOperation(String operation, long durationMs) {
        operationStats.computeIfAbsent(operation, k -> new OperationStats())
                     .recordExecution(durationMs);
    }
    
    /**
     * Возвращает детальную статистику производительности.
     */
    public static String getPerformanceReport() {
        StringBuilder report = new StringBuilder();
        report.append("=== Отчет о производительности ===\n");
        
        for (ConcurrentHashMap.Entry<String, OperationStats> entry : operationStats.entrySet()) {
            OperationStats stats = entry.getValue();
            report.append(String.format("%s: выполнено=%d, среднее=%.1fms, мин=%dms, макс=%dms\n",
                                      entry.getKey(), 
                                      stats.getExecutionCount(),
                                      stats.getAverageTime(),
                                      stats.getMinTime(),
                                      stats.getMaxTime()));
        }
        
        return report.toString();
    }
    
    /**
     * Логирует отчет о производительности.
     */
    public static void logPerformanceReport() {
        Log.i(TAG, getPerformanceReport());
    }
    
    /**
     * Очищает собранную статистику.
     */
    public static void clearStats() {
        operationStats.clear();
        Log.d(TAG, "Статистика производительности очищена");
    }
    
    /**
     * Проверяет, есть ли проблемы с производительностью.
     * @return true если обнаружены медленные операции
     */
    public static boolean hasPerformanceIssues() {
        for (OperationStats stats : operationStats.values()) {
            if (stats.getAverageTime() > 50) { // Более 50ms считается проблемой
                return true;
            }
        }
        return false;
    }
    
    /**
     * Внутренний класс для хранения статистики операций.
     */
    private static class OperationStats {
        private final AtomicInteger executionCount = new AtomicInteger(0);
        private final AtomicLong totalTime = new AtomicLong(0);
        private volatile long minTime = Long.MAX_VALUE;
        private volatile long maxTime = Long.MIN_VALUE;
        
        public void recordExecution(long durationMs) {
            executionCount.incrementAndGet();
            totalTime.addAndGet(durationMs);
            
            // Обновляем мин/макс значения потокобезопасно
            synchronized (this) {
                if (durationMs < minTime) {
                    minTime = durationMs;
                }
                if (durationMs > maxTime) {
                    maxTime = durationMs;
                }
            }
        }
        
        public int getExecutionCount() {
            return executionCount.get();
        }
        
        public double getAverageTime() {
            int count = executionCount.get();
            return count > 0 ? (double) totalTime.get() / count : 0;
        }
        
        public long getMinTime() {
            return minTime == Long.MAX_VALUE ? 0 : minTime;
        }
        
        public long getMaxTime() {
            return maxTime == Long.MIN_VALUE ? 0 : maxTime;
        }
    }
    
    /**
     * Вспомогательный класс для измерения времени выполнения блоков кода.
     */
    public static class Timer {
        private final String operationName;
        private final long startTime;
        
        private Timer(String operationName) {
            this.operationName = operationName;
            this.startTime = System.nanoTime();
        }
        
        /**
         * Создает новый таймер для операции.
         */
        public static Timer start(String operationName) {
            return new Timer(operationName);
        }
        
        /**
         * Останавливает таймер и записывает результат.
         */
        public void stop() {
            long durationMs = (System.nanoTime() - startTime) / 1_000_000;
            recordOperation(operationName, durationMs);
            Log.d(TAG, operationName + " завершена за " + durationMs + "ms");
        }
    }
} 
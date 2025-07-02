package com.example.cooking.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.util.LruCache;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.security.MessageDigest;

/**
 * Менеджер кэширования изображений с поддержкой memory и disk кэширования
 */
public class ImageCacheManager {
    private static final String TAG = "ImageCacheManager";
    private static ImageCacheManager instance;
    
    // Memory cache
    private LruCache<String, Bitmap> memoryCache;
    
    // Disk cache
    private File diskCacheDir;
    private static final String CACHE_DIR_NAME = "image_cache";
    private static final int DISK_CACHE_SIZE = 50 * 1024 * 1024; // 50MB
    private static final int MEMORY_CACHE_SIZE = (int) (Runtime.getRuntime().maxMemory() / 8); // 1/8 от доступной памяти
    
    private ImageCacheManager(Context context) {
        initMemoryCache();
        initDiskCache(context);
    }
    
    public static synchronized ImageCacheManager getInstance(Context context) {
        if (instance == null) {
            instance = new ImageCacheManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Инициализация memory кэша
     */
    private void initMemoryCache() {
        memoryCache = new LruCache<String, Bitmap>(MEMORY_CACHE_SIZE) {
            @Override
            protected int sizeOf(String key, Bitmap bitmap) {
                return bitmap.getByteCount();
            }
            
            @Override
            protected void entryRemoved(boolean evicted, String key, Bitmap oldValue, Bitmap newValue) {
                if (evicted && oldValue != null && !oldValue.isRecycled()) {
                    // Не рециклим здесь - bitmap может использоваться в UI
                    Log.d(TAG, "Memory cache: evicted bitmap for key " + key);
                }
            }
        };
        
        Log.d(TAG, "Memory cache initialized with size: " + (MEMORY_CACHE_SIZE / 1024 / 1024) + " MB");
    }
    
    /**
     * Инициализация disk кэша
     */
    private void initDiskCache(Context context) {
        diskCacheDir = new File(context.getCacheDir(), CACHE_DIR_NAME);
        if (!diskCacheDir.exists()) {
            diskCacheDir.mkdirs();
        }
        
        // Очищаем старые файлы при превышении лимита
        cleanupDiskCache();
        
        Log.d(TAG, "Disk cache initialized at: " + diskCacheDir.getAbsolutePath());
    }
    
    /**
     * Получение bitmap из кэша
     */
    public Bitmap getBitmap(String url) {
        if (url == null || url.isEmpty()) {
            return null;
        }
        
        String key = generateCacheKey(url);
        
        // Сначала проверяем memory cache
        Bitmap bitmap = memoryCache.get(key);
        if (bitmap != null && !bitmap.isRecycled()) {
            Log.d(TAG, "Memory cache hit for: " + url);
            return bitmap;
        }
        
        // Затем проверяем disk cache
        bitmap = getBitmapFromDisk(key);
        if (bitmap != null) {
            Log.d(TAG, "Disk cache hit for: " + url);
            // Добавляем в memory cache
            memoryCache.put(key, bitmap);
            return bitmap;
        }
        
        Log.d(TAG, "Cache miss for: " + url);
        return null;
    }
    
    /**
     * Сохранение bitmap в кэш
     */
    public void putBitmap(String url, Bitmap bitmap) {
        if (url == null || url.isEmpty() || bitmap == null || bitmap.isRecycled()) {
            return;
        }
        
        String key = generateCacheKey(url);
        
        // Сохраняем в memory cache
        memoryCache.put(key, bitmap);
        
        // Сохраняем в disk cache асинхронно
        AppExecutors.getInstance().diskIO().execute(() -> {
            saveBitmapToDisk(key, bitmap);
        });
        
        Log.d(TAG, "Bitmap cached for: " + url);
    }
    
    /**
     * Получение bitmap из disk кэша
     */
    private Bitmap getBitmapFromDisk(String key) {
        File cacheFile = new File(diskCacheDir, key);
        if (cacheFile.exists()) {
            try (FileInputStream fis = new FileInputStream(cacheFile)) {
                Bitmap bitmap = BitmapFactory.decodeStream(fis);
                if (bitmap != null) {
                    // Обновляем время доступа к файлу
                    cacheFile.setLastModified(System.currentTimeMillis());
                    return bitmap;
                }
            } catch (Exception e) {
                Log.w(TAG, "Error reading from disk cache: " + key, e);
                // Удаляем поврежденный файл
                cacheFile.delete();
            }
        }
        return null;
    }
    
    /**
     * Сохранение bitmap в disk кэш
     */
    private void saveBitmapToDisk(String key, Bitmap bitmap) {
        File cacheFile = new File(diskCacheDir, key);
        try (FileOutputStream fos = new FileOutputStream(cacheFile);
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
            fos.write(baos.toByteArray());
            fos.flush();
            
            Log.d(TAG, "Bitmap saved to disk cache: " + key);
        } catch (Exception e) {
            Log.w(TAG, "Error saving to disk cache: " + key, e);
            // Удаляем частично записанный файл
            if (cacheFile.exists()) {
                cacheFile.delete();
            }
        }
    }
    
    /**
     * Генерация ключа кэша из URL
     */
    private String generateCacheKey(String url) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            digest.update(url.getBytes());
            byte[] hash = digest.digest();
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            Log.w(TAG, "Error generating cache key", e);
            return String.valueOf(url.hashCode());
        }
    }
    
    /**
     * Очистка disk кэша при превышении лимита
     */
    private void cleanupDiskCache() {
        if (!diskCacheDir.exists()) {
            return;
        }
        
        File[] files = diskCacheDir.listFiles();
        if (files == null) {
            return;
        }
        
        long totalSize = 0;
        for (File file : files) {
            totalSize += file.length();
        }
        
        if (totalSize > DISK_CACHE_SIZE) {
            Log.d(TAG, "Disk cache cleanup needed. Current size: " + (totalSize / 1024 / 1024) + " MB");
            
            // Сортируем файлы по времени последнего доступа
            java.util.Arrays.sort(files, (f1, f2) -> 
                Long.compare(f1.lastModified(), f2.lastModified()));
            
            // Удаляем старые файлы пока не достигнем приемлемого размера
            for (File file : files) {
                if (totalSize <= DISK_CACHE_SIZE * 0.8) { // Удаляем до 80% от лимита
                    break;
                }
                totalSize -= file.length();
                file.delete();
                Log.d(TAG, "Deleted old cache file: " + file.getName());
            }
        }
    }
    
    /**
     * Очистка всего кэша
     */
    public void clearCache() {
        // Очищаем memory cache
        memoryCache.evictAll();
        
        // Очищаем disk cache
        AppExecutors.getInstance().diskIO().execute(() -> {
            if (diskCacheDir.exists()) {
                File[] files = diskCacheDir.listFiles();
                if (files != null) {
                    for (File file : files) {
                        file.delete();
                    }
                }
            }
            Log.d(TAG, "Cache cleared");
        });
    }
    
    /**
     * Получение информации о кэше
     */
    public void logCacheInfo() {
        int memorySize = memoryCache.size();
        int memoryMaxSize = memoryCache.maxSize();
        
        long diskSize = 0;
        int diskFileCount = 0;
        if (diskCacheDir.exists()) {
            File[] files = diskCacheDir.listFiles();
            if (files != null) {
                diskFileCount = files.length;
                for (File file : files) {
                    diskSize += file.length();
                }
            }
        }
        
        Log.i(TAG, String.format("Cache info - Memory: %d/%d bytes, Disk: %d files, %d MB", 
                memorySize, memoryMaxSize, diskFileCount, diskSize / 1024 / 1024));
    }
}
package com.example.cooking.domain.services;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import com.example.cooking.R;
import com.example.cooking.utils.AppExecutors;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;

/**
 * Класс для обработки изображений в приложении.
 * Централизует всю логику работы с изображениями: загрузка, обработка, сжатие, валидация.
 */
public class ImageProcessor {
    
    private static final String TAG = "ImageProcessor";
    
    // Константы для обработки изображений
    private static final int DEFAULT_MAX_SIZE = 800; // Максимальный размер стороны в пикселях
    private static final int DEFAULT_QUALITY = 85; // Качество JPEG сжатия (0-100)
    private static final int MAX_FILE_SIZE_MB = 5; // Максимальный размер файла в МБ
    private static final int MIN_FILE_SIZE_KB = 1; // Минимальный размер файла в КБ
    
    private final Context context;
    
    public ImageProcessor(Context context) {
        this.context = context;
    }
    
    /**
     * Результат обработки изображения
     */
    public static class ImageResult {
        private final boolean success;
        private final byte[] imageBytes;
        private final String errorMessage;
        private final int processedWidth;
        private final int processedHeight;
        private final long processedSize;
        
        private ImageResult(boolean success, byte[] imageBytes, String errorMessage,
                           int processedWidth, int processedHeight, long processedSize) {
            this.success = success;
            this.imageBytes = imageBytes;
            this.errorMessage = errorMessage;
            this.processedWidth = processedWidth;
            this.processedHeight = processedHeight;
            this.processedSize = processedSize;
        }
        
        // Геттеры
        public boolean isSuccess() { return success; }
        public byte[] getImageBytes() { return imageBytes; }
        public String getErrorMessage() { return errorMessage; }
        public int getProcessedWidth() { return processedWidth; }
        public int getProcessedHeight() { return processedHeight; }
        public long getProcessedSize() { return processedSize; }
        
        // Статические методы для создания результатов
        public static ImageResult success(byte[] imageBytes, int originalWidth, int originalHeight,
                                        int processedWidth, int processedHeight, long originalSize, long processedSize) {
            return new ImageResult(true, imageBytes, null,
                    processedWidth, processedHeight, processedSize);
        }
        
        public static ImageResult error(String errorMessage) {
            return new ImageResult(false, null, errorMessage, 0, 0, 0);
        }
    }
    
   
    public interface ImageProcessingCallback {
        void onSuccess(ImageResult result);
        void onError(String errorMessage);
        void onProgress(int progress); // 0-100
    }

    
    
    public void processImageFromUri(Uri imageUri, int maxSize, int quality, ImageProcessingCallback callback) {
        if (imageUri == null) {
            callback.onError(context.getString(R.string.error_image_selection_uri_null));
            return;
        }
        
        Log.d(TAG, "Начинаю обработку изображения из Uri: " + imageUri);
        callback.onProgress(10);
        
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                InputStream inputStream = context.getContentResolver().openInputStream(imageUri);
                if (inputStream == null) {
                    AppExecutors.getInstance().mainThread().execute(() -> 
                        callback.onError(context.getString(R.string.error_opening_image_stream)));
                    return;
                }
                
                AppExecutors.getInstance().mainThread().execute(() -> callback.onProgress(30));

                // Декодируем изображение
                Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
                inputStream.close();
                
                if (originalBitmap == null) {
                    AppExecutors.getInstance().mainThread().execute(() -> 
                        callback.onError(context.getString(R.string.error_decoding_image)));
                    return;
                }
                
                AppExecutors.getInstance().mainThread().execute(() -> callback.onProgress(50));
                
                // Обрабатываем изображение
                ImageResult result = processBitmap(originalBitmap, maxSize, quality);
                
                AppExecutors.getInstance().mainThread().execute(() -> {
                    callback.onProgress(100);
                    if (result.isSuccess()) {
                        callback.onSuccess(result);
                    } else {
                        callback.onError(result.getErrorMessage());
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при обработке изображения из Uri", e);
                AppExecutors.getInstance().mainThread().execute(() -> 
                    callback.onError(context.getString(R.string.error_processing_image, e.getMessage())));
            }
        });
    }
    

    /**
     * Загружает и обрабатывает изображение по URL с настраиваемыми параметрами
     *
     */
    public void processImageFromUrl(String imageUrl, int maxSize, int quality, ImageProcessingCallback callback) {
        if (imageUrl == null || imageUrl.trim().isEmpty()) {
            callback.onError("URL изображения пуст");
            return;
        }
        
        Log.d(TAG, "Начинаю загрузку изображения по URL: " + imageUrl);
        callback.onProgress(10);
        
        AppExecutors.getInstance().diskIO().execute(() -> {
            try {
                URL url = new URL(imageUrl);
                AppExecutors.getInstance().mainThread().execute(() -> callback.onProgress(30));
                
                // Загружаем изображение
                Bitmap originalBitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream());
                
                if (originalBitmap == null) {
                    AppExecutors.getInstance().mainThread().execute(() -> 
                        callback.onError("Не удалось декодировать изображение по URL"));
                    return;
                }
                
                AppExecutors.getInstance().mainThread().execute(() -> callback.onProgress(60));
                
                // Обрабатываем изображение
                ImageResult result = processBitmap(originalBitmap, maxSize, quality);
                
                AppExecutors.getInstance().mainThread().execute(() -> {
                    callback.onProgress(100);
                    if (result.isSuccess()) {
                        callback.onSuccess(result);
                    } else {
                        callback.onError(result.getErrorMessage());
                    }
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Ошибка при загрузке изображения по URL", e);
                AppExecutors.getInstance().mainThread().execute(() -> 
                    callback.onError("Ошибка загрузки изображения: " + e.getMessage()));
            }
        });
    }
    
    /**
     * Синхронная обработка Bitmap
     */
    public ImageResult processBitmap(Bitmap originalBitmap, int maxSize, int quality) {
        if (originalBitmap == null) {
            return ImageResult.error("Изображение не может быть null");
        }
        
        try {
            int originalWidth = originalBitmap.getWidth();
            int originalHeight = originalBitmap.getHeight();
            
            Log.d(TAG, "Обрабатываю изображение: " + originalWidth + "x" + originalHeight);
            
            // Изменяем размер если нужно
            Bitmap resizedBitmap = resizeBitmap(originalBitmap, maxSize);
            int processedWidth = resizedBitmap.getWidth();
            int processedHeight = resizedBitmap.getHeight();
            
            // Сжимаем в JPEG
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            boolean compressed = resizedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
            
            if (!compressed) {
                return ImageResult.error("Не удалось сжать изображение");
            }
            
            byte[] imageBytes = baos.toByteArray();
            long originalSize = estimateOriginalSize(originalWidth, originalHeight);
            long processedSize = imageBytes.length;
            
            // Валидируем размер файла
            if (processedSize > MAX_FILE_SIZE_MB * 1024 * 1024) {
                return ImageResult.error("Размер изображения превышает " + MAX_FILE_SIZE_MB + " МБ");
            }
            
            if (processedSize < MIN_FILE_SIZE_KB * 1024) {
                return ImageResult.error("Файл изображения слишком мал или поврежден");
            }
            
            Log.d(TAG, "Изображение обработано: " + processedWidth + "x" + processedHeight + 
                      ", размер: " + (processedSize / 1024) + " КБ");
            
            return ImageResult.success(imageBytes, originalWidth, originalHeight, 
                                     processedWidth, processedHeight, originalSize, processedSize);
            
        } catch (Exception e) {
            Log.e(TAG, "Ошибка при обработке Bitmap", e);
            return ImageResult.error("Ошибка обработки изображения: " + e.getMessage());
        }
    }
    
    /**
     * Изменяет размер Bitmap с сохранением пропорций
     */
    public Bitmap resizeBitmap(Bitmap bitmap, int maxSize) {
        if (bitmap == null) {
            return null;
        }
        
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        
        // Если изображение уже меньше максимального размера, возвращаем как есть
        if (width <= maxSize && height <= maxSize) {
            return bitmap;
        }
        
        float ratio = (float) width / height;
        int newWidth, newHeight;
        
        if (width > height) {
            // Ландшафтная ориентация
            newWidth = maxSize;
            newHeight = (int) (maxSize / ratio);
        } else {
            // Портретная ориентация
            newHeight = maxSize;
            newWidth = (int) (maxSize * ratio);
        }
        
        Log.d(TAG, "Изменяю размер с " + width + "x" + height + " на " + newWidth + "x" + newHeight);
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
    }
    

    
    /**
     * Оценивает исходный размер изображения в байтах
     */
    private long estimateOriginalSize(int width, int height) {
        // Приблизительная оценка: 4 байта на пиксель (ARGB) + 20% накладные расходы
        return (long) (width * height * 4 * 1.2);
    }

    
    
} 
package com.example.cooking;

import android.app.Application;
import androidx.preference.PreferenceManager;
import android.content.SharedPreferences;
import com.example.cooking.utils.AppExecutors;
import com.example.cooking.utils.ThemeUtils;


/**
 * Кастомный класс приложения для инициализации глобальных настроек
 */
public class MyApplication extends Application {

    @Override
    public void onCreate() {
        // Инициализация TokenStorage

        super.onCreate();

        // Инициализация темы приложения при запуске
        initializeTheme();

        

    }

    @Override
    public void onTerminate() {
        super.onTerminate();
        
        // Корректно завершаем все executor'ы для предотвращения утечек памяти
        try {
            AppExecutors.shutdown();
        } catch (Exception e) {
            android.util.Log.w("MyApplication", "Error shutting down AppExecutors", e);
        }
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
        
        // При нехватке памяти очищаем кэши
        try {
            // Можно добавить очистку кэшей репозиториев если необходимо
            android.util.Log.i("MyApplication", "Low memory callback - considering cache cleanup");
        } catch (Exception e) {
            android.util.Log.w("MyApplication", "Error during low memory cleanup", e);
        }
    }

    /**
     * Инициализирует тему приложения согласно сохраненным настройкам
     */
    private void initializeTheme() {

        SharedPreferences sharedPreferences = getSharedPreferences("acs", MODE_PRIVATE);


        String themeValue = sharedPreferences.getString("theme", "system");

        // Применяем сохраненную тему
        ThemeUtils.applyTheme(themeValue);
    }


}

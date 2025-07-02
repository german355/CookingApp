package com.example.cooking;

import android.app.Application;
import androidx.preference.PreferenceManager;
import android.content.SharedPreferences;
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

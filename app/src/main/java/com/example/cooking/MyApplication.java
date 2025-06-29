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
        // Получаем доступ к SharedPreferences с тем же именем, что и в SettingsFragment
        SharedPreferences sharedPreferences = getSharedPreferences("acs", MODE_PRIVATE);

        // Читаем сохраненное значение темы (по умолчанию - "system")
        String themeValue = sharedPreferences.getString("theme", "system");

        // Применяем сохраненную тему
        ThemeUtils.applyTheme(themeValue);
    }

    /**
     * Инициализирует клиент для Learning to Rank
     */

}

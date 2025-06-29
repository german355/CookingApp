package com.example.cooking.utils;

import android.app.Activity;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * Утилитный класс для управления темой приложения
 */
public class ThemeUtils {

    /**
     * Устанавливает режим темы в зависимости от выбранного значения
     * 
     * @param themeValue строковое значение темы ("light", "dark" или "system")
     */
    public static void applyTheme(String themeValue) {
        switch (themeValue) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            case "system":
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    /**
     * Применяет тему и пересоздает активити для немедленного применения
     * 
     * @param activity   текущая активити
     * @param themeValue строковое значение темы
     */
    public static void applyThemeWithRecreate(Activity activity, String themeValue) {
        applyTheme(themeValue);
        if (activity != null) {
            activity.recreate();
        }
    }
}
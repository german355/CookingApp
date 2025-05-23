package com.example.cooking.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class MySharedPreferences {

    // Имя файла настроек
    private static final String PREF_NAME = "acs";

    // Объект SharedPreferences и его редактор
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    // Конструктор класса принимает контекст приложения или активности
    public MySharedPreferences(Context context) {
        // Получаем SharedPreferences с именем PREF_NAME в приватном режиме
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        // Получаем редактор для внесения изменений
        editor = sharedPreferences.edit();
    }

    // Сохранение строкового значения по ключу
    public void putString(String key, String value) {
        editor.putString(key, value);
        editor.apply(); // Применяем изменения асинхронно
    }

    // Получение строкового значения по ключу, если значение отсутствует — возвращается defaultValue
    public String getString(String key, String defaultValue) {
        return sharedPreferences.getString(key, defaultValue);
    }

    // Сохранение целочисленного значения по ключу
    public void putInt(String key, int value) {
        editor.putInt(key, value);
        editor.apply();
    }

    // Получение целочисленного значения по ключу, если значение отсутствует — возвращается defaultValue
    public int getInt(String key, int defaultValue) {
        return sharedPreferences.getInt(key, defaultValue);
    }

    // Сохранение вещественного значения (float) по ключу
    public void putFloat(String key, float value) {
        editor.putFloat(key, value);
        editor.apply();
    }

    // Получение ID текущего пользователя
    public String getUserId() {
        return getString("userId", "0");
    }
    
    // Получение вещественного значения (float) по ключу, если значение отсутствует — возвращается defaultValue
    public float getFloat(String key, float defaultValue) {
        return sharedPreferences.getFloat(key, defaultValue);
    }

    // Сохранение булевого значения по ключу
    public void putBoolean(String key, boolean value) {
        editor.putBoolean(key, value);
        editor.apply();
    }
    public void putLong(String key, long value) {
        editor.putLong(key, value);
        editor.apply();
    }
    public long getLong(String key, long defaultValue) {
        return sharedPreferences.getLong(key, defaultValue);
    }

    // Получение булевого значения по ключу, если значение отсутствует — возвращается defaultValue
    public boolean getBoolean(String key, boolean defaultValue) {
        return sharedPreferences.getBoolean(key, defaultValue);
    }

    // Удаление значения по указанному ключу
    public void remove(String key) {
        editor.remove(key);
        editor.apply();
    }

    // Очистка всех данных из SharedPreferences
    public void clear() {
        editor.clear();
        editor.apply();
    }
    
    /**
     * Получает данные текущего пользователя
     * @return объект User с данными пользователя
     */
    public User getUser() {
        String userId = getString("userId", "0");
        String username = getString("username", "");
        String email = getString("email", "");
        int permission = getInt("permission", 1);
        
        return new User(userId, username, email, permission);
    }
}

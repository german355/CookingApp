package com.example.cooking.utils;

import android.app.Activity;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

/**
 * Утилиты для безопасной работы с lifecycle компонентов
 */
public class LifecycleUtils {
    
    /**
     * Проверяет, можно ли безопасно выполнять операции с Activity
     * @param activity Activity для проверки
     * @return true если Activity активно и не завершается
     */
    public static boolean isActivitySafe(Activity activity) {
        return activity != null && 
               !activity.isFinishing() && 
               !activity.isDestroyed();
    }
    
    /**
     * Проверяет, можно ли безопасно выполнять операции с Fragment
     * @param fragment Fragment для проверки
     * @return true если Fragment добавлен и его активность безопасна
     */
    public static boolean isFragmentSafe(Fragment fragment) {
        return fragment != null && 
               fragment.isAdded() && 
               !fragment.isStateSaved() &&
               isActivitySafe(fragment.getActivity());
    }
    
    /**
     * Проверяет, находится ли Activity в активном состоянии
     * @param activity Activity для проверки
     * @return true если Activity в состоянии STARTED или выше
     */
    public static boolean isActivityActive(Activity activity) {
        if (!isActivitySafe(activity)) {
            return false;
        }
        
        try {
            if (activity instanceof androidx.appcompat.app.AppCompatActivity) {
                androidx.appcompat.app.AppCompatActivity compatActivity = 
                    (androidx.appcompat.app.AppCompatActivity) activity;
                return compatActivity.getLifecycle()
                    .getCurrentState()
                    .isAtLeast(Lifecycle.State.STARTED);
            }
        } catch (Exception e) {
            // Fallback на базовые проверки
            return !activity.isFinishing() && !activity.isDestroyed();
        }
        
        return true;
    }
    
    /**
     * Проверяет, находится ли Fragment в активном состоянии
     * @param fragment Fragment для проверки
     * @return true если Fragment в активном состоянии
     */
    public static boolean isFragmentActive(Fragment fragment) {
        if (!isFragmentSafe(fragment)) {
            return false;
        }
        
        try {
            return fragment.getLifecycle()
                .getCurrentState()
                .isAtLeast(Lifecycle.State.STARTED);
        } catch (Exception e) {
            // Fallback на базовые проверки
            return fragment.isAdded() && !fragment.isDetached();
        }
    }
    
    /**
     * Выполняет действие только если Activity безопасно
     * @param activity Activity для проверки
     * @param action действие для выполнения
     */
    public static void runIfActivitySafe(Activity activity, Runnable action) {
        if (isActivitySafe(activity) && action != null) {
            try {
                action.run();
            } catch (Exception e) {
                // Логируем ошибку, но не падаем
                android.util.Log.w("LifecycleUtils", "Error executing safe activity action", e);
            }
        }
    }
    
    /**
     * Выполняет действие только если Fragment безопасно
     * @param fragment Fragment для проверки
     * @param action действие для выполнения
     */
    public static void runIfFragmentSafe(Fragment fragment, Runnable action) {
        if (isFragmentSafe(fragment) && action != null) {
            try {
                action.run();
            } catch (Exception e) {
                // Логируем ошибку, но не падаем
                android.util.Log.w("LifecycleUtils", "Error executing safe fragment action", e);
            }
        }
    }
}
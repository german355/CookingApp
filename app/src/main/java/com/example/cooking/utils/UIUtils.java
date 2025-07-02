package com.example.cooking.utils;

import android.text.Editable;
import android.widget.EditText;
import android.widget.TextView;

/**
 * Утилиты для безопасной работы с UI компонентами
 */
public class UIUtils {
    
    /**
     * Безопасное получение текста из EditText с проверкой на null
     * @param editText EditText для получения текста
     * @return строка или пустая строка если EditText null или текст null
     */
    public static String getTextSafely(EditText editText) {
        if (editText == null) {
            return "";
        }
        Editable editable = editText.getText();
        return editable != null ? editable.toString() : "";
    }
    
    /**
     * Безопасное получение обрезанного текста из EditText
     * @param editText EditText для получения текста
     * @return обрезанная строка или пустая строка если EditText null или текст null
     */
    public static String getTextSafelyTrimmed(EditText editText) {
        return getTextSafely(editText).trim();
    }
    
    /**
     * Безопасное получение текста из TextView с проверкой на null
     * @param textView TextView для получения текста
     * @return строка или пустая строка если TextView null или текст null
     */
    public static String getTextSafely(TextView textView) {
        if (textView == null) {
            return "";
        }
        CharSequence text = textView.getText();
        return text != null ? text.toString() : "";
    }
    
    /**
     * Проверяет, не пуст ли текст в EditText
     * @param editText EditText для проверки
     * @return true если текст не пуст, false иначе
     */
    public static boolean hasText(EditText editText) {
        return !getTextSafelyTrimmed(editText).isEmpty();
    }
    
    /**
     * Проверяет, что все EditText содержат текст
     * @param editTexts массив EditText для проверки
     * @return true если все содержат текст, false иначе
     */
    public static boolean allHaveText(EditText... editTexts) {
        if (editTexts == null) {
            return false;
        }
        for (EditText editText : editTexts) {
            if (!hasText(editText)) {
                return false;
            }
        }
        return true;
    }
}
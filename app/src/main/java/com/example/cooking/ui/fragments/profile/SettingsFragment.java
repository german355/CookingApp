package com.example.cooking.ui.fragments.profile;

import android.os.Bundle;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import com.example.cooking.R;
import com.example.cooking.utils.ThemeUtils;
import android.content.Context;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        // Использовать тот же файл SharedPreferences для умного поиска
        getPreferenceManager().setSharedPreferencesName("acs");
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_PRIVATE);

        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        // Находим ListPreference для выбора темы
        ListPreference themePreference = findPreference("theme");

        if (themePreference != null) {
            // Устанавливаем слушатель изменений
            themePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    // Получаем новое значение темы
                    String themeValue = (String) newValue;

                    // Применяем новую тему и пересоздаем активити для немедленного применения
                    ThemeUtils.applyThemeWithRecreate(getActivity(), themeValue);

                    // Возвращаем true, чтобы система сохранила новое значение
                    return true;
                }
            });
        }
    }
}
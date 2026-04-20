package com.example.cooking.ui.fragments.profile;

import android.content.Context;
import android.os.Bundle;

import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.example.cooking.R;
import com.example.cooking.utils.MySharedPreferences;
import com.example.cooking.utils.ThemeUtils;

public class SettingsFragment extends PreferenceFragmentCompat {

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        getPreferenceManager().setSharedPreferencesName("acs");
        getPreferenceManager().setSharedPreferencesMode(Context.MODE_PRIVATE);

        setPreferencesFromResource(R.xml.root_preferences, rootKey);

        ListPreference themePreference = findPreference("theme");
        ListPreference measurementSystemPreference = findPreference(MySharedPreferences.KEY_MEASUREMENT_SYSTEM);

        if (themePreference != null) {
            themePreference.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
            themePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    ThemeUtils.applyThemeWithRecreate(getActivity(), (String) newValue);
                    return true;
                }
            });
        }

        if (measurementSystemPreference != null) {
            measurementSystemPreference.setSummaryProvider(ListPreference.SimpleSummaryProvider.getInstance());
            measurementSystemPreference.setValue(
                    MySharedPreferences.normalizeMeasurementSystem(measurementSystemPreference.getValue())
            );
        }
    }
}

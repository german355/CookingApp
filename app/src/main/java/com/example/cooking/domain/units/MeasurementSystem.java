package com.example.cooking.domain.units;

import java.util.Locale;

/**
 * Supported display systems for ingredient measurements.
 */
public enum MeasurementSystem {
    ORIGINAL("original"),
    METRIC("metric"),
    IMPERIAL("imperial");

    private final String preferenceValue;

    MeasurementSystem(String preferenceValue) {
        this.preferenceValue = preferenceValue;
    }

    public String getPreferenceValue() {
        return preferenceValue;
    }

    public static MeasurementSystem fromPreferenceValue(String value) {
        if (value == null) {
            return ORIGINAL;
        }

        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (MeasurementSystem measurementSystem : values()) {
            if (measurementSystem.preferenceValue.equals(normalized)) {
                return measurementSystem;
            }
        }
        return ORIGINAL;
    }
}

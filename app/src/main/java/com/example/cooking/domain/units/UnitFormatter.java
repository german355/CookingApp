package com.example.cooking.domain.units;

import java.util.Locale;

/**
 * Formats amount plus unit using a chosen measurement system.
 */
public final class UnitFormatter {
    private UnitFormatter() {
    }

    public static String format(float amount, String rawUnitValue, MeasurementSystem measurementSystem) {
        UnitResolution resolution = UnitNormalizer.resolve(rawUnitValue);
        if (resolution.getSupportedUnit() == SupportedUnit.TO_TASTE) {
            return UnitConverter.convert(amount, resolution, measurementSystem).getUnitLabel();
        }

        UnitConverter.ConversionResult conversionResult =
                UnitConverter.convert(amount, resolution, measurementSystem);
        String formattedAmount = formatAmount(conversionResult.getAmount());
        String unitLabel = conversionResult.getUnitLabel();

        if (unitLabel == null || unitLabel.trim().isEmpty()) {
            return formattedAmount;
        }
        return formattedAmount + " " + unitLabel;
    }

    static String formatAmount(float amount) {
        if (amount == (int) amount) {
            return String.format(Locale.getDefault(), "%d", (int) amount);
        }
        return String.format(Locale.getDefault(), "%.1f", amount);
    }
}

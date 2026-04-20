package com.example.cooking.domain.units;

/**
 * Converts supported units for stage-1 display use cases.
 */
public final class UnitConverter {
    private static final float GRAMS_TO_OUNCES = 0.03527396f;
    private static final float KILOGRAMS_TO_OUNCES = 35.27396f;
    private static final float MILLILITERS_TO_FLUID_OUNCES = 0.03381402f;
    private static final float LITERS_TO_FLUID_OUNCES = 33.81402f;

    private UnitConverter() {
    }

    public static ConversionResult convert(float amount, String rawUnitValue, MeasurementSystem targetSystem) {
        return convert(amount, UnitNormalizer.resolve(rawUnitValue), targetSystem);
    }

    public static ConversionResult convert(float amount, UnitResolution resolution, MeasurementSystem targetSystem) {
        MeasurementSystem resolvedTarget = targetSystem != null ? targetSystem : MeasurementSystem.ORIGINAL;
        UnitResolution resolvedUnit = resolution != null ? resolution : UnitNormalizer.resolve(null);
        String rawDisplayValue = resolvedUnit.getDisplayValue();

        if (resolvedTarget == MeasurementSystem.ORIGINAL || !resolvedUnit.isKnown()) {
            return new ConversionResult(amount, displayLabelForOriginal(resolvedUnit, rawDisplayValue), false);
        }

        SupportedUnit supportedUnit = resolvedUnit.getSupportedUnit();
        if (resolvedTarget == MeasurementSystem.METRIC) {
            if (supportedUnit.getSystemOrNull() == MeasurementSystem.METRIC) {
                return new ConversionResult(amount, supportedUnit.getCode(), false);
            }
            return new ConversionResult(amount, fallbackLabel(rawDisplayValue, supportedUnit), false);
        }

        switch (supportedUnit) {
            case GRAM:
                return new ConversionResult(amount * GRAMS_TO_OUNCES, "oz", true);
            case KILOGRAM:
                return new ConversionResult(amount * KILOGRAMS_TO_OUNCES, "oz", true);
            case MILLILITER:
                return new ConversionResult(amount * MILLILITERS_TO_FLUID_OUNCES, "fl oz", true);
            case LITER:
                return new ConversionResult(amount * LITERS_TO_FLUID_OUNCES, "fl oz", true);
            default:
                return new ConversionResult(amount, fallbackLabel(rawDisplayValue, supportedUnit), false);
        }
    }

    private static String fallbackLabel(String rawDisplayValue, SupportedUnit supportedUnit) {
        if (supportedUnit == null) {
            return rawDisplayValue != null ? rawDisplayValue : "";
        }

        if (rawDisplayValue != null && !rawDisplayValue.isEmpty() && !rawDisplayValue.equals(supportedUnit.getCode())) {
            return rawDisplayValue;
        }
        return supportedUnit.getDisplayLabel();
    }

    private static String displayLabelForOriginal(UnitResolution resolution, String rawDisplayValue) {
        if (!resolution.isKnown()) {
            return rawDisplayValue;
        }

        SupportedUnit supportedUnit = resolution.getSupportedUnit();
        if (rawDisplayValue != null && !rawDisplayValue.isEmpty() && !rawDisplayValue.equals(supportedUnit.getCode())) {
            return rawDisplayValue;
        }
        return supportedUnit.getDisplayLabel();
    }

    public static final class ConversionResult {
        private final float amount;
        private final String unitLabel;
        private final boolean converted;

        public ConversionResult(float amount, String unitLabel, boolean converted) {
            this.amount = amount;
            this.unitLabel = unitLabel;
            this.converted = converted;
        }

        public float getAmount() {
            return amount;
        }

        public String getUnitLabel() {
            return unitLabel;
        }

        public boolean isConverted() {
            return converted;
        }
    }
}

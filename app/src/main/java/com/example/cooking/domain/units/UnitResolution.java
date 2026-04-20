package com.example.cooking.domain.units;

/**
 * Immutable result of resolving a stored unit value.
 */
public final class UnitResolution {
    private final String rawValue;
    private final String normalizedValue;
    private final SupportedUnit supportedUnit;

    UnitResolution(String rawValue, String normalizedValue, SupportedUnit supportedUnit) {
        this.rawValue = rawValue;
        this.normalizedValue = normalizedValue;
        this.supportedUnit = supportedUnit;
    }

    public String getRawValue() {
        return rawValue;
    }

    public String getNormalizedValue() {
        return normalizedValue;
    }

    public SupportedUnit getSupportedUnit() {
        return supportedUnit;
    }

    public boolean isKnown() {
        return supportedUnit != null;
    }

    public boolean isConvertible() {
        return supportedUnit != null && supportedUnit.isConvertible();
    }

    public MeasurementSystem getSystemOrNull() {
        return supportedUnit != null ? supportedUnit.getSystemOrNull() : null;
    }

    public String getDisplayValue() {
        return SupportedUnit.sanitize(rawValue);
    }
}

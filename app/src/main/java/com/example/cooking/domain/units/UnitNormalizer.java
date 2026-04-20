package com.example.cooking.domain.units;

/**
 * Resolves canonical and legacy unit values without mutating stored data.
 */
public final class UnitNormalizer {
    private UnitNormalizer() {
    }

    public static UnitResolution resolve(String rawValue) {
        SupportedUnit supportedUnit = SupportedUnit.fromValue(rawValue);
        String normalized = SupportedUnit.sanitize(rawValue);
        if (supportedUnit != null) {
            normalized = supportedUnit.getCode();
        }
        return new UnitResolution(rawValue, normalized, supportedUnit);
    }

    public static boolean isAcceptableForValidation(String rawValue) {
        return !SupportedUnit.sanitize(rawValue).isEmpty();
    }
}

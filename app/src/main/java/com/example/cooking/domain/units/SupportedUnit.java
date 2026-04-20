package com.example.cooking.domain.units;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Canonical stage-1 units with legacy alias support.
 */
public enum SupportedUnit {
    PIECE("pcs", "\u0448\u0442.", null, false),
    GRAM("g", "\u0433", MeasurementSystem.METRIC, true),
    KILOGRAM("kg", "\u043a\u0433", MeasurementSystem.METRIC, true),
    MILLILITER("ml", "\u043c\u043b", MeasurementSystem.METRIC, true),
    LITER("l", "\u043b", MeasurementSystem.METRIC, true),
    TABLESPOON("tbsp", "\u0441\u0442.\u043b.", MeasurementSystem.IMPERIAL, false),
    TEASPOON("tsp", "\u0447.\u043b.", MeasurementSystem.IMPERIAL, false),
    CUP("cup", "\u0441\u0442\u0430\u043a\u0430\u043d", MeasurementSystem.IMPERIAL, false),
    TO_TASTE("to_taste", "\u043f\u043e \u0432\u043a\u0443\u0441\u0443", null, false);

    private static final Map<String, SupportedUnit> LOOKUP;

    static {
        Map<String, SupportedUnit> lookup = new HashMap<>();

        register(lookup, PIECE, "pcs", "pc", "piece", "pieces", "\u0448\u0442", "\u0448\u0442.");
        register(lookup, GRAM, "g", "gram", "grams", "\u0433", "\u0433\u0440", "\u0433\u0440.");
        register(lookup, KILOGRAM, "kg", "kilogram", "kilograms", "\u043a\u0433");
        register(lookup, MILLILITER, "ml", "milliliter", "milliliters", "\u043c\u043b");
        register(lookup, LITER, "l", "liter", "liters", "litre", "litres", "\u043b");
        register(lookup, TABLESPOON, "tbsp", "tablespoon", "tablespoons", "\u0441\u0442.\u043b", "\u0441\u0442.\u043b.");
        register(lookup, TEASPOON, "tsp", "teaspoon", "teaspoons", "\u0447.\u043b", "\u0447.\u043b.");
        register(lookup, CUP, "cup", "cups", "\u0441\u0442\u0430\u043a\u0430\u043d", "\u0441\u0442\u0430\u043a\u0430\u043d\u0430", "\u0441\u0442\u0430\u043a\u0430\u043d\u043e\u0432");
        register(lookup, TO_TASTE, "to_taste", "to taste", "\u043f\u043e \u0432\u043a\u0443\u0441\u0443");

        LOOKUP = Collections.unmodifiableMap(lookup);
    }

    private final String code;
    private final String displayLabel;
    private final MeasurementSystem systemOrNull;
    private final boolean convertible;

    SupportedUnit(String code, String displayLabel, MeasurementSystem systemOrNull, boolean convertible) {
        this.code = code;
        this.displayLabel = displayLabel;
        this.systemOrNull = systemOrNull;
        this.convertible = convertible;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayLabel() {
        return displayLabel;
    }

    public MeasurementSystem getSystemOrNull() {
        return systemOrNull;
    }

    public boolean isConvertible() {
        return convertible;
    }

    public static SupportedUnit fromValue(String value) {
        String normalized = normalizeLookupKey(value);
        if (normalized.isEmpty()) {
            return null;
        }
        return LOOKUP.get(normalized);
    }

    private static void register(Map<String, SupportedUnit> lookup, SupportedUnit unit, String... aliases) {
        for (String alias : aliases) {
            lookup.put(normalizeLookupKey(alias), unit);
        }
    }

    private static String normalizeLookupKey(String value) {
        String sanitized = sanitize(value);
        if (sanitized.isEmpty()) {
            return "";
        }
        return sanitized.toLowerCase(Locale.ROOT).replace(" ", "");
    }

    static String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().replaceAll("\\s+", " ");
    }
}

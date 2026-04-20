package com.example.cooking.domain.units;

import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UnitFormatterTest {

    @Test
    public void convert_translatesMetricMassAndVolumeToImperial() {
        UnitConverter.ConversionResult grams =
                UnitConverter.convert(500f, "g", MeasurementSystem.IMPERIAL);
        UnitConverter.ConversionResult liters =
                UnitConverter.convert(1f, "\u043b", MeasurementSystem.IMPERIAL);

        assertEquals(17.6f, grams.getAmount(), 0.05f);
        assertEquals("oz", grams.getUnitLabel());
        assertTrue(grams.isConverted());

        assertEquals(33.8f, liters.getAmount(), 0.05f);
        assertEquals("fl oz", liters.getUnitLabel());
        assertTrue(liters.isConverted());
    }

    @Test
    public void convert_leavesNonConvertibleUnitsUntouched() {
        UnitConverter.ConversionResult cups =
                UnitConverter.convert(2f, "cup", MeasurementSystem.IMPERIAL);

        assertEquals(2f, cups.getAmount(), 0.0f);
        assertEquals("\u0441\u0442\u0430\u043a\u0430\u043d", cups.getUnitLabel());
        assertFalse(cups.isConverted());
    }

    @Test
    public void format_preservesLegacyLabelInOriginalSystem() {
        assertEquals("500 \u0433", UnitFormatter.format(500f, "\u0433", MeasurementSystem.ORIGINAL));
    }

    @Test
    public void format_usesCanonicalUnitForKnownMetricValues() {
        assertEquals("500 g", UnitFormatter.format(500f, "\u0433", MeasurementSystem.METRIC));
    }

    @Test
    public void format_usesLocalizedLabelForCanonicalOriginalValues() {
        assertEquals("2 \u0441\u0442.\u043b.", UnitFormatter.format(2f, "tbsp", MeasurementSystem.ORIGINAL));
    }

    @Test
    public void format_keepsUnknownUnitsSafeAndReadable() {
        assertEquals("2 \u0449\u0435\u043f\u043e\u0442\u043a\u0430", UnitFormatter.format(2f, "\u0449\u0435\u043f\u043e\u0442\u043a\u0430", MeasurementSystem.IMPERIAL));
    }

    @Test
    public void format_roundsFractionalValuesToOneDecimalPlace() {
        assertEquals(
                String.format(Locale.getDefault(), "%.1f oz", 17.6f),
                UnitFormatter.format(500f, "g", MeasurementSystem.IMPERIAL)
        );
    }

    @Test
    public void format_usesCurrentLocaleForFractionalValues() {
        Locale previousLocale = Locale.getDefault();
        try {
            Locale.setDefault(new Locale("ru", "RU"));
            assertEquals("17,6 oz", UnitFormatter.format(500f, "g", MeasurementSystem.IMPERIAL));
        } finally {
            Locale.setDefault(previousLocale);
        }
    }

    @Test
    public void format_hidesAmountForToTaste() {
        assertEquals("\u043f\u043e \u0432\u043a\u0443\u0441\u0443", UnitFormatter.format(1f, "to_taste", MeasurementSystem.ORIGINAL));
    }

    @Test
    public void format_preservesLegacyToTasteLabelInOriginalSystem() {
        assertEquals("\u043f\u043e \u0432\u043a\u0443\u0441\u0443", UnitFormatter.format(1f, "\u043f\u043e \u0432\u043a\u0443\u0441\u0443", MeasurementSystem.ORIGINAL));
    }
}

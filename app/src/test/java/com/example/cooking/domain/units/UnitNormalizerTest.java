package com.example.cooking.domain.units;

import com.example.cooking.domain.entities.Ingredient;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class UnitNormalizerTest {

    @Test
    public void resolve_mapsLegacyLabelsToCanonicalUnits() {
        assertEquals("g", UnitNormalizer.resolve("\u0433").getNormalizedValue());
        assertEquals(SupportedUnit.GRAM, UnitNormalizer.resolve("\u0433").getSupportedUnit());

        assertEquals("tbsp", UnitNormalizer.resolve("\u0441\u0442.\u043b.").getNormalizedValue());
        assertEquals(SupportedUnit.TABLESPOON, UnitNormalizer.resolve("\u0441\u0442.\u043b.").getSupportedUnit());

        assertEquals("to_taste", UnitNormalizer.resolve("\u043f\u043e \u0432\u043a\u0443\u0441\u0443").getNormalizedValue());
        assertEquals(SupportedUnit.TO_TASTE, UnitNormalizer.resolve("\u043f\u043e \u0432\u043a\u0443\u0441\u0443").getSupportedUnit());
    }

    @Test
    public void resolve_preservesUnknownValues() {
        UnitResolution resolution = UnitNormalizer.resolve("\u0449\u0435\u043f\u043e\u0442\u043a\u0430");

        assertFalse(resolution.isKnown());
        assertNull(resolution.getSupportedUnit());
        assertEquals("\u0449\u0435\u043f\u043e\u0442\u043a\u0430", resolution.getRawValue());
        assertEquals("\u0449\u0435\u043f\u043e\u0442\u043a\u0430", resolution.getNormalizedValue());
    }

    @Test
    public void validation_acceptsCanonicalLegacyAndUnknownNonBlankUnits() {
        assertTrue(UnitNormalizer.isAcceptableForValidation("g"));
        assertTrue(UnitNormalizer.isAcceptableForValidation("\u0433"));
        assertTrue(UnitNormalizer.isAcceptableForValidation("custom-unit"));
        assertFalse(UnitNormalizer.isAcceptableForValidation("   "));
    }

    @Test
    public void ingredient_keepsRawTypeAndExposesDerivedNormalization() {
        Ingredient ingredient = new Ingredient();
        ingredient.setType("\u0433");

        assertEquals("\u0433", ingredient.getType());
        assertEquals("\u0433", ingredient.getUnit());
        assertEquals(SupportedUnit.GRAM, ingredient.getSupportedUnit());
        assertEquals("g", ingredient.getNormalizedType());
        assertTrue(ingredient.hasValidUnitValue());
    }

    @Test
    public void copyForPersistence_normalizesKnownUnitsWithoutMutatingOriginal() {
        Ingredient ingredient = new Ingredient();
        ingredient.setName("Sugar");
        ingredient.setCount(2f);
        ingredient.setType("\u0441\u0442.\u043b.");

        Ingredient persistedCopy = ingredient.copyForPersistence();

        assertEquals("\u0441\u0442.\u043b.", ingredient.getType());
        assertEquals("tbsp", persistedCopy.getType());
        assertEquals(ingredient.getName(), persistedCopy.getName());
        assertEquals(ingredient.getCount(), persistedCopy.getCount(), 0.0f);
    }
}

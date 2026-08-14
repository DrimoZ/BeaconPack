package dev.drimoz.portablebeacons.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The fuel rules are cheap to get subtly wrong and expensive when they are. */
class FuelBudgetTest {

    @Test
    void aDrawBelowOneUnitPerTickStillCosts() {
        // Rounded down this would be free forever, and a pack running on an empty buffer looks
        // exactly like a pack that is broken.
        assertEquals(1, FuelBudget.costFor(0.1, 2.0));
    }

    @Test
    void noDrawCostsNothing() {
        assertEquals(0, FuelBudget.costFor(0.0, 2.0));
    }

    @Test
    void fuelIsBurnedOnlyWhenItFitsWhole() {
        assertTrue(FuelBudget.accepts(0, 3600, 10800));
        assertTrue(FuelBudget.accepts(7200, 3600, 10800));
        // 7201 + 3600 overflows: burning it would destroy the surplus.
        assertFalse(FuelBudget.accepts(7201, 3600, 10800));
    }

    @Test
    void fuelDenserThanTheWholeBufferIsNeverBurned() {
        assertFalse(FuelBudget.accepts(0, 28800, 10800));
    }

    @Test
    void nonFuelIsRejected() {
        assertFalse(FuelBudget.accepts(0, 0, 10800));
    }
}

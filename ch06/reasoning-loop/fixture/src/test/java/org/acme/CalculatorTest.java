package org.acme;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CalculatorTest {
    private final Calculator calculator = new Calculator();

    @Test void sumOfPositiveValues() {
        assertEquals(6, calculator.sum(new int[]{1, 2, 3}));
    }

    @Test void sumOfEmptyArray() {
        assertEquals(0, calculator.sum(new int[]{}));
    }

    @Test void fractionalAverage() {
        assertEquals(2.5, calculator.average(new int[]{2, 3}), 0.000001);
    }

    @Test void averageRejectsEmptyArray() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.average(new int[]{}));
    }

    @Test void maximumOfNegativeValues() {
        assertEquals(-2, calculator.max(new int[]{-9, -2, -5}));
    }

    @Test void maximumRejectsEmptyArray() {
        assertThrows(IllegalArgumentException.class,
                () -> calculator.max(new int[]{}));
    }
}

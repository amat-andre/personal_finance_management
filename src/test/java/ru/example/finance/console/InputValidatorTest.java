package ru.example.finance.console;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class InputValidatorTest {
    private final InputValidator validator = new InputValidator();

    @Test
    void acceptsPointCommaAndZeroBudget() {
        assertEquals(new BigDecimal("12.34"), validator.parsePositiveAmount("12,34"));
        assertEquals(new BigDecimal("0.5"), validator.parsePositiveAmount(".5"));
        assertEquals(BigDecimal.ZERO, validator.parseNonNegativeAmount("0"));
    }

    @Test
    void rejectsInvalidAmounts() {
        assertThrows(IllegalArgumentException.class, () -> validator.parsePositiveAmount("abc"));
        assertThrows(IllegalArgumentException.class, () -> validator.parsePositiveAmount("0"));
        assertThrows(IllegalArgumentException.class, () -> validator.parsePositiveAmount("-10"));
        assertThrows(IllegalArgumentException.class, () -> validator.parsePositiveAmount("1.234"));
        assertThrows(IllegalArgumentException.class, () -> validator.parseNonNegativeAmount("-1"));
    }
}

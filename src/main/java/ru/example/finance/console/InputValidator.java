package ru.example.finance.console;

import java.math.BigDecimal;

/** Проверка денежных значений, введённых пользователем. */
public final class InputValidator {
    public BigDecimal parsePositiveAmount(String value) {
        BigDecimal amount = parseAmount(value);
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Сумма должна быть больше нуля");
        }
        return amount;
    }

    public BigDecimal parseNonNegativeAmount(String value) {
        BigDecimal amount = parseAmount(value);
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("Бюджет не может быть отрицательным");
        }
        return amount;
    }

    private BigDecimal parseAmount(String value) {
        if (value == null || !value.trim().matches("(?:\\d+(?:[.,]\\d{1,2})?|[.,]\\d{1,2})")) {
            throw new IllegalArgumentException(
                    "Некорректная сумма: используйте неотрицательное число, максимум два знака после запятой"
            );
        }
        return new BigDecimal(value.trim().replace(',', '.'));
    }
}

package ru.example.finance.service;

import ru.example.finance.model.Category;

import java.math.BigDecimal;
import java.util.Objects;

/** Состояние бюджета: лимит, расход и остаток (отрицательный при превышении). */
public record BudgetStatus(
        Category category,
        BigDecimal limit,
        BigDecimal spent,
        BigDecimal remaining
) {
    public BudgetStatus {
        Objects.requireNonNull(category);
        Objects.requireNonNull(limit);
        Objects.requireNonNull(spent);
        Objects.requireNonNull(remaining);
    }

    public boolean isExceeded() {
        return remaining.signum() < 0;
    }
}

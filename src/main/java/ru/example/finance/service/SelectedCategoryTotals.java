package ru.example.finance.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/** Итоги по выбранным категориям и список категорий, которые не найдены. */
public record SelectedCategoryTotals(
        BigDecimal totalIncome,
        BigDecimal totalExpense,
        List<String> missingCategories
) {
    public SelectedCategoryTotals {
        Objects.requireNonNull(totalIncome);
        Objects.requireNonNull(totalExpense);
        missingCategories = List.copyOf(missingCategories);
    }

    public boolean hasMissingCategories() {
        return !missingCategories.isEmpty();
    }
}

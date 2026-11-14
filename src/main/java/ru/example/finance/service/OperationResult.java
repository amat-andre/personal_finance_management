package ru.example.finance.service;

import ru.example.finance.model.FinancialOperation;

import java.util.List;
import java.util.Objects;

/** Добавленная операция и предупреждения, которые нужно показать пользователю. */
public record OperationResult(FinancialOperation operation, List<String> warnings) {
    public OperationResult {
        Objects.requireNonNull(operation);
        warnings = List.copyOf(warnings);
    }
}

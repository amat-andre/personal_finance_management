package ru.example.finance.service;

import ru.example.finance.model.FinancialOperation;

import java.util.List;
import java.util.Objects;

/** Расход отправителя, доход получателя и предупреждения отправителю. */
public record TransferResult(
        FinancialOperation senderExpense,
        FinancialOperation recipientIncome,
        List<String> warnings
) {
    public TransferResult {
        Objects.requireNonNull(senderExpense);
        Objects.requireNonNull(recipientIncome);
        warnings = List.copyOf(warnings);
    }
}

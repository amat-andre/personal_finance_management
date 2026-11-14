package ru.example.finance.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Неизменяемая финансовая операция пользователя. */
public final class FinancialOperation {
    private final UUID id;
    private final OperationType type;
    private final Category category;
    private final BigDecimal amount;
    private final String description;
    private final Instant createdAt;

    public FinancialOperation(
            UUID id,
            OperationType type,
            Category category,
            BigDecimal amount,
            String description,
            Instant createdAt
    ) {
        this.id = Objects.requireNonNull(id, "Идентификатор операции обязателен");
        this.type = Objects.requireNonNull(type, "Тип операции обязателен");
        this.category = Objects.requireNonNull(category, "Категория операции обязательна");
        this.amount = Objects.requireNonNull(amount, "Сумма операции обязательна");
        if (amount.signum() <= 0) {
            throw new IllegalArgumentException("Сумма операции должна быть больше нуля");
        }
        this.description = description == null ? "" : description.trim();
        this.createdAt = Objects.requireNonNull(createdAt, "Дата операции обязательна");
    }

    public static FinancialOperation create(
            OperationType type,
            Category category,
            BigDecimal amount,
            String description
    ) {
        return new FinancialOperation(
                UUID.randomUUID(), type, category, amount, description, Instant.now()
        );
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof FinancialOperation operation && id.equals(operation.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    public UUID getId() {
        return id;
    }

    public OperationType getType() {
        return type;
    }

    public Category getCategory() {
        return category;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

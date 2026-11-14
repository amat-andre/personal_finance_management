package ru.example.finance.model;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Кошелёк, привязанный к одному пользователю. */
public final class Wallet {
    private final String ownerLogin;
    private final List<FinancialOperation> operations = new ArrayList<>();
    private final Map<Category, BigDecimal> budgets = new LinkedHashMap<>();

    public Wallet(String ownerLogin) {
        if (ownerLogin == null || ownerLogin.isBlank()) {
            throw new IllegalArgumentException("Логин владельца кошелька обязателен");
        }
        this.ownerLogin = ownerLogin.trim();
    }

    public String getOwnerLogin() {
        return ownerLogin;
    }

    public List<FinancialOperation> getOperations() {
        return Collections.unmodifiableList(operations);
    }

    public Map<Category, BigDecimal> getBudgets() {
        return Collections.unmodifiableMap(new LinkedHashMap<>(budgets));
    }

    public void addOperation(FinancialOperation operation) {
        operations.add(Objects.requireNonNull(operation, "Операция обязательна"));
    }

    public void setBudget(Category category, BigDecimal limit) {
        Objects.requireNonNull(category, "Категория бюджета обязательна");
        Objects.requireNonNull(limit, "Лимит бюджета обязателен");
        if (limit.signum() < 0) {
            throw new IllegalArgumentException("Бюджет не может быть отрицательным");
        }
        budgets.put(category, limit);
    }

    /** Переименовывает категорию, сохраняя идентификаторы и время операций. */
    public void renameCategory(Category current, Category renamed) {
        for (int index = 0; index < operations.size(); index++) {
            FinancialOperation operation = operations.get(index);
            if (operation.getCategory().equals(current)) {
                operations.set(index, new FinancialOperation(
                        operation.getId(), operation.getType(), renamed, operation.getAmount(),
                        operation.getDescription(), operation.getCreatedAt()
                ));
            }
        }
        Map<Category, BigDecimal> renamedBudgets = new LinkedHashMap<>();
        budgets.forEach((category, limit) -> renamedBudgets.put(
                category.equals(current) ? renamed : category, limit
        ));
        budgets.clear();
        budgets.putAll(renamedBudgets);
    }

    public void deleteCategory(Category category) {
        operations.removeIf(operation -> operation.getCategory().equals(category));
        budgets.remove(category);
    }

    public void removeBudget(Category category) {
        budgets.remove(category);
    }
}

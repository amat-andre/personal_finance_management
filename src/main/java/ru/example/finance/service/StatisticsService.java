package ru.example.finance.service;

import ru.example.finance.model.Category;
import ru.example.finance.model.FinancialOperation;
import ru.example.finance.model.OperationType;
import ru.example.finance.model.Wallet;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Расчёт доходов, расходов, баланса и состояния бюджетов. */
public final class StatisticsService {

    public BigDecimal totalIncome(Wallet wallet) {
        return total(wallet, OperationType.INCOME);
    }

    public BigDecimal totalExpense(Wallet wallet) {
        return total(wallet, OperationType.EXPENSE);
    }

    public BigDecimal balance(Wallet wallet) {
        return totalIncome(wallet).subtract(totalExpense(wallet));
    }

    public Map<Category, BigDecimal> incomeByCategory(Wallet wallet) {
        return totalsByCategory(wallet, OperationType.INCOME);
    }

    public Map<Category, BigDecimal> expenseByCategory(Wallet wallet) {
        return totalsByCategory(wallet, OperationType.EXPENSE);
    }

    public List<BudgetStatus> budgetStatuses(Wallet wallet) {
        Objects.requireNonNull(wallet, "Кошелёк обязателен");
        Map<Category, BigDecimal> expenses = expenseByCategory(wallet);
        List<BudgetStatus> result = new ArrayList<>();
        wallet.getBudgets().forEach((category, limit) -> {
            BigDecimal spent = expenses.getOrDefault(category, BigDecimal.ZERO);
            result.add(new BudgetStatus(category, limit, spent, limit.subtract(spent)));
        });
        return List.copyOf(result);
    }

    public Optional<BudgetStatus> findBudgetStatus(Wallet wallet, Category category) {
        return budgetStatuses(wallet).stream()
                .filter(status -> status.category().equals(category))
                .findFirst();
    }

    public SelectedCategoryTotals totalsForCategories(
            Wallet wallet, Collection<String> selectedCategoryNames
    ) {
        Objects.requireNonNull(wallet, "Кошелёк обязателен");
        Objects.requireNonNull(selectedCategoryNames, "Список категорий обязателен");

        Set<String> knownCategories = knownCategoryKeys(wallet);
        Set<String> selectedKeys = new LinkedHashSet<>();
        List<String> missing = new ArrayList<>();
        for (String name : selectedCategoryNames) {
            if (name == null || name.isBlank()) {
                missing.add(name == null ? "null" : name.trim());
                continue;
            }
            String key = normalize(name);
            if (knownCategories.contains(key)) {
                selectedKeys.add(key);
            } else {
                missing.add(name.trim());
            }
        }

        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expense = BigDecimal.ZERO;
        for (FinancialOperation operation : wallet.getOperations()) {
            if (!selectedKeys.contains(normalize(operation.getCategory().getName()))) {
                continue;
            }
            if (operation.getType() == OperationType.INCOME) {
                income = income.add(operation.getAmount());
            } else {
                expense = expense.add(operation.getAmount());
            }
        }
        return new SelectedCategoryTotals(income, expense, missing);
    }

    private BigDecimal total(Wallet wallet, OperationType type) {
        Objects.requireNonNull(wallet, "Кошелёк обязателен");
        BigDecimal result = BigDecimal.ZERO;
        for (FinancialOperation operation : wallet.getOperations()) {
            if (operation.getType() == type) {
                result = result.add(operation.getAmount());
            }
        }
        return result;
    }

    private Map<Category, BigDecimal> totalsByCategory(Wallet wallet, OperationType type) {
        Objects.requireNonNull(wallet, "Кошелёк обязателен");
        Map<Category, BigDecimal> result = new LinkedHashMap<>();
        for (FinancialOperation operation : wallet.getOperations()) {
            if (operation.getType() == type) {
                result.merge(operation.getCategory(), operation.getAmount(), BigDecimal::add);
            }
        }
        return result;
    }

    private Set<String> knownCategoryKeys(Wallet wallet) {
        Set<String> result = new LinkedHashSet<>();
        for (FinancialOperation operation : wallet.getOperations()) {
            result.add(normalize(operation.getCategory().getName()));
        }
        for (Category category : wallet.getBudgets().keySet()) {
            result.add(normalize(category.getName()));
        }
        return result;
    }

    private String normalize(String categoryName) {
        return categoryName.trim().toLowerCase(Locale.ROOT);
    }
}

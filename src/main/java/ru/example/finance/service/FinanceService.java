package ru.example.finance.service;

import ru.example.finance.model.Category;
import ru.example.finance.model.FinancialOperation;
import ru.example.finance.model.OperationType;
import ru.example.finance.model.Wallet;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/** Операции с доходами, расходами, категориями и бюджетами одного кошелька. */
public final class FinanceService {
    private static final BigDecimal WARNING_THRESHOLD = new BigDecimal("0.8");

    private final StatisticsService statisticsService;

    public FinanceService(StatisticsService statisticsService) {
        this.statisticsService = Objects.requireNonNull(statisticsService);
    }

    public OperationResult addIncome(
            Wallet wallet, String categoryName, BigDecimal amount, String description
    ) {
        return addOperation(wallet, OperationType.INCOME, categoryName, amount, description);
    }

    public OperationResult addExpense(
            Wallet wallet, String categoryName, BigDecimal amount, String description
    ) {
        return addOperation(wallet, OperationType.EXPENSE, categoryName, amount, description);
    }

    public List<String> setBudget(Wallet wallet, String categoryName, BigDecimal limit) {
        Objects.requireNonNull(wallet, "Кошелёк обязателен");
        Category category = new Category(categoryName);
        wallet.setBudget(category, limit);
        return budgetWarnings(wallet, category, BigDecimal.ZERO);
    }

    public void removeBudget(Wallet wallet, String categoryName) {
        Objects.requireNonNull(wallet, "Кошелёк обязателен");
        Category category = new Category(categoryName);
        if (!wallet.getBudgets().containsKey(category)) {
            throw new IllegalArgumentException("Бюджет для категории не найден: " + category.getName());
        }
        wallet.removeBudget(category);
    }

    public void renameCategory(Wallet wallet, String currentName, String newName) {
        Objects.requireNonNull(wallet, "Кошелёк обязателен");
        Category current = findCategory(wallet, currentName);
        Category renamed = new Category(newName);
        if (!current.equals(renamed) && categoryStream(wallet)
                .anyMatch(category -> category.equals(renamed))) {
            throw new IllegalArgumentException("Категория с новым названием уже существует: " + newName);
        }
        wallet.renameCategory(current, renamed);
    }

    public void deleteCategory(Wallet wallet, String categoryName) {
        Objects.requireNonNull(wallet, "Кошелёк обязателен");
        Category category = findCategory(wallet, categoryName);
        wallet.deleteCategory(category);
    }

    private Category findCategory(Wallet wallet, String categoryName) {
        Category requested = new Category(categoryName);
        return categoryStream(wallet)
                .filter(category -> category.equals(requested))
                .findFirst()
                .orElseThrow(() -> new CategoryNotFoundException(requested.getName()));
    }

    private Stream<Category> categoryStream(Wallet wallet) {
        return Stream.concat(
                wallet.getOperations().stream().map(FinancialOperation::getCategory),
                wallet.getBudgets().keySet().stream()
        ).distinct();
    }

    private OperationResult addOperation(
            Wallet wallet,
            OperationType type,
            String categoryName,
            BigDecimal amount,
            String description
    ) {
        Objects.requireNonNull(wallet, "Кошелёк обязателен");
        Category category = new Category(categoryName);
        BigDecimal previousSpent = type == OperationType.EXPENSE
                ? statisticsService.expenseByCategory(wallet).getOrDefault(category, BigDecimal.ZERO)
                : BigDecimal.ZERO;
        FinancialOperation operation = FinancialOperation.create(type, category, amount, description);
        wallet.addOperation(operation);

        List<String> warnings = new ArrayList<>();
        if (type == OperationType.EXPENSE) {
            warnings.addAll(budgetWarnings(wallet, category, previousSpent));
        }
        if (statisticsService.totalExpense(wallet).compareTo(statisticsService.totalIncome(wallet)) > 0) {
            warnings.add("Внимание: общие расходы превысили доходы");
        } else if (statisticsService.balance(wallet).signum() == 0) {
            warnings.add("Внимание: текущий баланс равен нулю");
        }
        return new OperationResult(operation, warnings);
    }

    private List<String> budgetWarnings(
            Wallet wallet, Category category, BigDecimal previousSpent
    ) {
        Optional<BudgetStatus> optionalStatus = statisticsService.findBudgetStatus(wallet, category);
        if (optionalStatus.isEmpty()) {
            return List.of();
        }
        BudgetStatus status = optionalStatus.get();
        if (status.isExceeded()) {
            return List.of(
                    "Внимание: превышен бюджет категории «" + status.category().getName()
                            + "» на " + status.remaining().abs().toPlainString()
            );
        }
        BigDecimal threshold = status.limit().multiply(WARNING_THRESHOLD);
        if (status.limit().signum() > 0
                && status.spent().compareTo(threshold) >= 0
                && previousSpent.compareTo(threshold) < 0) {
            return List.of(
                    "Внимание: израсходовано 80% или более бюджета категории «"
                            + status.category().getName() + "» (" + status.spent().toPlainString()
                            + " из " + status.limit().toPlainString() + ")"
            );
        }
        return List.of();
    }
}

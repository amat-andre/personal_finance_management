package ru.example.finance.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.example.finance.model.Category;
import ru.example.finance.model.OperationType;
import ru.example.finance.model.Wallet;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinanceServiceTest {
    private Wallet wallet;
    private FinanceService financeService;
    private StatisticsService statisticsService;

    @BeforeEach
    void setUp() {
        wallet = new Wallet("alice");
        statisticsService = new StatisticsService();
        financeService = new FinanceService(statisticsService);
    }

    @Test
    void addsIncomeAndExpenseToWallet() {
        OperationResult income = financeService.addIncome(
                wallet, "Зарплата", new BigDecimal("50000.00"), "Работа"
        );
        OperationResult expense = financeService.addExpense(
                wallet, "Еда", new BigDecimal("1200.50"), "Магазин"
        );

        assertEquals(OperationType.INCOME, income.operation().getType());
        assertEquals(OperationType.EXPENSE, expense.operation().getType());
        assertEquals(2, wallet.getOperations().size());
        assertEquals(new BigDecimal("48799.50"), statisticsService.balance(wallet));
        assertTrue(income.warnings().isEmpty());
    }

    @Test
    void calculatesTotalsAndRemainingBudgets() {
        addExampleOperations();
        financeService.setBudget(wallet, "Еда", new BigDecimal("4000.00"));
        financeService.setBudget(wallet, "Развлечения", new BigDecimal("3000.00"));
        financeService.setBudget(wallet, "Коммунальные услуги", new BigDecimal("2500.00"));

        assertEquals(new BigDecimal("63000.00"), statisticsService.totalIncome(wallet));
        assertEquals(new BigDecimal("60000.00"),
                statisticsService.incomeByCategory(wallet).get(new Category("Зарплата")));
        assertEquals(new BigDecimal("8300.00"), statisticsService.totalExpense(wallet));
        assertEquals(new BigDecimal("3200.00"), remaining("Еда"));
        assertEquals(new BigDecimal("0.00"), remaining("Развлечения"));
        assertEquals(new BigDecimal("-500.00"), remaining("Коммунальные услуги"));
    }

    @Test
    void warnsWhenBudgetIsExceeded() {
        financeService.addIncome(wallet, "Зарплата", new BigDecimal("1000"), "");
        financeService.setBudget(wallet, "Еда", new BigDecimal("100"));

        OperationResult result = financeService.addExpense(wallet, "Еда", new BigDecimal("101"), "");

        assertTrue(result.warnings().stream()
                .anyMatch(warning -> warning.contains("превышен бюджет") && warning.contains("1")));
        assertFalse(result.warnings().stream()
                .anyMatch(warning -> warning.contains("расходы превысили доходы")));
    }

    @Test
    void warnsWhenExpensesExceedIncome() {
        financeService.addIncome(wallet, "Зарплата", new BigDecimal("100"), "");

        OperationResult result = financeService.addExpense(wallet, "Еда", new BigDecimal("150"), "");

        assertTrue(result.warnings().stream()
                .anyMatch(warning -> warning.contains("расходы превысили доходы")));
    }

    @Test
    void settingBudgetBelowExistingExpensesReturnsWarning() {
        financeService.addIncome(wallet, "Зарплата", new BigDecimal("1000"), "");
        financeService.addExpense(wallet, "Еда", new BigDecimal("300"), "");

        List<String> warnings = financeService.setBudget(wallet, "Еда", new BigDecimal("200"));

        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("100"));
    }

    @Test
    void rejectsInvalidOperationAndBudgetData() {
        assertThrows(IllegalArgumentException.class,
                () -> financeService.addIncome(wallet, " ", new BigDecimal("10"), ""));
        assertThrows(IllegalArgumentException.class,
                () -> financeService.addExpense(wallet, "Еда", BigDecimal.ZERO, ""));
        assertThrows(IllegalArgumentException.class,
                () -> financeService.setBudget(wallet, "Еда", new BigDecimal("-1")));
        assertTrue(wallet.getOperations().isEmpty());
    }

    private void addExampleOperations() {
        financeService.addExpense(wallet, "Еда", new BigDecimal("300.00"), "");
        financeService.addExpense(wallet, "Еда", new BigDecimal("500.00"), "");
        financeService.addExpense(wallet, "Развлечения", new BigDecimal("3000.00"), "");
        financeService.addExpense(wallet, "Коммунальные услуги", new BigDecimal("3000.00"), "");
        financeService.addExpense(wallet, "Такси", new BigDecimal("1500.00"), "");
        financeService.addIncome(wallet, "Зарплата", new BigDecimal("20000.00"), "");
        financeService.addIncome(wallet, "Зарплата", new BigDecimal("40000.00"), "");
        financeService.addIncome(wallet, "Бонус", new BigDecimal("3000.00"), "");
    }

    private BigDecimal remaining(String category) {
        return statisticsService.findBudgetStatus(wallet, new Category(category))
                .orElseThrow().remaining();
    }
}

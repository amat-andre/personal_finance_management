package ru.example.finance.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.example.finance.model.Category;
import ru.example.finance.model.FinancialOperation;
import ru.example.finance.model.Wallet;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryEditingTest {
    private Wallet wallet;
    private FinanceService financeService;
    private StatisticsService statisticsService;

    @BeforeEach
    void setUp() {
        wallet = new Wallet("alice");
        statisticsService = new StatisticsService();
        financeService = new FinanceService(statisticsService);
        financeService.addExpense(wallet, "Еда", new BigDecimal("100"), "Магазин");
        financeService.setBudget(wallet, "Еда", new BigDecimal("500"));
    }

    @Test
    void renamePreservesOperationIdentityAndMovesBudget() {
        UUID operationId = wallet.getOperations().get(0).getId();

        financeService.renameCategory(wallet, "еда", "Продукты");

        FinancialOperation renamed = wallet.getOperations().get(0);
        assertEquals(operationId, renamed.getId());
        assertEquals(new Category("Продукты"), renamed.getCategory());
        assertTrue(wallet.getBudgets().containsKey(new Category("Продукты")));
        assertFalse(statisticsService.findBudgetStatus(wallet, new Category("Еда")).isPresent());
        assertEquals(new BigDecimal("100"), renamed.getAmount());
    }

    @Test
    void renameRejectsMissingCategoryAndNameCollision() {
        financeService.addExpense(wallet, "Такси", new BigDecimal("50"), "");

        assertThrows(CategoryNotFoundException.class,
                () -> financeService.renameCategory(wallet, "Отпуск", "Путешествие"));
        assertThrows(IllegalArgumentException.class,
                () -> financeService.renameCategory(wallet, "Еда", "такси"));
        assertEquals(2, wallet.getOperations().size());
        assertTrue(wallet.getBudgets().containsKey(new Category("Еда")));
    }

    @Test
    void deleteRemovesCategoryOperationsAndBudget() {
        financeService.addExpense(wallet, "Такси", new BigDecimal("50"), "");

        financeService.deleteCategory(wallet, "Еда");

        assertEquals(1, wallet.getOperations().size());
        assertEquals(new Category("Такси"), wallet.getOperations().get(0).getCategory());
        assertTrue(wallet.getBudgets().isEmpty());
        assertEquals(new BigDecimal("50"), statisticsService.totalExpense(wallet));
        assertThrows(CategoryNotFoundException.class,
                () -> financeService.deleteCategory(wallet, "Еда"));
    }

    @Test
    void updatesAndRemovesBudget() {
        financeService.setBudget(wallet, "Еда", new BigDecimal("750"));

        assertEquals(new BigDecimal("750"),
                statisticsService.findBudgetStatus(wallet, new Category("Еда")).orElseThrow().limit());
        financeService.removeBudget(wallet, "Еда");
        assertTrue(wallet.getBudgets().isEmpty());
        assertThrows(IllegalArgumentException.class,
                () -> financeService.removeBudget(wallet, "Еда"));
    }
}

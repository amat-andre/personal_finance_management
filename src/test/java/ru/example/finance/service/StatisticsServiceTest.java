package ru.example.finance.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.example.finance.model.Wallet;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StatisticsServiceTest {
    private Wallet wallet;
    private FinanceService financeService;
    private StatisticsService statisticsService;

    @BeforeEach
    void setUp() {
        wallet = new Wallet("alice");
        statisticsService = new StatisticsService();
        financeService = new FinanceService(statisticsService);
        financeService.addIncome(wallet, "Зарплата", new BigDecimal("1000"), "");
        financeService.addIncome(wallet, "Бонус", new BigDecimal("200"), "");
        financeService.addExpense(wallet, "Еда", new BigDecimal("100"), "");
        financeService.addExpense(wallet, "Такси", new BigDecimal("50"), "");
    }

    @Test
    void calculatesTotalsForSeveralSelectedCategories() {
        SelectedCategoryTotals result = statisticsService.totalsForCategories(
                wallet, List.of("Зарплата", "еда")
        );

        assertEquals(new BigDecimal("1000"), result.totalIncome());
        assertEquals(new BigDecimal("100"), result.totalExpense());
        assertTrue(result.missingCategories().isEmpty());
    }

    @Test
    void reportsMissingCategoriesWithoutLosingKnownTotals() {
        SelectedCategoryTotals result = statisticsService.totalsForCategories(
                wallet, List.of("Зарплата", "Путешествия", "Такси")
        );

        assertEquals(new BigDecimal("1000"), result.totalIncome());
        assertEquals(new BigDecimal("50"), result.totalExpense());
        assertEquals(List.of("Путешествия"), result.missingCategories());
        assertTrue(result.hasMissingCategories());
    }

    @Test
    void categoryWithBudgetButNoOperationsIsKnown() {
        financeService.setBudget(wallet, "Развлечения", new BigDecimal("500"));

        SelectedCategoryTotals result = statisticsService.totalsForCategories(
                wallet, List.of("Развлечения")
        );

        assertEquals(BigDecimal.ZERO, result.totalExpense());
        assertTrue(result.missingCategories().isEmpty());
    }
}

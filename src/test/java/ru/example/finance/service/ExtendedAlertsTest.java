package ru.example.finance.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.example.finance.model.Wallet;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtendedAlertsTest {
    private Wallet wallet;
    private FinanceService financeService;

    @BeforeEach
    void setUp() {
        wallet = new Wallet("alice");
        StatisticsService statisticsService = new StatisticsService();
        financeService = new FinanceService(statisticsService);
        financeService.addIncome(wallet, "Зарплата", new BigDecimal("1000"), "");
        financeService.setBudget(wallet, "Еда", new BigDecimal("100"));
    }

    @Test
    void warnsOnlyWhenCrossingEightyPercentThreshold() {
        OperationResult belowThreshold = financeService.addExpense(
                wallet, "Еда", new BigDecimal("70"), ""
        );
        OperationResult atThreshold = financeService.addExpense(
                wallet, "Еда", new BigDecimal("10"), ""
        );
        OperationResult aboveThreshold = financeService.addExpense(
                wallet, "Еда", new BigDecimal("10"), ""
        );

        assertFalse(belowThreshold.warnings().stream().anyMatch(warning -> warning.contains("80%")));
        assertTrue(atThreshold.warnings().stream().anyMatch(warning -> warning.contains("80%")));
        assertFalse(aboveThreshold.warnings().stream().anyMatch(warning -> warning.contains("80%")));
    }

    @Test
    void warnsAboutZeroBalance() {
        Wallet emptyWallet = new Wallet("bob");
        financeService.addIncome(emptyWallet, "Зарплата", new BigDecimal("100"), "");

        OperationResult result = financeService.addExpense(
                emptyWallet, "Еда", new BigDecimal("100"), ""
        );

        assertTrue(result.warnings().stream().anyMatch(warning -> warning.contains("баланс равен нулю")));
        assertFalse(result.warnings().stream().anyMatch(warning -> warning.contains("превысили доходы")));
    }

    @Test
    void settingBudgetAtCurrentEightyPercentUsageReturnsWarning() {
        financeService.addExpense(wallet, "Такси", new BigDecimal("80"), "");

        var warnings = financeService.setBudget(wallet, "Такси", new BigDecimal("100"));

        assertTrue(warnings.stream().anyMatch(warning -> warning.contains("80%")));
    }
}

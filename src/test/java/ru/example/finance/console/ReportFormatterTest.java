package ru.example.finance.console;

import org.junit.jupiter.api.Test;
import ru.example.finance.model.Wallet;
import ru.example.finance.service.FinanceService;
import ru.example.finance.service.StatisticsService;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportFormatterTest {
    @Test
    void formatsCategoryAndBudgetStatisticsAsTables() {
        Wallet wallet = new Wallet("alice");
        StatisticsService statistics = new StatisticsService();
        FinanceService financeService = new FinanceService(statistics);
        financeService.addExpense(wallet, "Еда", new BigDecimal("300"), "");
        financeService.setBudget(wallet, "Еда", new BigDecimal("1000"));

        String report = new ReportFormatter().formatFullReport(wallet, statistics);

        assertTrue(report.contains("| Категория | Сумма  |"));
        assertTrue(report.contains("| Еда       | 300.00 |"));
        assertTrue(report.contains("Оставшийся бюджет"));
        assertTrue(report.contains("700.00"));
        assertTrue(report.contains("+-----------+--------+"));
    }
}

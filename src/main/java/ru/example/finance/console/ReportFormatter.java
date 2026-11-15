package ru.example.finance.console;

import ru.example.finance.model.Category;
import ru.example.finance.model.Wallet;
import ru.example.finance.service.BudgetStatus;
import ru.example.finance.service.SelectedCategoryTotals;
import ru.example.finance.service.StatisticsService;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Табличное представление статистики для терминала и файла. */
public final class ReportFormatter {

    public String formatFullReport(Wallet wallet, StatisticsService statistics) {
        StringBuilder report = new StringBuilder();
        report.append("Общий доход: ").append(money(statistics.totalIncome(wallet))).append('\n');
        appendCategoryTotals(report, "Доходы по категориям:", statistics.incomeByCategory(wallet));
        report.append("Общие расходы: ").append(money(statistics.totalExpense(wallet))).append('\n');
        appendCategoryTotals(report, "Расходы по категориям:", statistics.expenseByCategory(wallet));
        report.append("Текущий баланс: ").append(money(statistics.balance(wallet))).append('\n');
        report.append("Бюджет по категориям:\n");
        List<BudgetStatus> statuses = statistics.budgetStatuses(wallet);
        if (statuses.isEmpty()) {
            report.append("Бюджеты не установлены\n");
        } else {
            List<List<String>> rows = new ArrayList<>();
            for (BudgetStatus status : statuses) {
                rows.add(List.of(
                        status.category().getName(), money(status.limit()),
                        money(status.spent()), money(status.remaining())
                ));
            }
            appendTable(report, List.of("Категория", "Лимит", "Расход", "Оставшийся бюджет"), rows);
        }
        return report.toString();
    }

    public String formatSelectedCategories(SelectedCategoryTotals totals) {
        StringBuilder report = new StringBuilder();
        report.append("Доходы по выбранным категориям: ")
                .append(money(totals.totalIncome())).append('\n');
        report.append("Расходы по выбранным категориям: ")
                .append(money(totals.totalExpense())).append('\n');
        if (totals.hasMissingCategories()) {
            report.append("Категории не найдены: ")
                    .append(String.join(", ", totals.missingCategories())).append('\n');
        }
        return report.toString();
    }

    private void appendCategoryTotals(
            StringBuilder report, String title, Map<Category, BigDecimal> totals
    ) {
        report.append(title).append('\n');
        if (totals.isEmpty()) {
            report.append("Нет операций\n");
            return;
        }
        List<List<String>> rows = new ArrayList<>();
        totals.forEach((category, amount) -> rows.add(
                List.of(category.getName(), money(amount))
        ));
        appendTable(report, List.of("Категория", "Сумма"), rows);
    }

    private void appendTable(StringBuilder report, List<String> headers, List<List<String>> rows) {
        int[] widths = new int[headers.size()];
        for (int column = 0; column < headers.size(); column++) {
            widths[column] = headers.get(column).length();
        }
        for (List<String> row : rows) {
            for (int column = 0; column < row.size(); column++) {
                widths[column] = Math.max(widths[column], row.get(column).length());
            }
        }
        appendTableLine(report, widths);
        appendTableRow(report, headers, widths);
        appendTableLine(report, widths);
        for (List<String> row : rows) {
            appendTableRow(report, row, widths);
        }
        appendTableLine(report, widths);
    }

    private void appendTableLine(StringBuilder report, int[] widths) {
        report.append('+');
        for (int width : widths) {
            report.append("-".repeat(width + 2)).append('+');
        }
        report.append('\n');
    }

    private void appendTableRow(StringBuilder report, List<String> values, int[] widths) {
        report.append('|');
        for (int column = 0; column < values.size(); column++) {
            report.append(' ').append(values.get(column))
                    .append(" ".repeat(widths[column] - values.get(column).length() + 1))
                    .append('|');
        }
        report.append('\n');
    }

    private String money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}

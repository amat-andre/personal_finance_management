package ru.example.finance.console;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.example.finance.model.User;
import ru.example.finance.persistence.JsonUserStorage;
import ru.example.finance.repository.InMemoryUserRepository;
import ru.example.finance.service.AuthService;
import ru.example.finance.service.FinanceService;
import ru.example.finance.service.PasswordHasher;
import ru.example.finance.service.StatisticsService;
import ru.example.finance.service.TransferService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleApplicationTest {
    @TempDir
    Path tempDirectory;

    @Test
    void executesFullConsoleFlowAndSavesOnExit() throws Exception {
        String output = runConsole("""
                register alice secret
                login alice secret
                income "Зарплата" 60000
                expense "Еда" 300
                budget "Еда" 4000
                stats
                exit
                """);

        assertTrue(output.contains("Общий доход: 60000.00"));
        assertTrue(output.contains("Общие расходы: 300.00"));
        assertTrue(output.contains("Оставшийся бюджет"));
        assertTrue(output.contains("| Еда"));
        assertTrue(output.contains("3700.00"));
        User saved = new JsonUserStorage(tempDirectory).load("alice").orElseThrow();
        assertEquals(2, saved.getWallet().getOperations().size());
        assertEquals(new BigDecimal("59700"), new StatisticsService().balance(saved.getWallet()));
    }

    @Test
    void loadsWalletWithLoginAfterRestart() {
        runConsole("""
                register alice secret
                login alice secret
                income "Зарплата" 1000
                exit
                """);

        String output = runConsole("""
                login alice secret
                balance
                exit
                """);

        assertTrue(output.contains("Добро пожаловать, alice!"));
        assertTrue(output.contains("Текущий баланс: 1000"));
    }

    @Test
    void validatesInputReportsUnknownCategoryAndContinuesLoop() {
        String output = runConsole("""
                register alice secret
                login alice secret
                income "Зарплата" abc
                income "Зарплата" 1000,50
                stats "Зарплата" "Отпуск"
                exit
                """);

        assertTrue(output.contains("Ошибка: Некорректная сумма"));
        assertTrue(output.contains("Доход добавлен: 1000.50"));
        assertTrue(output.contains("Доходы по выбранным категориям: 1000.50"));
        assertTrue(output.contains("Категории не найдены: Отпуск"));
        assertTrue(output.contains("Работа приложения завершена."));
    }

    @Test
    void writesReportToFile() throws Exception {
        Path report = tempDirectory.resolve("reports/finance.txt");
        String commands = """
                register alice secret
                login alice secret
                expense "Еда" 150,25
                report "%s"
                exit
                """.formatted(report);

        String output = runConsole(commands);

        assertTrue(output.contains("Отчёт сохранён в файл:"));
        String reportText = Files.readString(report);
        assertTrue(reportText.contains("Общие расходы: 150.25"));
        assertTrue(reportText.contains("Текущий баланс: -150.25"));
    }

    @Test
    void warnsAboutExceededBudgetInConsole() {
        String output = runConsole("""
                register alice secret
                login alice secret
                budget "Еда" 100
                expense "Еда" 150
                exit
                """);

        assertTrue(output.contains("превышен бюджет категории «Еда» на 50"));
        assertTrue(output.contains("общие расходы превысили доходы"));
    }

    @Test
    void transfersMoneyBetweenUsersFromConsole() throws Exception {
        String output = runConsole("""
                register alice secret
                register bob secret
                login alice secret
                income "Зарплата" 1000
                transfer bob 250
                logout
                login bob secret
                balance
                exit
                """);

        assertTrue(output.contains("Перевод пользователю bob выполнен: 250"));
        assertTrue(output.contains("Текущий баланс: 250"));
        JsonUserStorage storage = new JsonUserStorage(tempDirectory);
        assertEquals(new BigDecimal("750"),
                new StatisticsService().balance(storage.load("alice").orElseThrow().getWallet()));
        assertEquals(new BigDecimal("250"),
                new StatisticsService().balance(storage.load("bob").orElseThrow().getWallet()));
    }

    private String runConsole(String commands) {
        JsonUserStorage storage = new JsonUserStorage(tempDirectory);
        StatisticsService statisticsService = new StatisticsService();
        InMemoryUserRepository repository = new InMemoryUserRepository();
        AuthService authService = new AuthService(repository, new PasswordHasher(), storage);
        FinanceService financeService = new FinanceService(statisticsService);
        TransferService transferService = new TransferService(repository, storage, financeService);
        ConsoleApplication application = new ConsoleApplication(
                authService, financeService, statisticsService, storage, transferService
        );
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        application.run(
                new ByteArrayInputStream(commands.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(output, true, StandardCharsets.UTF_8)
        );
        return output.toString(StandardCharsets.UTF_8);
    }
}

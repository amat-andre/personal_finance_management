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
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ConsoleEditingTest {
    @TempDir
    Path tempDirectory;

    @Test
    void editsCategoriesAndBudgetsThroughConsole() throws Exception {
        String output = runConsole("""
                register alice secret
                login alice secret
                expense "Еда" 100
                budget "Еда" 500
                rename-category "Еда" "Продукты"
                remove-budget "Продукты"
                stats
                delete-category "Продукты"
                exit
                """);

        assertTrue(output.contains("Категория переименована: Еда → Продукты"));
        assertTrue(output.contains("Бюджет категории удалён: Продукты"));
        assertTrue(output.contains("| Продукты"));
        assertTrue(output.contains("100.00"));
        User saved = new JsonUserStorage(tempDirectory).load("alice").orElseThrow();
        assertTrue(saved.getWallet().getOperations().isEmpty());
        assertTrue(saved.getWallet().getBudgets().isEmpty());
    }

    @Test
    void helpContainsDetailedExamples() {
        String output = runConsole("""
                help
                exit
                """);

        assertTrue(output.contains("Примеры:"));
        assertTrue(output.contains("income \"Зарплата\" 60000"));
        assertTrue(output.contains("rename-category"));
    }

    private String runConsole(String commands) {
        JsonUserStorage storage = new JsonUserStorage(tempDirectory);
        StatisticsService statistics = new StatisticsService();
        InMemoryUserRepository repository = new InMemoryUserRepository();
        AuthService authService = new AuthService(repository, new PasswordHasher(), storage);
        FinanceService financeService = new FinanceService(statistics);
        TransferService transferService = new TransferService(repository, storage, financeService);
        ConsoleApplication application = new ConsoleApplication(
                authService, financeService, statistics, storage, transferService
        );
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        application.run(
                new ByteArrayInputStream(commands.getBytes(StandardCharsets.UTF_8)),
                new PrintStream(output, true, StandardCharsets.UTF_8)
        );
        return output.toString(StandardCharsets.UTF_8);
    }
}

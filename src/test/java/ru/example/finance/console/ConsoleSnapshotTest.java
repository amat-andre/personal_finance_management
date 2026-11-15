package ru.example.finance.console;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
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

class ConsoleSnapshotTest {
    @TempDir
    Path tempDirectory;

    @Test
    void exportsAndRestoresWalletThroughConsole() throws Exception {
        Path snapshot = tempDirectory.resolve("snapshots/alice.json");
        String commands = """
                register alice secret
                login alice secret
                income "Зарплата" 1000
                expense "Еда" 50
                budget "Еда" 500
                export "%s"
                delete-category "Зарплата"
                delete-category "Еда"
                import "%s"
                logout
                login alice secret
                stats
                exit
                """.formatted(snapshot, snapshot);

        String output = runConsole(commands);

        assertTrue(output.contains("JSON-снимок экспортирован:"));
        assertTrue(output.contains("JSON-снимок импортирован. Пароль текущего пользователя сохранён."));
        assertTrue(output.contains("Общий доход: 1000.00"));
        assertTrue(output.contains("Общие расходы: 50.00"));
        var saved = new JsonUserStorage(tempDirectory).load("alice").orElseThrow();
        assertEquals(new BigDecimal("950"), new StatisticsService().balance(saved.getWallet()));
    }

    @Test
    void corruptedImportDoesNotReplaceCurrentWallet() throws Exception {
        Path broken = tempDirectory.resolve("broken.json");
        Files.writeString(broken, "broken");
        String commands = """
                register alice secret
                login alice secret
                income "Зарплата" 100
                import "%s"
                balance
                exit
                """.formatted(broken);

        String output = runConsole(commands);

        assertTrue(output.contains("Ошибка сохранения или чтения файла:"));
        assertTrue(output.contains("Текущий баланс: 100"));
        var saved = new JsonUserStorage(tempDirectory).load("alice").orElseThrow();
        assertEquals(new BigDecimal("100"), new StatisticsService().balance(saved.getWallet()));
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

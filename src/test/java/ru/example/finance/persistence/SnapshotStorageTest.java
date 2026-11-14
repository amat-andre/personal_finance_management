package ru.example.finance.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.example.finance.model.User;
import ru.example.finance.repository.InMemoryUserRepository;
import ru.example.finance.service.AuthService;
import ru.example.finance.service.FinanceService;
import ru.example.finance.service.PasswordHasher;
import ru.example.finance.service.StatisticsService;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SnapshotStorageTest {
    @TempDir
    Path tempDirectory;

    @Test
    void exportsAndImportsPortableSnapshot() throws Exception {
        JsonUserStorage storage = new JsonUserStorage(tempDirectory.resolve("data"));
        User user = createUser();
        Path snapshot = tempDirectory.resolve("exports/alice.json");

        storage.exportTo(user, snapshot);
        User imported = storage.importFrom(snapshot, "ALICE");

        assertEquals(user.getWallet().getOperations(), imported.getWallet().getOperations());
        assertEquals(user.getWallet().getBudgets(), imported.getWallet().getBudgets());
        assertEquals(new BigDecimal("950.00"),
                new StatisticsService().balance(imported.getWallet()));
    }

    @Test
    void rejectsSnapshotOfAnotherUser() throws Exception {
        JsonUserStorage storage = new JsonUserStorage(tempDirectory);
        Path snapshot = tempDirectory.resolve("alice.json");
        storage.exportTo(createUser(), snapshot);

        assertThrows(IOException.class, () -> storage.importFrom(snapshot, "bob"));
    }

    @Test
    void rejectsCorruptedSnapshot() throws Exception {
        JsonUserStorage storage = new JsonUserStorage(tempDirectory);
        Path snapshot = tempDirectory.resolve("broken.json");
        Files.writeString(snapshot, "{not valid json");

        assertThrows(IOException.class, () -> storage.importFrom(snapshot, "alice"));
    }

    private User createUser() {
        User user = new AuthService(new InMemoryUserRepository(), new PasswordHasher())
                .register("alice", "secret");
        FinanceService financeService = new FinanceService(new StatisticsService());
        financeService.addIncome(user.getWallet(), "Зарплата", new BigDecimal("1000.00"), "");
        financeService.addExpense(user.getWallet(), "Еда", new BigDecimal("50.00"), "");
        financeService.setBudget(user.getWallet(), "Еда", new BigDecimal("500.00"));
        return user;
    }
}

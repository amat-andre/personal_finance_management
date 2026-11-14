package ru.example.finance.persistence;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.example.finance.model.User;
import ru.example.finance.repository.InMemoryUserRepository;
import ru.example.finance.service.AuthService;
import ru.example.finance.service.FinanceService;
import ru.example.finance.service.PasswordHasher;
import ru.example.finance.service.StatisticsService;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonUserStorageTest {
    @TempDir
    Path tempDirectory;

    @Test
    void savesAndLoadsWalletWithoutLosingData() throws Exception {
        User original = createUserWithFinances();
        JsonUserStorage storage = new JsonUserStorage(tempDirectory);

        storage.save(original);
        User restored = storage.load("ALICE").orElseThrow();

        assertEquals(original.getLogin(), restored.getLogin());
        assertEquals(original.getPasswordHash(), restored.getPasswordHash());
        assertEquals(original.getWallet().getOperations(), restored.getWallet().getOperations());
        assertEquals(original.getWallet().getBudgets(), restored.getWallet().getBudgets());
        assertEquals(new BigDecimal("47500.00"),
                new StatisticsService().balance(restored.getWallet()));
    }

    @Test
    void fileContainsPasswordHashButNotPlainPassword() throws Exception {
        User user = createUserWithFinances();
        JsonUserStorage storage = new JsonUserStorage(tempDirectory);

        storage.save(user);

        Path file = Files.list(tempDirectory).findFirst().orElseThrow();
        String json = Files.readString(file);
        assertFalse(json.contains("secret"));
        assertTrue(json.contains("passwordHash"));
        assertNotEquals(user.getPasswordHash(), "secret");
    }

    @Test
    void authenticationLoadsSavedUserAfterRestart() throws Exception {
        User original = createUserWithFinances();
        JsonUserStorage storage = new JsonUserStorage(tempDirectory);
        storage.save(original);
        InMemoryUserRepository emptyRepository = new InMemoryUserRepository();
        AuthService restartedAuth = new AuthService(emptyRepository, new PasswordHasher(), storage);

        User authenticated = restartedAuth.authenticate("alice", "secret");

        assertEquals(3, authenticated.getWallet().getOperations().size());
        assertEquals(1, authenticated.getWallet().getBudgets().size());
        assertEquals(authenticated, emptyRepository.findByLogin("alice").orElseThrow());
    }

    @Test
    void registeringSameLoginAsSavedUserIsRejected() throws Exception {
        JsonUserStorage storage = new JsonUserStorage(tempDirectory);
        storage.save(createUserWithFinances());
        AuthService authService = new AuthService(
                new InMemoryUserRepository(), new PasswordHasher(), storage
        );

        try {
            authService.register("alice", "another-password");
        } catch (ru.example.finance.service.AuthenticationException exception) {
            assertTrue(exception.getMessage().contains("уже существует"));
            return;
        }
        throw new AssertionError("Ожидалось исключение о повторном логине");
    }

    @Test
    void unknownUserHasNoSavedData() throws Exception {
        JsonUserStorage storage = new JsonUserStorage(tempDirectory);

        assertTrue(storage.load("unknown").isEmpty());
    }

    private User createUserWithFinances() {
        AuthService authService = new AuthService(new InMemoryUserRepository(), new PasswordHasher());
        User user = authService.register("alice", "secret");
        FinanceService financeService = new FinanceService(new StatisticsService());
        financeService.addIncome(user.getWallet(), "Зарплата", new BigDecimal("50000.00"), "Работа");
        financeService.addExpense(user.getWallet(), "Еда", new BigDecimal("1500.00"), "Магазин");
        financeService.addExpense(user.getWallet(), "Такси", new BigDecimal("1000.00"), "Поездка");
        financeService.setBudget(user.getWallet(), "Еда", new BigDecimal("10000.00"));
        return user;
    }
}

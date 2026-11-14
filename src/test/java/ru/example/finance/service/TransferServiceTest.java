package ru.example.finance.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.example.finance.model.OperationType;
import ru.example.finance.model.User;
import ru.example.finance.persistence.JsonUserStorage;
import ru.example.finance.repository.InMemoryUserRepository;

import java.math.BigDecimal;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TransferServiceTest {
    @TempDir
    Path tempDirectory;

    private InMemoryUserRepository repository;
    private JsonUserStorage storage;
    private StatisticsService statisticsService;
    private TransferService transferService;
    private User alice;
    private User bob;

    @BeforeEach
    void setUp() throws Exception {
        repository = new InMemoryUserRepository();
        storage = new JsonUserStorage(tempDirectory);
        statisticsService = new StatisticsService();
        FinanceService financeService = new FinanceService(statisticsService);
        transferService = new TransferService(repository, storage, financeService);
        AuthService authService = new AuthService(repository, new PasswordHasher());
        alice = authService.register("alice", "secret");
        bob = authService.register("bob", "secret");
        financeService.addIncome(alice.getWallet(), "Зарплата", new BigDecimal("1000"), "");
        storage.save(alice);
        storage.save(bob);
    }

    @Test
    void recordsExpenseForSenderAndIncomeForRecipient() throws Exception {
        TransferResult result = transferService.transfer(alice, "BOB", new BigDecimal("250"));

        assertEquals(OperationType.EXPENSE, result.senderExpense().getType());
        assertEquals(OperationType.INCOME, result.recipientIncome().getType());
        assertEquals(new BigDecimal("750"), statisticsService.balance(alice.getWallet()));
        assertEquals(new BigDecimal("250"), statisticsService.balance(bob.getWallet()));
        assertTrue(result.senderExpense().getDescription().contains("bob"));
        assertTrue(result.recipientIncome().getDescription().contains("alice"));
    }

    @Test
    void persistsBothWalletsAfterTransfer() throws Exception {
        transferService.transfer(alice, "bob", new BigDecimal("300"));

        User savedAlice = storage.load("alice").orElseThrow();
        User savedBob = storage.load("bob").orElseThrow();
        assertEquals(new BigDecimal("700"), statisticsService.balance(savedAlice.getWallet()));
        assertEquals(new BigDecimal("300"), statisticsService.balance(savedBob.getWallet()));
    }

    @Test
    void loadsRecipientFromStorageWhenAbsentInMemory() throws Exception {
        InMemoryUserRepository restartedRepository = new InMemoryUserRepository();
        restartedRepository.save(alice);
        TransferService restartedTransfer = new TransferService(
                restartedRepository, storage, new FinanceService(statisticsService)
        );

        restartedTransfer.transfer(alice, "bob", new BigDecimal("100"));

        assertEquals(new BigDecimal("100"),
                statisticsService.balance(storage.load("bob").orElseThrow().getWallet()));
        assertTrue(restartedRepository.findByLogin("bob").isPresent());
    }

    @Test
    void rejectsSelfTransferAndUnknownRecipientWithoutChangingWallets() {
        int aliceOperations = alice.getWallet().getOperations().size();
        int bobOperations = bob.getWallet().getOperations().size();

        assertThrows(TransferException.class,
                () -> transferService.transfer(alice, "alice", new BigDecimal("10")));
        assertThrows(TransferException.class,
                () -> transferService.transfer(alice, "unknown", new BigDecimal("10")));
        assertEquals(aliceOperations, alice.getWallet().getOperations().size());
        assertEquals(bobOperations, bob.getWallet().getOperations().size());
    }

    @Test
    void rejectsInvalidAmountWithoutChangingWallets() {
        int aliceOperations = alice.getWallet().getOperations().size();
        int bobOperations = bob.getWallet().getOperations().size();

        assertThrows(IllegalArgumentException.class,
                () -> transferService.transfer(alice, "bob", BigDecimal.ZERO));
        assertEquals(aliceOperations, alice.getWallet().getOperations().size());
        assertEquals(bobOperations, bob.getWallet().getOperations().size());
    }
}

package ru.example.finance.service;

import ru.example.finance.model.User;
import ru.example.finance.persistence.UserStorage;
import ru.example.finance.repository.UserRepository;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Objects;

/** Переводы между кошельками с одновременной фиксацией расхода и дохода. */
public final class TransferService {
    private static final String TRANSFER_CATEGORY = "Переводы";

    private final UserRepository userRepository;
    private final UserStorage userStorage;
    private final FinanceService financeService;

    public TransferService(
            UserRepository userRepository, UserStorage userStorage, FinanceService financeService
    ) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.userStorage = Objects.requireNonNull(userStorage);
        this.financeService = Objects.requireNonNull(financeService);
    }

    public synchronized TransferResult transfer(
            User sender, String recipientLogin, BigDecimal amount
    ) throws IOException {
        Objects.requireNonNull(sender, "Отправитель обязателен");
        String normalizedRecipient = User.normalizeLogin(recipientLogin);
        if (sender.getLogin().equals(normalizedRecipient)) {
            throw new TransferException("Нельзя перевести деньги самому себе");
        }

        User recipient = userRepository.findByLogin(normalizedRecipient).orElse(null);
        if (recipient == null) {
            recipient = userStorage.load(normalizedRecipient)
                    .orElseThrow(() -> new TransferException("Получатель не найден: " + normalizedRecipient));
            userRepository.save(recipient);
        }

        OperationResult expense = financeService.addExpense(
                sender.getWallet(), TRANSFER_CATEGORY, amount,
                "Перевод пользователю " + recipient.getLogin()
        );
        OperationResult income = financeService.addIncome(
                recipient.getWallet(), TRANSFER_CATEGORY, amount,
                "Перевод от пользователя " + sender.getLogin()
        );
        userStorage.save(sender);
        userStorage.save(recipient);
        return new TransferResult(expense.operation(), income.operation(), expense.warnings());
    }
}

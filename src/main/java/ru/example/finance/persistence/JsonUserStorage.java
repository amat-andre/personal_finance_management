package ru.example.finance.persistence;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import ru.example.finance.model.Category;
import ru.example.finance.model.FinancialOperation;
import ru.example.finance.model.OperationType;
import ru.example.finance.model.User;
import ru.example.finance.model.Wallet;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/** JSON-хранилище: один файл на пользователя, атомарная запись и переносимые снимки. */
public final class JsonUserStorage implements UserStorage {
    private static final int FORMAT_VERSION = 1;

    private final Path directory;
    private final ObjectMapper objectMapper;

    public JsonUserStorage(Path directory) {
        this.directory = Objects.requireNonNull(directory, "Каталог хранения обязателен");
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
    }

    @Override
    public Optional<User> load(String login) throws IOException {
        Path file = fileFor(login);
        if (!Files.exists(file)) {
            return Optional.empty();
        }
        return Optional.of(readUser(file));
    }

    @Override
    public void save(User user) throws IOException {
        Objects.requireNonNull(user, "Пользователь обязателен");
        writeUser(user, fileFor(user.getLogin()));
    }

    @Override
    public void exportTo(User user, Path target) throws IOException {
        Objects.requireNonNull(user, "Пользователь обязателен");
        Objects.requireNonNull(target, "Путь экспорта обязателен");
        writeUser(user, target);
    }

    @Override
    public User importFrom(Path source, String expectedLogin) throws IOException {
        Objects.requireNonNull(source, "Путь импорта обязателен");
        User imported = readUser(source);
        if (!User.normalizeLogin(expectedLogin).equals(imported.getLogin())) {
            throw new IOException("Снимок принадлежит другому пользователю: " + imported.getLogin());
        }
        return imported;
    }

    private User readUser(Path file) throws IOException {
        try {
            UserSnapshot snapshot = objectMapper.readValue(Files.readString(file), UserSnapshot.class);
            if (snapshot.version() != FORMAT_VERSION) {
                throw new IOException("Неподдерживаемая версия файла данных: " + file);
            }
            Wallet wallet = new Wallet(snapshot.login());
            for (OperationSnapshot operation : snapshot.operations()) {
                wallet.addOperation(new FinancialOperation(
                        UUID.fromString(operation.id()),
                        OperationType.valueOf(operation.type()),
                        new Category(operation.category()),
                        operation.amount(),
                        operation.description(),
                        Instant.parse(operation.createdAt())
                ));
            }
            for (BudgetSnapshot budget : snapshot.budgets()) {
                wallet.setBudget(new Category(budget.category()), budget.limit());
            }
            return new User(snapshot.login(), snapshot.passwordHash(), wallet);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new IOException("Файл данных повреждён: " + file, exception);
        }
    }

    private void writeUser(User user, Path target) throws IOException {
        List<OperationSnapshot> operations = user.getWallet().getOperations().stream()
                .map(operation -> new OperationSnapshot(
                        operation.getId().toString(),
                        operation.getType().name(),
                        operation.getCategory().getName(),
                        operation.getAmount(),
                        operation.getDescription(),
                        operation.getCreatedAt().toString()
                ))
                .toList();
        List<BudgetSnapshot> budgets = user.getWallet().getBudgets().entrySet().stream()
                .map(entry -> new BudgetSnapshot(entry.getKey().getName(), entry.getValue()))
                .toList();
        UserSnapshot snapshot = new UserSnapshot(
                FORMAT_VERSION, user.getLogin(), user.getPasswordHash(), operations, budgets
        );

        Path parent = target.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        objectMapper.writeValue(temporary.toFile(), snapshot);
        try {
            Files.move(temporary, target,
                    StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporary, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private Path fileFor(String login) {
        String normalizedLogin = User.normalizeLogin(login);
        String fileName = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(normalizedLogin.getBytes(StandardCharsets.UTF_8)) + ".json";
        return directory.resolve(fileName);
    }

    private record UserSnapshot(
            int version,
            String login,
            String passwordHash,
            List<OperationSnapshot> operations,
            List<BudgetSnapshot> budgets
    ) {
    }

    private record OperationSnapshot(
            String id,
            String type,
            String category,
            BigDecimal amount,
            String description,
            String createdAt
    ) {
    }

    private record BudgetSnapshot(String category, BigDecimal limit) {
    }
}

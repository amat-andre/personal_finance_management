package ru.example.finance.console;

import ru.example.finance.model.User;
import ru.example.finance.persistence.UserStorage;
import ru.example.finance.service.AuthService;
import ru.example.finance.service.AuthenticationException;
import ru.example.finance.service.FinanceService;
import ru.example.finance.service.OperationResult;
import ru.example.finance.service.SelectedCategoryTotals;
import ru.example.finance.service.StatisticsService;
import ru.example.finance.service.TransferResult;
import ru.example.finance.service.TransferService;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/** Цикл чтения и выполнения консольных команд. */
public final class ConsoleApplication {
    private final AuthService authService;
    private final FinanceService financeService;
    private final StatisticsService statisticsService;
    private final UserStorage userStorage;
    private final TransferService transferService;
    private final CommandParser commandParser = new CommandParser();
    private final InputValidator inputValidator = new InputValidator();
    private final ReportFormatter reportFormatter = new ReportFormatter();

    private User currentUser;

    public ConsoleApplication(
            AuthService authService,
            FinanceService financeService,
            StatisticsService statisticsService,
            UserStorage userStorage,
            TransferService transferService
    ) {
        this.authService = Objects.requireNonNull(authService);
        this.financeService = Objects.requireNonNull(financeService);
        this.statisticsService = Objects.requireNonNull(statisticsService);
        this.userStorage = Objects.requireNonNull(userStorage);
        this.transferService = Objects.requireNonNull(transferService);
    }

    public void run(InputStream inputStream, PrintStream printStream) {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(inputStream, StandardCharsets.UTF_8)
        );
        PrintWriter output = new PrintWriter(
                new java.io.OutputStreamWriter(printStream, StandardCharsets.UTF_8), true
        );
        output.println("Управление личными финансами. Введите help для списка команд.");
        boolean running = true;
        try {
            while (running) {
                output.print(currentUser == null ? "> " : currentUser.getLogin() + "> ");
                output.flush();
                String line = reader.readLine();
                if (line == null) {
                    break;
                }
                if (line.isBlank()) {
                    continue;
                }
                try {
                    running = execute(line, output);
                } catch (AuthenticationException | IllegalArgumentException exception) {
                    output.println("Ошибка: " + exception.getMessage());
                } catch (IOException exception) {
                    output.println("Ошибка сохранения или чтения файла: " + exception.getMessage());
                }
            }
        } catch (IOException exception) {
            output.println("Ошибка чтения команд: " + exception.getMessage());
        } finally {
            saveCurrentUser(output);
            output.println("Работа приложения завершена.");
        }
    }

    private boolean execute(String line, PrintWriter output) throws IOException {
        List<String> arguments = commandParser.parse(line);
        String command = arguments.get(0).toLowerCase(Locale.ROOT);
        switch (command) {
            case "help" -> printHelp(output);
            case "register" -> register(arguments, output);
            case "login" -> login(arguments, output);
            case "logout" -> logout(arguments, output);
            case "income" -> addOperation(arguments, output, true);
            case "expense" -> addOperation(arguments, output, false);
            case "budget" -> setBudget(arguments, output);
            case "remove-budget" -> removeBudget(arguments, output);
            case "rename-category" -> renameCategory(arguments, output);
            case "delete-category" -> deleteCategory(arguments, output);
            case "transfer" -> transfer(arguments, output);
            case "stats" -> showStatistics(arguments, output);
            case "balance" -> showBalance(arguments, output);
            case "report" -> saveReport(arguments, output);
            case "export" -> exportSnapshot(arguments, output);
            case "import" -> importSnapshot(arguments, output);
            case "save" -> save(arguments, output);
            case "exit" -> {
                requireArgumentCount(arguments, 1);
                return false;
            }
            default -> throw new IllegalArgumentException("Неизвестная команда. Введите help");
        }
        return true;
    }

    private void register(List<String> arguments, PrintWriter output) throws IOException {
        requireNoCurrentUser();
        requireArgumentCount(arguments, 3);
        User user = authService.register(arguments.get(1), arguments.get(2));
        userStorage.save(user);
        output.println("Пользователь " + user.getLogin() + " зарегистрирован. Теперь выполните login.");
    }

    private void login(List<String> arguments, PrintWriter output) {
        requireNoCurrentUser();
        requireArgumentCount(arguments, 3);
        currentUser = authService.authenticate(arguments.get(1), arguments.get(2));
        output.println("Добро пожаловать, " + currentUser.getLogin() + "!");
    }

    private void logout(List<String> arguments, PrintWriter output) throws IOException {
        requireArgumentCount(arguments, 1);
        requireCurrentUser();
        userStorage.save(currentUser);
        output.println("Данные сохранены. Пользователь " + currentUser.getLogin() + " вышел.");
        currentUser = null;
    }

    private void addOperation(
            List<String> arguments, PrintWriter output, boolean income
    ) {
        requireCurrentUser();
        if (arguments.size() < 3 || arguments.size() > 4) {
            throw new IllegalArgumentException(
                    "Использование: " + (income ? "income" : "expense")
                            + " \"категория\" сумма [\"описание\"]"
            );
        }
        var amount = inputValidator.parsePositiveAmount(arguments.get(2));
        String description = arguments.size() == 4 ? arguments.get(3) : "";
        OperationResult result = income
                ? financeService.addIncome(currentUser.getWallet(), arguments.get(1), amount, description)
                : financeService.addExpense(currentUser.getWallet(), arguments.get(1), amount, description);
        output.println((income ? "Доход добавлен: " : "Расход добавлен: ") + amount.toPlainString());
        result.warnings().forEach(output::println);
    }

    private void setBudget(List<String> arguments, PrintWriter output) {
        requireCurrentUser();
        requireArgumentCount(arguments, 3);
        var limit = inputValidator.parseNonNegativeAmount(arguments.get(2));
        List<String> warnings = financeService.setBudget(
                currentUser.getWallet(), arguments.get(1), limit
        );
        output.println("Бюджет установлен: " + arguments.get(1) + " = " + limit.toPlainString());
        warnings.forEach(output::println);
    }

    private void removeBudget(List<String> arguments, PrintWriter output) {
        requireCurrentUser();
        requireArgumentCount(arguments, 2);
        financeService.removeBudget(currentUser.getWallet(), arguments.get(1));
        output.println("Бюджет категории удалён: " + arguments.get(1));
    }

    private void renameCategory(List<String> arguments, PrintWriter output) {
        requireCurrentUser();
        requireArgumentCount(arguments, 3);
        financeService.renameCategory(currentUser.getWallet(), arguments.get(1), arguments.get(2));
        output.println("Категория переименована: " + arguments.get(1) + " → " + arguments.get(2));
    }

    private void deleteCategory(List<String> arguments, PrintWriter output) {
        requireCurrentUser();
        requireArgumentCount(arguments, 2);
        financeService.deleteCategory(currentUser.getWallet(), arguments.get(1));
        output.println("Категория, её операции и бюджет удалены: " + arguments.get(1));
    }

    private void transfer(List<String> arguments, PrintWriter output) throws IOException {
        requireCurrentUser();
        requireArgumentCount(arguments, 3);
        var amount = inputValidator.parsePositiveAmount(arguments.get(2));
        TransferResult result = transferService.transfer(currentUser, arguments.get(1), amount);
        output.println("Перевод пользователю " + arguments.get(1) + " выполнен: " + amount.toPlainString());
        result.warnings().forEach(output::println);
    }

    private void showStatistics(List<String> arguments, PrintWriter output) {
        requireCurrentUser();
        if (arguments.size() == 1) {
            output.print(reportFormatter.formatFullReport(currentUser.getWallet(), statisticsService));
            return;
        }
        SelectedCategoryTotals totals = statisticsService.totalsForCategories(
                currentUser.getWallet(), arguments.subList(1, arguments.size())
        );
        output.print(reportFormatter.formatSelectedCategories(totals));
    }

    private void showBalance(List<String> arguments, PrintWriter output) {
        requireCurrentUser();
        requireArgumentCount(arguments, 1);
        output.println("Текущий баланс: " + statisticsService.balance(currentUser.getWallet()).toPlainString());
    }

    private void saveReport(List<String> arguments, PrintWriter output) throws IOException {
        requireCurrentUser();
        requireArgumentCount(arguments, 2);
        Path path = Path.of(arguments.get(1));
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(path, reportFormatter.formatFullReport(currentUser.getWallet(), statisticsService),
                StandardCharsets.UTF_8);
        output.println("Отчёт сохранён в файл: " + path.toAbsolutePath());
    }

    private void exportSnapshot(List<String> arguments, PrintWriter output) throws IOException {
        requireCurrentUser();
        requireArgumentCount(arguments, 2);
        Path path = Path.of(arguments.get(1));
        userStorage.exportTo(currentUser, path);
        output.println("JSON-снимок экспортирован: " + path.toAbsolutePath());
    }

    private void importSnapshot(List<String> arguments, PrintWriter output) throws IOException {
        requireCurrentUser();
        requireArgumentCount(arguments, 2);
        Path path = Path.of(arguments.get(1));
        User imported = userStorage.importFrom(path, currentUser.getLogin());
        currentUser = new User(currentUser.getLogin(), currentUser.getPasswordHash(), imported.getWallet());
        userStorage.save(currentUser);
        output.println("JSON-снимок импортирован. Пароль текущего пользователя сохранён.");
    }

    private void save(List<String> arguments, PrintWriter output) throws IOException {
        requireCurrentUser();
        requireArgumentCount(arguments, 1);
        userStorage.save(currentUser);
        output.println("Данные кошелька сохранены.");
    }

    private void saveCurrentUser(PrintWriter output) {
        if (currentUser != null) {
            try {
                userStorage.save(currentUser);
            } catch (IOException exception) {
                output.println("Не удалось сохранить данные при выходе: " + exception.getMessage());
            }
        }
    }

    private void requireCurrentUser() {
        if (currentUser == null) {
            throw new AuthenticationException("Сначала выполните login");
        }
    }

    private void requireNoCurrentUser() {
        if (currentUser != null) {
            throw new AuthenticationException("Сначала выполните logout");
        }
    }

    private void requireArgumentCount(List<String> arguments, int expected) {
        if (arguments.size() != expected) {
            throw new IllegalArgumentException("Неверное число аргументов. Введите help");
        }
    }

    private void printHelp(PrintWriter output) {
        output.println("""
                Доступные команды:
                  register логин пароль                 — регистрация
                  login логин пароль                    — вход
                  logout                                — сохранить данные и выйти из аккаунта
                  income \"категория\" сумма [\"описание\"] — добавить доход
                  expense \"категория\" сумма [\"описание\"] — добавить расход
                  budget \"категория\" лимит              — установить или изменить бюджет
                  remove-budget \"категория\"             — удалить только бюджет
                  rename-category \"старое\" \"новое\"     — переименовать категорию
                  delete-category \"категория\"           — удалить категорию, операции и бюджет
                  transfer логин сумма                  — перевести деньги пользователю
                  stats                                 — полная статистика в таблицах
                  stats \"категория\" [\"категория\" ...]   — итоги по выбранным категориям
                  balance                               — текущий баланс
                  report \"путь к файлу\"                  — сохранить текстовый отчёт
                  export \"путь к JSON\"                   — экспортировать снимок кошелька
                  import \"путь к JSON\"                   — импортировать свой снимок кошелька
                  save                                  — сохранить кошелёк
                  exit                                  — сохранить данные и закрыть приложение
                Категории и описания с пробелами заключайте в двойные кавычки.

                Примеры:
                  register alex password
                  login alex password
                  income \"Зарплата\" 60000 \"Основная работа\"
                  expense \"Коммунальные услуги\" 4500,50 \"Счёт за месяц\"
                  budget \"Еда\" 15000
                  rename-category \"Еда\" \"Продукты\"
                  stats \"Продукты\" \"Зарплата\"
                  transfer bob 2500
                  report \"finance-report.txt\"""");
    }
}

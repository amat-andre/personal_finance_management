package ru.example.finance;

import ru.example.finance.console.ConsoleApplication;
import ru.example.finance.persistence.JsonUserStorage;
import ru.example.finance.persistence.UserStorage;
import ru.example.finance.repository.InMemoryUserRepository;
import ru.example.finance.repository.UserRepository;
import ru.example.finance.service.AuthService;
import ru.example.finance.service.FinanceService;
import ru.example.finance.service.PasswordHasher;
import ru.example.finance.service.StatisticsService;
import ru.example.finance.service.TransferService;

import java.nio.file.Path;

/** Точка входа в консольное приложение. */
public final class PersonalFinanceApplication {
    private PersonalFinanceApplication() {
    }

    public static void main(String[] args) {
        Path dataDirectory = Path.of(System.getProperty("finance.data.dir", "data"));
        UserStorage userStorage = new JsonUserStorage(dataDirectory);
        UserRepository userRepository = new InMemoryUserRepository();
        StatisticsService statisticsService = new StatisticsService();
        AuthService authService = new AuthService(
                userRepository, new PasswordHasher(), userStorage
        );
        FinanceService financeService = new FinanceService(statisticsService);
        TransferService transferService = new TransferService(
                userRepository, userStorage, financeService
        );
        ConsoleApplication application = new ConsoleApplication(
                authService, financeService, statisticsService, userStorage, transferService
        );
        application.run(System.in, System.out);
    }
}

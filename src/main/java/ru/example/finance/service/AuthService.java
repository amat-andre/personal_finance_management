package ru.example.finance.service;

import ru.example.finance.model.User;
import ru.example.finance.persistence.UserStorage;
import ru.example.finance.repository.UserRepository;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/** Регистрация и авторизация пользователей с загрузкой сохранённых кошельков. */
public final class AuthService {
    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final UserStorage userStorage;

    public AuthService(UserRepository userRepository, PasswordHasher passwordHasher) {
        this(userRepository, passwordHasher, UserStorage.disabled());
    }

    public AuthService(
            UserRepository userRepository, PasswordHasher passwordHasher, UserStorage userStorage
    ) {
        this.userRepository = Objects.requireNonNull(userRepository);
        this.passwordHasher = Objects.requireNonNull(passwordHasher);
        this.userStorage = Objects.requireNonNull(userStorage);
    }

    public synchronized User register(String login, String password) {
        String normalizedLogin = User.normalizeLogin(login);
        validatePassword(password);
        try {
            if (userRepository.findByLogin(normalizedLogin).isPresent()
                    || userStorage.load(normalizedLogin).isPresent()) {
                throw new AuthenticationException("Пользователь с таким логином уже существует");
            }
        } catch (IOException exception) {
            throw new AuthenticationException("Не удалось проверить сохранённые данные пользователя");
        }
        User user = new User(normalizedLogin, passwordHasher.hash(password));
        userRepository.save(user);
        return user;
    }

    public User authenticate(String login, String password) {
        if (password == null) {
            throw new AuthenticationException("Неверный логин или пароль");
        }
        String normalizedLogin;
        try {
            normalizedLogin = User.normalizeLogin(login);
        } catch (IllegalArgumentException exception) {
            throw new AuthenticationException("Неверный логин или пароль");
        }

        User user;
        try {
            Optional<User> storedUser = userStorage.load(normalizedLogin);
            if (storedUser.isPresent()) {
                user = storedUser.get();
                userRepository.save(user);
            } else {
                user = userRepository.findByLogin(normalizedLogin).orElse(null);
            }
        } catch (IOException exception) {
            throw new AuthenticationException("Не удалось загрузить данные кошелька");
        }
        if (user == null || !passwordHasher.matches(password, user.getPasswordHash())) {
            throw new AuthenticationException("Неверный логин или пароль");
        }
        return user;
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 4) {
            throw new AuthenticationException("Пароль должен содержать не менее 4 символов");
        }
    }
}

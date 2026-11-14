package ru.example.finance.repository;

import ru.example.finance.model.User;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Простое хранилище пользователей в памяти приложения. */
public final class InMemoryUserRepository implements UserRepository {
    private final Map<String, User> users = new LinkedHashMap<>();

    @Override
    public synchronized Optional<User> findByLogin(String login) {
        if (login == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(users.get(User.normalizeLogin(login)));
    }

    @Override
    public synchronized List<User> findAll() {
        return List.copyOf(new ArrayList<>(users.values()));
    }

    @Override
    public synchronized void save(User user) {
        users.put(user.getLogin(), user);
    }
}

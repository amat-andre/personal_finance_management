package ru.example.finance.persistence;

import ru.example.finance.model.User;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;

/** Файловое хранилище пользователей, кошельков и переносимых JSON-снимков. */
public interface UserStorage {
    Optional<User> load(String login) throws IOException;

    void save(User user) throws IOException;

    void exportTo(User user, Path target) throws IOException;

    User importFrom(Path source, String expectedLogin) throws IOException;

    static UserStorage disabled() {
        return new UserStorage() {
            @Override
            public Optional<User> load(String login) {
                return Optional.empty();
            }

            @Override
            public void save(User user) {
                // Сохранение отключено; используется в тестах чистой памяти.
            }

            @Override
            public void exportTo(User user, Path target) {
                throw new UnsupportedOperationException("Хранилище отключено");
            }

            @Override
            public User importFrom(Path source, String expectedLogin) {
                throw new UnsupportedOperationException("Хранилище отключено");
            }
        };
    }
}

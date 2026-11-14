package ru.example.finance.repository;

import ru.example.finance.model.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findByLogin(String login);

    List<User> findAll();

    void save(User user);
}

package ru.example.finance.model;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/** Пользователь приложения и привязанный к нему кошелёк. */
public final class User {
    private static final Pattern LOGIN_PATTERN = Pattern.compile("[\\p{L}\\p{N}_-]{3,32}");

    private final String login;
    private final String passwordHash;
    private final Wallet wallet;

    public User(String login, String passwordHash) {
        this(login, passwordHash, new Wallet(login));
    }

    public User(String login, String passwordHash, Wallet wallet) {
        this.login = normalizeLogin(login);
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Хеш пароля обязателен");
        }
        Objects.requireNonNull(wallet, "Кошелёк пользователя обязателен");
        if (!this.login.equals(wallet.getOwnerLogin())) {
            throw new IllegalArgumentException("Кошелёк принадлежит другому пользователю");
        }
        this.passwordHash = passwordHash;
        this.wallet = wallet;
    }

    public static String normalizeLogin(String login) {
        if (login == null) {
            throw new IllegalArgumentException("Логин обязателен");
        }
        String normalized = login.trim().toLowerCase(Locale.ROOT);
        if (!LOGIN_PATTERN.matcher(normalized).matches()) {
            throw new IllegalArgumentException(
                    "Логин должен содержать от 3 до 32 букв, цифр, дефисов или подчёркиваний"
            );
        }
        return normalized;
    }

    public String getLogin() {
        return login;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public Wallet getWallet() {
        return wallet;
    }

    @Override
    public boolean equals(Object other) {
        return this == other || other instanceof User user && login.equals(user.login);
    }

    @Override
    public int hashCode() {
        return Objects.hash(login);
    }
}

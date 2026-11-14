package ru.example.finance.service;

/** Ошибка регистрации или авторизации, понятная пользователю приложения. */
public final class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) {
        super(message);
    }
}

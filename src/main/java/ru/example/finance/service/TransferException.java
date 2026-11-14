package ru.example.finance.service;

/** Ошибка перевода между кошельками. */
public final class TransferException extends IllegalArgumentException {
    public TransferException(String message) {
        super(message);
    }
}

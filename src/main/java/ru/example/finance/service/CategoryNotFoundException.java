package ru.example.finance.service;

/** Запрошенная категория отсутствует в кошельке. */
public final class CategoryNotFoundException extends IllegalArgumentException {
    public CategoryNotFoundException(String categoryName) {
        super("Категория не найдена: " + categoryName);
    }
}

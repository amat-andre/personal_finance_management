package ru.example.finance.model;

import java.util.Locale;
import java.util.Objects;

/** Категория дохода или расхода. Названия сравниваются без учёта регистра. */
public final class Category {
    private final String name;
    private final String key;

    public Category(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Название категории не может быть пустым");
        }
        this.name = name.trim();
        this.key = this.name.toLowerCase(Locale.ROOT);
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object other) {
        return this == other
                || other instanceof Category category && key.equals(category.key);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key);
    }

    @Override
    public String toString() {
        return name;
    }
}

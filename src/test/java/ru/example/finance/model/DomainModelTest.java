package ru.example.finance.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainModelTest {

    @Test
    void categoryTrimsNameAndIgnoresCase() {
        Category food = new Category("  Еда  ");
        Category sameFood = new Category("еда");

        assertEquals("Еда", food.getName());
        assertEquals(food, sameFood);
        assertEquals(food.hashCode(), sameFood.hashCode());
    }

    @Test
    void categoryRejectsBlankName() {
        assertThrows(IllegalArgumentException.class, () -> new Category("  "));
    }

    @Test
    void operationRejectsZeroAndNegativeAmounts() {
        Category salary = new Category("Зарплата");

        assertThrows(IllegalArgumentException.class, () -> FinancialOperation.create(
                OperationType.INCOME, salary, BigDecimal.ZERO, ""
        ));
        assertThrows(IllegalArgumentException.class, () -> FinancialOperation.create(
                OperationType.EXPENSE, salary, new BigDecimal("-1"), ""
        ));
    }

    @Test
    void walletBelongsToUserAndStoresOperationsAndBudgets() {
        Wallet wallet = new Wallet("alice");
        FinancialOperation income = FinancialOperation.create(
                OperationType.INCOME,
                new Category("Зарплата"),
                new BigDecimal("50000.00"),
                "Основная работа"
        );

        wallet.addOperation(income);
        wallet.setBudget(new Category("Еда"), new BigDecimal("10000.00"));

        assertEquals("alice", wallet.getOwnerLogin());
        assertEquals(1, wallet.getOperations().size());
        assertEquals(income, wallet.getOperations().get(0));
        assertEquals(new BigDecimal("10000.00"), wallet.getBudgets().get(new Category("еда")));
    }

    @Test
    void walletDoesNotAllowExternalModificationOfOperations() {
        Wallet wallet = new Wallet("alice");

        assertThrows(UnsupportedOperationException.class,
                () -> wallet.getOperations().add(null));
    }

    @Test
    void walletRejectsNegativeBudget() {
        Wallet wallet = new Wallet("alice");

        assertThrows(IllegalArgumentException.class,
                () -> wallet.setBudget(new Category("Еда"), new BigDecimal("-0.01")));
        assertTrue(wallet.getBudgets().isEmpty());
    }
}

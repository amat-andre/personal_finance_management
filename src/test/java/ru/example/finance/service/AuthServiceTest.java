package ru.example.finance.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.example.finance.model.User;
import ru.example.finance.repository.InMemoryUserRepository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthServiceTest {
    private InMemoryUserRepository repository;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        repository = new InMemoryUserRepository();
        authService = new AuthService(repository, new PasswordHasher());
    }

    @Test
    void registersUserAndCreatesPersonalWallet() {
        User user = authService.register(" Alice ", "secret");

        assertEquals("alice", user.getLogin());
        assertEquals("alice", user.getWallet().getOwnerLogin());
        assertNotEquals("secret", user.getPasswordHash());
        assertSame(user, repository.findByLogin("ALICE").orElseThrow());
    }

    @Test
    void authenticatesRegisteredUserIgnoringLoginCase() {
        User registered = authService.register("alice", "secret");

        User authenticated = authService.authenticate(" ALICE ", "secret");

        assertSame(registered, authenticated);
    }

    @Test
    void rejectsWrongPasswordAndUnknownLogin() {
        authService.register("alice", "secret");

        assertThrows(AuthenticationException.class,
                () -> authService.authenticate("alice", "wrong"));
        assertThrows(AuthenticationException.class,
                () -> authService.authenticate("unknown", "secret"));
    }

    @Test
    void rejectsDuplicateLoginAndShortPassword() {
        authService.register("alice", "secret");

        assertThrows(AuthenticationException.class,
                () -> authService.register("ALICE", "another"));
        assertThrows(AuthenticationException.class,
                () -> authService.register("bob", "123"));
    }

    @Test
    void differentUsersHaveDifferentWallets() {
        User alice = authService.register("alice", "secret");
        User bob = authService.register("bob", "secret");

        assertNotSame(alice.getWallet(), bob.getWallet());
        assertEquals(2, repository.findAll().size());
    }
}

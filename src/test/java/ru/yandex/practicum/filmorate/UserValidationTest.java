package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.controller.UserController;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class UserValidationTest {

    private UserController userController;

    @BeforeEach
    void setUp() {
        userController = new UserController();
    }

    @Test
    void shouldCreateUserWithValidData() {
        User user = User.builder()
                .email("test@yandex.ru")
                .login("common_login")
                .name("Ivan")
                .birthday(LocalDate.of(1990, 5, 20))
                .build();

        User created = userController.create(user);
        assertNotNull(created);
        assertEquals(1, created.getId());
    }

    @Test
    void shouldThrowExceptionWhenEmailIsInvalid() {
        User userNoAt = User.builder()
                .email("testyandex.ru")
                .login("login")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        User userBlank = User.builder()
                .email("   ")
                .login("login")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        assertThrows(ValidationException.class, () -> userController.create(userNoAt));
        assertThrows(ValidationException.class, () -> userController.create(userBlank));
    }

    @Test
    void shouldThrowExceptionWhenLoginIsInvalid() {
        User userWithSpaces = User.builder()
                .email("test@yandex.ru")
                .login("my login")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        User userBlank = User.builder()
                .email("test@yandex.ru")
                .login("   ")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        assertThrows(ValidationException.class, () -> userController.create(userWithSpaces));
        assertThrows(ValidationException.class, () -> userController.create(userBlank));
    }

    @Test
    void shouldUseLoginIfNameIsEmpty() {
        User user = User.builder()
                .email("test@yandex.ru")
                .login("user_login")
                .name("")
                .birthday(LocalDate.of(1990, 1, 1))
                .build();

        User created = userController.create(user);
        assertEquals("user_login", created.getName());
    }

    @Test
    void shouldThrowExceptionWhenBirthdayInFuture() {
        User user = User.builder()
                .email("test@yandex.ru")
                .login("login")
                .birthday(LocalDate.now().plusDays(1))
                .build();

        assertThrows(ValidationException.class, () -> userController.create(user));
    }
}
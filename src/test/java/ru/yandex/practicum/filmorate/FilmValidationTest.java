package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.storage.film.InMemoryFilmStorage;
import ru.yandex.practicum.filmorate.storage.user.InMemoryUserStorage;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class FilmValidationTest {
    private FilmController filmController;

    @BeforeEach
    void setUp() {
        InMemoryFilmStorage filmStorage = new InMemoryFilmStorage();
        InMemoryUserStorage userStorage = new InMemoryUserStorage();
        FilmService filmService = new FilmService(filmStorage, userStorage);
        filmController = new FilmController(filmService);
    }

    @Test
    void shouldCreateFilmWithValidData() {
        Film film = Film.builder()
                .name("Inception")
                .description("A mind-bending thriller")
                .releaseDate(LocalDate.of(2010, 7, 16))
                .duration(148)
                .build();

        Film created = filmController.create(film);
        assertNotNull(created);
        assertEquals(1L, created.getId());
        assertEquals("Inception", created.getName());
    }

    @Test
    void shouldThrowExceptionWhenNameIsBlank() {
        Film film = Film.builder()
                .name(" ")
                .description("Description")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(100)
                .build();

        assertThrows(ValidationException.class, () -> filmController.create(film));
    }

    @Test
    void shouldAllowDescriptionOf200Characters() {
        String desc200 = "a".repeat(200);
        Film film = Film.builder()
                .name("Film")
                .description(desc200)
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(100)
                .build();

        assertDoesNotThrow(() -> filmController.create(film));
    }

    @Test
    void shouldThrowExceptionWhenDescriptionIs201Characters() {
        String desc201 = "a".repeat(201);
        Film film = Film.builder()
                .name("Film")
                .description(desc201)
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(100)
                .build();

        assertThrows(ValidationException.class, () -> filmController.create(film));
    }

    @Test
    void shouldAllowReleaseDateOnCinemaBirthday() {
        Film film = Film.builder()
                .name("First Film")
                .description("First movie ever")
                .releaseDate(LocalDate.of(1895, 12, 28))
                .duration(1)
                .build();

        assertDoesNotThrow(() -> filmController.create(film));
    }

    @Test
    void shouldThrowExceptionWhenReleaseDateBeforeCinemaBirthday() {
        Film film = Film.builder()
                .name("Early Film")
                .description("Too early")
                .releaseDate(LocalDate.of(1895, 12, 27))
                .duration(1)
                .build();

        assertThrows(ValidationException.class, () -> filmController.create(film));
    }

    @Test
    void shouldThrowExceptionWhenDurationIsZeroOrNegative() {
        Film filmZero = Film.builder()
                .name("Film Zero")
                .description("Description")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(0)
                .build();

        Film filmNegative = Film.builder()
                .name("Film Negative")
                .description("Description")
                .releaseDate(LocalDate.of(2000, 1, 1))
                .duration(-10)
                .build();

        assertThrows(ValidationException.class, () -> filmController.create(filmZero));
        assertThrows(ValidationException.class, () -> filmController.create(filmNegative));
    }
}
package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.storage.dao.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.dao.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.dao.MpaDbStorage;
import ru.yandex.practicum.filmorate.storage.dao.UserDbStorage;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FilmServiceSearchTest {

    @Mock
    private FilmDbStorage filmStorage;
    @Mock
    private UserDbStorage userStorage;
    @Mock
    private GenreDbStorage genreStorage;
    @Mock
    private MpaDbStorage mpaStorage;

    @InjectMocks
    private FilmService filmService;

    @Test
    void searchWithValidQueryDelegatesToStorage() {
        Film matrix = Film.builder().id(1L).name("Матрица").build();
        when(filmStorage.search("матриц", "title")).thenReturn(List.of(matrix));

        List<Film> result = filmService.search("матриц", "title");

        assertThat(result).containsExactly(matrix);
        verify(filmStorage).search("матриц", "title");
    }

    @Test
    void searchWithNullByNormalizesToEmpty() {
        when(filmStorage.search("космос", "")).thenReturn(List.of());

        filmService.search("космос", null);

        verify(filmStorage).search("космос", "");
    }

    @Test
    void searchLowercasesBy() {
        when(filmStorage.search("к", "title,description")).thenReturn(List.of());

        filmService.search("к", "TITLE,DESCRIPTION");

        verify(filmStorage).search("к", "title,description");
    }

    @Test
    void searchWithBlankQueryReturnsEmptyListWithoutStorageCall() {
        List<Film> result = filmService.search("   ", "title");

        assertThat(result).isEmpty();
        verifyNoInteractions(filmStorage);
    }

    @Test
    void searchWithNullQueryReturnsEmptyListWithoutStorageCall() {
        List<Film> result = filmService.search(null, "title");

        assertThat(result).isEmpty();
        verifyNoInteractions(filmStorage);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t", "\n"})
    void searchWithBlankQueryVariantsReturnsEmpty(String blankQuery) {
        assertThat(filmService.search(blankQuery, "title")).isEmpty();
        verifyNoInteractions(filmStorage);
    }
}

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
import ru.yandex.practicum.filmorate.storage.dao.*;

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
    @Mock
    private DirectorDbStorage directorStorage;

    @InjectMocks
    private FilmService filmService;

    @Test
    void searchByTitleDelegatesToStorage() {
        when(filmStorage.search("крад", "title")).thenReturn(List.of());

        filmService.search("крад", "title");

        verify(filmStorage).search("крад", "title");
    }

    @Test
    void searchByDirectorDelegatesToStorage() {
        when(filmStorage.search("ли", "director")).thenReturn(List.of());

        filmService.search("ли", "director");

        verify(filmStorage).search("ли", "director");
    }

    @Test
    void searchByBothDelegatesToStorage() {
        when(filmStorage.search("крад", "title,director")).thenReturn(List.of());

        filmService.search("крад", "title,director");

        verify(filmStorage).search("крад", "title,director");
    }

    @Test
    void searchWithNullQueryReturnsEmptyWithoutStorageCall() {
        assertThat(filmService.search(null, "title")).isEmpty();
        verifyNoInteractions(filmStorage);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "\t", "\n"})
    void searchWithBlankQueryReturnsEmptyWithoutStorageCall(String blank) {
        assertThat(filmService.search(blank, "title")).isEmpty();
        verifyNoInteractions(filmStorage);
    }

    @Test
    void searchTrimsQueryBeforePassingToStorage() {
        when(filmStorage.search("крад", "title")).thenReturn(List.of());

        filmService.search("  крад  ", "title");

        verify(filmStorage).search("крад", "title");
    }

    @Test
    void searchReturnsStorageResult() {
        Film film = Film.builder().id(1L).name("Крадущийся тигр").build();
        when(filmStorage.search("крад", "title,director")).thenReturn(List.of(film));

        List<Film> result = filmService.search("крад", "title,director");

        assertThat(result).containsExactly(film);
    }
}
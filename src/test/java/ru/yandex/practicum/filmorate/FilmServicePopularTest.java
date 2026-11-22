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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FilmServicePopularTest {

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
    void getPopularWithNullCountUsesDefault10() {
        when(filmStorage.getPopular(10, null, null)).thenReturn(List.of());

        filmService.getPopular(null, null, null);

        verify(filmStorage).getPopular(10, null, null);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -5, -100})
    void getPopularWithNonPositiveCountUsesDefault10(int count) {
        when(filmStorage.getPopular(10, null, null)).thenReturn(List.of());

        filmService.getPopular(count, null, null);

        verify(filmStorage).getPopular(10, null, null);
    }

    @Test
    void getPopularWithPositiveCountPassesThrough() {
        when(filmStorage.getPopular(3, null, null)).thenReturn(List.of());

        filmService.getPopular(3, null, null);

        verify(filmStorage).getPopular(3, null, null);
    }

    @Test
    void getPopularPassesGenreId() {
        when(filmStorage.getPopular(10, 1, null)).thenReturn(List.of());

        filmService.getPopular(10, 1, null);

        verify(filmStorage).getPopular(10, 1, null);
    }

    @Test
    void getPopularPassesYear() {
        when(filmStorage.getPopular(10, null, 2000)).thenReturn(List.of());

        filmService.getPopular(10, null, 2000);

        verify(filmStorage).getPopular(10, null, 2000);
    }

    @Test
    void getPopularPassesBothFilters() {
        when(filmStorage.getPopular(5, 1, 2000)).thenReturn(List.of());

        filmService.getPopular(5, 1, 2000);

        verify(filmStorage).getPopular(5, 1, 2000);
    }

    @Test
    void getPopularReturnsStorageResult() {
        Film a = Film.builder().id(1L).name("Популярный").build();
        Film b = Film.builder().id(2L).name("Второй").build();
        when(filmStorage.getPopular(10, null, null)).thenReturn(List.of(a, b));

        List<Film> result = filmService.getPopular(10, null, null);

        assertThat(result).containsExactly(a, b);
    }

    @Test
    void getPopularWithEmptyStorageReturnsEmptyList() {
        when(filmStorage.getPopular(10, null, null)).thenReturn(List.of());

        assertThat(filmService.getPopular(10, null, null)).isEmpty();
    }
}
package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.dao.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.dao.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.dao.MpaDbStorage;
import ru.yandex.practicum.filmorate.storage.dao.UserDbStorage;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilmService {

    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);
    private final FilmDbStorage filmStorage;
    private final UserDbStorage userStorage;
    private final MpaDbStorage mpaStorage;
    private final GenreDbStorage genreStorage;

    public Film create(Film film) {
        validateFilm(film);
        validateMpaAndGenres(film);
        Film created = filmStorage.create(film);
        if (created != null) {
            genreStorage.loadGenresForFilms(List.of(created));
        }
        return created;
    }

    public Film update(Film film) {
        getById(film.getId());
        validateFilm(film);
        validateMpaAndGenres(film);
        Film updated = filmStorage.update(film);
        if (updated != null) {
            genreStorage.loadGenresForFilms(List.of(updated));
        }
        return updated;
    }

    public List<Film> getAll() {
        List<Film> films = filmStorage.getAll();
        genreStorage.loadGenresForFilms(films);
        return films;
    }

    public Film getById(Long id) {
        Film film = filmStorage.getById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Фильм с id = %d не найден", id)));
        genreStorage.loadGenresForFilms(List.of(film));
        return film;
    }

    public void delete(Long id) {
        getById(id);
        filmStorage.delete(id);
    }

    public void addLike(Long filmId, Long userId) {
        getById(filmId);
        checkUserExists(userId);
        filmStorage.addLike(filmId, userId);
    }

    public void removeLike(Long filmId, Long userId) {
        getById(filmId);
        checkUserExists(userId);
        filmStorage.removeLike(filmId, userId);
    }

    public List<Film> getPopular(int count) {
        List<Film> films = filmStorage.getPopular(count);
        genreStorage.loadGenresForFilms(films);
        return films;
    }

    private void checkUserExists(Long userId) {
        userStorage.getById(userId)
                .orElseThrow(() -> new NotFoundException(String.format("Пользователь с id = %d не найден", userId)));
    }

    private void validateFilm(Film film) {
        if (film.getName() == null || film.getName().isBlank()) {
            throw new ValidationException("Название фильма не может быть пустым");
        }
        if (film.getDescription() != null && film.getDescription().length() > 200) {
            throw new ValidationException("Максимальная длина описания — 200 символов");
        }
        if (film.getReleaseDate() != null && film.getReleaseDate().isBefore(MIN_RELEASE_DATE)) {
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }
        if (film.getDuration() <= 0) {
            throw new ValidationException("Продолжительность фильма должна быть положительной");
        }
    }

    private void validateMpaAndGenres(Film film) {
        if (film.getMpa() != null && film.getMpa().getId() != null) {
            mpaStorage.getById(film.getMpa().getId())
                    .orElseThrow(() -> new NotFoundException(String.format("MPA с id = %d не найден", film.getMpa().getId())));
        }
        if (film.getGenres() != null) {
            film.getGenres().forEach(genre -> genreStorage.getById(genre.getId())
                    .orElseThrow(() -> new NotFoundException(String.format("Жанр с id = %d не найден", genre.getId())))
            );
        }
    }
}
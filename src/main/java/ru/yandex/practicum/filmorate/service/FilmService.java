package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.dao.*;

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
    private final DirectorDbStorage directorStorage;

    public Film create(Film film) {
        validateFilm(film);
        validateMpaGenresAndDirectors(film);
        return filmStorage.create(film);
    }

    public Film update(Film film) {
        checkFilmExists(film.getId());
        validateFilm(film);
        validateMpaGenresAndDirectors(film);
        return filmStorage.update(film);
    }

    public List<Film> getAll() {
        return filmStorage.getAll();
    }

    public Film getById(Long id) {
        return filmStorage.getById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Фильм с id = %d не найден", id)));
    }

    public void delete(Long id) {
        checkFilmExists(id);
        filmStorage.delete(id);
    }

    public void addLike(Long filmId, Long userId) {
        checkFilmExists(filmId);
        checkUserExists(userId);
        filmStorage.addLike(filmId, userId);
    }

    public void removeLike(Long filmId, Long userId) {
        checkFilmExists(filmId);
        checkUserExists(userId);
        filmStorage.removeLike(filmId, userId);
    }

    public List<Film> getPopular(Integer count, Integer genreId, Integer year) {
        int limit = (count == null || count <= 0) ? 10 : count;

        List<Film> popular = filmStorage.getPopular(limit, genreId, year);
        log.info("Популярные фильмы: count={}, genreId={}, year={}, найдено {}",
                limit, genreId, year, popular.size());

        return popular;
    }

    /*public List<Film> getPopular(int count) {
        if (count <= 0) {
            throw new ValidationException("Параметр count должен быть положительным числом");
        }
        return filmStorage.getPopular(count);
    }*/

    public List<Film> getDirectorFilms(Long directorId, String sortBy) {
        directorStorage.getById(directorId)
                .orElseThrow(() -> new NotFoundException(String.format("Режиссёр с id = %d не найден", directorId)));
        if (!"year".equals(sortBy) && !"likes".equals(sortBy)) {
            throw new ValidationException("Некорректный параметр сортировки sortBy: " + sortBy);
        }
        return filmStorage.getDirectorFilms(directorId, sortBy);
    }

    public List<Film> search(String query, String by) {
        if (query == null || query.isBlank()) return List.of();
        String normalizedQuery = query.trim();
        String normalizedBy = (by == null || by.isBlank())
                ? "title,director"
                : by.toLowerCase();
        List<Film> result = filmStorage.search(normalizedQuery, normalizedBy);
        log.info("Поиск фильмов: query='{}', by='{}', найдено {} фильмов", query, by, result.size());
        return result;
    }

    public List<Film> getCommonFilms(Long userId, Long friendId) {
        checkUserExists(userId);
        checkUserExists(friendId);
        List<Film> films = filmStorage.getCommon(userId, friendId);
        genreStorage.loadGenresForFilms(films);
        return films;
    }

    private void checkFilmExists(Long filmId) {
        filmStorage.getById(filmId)
                .orElseThrow(() -> new NotFoundException(String.format("Фильм с id = %d не найден", filmId)));
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

    private void validateMpaGenresAndDirectors(Film film) {
        if (film.getMpa() != null && film.getMpa().getId() != null) {
            mpaStorage.getById(film.getMpa().getId())
                    .orElseThrow(() -> new NotFoundException(String.format("MPA с id = %d не найден", film.getMpa().getId())));
        }
        if (film.getGenres() != null) {
            film.getGenres().forEach(genre -> {
                if (genre == null || genre.getId() == null) {
                    throw new ValidationException("Жанр не может быть null или без ID");
                }
                genreStorage.getById(genre.getId())
                        .orElseThrow(() -> new NotFoundException(String.format("Жанр с id = %d не найден", genre.getId())));
            });
        }
        if (film.getDirectors() != null) {
            film.getDirectors().forEach(director -> {
                if (director == null || director.getId() == null) {
                    throw new ValidationException("Режиссёр не может быть null или без ID");
                }
                directorStorage.getById(director.getId())
                        .orElseThrow(() -> new NotFoundException(String.format("Режиссёр с id = %d не найден", director.getId())));
            });
        }
    }
}
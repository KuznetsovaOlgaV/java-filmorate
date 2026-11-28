package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.dao.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilmService {
    private static final LocalDate MIN_RELEASE_DATE = LocalDate.of(1895, 12, 28);

    private static final String DEFAULT_SEARCH_BY = "title,director";
    private static final String SORT_BY_YEAR = "year";
    private static final String SORT_BY_LIKES = "likes";
    private static final int DEFAULT_POPULAR_LIMIT = 10;
    private static final int MAX_DESCRIPTION_LENGTH = 200;

    private static final int MIN_RATING = 1;
    private static final int MAX_RATING = 10;
    private static final String SORT_BY_RATING = "rating";

    private final FilmDbStorage filmStorage;
    private final UserDbStorage userStorage;
    private final MpaDbStorage mpaStorage;
    private final GenreDbStorage genreStorage;

    private final DirectorDbStorage directorStorage;
    private final EventService eventService;

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

    public List<Film> getPopular(Integer count, Integer genreId, Integer year) {
        int limit = (count == null || count <= 0) ? DEFAULT_POPULAR_LIMIT : count;

        List<Film> popular = filmStorage.getPopular(limit, genreId, year);
        log.info("Популярные фильмы: count={}, genreId={}, year={}, найдено {}",
                limit, genreId, year, popular.size());

        return popular;
    }

    public List<Film> getDirectorFilms(Long directorId, String sortBy) {
        checkDirectorExists(directorId);
        if (!SORT_BY_YEAR.equals(sortBy) && !SORT_BY_LIKES.equals(sortBy)) {
            throw new ValidationException("Некорректный параметр сортировки sortBy: " + sortBy);
        }
        return filmStorage.getDirectorFilms(directorId, sortBy);
    }

    public List<Film> search(String query, String by) {
        if (query == null || query.isBlank()) return List.of();
        String normalizedQuery = query.trim();
        String normalizedBy = (by == null || by.isBlank()) ? DEFAULT_SEARCH_BY : by.toLowerCase();
        List<Film> result = filmStorage.search(normalizedQuery, normalizedBy);
        log.info("Поиск фильмов: query='{}', by='{}', найдено {} фильмов", query, by, result.size());
        return result;
    }

    public List<Film> getCommonFilms(Long userId, Long friendId) {
        checkUserExists(userId);
        checkUserExists(friendId);
        List<Film> films = filmStorage.getCommon(userId, friendId);
        return films;
    }

    private void checkFilmExists(Long filmId) {
        filmStorage.getById(filmId)
                .orElseThrow(() -> new NotFoundException(String.format("Фильм с id = %d не найден", filmId)));
    }

    private void checkUserExists(Long userId) {
        if (!userStorage.existsById(userId)) {
            throw new NotFoundException(String.format("Пользователь с id = %d не найден", userId));
        }
    }

    private void checkDirectorExists(Long directorId) {
        directorStorage.getById(directorId)
                .orElseThrow(() -> new NotFoundException(String.format("Режиссёр с id = %d не найден", directorId)));
    }

    private void validateFilm(Film film) {
        if (film.getName() == null || film.getName().isBlank()) {
            throw new ValidationException("Название фильма не может быть пустым");
        }
        if (film.getDescription() != null && film.getDescription().length() > MAX_DESCRIPTION_LENGTH) {
            throw new ValidationException("Максимальная длина описания — " + MAX_DESCRIPTION_LENGTH + " символов");
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
        if (film.getGenres() != null && !film.getGenres().isEmpty()) {
            List<Integer> genreIds = film.getGenres().stream()
                    .peek(genre -> {
                        if (genre == null || genre.getId() == null) {
                            throw new ValidationException("Жанр не может быть null или без ID");
                        }
                    })
                    .map(Genre::getId)
                    .distinct()
                    .toList();

            Set<Integer> existingGenreIds = genreStorage.getExistingIds(genreIds);
            for (Integer id : genreIds) {
                if (!existingGenreIds.contains(id)) {
                    throw new NotFoundException(String.format("Жанр с id = %d не найден", id));
                }
            }
        }
        if (film.getDirectors() != null && !film.getDirectors().isEmpty()) {
            List<Long> directorIds = film.getDirectors().stream()
                    .peek(director -> {
                        if (director == null || director.getId() == null) {
                            throw new ValidationException("Режиссёр не может быть null или без ID");
                        }
                    })
                    .map(Director::getId)
                    .distinct()
                    .toList();

            Set<Long> existingDirectorIds = directorStorage.getExistingIds(directorIds);
            for (Long id : directorIds) {
                if (!existingDirectorIds.contains(id)) {
                    throw new NotFoundException(String.format("Режиссёр с id = %d не найден", id));
                }
            }
        }
    }

    public void addRating(Long filmId, Long userId, Integer rating) {
        if (rating == null || rating < MIN_RATING || rating > MAX_RATING) {
            throw new ValidationException("Оценка должна быть в диапазоне от 1 до 10");
        }
        checkFilmExists(filmId);
        checkUserExists(userId);

        boolean isNew = filmStorage.upsertRating(filmId, userId, rating);
        if (isNew) {
            eventService.addLikeEvent(userId, filmId);
        } else {
            eventService.updateLikeEvent(userId, filmId);
        }
    }

    public void removeRating(Long filmId, Long userId) {
        checkFilmExists(filmId);
        checkUserExists(userId);
        filmStorage.removeRating(filmId, userId);
        eventService.removeLikeEvent(userId, filmId);
    }
}
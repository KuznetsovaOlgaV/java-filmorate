package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.List;
import java.util.Optional;

public interface FilmStorage {
    Film create(Film film);

    Film update(Film film);

    void delete(Long id);

    List<Film> getAll();

    Optional<Film> getById(Long id);

    List<Film> getPopular(Integer count, Integer genreId, Integer year);

    List<Film> search(String query, String by);

    boolean upsertRating(Long filmId, Long userId, int rating);

    void removeRating(Long filmId, Long userId);

    List<Film> getRecommendations(Long userId);

}
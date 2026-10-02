package ru.yandex.practicum.filmorate.storage.film;

import ru.yandex.practicum.filmorate.model.Film;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface FilmStorage {
    Film create(Film film);

    Film update(Film film);

    void delete(Long id);

    Collection<Film> findAll();

    Optional<Film> findById(Long id);

    // проверка существования фильма по id
    boolean existsById(Long id);

    // вынесение выборки и сортировки популярных фильмов в хранилище
    List<Film> getPopular(int count);
}
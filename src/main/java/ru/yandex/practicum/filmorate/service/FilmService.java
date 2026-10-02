package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FilmService {
    private static final LocalDate CINEMA_BIRTHDAY = LocalDate.of(1895, 12, 28);
    private static final int MAX_DESCRIPTION_LENGTH = 200;

    private final FilmStorage filmStorage;
    private final UserStorage userStorage;

    public Collection<Film> findAll() {
        return filmStorage.findAll();
    }

    public Film findById(Long id) {
        return filmStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Фильм с id = " + id + " не найден"));
    }

    public Film create(Film film) {
        validateFilm(film);
        Film created = filmStorage.create(film);
        log.info("Фильм успешно добавлен: {}", created);
        return created;
    }

    public Film update(Film film) {
        Film oldFilm = findById(film.getId());
        validateFilm(film);
        // Сохраняем уже существующие лайки при обновлении фильма
        film.setLikes(oldFilm.getLikes());
        Film updated = filmStorage.update(film);
        log.info("Фильм с id {} успешно обновлен", updated.getId());
        return updated;
    }

    public void addLike(Long filmId, Long userId) {
        Film film = findById(filmId);
        userStorage.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + userId + " не найден"));
        film.getLikes().add(userId);
        log.info("Пользователь id {} поставил лайк фильму id {}", userId, filmId);
    }

    public void removeLike(Long filmId, Long userId) {
        Film film = findById(filmId);
        userStorage.findById(userId)
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + userId + " не найден"));
        film.getLikes().remove(userId);
        log.info("Пользователь id {} удалил лайк у фильма id {}", userId, filmId);
    }

    public List<Film> getPopularFilms(int count) {
        if (count <= 0) {
            throw new ValidationException("Параметр count должен быть положительным числом");
        }
        return filmStorage.findAll().stream()
                .sorted((f1, f2) -> Integer.compare(f2.getLikes().size(), f1.getLikes().size()))
                .limit(count)
                .collect(Collectors.toList());
    }

    public void validateFilm(Film film) {
        if (!StringUtils.hasText(film.getName())) {
            log.warn("Валидация не пройдена: название фильма пустое");
            throw new ValidationException("Название фильма не может быть пустым");
        }
        if (film.getDescription() != null && film.getDescription().length() > MAX_DESCRIPTION_LENGTH) {
            log.warn("Валидация не пройдена: длина описания превышает {} символов", MAX_DESCRIPTION_LENGTH);
            throw new ValidationException("Максимальная длина описания — " + MAX_DESCRIPTION_LENGTH + " символов");
        }
        if (film.getReleaseDate() == null || film.getReleaseDate().isBefore(CINEMA_BIRTHDAY)) {
            log.warn("Валидация не пройдена: дата релиза раньше 28 декабря 1895 года");
            throw new ValidationException("Дата релиза не может быть раньше 28 декабря 1895 года");
        }
        if (film.getDuration() <= 0) {
            log.warn("Валидация не пройдена: продолжительность фильма должна быть положительной");
            throw new ValidationException("Продолжительность фильма должна быть положительным числом");
        }
    }
}
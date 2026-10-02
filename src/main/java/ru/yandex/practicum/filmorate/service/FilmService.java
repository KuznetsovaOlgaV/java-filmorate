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
        // валидация лайков на существование пользователей при создании фильма
        validateLikes(film);
        Film created = filmStorage.create(film);
        log.info("Фильм успешно добавлен: {}", created);
        return created;
    }

    public Film update(Film film) {
        Film oldFilm = findById(film.getId());
        validateFilm(film);
        //  если не передан — сохраняем старые иначе если передан — проверяем существование всех id пользователей в списке likes
        if (film.getLikes() == null || film.getLikes().isEmpty()) {
            film.setLikes(oldFilm.getLikes());
        } else {
            validateLikes(film);
        }

        Film updated = filmStorage.update(film);
        log.info("Фильм с id {} успешно обновлен", updated.getId());
        return updated;
    }

    public void addLike(Long filmId, Long userId) {
        Film film = findById(filmId);
        if (!userStorage.existsById(userId)) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }
        film.getLikes().add(userId);
        log.info("Пользователь id {} поставил лайк фильму id {}", userId, filmId);
    }

    public void removeLike(Long filmId, Long userId) {
        Film film = findById(filmId);
        if (!userStorage.existsById(userId)) {
            throw new NotFoundException("Пользователь с id = " + userId + " не найден");
        }
        film.getLikes().remove(userId);
        log.info("Пользователь id {} удалил лайк у фильма id {}", userId, filmId);
    }

    public List<Film> getPopularFilms(int count) {
        if (count <= 0) {
            throw new ValidationException("Параметр count должен быть положительным числом");
        }
        // сортировка и лимит в хранилище
        return filmStorage.getPopular(count);
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

    // валидация существования пользователей
    private void validateLikes(Film film) {
        if (film.getLikes() != null) {
            for (Long userId : film.getLikes()) {
                if (!userStorage.existsById(userId)) {
                    log.warn("Пользователь с id = {} не найден для лайка", userId);
                    throw new NotFoundException("Пользователь с id = " + userId + " не найден");
                }
            }
        }
    }
}
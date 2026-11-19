package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.ReviewStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewService {

    private final ReviewStorage reviewStorage;
    private final UserStorage userStorage;
    private final FilmStorage filmStorage;

    public Review create(Review review) {
        validateReview(review);

        if (userStorage.getById(review.getUserId()).isEmpty()) {
            throw new NotFoundException("Пользователь с id=" + review.getUserId() + " не найден");
        }
        if (filmStorage.getById(review.getFilmId()).isEmpty()) {
            throw new NotFoundException("Фильм с id=" + review.getFilmId() + " не найден");
        }
        Review created = reviewStorage.add(review);
        log.info("Добавлен отзыв: id={}, filmId={}, userId={}", created.getReviewId(), created.getFilmId(), created.getUserId());
        return created;
    }

    public Review update(Review review) {
        if (review.getReviewId() == null || reviewStorage.getById(review.getReviewId()).isEmpty()) {
            throw new NotFoundException("Отзыв с id=" + review.getReviewId() + " не найден");
        }
        Review updated = reviewStorage.update(review);
        log.info("Обновлён отзыв: id={}", updated.getReviewId());
        return updated;
    }

    public void delete(Long reviewId) {
        if (reviewStorage.getById(reviewId).isEmpty()) {
            throw new NotFoundException("Отзыв с id=" + reviewId + " не найден");
        }
        reviewStorage.delete(reviewId);
        log.info("Удалён отзыв: id={}", reviewId);
    }

    public Review getById(Long reviewId) {
        return reviewStorage.getById(reviewId)
                .orElseThrow(() -> new NotFoundException("Отзыв с id=" + reviewId + " не найден"));
    }

    public List<Review> getReviewsForFilm(Long filmId, Integer count) {
        int limit = (count == null || count <= 0) ? 10 : count;
        if (filmId == null) {
            return reviewStorage.findAll(limit);
        }
        return reviewStorage.findByFilmId(filmId, limit);
    }

    public void addLike(Long reviewId, Long userId) {
        checkReviewAndUser(reviewId, userId);
        reviewStorage.addMark(reviewId, userId, true);
        log.info("Пользователь id={} поставил лайк отзыву id={}", userId, reviewId);
    }

    public void addDislike(Long reviewId, Long userId) {
        checkReviewAndUser(reviewId, userId);
        reviewStorage.addMark(reviewId, userId, false);
        log.info("Пользователь id={} поставил дизлайк отзыву id={}", userId, reviewId);
    }

    public void removeLike(Long reviewId, Long userId) {
        checkReviewAndUser(reviewId, userId);
        reviewStorage.removeMark(reviewId, userId, true);
        log.info("Пользователь id={} удалил лайк с отзыва id={}", userId, reviewId);
    }

    public void removeDislike(Long reviewId, Long userId) {
        checkReviewAndUser(reviewId, userId);
        reviewStorage.removeMark(reviewId, userId, false);
        log.info("Пользователь id={} удалил дизлайк с отзыва id={}", userId, reviewId);
    }

    private void checkReviewAndUser(Long reviewId, Long userId) {
        if (reviewStorage.getById(reviewId).isEmpty()) {
            throw new NotFoundException("Отзыв с id=" + reviewId + " не найден");
        }
        if (userStorage.getById(userId).isEmpty()) {
            throw new NotFoundException("Пользователь с id=" + userId + " не найден");
        }
    }

    private void validateReview(Review review) {
        if (review.getContent() == null || review.getContent().isBlank()) {
            throw new ValidationException("Содержание отзыва не может быть пустым");
        }
        if (review.getIsPositive() == null) {
            throw new ValidationException("Тип отзыва (isPositive) должен быть указан");
        }
        if (review.getUserId() == null) {
            throw new ValidationException("userId должен быть указан");
        }
        if (review.getFilmId() == null) {
            throw new ValidationException("filmId должен быть указан");
        }
    }
}
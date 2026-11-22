package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Review;

import java.util.List;
import java.util.Optional;

public interface ReviewStorage {
    Review add(Review review);

    Review update(Review review);

    void delete(Long reviewId);

    Optional<Review> getById(Long reviewId);

    List<Review> findByFilmId(Long filmId, int count);

    List<Review> findAll(int count);

    void addMark(Long reviewId, Long userId, boolean isLike);

    void removeMark(Long reviewId, Long userId, boolean isLike);
}
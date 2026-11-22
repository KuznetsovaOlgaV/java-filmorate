package ru.yandex.practicum.filmorate.storage.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.storage.ReviewStorage;
import ru.yandex.practicum.filmorate.storage.mapper.ReviewRowMapper;

import java.sql.PreparedStatement;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class ReviewDbStorage implements ReviewStorage {

    private final JdbcTemplate jdbcTemplate;
    private final ReviewRowMapper reviewMapper;

    @Override
    public Review add(Review review) {
        String sql = "INSERT INTO reviews (content, is_positive, user_id, film_id) VALUES (?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, new String[]{"review_id"});
            ps.setString(1, review.getContent());
            ps.setBoolean(2, review.getIsPositive());
            ps.setLong(3, review.getUserId());
            ps.setLong(4, review.getFilmId());
            return ps;
        }, keyHolder);
        review.setReviewId(keyHolder.getKey().longValue());
        review.setUseful(0);
        return review;
    }

    @Override
    public Review update(Review review) {
        String sql = "UPDATE reviews SET content = ?, is_positive = ? WHERE review_id = ?";
        jdbcTemplate.update(sql, review.getContent(), review.getIsPositive(), review.getReviewId());
        return getById(review.getReviewId()).orElseThrow();
    }

    @Override
    public void delete(Long reviewId) {
        jdbcTemplate.update("DELETE FROM reviews WHERE review_id = ?", reviewId);
    }

    @Override
    public Optional<Review> getById(Long reviewId) {
        String sql = "SELECT * FROM reviews WHERE review_id = ?";
        return jdbcTemplate.query(sql, reviewMapper, reviewId).stream().findFirst();
    }

    @Override
    public List<Review> findByFilmId(Long filmId, int count) {
        String sql = "SELECT * FROM reviews WHERE film_id = ? ORDER BY useful DESC LIMIT ?";
        return jdbcTemplate.query(sql, reviewMapper, filmId, count);
    }

    @Override
    public List<Review> findAll(int count) {
        String sql = "SELECT * FROM reviews ORDER BY useful DESC LIMIT ?";
        return jdbcTemplate.query(sql, reviewMapper, count);
    }

    @Override
    public void addMark(Long reviewId, Long userId, boolean isLike) {
        // Удаляем предыдущую оценку пользователя, если она была
        removeMark(reviewId, userId, true);
        removeMark(reviewId, userId, false);

        String sql = "INSERT INTO review_marks (review_id, user_id, is_like) VALUES (?, ?, ?)";
        jdbcTemplate.update(sql, reviewId, userId, isLike);

        // Пересчитываем useful: +1 за лайк, -1 за дизлайк
        String updateUseful = "UPDATE reviews SET useful = " +
                "(SELECT COALESCE(SUM(CASE WHEN is_like THEN 1 ELSE -1 END), 0) " +
                " FROM review_marks WHERE review_id = ?) WHERE review_id = ?";
        jdbcTemplate.update(updateUseful, reviewId, reviewId);
    }

    @Override
    public void removeMark(Long reviewId, Long userId, boolean isLike) {
        String sql = "DELETE FROM review_marks WHERE review_id = ? AND user_id = ? AND is_like = ?";
        jdbcTemplate.update(sql, reviewId, userId, isLike);
        // После удаления также пересчитываем useful
        String updateUseful = "UPDATE reviews SET useful = " +
                "(SELECT COALESCE(SUM(CASE WHEN is_like THEN 1 ELSE -1 END), 0) " +
                " FROM review_marks WHERE review_id = ?) WHERE review_id = ?";
        jdbcTemplate.update(updateUseful, reviewId, reviewId);
    }
}

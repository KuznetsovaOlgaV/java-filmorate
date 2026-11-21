package ru.yandex.practicum.filmorate.storage.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.storage.EventStorage;
import ru.yandex.practicum.filmorate.storage.mapper.EventRowMapper;


import java.util.List;

@Repository
@Primary
@RequiredArgsConstructor
public class EventDbStorage implements EventStorage {
    private final JdbcTemplate jdbcTemplate;
    private final EventRowMapper eventRowMapper;

    @Override
    public List<Event> getEventsByUserId(Long id) {
        String sql = "SELECT e.event_id, e.timestamp, e.user_id, e.event_type, e.operation, e.entity_user_id," +
                "e.entity_film_id, e.entity_review_id " +
                "FROM events e " +
                "WHERE e.user_id = ?";
        return jdbcTemplate.query(sql, eventRowMapper, id);
    }

    @Override
    public void putUserEvent(Long userId, String eventType, String operation, Long entityId) {
        String sql = "INSERT INTO events (user_id, event_type, operation, entity_user_id) " +
                "VALUES (?,?,?,?)";
        jdbcTemplate.update(sql, userId, eventType, operation, entityId);
    }

    @Override
    public void putFilmEvent(Long userId, String eventType, String operation, Long entityId) {
        String sql = "INSERT INTO events (user_id, event_type, operation, entity_film_id) " +
                "VALUES (?,?,?,?)";
        jdbcTemplate.update(sql, userId, eventType, operation, entityId);
    }

    @Override
    public void putReviewEvent(Long userId, String eventType, String operation, Long entityId) {
        String sql = "INSERT INTO events (user_id, event_type, operation, entity_review_id) " +
                "VALUES (?,?,?,?)";
        jdbcTemplate.update(sql, userId, eventType, operation, entityId);
    }

}

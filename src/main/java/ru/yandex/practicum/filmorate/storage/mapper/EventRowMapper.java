package ru.yandex.practicum.filmorate.storage.mapper;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Event;

import java.sql.ResultSet;
import java.sql.SQLException;


@Component
public class EventRowMapper implements RowMapper<Event> {
    @Override
    public Event mapRow(ResultSet rs, int rowNum) throws SQLException {
        return Event.builder()
                .eventId(rs.getLong("event_id"))
                .userId(rs.getLong("user_id"))
                .eventType(rs.getString("event_type"))
                .operation(rs.getString("operation"))
                .timestamp(rs.getTimestamp("timestamp").getTime())
                .entityId(getEntityId(rs))
                .build();
    }

    private Long getEntityId(ResultSet rs) throws SQLException {
        var id = rs.getLong("entity_user_id");
        if (id > 0) return id;
        id = rs.getLong("entity_film_id");
        if (id > 0) return id;
        return rs.getLong("entity_review_id");
    }
}

package ru.yandex.practicum.filmorate.storage.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.storage.mapper.DirectorRowMapper;

import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DirectorDbStorage {
    private final JdbcTemplate jdbcTemplate;
    private final DirectorRowMapper directorRowMapper;

    public List<Director> getAll() {
        String sql = "SELECT director_id, name FROM directors ORDER BY director_id";
        return jdbcTemplate.query(sql, directorRowMapper);
    }

    public Optional<Director> getById(Long id) {
        String sql = "SELECT director_id, name FROM directors WHERE director_id = ?";
        List<Director> directors = jdbcTemplate.query(sql, directorRowMapper, id);
        return directors.isEmpty() ? Optional.empty() : Optional.of(directors.get(0));
    }

    public Director create(Director director) {
        String sql = "INSERT INTO directors (name) VALUES (?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, director.getName());
            return ps;
        }, keyHolder);

        Long generatedId = Objects.requireNonNull(keyHolder.getKey()).longValue();
        director.setId(generatedId);
        log.info("Добавлен режиссёр с ID: {}", generatedId);
        return director;
    }

    public Director update(Director director) {
        String sql = "UPDATE directors SET name = ? WHERE director_id = ?";
        int rowsUpdated = jdbcTemplate.update(sql, director.getName(), director.getId());
        if (rowsUpdated == 0) {
            throw new NotFoundException(String.format("Режиссёр с id = %d не найден", director.getId()));
        }
        log.info("Режиссёр с ID {} обновлён", director.getId());
        return director;
    }

    public void delete(Long id) {
        String sql = "DELETE FROM directors WHERE director_id = ?";
        int rowsDeleted = jdbcTemplate.update(sql, id);
        if (rowsDeleted == 0) {
            throw new NotFoundException(String.format("Режиссёр с id = %d не найден", id));
        }
        log.info("Режиссёр с ID {} удалён", id);
    }
}
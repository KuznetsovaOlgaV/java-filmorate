package ru.yandex.practicum.filmorate.storage.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.storage.mapper.MpaRowMapper;

import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MpaDbStorage {

    private final JdbcTemplate jdbcTemplate;
    private final MpaRowMapper mpaRowMapper;

    public List<Mpa> getAll() {
        String sql = "SELECT * FROM mpa_ratings ORDER BY mpa_id ASC";
        return jdbcTemplate.query(sql, mpaRowMapper);
    }

    public Optional<Mpa> getById(int id) {
        String sql = "SELECT * FROM mpa_ratings WHERE mpa_id = ?";
        List<Mpa> ratings = jdbcTemplate.query(sql, mpaRowMapper, id);
        return ratings.stream().findFirst();
    }
}
package ru.yandex.practicum.filmorate.storage.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.mapper.GenreRowMapper;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class GenreDbStorage {

    private final JdbcTemplate jdbcTemplate;
    private final GenreRowMapper genreRowMapper;

    public List<Genre> getAll() {
        String sql = "SELECT * FROM genres ORDER BY genre_id ASC";
        return jdbcTemplate.query(sql, genreRowMapper);
    }

    public Optional<Genre> getById(int id) {
        String sql = "SELECT * FROM genres WHERE genre_id = ?";
        List<Genre> genres = jdbcTemplate.query(sql, genreRowMapper, id);
        return genres.stream().findFirst();
    }

    public void loadGenresForFilms(List<Film> films) {
        if (films.isEmpty()) {
            return;
        }

        List<Long> filmIds = films.stream().map(Film::getId).collect(Collectors.toList());
        String inSql = String.join(",", Collections.nCopies(filmIds.size(), "?"));

        String sql = String.format(
                "SELECT fg.film_id, g.genre_id, g.name " +
                        "FROM film_genres fg " +
                        "JOIN genres g ON fg.genre_id = g.genre_id " +
                        "WHERE fg.film_id IN (%s) " +
                        "ORDER BY g.genre_id ASC", inSql);

        Map<Long, LinkedHashSet<Genre>> genresByFilmId = jdbcTemplate.query(sql, rs -> {
            Map<Long, LinkedHashSet<Genre>> map = new java.util.HashMap<>();
            while (rs.next()) {
                Long filmId = rs.getLong("film_id");
                Genre genre = new Genre(rs.getInt("genre_id"), rs.getString("name"));
                map.computeIfAbsent(filmId, k -> new LinkedHashSet<>()).add(genre);
            }
            return map;
        }, filmIds.toArray());

        if (genresByFilmId != null) {
            films.forEach(film -> film.setGenres(genresByFilmId.getOrDefault(film.getId(), new LinkedHashSet<>())));
        }
    }
}
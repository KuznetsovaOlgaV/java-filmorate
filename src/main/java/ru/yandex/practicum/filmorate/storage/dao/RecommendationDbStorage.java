package ru.yandex.practicum.filmorate.storage.dao;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.mapper.FilmRowMapper;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class RecommendationDbStorage {
    private final JdbcTemplate jdbcTemplate;
    private final FilmRowMapper filmRowMapper;

    public List<Film> getRecommendations(Long userId) {
        String sql = """
                SELECT f.film_id, f.name, f.description, f.release_date, f.duration,
                       f.mpa_rating_id, m.name AS mpa_name, m.description AS mpa_description
                FROM films f
                LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.mpa_id
                WHERE f.film_id IN (
                    SELECT DISTINCT l.film_id
                    FROM likes l
                    WHERE l.user_id IN (
                        SELECT l2.user_id
                        FROM likes l1
                        JOIN likes l2 ON l1.film_id = l2.film_id
                        WHERE l1.user_id = ? AND l2.user_id <> ?
                        GROUP BY l2.user_id
                        HAVING COUNT(*) = (
                            SELECT MAX(match_count)
                            FROM (
                                SELECT COUNT(*) AS match_count
                                FROM likes l3
                                JOIN likes l4 ON l3.film_id = l4.film_id
                                WHERE l3.user_id = ? AND l4.user_id <> ?
                                GROUP BY l4.user_id
                            ) matches
                        )
                    )
                    AND l.film_id NOT IN (
                        SELECT film_id FROM likes WHERE user_id = ?
                    )
                )
                ORDER BY f.film_id
                """;

        List<Film> films = jdbcTemplate.query(sql, filmRowMapper, userId, userId, userId, userId, userId);
        loadGenresForFilms(films);
        loadDirectorsForFilms(films);
        return films;
    }

    private void loadGenresForFilms(List<Film> films) {
        if (films.isEmpty()) {
            return;
        }
        Map<Long, Film> filmMap = films.stream()
                .collect(Collectors.toMap(Film::getId, f -> f));

        String inSql = String.join(",", Collections.nCopies(films.size(), "?"));
        String sql = "SELECT fg.film_id, g.genre_id, g.name " +
                "FROM film_genres fg " +
                "JOIN genres g ON fg.genre_id = g.genre_id " +
                "WHERE fg.film_id IN (" + inSql + ") " +
                "ORDER BY g.genre_id";

        Object[] filmIds = filmMap.keySet().toArray();
        jdbcTemplate.query(sql, rs -> {
            Long filmId = rs.getLong("film_id");
            Genre genre = new Genre(rs.getInt("genre_id"), rs.getString("name"));
            Film film = filmMap.get(filmId);
            if (film != null) {
                film.getGenres().add(genre);
            }
        }, filmIds);
    }

    private void loadDirectorsForFilms(List<Film> films) {
        if (films.isEmpty()) {
            return;
        }
        Map<Long, Film> filmMap = films.stream()
                .collect(Collectors.toMap(Film::getId, f -> f));

        String inSql = String.join(",", Collections.nCopies(films.size(), "?"));
        String sql = "SELECT fd.film_id, d.director_id, d.name " +
                "FROM film_directors fd " +
                "JOIN directors d ON fd.director_id = d.director_id " +
                "WHERE fd.film_id IN (" + inSql + ") " +
                "ORDER BY d.director_id";

        Object[] filmIds = filmMap.keySet().toArray();
        jdbcTemplate.query(sql, rs -> {
            Long filmId = rs.getLong("film_id");
            Director director = new Director(rs.getLong("director_id"), rs.getString("name"));
            Film film = filmMap.get(filmId);
            if (film != null) {
                film.getDirectors().add(director);
            }
        }, filmIds);
    }
}
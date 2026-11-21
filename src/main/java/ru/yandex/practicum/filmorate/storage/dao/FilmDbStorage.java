package ru.yandex.practicum.filmorate.storage.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.mapper.DirectorRowMapper;
import ru.yandex.practicum.filmorate.storage.mapper.FilmRowMapper;

import java.sql.*;
import java.sql.Date;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Repository
@Primary
@RequiredArgsConstructor
public class FilmDbStorage implements FilmStorage {
    private final JdbcTemplate jdbcTemplate;
    private final FilmRowMapper filmRowMapper;
    private final DirectorRowMapper directorRowMapper;

    private static final String BASE_FILM_SELECT =
            "SELECT f.film_id, f.name, f.description, f.release_date, f.duration, " +
                    "f.mpa_rating_id, m.name AS mpa_name, m.description AS mpa_description " +
                    "FROM films f " +
                    "LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.mpa_id ";

    @Override
    public Film create(Film film) {
        String sql = "INSERT INTO films (name, description, release_date, duration, mpa_rating_id) " +
                "VALUES (?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        jdbcTemplate.update(connection -> {
            PreparedStatement ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, film.getName());
            ps.setString(2, film.getDescription());
            ps.setDate(3, Date.valueOf(film.getReleaseDate()));
            ps.setInt(4, film.getDuration());
            if (film.getMpa() != null) {
                ps.setInt(5, film.getMpa().getId());
            } else {
                ps.setNull(5, java.sql.Types.INTEGER);
            }
            return ps;
        }, keyHolder);

        Long generatedId = Objects.requireNonNull(keyHolder.getKey()).longValue();
        film.setId(generatedId);
        setFilmGenres(film);
        setFilmDirectors(film);

        log.info("Фильм успешно добавлен в БД с ID: {}", film.getId());
        return getById(generatedId).orElseThrow(() -> new NotFoundException("Фильм не найден"));
    }

    @Override
    public Film update(Film film) {
        String sql = "UPDATE films SET name = ?, description = ?, release_date = ?, " +
                "duration = ?, mpa_rating_id = ? WHERE film_id = ?";
        int rowsUpdated = jdbcTemplate.update(sql,
                film.getName(),
                film.getDescription(),
                Date.valueOf(film.getReleaseDate()),
                film.getDuration(),
                film.getMpa() != null ? film.getMpa().getId() : null,
                film.getId()
        );
        if (rowsUpdated == 0) {
            throw new NotFoundException("Фильм с ID " + film.getId() + " не найден");
        }
        setFilmGenres(film);
        setFilmDirectors(film);

        log.info("Фильм с ID {} успешно обновлён", film.getId());
        return getById(film.getId()).orElseThrow(() -> new NotFoundException("Фильм не найден"));
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM films WHERE film_id = ?";
        int rowsDeleted = jdbcTemplate.update(sql, id);
        if (rowsDeleted == 0) {
            throw new NotFoundException("Фильм с ID " + id + " не найден для удаления");
        }
        log.info("Фильм с ID {} успешно удалён", id);
    }

    @Override
    public Optional<Film> getById(Long id) {
        String sql = BASE_FILM_SELECT + "WHERE f.film_id = ?";
        List<Film> films = jdbcTemplate.query(sql, filmRowMapper, id);
        if (films.isEmpty()) {
            return Optional.empty();
        }
        Film film = films.get(0);
        loadGenresForFilms(List.of(film));
        loadDirectorsForFilms(List.of(film));
        return Optional.of(film);
    }

    @Override
    public List<Film> getAll() {
        List<Film> films = jdbcTemplate.query(BASE_FILM_SELECT + "ORDER BY f.film_id", filmRowMapper);
        loadGenresForFilms(films);
        loadDirectorsForFilms(films);
        return films;
    }

    public void addLike(Long filmId, Long userId) {
        String sql = "MERGE INTO likes (film_id, user_id) KEY (film_id, user_id) VALUES (?, ?)";
        jdbcTemplate.update(sql, filmId, userId);
    }

    public void removeLike(Long filmId, Long userId) {
        String sql = "DELETE FROM likes WHERE film_id = ? AND user_id = ?";
        jdbcTemplate.update(sql, filmId, userId);
    }

    public List<Film> getPopular(int count) {
        String sql = "SELECT f.film_id, f.name, f.description, f.release_date, f.duration, " +
                "f.mpa_rating_id, m.name AS mpa_name, m.description AS mpa_description, " +
                "COUNT(l.user_id) AS likes_count " +
                "FROM films f " +
                "LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.mpa_id " +
                "LEFT JOIN likes l ON f.film_id = l.film_id " +
                "GROUP BY f.film_id, f.name, f.description, f.release_date, f.duration, " +
                "f.mpa_rating_id, m.name, m.description " +
                "ORDER BY likes_count DESC, f.film_id ASC " +
                "LIMIT ?";
        List<Film> films = jdbcTemplate.query(sql, filmRowMapper, count);
        loadGenresForFilms(films);
        loadDirectorsForFilms(films);
        return films;
    }

    public List<Film> getDirectorFilms(Long directorId, String sortBy) {
        String sql;
        if ("likes".equals(sortBy)) {
            sql = "SELECT f.film_id, f.name, f.description, f.release_date, f.duration, " +
                    "f.mpa_rating_id, m.name AS mpa_name, m.description AS mpa_description, " +
                    "COUNT(l.user_id) AS likes_count " +
                    "FROM films f " +
                    "LEFT JOIN mpa_ratings m ON f.mpa_rating_id = m.mpa_id " +
                    "JOIN film_directors fd ON f.film_id = fd.film_id " +
                    "LEFT JOIN likes l ON f.film_id = l.film_id " +
                    "WHERE fd.director_id = ? " +
                    "GROUP BY f.film_id, f.name, f.description, f.release_date, f.duration, " +
                    "f.mpa_rating_id, m.name, m.description " +
                    "ORDER BY likes_count DESC, f.film_id ASC";
        } else {
            sql = BASE_FILM_SELECT +
                    "JOIN film_directors fd ON f.film_id = fd.film_id " +
                    "WHERE fd.director_id = ? " +
                    "ORDER BY EXTRACT(YEAR FROM f.release_date) ASC, f.film_id ASC";
        }
        List<Film> films = jdbcTemplate.query(sql, filmRowMapper, directorId);
        loadGenresForFilms(films);
        loadDirectorsForFilms(films);
        return films;
    }

    private void setFilmGenres(Film film) {
        String deleteSql = "DELETE FROM film_genres WHERE film_id = ?";
        jdbcTemplate.update(deleteSql, film.getId());
        if (film.getGenres() == null || film.getGenres().isEmpty()) {
            return;
        }
        LinkedHashSet<Genre> uniqueGenres = new LinkedHashSet<>(film.getGenres());
        String insertSql = "INSERT INTO film_genres (film_id, genre_id) VALUES (?, ?)";
        List<Object[]> batchParams = uniqueGenres.stream()
                .map(genre -> new Object[]{film.getId(), genre.getId()})
                .collect(Collectors.toList());
        jdbcTemplate.batchUpdate(insertSql, batchParams);
        film.setGenres(uniqueGenres);
    }

    private void setFilmDirectors(Film film) {
        String deleteSql = "DELETE FROM film_directors WHERE film_id = ?";
        jdbcTemplate.update(deleteSql, film.getId());
        if (film.getDirectors() == null || film.getDirectors().isEmpty()) {
            return;
        }
        LinkedHashSet<Director> uniqueDirectors = new LinkedHashSet<>(film.getDirectors());
        String insertSql = "INSERT INTO film_directors (film_id, director_id) VALUES (?, ?)";
        List<Object[]> batchParams = uniqueDirectors.stream()
                .map(director -> new Object[]{film.getId(), director.getId()})
                .collect(Collectors.toList());
        jdbcTemplate.batchUpdate(insertSql, batchParams);
        film.setDirectors(uniqueDirectors);
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

    @Override
    public List<Film> search(String query, String by) {
        String pattern = "%" + query.toLowerCase() + "%";
        Set<String> fields = parseSearchFields(by);   // {title, director}

        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (fields.contains("title")) {
            conditions.add("LOWER(f.film_name) LIKE ?");
            params.add(pattern);
        }
        if (fields.contains("director")) {
            conditions.add(
                    "EXISTS (SELECT 1 FROM film_directors fd " +
                            "JOIN directors d ON d.director_id = fd.director_id " +
                            "WHERE fd.film_id = f.film_id AND LOWER(d.name) LIKE ?)"
            );
            params.add(pattern);
        }

        if (conditions.isEmpty()) return List.of();

        String sql = "SELECT f.*, " +
                "(SELECT COUNT(*) FROM likes l WHERE l.film_id = f.film_id) AS likes_count " +
                "FROM films f " +
                "WHERE " + String.join(" OR ", conditions) + " " +
                "ORDER BY likes_count DESC";

        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Film film = filmRowMapper.mapRow(rs, rowNum);
            if (film != null) {
                film.setDirectors(loadDirectors(film.getId()));
            }
            return film;
        }, params.toArray());
    }

    private LinkedHashSet<Director> loadDirectors(Long filmId) {
        String sql = "SELECT d.* FROM directors d " +
                "JOIN film_directors fd ON fd.director_id = d.director_id " +
                "WHERE fd.film_id = ? ORDER BY d.director_id";
        return new LinkedHashSet<>(jdbcTemplate.query(sql, directorRowMapper, filmId));
    }

    private Set<String> parseSearchFields(String by) {
        if (by == null || by.isBlank()) return Set.of("title", "director");
        Set<String> result = new HashSet<>();
        for (String part : by.toLowerCase().split(",")) {
            String t = part.trim();
            if (t.equals("title") || t.equals("director")) result.add(t);
        }
        return result;
    }
}
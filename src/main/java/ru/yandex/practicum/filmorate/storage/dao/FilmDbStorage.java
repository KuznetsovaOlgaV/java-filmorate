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
import ru.yandex.practicum.filmorate.storage.mapper.FilmRowMapper;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Repository
@Primary
@RequiredArgsConstructor
public class FilmDbStorage implements FilmStorage {
    private final JdbcTemplate jdbcTemplate;
    private final FilmRowMapper filmRowMapper;
    private static final String SEARCH_BY_TITLE = "title";
    private static final String SEARCH_BY_DIRECTOR = "director";
    private static final String SEARCH_BY_DESCRIPTION = "description";
    private static final Set<String> DEFAULT_SEARCH_FIELDS = Set.of(SEARCH_BY_TITLE, SEARCH_BY_DIRECTOR);
    private static final String SORT_BY_YEAR = "year";

    private static final String BASE_FILM_SELECT =
            "SELECT f.film_id, f.name, f.description, f.release_date, f.duration, " +
                    "f.mpa_rating_id, m.name AS mpa_name, m.description AS mpa_description, " +
                    "(SELECT COALESCE(AVG(CAST(r.rating AS DOUBLE PRECISION)), 0.0) " +
                    " FROM film_ratings r WHERE r.film_id = f.film_id) AS rating " +
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
            if (film.getReleaseDate() != null) {
                ps.setDate(3, Date.valueOf(film.getReleaseDate()));
            } else {
                ps.setNull(3, java.sql.Types.DATE);
            }
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
                film.getReleaseDate() != null ? Date.valueOf(film.getReleaseDate()) : null,
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
        enrichFilms(films);
        return Optional.of(films.get(0));
    }

    @Override
    public List<Film> getAll() {
        List<Film> films = jdbcTemplate.query(BASE_FILM_SELECT + "ORDER BY f.film_id", filmRowMapper);
        enrichFilms(films);
        return films;
    }

    @Override
    public List<Film> getPopular(Integer count, Integer genreId, Integer year) {
        StringBuilder sql = new StringBuilder(BASE_FILM_SELECT);
        List<Object> params = new ArrayList<>();
        List<String> conditions = new ArrayList<>();

        if (genreId != null) {
            conditions.add(
                    "EXISTS (SELECT 1 FROM film_genres fg " +
                            "WHERE fg.film_id = f.film_id AND fg.genre_id = ?)"
            );
            params.add(genreId);
        }
        if (year != null) {
            conditions.add("EXTRACT(YEAR FROM f.release_date) = ?");
            params.add(year);
        }
        if (!conditions.isEmpty()) {
            sql.append("WHERE ").append(String.join(" AND ", conditions)).append(" ");
        }

        sql.append("ORDER BY rating DESC, " +
                "(SELECT COUNT(*) FROM film_ratings r WHERE r.film_id = f.film_id) DESC, " +
                "f.film_id ASC");

        List<Film> films = jdbcTemplate.query(
                sql.toString(),
                ps -> {
                    if (count != null) {
                        ps.setMaxRows(count);
                    }
                    for (int i = 0; i < params.size(); i++) {
                        ps.setObject(i + 1, params.get(i));
                    }
                },
                filmRowMapper
        );
        enrichFilms(films);
        return films;
    }

    public List<Film> getDirectorFilms(Long directorId, String sortBy) {
        String orderBy = SORT_BY_YEAR.equals(sortBy)
                ? "ORDER BY EXTRACT(YEAR FROM f.release_date) ASC, f.film_id ASC"
                : "ORDER BY rating DESC, (SELECT COUNT(*) FROM film_ratings r WHERE r.film_id = f.film_id) DESC, f.film_id ASC";

        String sql = BASE_FILM_SELECT +
                "JOIN film_directors fd ON f.film_id = fd.film_id " +
                "WHERE fd.director_id = ? " +
                orderBy;

        List<Film> films = jdbcTemplate.query(sql, filmRowMapper, directorId);
        enrichFilms(films);
        return films;
    }


    public List<Film> getCommon(Long userId, Long friendId) {
        String sql = BASE_FILM_SELECT +
                "WHERE f.film_id IN (" +
                "  SELECT film_id FROM film_ratings WHERE user_id = ? " +
                "  INTERSECT " +
                "  SELECT film_id FROM film_ratings WHERE user_id = ?" +
                ") " +
                "ORDER BY rating DESC, f.film_id ASC";

        List<Film> films = jdbcTemplate.query(sql, filmRowMapper, userId, friendId);
        enrichFilms(films);
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
        Set<String> fields = parseSearchFields(by);
        List<String> conditions = new ArrayList<>();
        List<Object> params = new ArrayList<>();

        if (fields.contains(SEARCH_BY_TITLE)) {
            conditions.add("LOWER(f.name) LIKE ?");
            params.add(pattern);
        }
        if (fields.contains(SEARCH_BY_DIRECTOR)) {
            conditions.add(
                    "EXISTS (SELECT 1 FROM film_directors fd " +
                            "JOIN directors d ON d.director_id = fd.director_id " +
                            "WHERE fd.film_id = f.film_id AND LOWER(d.name) LIKE ?)"
            );
            params.add(pattern);
        }

        //поиск по описанию фильма
        if (fields.contains(SEARCH_BY_DESCRIPTION)) {
            conditions.add("LOWER(f.description) LIKE ?");
            params.add(pattern);
        }
        if (conditions.isEmpty()) {
            return List.of();
        }

        String sql = BASE_FILM_SELECT +
                "WHERE " + String.join(" OR ", conditions) + " " +
                "ORDER BY rating DESC, f.film_id ASC";

        List<Film> films = jdbcTemplate.query(sql, filmRowMapper, params.toArray());
        enrichFilms(films);
        return films;
    }

    private void enrichFilms(List<Film> films) {
        loadGenresForFilms(films);
        loadDirectorsForFilms(films);
        loadRatingsForFilms(films);
    }

    private void loadRatingsForFilms(List<Film> films) {
        if (films.isEmpty()) {
            return;
        }
        Map<Long, Film> filmMap = films.stream()
                .collect(Collectors.toMap(Film::getId, f -> f));
        String inSql = String.join(",", Collections.nCopies(films.size(), "?"));
        String sql = "SELECT film_id, user_id, rating FROM film_ratings WHERE film_id IN (" + inSql + ")";

        jdbcTemplate.query(sql, rs -> {
            long filmId = rs.getLong("film_id");
            long userId = rs.getLong("user_id");
            int rating = rs.getInt("rating");
            Film film = filmMap.get(filmId);
            if (film != null) {
                film.getRatings().put(userId, rating);
            }
        }, filmMap.keySet().toArray());
    }

    private Set<String> parseSearchFields(String by) {
        if (by == null || by.isBlank()) return DEFAULT_SEARCH_FIELDS;
        Set<String> result = new HashSet<>();
        for (String part : by.toLowerCase().split(",")) {
            String t = part.trim();
            if (t.equals(SEARCH_BY_TITLE) || t.equals(SEARCH_BY_DIRECTOR) || t.equals(SEARCH_BY_DESCRIPTION)) {
                result.add(t);
            }
        }
        return result;
    }

    @Override
    public boolean upsertRating(Long filmId, Long userId, int rating) {
        String updateSql = "UPDATE film_ratings SET rating = ? WHERE film_id = ? AND user_id = ?";
        int updated = jdbcTemplate.update(updateSql, rating, filmId, userId);
        if (updated == 0) {
            String insertSql = "INSERT INTO film_ratings (film_id, user_id, rating) VALUES (?, ?, ?)";
            jdbcTemplate.update(insertSql, filmId, userId, rating);
            return true;
        }
        return false;
    }

    @Override
    public void removeRating(Long filmId, Long userId) {
        String sql = "DELETE FROM film_ratings WHERE film_id = ? AND user_id = ?";
        jdbcTemplate.update(sql, filmId, userId);
    }

    @Override
    public List<Film> getRecommendations(Long userId) {
        Map<Long, Map<Long, Integer>> allRatings = new HashMap<>();
        String sql = "SELECT user_id, film_id, rating FROM film_ratings";
        jdbcTemplate.query(sql, rs -> {
            long uId = rs.getLong("user_id");
            long fId = rs.getLong("film_id");
            int rating = rs.getInt("rating");
            allRatings.computeIfAbsent(uId, k -> new HashMap<>()).put(fId, rating);
        });

        Map<Long, Integer> targetRatings = allRatings.get(userId);
        if (targetRatings == null || targetRatings.isEmpty()) {
            return List.of();
        }

        record UserSimilarity(long userId, double avgDiff, int commonCount) {
        }
        List<UserSimilarity> similarUsers = new ArrayList<>();

        for (var entry : allRatings.entrySet()) {
            long otherId = entry.getKey();
            if (otherId == userId) {
                continue;
            }
            Map<Long, Integer> otherRatings = entry.getValue();

            int commonCount = 0;
            double diffSum = 0.0;

            for (var targetEntry : targetRatings.entrySet()) {
                Integer otherScore = otherRatings.get(targetEntry.getKey());
                if (otherScore != null) {
                    commonCount++;
                    int targetScore = targetEntry.getValue();
                    double diff = Math.abs(targetScore - otherScore);
                    if ((targetScore >= 6) != (otherScore >= 6)) {
                        diff += 5.0;
                    }
                    diffSum += diff;
                }
            }

            if (commonCount > 0) {
                similarUsers.add(new UserSimilarity(otherId, diffSum / commonCount, commonCount));
            }
        }

        similarUsers.sort(
                Comparator.comparingDouble(UserSimilarity::avgDiff)
                        .thenComparing(Comparator.comparingInt(UserSimilarity::commonCount).reversed())
        );

        List<Long> recommendedIds = List.of();
        for (UserSimilarity similar : similarUsers) {
            Map<Long, Integer> candidateRatings = allRatings.get(similar.userId());
            List<Long> candidates = candidateRatings.entrySet().stream()
                    .filter(e -> !targetRatings.containsKey(e.getKey()))
                    .filter(e -> e.getValue() >= 6)
                    .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                    .map(Map.Entry::getKey)
                    .toList();

            if (!candidates.isEmpty()) {
                recommendedIds = candidates;
                break;
            }
        }

        if (recommendedIds.isEmpty()) {
            return List.of();
        }

        String placeholders = recommendedIds.stream()
                .map(id -> "?")
                .collect(Collectors.joining(", "));
        String filmsSql = BASE_FILM_SELECT + "WHERE f.film_id IN (" + placeholders + ")";

        List<Film> films = jdbcTemplate.query(filmsSql, filmRowMapper, recommendedIds.toArray());
        enrichFilms(films);

        Map<Long, Integer> orderMap = new HashMap<>();
        for (int i = 0; i < recommendedIds.size(); i++) {
            orderMap.put(recommendedIds.get(i), i);
        }
        films.sort(Comparator.comparingInt(f -> orderMap.getOrDefault(f.getId(), Integer.MAX_VALUE)));

        return films;
    }
}
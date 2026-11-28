package ru.yandex.practicum.filmorate.storage.film;

import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class InMemoryFilmStorage implements FilmStorage {
    private final Map<Long, Film> films = new HashMap<>();
    private long idCounter = 0;

    @Override
    public Film create(Film film) {
        film.setId(++idCounter);
        if (film.getRatings() == null) {
            film.setRatings(new HashMap<>());
        }
        if (film.getRating() == null) {
            film.setRating(0.0);
        }
        films.put(film.getId(), film);
        return film;
    }

    @Override
    public Film update(Film film) {
        if (film.getRatings() == null) {
            film.setRatings(new HashMap<>());
        }
        recalculateRating(film);
        films.put(film.getId(), film);
        return film;
    }

    @Override
    public List<Film> getAll() {
        return new ArrayList<>(films.values());
    }

    @Override
    public Optional<Film> getById(Long id) {
        return Optional.ofNullable(films.get(id));
    }

    @Override
    public List<Film> getPopular(Integer count, Integer genreId, Integer year) {
        int limit = count != null ? count : 10;
        return films.values().stream()
                .filter(film -> genreId == null || (film.getGenres() != null
                        && film.getGenres().stream().anyMatch(g -> Objects.equals(g.getId(), genreId))))
                .filter(film -> year == null || (film.getReleaseDate() != null
                        && film.getReleaseDate().getYear() == year))
                .sorted(Comparator.comparingDouble(
                        (Film f) -> f.getRating() != null ? f.getRating() : 0.0
                ).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    @Override
    public List<Film> search(String query, String by) {
        throw new UnsupportedOperationException("search operation is not supported in InMemoryFilmStorage");
    }

    public void delete(Long id) {
        films.remove(id);
    }

    public boolean existsById(Long id) {
        return films.containsKey(id);
    }

    @Override
    public boolean upsertRating(Long filmId, Long userId, int rating) {
        Film film = films.get(filmId);
        if (film == null) {
            return false;
        }
        if (film.getRatings() == null) {
            film.setRatings(new HashMap<>());
        }
        boolean isNew = film.getRatings().put(userId, rating) == null;
        recalculateRating(film);
        return isNew;
    }

    @Override
    public void removeRating(Long filmId, Long userId) {
        Film film = films.get(filmId);
        if (film != null && film.getRatings() != null) {
            film.getRatings().remove(userId);
            recalculateRating(film);
        }
    }

    @Override
    public List<Film> getRecommendations(Long userId) {
        Map<Long, Integer> targetRatings = new HashMap<>();
        Map<Long, Map<Long, Integer>> otherUsersRatings = new HashMap<>();

        for (Film film : films.values()) {
            if (film.getRatings() == null) {
                continue;
            }
            for (Map.Entry<Long, Integer> entry : film.getRatings().entrySet()) {
                Long raterId = entry.getKey();
                Integer score = entry.getValue();
                if (Objects.equals(raterId, userId)) {
                    targetRatings.put(film.getId(), score);
                } else {
                    otherUsersRatings
                            .computeIfAbsent(raterId, k -> new HashMap<>())
                            .put(film.getId(), score);
                }
            }
        }

        if (targetRatings.isEmpty()) {
            return Collections.emptyList();
        }

        record Neighbor(Long userId, double avgDiff, int commonCount) {
        }

        List<Neighbor> neighbors = new ArrayList<>();
        for (Map.Entry<Long, Map<Long, Integer>> entry : otherUsersRatings.entrySet()) {
            int commonCount = 0;
            double diffSum = 0.0;
            for (Map.Entry<Long, Integer> filmRating : entry.getValue().entrySet()) {
                Integer targetScore = targetRatings.get(filmRating.getKey());
                if (targetScore != null) {
                    commonCount++;
                    diffSum += Math.abs(targetScore - filmRating.getValue());
                }
            }
            if (commonCount > 0) {
                neighbors.add(new Neighbor(entry.getKey(), diffSum / commonCount, commonCount));
            }
        }

        neighbors.sort(Comparator.comparingDouble(Neighbor::avgDiff)
                .thenComparing(Comparator.comparingInt(Neighbor::commonCount).reversed()));

        for (Neighbor neighbor : neighbors) {
            Map<Long, Integer> neighborRatings = otherUsersRatings.get(neighbor.userId());
            List<Film> recommended = neighborRatings.entrySet().stream()
                    .filter(e -> !targetRatings.containsKey(e.getKey()) && e.getValue() >= 6)
                    .sorted(Map.Entry.<Long, Integer>comparingByValue().reversed())
                    .map(e -> films.get(e.getKey()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            if (!recommended.isEmpty()) {
                return recommended;
            }
        }

        return Collections.emptyList();
    }

    private void recalculateRating(Film film) {
        if (film.getRatings() == null || film.getRatings().isEmpty()) {
            film.setRating(0.0);
            return;
        }
        double avg = film.getRatings().values().stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);
        film.setRating(avg);
    }
}
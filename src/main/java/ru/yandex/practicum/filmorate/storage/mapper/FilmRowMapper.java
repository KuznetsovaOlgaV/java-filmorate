package ru.yandex.practicum.filmorate.storage.mapper;

import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Mpa;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashSet;

@Component
public class FilmRowMapper implements RowMapper<Film> {
    @Override
    public Film mapRow(ResultSet rs, int rowNum) throws SQLException {
        Film film = Film.builder()
                .id(rs.getLong("film_id"))
                .name(rs.getString("name"))
                .description(rs.getString("description"))
                .releaseDate(rs.getDate("release_date").toLocalDate())
                .duration(rs.getInt("duration"))
                .genres(new LinkedHashSet<>())
                .build();

        int mpaId = rs.getInt("mpa_rating_id");
        if (!rs.wasNull()) {
            Mpa mpa = Mpa.builder()
                    .id(mpaId)
                    .name(rs.getString("mpa_name"))
                    .description(rs.getString("mpa_description"))
                    .build();
            film.setMpa(mpa);
        }

        return film;
    }
}
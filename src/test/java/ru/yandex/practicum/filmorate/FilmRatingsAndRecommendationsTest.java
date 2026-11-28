package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.FilmService;
import ru.yandex.practicum.filmorate.service.UserService;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class FilmRatingsAndRecommendationsTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private FilmService filmService;

    @Autowired
    private UserService userService;

    private User createUser(String email, String login) {
        return userService.create(User.builder()
                .email(email)
                .login(login)
                .name(login)
                .birthday(LocalDate.of(1995, 1, 1))
                .build());
    }

    private Film createFilm(String name) {
        return filmService.create(Film.builder()
                .name(name)
                .description("Desc")
                .releaseDate(LocalDate.of(2020, 1, 1))
                .duration(120)
                .mpa(Mpa.builder().id(1).build())
                .build());
    }

    @Test
    void shouldValidateRatingRangeAndRecalculateAverage() throws Exception {
        User u1 = createUser("u1@mail.ru", "u1");
        User u2 = createUser("u2@mail.ru", "u2");
        Film film = createFilm("Film 1");

        mockMvc.perform(put("/films/{id}/like/{userId}?rate=0", film.getId(), u1.getId()))
                .andExpect(status().isBadRequest());
        mockMvc.perform(put("/films/{id}/like/{userId}?rate=11", film.getId(), u1.getId()))
                .andExpect(status().isBadRequest());

        mockMvc.perform(put("/films/{id}/like/{userId}?rate=7", film.getId(), u1.getId()))
                .andExpect(status().isOk());
        mockMvc.perform(put("/films/{id}/like/{userId}?rate=8", film.getId(), u2.getId()))
                .andExpect(status().isOk());
        assertEquals(7.5, filmService.getById(film.getId()).getRating(), 0.001);

        mockMvc.perform(put("/films/{id}/like/{userId}?rate=10", film.getId(), u1.getId()))
                .andExpect(status().isOk());
        assertEquals(9.0, filmService.getById(film.getId()).getRating(), 0.001);

        mockMvc.perform(delete("/films/{id}/like/{userId}", film.getId(), u1.getId()))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/films/{id}/like/{userId}", film.getId(), u2.getId()))
                .andExpect(status().isOk());
        assertEquals(0.0, filmService.getById(film.getId()).getRating(), 0.001);
    }

    @Test
    void shouldSortPopularByAverageRatingNotCount() {
        User u1 = createUser("u1@mail.ru", "u1");
        User u2 = createUser("u2@mail.ru", "u2");
        User u3 = createUser("u3@mail.ru", "u3");

        Film highAvg = createFilm("High Avg");
        Film manyMid = createFilm("Many Mid");
        Film noRates = createFilm("No Rates");

        filmService.addRating(highAvg.getId(), u1.getId(), 9);
        filmService.addRating(highAvg.getId(), u2.getId(), 10);

        filmService.addRating(manyMid.getId(), u1.getId(), 8);
        filmService.addRating(manyMid.getId(), u2.getId(), 8);
        filmService.addRating(manyMid.getId(), u3.getId(), 8);

        List<Film> popular = filmService.getPopular(10, null, null);
        assertEquals(
                List.of(highAvg.getId(), manyMid.getId(), noRates.getId()),
                popular.stream().map(Film::getId).toList()
        );
    }

    @Test
    void shouldRecommendOnlyPositiveUnseenFilmsFromSimilarUser() {
        User target = createUser("t@mail.ru", "target");
        User similar = createUser("s@mail.ru", "similar");
        User opposite = createUser("o@mail.ru", "opposite");

        Film f1 = createFilm("Common Good");
        Film f2 = createFilm("Common Bad");
        Film rec = createFilm("Good Unseen");
        Film badRec = createFilm("Bad Unseen");
        Film oppRec = createFilm("Opposite Rec");

        filmService.addRating(f1.getId(), target.getId(), 9);
        filmService.addRating(f2.getId(), target.getId(), 2);

        filmService.addRating(f1.getId(), similar.getId(), 8);
        filmService.addRating(f2.getId(), similar.getId(), 3);
        filmService.addRating(rec.getId(), similar.getId(), 9);
        filmService.addRating(badRec.getId(), similar.getId(), 5);

        filmService.addRating(f1.getId(), opposite.getId(), 2);
        filmService.addRating(f2.getId(), opposite.getId(), 9);
        filmService.addRating(oppRec.getId(), opposite.getId(), 10);

        List<Film> recommendations = userService.getRecommendations(target.getId());
        assertEquals(List.of(rec.getId()), recommendations.stream().map(Film::getId).toList());
    }
}
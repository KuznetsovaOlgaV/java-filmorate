package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import ru.yandex.practicum.filmorate.controller.ErrorHandler;
import ru.yandex.practicum.filmorate.controller.FilmController;
import ru.yandex.practicum.filmorate.model.Director;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.service.*;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(FilmController.class)
@Import(ErrorHandler.class)
class FilmControllerSearchTest {

    @Autowired private MockMvc mockMvc;

    @MockBean private FilmService filmService;
    // добавьте те @MockBean, которые реально есть в конструкторе FilmController:
    @MockBean private UserService userService;
    @MockBean private GenreService genreService;
    @MockBean private MpaService mpaService;
    @MockBean private DirectorService directorService;

    private Film sampleFilm() {
        return Film.builder()
                .id(1L)
                .name("Крадущийся тигр, затаившийся дракон")
                .description("Эпическая драма")
                .releaseDate(LocalDate.of(2000, 7, 7))
                .duration(120)
                .mpa(new Mpa(1, "G"))
                .genres(new LinkedHashSet<>(List.of(new Genre(1, "Драма"))))
                .directors(new LinkedHashSet<>(List.of(new Director(1L, "Энг Ли"))))
                .build();
    }

    // ---------- успешные сценарии ----------

    @Test
    void searchByTitleAndDirectorReturnsFilms() throws Exception {
        when(filmService.search("крад", "title,director"))
                .thenReturn(List.of(sampleFilm()));

        mockMvc.perform(get("/films/search")
                        .param("query", "крад")
                        .param("by", "title,director"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Крадущийся тигр, затаившийся дракон"))
                .andExpect(jsonPath("$[0].description").value("Эпическая драма"))
                .andExpect(jsonPath("$[0].releaseDate").value("2000-07-07"))
                .andExpect(jsonPath("$[0].duration").value(120))
                .andExpect(jsonPath("$[0].mpa.id").value(1))
                .andExpect(jsonPath("$[0].genres[0].id").value(1))
                .andExpect(jsonPath("$[0].directors[0].id").value(1))
                .andExpect(jsonPath("$[0].directors[0].name").value("Энг Ли"));

        verify(filmService).search("крад", "title,director");
    }

    @Test
    void searchPassesQueryAndByToService() throws Exception {
        when(filmService.search(eq("awE9d0I"), eq("title,director")))
                .thenReturn(List.of());

        mockMvc.perform(get("/films/search")
                        .param("query", "awE9d0I")
                        .param("by", "title,director"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));

        verify(filmService).search("awE9d0I", "title,director");
    }

    @Test
    void searchWithoutByUsesDefaultValue() throws Exception {
        when(filmService.search("крад", "title,director")).thenReturn(List.of());

        // by не передан — контроллер подставляет defaultValue = "title,director"
        mockMvc.perform(get("/films/search").param("query", "крад"))
                .andExpect(status().isOk());

        verify(filmService).search("крад", "title,director");
    }

    @Test
    void searchWithEmptyResultReturnsEmptyArray() throws Exception {
        when(filmService.search("абракадабра", "title,director")).thenReturn(List.of());

        mockMvc.perform(get("/films/search")
                        .param("query", "абракадабра")
                        .param("by", "title,director"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void searchWithMultipleResultsPreservesOrder() throws Exception {
        Film a = Film.builder().id(1L).name("Крадущийся 1").build();
        Film b = Film.builder().id(2L).name("Крадущийся 2").build();
        when(filmService.search("крад", "title,director")).thenReturn(List.of(a, b));

        mockMvc.perform(get("/films/search")
                        .param("query", "крад")
                        .param("by", "title,director"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    void searchWithOnlyTitleBy() throws Exception {
        when(filmService.search("крад", "title")).thenReturn(List.of());

        mockMvc.perform(get("/films/search")
                        .param("query", "крад")
                        .param("by", "title"))
                .andExpect(status().isOk());

        verify(filmService).search("крад", "title");
    }

    @Test
    void searchWithOnlyDirectorBy() throws Exception {
        when(filmService.search("ли", "director")).thenReturn(List.of());

        mockMvc.perform(get("/films/search")
                        .param("query", "ли")
                        .param("by", "director"))
                .andExpect(status().isOk());

        verify(filmService).search("ли", "director");
    }

    @Test
    void searchHandlesCyrillicQueryInUrl() throws Exception {
        when(filmService.search("крад", "title,director")).thenReturn(List.of());

        mockMvc.perform(get("/films/search")
                        .param("query", "крад")
                        .param("by", "title,director"))
                .andExpect(status().isOk());

        verify(filmService).search("крад", "title,director");
    }

    @Test
    void searchWithoutQueryReturns400() throws Exception {
        mockMvc.perform(get("/films/search").param("by", "title,director"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(filmService);
    }
}

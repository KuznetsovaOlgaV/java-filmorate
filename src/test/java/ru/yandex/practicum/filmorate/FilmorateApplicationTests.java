package ru.yandex.practicum.filmorate;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Genre;
import ru.yandex.practicum.filmorate.model.Mpa;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.dao.FilmDbStorage;
import ru.yandex.practicum.filmorate.storage.dao.GenreDbStorage;
import ru.yandex.practicum.filmorate.storage.dao.MpaDbStorage;
import ru.yandex.practicum.filmorate.storage.dao.UserDbStorage;
import ru.yandex.practicum.filmorate.storage.mapper.FilmRowMapper;
import ru.yandex.practicum.filmorate.storage.mapper.GenreRowMapper;
import ru.yandex.practicum.filmorate.storage.mapper.MpaRowMapper;
import ru.yandex.practicum.filmorate.storage.mapper.UserRowMapper;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@JdbcTest
@AutoConfigureTestDatabase
@RequiredArgsConstructor(onConstructor_ = @Autowired)
@Import({
        UserDbStorage.class,
        FilmDbStorage.class,
        GenreDbStorage.class,
        MpaDbStorage.class,
        UserRowMapper.class,
        FilmRowMapper.class,
        GenreRowMapper.class,
        MpaRowMapper.class
})
class FilmorateApplicationTests {

    private final UserDbStorage userStorage;
    private final FilmDbStorage filmStorage;
    private final MpaDbStorage mpaStorage;
    private final GenreDbStorage genreStorage;

    private User createSampleUser(String email, String login, String name) {
        return User.builder()
                .email(email)
                .login(login)
                .name(name)
                .birthday(LocalDate.of(2000, 1, 1))
                .build();
    }

    private Film createSampleFilm(String name, int mpaId) {
        return Film.builder()
                .name(name)
                .description("Описание фильма " + name)
                .releaseDate(LocalDate.of(2020, 5, 20))
                .duration(120)
                .mpa(new Mpa(mpaId, null, null))
                .genres(new LinkedHashSet<>())
                .build();
    }

    @Test
    @DisplayName("Создание пользователя и поиск по ID")
    void testCreateAndFindUserById() {
        User newUser = userStorage.create(createSampleUser("test@mail.ru", "testlogin", "Test User"));

        Optional<User> userOptional = userStorage.getById(newUser.getId());

        assertThat(userOptional)
                .isPresent()
                .hasValueSatisfying(user -> {
                    assertThat(user).hasFieldOrPropertyWithValue("id", newUser.getId());
                    assertThat(user).hasFieldOrPropertyWithValue("email", "test@mail.ru");
                    assertThat(user).hasFieldOrPropertyWithValue("login", "testlogin");
                    assertThat(user).hasFieldOrPropertyWithValue("name", "Test User");
                });
    }

    @Test
    @DisplayName("Поиск пользователя по несуществующему ID возвращает пустой Optional")
    void testFindUserByNonExistentId() {
        Optional<User> userOptional = userStorage.getById(9999L);
        assertThat(userOptional).isEmpty();
    }

    @Test
    @DisplayName("Обновление данных пользователя")
    void testUpdateUser() {
        User user = userStorage.create(createSampleUser("old@mail.ru", "oldlogin", "Old Name"));

        user.setName("Updated Name");
        user.setEmail("new@mail.ru");
        userStorage.update(user);

        Optional<User> updatedUser = userStorage.getById(user.getId());
        assertThat(updatedUser)
                .isPresent()
                .hasValueSatisfying(u -> {
                    assertThat(u).hasFieldOrPropertyWithValue("name", "Updated Name");
                    assertThat(u).hasFieldOrPropertyWithValue("email", "new@mail.ru");
                });
    }

    @Test
    @DisplayName("Получение всех пользователей")
    void testGetAllUsers() {
        userStorage.create(createSampleUser("user1@mail.ru", "login1", "User One"));
        userStorage.create(createSampleUser("user2@mail.ru", "login2", "User Two"));

        List<User> users = userStorage.getAll();

        assertThat(users)
                .isNotNull()
                .hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Удаление пользователя")
    void testDeleteUser() {
        User user = userStorage.create(createSampleUser("delete@mail.ru", "delete_me", "Delete"));
        userStorage.delete(user.getId());

        Optional<User> deletedUser = userStorage.getById(user.getId());
        assertThat(deletedUser).isEmpty();
    }

    @Test
    @DisplayName("Добавление, получение и удаление друзей")
    void testFriendsOperations() {
        User user1 = userStorage.create(createSampleUser("u1@mail.ru", "u1", "U1"));
        User user2 = userStorage.create(createSampleUser("u2@mail.ru", "u2", "U2"));

        userStorage.addFriend(user1.getId(), user2.getId());

        List<User> friends = userStorage.getFriends(user1.getId());
        assertThat(friends)
                .isNotEmpty()
                .extracting(User::getId)
                .contains(user2.getId());

        userStorage.removeFriend(user1.getId(), user2.getId());
        List<User> friendsAfterRemoval = userStorage.getFriends(user1.getId());
        assertThat(friendsAfterRemoval).isEmpty();
    }

    @Test
    @DisplayName("Получение списка общих друзей")
    void testGetCommonFriends() {
        User user1 = userStorage.create(createSampleUser("first@mail.ru", "first", "First"));
        User user2 = userStorage.create(createSampleUser("second@mail.ru", "second", "Second"));
        User common = userStorage.create(createSampleUser("common@mail.ru", "common", "Common"));

        userStorage.addFriend(user1.getId(), common.getId());
        userStorage.addFriend(user2.getId(), common.getId());

        List<User> commonFriends = userStorage.getCommonFriends(user1.getId(), user2.getId());

        assertThat(commonFriends)
                .hasSize(1)
                .extracting(User::getId)
                .contains(common.getId());
    }

    @Test
    @DisplayName("Создание фильма и поиск по ID")
    void testCreateAndFindFilmById() {
        Film newFilm = filmStorage.create(createSampleFilm("Интерстеллар", 1));

        Optional<Film> filmOptional = filmStorage.getById(newFilm.getId());

        assertThat(filmOptional)
                .isPresent()
                .hasValueSatisfying(film -> {
                    assertThat(film).hasFieldOrPropertyWithValue("id", newFilm.getId());
                    assertThat(film).hasFieldOrPropertyWithValue("name", "Интерстеллар");
                    assertThat(film.getMpa()).isNotNull();
                    assertThat(film.getMpa().getId()).isEqualTo(1);
                });
    }

    @Test
    @DisplayName("Создание и сохранение фильма с жанрами")
    void testCreateFilmWithGenres() {
        Film film = createSampleFilm("Матрица", 1);
        film.setGenres(new LinkedHashSet<>(Set.of(
                new Genre(1, null),
                new Genre(2, null)
        )));

        Film createdFilm = filmStorage.create(film);
        Optional<Film> foundFilm = filmStorage.getById(createdFilm.getId());

        assertThat(foundFilm)
                .isPresent()
                .hasValueSatisfying(f -> assertThat(f.getGenres()).hasSize(2));
    }

    @Test
    @DisplayName("Обновление фильма")
    void testUpdateFilm() {
        Film film = filmStorage.create(createSampleFilm("Трудно быть богом", 2));

        film.setName("Трудно быть богом (Режиссерская версия)");
        film.setDuration(177);
        filmStorage.update(film);

        Optional<Film> updatedFilm = filmStorage.getById(film.getId());
        assertThat(updatedFilm)
                .isPresent()
                .hasValueSatisfying(f -> {
                    assertThat(f).hasFieldOrPropertyWithValue("name", "Трудно быть богом (Режиссерская версия)");
                    assertThat(f).hasFieldOrPropertyWithValue("duration", 177);
                });
    }

    @Test
    @DisplayName("Получение всех фильмов")
    void testGetAllFilms() {
        filmStorage.create(createSampleFilm("Фильм 1", 1));
        filmStorage.create(createSampleFilm("Фильм 2", 2));

        List<Film> films = filmStorage.getAll();

        assertThat(films)
                .isNotNull()
                .hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("Удаление фильма")
    void testDeleteFilm() {
        Film film = filmStorage.create(createSampleFilm("Для удаления", 1));
        filmStorage.delete(film.getId());

        Optional<Film> deletedFilm = filmStorage.getById(film.getId());
        assertThat(deletedFilm).isEmpty();
    }

    @Test
    @DisplayName("Добавление лайка, получение популярных фильмов и удаление лайка")
    void testLikeAndPopularFilms() {
        User user1 = userStorage.create(createSampleUser("like1@mail.ru", "like1", "L1"));
        User user2 = userStorage.create(createSampleUser("like2@mail.ru", "like2", "L2"));

        Film film1 = filmStorage.create(createSampleFilm("Менее популярный", 1));
        Film film2 = filmStorage.create(createSampleFilm("Самый популярный", 1));

        filmStorage.addLike(film1.getId(), user1.getId());
        filmStorage.addLike(film2.getId(), user1.getId());
        filmStorage.addLike(film2.getId(), user2.getId());

        List<Film> popular = filmStorage.getPopular(2);

        assertThat(popular)
                .isNotEmpty()
                .hasSize(2);
        assertThat(popular.get(0).getId()).isEqualTo(film2.getId());

        filmStorage.removeLike(film2.getId(), user1.getId());
        filmStorage.removeLike(film2.getId(), user2.getId());

        List<Film> popularAfterRemove = filmStorage.getPopular(1);
        assertThat(popularAfterRemove.get(0).getId()).isEqualTo(film1.getId());
    }

    @Test
    @DisplayName("Получение MPA-рейтинга по ID из справочника (data.sql)")
    void testGetMpaById() {
        Optional<Mpa> mpaOptional = mpaStorage.getById(1);

        assertThat(mpaOptional)
                .isPresent()
                .hasValueSatisfying(mpa -> {
                    assertThat(mpa.getId()).isEqualTo(1);
                    assertThat(mpa.getName()).isEqualTo("G");
                });
    }

    @Test
    @DisplayName("Получение всех MPA-рейтингов")
    void testGetAllMpa() {
        List<Mpa> mpaList = mpaStorage.getAll();
        assertThat(mpaList).hasSize(5);
    }

    @Test
    @DisplayName("Получение Жанра по ID из справочника (data.sql)")
    void testGetGenreById() {
        Optional<Genre> genreOptional = genreStorage.getById(1);

        assertThat(genreOptional)
                .isPresent()
                .hasValueSatisfying(genre -> {
                    assertThat(genre.getId()).isEqualTo(1);
                    assertThat(genre.getName()).isEqualTo("Комедия");
                });
    }

    @Test
    @DisplayName("Получение всех Жанров")
    void testGetAllGenres() {
        List<Genre> genres = genreStorage.getAll();
        assertThat(genres).hasSize(6);
    }
}
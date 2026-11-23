package ru.yandex.practicum.filmorate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.Film;
import ru.yandex.practicum.filmorate.model.Review;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.service.EventService;
import ru.yandex.practicum.filmorate.service.ReviewService;
import ru.yandex.practicum.filmorate.storage.ReviewStorage;
import ru.yandex.practicum.filmorate.storage.film.FilmStorage;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Mockito — моки и верификация
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewStorage reviewStorage;
    @Mock
    private UserStorage userStorage;
    @Mock
    private FilmStorage filmStorage;
    @Mock
    private EventService eventService;

    @InjectMocks
    private ReviewService reviewService;

    private Review review;
    private Film film;

    @BeforeEach
    void setUp() {
        film = Film.builder().id(2L).build();
        review = Review.builder()
                .reviewId(10L)
                .content("Отзыв")
                .isPositive(true)
                .userId(1L)
                .filmId(2L)
                .useful(0)
                .build();
    }

    // ---------- create ----------

    @Test
    void createWhenAllValidThenCallsStorage() {
        when(userStorage.existsById(1L)).thenReturn(true);
        when(filmStorage.getById(2L)).thenReturn(Optional.of(film));
        when(reviewStorage.add(review)).thenReturn(review);

        Review result = reviewService.create(review);

        assertThat(result).isEqualTo(review);
        verify(reviewStorage).add(review);
    }

    @Test
    void createWhenContentBlankThenValidationException() {
        review.setContent("   ");

        assertThatThrownBy(() -> reviewService.create(review))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Содержание");

        verifyNoInteractions(reviewStorage, userStorage, filmStorage);
    }

    @Test
    void createWhenContentNullThenValidationException() {
        review.setContent(null);

        assertThatThrownBy(() -> reviewService.create(review))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createWhenIsPositiveNullThenValidationException() {
        review.setIsPositive(null);

        assertThatThrownBy(() -> reviewService.create(review))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createWhenUserIdNullThenValidationException() {
        review.setUserId(null);

        assertThatThrownBy(() -> reviewService.create(review))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createWhenFilmIdNullThenValidationException() {
        review.setFilmId(null);

        assertThatThrownBy(() -> reviewService.create(review))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    void createWhenUserMissingThenNotFoundException() {
        when(userStorage.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.create(review))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Пользователь");

        verify(reviewStorage, never()).add(any());
    }

    @Test
    void createWhenFilmMissingThenNotFoundException() {
        when(userStorage.existsById(1L)).thenReturn(true);
        when(filmStorage.getById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.create(review))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Фильм");
    }

    // ---------- update / delete / getById ----------

    @Test
    void updateWhenNotFoundThenNotFoundException() {
        when(reviewStorage.getById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.update(review))
                .isInstanceOf(NotFoundException.class);

        verify(reviewStorage, never()).update(any());
    }

    @Test
    void updateWhenNullIdThenNotFoundException() {
        review.setReviewId(null);

        assertThatThrownBy(() -> reviewService.update(review))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void updateWhenFoundThenReturnsUpdated() {
        when(reviewStorage.getById(10L)).thenReturn(Optional.of(review));
        when(reviewStorage.update(review)).thenReturn(review);

        Review result = reviewService.update(review);

        assertThat(result).isEqualTo(review);
    }

    @Test
    void deleteWhenNotFoundThenNotFoundException() {
        when(reviewStorage.getById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.delete(10L))
                .isInstanceOf(NotFoundException.class);

        verify(reviewStorage, never()).delete(any());
    }

    @Test
    void getByIdWhenNotFoundThenNotFoundException() {
        when(reviewStorage.getById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.getById(10L))
                .isInstanceOf(NotFoundException.class);
    }

    // ---------- выборки с дефолтным count ----------

    @Test
    void getReviewsForFilmWithNullFilmIdDefaultsToFindAll() {
        reviewService.getReviewsForFilm(null, null);
        verify(reviewStorage).findAll(10);
    }

    @Test
    void getReviewsForFilmWithNullCountDefaultsTo10() {
        reviewService.getReviewsForFilm(2L, null);
        verify(reviewStorage).findByFilmId(2L, 10);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    void getReviewsForFilmWithNonPositiveCountDefaultsTo10(int count) {
        reviewService.getReviewsForFilm(2L, count);
        verify(reviewStorage).findByFilmId(2L, 10);
    }

    @Test
    void getReviewsForFilmWithValidCountPassesThrough() {
        reviewService.getReviewsForFilm(2L, 5);
        verify(reviewStorage).findByFilmId(2L, 5);
    }

    // ---------- лайки / дизлайки ----------

    @Test
    void addLikeWhenAllExistThenCallsStorage() {
        when(reviewStorage.getById(10L)).thenReturn(Optional.of(review));
        when(userStorage.existsById(1L)).thenReturn(true);

        reviewService.addLike(10L, 1L);

        verify(reviewStorage).addMark(10L, 1L, true);
    }

    @Test
    void addLikeWhenReviewMissingThenNotFoundException() {
        when(reviewStorage.getById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> reviewService.addLike(10L, 1L))
                .isInstanceOf(NotFoundException.class);
        verify(reviewStorage, never()).addMark(any(), any(), anyBoolean());
    }

    @Test
    void addLikeWhenUserMissingThenNotFoundException() {
        when(reviewStorage.getById(10L)).thenReturn(Optional.of(review));
        when(userStorage.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> reviewService.addLike(10L, 1L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void addDislikeThenCallsAddMarkWithFalse() {
        when(reviewStorage.getById(10L)).thenReturn(Optional.of(review));
        when(userStorage.existsById(1L)).thenReturn(true);

        reviewService.addDislike(10L, 1L);

        verify(reviewStorage).addMark(10L, 1L, false);
    }

    @Test
    void removeLikeThenCallsRemoveMarkWithTrue() {
        when(reviewStorage.getById(10L)).thenReturn(Optional.of(review));
        when(userStorage.existsById(1L)).thenReturn(true);

        reviewService.removeLike(10L, 1L);

        verify(reviewStorage).removeMark(10L, 1L, true);
    }

    @Test
    void removeDislikeThenCallsRemoveMarkWithFalse() {
        when(reviewStorage.getById(10L)).thenReturn(Optional.of(review));
        when(userStorage.existsById(1L)).thenReturn(true);

        reviewService.removeDislike(10L, 1L);

        verify(reviewStorage).removeMark(10L, 1L, false);
    }
}
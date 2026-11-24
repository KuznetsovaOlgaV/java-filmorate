package ru.yandex.practicum.filmorate.model;

import lombok.Builder;
import lombok.Data;

// У отзыва имеется рейтинг. При создании отзыва рейтинг равен нулю. Если пользователь оценил
// отзыв как полезный, это увеличивает его рейтинг на 1. Если как бесполезный, то уменьшает на 1.
@Data
@Builder
public class Review {
    private Long reviewId;
    private String content;      // текст отзыва
    private Boolean isPositive;  // тип отзыва: true — положительный/false — негативный
    private Long userId;         // автор отзыва
    private Long filmId;         // фильм, к которому оставлен отзыв
    private Integer useful;      // оценка полезности (полезно/бесполезно)
}

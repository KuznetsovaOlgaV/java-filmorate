package ru.yandex.practicum.filmorate.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import ru.yandex.practicum.filmorate.validator.MinimumDate;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Film {
    private Long id;

    @NotBlank(message = "Название фильма не может быть пустым")
    private String name;

    @Size(max = 200, message = "Максимальная длина описания — 200 символов")
    private String description;

    @NotNull(message = "Дата релиза обязательна к заполнению")
    @MinimumDate(value = "1895-12-28", message = "Дата релиза не может быть раньше 28 декабря 1895 года")
    private LocalDate releaseDate;

    @NotNull(message = "Продолжительность фильма обязательна")
    @Positive(message = "Продолжительность фильма должна быть положительным числом")
    private Integer duration;

    private Mpa mpa;

    @Builder.Default
    private LinkedHashSet<Genre> genres = new LinkedHashSet<>();

    @Builder.Default
    private Set<Long> likes = new HashSet<>();
}
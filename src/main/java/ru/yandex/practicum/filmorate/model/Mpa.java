package ru.yandex.practicum.filmorate.model;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(of = "id")
public class Mpa {
    private Integer id;
    private String name;
    private String description;

    public Mpa(Integer id, String name) {
        this.id = id;
        this.name = name;
    }
}
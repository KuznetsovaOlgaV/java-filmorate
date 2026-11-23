package ru.yandex.practicum.filmorate.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class Event {
    private Long eventId;
    private Long userId;
    private String eventType;
    private String operation;
    private Long timestamp;
    private Long entityId;
}

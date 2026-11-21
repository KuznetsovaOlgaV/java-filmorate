package ru.yandex.practicum.filmorate.storage;

import ru.yandex.practicum.filmorate.model.Event;

import java.util.List;

public interface EventStorage {
    List<Event> getEventsByUserId(Long id);
    public void putUserEvent(Long userId, String eventType, String operation, Long entityId);
    public void putFilmEvent(Long userId, String eventType, String operation, Long entityId);
    public void putReviewEvent(Long userId, String eventType, String operation, Long entityId);
}

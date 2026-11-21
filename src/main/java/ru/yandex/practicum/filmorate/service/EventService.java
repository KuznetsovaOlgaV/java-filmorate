package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.storage.EventStorage;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {
    private final EventStorage eventStorage;

    public List<Event> getEventsByUserId(Long id) {
        return eventStorage.getEventsByUserId(id);
    }
    public void putUserEvent(Long userId, String eventType, String operation, Long entityId) {
        eventStorage.putUserEvent(userId, eventType, operation, entityId);
    }

    public void putFilmEvent(Long userId, String eventType, String operation, Long entityId) {
        eventStorage.putFilmEvent(userId, eventType, operation, entityId);
    }

    public void putReviewEvent(Long userId, String eventType, String operation, Long entityId) {
        eventStorage.putReviewEvent(userId, eventType, operation, entityId);
    }
}

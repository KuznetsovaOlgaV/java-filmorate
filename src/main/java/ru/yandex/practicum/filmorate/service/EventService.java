package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ru.yandex.practicum.filmorate.model.Event;
import ru.yandex.practicum.filmorate.storage.EventStorage;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EventService {

    public enum EventType {
        LIKE, REVIEW, FRIEND
    }

    public enum Operation {
        ADD, REMOVE, UPDATE
    }

    private final EventStorage eventStorage;

    public List<Event> getEventsByUserId(Long id) {
        return eventStorage.getEventsByUserId(id);
    }

    private void putUserEvent(Long userId, EventType eventType, Operation operation, Long entityId) {
        eventStorage.putUserEvent(userId, eventType.name(), operation.name(), entityId);
    }

    private void putFilmEvent(Long userId, EventType eventType, Operation operation, Long entityId) {
        eventStorage.putFilmEvent(userId, eventType.name(), operation.name(), entityId);
    }

    private void putReviewEvent(Long userId, EventType eventType, Operation operation, Long entityId) {
        eventStorage.putReviewEvent(userId, eventType.name(), operation.name(), entityId);
    }

    public void addLikeEvent(Long userId, Long filmId) {
        putFilmEvent(userId, EventType.LIKE, Operation.ADD, filmId);
    }

    public void removeLikeEvent(Long userId, Long filmId) {
        putFilmEvent(userId, EventType.LIKE, Operation.REMOVE, filmId);
    }

    public void addFriendEvent(Long userId, Long friendId) {
        putUserEvent(userId, EventType.FRIEND, Operation.ADD, friendId);
    }

    public void removeFriendEvent(Long userId, Long friendId) {
        putUserEvent(userId, EventType.FRIEND, Operation.REMOVE, friendId);
    }

    public void addReviewEvent(Long userId, Long reviewId) {
        putReviewEvent(userId, EventType.REVIEW, Operation.ADD, reviewId);
    }

    public void updateReviewEvent(Long userId, Long reviewId) {
        putReviewEvent(userId, EventType.REVIEW, Operation.UPDATE, reviewId);
    }

    public void removeReviewEvent(Long userId, Long reviewId) {
        putReviewEvent(userId, EventType.REVIEW, Operation.REMOVE, reviewId);
    }

}

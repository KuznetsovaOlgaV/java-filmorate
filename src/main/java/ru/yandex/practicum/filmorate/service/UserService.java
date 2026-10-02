package ru.yandex.practicum.filmorate.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.yandex.practicum.filmorate.exception.NotFoundException;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;
import ru.yandex.practicum.filmorate.storage.user.UserStorage;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {
    private final UserStorage userStorage;

    public Collection<User> findAll() {
        return userStorage.findAll();
    }

    public User findById(Long id) {
        return userStorage.findById(id)
                .orElseThrow(() -> new NotFoundException("Пользователь с id = " + id + " не найден"));
    }

    public User create(User user) {
        validateUser(user);
        User created = userStorage.create(user);
        log.info("Пользователь успешно создан: {}", created);
        return created;
    }

    public User update(User user) {
        User oldUser = findById(user.getId());
        validateUser(user);
        // Сохраняем уже существующих друзей при обновлении пользователя
        user.setFriends(oldUser.getFriends());
        User updated = userStorage.update(user);
        log.info("Пользователь с id {} успешно обновлен", updated.getId());
        return updated;
    }

    public void addFriend(Long userId, Long friendId) {
        if (Objects.equals(userId, friendId)) {
            throw new ValidationException("Пользователь не может добавить сам себя в друзья");
        }
        User user = findById(userId);
        User friend = findById(friendId);

        user.getFriends().add(friendId);
        friend.getFriends().add(userId);
        log.info("Пользователи id {} и id {} теперь друзья", userId, friendId);
    }

    public void removeFriend(Long userId, Long friendId) {
        User user = findById(userId);
        User friend = findById(friendId);

        user.getFriends().remove(friendId);
        friend.getFriends().remove(userId);
        log.info("Пользователи id {} и id {} больше не друзья", userId, friendId);
    }

    public List<User> getFriends(Long userId) {
        User user = findById(userId);
        return user.getFriends().stream()
                .map(this::findById)
                .collect(Collectors.toList());
    }

    public List<User> getCommonFriends(Long userId, Long otherId) {
        User user = findById(userId);
        User otherUser = findById(otherId);

        Set<Long> commonFriendIds = new HashSet<>(user.getFriends());
        commonFriendIds.retainAll(otherUser.getFriends());

        return commonFriendIds.stream()
                .map(this::findById)
                .collect(Collectors.toList());
    }

    public void validateUser(User user) {
        if (!StringUtils.hasText(user.getEmail()) || !user.getEmail().contains("@")) {
            log.warn("Валидация не пройдена: некорректный email");
            throw new ValidationException("Электронная почта не может быть пустой и должна содержать символ @");
        }
        if (!StringUtils.hasText(user.getLogin()) || user.getLogin().contains(" ")) {
            log.warn("Валидация не пройдена: некорректный логин");
            throw new ValidationException("Логин не может быть пустым и содержать пробелы");
        }
        if (user.getBirthday() == null || user.getBirthday().isAfter(LocalDate.now())) {
            log.warn("Валидация не пройдена: дата рождения указана в будущем");
            throw new ValidationException("Дата рождения не может быть в будущем");
        }
        if (!StringUtils.hasText(user.getName())) {
            log.info("Имя пользователя не указано, используется логин: {}", user.getLogin());
            user.setName(user.getLogin());
        }
    }
}
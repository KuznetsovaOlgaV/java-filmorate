package ru.yandex.practicum.filmorate.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import ru.yandex.practicum.filmorate.exception.ValidationException;
import ru.yandex.practicum.filmorate.model.User;

import java.time.LocalDate;
import java.util.*;

@Slf4j
@RestController
@RequestMapping("/users")
public class UserController {

    private final Map<Integer, User> users = new HashMap<>();
    private int idCounter = 0;

    @GetMapping
    public Collection<User> findAll() {
        log.info("Получен запрос на получение списка всех пользователей. Текущее количество: {}", users.size());
        return users.values();
    }

    @PostMapping
    public User create(@RequestBody User user) {
        log.info("Получен запрос на создание пользователя: {}", user);
        validateUser(user);

        user.setId(++idCounter);
        users.put(user.getId(), user);
        log.info("Пользователь успешно создан с id: {}", user.getId());
        return user;
    }

    @PutMapping
    public User update(@RequestBody User user) {
        log.info("Получен запрос на обновление пользователя: {}", user);
        if (!users.containsKey(user.getId())) {
            log.error("Ошибка обновления: пользователь с id {} не найден", user.getId());
            throw new ValidationException("Пользователь с id = " + user.getId() + " не найден");
        }

        validateUser(user);
        users.put(user.getId(), user);
        log.info("Пользователь с id {} успешно обновлен", user.getId());
        return user;
    }

    public void validateUser(User user) {
        if (user.getEmail() == null || user.getEmail().isBlank() || !user.getEmail().contains("@")) {
            log.warn("Валидация не пройдена: некорректный email");
            throw new ValidationException("Электронная почта не может быть пустой и должна содержать символ @");
        }
        if (user.getLogin() == null || user.getLogin().isBlank() || user.getLogin().contains(" ")) {
            log.warn("Валидация не пройдена: некорректный логин");
            throw new ValidationException("Логин не может быть пустым и содержать пробелы");
        }
        if (user.getBirthday() == null || user.getBirthday().isAfter(LocalDate.now())) {
            log.warn("Валидация не пройдена: дата рождения указана в будущем");
            throw new ValidationException("Дата рождения не может быть в будущем");
        }
        // Если имя для отображения пустое, используется логин
        if (user.getName() == null || user.getName().isBlank()) {
            log.info("Имя пользователя не указано, используется логин: {}", user.getLogin());
            user.setName(user.getLogin());
        }
    }
}
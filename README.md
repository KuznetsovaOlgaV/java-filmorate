# java-filmorate

Template repository for Filmorate project.

# Filmorate

Сервис для подбора фильмов и общения пользователей.

## Схема базы данных

## filmorate_db.png

##  

erDiagram
USERS ||--o{ LIKES : "ставит"
USERS ||--o{ FRIENDS : "инициатор"
USERS ||--o{ FRIENDS : "друг"
FRIENDSHIP_STATUS ||--o{ FRIENDS : "определяет"

    FILMS ||--o{ LIKES : "получает"
    FILMS ||--o{ FILM_GENRES : "содержит"
    GENRES ||--o{ FILM_GENRES : "относится к"
    MPA_RATINGS ||--o{ FILMS : "ограничивает"

    USERS {
        bigint user_id PK
        varchar email UK
        varchar login UK
        varchar name
        date birthday
    }

    FRIENDSHIP_STATUS {
        int status_id PK
        varchar name UK
    }

    FRIENDS {
        bigint user_id PK, FK
        bigint friend_id PK, FK
        int status_id FK
    }

    MPA_RATINGS {
        int mpa_id PK
        varchar name UK
        varchar description
    }

    GENRES {
        int genre_id PK
        varchar name UK
    }

    FILMS {
        bigint film_id PK
        varchar name
        varchar description
        date release_date
        int duration
        int mpa_rating_id FK
    }

    FILM_GENRES {
        bigint film_id PK, FK
        int genre_id PK, FK
    }

    LIKES {
        bigint film_id PK, FK
        bigint user_id PK, FK
    }

Примеры SQL-запросов для основных операций
Фильмы
Получить все фильмы:
SELECT f.film_id, f.name, f.description, f.release_date, f.duration,
m.mpa_id, m.name AS mpa_name
FROM films AS f
JOIN mpa_ratings AS m ON f.mpa_rating_id = m.mpa_id;

Получить фильм по ID:
SELECT f.film_id, f.name, f.description, f.release_date, f.duration,
m.mpa_id, m.name AS mpa_name
FROM films AS f
JOIN mpa_ratings AS m ON f.mpa_rating_id = m.mpa_id
WHERE f.film_id = 1;

Получить список жанров фильма:
SELECT g.genre_id, g.name
FROM genres AS g
JOIN film_genres AS fg ON g.genre_id = fg.genre_id
WHERE fg.film_id = 1
ORDER BY g.genre_id;

Топ-N наиболее популярных фильмов по лайкам:
SELECT f.film_id, f.name, f.description, f.release_date, f.duration,
m.name AS mpa_name,
COUNT(l.user_id) AS likes_count
FROM films AS f
JOIN mpa_ratings AS m ON f.mpa_rating_id = m.mpa_id
LEFT JOIN likes AS l ON f.film_id = l.film_id
GROUP BY f.film_id, m.name
ORDER BY likes_count DESC
LIMIT 10;

Поставить лайк фильму:
INSERT INTO likes (film_id, user_id)
VALUES (1, 2);

Удалить лайк у фильма:
DELETE FROM likes
WHERE film_id = 1 AND user_id = 2;

Пользователи
Получить всех пользователей:
SELECT user_id, email, login, name, birthday
FROM users;

Получить пользователя по ID:
SELECT user_id, email, login, name, birthday
FROM users
WHERE user_id = 1;

Список подтверждённых друзей пользователя:
SELECT u.user_id, u.email, u.login, u.name, u.birthday
FROM users AS u
JOIN friends AS f ON u.user_id = f.friend_id
JOIN friendship_status AS fs ON f.status_id = fs.status_id
WHERE f.user_id = 1 AND fs.name = 'Подтверждённая';

Общие друзья двух пользователей (id 1 и id 2):
SELECT u.user_id, u.email, u.login, u.name, u.birthday
FROM users AS u
JOIN friends AS f1 ON u.user_id = f1.friend_id AND f1.user_id = 1
JOIN friends AS f2 ON u.user_id = f2.friend_id AND f2.user_id = 2;

Отправить запрос на добавление в друзья:
INSERT INTO friends (user_id, friend_id, status_id)
VALUES (1, 2, (SELECT status_id FROM friendship_status WHERE name = 'Неподтверждённая'));

Подтвердить дружбу:
UPDATE friends
SET status_id = (SELECT status_id FROM friendship_status WHERE name = 'Подтверждённая')
WHERE user_id = 1 AND friend_id = 2;
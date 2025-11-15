# java-filmorate
Template repository for Filmorate project.
![Схема базы данных](https://raw.githubusercontent.com/vd89zen/java-filmorate/main/QuickDBD-filmorate.png)

# Описание схемы базы данных кино‑сервиса


## Общее назначение

Схема реализует базовый функционал кино‑сервиса, позволяющий:
- хранить данные о пользователях и фильмах;
- назначать фильмам жанры и рейтинги MPAA;
- управлять дружескими связями между пользователями;
- ставить лайки фильмам.


## Ключевые сущности и связи

- **`Film ↔ RatingMPAA`**  
  Каждый фильм имеет один рейтинг MPAA (связь «один‑к‑одному»).

- **`Film ↔ Genre`**  
  Многие‑ко‑многим через связующую таблицу `FilmGenre`.


- **`User ↔ User`**  
  Дружеские связи реализуются через таблицу `Friendship` (связь «многие‑ко‑многим» с атрибутом статуса).


- **`User ↔ Film`**  
  Лайки фильмов хранятся в таблице `FilmLike` (связь «многие‑ко‑многим»).


## Примеры запросов
```sql
1. Получение фильма с рейтингом и жанрами
SELECT
  f.id,
  f.name,
  f.description,
  f.releaseDate,
  f.duration,
  r.code AS rating_code,
  STRING_AGG(g.name, ', ') AS genres
FROM Film f
JOIN RatingMPAA r ON f.ratingMPAA = r.id
LEFT JOIN FilmGenre fg ON f.id = fg.film_id
LEFT JOIN Genre g ON fg.genre_id = g.id
WHERE f.id = 1
GROUP BY f.id, f.name, f.description, f.releaseDate, f.duration, r.code;

2. Друзья пользователя
SELECT
  u.id,
  u.email,
  u.login,
  u.name,
  fr.status
FROM Friendship fr
JOIN User u ON fr.friend_id = u.id
WHERE fr.user_id = 888 AND fr.status = 'CONFIRMED';

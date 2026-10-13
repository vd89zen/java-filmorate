# Модель данных

H2, реляционная схема.

| Таблица | Колонки |
|---|---|
| `users` | `id`, `email`, `login`, `name`, `birthday`, `password`, `role`, `created_at` |
| `films` | `id`, `name`, `description`, `release_date`, `duration`, `rating_mpaa_id` (nullable), `rating_rars_id` (nullable) |
| `genres` | `id`, `name` (`UNIQUE`) |
| `rating_mpaa` | `id`, `name` |
| `rating_rars` | `id`, `name` |
| `film_genres` | `film_id`, `genre_id` (many-to-many) |
| `film_likes` | `film_id`, `user_id` |
| `friendship` | `user_id`, `friend_id` |
| `directors` | `id`, `name` (`UNIQUE`) |
| `film_directors` | `film_id`, `director_id` (many-to-many) |
| `reviews` | `id`, `content`, `is_positive`, `user_id`, `film_id`, `useful`, `UNIQUE (user_id, film_id)` |
| `review_opinions` | `review_id`, `user_id`, `is_useful` (PK: `review_id, user_id`) |
| `events` | `id`, `time_stamp`, `user_id`, `event_type_id`, `operation_type_id`, `entity_id` |
| `event_types` | `id`, `name` (`LIKE`, `REVIEW`, `FRIEND`) |
| `operation_types` | `id`, `name` (`ADD`, `UPDATE`, `REMOVE`) |

## Особенности

- `users.password` — BCrypt-хеш. Никогда не отдаётся наружу: `UserPublicDto` его не содержит.
- `users.role` — `USER` или `ADMIN`, `VARCHAR(20)`.
- `users.created_at` — момент регистрации, `TIMESTAMP`.
- `films.rating_mpaa_id` и `films.rating_rars_id` — обе колонки **nullable**.
  У фильма может быть рейтинг только из одной системы, только из другой, из обеих,
  но не больше одного рейтинга на систему (гарантия — на уровне модели: в `Film` поля
  `RatingMpaaId mpa` и `RatingRarsId rars` — не коллекции).
  Правило «хотя бы один задан» проверяется на уровне DTO (`@AssertTrue` в `NewFilmRequest`),
  а не схемы — так проще поддерживать и понятнее в API.
- Каскадное удаление для `film_likes`, `friendship`, `film_directors`, `reviews`,
  `review_opinions`, `events` — связанные строки подчищаются автоматически при удалении
  пользователя или фильма.
- Для `film_genres.genre_id` — `ON DELETE RESTRICT`: нельзя удалить жанр, который
  используется в фильмах (сервис отдаёт `400` вместо `500` при попытке).

## Справочники

Предзаполнены при первом запуске:

- `genres` — 6 записей;
- `rating_mpaa` — 5 записей (американская система: `G`, `PG`, `PG-13`, `R`, `NC-17`);
- `rating_rars` — 5 записей (российская система: `0+`, `6+`, `12+`, `16+`, `18+`).

Данные сохраняются между перезапусками: скрипты инициализации не создают дубликатов
и не стирают существующие записи.

## Как добавить жанр

`src/main/resources/data.sql`:

```sql
INSERT INTO genres (name)
SELECT 'Новый жанр' WHERE NOT EXISTS (SELECT 1 FROM genres WHERE name = 'Новый жанр');
```

После перезапуска жанр появится в `GET /genres`.

Альтернатива — через API: `POST /admin/genres` (требует роль `ADMIN`).
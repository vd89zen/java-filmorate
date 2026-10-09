# Модель данных

H2, реляционная схема.

| Таблица | Колонки |
|---|---|
| `users` | `id`, `email`, `login`, `name`, `birthday`, `password`, `role`, `created_at` |
| `films` | `id`, `name`, `description`, `release_date`, `duration`, `rating_mpaa_id` |
| `genres` | `id`, `name` |
| `rating_mpaa` | `id`, `name` |
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
- Каскадное удаление для `film_likes`, `friendship`, `film_directors`, `reviews`,
  `review_opinions`, `events` — связанные строки подчищаются автоматически при удалении
  пользователя или фильма.

## Справочники

Предзаполнены при первом запуске:

- `genres` — 6 записей;
- `rating_mpaa` — 5 записей.

Данные сохраняются между перезапусками: скрипты инициализации не создают дубликатов
и не стирают существующие записи.

## Как добавить жанр

`src/main/resources/data.sql`:

```sql
INSERT INTO genres (name)
SELECT 'Новый жанр' WHERE NOT EXISTS (SELECT 1 FROM genres WHERE name = 'Новый жанр');
```

После перезапуска жанр появится в `GET /genres`.
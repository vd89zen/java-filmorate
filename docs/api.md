# API

Базовый URL: `http://localhost:8080`.
Интерактивная документация: `/swagger-ui.html` (OpenAPI: `/v3/api-docs`).

## Пагинация

`GET /films`, `GET /admin/users`:

| Параметр | По умолчанию | Ограничение |
|---|---|---|
| `from` | `0` | `>= 0` |
| `size` | `10` | `1..100` |

Выход за границы → `400`.

## Аутентификация

| Метод | Путь | Успех |
|---|---|---|
| `POST` | `/auth/login` | `200 OK` + `AuthResponse` |

## Пользователи (публичное)

| Метод | Путь | Успех |
|---|---|---|
| `POST` | `/users` | `201 Created` + `UserDto` |
| `GET` | `/users/{userId}` | `200 OK` + `UserPublicDto` (без email/password/role) |

**Регистрация:**

```bash
curl -X POST http://localhost:8080/users \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","login":"userlogin","name":"Иван","birthday":"1990-01-15","password":"secret123"}'
```

`password`: обязательное, 6–100 символов. Если `name` пуст — используется `login`.
Email уникален.

## Пользователи (`/me`, токен обязателен)

| Метод | Путь | Успех |
|---|---|---|
| `PUT` | `/me` | `200 OK` + `UserDto` |
| `DELETE` | `/me` | `204 No Content` |
| `PUT` | `/me/friends/{friendId}` | `204 No Content` |
| `DELETE` | `/me/friends/{friendId}` | `204 No Content` |
| `GET` | `/me/friends` | `200 OK` + `[UserPublicDto]` |
| `GET` | `/me/friends/common/{friendId}` | `200 OK` + `[UserPublicDto]` |
| `GET` | `/me/recommendations` | `200 OK` + `[FilmDto]` |

`userId` — из токена. В `PUT /me` поле `id` в теле игнорируется.

**Рекомендации** — коллаборативная фильтрация:

1. Находятся пользователи с пересечением по лайкам.
2. Из их лайков исключаются уже лайкнутые целевым.
3. Сортировка — по сумме пересечений с похожими пользователями.

Пустой массив — если нет лайков или нет похожих.

## Фильмы (публичное)

| Метод | Путь | Успех |
|---|---|---|
| `GET` | `/films?from=&size=` | `200 OK` + `[FilmDto]` |
| `GET` | `/films/{filmId}` | `200 OK` + `FilmDto` |
| `GET` | `/films/popular?count=&genreId=&year=` | `200 OK` + `[FilmDto]` |
| `GET` | `/films/director/{directorId}?sortBy=year\|likes` | `200 OK` + `[FilmDto]` |
| `GET` | `/films/search?...` | `200 OK` + `[FilmDto]` |

**Топ популярных:** сортировка по убыванию лайков, при равенстве — по `id`.
Фильмы без лайков в топ не попадают. Несуществующий `genreId` → `404`.

**Фильмы режиссёра:** `sortBy=year` (возрастание даты) или `likes` (убывание лайков).
Несуществующий режиссёр → `404`, `sortBy=bad` → `400`.

**Рейтинги в `FilmDto`:**

У фильма может быть **не более одного** рейтинга каждой системы: MPAA и RARS.
Оба поля опциональны, но при создании (см. раздел Admin) требуется **хотя бы один**.

```json
{
  "id": 1,
  "name": "Inception",
  "mpa":  { "id": 3, "name": "PG-13" },
  "rars": { "id": 4, "name": "16+" }
}
```

Если у фильма нет рейтинга в какой-то системе, соответствующее поле в ответе отсутствует
(`mpa: null` или `rars: null`).

**Поиск:**

```http
GET /films/search?query=нолан&by=director,title&yearFrom=2000&yearTo=2020&mpaIds=2,3&rarsIds=4,5&from=0&size=10
```

| Параметр | Описание |
|---|---|
| `query` | Подстрока, регистронезависимо |
| `by` | `title`, `director`, `description` через запятую. По умолчанию `title`, если задан `query` |
| `year` | Точный год |
| `yearFrom`/`yearTo` | Диапазон годов включительно |
| `duration` | Точная длительность (мин) |
| `durationFrom`/`durationTo` | Диапазон длительностей |
| `mpaIds` | Список id рейтингов MPAA через запятую |
| `rarsIds` | Список id рейтингов RARS через запятую |
| `from`/`size` | Пагинация |

Правила:

- Все фильтры — `AND`; внутри `query` — `OR` между выбранными `by`.
- `year` и `yearFrom`/`yearTo` взаимоисключающие.
- `duration` и `durationFrom`/`durationTo` взаимоисключающие.
- `by` без `query` → `400`.
- Спецсимволы LIKE (`%`, `_`) трактуются буквально.
- `mpaIds` и `rarsIds` можно комбинировать: вернутся фильмы, у которых MPAA — из `mpaIds`
  **и** RARS — из `rarsIds` (оба условия через `AND`).
- Без параметров — всё, отсортированное по лайкам.

## Фильмы (`/me`, токен обязателен)

| Метод | Путь | Успех |
|---|---|---|
| `PUT` | `/me/films/{filmId}/like` | `204 No Content` |
| `DELETE` | `/me/films/{filmId}/like` | `204 No Content` |
| `GET` | `/me/films/common?friendId={id}` | `200 OK` + `[FilmDto]` |

Пользователь — из токена. `friendId` обязателен, иначе `400`.

## Отзывы (публичное)

| Метод | Путь | Успех |
|---|---|---|
| `GET` | `/reviews/{id}` | `200 OK` + `ReviewDto` |
| `GET` | `/reviews?filmId=&count=10` | `200 OK` + `[ReviewDto]` |

`count ≤ 100`.

## Отзывы (`/me`, токен обязателен)

| Метод | Путь | Успех |
|---|---|---|
| `POST` | `/me/reviews` | `201 Created` + `ReviewDto` |
| `PUT` | `/me/reviews` | `200 OK` + `ReviewDto` |
| `DELETE` | `/me/reviews/{id}` | `204 No Content` |
| `PUT` | `/me/reviews/{id}/like` | `204 No Content` |
| `PUT` | `/me/reviews/{id}/dislike` | `204 No Content` |
| `DELETE` | `/me/reviews/{id}/like` | `204 No Content` |
| `DELETE` | `/me/reviews/{id}/dislike` | `204 No Content` |

Автор — из токена. Один пользователь — один отзыв на фильм, повтор → `400`.

**`update`/`delete` чужого отзыва → `404`.**

`useful`: 0 → ±1 за лайк/дизлайк. Сортировка — по `useful` DESC, затем по `reviewId` ASC.

**Создание:**

```bash
curl -X POST http://localhost:8080/me/reviews \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"content":"This film is sooo baad.","isPositive":false,"filmId":1}'
```

`userId` в теле нет.

`PUT /me/reviews` — частичное: только `content`, только `isPositive` или оба.
Пустой → `400`.

## Лента событий (`/me`, токен обязателен)

| Метод | Путь | Успех |
|---|---|---|
| `GET` | `/me/feed/user` | `200 OK` + `[EventDto]` |
| `GET` | `/me/feed/friends` | `200 OK` + `[EventDto]` |
| `GET` | `/me/feed/user/enriched` | `200 OK` + `[EnrichedEventDto]` |
| `GET` | `/me/feed/friends/enriched` | `200 OK` + `[EnrichedEventDto]` |

В ленту попадают:

- `LIKE/ADD|REMOVE` — лайки фильмов (в ленте друзей — только от друзей);
- `FRIEND/ADD|REMOVE` — дружба;
- `REVIEW/ADD|UPDATE|REMOVE` — отзывы.

Лайки/дизлайки отзывов событий **не** создают.

Пример «сырого» события:

```json
{
  "eventId": 42,
  "timestamp": 1700000000000,
  "userId": 7,
  "eventType": "LIKE",
  "operation": "ADD",
  "entityId": 13
}
```

`eventType`: `LIKE`, `REVIEW`, `FRIEND`. `operation`: `ADD`, `REMOVE`, `UPDATE`.

Пример обогащённого `LIKE`:

```json
{
  "eventId": 42,
  "timestamp": 1700000000000,
  "userId": 7,
  "eventType": "LIKE",
  "operation": "ADD",
  "film": { "id": 13, "name": "Inception", "releaseDate": "2010-07-16" },
  "user": null,
  "review": null
}
```

`film` — для `LIKE`, `user` — для `FRIEND`, `review` — для `REVIEW`.
`entityId` в обогащённый ответ не входит.
Для `REVIEW/REMOVE` поле `review` = `null`.

## Справочники (публичное)

| Метод | Путь | Успех |
|---|---|---|
| `GET` | `/genres` | `200 OK` + `[GenreDto]` |
| `GET` | `/genres/{genreId}` | `200 OK` + `GenreDto` |
| `GET` | `/mpa` | `200 OK` + `[RatingMpaaDto]` |
| `GET` | `/mpa/{ratingId}` | `200 OK` + `RatingMpaaDto` |
| `GET` | `/rars` | `200 OK` + `[RatingRarsDto]` |
| `GET` | `/rars/{ratingId}` | `200 OK` + `RatingRarsDto` |
| `GET` | `/directors` | `200 OK` + `[DirectorDto]` |
| `GET` | `/directors/{id}` | `200 OK` + `DirectorDto` |

Предзаполнено:

- 6 жанров;
- 5 рейтингов MPAA (`G`, `PG`, `PG-13`, `R`, `NC-17`);
- 5 рейтингов RARS (`0+`, `6+`, `12+`, `16+`, `18+`).

MPAA — американская система, RARS (Russian Age Rating System) — российская.
Это два независимых справочника; у фильма может быть рейтинг из одной, из другой
или из обеих систем одновременно.

## Admin (роль `ADMIN`)

| Метод | Путь | Успех |
|---|---|---|
| `POST` | `/admin/films` | `201 Created` + `FilmDto` |
| `PUT` | `/admin/films` | `200 OK` + `FilmDto` |
| `DELETE` | `/admin/films/{filmId}` | `204 No Content` |
| `GET` | `/admin/users?from=&size=` | `200 OK` + `[UserDto]` |
| `DELETE` | `/admin/users/{userId}` | `204 No Content` |
| `PUT` | `/admin/users/{userId}/password` | `204 No Content` |
| `POST` | `/admin/directors` | `201 Created` + `DirectorDto` |
| `PUT` | `/admin/directors` | `200 OK` + `DirectorDto` |
| `DELETE` | `/admin/directors/{id}` | `204 No Content` |
| `POST` | `/admin/genres` | `201 Created` + `GenreDto` |
| `PUT` | `/admin/genres` | `200 OK` + `GenreDto` |
| `DELETE` | `/admin/genres/{id}` | `204 No Content` |

**Создание фильма:**

Обязательно указать **хотя бы один** рейтинг (MPAA или RARS). Оба опциональны по отдельности,
но не одновременно.

Только MPAA:

```bash
curl -X POST http://localhost:8080/admin/films \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
        "name": "Inception",
        "description": "...",
        "releaseDate": "2010-07-16",
        "duration": 148,
        "mpa": { "id": 1 },
        "genres": [ { "id": 1 }, { "id": 2 } ],
        "directors": [ { "id": 1 } ]
      }'
```

Только RARS (для российских фильмов):

```bash
curl -X POST http://localhost:8080/admin/films \
  -H "Authorization: Bearer $ADMIN_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
        "name": "Левиафан",
        "description": "...",
        "releaseDate": "2014-09-05",
        "duration": 140,
        "rars": { "id": 4 }
      }'
```

Оба рейтинга:

```bash
-d '{
      "name": "Дюна",
      "mpa":  { "id": 3 },
      "rars": { "id": 4 },
      ...
    }'
```

`mpa`, `rars`, `genres`, `directors` — объекты с полем `id`.

Если не указать ни `mpa`, ни `rars` → `400`.

Имя режиссёра и жанра уникальны, повтор → `400`.
Нельзя удалить жанр, который используется в фильмах → `400`.

Без токена → `401`. С токеном без `ADMIN` → `403`.